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
package org.eclipse.tractusx.agents.edc;

import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.configuration.Config;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * typed wrapper around the
 * EDC configuration
 */
public class AgentConfig {

    public static final String DEFAULT_ASSET_PROPERTY = "cx.agent.asset.default";
    public static final String DEFAULT_ASSET_NAME = "urn:x-arq:DefaultGraph";

    public static final String ASSET_FILE_PROPERTY = "cx.agent.asset.file";

    public static final String ACCESS_POINT_PROPERTY = "cx.agent.accesspoint.name";
    public static final String DEFAULT_ACCESS_POINT = "api";

    public static final String VERBOSE_PROPERTY = "cx.agent.sparql.verbose";
    public static final boolean DEFAULT_VERBOSE_PROPERTY = false;

    public static final String CONTROL_PLANE_MANAGEMENT_PROVIDER = "cx.agent.controlplane.management.provider";
    public static final String CONTROL_PLANE_MANAGEMENT = "cx.agent.controlplane.management";
    public static final String CONTROL_PLANE_IDS = "cx.agent.controlplane.protocol";

    public static final String BUSINESS_PARTNER_NUMBER = "edc.participant.id";
    public static final String CONTROL_PLANE_AUTH_HEADER = "edc.api.auth.header";
    public static final String CONTROL_PLANE_AUTH_VALUE = "edc.api.auth.key";

    public static final String NEGOTIATION_TIMEOUT_PROPERTY = "cx.agent.negotiation.timeout";
    public static final long DEFAULT_NEGOTIATION_TIMEOUT = 30000;

    public static final String NEGOTIATION_POLLINTERVAL_PROPERTY = "cx.agent.negotiation.poll";
    public static final long DEFAULT_NEGOTIATION_POLLINTERVAL = 1000;

    public static final String DATASPACE_SYNCINTERVAL_PROPERTY = "cx.agent.dataspace.synchronization";
    public static final long DEFAULT_DATASPACE_SYNCINTERVAL = -1;

    public static final String DATASPACE_SYNCCONNECTORS_PROPERTY = "cx.agent.dataspace.remotes";

    public static final String VALIDATION_ENDPOINTS = "edc.dataplane.token.validation.endpoints";

    public static final String FEDERATION_SERVICE_BATCH_SIZE = "cx.agent.federation.batch.max";
    public static final long DEFAULT_FEDERATION_SERVICE_BATCH_SIZE = Long.MAX_VALUE;

    public static final String THREAD_POOL_SIZE = "cx.agent.threadpool.size";
    public static final int DEFAULT_THREAD_POOL_SIZE = 4;

    public static final String CONNECT_TIMEOUT_PROPERTY = "cx.agent.connect.timeout";
    public static final String WRITE_TIMEOUT_PROPERTY = "cx.agent.write.timeout";
    public static final String CALL_TIMEOUT_PROPERTY = "cx.agent.call.timeout";
    public static final String READ_TIMEOUT_PROPERTY = "cx.agent.read.timeout";
    public static final int DEFAULT_READ_TIMEOUT = 1080000;

    public static final String CALLBACK_ENDPOINT = "cx.agent.callback";

    public static final String DEFAULT_SKILL_CONTRACT_PROPERTY = "cx.agent.skill.contract.default";

    public static final String SERVICE_ALLOW_PROPERTY = "cx.agent.service.allow";
    public static final String DEFAULT_SERVICE_ALLOW_PATTERN = "(http|edc)s?://.*";

    public static final String SERVICE_DENY_PROPERTY = "cx.agent.service.deny";
    public static final String DEFAULT_SERVICE_DENY_PATTERN = "^$";

    public static final String SERVICE_ALLOW_ASSET_PROPERTY = "cx.agent.service.asset.allow";
    public static final String DEFAULT_SERVICE_ALLOW_ASSET_PATTERN = "(http|edc)s://.*";

    public static final String SERVICE_DENY_ASSET_PROPERTY = "cx.agent.service.asset.deny";
    public static final String DEFAULT_SERVICE_DENY_ASSET_PATTERN = "^$";

    public static final String TX_EDC_VERSION_PROPERTY = "cx.agent.edc.version";

    public static final String MATCHMAKING_URL = "cx.agent.matchmaking";

