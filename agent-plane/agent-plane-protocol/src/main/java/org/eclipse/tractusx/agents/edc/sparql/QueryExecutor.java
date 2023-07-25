//
// Copyright (C) 2022-2023 Catena-X Association and others. 
// 
// This program and the accompanying materials are made available under the
// terms of the Apache License 2.0 which is available at
// http://www.apache.org/licenses/.
//  
// SPDX-FileType: SOURCE
// SPDX-FileCopyrightText: 2022-2023 Catena-X Association
// SPDX-License-Identifier: Apache-2.0
//
package org.eclipse.tractusx.agents.edc.sparql;

import static org.apache.jena.http.HttpLib.*;

import java.io.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.util.IOUtils;
import org.eclipse.tractusx.agents.edc.AgentConfig;
import org.apache.jena.atlas.RuntimeIOException;
import org.apache.jena.atlas.iterator.Iter;
import org.apache.jena.atlas.json.JSON;
import org.apache.jena.atlas.json.JsonArray;
import org.apache.jena.atlas.json.JsonObject;
import org.apache.jena.atlas.lib.InternalErrorException;
import org.apache.jena.atlas.lib.Pair;
import org.apache.jena.atlas.logging.Log;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Triple;
import org.apache.jena.http.HttpEnv;
import org.apache.jena.http.HttpLib;
import org.apache.jena.query.*;
import org.apache.jena.riot.*;
import org.apache.jena.riot.resultset.ResultSetLang;
import org.apache.jena.riot.resultset.ResultSetReaderRegistry;
import org.apache.jena.riot.web.HttpNames;
import org.apache.jena.sparql.ARQException;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.DatasetGraphFactory;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.engine.http.HttpParams;
import org.apache.jena.sparql.engine.http.QueryExceptionHTTP;
import org.apache.jena.sparql.exec.RowSet;
import org.apache.jena.sparql.exec.http.Params;
import org.apache.jena.sparql.exec.http.QuerySendMode;
import org.apache.jena.sparql.util.Context;
import org.apache.jena.sparql.exec.QueryExec;

/**
 * An Exec implementation which understands KA-MATCH and KA-TRANSFER remote
 * services over HTTP.
 */
public class QueryExecutor implements QueryExec {

    /** @deprecated Use {@link #newBuilder} */
    @Deprecated
    public static QueryExecutorBuilder create() { return newBuilder() ; }

    public static QueryExecutorBuilder newBuilder() { return QueryExecutorBuilder.create(); }

    public static QueryExecutorBuilder service(String serviceURL) {
        return newBuilder().endpoint(serviceURL);
    }

    // Blazegraph has a bug : it impacts wikidata.
    // Unless the charset is set, wikidata interprets a POST as ISO-8859-??? (c.f. POST as form).
    // https://github.com/blazegraph/database/issues/224
    // Only applies to SendMode.asPost of a SPARQL query.
    public static final String QUERY_MIME_TYPE = WebContent.contentTypeSPARQLQuery+";charset="+WebContent.charsetUTF8;
    private final Query query;
    private final String queryString;
    private final String service;
    private final Context context;
    private final ObjectMapper objectMapper;
    private final AgentConfig agentConfig;

    // Params
    private final Params params;

    private final QuerySendMode sendMode;
    private final int urlLimit;

    // Protocol
    private final List<String> defaultGraphURIs;
    private final List<String> namedGraphURIs;

    private boolean closed = false;

    // Timeout of query execution.
    private final long readTimeout;
    private final TimeUnit readTimeoutUnit;

    // Content Types: these list the standard formats and also include */*.
    private final String selectAcceptheader    = WebContent.defaultSparqlResultsHeader;
    private final String askAcceptHeader       = WebContent.defaultSparqlAskHeader;
    private final String datasetAcceptHeader   = WebContent.defaultDatasetAcceptHeader;

