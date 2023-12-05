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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.tractusx.agents.edc.AgentConfig;
import org.eclipse.tractusx.agents.edc.rdf.ExternalFormat;
import org.eclipse.tractusx.agents.edc.rdf.RdfStore;
import org.eclipse.tractusx.agents.edc.service.DataManagement;

import java.io.IOException;

/**
 * The Graph Controller exposes a REST API endpoint
 * with which the EDC tenant can manage graph content
 * which can be published as assets.
 */
@Path("/graph")
public class GraphController {

    // EDC services
    protected final Monitor monitor;
    protected final RdfStore store;
    protected final DataManagement management;
    protected final AgentConfig config;

    /**
     * creates a new agent controller
     *
     * @param monitor logging subsystem
     * @param store the rdf store to extend
     */
    public GraphController(Monitor monitor, RdfStore store, DataManagement management, AgentConfig config) {
        this.monitor = monitor;
        this.store = store;
        this.management = management;
        this.config = config;
    }

    /**
     * render nicely
     */
    @Override
    public String toString() {
        return super.toString() + "/graph";
    }

    /**
     * endpoint for posting a ttl into a local graph asset
     *
     * @param content mandatory content
     * @param asset asset key
     * @param name asset name
     * @param description asset description
     * @param version asset version
     * @param contract asset contract
     * @param shape asset shape
     * @param isFederated whether it appears in fed catalogue
     * @param ontologies list of ontologies
     * @return response indicating the number of triples updated
     */
    @POST
    @Consumes({"text/turtle", "text/csv"})
    public Response postAsset(String content,
                              @QueryParam("asset") String asset,
                              @QueryParam("assetName") String name,
                              @QueryParam("assetDescription") String description,
                              @QueryParam("assetVersion") String version,
                              @QueryParam("contract") String contract,
                              @QueryParam("shape") String shape,
                              @QueryParam("isFederated") boolean isFederated,
                              @QueryParam("ontology") String[] ontologies,
                              @Context HttpServletRequest request
    ) {
        ExternalFormat format = ExternalFormat.valueOfFormat(request.getContentType());
        monitor.debug(String.format("Received a POST asset request %s %s %s %s %s %b in format %s", asset, name, description, version, contract, shape, isFederated, format));
        if (format == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        try {
            if (name == null) {
                name = "No name given";
            }
            if (description == null) {
                description = "No description given";
            }
            if (version == null) {
                version = "unknown version";
            }
            if (contract == null) {
                contract = config.getDefaultGraphContract();
            }
            String ontologiesString = String.join(", ", ontologies);
            if (shape == null) {
                shape = String.format("@prefix : <%s#> .\\n", asset);
            }
            management.createOrUpdateGraph(asset, name, description, version, contract, ontologiesString, shape, isFederated);
            return Response.ok(store.registerAsset(asset, content, format), MediaType.APPLICATION_JSON_TYPE).build();
        } catch (IOException e) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
    }

    /**
     * endpoint for deleting a local graph asset
     *
     * @param asset can be a named graph for executing a query or a skill asset
     * @return response
     */
    @DELETE
    public Response deleteAsset(@QueryParam("asset") String asset,
                              @Context HttpHeaders headers,
                              @Context HttpServletRequest request,
                              @Context HttpServletResponse response,
                              @Context UriInfo uri
    ) {
        monitor.debug(String.format("Received a DELETE request %s for asset %s", request, asset));
        try {
            management.deleteAsset(asset);
            return Response.ok(store.deleteAsset(asset), MediaType.APPLICATION_JSON_TYPE).build();
        } catch (IOException e) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
    }

}
