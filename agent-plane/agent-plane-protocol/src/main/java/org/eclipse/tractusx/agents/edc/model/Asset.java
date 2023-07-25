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

import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import org.eclipse.tractusx.agents.edc.jsonld.JsonLdObject;

import java.util.Map;

/**
 * represents an asset
 */
public class Asset extends JsonLdObject {

    Map<String, JsonValue> publicProperties;
    Map<String, JsonValue> privateProperties;

    public Asset(JsonObject node) {
        super(node);
        this.publicProperties=node.getJsonObject("https://w3id.org/edc/v0.0.1/ns/properties");
        this.privateProperties=node.getJsonObject("https://w3id.org/edc/v0.0.1/ns/privateProperties");
    }

    public Map<String, JsonValue> getPrivateProperties() {
        return privateProperties;
    }

    public Map<String, JsonValue> getPublicProperties() {
        return publicProperties;
    }

}