    // If this is non-null, it overrides the use of any Content-Type above.
    private String appProvidedAcceptHeader;

    // Releasing HTTP input streams is important. We remember this for SELECT result
    // set streaming, and will close it when the execution is closed
    private InputStream retainedConnection = null;

    private final HttpClient httpClient;
    private Map<String, String> httpHeaders;

    public QueryExecutor(String serviceURL, Query query, String queryString, int urlLimit,
                         HttpClient httpClient, Map<String, String> httpHeaders, Params params, Context context,
                         List<String> defaultGraphURIs, List<String> namedGraphURIs,
                         QuerySendMode sendMode, String explicitAcceptHeader,
                         long timeout, TimeUnit timeoutUnit, ObjectMapper objectMapper, AgentConfig agentConfig) {
        this.context = context;
        this.service = serviceURL;
        this.query = query;
        this.queryString = queryString;
        this.urlLimit = urlLimit;
        this.httpHeaders = httpHeaders;
        this.defaultGraphURIs = defaultGraphURIs;
        this.namedGraphURIs = namedGraphURIs;
        this.sendMode = Objects.requireNonNull(sendMode);
        this.appProvidedAcceptHeader = explicitAcceptHeader;
        // Important - handled as special case because the defaults vary by query type.
        if ( httpHeaders.containsKey(HttpNames.hAccept) ) {
            if ( this.appProvidedAcceptHeader != null )
                this.appProvidedAcceptHeader = httpHeaders.get(HttpNames.hAccept);
            this.httpHeaders.remove(HttpNames.hAccept);
        }
        this.httpHeaders = httpHeaders;
        this.params = params;
        this.readTimeout = timeout;
        this.readTimeoutUnit = timeoutUnit;
        this.httpClient = HttpLib.dft(httpClient, HttpEnv.getDftHttpClient());
        this.objectMapper=objectMapper;
        this.agentConfig=agentConfig;
    }

    @Override
    public RowSet select() {
        checkNotClosed();
        check(QueryType.SELECT);
        return execRowSet();
    }

    private RowSet execRowSet() {
        // Use the explicitly given header or the default selectAcceptheader
        String thisAcceptHeader = dft(appProvidedAcceptHeader, selectAcceptheader);

        Map.Entry<String,InputStream> response = performQuery(thisAcceptHeader);
        InputStream in = response.getValue();
        // Don't assume the endpoint actually gives back the content type we asked for
        String actualContentType = response.getKey();

        // More reliable to use the format-defined charsets e.g. JSON -> UTF-8
        actualContentType = removeCharset(actualContentType);

        retainedConnection = in; // This will be closed on close()

        // Map to lang, with pragmatic alternatives.
        Lang lang = WebContent.contentTypeToLangResultSet(actualContentType);
        if ( lang == null )
            throw new QueryException("Endpoint returned Content-Type: " + actualContentType + " which is not recognized for SELECT queries");
        if ( !ResultSetReaderRegistry.isRegistered(lang) )
            throw new QueryException("Endpoint returned Content-Type: " + actualContentType + " which is not supported for SELECT queries");
        // This returns a streaming result set for some formats.
        // Do not close the InputStream at this point.
        ResultSet result = ResultSetMgr.read(in, lang);
        return RowSet.adapt(result);
    }

    @Override
    public boolean ask() {
        checkNotClosed();
        check(QueryType.ASK);
        String thisAcceptHeader = dft(appProvidedAcceptHeader, askAcceptHeader);
        Map.Entry<String,InputStream> response = performQuery(thisAcceptHeader);
        InputStream in = response.getValue();

        String actualContentType = response.getKey();
        actualContentType = removeCharset(actualContentType);

        Lang lang = RDFLanguages.contentTypeToLang(actualContentType);
        if ( lang == null ) {
            // Any specials :
            // application/xml for application/sparql-results+xml
            // application/json for application/sparql-results+json
            if (actualContentType.equals(WebContent.contentTypeXML))
                lang = ResultSetLang.RS_XML;
            else if ( actualContentType.equals(WebContent.contentTypeJSON))
                lang = ResultSetLang.RS_JSON;
        }
        if ( lang == null )
            throw new QueryException("Endpoint returned Content-Type: " + actualContentType + " which is not supported for ASK queries");
        boolean result = ResultSetMgr.readBoolean(in, lang);
        finish(in);
        return result;
    }

