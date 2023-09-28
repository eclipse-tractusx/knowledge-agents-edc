// Copyright (c) 2022,2023 Contributors to the Eclipse Foundation
//
// See the NOTICE file(s) distributed with this work for additional
// information regarding copyright ownership.
//
// This program and the accompanying materials are made available under the
// terms of the Apache License, Version 2.0 which is available at
// https://www.apache.org/licenses/LICENSE-2.0.
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
// WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
// License for the specific language governing permissions and limitations
// under the License.
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.tractusx.agents.edc.sparql;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.InternalServerErrorException;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.http.HttpStatus;
import org.apache.jena.atlas.lib.Pair;
import org.apache.jena.fuseki.Fuseki;
import org.apache.jena.fuseki.server.DataAccessPointRegistry;
import org.apache.jena.fuseki.server.OperationRegistry;
import org.apache.jena.fuseki.servlets.ActionErrorException;
import org.apache.jena.fuseki.servlets.HttpAction;
import org.apache.jena.fuseki.servlets.SPARQL_QueryGeneral;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecException;
import org.apache.jena.sparql.ARQConstants;
import org.apache.jena.sparql.algebra.optimize.RewriteFactory;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.engine.http.QueryExceptionHTTP;
import org.apache.jena.sparql.service.ServiceExecutorRegistry;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.tractusx.agents.edc.AgentConfig;
import org.eclipse.tractusx.agents.edc.MonitorWrapper;
import org.eclipse.tractusx.agents.edc.Tuple;
import org.eclipse.tractusx.agents.edc.TupleSet;
import org.eclipse.tractusx.agents.edc.http.AgentHttpAction;
import org.eclipse.tractusx.agents.edc.http.HttpServletContextAdapter;
import org.eclipse.tractusx.agents.edc.http.HttpServletRequestAdapter;
import org.eclipse.tractusx.agents.edc.http.HttpServletResponseAdapter;
import org.eclipse.tractusx.agents.edc.http.HttpUtils;
import org.eclipse.tractusx.agents.edc.http.JakartaAdapter;
import org.eclipse.tractusx.agents.edc.http.transfer.AgentSourceHttpParamsDecorator;
import org.eclipse.tractusx.agents.edc.rdf.RdfStore;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * dedicated SparQL query processor which is skill-enabled and open for edc-based services:
 * Execute predefined queries and parameterize the queries with an additional layer
 * of URL parameterization.
 */
public class SparqlQueryProcessor extends SPARQL_QueryGeneral.SPARQL_QueryProc {

    /**
     * other services
     */
    protected final Monitor monitor;
    protected final ServiceExecutorRegistry registry;
    protected final AgentConfig config;
    protected final ObjectMapper objectMapper;

    /**
     * state
     */
    protected final OperationRegistry operationRegistry = OperationRegistry.createEmpty();
    protected final DataAccessPointRegistry dataAccessPointRegistry = new DataAccessPointRegistry();
    protected final RewriteFactory optimizerFactory = new OptimizerFactory();

    // map EDC monitor to SLF4J (better than the builtin MonitorProvider)
    private final MonitorWrapper monitorWrapper;
    // some state to set when interacting with Fuseki
    protected final RdfStore rdfStore;
    private long count = -1;

    public static final String UNSET_BASE = "http://server/unset-base/";

    /**
     * create a new sparql processor
     *
     * @param registry service execution registry
     * @param monitor  EDC logging
     */
    public SparqlQueryProcessor(ServiceExecutorRegistry registry, Monitor monitor, AgentConfig config, RdfStore rdfStore, TypeManager typeManager) {
        this.monitor = monitor;
        this.registry = registry;
        this.config = config;
        this.monitorWrapper = new MonitorWrapper(getClass().getName(), monitor);
        this.rdfStore = rdfStore;
        this.objectMapper = typeManager.getMapper();
        dataAccessPointRegistry.register(rdfStore.getDataAccessPoint());
    }

    /**
     * access
     *
     * @return the operation registry
     */
    public OperationRegistry getOperationRegistry() {
        return operationRegistry;
    }

    /**
     * access
     *
     * @return the data access point registry
     */
    public DataAccessPointRegistry getDataAccessPointRegistry() {
        return dataAccessPointRegistry;
    }

