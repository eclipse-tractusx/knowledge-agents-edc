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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import jakarta.json.JsonObject;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.tractusx.agents.edc.jsonld.JsonLdObject;

public class ContractAgreement extends JsonLdObject {

    public ContractAgreement(JsonObject node) {
        super(node);
    }

    public String getAssetId() {
        return object.getString("https://w3id.org/edc/v0.0.1/ns/assetId");
    }

    public long getContractSigningDate() {
        return object.getInt("https://w3id.org/edc/v0.0.1/ns/contractSigningDate");
    }
}