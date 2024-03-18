// Copyright (c) 2022,2024 Contributors to the Eclipse Foundation
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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.apache.http.HttpStatus;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.tractusx.agents.edc.AgentConfig;
import org.eclipse.tractusx.agents.edc.AgentExtension;
import org.eclipse.tractusx.agents.edc.AgreementController;
import org.eclipse.tractusx.agents.edc.SkillDistribution;
import org.eclipse.tractusx.agents.edc.SkillStore;
import org.eclipse.tractusx.agents.edc.sparql.SparqlQueryProcessor;

import java.util.Optional;
import java.util.regex.Matcher;

/**
 * The Agent Controller exposes a REST API endpoint
 * with which the EDC tenant can issue queries and execute
 * skills in interaction with local resources and the complete
 * Dataspace (the so-called Matchmaking Agent that is also hit by
 * incoming Agent transfers).
 * TODO exchange fixed memory store by configurable options
 * TODO generalize sub-protocols from SparQL
 */
@Path("/agent")
public class AgentController {

    // EDC services
    protected final Monitor monitor;
    protected final AgreementController agreementController;
    protected final AgentConfig config;
    protected final SkillStore skillStore;

    // the actual Matchmaking Agent is a Fuseki engine
    protected final SparqlQueryProcessor processor;
    protected final DelegationService delegationService;

    /**
     * creates a new agent controller
     *
     * @param monitor             logging subsystem
     * @param agreementController agreement controller for remote skill/queries
     * @param config              configuration
     * @param processor           sparql processor
     */
    public AgentController(Monitor monitor, AgreementController agreementController, AgentConfig config, SparqlQueryProcessor processor, SkillStore skillStore, DelegationService delegationService) {
        this.monitor = monitor;
        this.agreementController = agreementController;
        this.config = config;
        this.processor = processor;
        this.skillStore = skillStore;
        this.delegationService = delegationService;
    }

    /**
     * render nicely
     */
    @Override
    public String toString() {
        return super.toString() + "/agent";
    }

    /**
     * endpoint for posting a sparql query (maybe as a stored skill with a bindingset)
     *
     * @param asset can be a named graph for executing a query or a skill asset
     * @return response
     */
    @POST
    @Consumes({ "application/sparql-query", "application/sparql-results+json" })
    public Response postSparqlQuery(@QueryParam("asset") String asset,
                                    @Context HttpHeaders headers,
                                    @Context HttpServletRequest request,
                                    @Context HttpServletResponse response,
                                    @Context UriInfo uri
    ) {
        monitor.debug(String.format("Received a SparQL POST request %s for asset %s", request, asset));
        return executeQuery(asset, headers, request, response, uri);
    }

    /**
     * endpoint for posting a url-encoded form-based query
     * this is the default mode for graphdb which
     * sends the actual query language and other params together
     * with the query text in the body and expects a
     * special content disposition in the response header which
     * marks the body as a kind of file attachement.
     *
     * @param asset can be a named graph for executing a query or a skill asset
     * @return response compatible with graphdb convention
     */
    @POST
    @Consumes({ "application/x-www-form-urlencoded" })
    public Response postFormQuery(@QueryParam("asset") String asset,
                                  @Context HttpHeaders headers,
                                  @Context HttpServletRequest request,
                                  @Context HttpServletResponse response,
                                  @Context UriInfo uri) {
        monitor.debug(String.format("Received a Form-based POST request %s for asset %s", request, asset));
        Response result = executeQuery(asset, headers, request, response, uri);
        response.addHeader("Content-Disposition", "attachement; filename=query-result.srjs");
        return result;
    }

