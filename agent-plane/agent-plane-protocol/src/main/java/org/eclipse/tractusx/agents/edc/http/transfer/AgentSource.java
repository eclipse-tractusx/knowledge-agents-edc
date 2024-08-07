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
package org.eclipse.tractusx.agents.edc.http.transfer;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okio.Buffer;
import org.eclipse.edc.connector.dataplane.http.params.HttpRequestFactory;
import org.eclipse.edc.connector.dataplane.http.spi.HttpRequestParams;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSource;
import org.eclipse.edc.connector.dataplane.spi.pipeline.StreamResult;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;
import org.eclipse.tractusx.agents.edc.AgentConfig;
import org.eclipse.tractusx.agents.edc.SkillDistribution;
import org.eclipse.tractusx.agents.edc.SkillStore;
import org.eclipse.tractusx.agents.edc.TupleSet;
import org.eclipse.tractusx.agents.edc.http.AgentHttpAction;
import org.eclipse.tractusx.agents.edc.sparql.SparqlQueryProcessor;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.stream.Stream;

import static java.lang.String.format;

/**
 * An agent-enabled http-transfer source
 * there are two modes of operation: Routing transfer and
 * calling a single (in the future: several) graph endpoint
 * (in which case we rather need to
 * invoke the surrounding plane sparql engine to
 * perform some joins/pre and postprocessing and event
 * further delegations) and that engine will in turn
 * delegate to the final endpoint (for which we
 * replace any "GRAPH" occurences with "SERVICE" references
 * in the processor)
 * TODO generalize to shield several endpoints/assets
 */
public class AgentSource implements DataSource {
    protected String name;

    protected HttpRequestParams params;
    protected String requestId;
    protected HttpRequestFactory requestFactory;
    protected EdcHttpClient httpClient;
    protected SparqlQueryProcessor processor;
    protected SkillStore skillStore;

    protected DataFlowStartMessage request;

    protected TypeManager typeManager;

    protected String matchmakingAgentUrl;

    public static final String AGENT_BOUNDARY = "--";

    /**
     * creates new agent source
     */
    public AgentSource() {
    }

    @Override
    public StreamResult<Stream<Part>> openPartStream() {
        if (matchmakingAgentUrl != null) {
            return openMatchmakingRest();
        } else {
            return openMatchmakingInternal();
        }
    }

    /**
     * executes a KA-MATCHMAKING call and pipes the results into KA-TRANSFER
     *
     * @return multipart body containing result and warnings
     */
    @NotNull
    protected StreamResult<Stream<Part>> openMatchmakingInternal() {
        // Agent call, we translate from KA-MATCH to KA-TRANSFER
        String skill = null;
        String graph = null;
        String asset = String.valueOf(request.getSourceDataAddress().getProperties().get(AgentSourceHttpParamsDecorator.ASSET_PROP_ID));
        if (asset != null && asset.length() > 0) {
            Matcher assetMatcher = AgentConfig.getAssetReferencePattern().matcher(asset);
            if (assetMatcher.matches()) {
                if (assetMatcher.group("asset").contains("Graph")) {
                    graph = asset;
                } else if (assetMatcher.group("asset").contains("Skill")) {
                    var skillText = skillStore.get(asset);
                    if (skillText.isEmpty()) {
                        return StreamResult.error(format("Skill %s does not exist.", asset));
                    }
                    SkillDistribution distribution = skillStore.getDistribution(asset);
                    String params = request.getProperties().get(AgentSourceHttpParamsDecorator.QUERY_PARAMS);
                    SkillDistribution runMode = SkillDistribution.ALL;
                    // if we have no distribution constraints, let the runMode decide
                    if (distribution == SkillDistribution.ALL) {
                        distribution = runMode;
                    } else if (runMode == SkillDistribution.ALL) {
                        // if we have no runMode constraints, let the distribution decide
                        runMode = distribution;
                    }
                    if (params.contains("runMode=provider") || params.contains("runMode=PROVIDER")) {
                        runMode = SkillDistribution.PROVIDER;
                    } else if (params.contains("runMode=consumer") || params.contains("runMode=CONSUMER")) {
                        runMode = SkillDistribution.CONSUMER;
                    }
                    if (runMode == SkillDistribution.CONSUMER) {
                        if (distribution == SkillDistribution.PROVIDER) {
                            return StreamResult.error(String.format("Run distribution of skill %s should be consumer, but was set to provider only.", asset));
                        }
                        String query = skillText.get();
                        okhttp3.Request tempRequest = this.requestFactory.toRequest(this.params);
                        if (tempRequest.body() != null && AgentHttpAction.RESULTSET_CONTENT_TYPE.equals(tempRequest.body().contentType().toString())) {
                            TupleSet bindings = new TupleSet();
                            try {
                                Buffer buffer = new Buffer();
                                tempRequest.body().writeTo(buffer);
                                AgentHttpAction.parseBinding(typeManager.getMapper().readTree(buffer.readByteArray()), bindings);
                                query = AgentHttpAction.bind(query, bindings);
                            } catch (Exception e) {
                                return StreamResult.error(String.format("The query could not be bound to the given input tupleset.", e));
                            }
                        }
                        return StreamResult.success(Stream.of(new AgentPart("application/sparql-query", query.getBytes())));
                    } else if (runMode == SkillDistribution.PROVIDER && distribution == SkillDistribution.CONSUMER) {
                        return StreamResult.error(String.format("Run distribution of skill %s should be provider, but was set to consumer only.", asset));
                    }
                    skill = skillText.get(); // default execution for runMode=ALL or runMode=provider and DistributionMode is ALL or provider
                }
            }
        }

        try (Response response = processor.execute(this.requestFactory.toRequest(params), skill, graph, request.getSourceDataAddress().getProperties())) {
            if (!response.isSuccessful()) {
                return StreamResult.error(format("Received code transferring HTTP data for request %s: %s - %s.", requestId, response.code(), response.message()));
            }
            List<Part> results = new ArrayList<>();
            if (response.body() != null) {
                results.add(new AgentPart(response.body().contentType().toString(), response.body().bytes()));
            }
            if (response.header("cx_warnings") != null) {
                results.add(new AgentPart("application/cx-warnings+json", response.header("cx_warnings").getBytes()));
            }
            return StreamResult.success(results.stream());
        } catch (IOException e) {
            return StreamResult.error(e.getMessage());
        }
    }

