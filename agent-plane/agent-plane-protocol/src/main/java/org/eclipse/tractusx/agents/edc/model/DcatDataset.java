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

import java.util.ArrayList;
import java.util.List;

/**
 * represents a dcat data set
 */
public class DcatDataset extends JsonLdObject {

    List<OdrlPolicy> policies = new ArrayList<>();

    public DcatDataset(JsonObject node) {
        super(node);
        JsonValue jpolicies = node.get("http://www.w3.org/ns/odrl/2/hasPolicy");
        if (jpolicies != null) {
            if (jpolicies.getValueType() == JsonValue.ValueType.ARRAY) {
                for (JsonValue policy : jpolicies.asJsonArray()) {
                    policies.add(new OdrlPolicy(policy.asJsonObject()));
                }
            } else {
                policies.add(new OdrlPolicy(jpolicies.asJsonObject()));
            }
        }
    }

    /**
     * access default policy
     *
     * @return null, if no policy exists
     */
    public OdrlPolicy hasPolicy() {
        if (policies.isEmpty()) {
            return null;
        } else {
            return policies.get(0);
        }
    }
}