    /**
     * endpoint for posting a url-encoded form-based query
     * this is the default mode for graphdb which
     * sends the actual query language and other params together
     * with the query text in the body and expects a
     * special content disposition in the response header which
     * marks the body as a kind of file attachement.
     *
     * @param asset can be a named graph for executing a query or a skill asset
     * @return response compatible with graphdb convention
     */
    @POST
    @Path("/repositories/AGENT")
    @Consumes({ "application/x-www-form-urlencoded" })
    public Response postFormRepositoryQuery(@QueryParam("asset") String asset,
                                            @Context HttpHeaders headers,
                                            @Context HttpServletRequest request,
                                            @Context HttpServletResponse response,
                                            @Context UriInfo uri) {
        monitor.debug(String.format("Received a Form-based POST repository request %s for asset %s", request, asset));
        Response result = executeQuery(asset, headers, request, response, uri);
        response.addHeader("Content-Disposition", "attachement; filename=query-result.srjs");
        return result;
    }

    /**
     * endpoint for getting a query
     *
     * @param asset can be a named graph for executing a query or a skill asset
     * @return response
     */
    @GET
    public Response getQuery(@QueryParam("asset") String asset,
                             @Context HttpHeaders headers,
                             @Context HttpServletRequest request,
                             @Context HttpServletResponse response,
                             @Context UriInfo uri) {
        monitor.debug(String.format("Received a GET request %s for asset %s", request, asset));
        return executeQuery(asset, headers, request, response, uri);
    }

    /**
     * 2nd endpoint for getting a query
     *
     * @param asset can be a named graph for executing a query or a skill asset
     * @return response
     */
    @GET
    @Path("/repositories/AGENT")
    public Response getRepositoryQuery(@QueryParam("asset") String asset,
                                       @Context HttpHeaders headers,
                                       @Context HttpServletRequest request,
                                       @Context HttpServletResponse response,
                                       @Context UriInfo uri) {
        monitor.debug(String.format("Received a GET repository request %s for asset %s", request, asset));
        return executeQuery(asset, headers, request, response, uri);
    }

    /**
     * check import status
     *
     * @return response
     */
    @GET
    @Path("/repositories/AGENT/import/active")
    public Response getRepositoryImportQuery(
            @Context HttpServletRequest request
    ) {
        monitor.debug(String.format("Received a GET repository import active request %s", request));
        return Response.status(406, "Not Acceptable (HTTP status 406)").build();
    }

    /**
     * check size
     *
     * @return response
     */
    @GET
    @Path("/rest/repositories/AGENT/size")
    public Response getRestRepositorySizeQuery(
            @Context HttpServletRequest request
    ) {
        monitor.debug(String.format("Received a GET rest repository size request %s", request));
        return Response.ok("{\n" +
                "    \"inferred\": 70,\n" +
                "    \"total\": 70,\n" +
                "    \"explicit\": 0\n" +
                "}").type(jakarta.ws.rs.core.MediaType.APPLICATION_JSON_TYPE).build();
    }

    /**
     * check size
     *
     * @return response
     */
    @GET
    @Path("/repositories/AGENT/size")
    public Response getRepositorySizeQuery(
            @Context HttpServletRequest request
    ) {
        monitor.debug(String.format("Received a GET repository size request %s", request));
        return Response.ok("0").type(jakarta.ws.rs.core.MediaType.APPLICATION_JSON_TYPE).build();
    }

    /**
     * check import status
     *
     * @return response
     */
    @GET
    @Path("/rest/repositories/AGENT/import/active")
    public Response getRestRepositoryImportQuery(
            @Context HttpServletRequest request
    ) {
        monitor.debug(String.format("Received a GET rest repository import active request %s", request));
        return Response.ok("0").type(jakarta.ws.rs.core.MediaType.APPLICATION_JSON_TYPE).build();
    }

    /**
     * return version info for graphdb/fedx integration
     *
     * @return version string
     */
    @GET
    @Path("/rest/info/version")
    public String getVersion(
            @Context HttpServletRequest request
    ) {
        monitor.debug(String.format("Received a GET Version request %s", request));
        return "0.8.4";
    }