    /**
     * precompiled stuff
     */
    protected final Pattern serviceAllowPattern;
    protected final Pattern serviceDenyPattern;
    protected final Pattern serviceAssetAllowPattern;
    protected final Pattern serviceAssetDenyPattern;

    /**
     * references to EDC services
     */
    protected final Config config;
    protected final Monitor monitor;

    /**
     * creates the typed config
     *
     * @param monitor logger
     * @param config  untyped config
     */
    public AgentConfig(Monitor monitor, Config config) {
        this.monitor = monitor;
        this.config = config;
        serviceAllowPattern = Pattern.compile(config.getString(SERVICE_ALLOW_PROPERTY, DEFAULT_SERVICE_ALLOW_PATTERN));
        serviceDenyPattern = Pattern.compile(config.getString(SERVICE_DENY_PROPERTY, DEFAULT_SERVICE_DENY_PATTERN));
        serviceAssetAllowPattern = Pattern.compile(config.getString(SERVICE_ALLOW_ASSET_PROPERTY, DEFAULT_SERVICE_ALLOW_ASSET_PATTERN));
        serviceAssetDenyPattern = Pattern.compile(config.getString(SERVICE_DENY_ASSET_PROPERTY, DEFAULT_SERVICE_DENY_ASSET_PATTERN));
    }

    /**
     * access
     *
     * @return callback endpoint
     */
    public String getCallbackEndpoint() {
        return config.getString(CALLBACK_ENDPOINT);
    }

    /**
     * access
     *
     * @return the name of the default asset/graph
     */
    public String getDefaultAsset() {
        return config.getString(DEFAULT_ASSET_PROPERTY, DEFAULT_ASSET_NAME);
    }

    public String getBusinessPartnerNumber() {
        return config.getString(BUSINESS_PARTNER_NUMBER, "anonymous");
    }

    /**
     * access
     *
     * @return initial file to load
     */
    public String[] getAssetFiles() {
        String[] files = config.getString(ASSET_FILE_PROPERTY, "").split(",");
        if (files.length == 1 && (files[0] == null || files[0].length() == 0)) {
            return null;
        }
        return files;
    }

    /**
     * access
     *
     * @return name of the sparql access point
     */
    public String getAccessPoint() {
        return config.getString(ACCESS_POINT_PROPERTY, DEFAULT_ACCESS_POINT);
    }

    /**
     * access
     *
     * @return uri of the control plane management endpoint (without concrete api)
     */
    public String getControlPlaneManagementUrl() {
        return config.getString(CONTROL_PLANE_MANAGEMENT, null);
    }

    /**
     * access
     *
     * @return uri of the control plane management endpoint (without concrete api)
     */
    public String getControlPlaneManagementProviderUrl() {
        return config.getString(CONTROL_PLANE_MANAGEMENT_PROVIDER, config.getString(CONTROL_PLANE_MANAGEMENT, null));
    }

    /**
     * access
     *
     * @return uri of the control plane ids endpoint (without concrete api)
     */
    public String getControlPlaneIdsUrl() {
        return config.getString(CONTROL_PLANE_IDS, null);
    }

    /**
     * access
     *
     * @return a map of key/value paris to be used when interacting with the control plane management endpoint
     */
    public Map<String, String> getControlPlaneManagementHeaders() {
        String key = config.getString(CONTROL_PLANE_AUTH_HEADER, "X-Api-Key");
        String value = config.getString(CONTROL_PLANE_AUTH_VALUE, null);
        if (key != null && value != null) {
            return Map.of(key, value);
        }
        return Map.of();
    }

    /**
     * access
     *
     * @return the default overall timeout when waiting for a negotation result
     */
    public long getNegotiationTimeout() {
        return config.getLong(NEGOTIATION_TIMEOUT_PROPERTY, DEFAULT_NEGOTIATION_TIMEOUT);
    }

    /**
     * access
     *
     * @return the thread pool size of the agent executors
     */
    public int getThreadPoolSize() {
        return config.getInteger(THREAD_POOL_SIZE, DEFAULT_THREAD_POOL_SIZE);
    }

    /**
     * access
     *
     * @return the default overall timeout when waiting for a negotation result
     */
    public long getNegotiationPollInterval() {
        return config.getLong(NEGOTIATION_POLLINTERVAL_PROPERTY, DEFAULT_NEGOTIATION_POLLINTERVAL);
    }

