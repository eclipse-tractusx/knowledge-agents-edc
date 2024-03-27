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
package org.eclipse.tractusx.agents.edc.http.transfer;

import org.eclipse.edc.connector.dataplane.http.params.HttpRequestFactory;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSource;
import org.eclipse.edc.spi.http.EdcHttpClient;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.types.domain.HttpDataAddress;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowRequest;
import org.eclipse.tractusx.agents.edc.AgentProtocol;
import org.eclipse.tractusx.agents.edc.SkillStore;
import org.eclipse.tractusx.agents.edc.sparql.SparqlQueryProcessor;

/**
 * A factory for Agent Sources (representing backend SparQL endpoints)
 */
public class AgentSourceFactory extends org.eclipse.edc.connector.dataplane.http.pipeline.HttpDataSourceFactory {

    final AgentSourceRequestParamsSupplier supplier;
    final Monitor monitor;
    final EdcHttpClient httpClient;
    final SparqlQueryProcessor processor;
    final SkillStore skillStore;
    final HttpRequestFactory requestFactory;


    /**
     * create a new agent source factory
     *
     * @param httpClient http outgoing system
     * @param supplier a parameter supplier helper
     * @param monitor logging facility
     * @param requestFactory for outgoing calls
     * @param processor the query processor/sparql engine
     * @param skillStore store for skills
     */
    public AgentSourceFactory(EdcHttpClient httpClient, AgentSourceRequestParamsSupplier supplier, Monitor monitor, HttpRequestFactory requestFactory, SparqlQueryProcessor processor, SkillStore skillStore) {
        super(httpClient, supplier, monitor, requestFactory);
        this.supplier = supplier;
        this.monitor = monitor;
        this.httpClient = httpClient;
        this.skillStore = skillStore;
        this.processor = processor;
        this.requestFactory = requestFactory;
    }

    /**
     * choose the agent protocol
     *
     * @param request the request to check
     * @return flag
     */
    @Override
    public boolean canHandle(DataFlowRequest request) {
        return AgentProtocol.SPARQL_HTTP.getProtocolId().equals(request.getSourceDataAddress().getType()) ||
                AgentProtocol.SKILL_HTTP.getProtocolId().equals(request.getSourceDataAddress().getType());
    }

    /**
     * depending on the transfer mode,  choose to manipulate the
     * target address.
     *
     * @param request incoming agent protocol request
     * @return new data source
     */
    @Override
    public DataSource createSource(DataFlowRequest request) {
        var dataAddress = HttpDataAddress.Builder.newInstance()
                .copyFrom(request.getSourceDataAddress())
                .build();
        AgentSource dataSource = AgentSource.Builder.newInstance()
                .httpClient(httpClient)
                .requestId(request.getId())
                .name(dataAddress.getName())
                .params(supplier.provideSourceParams(request))
                .requestFactory(requestFactory)
                .skillStore(skillStore)
                .processor(processor)
                .request(request)
                .matchmakingAgentUrl(supplier.provideMatchmakingUrl())
                .matchmakingAgentRest(supplier.provideMatchmakingRest())
                .build();
        monitor.debug(String.format("Created a new agent source %s for destination type %s",
                dataSource,
                request.getDestinationDataAddress().getType()));
        return dataSource;
    }

}