    /**
     * return protocol info for graphdb/fedx integration
     *
     * @return protocol string
     */
    @GET
    @Path("/protocol")
    public String getProtocol(
            @Context HttpServletRequest request
    ) {
        monitor.debug(String.format("Received a GET Protocol request %s", request));
        return "12";
    }

    /**
     * return version info for graphdb/fedx integration
     *
     * @return version string
     */
    @GET
    @Path("/rest/locations/id")
    public String getId(
            @Context HttpServletRequest request
    ) {
        monitor.debug(String.format("Received a GET Id request %s", request));
        return "Catena-X Knowledge Agent";
    }

    /**
     * return repositories for graphdb/fedx integration
     *
     * @return single repo as json
     */
    @GET
    @Path("/rest/repositories")
    public String getRestRepositories(
            @Context HttpServletRequest request,
            @Context UriInfo uri
    ) {
        monitor.debug(String.format("Received a GET Rest Repositories request %s", request));
        String url = uri.getAbsolutePath().toString();
        url = url.substring(0, url.length() - 18);
        url = HttpUtils.urlEncode(url);
        return "[\n" +
                "    {\n" +
                "        \"id\": \"AGENT\",\n" +
                "        \"title\": \"Catena-X Knowledge Agent Dataspace Endpoint\",\n" +
                "        \"uri\": \"" + url + "\",\n" +
                "        \"externalUrl\": \"" + url + "\",\n" +
                "        \"local\": false,\n" +
                "        \"type\": \"fuseki\",\n" +
                "        \"sesameType\": \"cx:AgentController\",\n" +
                "        \"location\": \"Catena-X Dev Dataspace\",\n" +
                "        \"readable\": true,\n" +
                "        \"writable\": true,\n" +
                "        \"unsupported\": false,\n" +
                "        \"state\": \"RUNNING\"\n" +
                "    }\n" +
                "]";
    }

    /**
     * return repositories for graphdb/fedx integration
     *
     * @return single repo as csv
     */
    @GET
    @Path("/repositories")
    public Response getRepositories(
            @Context HttpServletRequest request,
            @Context UriInfo uri
    ) {
        monitor.debug(String.format("Received a GET Repositories request %s", request));
        String url = uri.getAbsolutePath().toString();
        url = url.substring(0, url.length() - 13);
        url = HttpUtils.urlEncode(url);
        Response.ResponseBuilder builder = Response.ok("uri,id,title,readable,writable\n" + url + ",AGENT,Catena-X Knowledge Agent Dataspace Endpoint,true,true\n");
        builder.type("text/csv;charset=UTF-8");
        builder.header("Content-Disposition", "attachment; filename=repositories.csv");
        return builder.build();
    }

    /**
     * return repositories for graphdb/fedx integration
     *
     * @return single repo as csv
     */
    @GET
    @Path("/repositories/AGENT/namespaces")
    public Response getNamespaces(
            @Context HttpServletRequest request
    ) {
        monitor.debug(String.format("Received a GET Namespaces request %s", request));
        Response.ResponseBuilder builder = Response.ok("prefix,namespace\n" +
                "rdf,http://www.w3.org/1999/02/22-rdf-syntax-ns#\n" +
                "owl,http://www.w3.org/2002/07/owl#\n" +
                "xsd,http://www.w3.org/2001/XMLSchema#\n" +
                "rdfs,http://www.w3.org/2000/01/rdf-schema#\n" +
                "cx,https://w3id.org/catenax/ontology#\n");
        builder.type("text/csv;charset=UTF-8");
        builder.header("Content-Disposition", "attachment; filename=namespaces.csv");
        return builder.build();
    }