    /**
     * access
     *
     * @return the synchronization interval between individual sync calls, -1 if no sync
     */
    public long getDataspaceSynchronizationInterval() {
        return config.getLong(DATASPACE_SYNCINTERVAL_PROPERTY, DEFAULT_DATASPACE_SYNCINTERVAL);
    }

    protected volatile Map<String, String> knownConnectors;

    /**
     * access
     *
     * @return map of business partner ids to connector urls to synchronize with, null if no sync
     */
    public Map<String, String> getDataspaceSynchronizationConnectors() {
        if (knownConnectors == null) {
            synchronized (config) {
                if (knownConnectors == null) {
                    knownConnectors = new HashMap<>();
                    String[] connectors = config.getString(DATASPACE_SYNCCONNECTORS_PROPERTY, "").split(",");
                    for (String connector : connectors) {
                        String[] entry = connector.split("=");
                        if (entry.length > 0) {
                            String key = UUID.randomUUID().toString();
                            String value = entry[0];
                            if (entry.length > 1) {
                                key = entry[0];
                                value = entry[1];
                            }
                            knownConnectors.put(key, value);
                        }
                    }
                }
            }
        }
        return knownConnectors;
    }

    /**
     * access
     *
     * @return array of validation endpoints
     */
    public String[] getValidatorEndpoints() {
        return config.getConfig(VALIDATION_ENDPOINTS).getEntries().values().toArray(new String[0]);
    }

    /**
     * access
     *
     * @return whether sparql engine is set to verbose
     */
    public boolean isSparqlVerbose() {
        return config.getBoolean(VERBOSE_PROPERTY, DEFAULT_VERBOSE_PROPERTY);
    }

    /**
     * access
     *
     * @return maximal batch size for remote service calls
     */
    public long getFederationServiceBatchSize() {
        return config.getLong(FEDERATION_SERVICE_BATCH_SIZE, DEFAULT_FEDERATION_SERVICE_BATCH_SIZE);
    }

    /**
     * access
     *
     * @return outgoing socket connect timeout
     */
    public Integer getConnectTimeout() {
        return config.getInteger(CONNECT_TIMEOUT_PROPERTY, null);
    }

    /**
     * access
     *
     * @return outgoing socket read timeout
     */
    public Integer getReadTimeout() {
        return config.getInteger(READ_TIMEOUT_PROPERTY, DEFAULT_READ_TIMEOUT);
    }

    /**
     * access
     *
     * @return outgoing socket write timeout
     */
    public Integer getWriteTimeout() {
        return config.getInteger(WRITE_TIMEOUT_PROPERTY, null);
    }

    /**
     * access
     *
     * @return outgoing socket write timeout
     */
    public Integer getCallTimeout() {
        return config.getInteger(CALL_TIMEOUT_PROPERTY, null);
    }

    /**
     * access
     *
     * @return default skill contract
     */
    public String getDefaultSkillContract() {
        return config.getString(DEFAULT_SKILL_CONTRACT_PROPERTY, null);
    }

    /**
     * access
     *
     * @return regular expression for allowed service URLs
     */
    public Pattern getServiceAllowPattern() {
        return serviceAllowPattern;
    }

    /**
     * access
     *
     * @return regular expression for denied service URLs
     */
    public Pattern getServiceDenyPattern() {
        return serviceDenyPattern;
    }

    /**
     * access
     *
     * @return regular expression for allowed service URLs in assets
     */
    public Pattern getServiceAssetAllowPattern() {
        return serviceAssetAllowPattern;
    }

    /**
     * access
     *
     * @return regular expression for denied service URLs in assets
     */
    public Pattern getServiceAssetDenyPattern() {
        return serviceAssetDenyPattern;
    }

    /**
     * access
     *
     * @return tx edc version as a string
     */
    public String getEdcVersion() {
        return config.getString(TX_EDC_VERSION_PROPERTY, "0.5.0");
    }

    /**
     * check
     *
     * @return whether the edc version is less than 23.09
     */
    public boolean isPrerelease() {
        return getEdcVersion().compareTo("0.5.0") <= 0;
    }

    /**
     * access
     *
     * @return URL for Matchmaking Agent REST call
     */
    public String getMatchmakingAgentUrl() {
        return config.getString(MATCHMAKING_URL, null);
    }

}