    /**
     * executes a KA-MATCHMAKING REST API call and pipes the results into KA-TRANSFER
     *
     * @return multipart body containing result and warnings
     */
    @NotNull
    protected StreamResult<Stream<Part>> openMatchmakingRest() {
        // Agent call, we translate from KA-MATCH to KA-TRANSFER
        String skill = null;
        String graph = null;
        String asset = String.valueOf(request.getSourceDataAddress().getProperties().get(AgentSourceHttpParamsDecorator.ASSET_PROP_ID));
        String assetValue;
        OkHttpClient client = new OkHttpClient();
        String baseUrl = matchmakingAgentUrl;

        String url = baseUrl + "?asset=" + asset;
        if (asset != null && asset.length() > 0) {
            Matcher assetMatcher = AgentConfig.getAssetReferencePattern().matcher(asset);
            if (assetMatcher.matches()) {
                if (assetMatcher.group("asset").contains("Graph")) {
                    graph = asset;
                } else if (assetMatcher.group("asset").contains("Skill")) {
                    var skillText = skillStore.get(asset);
                    if (skillText.isEmpty()) {
                        return StreamResult.error(format("Skill %s does not exist.", asset));
                    }
                    SkillDistribution distribution = skillStore.getDistribution(asset);
                    String params = request.getProperties().get(AgentSourceHttpParamsDecorator.QUERY_PARAMS);
                    SkillDistribution runMode = SkillDistribution.ALL;
                    if (params.contains("runMode=provider") || params.contains("runMode=PROVIDER")) {
                        runMode = SkillDistribution.PROVIDER;
                    } else if (params.contains("runMode=consumer") || params.contains("runMode=CONSUMER")) {
                        runMode = SkillDistribution.CONSUMER;
                    }
                    // if we have no distribution constraints, let the runMode decide
                    if (distribution == SkillDistribution.ALL) {
                        distribution = runMode;
                    } else if (runMode == SkillDistribution.ALL) {
                        // if we have no runMode constraints, let the distribution decide
                        runMode = distribution;
                    }
                    if (runMode == SkillDistribution.CONSUMER) {
                        if (distribution == SkillDistribution.PROVIDER) {
                            return StreamResult.error(String.format("Run distribution of skill %s should be consumer, but was set to provider only.", asset));
                        }
                        String query = skillText.get();
                        okhttp3.Request tempRequest = this.requestFactory.toRequest(this.params);
                        if (tempRequest.body() != null && AgentHttpAction.RESULTSET_CONTENT_TYPE.equals(tempRequest.body().contentType().toString())) {
                            TupleSet bindings = new TupleSet();
                            try {
                                Buffer buffer = new Buffer();
                                tempRequest.body().writeTo(buffer);
                                AgentHttpAction.parseBinding(typeManager.getMapper().readTree(buffer.readByteArray()), bindings);
                                query = AgentHttpAction.bind(query, bindings);
                            } catch (Exception e) {
                                return StreamResult.error(String.format("The query could not be bound to the given input tupleset.", e));
                            }
                        }
                        return StreamResult.success(Stream.of(new AgentPart("application/sparql-query", query.getBytes())));
                    } else if (runMode == SkillDistribution.PROVIDER && distribution == SkillDistribution.CONSUMER) {
                        return StreamResult.error(String.format("Run distribution of skill %s should be provider, but was set to consumer only.", asset));
                    }
                    skill = skillText.get(); // default execution for runMode=ALL or runMode=provider and DistributionMode is ALL or provider
                }
            }
        }

        try {
            // Either put skill or asset in URL as param
            if (skill == null) {
                assetValue = graph;
            } else {
                assetValue = skill;
            }

            HttpUrl.Builder urlBuilder = HttpUrl.parse(url).newBuilder();
            urlBuilder.addQueryParameter("asset", assetValue);
            // Put parameters into request
            Map<String, Object> targetProperties = request.getSourceDataAddress().getProperties();
            Set<Entry<String, Object>> entrySet = targetProperties.entrySet();
            for (Entry<String, Object> entry : entrySet) {
                urlBuilder.addQueryParameter(entry.getKey(), entry.getValue().toString());
            }
            HttpUrl httpUrl = urlBuilder.build();
            // Build request from original request with adapted URL
            Request httpRequest = this.requestFactory.toRequest(params)
                    .newBuilder().url(httpUrl)
                    .build();

            // Send request and get response
            Response response = client.newCall(httpRequest).execute();
            if (!response.isSuccessful()) {
                return StreamResult.error(format("Received code transferring HTTP data for request %s: %s - %s.", requestId, response.code(), response.message()));
            }
            List<Part> results = new ArrayList<>();
            if (response.body() != null) {
                results.add(new AgentPart(response.body().contentType().toString(), response.body().bytes()));
            }
            if (response.header("cx_warnings") != null) {
                results.add(new AgentPart("application/cx-warnings+json", response.header("cx_warnings").getBytes()));
            }
            return StreamResult.success(results.stream());
        } catch (IOException e) {
            return StreamResult.error(e.getMessage());
        }
    }

