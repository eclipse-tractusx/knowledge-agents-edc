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
package org.eclipse.tractusx.agents.edc.http;

import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.spi.types.domain.edr.EndpointDataReference;
import org.eclipse.tractusx.agents.edc.AgentConfig;
import org.eclipse.tractusx.agents.edc.IAgreementController;
import org.eclipse.tractusx.agents.edc.sparql.CatenaxWarning;

import java.io.*;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A service that may delegate an incoming
 * agent http request ot another agent in the
 * dataspace
 * deals with the special case of remote/provided (textual) 
 * skills which should be executed locally nevertheless
 */
public class DelegationService implements IDelegationService {

    protected final IAgreementController agreementController;
    protected final Monitor monitor;
    protected final OkHttpClient client;
    public final static TypeReference<List<CatenaxWarning>> warningTypeReference = new TypeReference<>(){};
    protected final TypeManager typeManager;
    protected final AgentConfig config;

    /**
     * creates a new delegation service
     * @param agreementController EDC agreement helper
     * @param monitor logging facility
     * @param client outgoing http infrastructure
     */
    public DelegationService(IAgreementController agreementController, Monitor monitor, OkHttpClient client, TypeManager typeManager, AgentConfig config) {
        this.agreementController=agreementController;
        this.monitor=monitor;
        this.client=client;
        this.typeManager=typeManager;
        this.config=config;
    }

    /**
     * the actual execution is done by delegating to the Dataspace
     * @param remoteUrl remote connector
     * @param skill target skill
     * @param graph target graph
     * @return a wrapped response which indicates the runMode that the execution should be done
     */
    public DelegationResponse executeQueryRemote(String remoteUrl, String skill, String graph, HttpHeaders headers, HttpServletRequest request, HttpServletResponse response, UriInfo uri)  {
        Pattern serviceAllowPattern=config.getServiceAllowPattern();
        if(!serviceAllowPattern.matcher(remoteUrl).matches()) {
            return new DelegationResponse(HttpUtils.respond(monitor,headers, HttpStatus.SC_FORBIDDEN,String.format("Service %s does not match the allowed service pattern %s",remoteUrl,serviceAllowPattern.pattern()),null));
        }
        Pattern serviceDenyPattern=config.getServiceDenyPattern();
        if(serviceDenyPattern.matcher(remoteUrl).matches()) {
        	return new DelegationResponse( HttpUtils.respond(monitor,headers, HttpStatus.SC_FORBIDDEN,String.format("Service %s matches the denied service pattern %s",remoteUrl,serviceDenyPattern.pattern()),null));
        }
        // replace edc addresses with their real underlying protocol
        if (remoteUrl.startsWith("edc://")) {
            remoteUrl="http://"+remoteUrl.substring(6);
        } else if (remoteUrl.startsWith("edcs://")) {
            remoteUrl="https://"+remoteUrl.substring(7);
        }
        String asset = skill != null ? skill : graph;
        EndpointDataReference endpoint = agreementController.get(asset);
        if(endpoint==null) {
            try {
                endpoint=agreementController.createAgreement(remoteUrl,asset);
            } catch(WebApplicationException e) {
            	return new DelegationResponse( HttpUtils.respond(monitor,headers, e.getResponse().getStatus(),String.format("Could not get an agreement from connector %s to asset %s",remoteUrl,asset),e.getCause()));
            }
        }
        if(endpoint==null) {
        	return new DelegationResponse(HttpUtils.respond(monitor,headers, HttpStatus.SC_FORBIDDEN,String.format("Could not get an agreement from connector %s to asset %s",remoteUrl,asset),null));
        }
        if("GET".equals(request.getMethod())) {
            try {
                return sendGETRequest(endpoint, "", headers, response, uri);
            } catch(IOException e) {
            	return new DelegationResponse(HttpUtils.respond(monitor,headers, HttpStatus.SC_INTERNAL_SERVER_ERROR,String.format("Could not delegate remote GET call to connector %s asset %s",remoteUrl,asset),e));
            }
        } else if("POST".equals(request.getMethod())) {
            try {
                return sendPOSTRequest(endpoint, "", headers, request, response, uri);
            } catch(IOException e) {
            	return new DelegationResponse( HttpUtils.respond(monitor,headers, HttpStatus.SC_INTERNAL_SERVER_ERROR,String.format("Could not delegate remote POST call to connector %s asset %s",remoteUrl,asset),e));
            }
        } else {
        	return new DelegationResponse(HttpUtils.respond(monitor,headers, HttpStatus.SC_METHOD_NOT_ALLOWED,String.format("%s calls to connector %s asset %s are not allowed",request.getMethod(),remoteUrl,asset),null));
        }

    }

