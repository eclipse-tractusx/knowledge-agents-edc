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
package org.eclipse.tractusx.agents.edc.validation;

import org.eclipse.tractusx.agents.edc.AgentConfig;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.eclipse.edc.spi.monitor.Monitor;

import java.io.IOException;

/**
 * a token validator that may delegate to several control plane validators
 */
@Path("/validation")
public class SwitchingDataPlaneTokenValidatorController implements DataPlaneTokenValidationApi {

    protected final OkHttpClient httpClient;
    protected final Monitor monitor;
    protected final AgentConfig config;
    protected final String[] endpoints;

    /**
     * creates a new controller
     * @param httpClient to use
     * @param config to obey
     * @param monitor to log
     */
    public SwitchingDataPlaneTokenValidatorController(OkHttpClient httpClient, AgentConfig config, Monitor monitor) {
        this.httpClient=httpClient;
        this.config=config;
        this.monitor=monitor;
        this.endpoints=config.getValidatorEndpoints();
    }

    /**
     * @return a flag indicating whether this endpoint is enabled
     */
    public boolean isEnabled() {
        return endpoints!=null && endpoints.length>0;
    }

    /**
     * Validate the token provided in input by delegating to the multiple endpoints
     *
     * @param token Input token.
     * @return Decrypted DataAddress contained in the input token claims.
     */
    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    @Override
    public Response validate(@HeaderParam(HttpHeaders.AUTHORIZATION) String token) {
        Response result=Response.status(400,"No validation endpoint could be found to switch to.").build();
        if(isEnabled()) {
            for (String endpoint : endpoints) {
                var request = new Request.Builder().url(endpoint).header(HttpHeaders.AUTHORIZATION, token).get().build();
                try (var response = httpClient.newCall(request).execute()) {
                    var body = response.body();
                    var stringBody = body != null ? body.string() : null;
                    if (stringBody == null) {
                        result = Response.status(400, "Token validation server returned null body").build();
                    } else if (response.isSuccessful()) {
                        return Response.ok(stringBody).build();
                    } else {
                        result = Response.status(response.code(), String.format("Call to token validation sever failed: %s - %s. %s", response.code(), response.message(), stringBody)).build();
                    }
                } catch (IOException e) {
                    result = Response.status(500, "Unhandled exception occurred during call to token validation server: " + e.getMessage()).build();
                }
            }
        }
        return result;
    }
}