    /**
     * wraps a response to a previous servlet API
     *
     * @param jakartaResponse new servlet object
     * @return wrapped/adapted response
     */
    public javax.servlet.http.HttpServletResponse getJavaxResponse(HttpServletResponse jakartaResponse) {
        return JakartaAdapter.javaxify(jakartaResponse, javax.servlet.http.HttpServletResponse.class, monitor);
    }

    /**
     * wraps a request to a previous servlet API
     *
     * @param jakartaRequest new servlet object
     * @return wrapped/adapted request
     */
    public javax.servlet.http.HttpServletRequest getJavaxRequest(HttpServletRequest jakartaRequest) {
        return JakartaAdapter.javaxify(jakartaRequest, javax.servlet.http.HttpServletRequest.class, monitor);
    }

    /**
     * execute sparql based on the given request and response
     *
     * @param request  jakarta request
     * @param response jakarta response
     * @param skill    skill ref
     * @param graph    graph ref
     */
    public void execute(HttpServletRequest request, HttpServletResponse response, String skill, String graph) {
        request.getServletContext().setAttribute(Fuseki.attrVerbose, config.isSparqlVerbose());
        request.getServletContext().setAttribute(Fuseki.attrOperationRegistry, operationRegistry);
        request.getServletContext().setAttribute(Fuseki.attrNameRegistry, dataAccessPointRegistry);
        AgentHttpAction action = new AgentHttpAction(++count, monitorWrapper, getJavaxRequest(request), getJavaxResponse(response), skill, graph);
        // Should we check whether this already has been done? the context should be quite static
        action.setRequest(rdfStore.getDataAccessPoint(), rdfStore.getDataService());
        ServiceExecutorRegistry.set(action.getContext(), registry);
        action.getContext().set(ARQConstants.sysOptimizerFactory, optimizerFactory);
        List<CatenaxWarning> previous = CatenaxWarning.getWarnings(action.getContext());
        CatenaxWarning.setWarnings(action.getContext(), null);
        try {
            executeAction(action);
            List<CatenaxWarning> newWarnings = CatenaxWarning.getWarnings(action.getContext());
            if (newWarnings != null) {
                response.addHeader("cx_warnings", objectMapper.writeValueAsString(newWarnings));
                response.addHeader("Access-Control-Expose-Headers", "cx_warnings, content-length, content-type");
                if (response.getStatus() == 200) {
                    response.setStatus(203);
                }
            }
        } catch (ActionErrorException e) {
            throw new BadRequestException(e.getMessage(), e.getCause());
        } catch (QueryExecException | JsonProcessingException e) {
            throw new InternalServerErrorException(e.getMessage(), e.getCause());
        } finally {
            CatenaxWarning.setWarnings(action.getContext(), previous);
        }
    }

    /**
     * execute the given action. Circumvents
     * too strict SPARQL requirements in favor
     * to KA-MATCH semantics.
     *
     * @param action a jena http action
     */
    protected void executeAction(AgentHttpAction action) {
        if (action.getRequestMethod().equals("GET")) {
            this.executeWithParameter(action);
        } else {
            this.executeBody(action);
        }
    }