    /**
     * route a get request
     * @param dataReference the encoded call embedding
     * @param subUrl protocol-specific part
     * @return a wrapped response which indicates the runMode that the execution should be done
     * @throws IOException in case something strange happens
     */
    public DelegationResponse sendGETRequest(EndpointDataReference dataReference, String subUrl, HttpHeaders headers, HttpServletResponse response, UriInfo uri) throws IOException {
        var url = getUrl(dataReference.getEndpoint(), subUrl, headers, uri);

        monitor.debug(String.format("About to delegate GET %s",url));

        var requestBuilder = new okhttp3.Request.Builder()
                .url(url);

        if(dataReference.getAuthKey()!=null) {
            requestBuilder = requestBuilder.addHeader(dataReference.getAuthKey(), Objects.requireNonNull(dataReference.getAuthCode()));
        }

        var newRequest = requestBuilder.build();

		return new DelegationResponse(sendRequest(newRequest, response), Response.status(response.getStatus()).build());
    }

    /**
     * route a post request
     * @param dataReference the encoded call embedding
     * @param subUrl protocol-specific part
     * @return a wrapped response which indicates the runMode that the execution should be done
     * @throws IOException in case something strange happens
     */
    public DelegationResponse sendPOSTRequest(EndpointDataReference dataReference, String subUrl, HttpHeaders headers, HttpServletRequest request, HttpServletResponse response, UriInfo uri) throws IOException {
        var url = getUrl(dataReference.getEndpoint(), subUrl, headers, uri);

        String contentType=request.getContentType();
        okhttp3.MediaType parsedContentType=okhttp3.MediaType.parse(contentType);

        monitor.debug(String.format("About to delegate POST %s with content type %s",url,contentType));

        var requestBuilder = new okhttp3.Request.Builder()
                .url(url)
                .addHeader("Content-Type", contentType);

        if(dataReference.getAuthKey()!=null) {
            requestBuilder = requestBuilder.addHeader(dataReference.getAuthKey(), Objects.requireNonNull(dataReference.getAuthCode()));
        }

        requestBuilder.post(okhttp3.RequestBody.create(request.getInputStream().readAllBytes(),parsedContentType));

        var newRequest = requestBuilder.build();
        
		return new DelegationResponse(sendRequest(newRequest, response), Response.status(response.getStatus()).build());
    }

    protected static Pattern PARAMETER_KEY_ALLOW = Pattern.compile("^(?!asset$)[^&?=]+$");
    protected static Pattern PARAMETER_VALUE_ALLOW = Pattern.compile("^.+$");

    /**
     * computes the url to target the given data plane
     * @param connectorUrl data plane url
     * @param subUrl sub-path to use
     * @param headers containing additional info that we need to wrap into a transfer request
     * @return typed url
     */
    protected HttpUrl getUrl(String connectorUrl, String subUrl, HttpHeaders headers, UriInfo uri)  {
        var url = connectorUrl;

        // EDC public api slash problem
        if(!url.endsWith("/") && !url.contains("#")) {
            url = url + "/";
        }

        if (subUrl != null && !subUrl.isEmpty()) {
            url = url + subUrl;
        }

        HttpUrl.Builder httpBuilder = Objects.requireNonNull(okhttp3.HttpUrl.parse(url)).newBuilder();
        for (Map.Entry<String, List<String>> param : uri.getQueryParameters().entrySet()) {
            String key=param.getKey();
            if(PARAMETER_KEY_ALLOW.matcher(key).matches()) {
                for (String value : param.getValue()) {
                    if(PARAMETER_VALUE_ALLOW.matcher(value).matches()) {
                        String recodeKey = HttpUtils.urlEncodeParameter(key);
                        String recodeValue = HttpUtils.urlEncodeParameter(value);
                        httpBuilder = httpBuilder.addQueryParameter(recodeKey, recodeValue);
                    }
                }
            }
        }

        List<MediaType> mediaTypes=headers.getAcceptableMediaTypes();
        if(mediaTypes.isEmpty() || mediaTypes.stream().anyMatch(MediaType.APPLICATION_JSON_TYPE::isCompatible)) {
            httpBuilder = httpBuilder.addQueryParameter("cx_accept", HttpUtils.urlEncodeParameter("application/json"));
        } else {
            String mediaParam=mediaTypes.stream().map(MediaType::toString).collect(Collectors.joining(", "));
            mediaParam=HttpUtils.urlEncodeParameter(mediaParam);
            httpBuilder.addQueryParameter("cx_accept",mediaParam);
        }
        return httpBuilder.build();
    }