    private String removeCharset(String contentType) {
        int idx = contentType.indexOf(';');
        if ( idx < 0 )
            return contentType;
        return contentType.substring(0,idx);
    }

    @Override
    public Graph construct(Graph graph) {
        checkNotClosed();
        check(QueryType.CONSTRUCT);
        return execGraph(graph);
    }

    @Override
    public Iterator<Triple> constructTriples() {
        checkNotClosed();
        check(QueryType.CONSTRUCT);
        return execTriples();
    }

    @Override
    public Iterator<Quad> constructQuads(){
        checkNotClosed();
        return execQuads();
    }

    @Override
    public DatasetGraph constructDataset(){
        checkNotClosed();
        return constructDataset(DatasetGraphFactory.createTxnMem());
    }

    @Override
    public DatasetGraph constructDataset(DatasetGraph dataset){
        checkNotClosed();
        check(QueryType.CONSTRUCT);
        return execDataset(dataset);
    }

    @Override
    public Graph describe(Graph graph) {
        checkNotClosed();
        check(QueryType.DESCRIBE);
        return execGraph(graph);
    }

    @Override
    public Iterator<Triple> describeTriples() {
        checkNotClosed();
        return execTriples();
    }

    private Graph execGraph(Graph graph) {
        Pair<InputStream, Lang> p = execRdfWorker(WebContent.defaultRDFAcceptHeader);
        InputStream in = p.getLeft();
        Lang lang = p.getRight();
        try {
            RDFDataMgr.read(graph, in, lang);
        } catch (RiotException ex) {
            HttpLib.finish(in);
            throw ex;
        }
        return graph;
    }

    private DatasetGraph execDataset(DatasetGraph dataset) {
        Pair<InputStream, Lang> p = execRdfWorker(datasetAcceptHeader);
        InputStream in = p.getLeft();
        Lang lang = p.getRight();
        try {
            RDFDataMgr.read(dataset, in, lang);
        } catch (RiotException ex) {
            finish(in);
            throw ex;
        }
        return dataset;
    }

    @SuppressWarnings("deprecation")
    private Iterator<Triple> execTriples() {
        Pair<InputStream, Lang> p = execRdfWorker(WebContent.defaultGraphAcceptHeader);
        InputStream input = p.getLeft();
        Lang lang = p.getRight();
        // Base URI?
        // Unless N-Triples, this creates a thread.
        Iterator<Triple> iter = RDFDataMgr.createIteratorTriples(input, lang, null);
        return Iter.onCloseIO(iter, input);
    }

    @SuppressWarnings("deprecation")
    private Iterator<Quad> execQuads() {
        checkNotClosed();
        Pair<InputStream, Lang> p = execRdfWorker(datasetAcceptHeader);
        InputStream input = p.getLeft();
        Lang lang = p.getRight();
        // Unless N-Quads, this creates a thread.
        Iterator<Quad> iter = RDFDataMgr.createIteratorQuads(input, lang, null);
        return Iter.onCloseIO(iter, input);
    }