    /**
     * execute sparql based on the given internal okhttp request and response
     *
     * @param request          ok request
     * @param skill            skill ref
     * @param graph            graph ref
     * @param targetProperties a set of address properties of the asset to invoke
     * @return simulated ok response
     */
    public Response execute(Request request, String skill, String graph, Map<String, String> targetProperties) {

        // wrap jakarta into java.servlet
        HttpServletContextAdapter contextAdapter = new HttpServletContextAdapter(request);
        HttpServletRequestAdapter requestAdapter = new HttpServletRequestAdapter(request, contextAdapter);
        HttpServletResponseAdapter responseAdapter = new HttpServletResponseAdapter(request);
        contextAdapter.setAttribute(Fuseki.attrVerbose, config.isSparqlVerbose());
        contextAdapter.setAttribute(Fuseki.attrOperationRegistry, operationRegistry);
        contextAdapter.setAttribute(Fuseki.attrNameRegistry, dataAccessPointRegistry);

        // build and populate a SPARQL action from the wrappers
        AgentHttpAction action = new AgentHttpAction(++count, monitorWrapper, requestAdapter, responseAdapter, skill, graph);
        action.setRequest(rdfStore.getDataAccessPoint(), rdfStore.getDataService());
        ServiceExecutorRegistry.set(action.getContext(), registry);
        action.getContext().set(DataspaceServiceExecutor.TARGET_URL_SYMBOL, request.header(DataspaceServiceExecutor.TARGET_URL_SYMBOL.getSymbol()));
        action.getContext().set(DataspaceServiceExecutor.AUTH_KEY_SYMBOL, targetProperties.getOrDefault(DataspaceServiceExecutor.AUTH_KEY_SYMBOL.getSymbol(), null));
        action.getContext().set(DataspaceServiceExecutor.AUTH_CODE_SYMBOL, targetProperties.getOrDefault(DataspaceServiceExecutor.AUTH_CODE_SYMBOL.getSymbol(), null));
        action.getContext().set(ARQConstants.sysOptimizerFactory, optimizerFactory);
        if (targetProperties.containsKey(DataspaceServiceExecutor.ALLOW_SYMBOL.getSymbol())) {
            action.getContext().set(DataspaceServiceExecutor.ALLOW_SYMBOL, Pattern.compile(targetProperties.get(DataspaceServiceExecutor.ALLOW_SYMBOL.getSymbol())));
        } else {
            action.getContext().set(DataspaceServiceExecutor.ALLOW_SYMBOL, config.getServiceAssetAllowPattern());
        }
        if (targetProperties.containsKey(DataspaceServiceExecutor.DENY_SYMBOL.getSymbol())) {
            action.getContext().set(DataspaceServiceExecutor.DENY_SYMBOL, Pattern.compile(targetProperties.get(DataspaceServiceExecutor.DENY_SYMBOL.getSymbol())));
        } else {
            action.getContext().set(DataspaceServiceExecutor.DENY_SYMBOL, config.getServiceAssetDenyPattern());
        }
        if (graph != null) {
            action.getContext().set(DataspaceServiceExecutor.ASSET_SYMBOL, graph);
        }
        List<CatenaxWarning> previous = CatenaxWarning.getWarnings(action.getContext());
        CatenaxWarning.setWarnings(action.getContext(), null);

        // and finally execute the SPARQL action
        try {
            executeAction(action);
            List<CatenaxWarning> newWarnings = CatenaxWarning.getWarnings(action.getContext());
            if (newWarnings != null) {
                responseAdapter.addHeader("cx_warnings", objectMapper.writeValueAsString(newWarnings));
                responseAdapter.addHeader("Access-Control-Expose-Headers", "cx_warnings, content-length, content-type");
            }
            if (responseAdapter.getStatus() == 200) {
                responseAdapter.setStatus(203);
            }
        } catch (ActionErrorException e) {
            responseAdapter.setStatus(HttpStatus.SC_BAD_REQUEST, e.getMessage());
        } catch (QueryExecException | JsonProcessingException | QueryExceptionHTTP e) {
            responseAdapter.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        } finally {
            CatenaxWarning.setWarnings(action.getContext(), previous);
        }
        return responseAdapter.toResponse();
    }

    /**
     * execute GET-style with possibility of asset=local skill
     *
     * @param action typically a GET request
     */
    @Override
    protected void executeWithParameter(HttpAction action) {
        String queryString = ((AgentHttpAction) action).getSkill();
        if (queryString == null) {
            super.executeWithParameter(action);
        } else {
            execute(queryString, action);
        }
    }

    /**
     * execute POST-style with possiblity of asset=local skill
     *
     * @param action typically a POST request
     */
    @Override
    protected void executeBody(HttpAction action) {
        String queryString = ((AgentHttpAction) action).getSkill();
        if (queryString == null) {
            super.executeBody(action);
        } else {
            execute(queryString, action);
        }
    }