    @Override
    public String toString() {
        return String.format("AgentSource(%s,%s)", requestId, name);
    }

    @Override
    public void close() throws Exception {

    }

    /**
     * the agent source builder
     */
    public static class Builder {
        protected final AgentSource dataSource;

        public static AgentSource.Builder newInstance() {
            return new AgentSource.Builder();
        }

        public AgentSource.Builder params(HttpRequestParams params) {
            dataSource.params = params;
            return this;
        }

        public AgentSource.Builder name(String name) {
            dataSource.name = name;
            return this;
        }

        public AgentSource.Builder requestId(String requestId) {
            dataSource.requestId = requestId;
            return this;
        }

        public AgentSource.Builder requestFactory(HttpRequestFactory requestFactory) {
            dataSource.requestFactory = requestFactory;
            return this;
        }

        public AgentSource.Builder httpClient(EdcHttpClient httpClient) {
            dataSource.httpClient = httpClient;
            return this;
        }

        public AgentSource.Builder processor(SparqlQueryProcessor processor) {
            dataSource.processor = processor;
            return this;
        }

        public AgentSource.Builder skillStore(SkillStore skillStore) {
            dataSource.skillStore = skillStore;
            return this;
        }

        public AgentSource.Builder request(DataFlowStartMessage request) {
            dataSource.request = request;
            return this;
        }

        public AgentSource.Builder matchmakingAgentUrl(String matchmakingAgentUrl) {
            dataSource.matchmakingAgentUrl = matchmakingAgentUrl;
            return this;
        }

        public AgentSource.Builder typeManager(TypeManager typeManager) {
            dataSource.typeManager = typeManager;
            return this;
        }

        public AgentSource build() {
            Objects.requireNonNull(dataSource.requestId, "requestId");
            Objects.requireNonNull(dataSource.httpClient, "httpClient");
            Objects.requireNonNull(dataSource.requestFactory, "requestFactory");
            return dataSource;
        }

        public Builder() {
            dataSource = new AgentSource();
        }
    }

    private static class AgentPart implements Part {
        private final String name;
        private final byte[] content;

        AgentPart(String name, byte[] content) {
            this.name = name;
            if (this.name != null) {
                StringBuilder newContent = new StringBuilder();
                newContent.append(AGENT_BOUNDARY);
                newContent.append("\n");
                newContent.append("Content-Type: ");
                newContent.append(name);
                newContent.append("\n");
                newContent.append(new String(content));
                this.content = newContent.toString().getBytes();
            } else {
                this.content = content;
            }
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public long size() {
            return content.length;
        }

        @Override
        public InputStream openStream() {
            return new ByteArrayInputStream(content);
        }

    }

}
