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

import jakarta.json.JsonObject;
import org.eclipse.tractusx.agents.edc.jsonld.JsonLdObject;

/**
 * Result of a contract negotiation
 */
public class ContractNegotiation extends JsonLdObject {

    public ContractNegotiation(JsonObject node) {
        super(node);
    }

    public String getContractAgreementId() {
        return getAgreementId();
    }

    public String getState() {
        return getEdrState();
    }

    public String getAgreementId() {
        return object.getString("https://w3id.org/edc/v0.0.1/ns/agreementId", "UNKNOWN");
    }

    public String getTransferProcessId() {
        return object.getString("https://w3id.org/edc/v0.0.1/ns/transferProcessId", "UNKNOWN");
    }

    public String getAssetId() {
        return object.getString("https://w3id.org/edc/v0.0.1/ns/assetId", "UNKNOWN");
    }

    public String getProviderId() {
        return object.getString("https://w3id.org/edc/v0.0.1/ns/providerId", "UNKNOWN");
    }

    public String getEdrState() {
        return object.getString("https://w3id.org/tractusx/v0.0.1/ns/edrState", "UNKNOWN");
    }

    public long getExpirationDate() {
        return object.getJsonNumber("https://w3id.org/tractusx/v0.0.1/ns/expirationDate").longValue();
    }

    public String getErrorDetail() {
        return object.getString("https://w3id.org/edc/v0.0.1/ns/errorDetail", null);
    }
}
