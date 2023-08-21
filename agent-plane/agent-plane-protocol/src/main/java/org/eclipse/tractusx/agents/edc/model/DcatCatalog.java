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
 * represents a dcat catalogue
 */
public class DcatCatalog extends JsonLdObject {

    List<DcatDataset> datasets=new ArrayList<>();

    public DcatCatalog(JsonObject node) {
        super(node);
        JsonValue dataset = node.get("https://www.w3.org/ns/dcat/dataset");
        if(dataset!=null) {
            if(dataset.getValueType()== JsonValue.ValueType.ARRAY) {
                for(JsonValue ds : dataset.asJsonArray()) {
                    datasets.add(new DcatDataset(ds.asJsonObject()));
                }
            } else {
                datasets.add(new DcatDataset(dataset.asJsonObject()));
            }
        }
    }

    public String getParticipantId() {
        return object.getString("https://w3id.org/edc/v0.0.1/ns/participantId","anonymous");
    }

    public List<DcatDataset> getDatasets() {
        return datasets;
    }
}