    /**
     * generic sendRequest method which extracts the result string of textual responses
     * @param request predefined request
     * @param response the final response
     * @return the text of a downloaded skill if runMode = consumer, null otherwise
     * @throws IOException in case something goes wrong
     */
    protected String sendRequest(okhttp3.Request request, HttpServletResponse response) throws IOException {
        try(var myResponse = client.newCall(request).execute()) {

            if(!myResponse.isSuccessful()) {
                monitor.warning(String.format("Data plane call was not successful: %s", myResponse.code()));
            }
            
            Optional<List<CatenaxWarning>> warnings=Optional.empty();

            var body = myResponse.body();

            if (body != null) {
                okhttp3.MediaType contentType=body.contentType();
                InputStream inputStream=new BufferedInputStream(body.byteStream());

                //
                // Analyze whether this response contains a multipart body
                // while maintaining the state of the inputstream (mark/reset approach)
                //
                inputStream.mark(2);
                byte[] boundaryBytes=new byte[2];
                String boundary="";
                if(inputStream.read(boundaryBytes)>0) {
                    boundary = new String(boundaryBytes);
                }
                inputStream.reset();
                if("--".equals(boundary)) {
                    //
                    // Multipart Case separates the actual result (last part) from the warnings (first part)
                    //
                    if(contentType!=null) {
                        int boundaryIndex;
                        boundaryIndex=contentType.toString().indexOf(";boundary=");
                        if(boundaryIndex>=0) {
                            boundary=boundary+contentType.toString().substring(boundaryIndex+10);
                        }
                    }
                    StringBuilder nextPart=null;
                    String embeddedContentType=null;
                    BufferedReader reader=new BufferedReader(new InputStreamReader(inputStream));
                    for(String line = reader.readLine(); line!=null; line=reader.readLine()) {
                        if(boundary.equals(line)) {
                            if(nextPart!=null && embeddedContentType!=null) {
                                if(embeddedContentType.equals("application/cx-warnings+json")) {
                                    List<CatenaxWarning> nextWarnings=typeManager.readValue(nextPart.toString(),warningTypeReference);
                                    if(warnings.isPresent()) {
                                        warnings.get().addAll(nextWarnings);
                                    } else {
                                        warnings=Optional.of(nextWarnings);
                                    }
                                } else {
                                    inputStream=new ByteArrayInputStream(nextPart.toString().getBytes());
                                    contentType=okhttp3.MediaType.parse(embeddedContentType);
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
                    reader.close();
                    // multipart parsing through, now look at the actual result in the last part
                    if(nextPart!=null && embeddedContentType!=null) {
                        // is it a downloaded skill text?
                    	if (embeddedContentType.equals("application/sparql-query")) {
                            // immediately return
                    		return nextPart.toString();
                    	} else if(embeddedContentType.equals("application/cx-warnings+json")) {
                            // is it a trailing warnings structure
                            List<CatenaxWarning> nextWarnings=typeManager.readValue(nextPart.toString(), warningTypeReference);
                            if(warnings.isPresent()) {
                                warnings.get().addAll(nextWarnings);
                            } else {
                                warnings=Optional.of(nextWarnings);
                            }
                        } else {
                            // it is a normal "result" that we take as the actual response
                            inputStream=new ByteArrayInputStream(nextPart.toString().getBytes());
                            contentType=okhttp3.MediaType.parse(embeddedContentType);
                        }
                    }
                }
                // if we got a simple skill text as answer (not multipart)
                if (contentType.toString().equals("application/sparql-query")) {
                    // return the skill text
                    return IOUtils.toString(inputStream, Charset.defaultCharset());
                }
                // else set the response status a
                response.setStatus(myResponse.code());
                // and the response headers (including the completed warnings
                for(String header : myResponse.headers().names()) {
                    for(String value : myResponse.headers().values(header)) {
                        if(header.equals("cx_warnings")) {
                        	List<CatenaxWarning> nextWarnings=typeManager.getMapper().readValue(value,warningTypeReference);
                        	if (nextWarnings != null) {                           
	                        	if(warnings.isPresent()) {
	                                warnings.get().addAll(nextWarnings);
	                            } else {
	                                warnings=Optional.of(nextWarnings);
	                            }
                        	}
                        } else if(!header.equalsIgnoreCase("content-length")) {
                            response.addHeader(header, value);
                        }
                    }
                }
                warnings.ifPresent(catenaxWarnings -> response.addHeader("cx_warnings", typeManager.writeValueAsString(catenaxWarnings)));
                if(contentType!=null) {
                    response.setContentType(contentType.toString());
                }
                // and finally copy the body from intermediate response to final response
                IOUtils.copy(inputStream, response.getOutputStream());
                inputStream.close();
            }
        }
        return null;
    }

}
