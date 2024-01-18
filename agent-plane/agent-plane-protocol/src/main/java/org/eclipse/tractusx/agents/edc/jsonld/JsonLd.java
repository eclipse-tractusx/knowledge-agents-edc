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
package org.eclipse.tractusx.agents.edc.jsonld;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import org.eclipse.tractusx.agents.edc.model.Asset;
import org.eclipse.tractusx.agents.edc.model.ContractAgreement;
import org.eclipse.tractusx.agents.edc.model.ContractNegotiation;
import org.eclipse.tractusx.agents.edc.model.DcatCatalog;
import org.eclipse.tractusx.agents.edc.model.IdResponse;
import org.eclipse.tractusx.agents.edc.model.TransferProcess;

import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * base facility to deal with EDC specific JSONLD structures
 */
public class JsonLd {

    public static DcatCatalog processCatalog(String cat) {
        return processCatalog(Json.createReader(new StringReader(cat)).readObject());
    }

    public static DcatCatalog processCatalog(JsonObject cat) {
        return new DcatCatalog(processJsonLd(cat, null));
    }

    public static IdResponse processIdResponse(String response) {
        return processIdResponse(Json.createReader(new StringReader(response)).readObject());
    }

    public static IdResponse processIdResponse(JsonObject response) {
        return new IdResponse(processJsonLd(response, null));
    }

    public static ContractNegotiation processContractNegotiation(String response) {
        return processContractNegotiation(Json.createReader(new StringReader(response)).readObject());
    }

    public static ContractNegotiation processContractNegotiation(JsonObject response) {
        return new ContractNegotiation(processJsonLd(response, null));
    }

    public static ContractAgreement processContractAgreement(String response) {
        return processContractAgreement(Json.createReader(new StringReader(response)).readObject());
    }

    public static ContractAgreement processContractAgreement(JsonObject response) {
        return new ContractAgreement(processJsonLd(response, null));
    }

    public static TransferProcess processTransferProcess(String response) {
        return processTransferProcess(Json.createReader(new StringReader(response)).readObject());
    }

    public static TransferProcess processTransferProcess(JsonObject response) {
        return new TransferProcess(processJsonLd(response, null));
    }

    public static List<Asset> processAssetList(String response) {
        return processAssetList(Json.createReader(new StringReader(response)).readArray());
    }

    public static List<Asset> processAssetList(JsonArray response) {
        return response.stream().map(responseObject ->
                new Asset(processJsonLd(responseObject.asJsonObject(), null))
        ).collect(Collectors.toList());
    }

    public static String asString(JsonValue value) {
        if (value == null) {
            return "null";
        }
        switch (value.getValueType()) {
            case STRING:
                return ((JsonString) value).getString();
            case NUMBER:
                return ((JsonNumber) value).numberValue().toString();
            default:
                return value.toString();
        }
    }

    public static <JSONTYPE extends JsonValue> JSONTYPE processJsonLd(JSONTYPE source, Map<String, String> context) {
        switch (source.getValueType()) {
            case ARRAY:
                final JsonArrayBuilder array = Json.createArrayBuilder();
                source.asJsonArray().forEach(value -> array.add(processJsonLd(value, context)));
                return (JSONTYPE) array.build();
            case OBJECT:
                JsonObject sourceObject = source.asJsonObject();
                Map<String, String> namespaces = new HashMap<>();
                if (context != null) {
                    namespaces.putAll(context);
                }
                if (sourceObject.containsKey("@context")) {
                    for (Map.Entry<String, JsonValue> ns : sourceObject.getJsonObject("@context").entrySet()) {
                        namespaces.put(ns.getKey(), JsonLd.asString(ns.getValue()));
                    }
                }
                final JsonObjectBuilder object = Json.createObjectBuilder();
                sourceObject.forEach((prop, value) -> {
                    int colonIndex = prop.indexOf(":");
                    if (colonIndex > 0) {
                        String prefix = prop.substring(0, colonIndex);
                        if (namespaces.containsKey(prefix)) {
                            prefix = namespaces.get(prefix);
                        } else {
                            prefix = prefix + ":";
                        }
                        prop = prefix + prop.substring(colonIndex + 1);
                    }
                    object.add(prop, processJsonLd(value, namespaces));
                });
                return (JSONTYPE) object.build();
            default:
                return source;
        }
    }

}
