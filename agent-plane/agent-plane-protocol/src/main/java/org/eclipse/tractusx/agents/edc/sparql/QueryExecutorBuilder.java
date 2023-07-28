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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.tractusx.agents.edc.AgentConfig;
import org.apache.jena.http.sys.ExecHTTPBuilder;
import org.apache.jena.query.Query;
import org.apache.jena.sparql.exec.QueryExecMod;
import org.apache.jena.sparql.exec.http.Params;
import org.apache.jena.sparql.util.Context;

import java.net.http.HttpClient;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import static org.apache.jena.http.HttpLib.copyArray;

import org.apache.jena.sparql.exec.QueryExecBuilder;

/**
 * A builder for KA Remote Query Execs
 */
public class QueryExecutorBuilder extends ExecHTTPBuilder<QueryExecutor, QueryExecutorBuilder> implements QueryExecMod, QueryExecBuilder {

    public static QueryExecutorBuilder create() { return new QueryExecutorBuilder(); }

    public static QueryExecutorBuilder service(String serviceURL) { return create().endpoint(serviceURL); }

    private QueryExecutorBuilder() {}

    @Override
    protected QueryExecutorBuilder thisBuilder() {
        return this;
    }

    protected ObjectMapper objectMapper;
    protected AgentConfig agentConfig;

    @Override
    protected QueryExecutor buildX(HttpClient hClient, Query queryActual, String queryStringActual, Context cxt) {
        return new QueryExecutor(serviceURL, queryActual, queryStringActual, urlLimit,
                hClient, new HashMap<>(httpHeaders), Params.create(params), cxt,
                copyArray(defaultGraphURIs),
                copyArray(namedGraphURIs),
                sendMode, appAcceptHeader,
                timeout, timeoutUnit, objectMapper,agentConfig);
    }

    @Override
    public QueryExecutorBuilder initialTimeout(long timeout, TimeUnit timeUnit) {
        throw new UnsupportedOperationException();
    }

    public QueryExecutorBuilder objectMapper(ObjectMapper objectMapper) {
        this.objectMapper=objectMapper;
        return this;
    }

    public QueryExecutorBuilder agentConfig(AgentConfig agentConfig) {
        this.agentConfig=agentConfig;
        return this;
    }

    @Override
    public QueryExecutorBuilder overallTimeout(long timeout, TimeUnit timeUnit) {
        super.timeout(timeout, timeUnit);
        return thisBuilder();
    }

    @Override
    public Context getContext() {
        return null;
    }
}