    /**
     * general (URL-parameterized) query execution
     *
     * @param queryString the resolved query
     * @param action      the http action containing the parameters
     *                    TODO error handling
     */
    @Override
    protected void execute(String queryString, HttpAction action) {
        // make sure the query param is decoded (which Fuseki sometimes forgets)
        queryString = HttpUtils.urlDecodeParameter(queryString);
        // support for the special www-forms form
        if (action.getRequestContentType() != null && action.getRequestContentType().contains("application/x-www-form-urlencoded")) {
            Map<String, List<String>> parts = AgentSourceHttpParamsDecorator.parseParams(queryString);
            Optional<String> query = parts.getOrDefault("query", List.of()).stream().findFirst();
            if (query.isEmpty()) {
                action.getResponse().setStatus(HttpStatus.SC_BAD_REQUEST);
                return;
            } else {
                queryString = HttpUtils.urlDecodeParameter(query.get());
            }
        }
        TupleSet ts = ((AgentHttpAction) action).getInputBindings();
        Pattern tuplePattern = Pattern.compile("\\([^()]*\\)");
        Pattern variablePattern = Pattern.compile("@(?<name>[a-zA-Z0-9]+)");
        Matcher tupleMatcher = tuplePattern.matcher(queryString);
        StringBuilder replaceQuery = new StringBuilder();
        int lastStart = 0;
        while (tupleMatcher.find()) {
            replaceQuery.append(queryString.substring(lastStart, tupleMatcher.start()));
            String otuple = tupleMatcher.group(0);
            Matcher variableMatcher = variablePattern.matcher(otuple);
            List<String> variables = new java.util.ArrayList<>();
            while (variableMatcher.find()) {
                variables.add(variableMatcher.group("name"));
            }
            if (variables.size() > 0) {
                try {
                    boolean isFirst = true;
                    Collection<Tuple> tuples = ts.getTuples(variables.toArray(new String[0]));
                    for (Tuple rtuple : tuples) {
                        if (isFirst) {
                            isFirst = false;
                        } else {
                            replaceQuery.append(" ");
                        }
                        String newTuple = otuple;
                        for (String key : rtuple.getVariables()) {
                            newTuple = newTuple.replace("@" + key, rtuple.get(key));
                        }
                        replaceQuery.append(newTuple);
                    }
                } catch (Exception e) {
                    System.err.println(e.getMessage());
                    action.getResponse().setStatus(HttpStatus.SC_BAD_REQUEST);
                    return;
                }
            } else {
                replaceQuery.append(otuple);
            }
            lastStart = tupleMatcher.end();
        }
        replaceQuery.append(queryString.substring(lastStart));

        queryString = replaceQuery.toString();
        Matcher variableMatcher = variablePattern.matcher(queryString);
        List<String> variables = new java.util.ArrayList<>();
        while (variableMatcher.find()) {
            variables.add(variableMatcher.group("name"));
        }
        try {
            Collection<Tuple> tuples = ts.getTuples(variables.toArray(new String[0]));
            if (tuples.size() == 0 && variables.size() > 0) {
                throw new BadRequestException(String.format("Error: Got variables %s on top-level but no bindings.", Arrays.toString(variables.toArray())));
            } else if (tuples.size() > 1) {
                System.err.println(String.format("Warning: Got %s tuples for top-level bindings of variables %s. Using only the first one.", tuples.size(), Arrays.toString(variables.toArray())));
            }
            if (tuples.size() > 0) {
                Tuple rtuple = tuples.iterator().next();
                for (String key : rtuple.getVariables()) {
                    queryString = queryString.replace("@" + key, rtuple.get(key));
                }
            }
        } catch (Exception e) {
            throw new BadRequestException(String.format("Error: Could not bind variables"), e);
        }
        if (action.getContext().isDefined(DataspaceServiceExecutor.ASSET_SYMBOL)) {
            String targetUrl = action.getContext().get(DataspaceServiceExecutor.TARGET_URL_SYMBOL);
            String asset = action.getContext().get(DataspaceServiceExecutor.ASSET_SYMBOL);
            asset = asset.replace("?", "\\?");
            String graphPattern = String.format("GRAPH\\s*\\<?(%s)?%s\\>?", UNSET_BASE, asset);
            Matcher graphMatcher = Pattern.compile(graphPattern).matcher(queryString);
            replaceQuery = new StringBuilder();
            lastStart = 0;
            while (graphMatcher.find()) {
                replaceQuery.append(queryString.substring(lastStart, graphMatcher.start() - 1));
                replaceQuery.append(String.format("SERVICE <%s>", targetUrl));
                lastStart = graphMatcher.end();
            }
            replaceQuery.append(queryString.substring(lastStart));
            queryString = replaceQuery.toString();
        }
        super.execute(queryString, action);
    }

    /**
     * deal with predefined assets=local graphs
     */
    @Override
    protected Pair<DatasetGraph, Query> decideDataset(HttpAction action, Query query, String queryStringLog) {
        // These will have been taken care of by the "getDatasetDescription"
        if (query.hasDatasetDescription()) {
            // Don't modify input.
            query = query.cloneQuery();
            query.getNamedGraphURIs().clear();
            query.getGraphURIs().clear();
        }
        return Pair.create(rdfStore.getDataSet(), query);
    }
}
