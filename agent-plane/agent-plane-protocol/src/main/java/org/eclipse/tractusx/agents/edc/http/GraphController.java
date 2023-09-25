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
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import org.apache.http.HttpStatus;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.tractusx.agents.edc.*;
import org.eclipse.tractusx.agents.edc.rdf.ExternalFormat;
import org.eclipse.tractusx.agents.edc.rdf.RDFStore;
import org.eclipse.tractusx.agents.edc.sparql.SparqlQueryProcessor;

import java.io.IOException;
import java.io.StringReader;
import java.util.Optional;
import java.util.regex.Matcher;

/**
 * The Graph Controller exposes a REST API endpoint
 * with which the EDC tenant can manage graph content
 * which can be published as assets.
 */
@Path("/graph")
public class GraphController {

    // EDC services
    protected final Monitor monitor;
    protected final RDFStore store;

    /**
     * creates a new agent controller
     * @param monitor logging subsystem
     * @param store the rdf store to extend
     */
    public GraphController(Monitor monitor, RDFStore store) {
        this.monitor = monitor;
        this.store=store;
    }

    /**
     * render nicely
     */
    @Override
    public String toString() {
        return super.toString()+"/graph";
    }

    /**
     * endpoint for posting a ttl into a local graph asset
     * @param asset can be a named graph for executing a query or a skill asset
     * @return response
     */
    @POST
    @Consumes({"text/turtle","text/csv"})
    public Response postAsset(@QueryParam("asset") String asset,
                              @Context HttpHeaders headers,
                              @Context HttpServletRequest request,
                              @Context HttpServletResponse response,
                              @Context UriInfo uri
    ) {
        ExternalFormat format=ExternalFormat.valueOfFormat(request.getContentType());
        monitor.debug(String.format("Received a POST request %s for asset %s in format %s", request, asset, format));
        if(format==null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        try {
            return Response.ok(store.registerAsset(asset, request.getInputStream(),format),MediaType.APPLICATION_JSON_TYPE).build();
        } catch(IOException e) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
    }

    /**
     * endpoint for deleting a local graph asset
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
        return Response.ok(store.deleteAsset(asset),MediaType.APPLICATION_JSON_TYPE).build();
    }

}
