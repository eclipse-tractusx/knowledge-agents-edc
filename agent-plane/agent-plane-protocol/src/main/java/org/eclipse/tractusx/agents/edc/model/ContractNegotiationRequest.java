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
