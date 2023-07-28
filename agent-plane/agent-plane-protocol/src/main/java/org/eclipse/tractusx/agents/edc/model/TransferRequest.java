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
package org.eclipse.tractusx.agents.edc.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.callback.CallbackAddress;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonDeserialize(builder = TransferRequest.Builder.class)
public class TransferRequest {

    private String id;
    private String connectorAddress; // TODO change to callbackAddress
    private String contractId;
    private DataAddress dataDestination;
    private boolean managedResources = true;
    private Map<String, String> properties = new HashMap<>();

    private Map<String, String> privateProperties = new HashMap<>();

    private String protocol;
    private String connectorId;
    private String providerId;
    private String assetId;

    private List<CallbackAddress> callbackAddresses = new ArrayList<>();


    public String getConnectorAddress() {
        return connectorAddress;
    }

    public String getId() {
        return id;
    }

    public String getContractId() {
        return contractId;
    }

    public DataAddress getDataDestination() {
        return dataDestination;
    }

    public boolean isManagedResources() {
        return managedResources;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public Map<String, String> getPrivateProperties() {
        return privateProperties;
    }

    public String getProtocol() {
        return protocol;
    }

    public String getConnectorId() {
        return connectorId;
    }

    public String getProviderId() {
        return connectorId;
    }

    public String getAssetId() {
        return assetId;
    }

    public List<CallbackAddress> getCallbackAddresses() {
        return callbackAddresses;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {
        private final TransferRequest request;

        private Builder() {
            request = new TransferRequest();
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder connectorAddress(String connectorAddress) {
            request.connectorAddress = connectorAddress;
            return this;
        }

        public Builder id(String id) {
            request.id = id;
            return this;
        }

        public Builder contractId(String contractId) {
            request.contractId = contractId;
            return this;
        }

        public Builder dataDestination(DataAddress dataDestination) {
            request.dataDestination = dataDestination;
            return this;
        }

        public Builder managedResources(boolean managedResources) {
            request.managedResources = managedResources;
            return this;
        }

        public Builder properties(Map<String, String> properties) {
            request.properties = properties;
            return this;
        }

        public Builder privateProperties(Map<String, String> privateProperties) {
            request.privateProperties = privateProperties;
            return this;
        }

        public Builder protocol(String protocol) {
            request.protocol = protocol;
            return this;
        }

        public Builder connectorId(String connectorId) {
            request.connectorId = connectorId;
            return this;
        }

        public Builder providerId(String providerId) {
            request.providerId = providerId;
            return this;
        }

        public Builder assetId(String assetId) {
            request.assetId = assetId;
            return this;
        }

        public Builder callbackAddresses(List<CallbackAddress> callbackAddresses) {
            request.callbackAddresses = callbackAddresses;
            return this;
        }

        public TransferRequest build() {
            return request;
        }
    }
}
