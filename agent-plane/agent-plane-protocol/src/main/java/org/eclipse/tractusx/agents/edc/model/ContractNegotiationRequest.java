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

public class ContractNegotiationRequest {

    private String connectorAddress;
    private String protocol = "dataspace-protocol-http";
    private String connectorId;

    private String localBusinessPartnerNumber;
    private String remoteBusinessPartnerNumber;
    private ContractOfferDescription offer;

    private ContractNegotiationRequest() {
    }


    public String getLocalBusinessPartnerNumber() {
        return localBusinessPartnerNumber;
    }

    public String getRemoteBusinessPartnerNumber() {
        return remoteBusinessPartnerNumber;
    }

    public String getConnectorAddress() {
        return connectorAddress;
    }

    public String getProtocol() {
        return protocol;
    }

    public String getConnectorId() {
        return connectorId;
    }

    public ContractOfferDescription getOffer() {
        return offer;
    }


    public static final class Builder {
        private final ContractNegotiationRequest dto;

        private Builder() {
            dto = new ContractNegotiationRequest();
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder connectorAddress(String connectorAddress) {
            dto.connectorAddress = connectorAddress;
            return this;
        }

        public Builder protocol(String protocol) {
            dto.protocol = protocol;
            return this;
        }

        public Builder connectorId(String connectorId) {
            dto.connectorId = connectorId;
            return this;
        }

        public Builder offerId(ContractOfferDescription offerId) {
            dto.offer = offerId;
            return this;
        }

        public Builder localBusinessPartnerNumber(String bpn) {
            dto.localBusinessPartnerNumber=bpn;
            return this;
        }

        public Builder remoteBusinessPartnerNumber(String bpn) {
            dto.remoteBusinessPartnerNumber=bpn;
            return this;
        }

        public ContractNegotiationRequest build() {
            return dto;
        }
    }
}
