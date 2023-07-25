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
package org.eclipse.tractusx.agents.edc.jsonld;

import jakarta.json.JsonObject;
import jakarta.json.JsonValue;

import java.util.Map;

public class JsonLdObject {

    protected JsonObject object;

    public JsonLdObject(JsonObject object) {
        this.object=object;
    }

    public Map<String, JsonValue> getProperties() {
        return object;
    }

    public String getId() {
        return object.getString("@id");
    }

    public String asString() {
        return JsonLd.asString(object);
    }
}