    // Any RDF data back (CONSTRUCT, DESCRIBE, QUADS)
    // ifNoContentType - some wild guess at the content type.
    private Pair<InputStream, Lang> execRdfWorker(String contentType) {
        checkNotClosed();
        String thisAcceptHeader = dft(appProvidedAcceptHeader, contentType);
        Map.Entry<String,InputStream> response = performQuery(thisAcceptHeader);
        InputStream in = response.getValue();

        // Don't assume the endpoint actually gives back the content type we asked for
        String actualContentType = response.getKey();
        actualContentType = removeCharset(actualContentType);

        Lang lang = RDFLanguages.contentTypeToLang(actualContentType);
        if ( ! RDFLanguages.isQuads(lang) && ! RDFLanguages.isTriples(lang) )
            throw new QueryException("Endpoint returned Content Type: "
                    + actualContentType
                    + " which is not a valid RDF syntax");
        return Pair.create(in, lang);
    }

    @Override
    public JsonArray execJson() {
        checkNotClosed();
        check(QueryType.CONSTRUCT_JSON);
        String thisAcceptHeader = dft(appProvidedAcceptHeader, WebContent.contentTypeJSON);
        Map.Entry<String,InputStream> response = performQuery(thisAcceptHeader);
        InputStream in = response.getValue();
        try {
            return JSON.parseAny(in).getAsArray();
        } finally { finish(in); }
    }

    @Override
    public Iterator<JsonObject> execJsonItems() {
        JsonArray array = execJson().getAsArray();
        List<JsonObject> x = new ArrayList<>(array.size());
        array.forEach(elt->{
            if ( ! elt.isObject())
                throw new QueryExecException("Item in an array from a JSON query isn't an object");
            x.add(elt.getAsObject());
        });
        return x.iterator();
    }

    private void checkNotClosed() {
        if ( closed )
            throw new QueryExecException("HTTP QueryExecHTTP has been closed");
    }

    private void check(QueryType queryType) {
        if ( query == null ) {
            // Pass through the queryString.
            return;
        }
        if ( query.queryType() != queryType )
            throw new QueryExecException("Not the right form of query. Expected "+queryType+" but got "+query.queryType());
    }

    @Override
    public Context getContext() {
        return context;
    }

    @Override
    public DatasetGraph getDataset() {
        return null;
    }

    // This may be null - if we were created form a query string,
    // we don't guarantee to parse it so we let through non-SPARQL
    // extensions to the far end.
    @Override
    public Query getQuery() {
        if ( query != null )
            return query;
        if ( queryString != null ) {
            // Object not created with a Query object, may be because there is foreign
            // syntax in the query or may be because the query string was available and the app
            // didn't want the overhead of parsing it every time.
            // Try to parse it else return null;
            try {
                return QueryFactory.create(queryString, Syntax.syntaxARQ); }
            catch (QueryParseException ex) {
                return null;
            }
        }
        return null;
    }

    /**
     * Return the query string. If this was supplied as a string,
     * there is no guarantee this is legal SPARQL syntax.
     */
    @Override
    public String getQueryString() {
        return queryString;
    }

    /**
     * Make a query over HTTP.
     * The response is returned after status code processing so the caller can assume the
     * query execution was successful and return 200.
     * Use {@link HttpLib#getInputStream} to access the body.
     */
    private Map.Entry<String,InputStream> performQuery(String reqAcceptHeader) {
        if (closed)
            throw new ARQException("HTTP execution already closed");

        //  SERVICE specials.

        Params thisParams = Params.create(params);

        if ( defaultGraphURIs != null ) {
            for ( String dft : defaultGraphURIs )
                thisParams.add( HttpParams.pDefaultGraph, dft );
        }
        if ( namedGraphURIs != null ) {
            for ( String name : namedGraphURIs )
                thisParams.add( HttpParams.pNamedGraph, name );
        }

        HttpLib.modifyByService(service, context, thisParams, httpHeaders);

        HttpRequest request = makeRequest(thisParams, reqAcceptHeader);

        return executeQuery(request);
    }

