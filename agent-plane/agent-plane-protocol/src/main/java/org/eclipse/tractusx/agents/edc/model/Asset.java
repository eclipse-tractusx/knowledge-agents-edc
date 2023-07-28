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
