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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.databind.deser.std.UntypedObjectDeserializer;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiation.Type;
import org.eclipse.tractusx.agents.edc.jsonld.JsonLdObject;

/**
 * Result of a contract negotiation
 */
public class ContractNegotiation extends JsonLdObject {

    public ContractNegotiation(JsonObject node) {
        super(node);
    }

    public String getContractAgreementId() {
        return object.getString("https://w3id.org/edc/v0.0.1/ns/contractAgreementId",null);
    }

    public String getState() {
        return object.getString("https://w3id.org/edc/v0.0.1/ns/state");
    }

    public String getErrorDetail() {
        return object.getString("https://w3id.org/edc/v0.0.1/ns/errorDetail",null);
    }
}