    private HttpRequest makeRequest(Params thisParams, String reqAcceptHeader) {
        QuerySendMode actualSendMode = actualSendMode();
        HttpRequest.Builder requestBuilder;
        switch(actualSendMode) {
            case asGetAlways :
                requestBuilder = executeQueryGet(thisParams, reqAcceptHeader);
                break;
            case asPostForm :
                requestBuilder = executeQueryPostForm(thisParams, reqAcceptHeader);
                break;
            case asPost :
                requestBuilder = executeQueryPostBody(thisParams, reqAcceptHeader);
                break;
            default :
                // Should not happen!
                throw new InternalErrorException("Invalid value for 'actualSendMode' "+actualSendMode);
        }
        return requestBuilder.build();
    }

    private Map.Entry<String,InputStream>  executeQuery(HttpRequest request) {
        try {
            HttpResponse<InputStream> response = execute(httpClient, request);
            String contentType=responseHeader(response,HttpNames.hContentType);
            InputStream inputStream=new BufferedInputStream(HttpLib.getInputStream(response));
            inputStream.mark(2);
            byte[] boundaryBytes=new byte[2];
            int all=inputStream.read(boundaryBytes);
            String boundary=new String(boundaryBytes);
            inputStream.reset();
            Optional<String> warnings=response.headers().firstValue("cx_warnings");
            if(all==boundaryBytes.length && contentType.startsWith("multipart/form-data") || "--".equals(boundary)) {
                int boundaryIndex=contentType.indexOf(";boundary=");
                if(boundaryIndex>=0) {
                    boundary=boundary+contentType.substring(boundaryIndex+10);
                }
                StringBuilder nextPart=null;
                String embeddedContentType=null;
                BufferedReader reader=new BufferedReader(new InputStreamReader(inputStream));
                for(String line = reader.readLine(); line!=null; line=reader.readLine()) {
                    if(boundary.equals(line)) {
                        if(nextPart!=null && embeddedContentType!=null) {
                            if(embeddedContentType.equals("application/cx-warnings+json")) {
                                warnings=Optional.of(nextPart.toString());
                            } else {
                                inputStream=new ByteArrayInputStream(nextPart.toString().getBytes());
                                contentType=embeddedContentType;
                            }
                        }
                        nextPart=new StringBuilder();
                        String contentLine=reader.readLine();
                        if(contentLine!=null && contentLine.startsWith("Content-Type: ")) {
                            embeddedContentType=contentLine.substring(14);
                        } else {
                            embeddedContentType=null;
                        }
                    } else if(nextPart!=null) {
                        nextPart.append(line);
                        nextPart.append("\n");
                    }
                }
                if(nextPart!=null && embeddedContentType!=null) {
                    if(embeddedContentType.equals("application/cx-warnings+json")) {
                        warnings=Optional.of(nextPart.toString());
                    } else {
                        inputStream=new ByteArrayInputStream(nextPart.toString().getBytes());
                        contentType=embeddedContentType;
                    }
                }
            }
            if(warnings.isPresent()) {
                List<CatenaxWarning> yetWarnings=CatenaxWarning.getOrSetWarnings(context);
                try {
                    List<CatenaxWarning> newWarnings=objectMapper.readValue(warnings.get(), new TypeReference<>(){});
                    yetWarnings.addAll(newWarnings);
                } catch (JsonProcessingException e) {
                    CatenaxWarning newWarning=new CatenaxWarning();
                    newWarning.setSourceTenant(agentConfig.getControlPlaneIdsUrl());
                    newWarning.setSourceAsset(agentConfig.getDefaultAsset());
                    newWarning.setTargetTenant(request.uri().toString());
                    newWarning.setTargetAsset(request.uri().toString());
                    newWarning.setContext(String.valueOf(context.hashCode()));
                    newWarning.setProblem("Could not deserialize embedded warnings.");
                    yetWarnings.add(newWarning);
                }
            }
            int httpStatusCode = response.statusCode();
            if(httpStatusCode<200 || httpStatusCode>299) {
                String msg= IOUtils.readInputStreamToString(inputStream);
                throw new QueryExceptionHTTP(httpStatusCode,msg);
            }
            return new AbstractMap.SimpleEntry<>(contentType,inputStream);
        } catch (IOException e) {
            throw new QueryException(e);
        }
    }

