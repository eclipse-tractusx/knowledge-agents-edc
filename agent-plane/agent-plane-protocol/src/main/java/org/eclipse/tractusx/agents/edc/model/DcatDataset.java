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
import org.eclipse.tractusx.agents.edc.jsonld.JsonLdObject;

/**
 * represents a dcat data set
 */
public class DcatDataset extends JsonLdObject {

    OdrlPolicy policy;

    public DcatDataset(JsonObject node) {
        super(node);
        policy=new OdrlPolicy(node.getJsonObject("http://www.w3.org/ns/odrl/2/hasPolicy"));
    }

    public OdrlPolicy hasPolicy() {
        return policy;
    }
}