    /**
     * the actual execution is done by delegating to the Fuseki engine
     *
     * @param asset target graph
     * @return a response
     */
    public Response executeQuery(String asset, HttpHeaders headers, HttpServletRequest request, HttpServletResponse response, UriInfo uri) {
        String skill = null;
        String graph = null;
        String remoteUrl = null;

        if (asset != null) {
            Matcher matcher = AgentExtension.GRAPH_PATTERN.matcher(asset);
            if (matcher.matches()) {
                remoteUrl = matcher.group("url");
                graph = matcher.group("graph");
            } else {
                matcher = SkillStore.matchSkill(asset);
                if (!matcher.matches()) {
                    return Response.status(Response.Status.BAD_REQUEST).build();
                }
                remoteUrl = matcher.group("url");
                skill = matcher.group("skill");
            }
        }

        if (remoteUrl != null) {
            // we need to delegate to the agent under remote URL
            DelegationResponse intermediateResponse = delegationService.executeQueryRemote(remoteUrl, skill, graph, headers, request, response, uri);
            // in case runMode = provider the response already contains the final result
            if (intermediateResponse.getQueryString() == null) {
                return intermediateResponse.getResponse();
            } else {
                // in case runMode = consumer we set the skill text to the downloaded text and advance
                skill = intermediateResponse.getQueryString();
                asset = null;
            }
        }

        try {
            // exchange skill against text
            if (asset != null) {
                if (skillStore.isSkill(asset)) {
                    Optional<String> skillOption = skillStore.get(asset);
                    if (skillOption.isPresent()) {
                        skill = skillOption.get();
                    } else {
                        return HttpUtils.respond(monitor, headers, HttpStatus.SC_NOT_FOUND, "The requested skill is not registered.", null);
                    }
                }
            }

            processor.execute(request, response, skill, graph);
            // kind of redundant, but javax.ws.rs likes it this way
            return Response.status(response.getStatus()).build();
        } catch (WebApplicationException e) {
            return HttpUtils.respond(monitor, headers, e.getResponse().getStatus(), e.getMessage(), e.getCause());
        }
    }

    /**
     * endpoint for posting a skill
     *
     * @param query       mandatory query
     * @param asset       asset key
     * @param name        asset name
     * @param description asset description
     * @param version     asset version
     * @param contract    asset contract
     * @param mode        asset mode
     * @param isFederated whether it appears in fed catalogue
     * @param ontologies  list of ontologies
     * @return only status
     */
    @POST
    @Path("/skill")
    @Consumes({ "application/sparql-query" })
    public Response postSkill(String query,
                              @QueryParam("asset") String asset,
                              @QueryParam("assetName") String name,
                              @QueryParam("assetDescription") String description,
                              @QueryParam("assetVersion") String version,
                              @QueryParam("contract") String contract,
                              @QueryParam("distributionMode") SkillDistribution mode,
                              @QueryParam("isFederated") boolean isFederated,
                              @QueryParam("allowServicesPattern") String allowServicePattern,
                              @QueryParam("denyServicesPattern") String denyServicePattern,
                              @QueryParam("ontology") String[] ontologies
    ) {
        monitor.debug(String.format("Received a POST skill request %s %s %s %s %s %b %s %s %s ", asset, name, description, version, contract, mode.getMode(), isFederated, allowServicePattern, denyServicePattern, query));
        Response.ResponseBuilder rb;
        if (skillStore.put(asset, query, name, description, version, contract, mode, isFederated, allowServicePattern, denyServicePattern, ontologies) != null) {
            rb = Response.ok();
        } else {
            rb = Response.status(HttpStatus.SC_CREATED);
        }
        return rb.build();
    }

    /**
     * endpoint for getting a skill
     *
     * @param asset can be a named graph for executing a query or a skill asset
     * @return response
     */
    @GET
    @Path("/skill")
    @Produces({ "application/sparql-query" })
    public Response getSkill(@QueryParam("asset") String asset) {
        monitor.debug(String.format("Received a GET skill request %s", asset));
        Response.ResponseBuilder rb;
        String query = skillStore.get(asset).orElse(null);
        if (query == null) {
            rb = Response.status(HttpStatus.SC_NOT_FOUND);
        } else {
            rb = Response.ok(query);
        }
        return rb.build();
    }
}