    private QuerySendMode actualSendMode() {
        switch(sendMode) {
            case asGetAlways :
            case asPostForm :
            case asPost :
                return sendMode;
            case asGetWithLimitBody :
            case asGetWithLimitForm :
                break;
        }

        // Other params (query= has not been added at this point)
        int paramsLength = params.httpString().length();
        int qEncodedLength = calcEncodeStringLength(queryString);

        // URL Length, including service (for safety)
        int length = service.length()
                + /* ?query= */        1 + HttpParams.pQuery.length()
                + /* encoded query */  qEncodedLength
                + /* &other params*/   1 + paramsLength;
        if ( length <= urlLimit )
            return QuerySendMode.asGetAlways;
        return (sendMode==QuerySendMode.asGetWithLimitBody) ? QuerySendMode.asPost : QuerySendMode.asPostForm;
    }

    private static int calcEncodeStringLength(String str) {
        // Could approximate by counting non-queryString character and adding that *2 to the length of the string.
        String qs = HttpLib.urlEncodeQueryString(str);
        return qs.length();
    }

    private HttpRequest.Builder executeQueryGet(Params thisParams, String acceptHeader) {
        thisParams.add(HttpParams.pQuery, queryString);
        String requestURL = requestURL(service, thisParams.httpString());
        HttpRequest.Builder builder = HttpLib.requestBuilder(requestURL, httpHeaders, readTimeout, readTimeoutUnit);
        acceptHeader(builder, acceptHeader);
        return builder.GET();
    }

    private HttpRequest.Builder executeQueryPostForm(Params thisParams, String acceptHeader) {
        thisParams.add(HttpParams.pQuery, queryString);
        String formBody = thisParams.httpString();
        HttpRequest.Builder builder = HttpLib.requestBuilder(service, httpHeaders, readTimeout, readTimeoutUnit);
        acceptHeader(builder, acceptHeader);
        // Use an HTML form.
        contentTypeHeader(builder, WebContent.contentTypeHTMLForm);
        // Already UTF-8 encoded to ASCII.
        return builder.POST(BodyPublishers.ofString(formBody, StandardCharsets.US_ASCII));
    }

    // Use SPARQL query body and MIME type.
    private HttpRequest.Builder executeQueryPostBody(Params thisParams, String acceptHeader) {
        // Use thisParams (for default-graph-uri etc)
        String requestURL = requestURL(service, thisParams.httpString());
        HttpRequest.Builder builder = HttpLib.requestBuilder(requestURL, httpHeaders, readTimeout, readTimeoutUnit);
        contentTypeHeader(builder, QUERY_MIME_TYPE);
        acceptHeader(builder, acceptHeader);
        return builder.POST(BodyPublishers.ofString(queryString));
    }

    @Override
    public void abort() {
        try {
            close();
        } catch (Exception ex) {
            Log.warn(this, "Error during abort", ex);
        }
    }

    @Override
    public void close() {
        closed = true;
        if (retainedConnection != null) {
            try {
                // This call may take a long time if the response has not been consumed
                // as HTTP client will consume the remaining response so it can re-use the
                // connection. If we're closing when we're not at the end of the stream then
                // issue a warning to the logs
                if (retainedConnection.read() != -1)
                    Log.warn(this, "HTTP response not fully consumed, if HTTP Client is reusing connections (its default behaviour) then it will consume the remaining response data which may take a long time and cause this application to become unresponsive");
                retainedConnection.close();
            } catch (RuntimeIOException | java.io.IOException e) {
                // If we are closing early and the underlying stream is chunk encoded
                // the close() can result in a IOException. TypedInputStream catches
                // and re-wraps that and we want to suppress both forms.
            } finally {
                retainedConnection = null;
            }
        }
    }

    @Override
    public boolean isClosed() { return closed; }
}