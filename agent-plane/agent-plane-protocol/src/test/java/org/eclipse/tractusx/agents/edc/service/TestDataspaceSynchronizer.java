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
package org.eclipse.tractusx.agents.edc.service;

import jakarta.json.Json;
import jakarta.json.JsonObjectBuilder;
import org.eclipse.tractusx.agents.edc.TestConfig;
import org.eclipse.tractusx.agents.edc.jsonld.JsonLd;
import org.eclipse.tractusx.agents.edc.model.DcatCatalog;
import org.eclipse.tractusx.agents.edc.model.DcatDataset;
import org.eclipse.tractusx.agents.edc.rdf.RdfStore;
import okhttp3.*;
import org.apache.jena.graph.Node;
import org.apache.jena.sparql.core.Quad;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.tractusx.agents.edc.AgentConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import org.eclipse.edc.spi.monitor.ConsoleMonitor;
import org.apache.jena.graph.NodeFactory;


import org.mockito.MockitoAnnotations;

import java.util.Collection;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;

/**
 * Tests the dataspace synchronization
 */
public class TestDataspaceSynchronizer {

    ConsoleMonitor monitor = new ConsoleMonitor();
    TestConfig config = new TestConfig();
    AgentConfig agentConfig = new AgentConfig(monitor, config);
    OkHttpClient client = new OkHttpClient();
    ScheduledExecutorService threadedExecutor = Executors.newSingleThreadScheduledExecutor();
    RdfStore store = new RdfStore(agentConfig, monitor);

    TypeManager typeManager = new TypeManager();

    DataManagement dm = new DataManagement(monitor, typeManager, client, agentConfig);
    DataspaceSynchronizer synchronizer = new DataspaceSynchronizer(threadedExecutor, agentConfig, dm, store, monitor);

    AutoCloseable mocks = null;

    @BeforeEach
    public void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    public void tearDown() throws Exception {
        if (mocks != null) {
            mocks.close();
            mocks = null;
        }
    }

    /**
     * test quad representation of a contract offer
     */
    @Test
    public void testQuadRepresentation() {
        Node graph = store.getDefaultGraph();
        Node connector = NodeFactory.createURI("edc://test");
        JsonObjectBuilder offerBuilder = Json.createObjectBuilder()
                .add("@id", "4bf62562-9026-4dcf-93b5-42ea0de25490")
                .add("https://w3id.org/edc/v0.0.1/ns/id", "https://w3id.org/catenax/ontology/common#GraphAsset?test:ExampleAsset")
                .add("https://w3id.org/edc/v0.0.1/ns/contenttype", "application/json, application/xml")
                .add("https://w3id.org/edc/v0.0.1/ns/version", "1.10.2-SNAPSHOT")
                .add("https://w3id.org/edc/v0.0.1/ns/name", "Test Asset")
                .add("https://w3id.org/edc/v0.0.1/ns/description", "Test Asset for RDF Representation")
                .add("https://w3id.org/catenax/ontology/common#publishedUnderContract", "<https://w3id.org/catenax/ontology/common#Contract?test:Contract>")
                .add("http://www.w3.org/1999/02/22-rdf-syntax-ns#type", "cx-common:GraphAsset")
                .add("http://www.w3.org/2000/01/rdf-schema#isDefinedBy", "<https://w3id.org/catenax/ontology/diagnosis>,<https://w3id.org/catenax/ontology/part>")
                .add("https://w3id.org/catenax/ontology/common#implementsProtocol", "cx-common:Protocol?w3c:http:SPARQL")
                .add("http://www.w3.org/ns/shacl#shapesGraph",
                        "@prefix : <https://w3id.org/catenax/ontology/common#GraphAsset?test:ExampleAsset> .\n" +
                                "@prefix cx-common: <https://w3id.org/catenax/ontology/common#> .\n" +
                                "@prefix cx-part: <https://w3id.org/catenax/ontology/part#> .\n" +
                                "@prefix cx-diag: <https://w3id.org/catenax/ontology/diagnosis#> .\n" +
                                "@prefix owl: <http://www.w3.org/2002/07/owl#> .\n" +
                                "@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\n" +
                                "@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\n" +
                                "@prefix sh: <http://www.w3.org/ns/shacl#> .\n" +
                                ":OemDTC rdf:type sh:NodeShape ;\n" +
                                "  sh:targetClass cx-diag:DTC ;\n" +
                                "  sh:property [\n" +
                                "        sh:path cx-diag:provisionedBy ;\n" +
                                "        sh:hasValue <urn:bpn:legal:BPNL00000003COJN> ;\n" +
                                "    ] ;\n" +
                                "  sh:property [\n" +
                                "        sh:path cx-diag:version ;\n" +
                                "        sh:hasValue \"0\"^^xsd:long ;\n    ] ;\n" +
                                "  sh:property [\n" +
                                "        sh:path cx-diag:affects ;\n" +
                                "        sh:class :OemDiagnosedParts ;\n    ] .\n" +
                                ":OemDiagnosedParts rdf:type sh:NodeShape ;\n" +
                                "  sh:targetClass cx-part:Part ;\n" +
                                "  sh:property [\n" +
                                "        sh:path cx-part:provisionedBy ;\n" +
                                "        sh:hasValue <urn:bpn:legal:BPNL00000003COJN> ;\n" +
                                "    ] .\n")
                .add("https://w3id.org/catenax/ontology/common#isFederated", "true");
        DcatDataset offer = new DcatDataset(offerBuilder.build());
        Collection<Quad> result = synchronizer.convertToQuads(graph, connector, offer);
        assertEquals(1 + 12 + 16, result.size(), "Got correct number of quads (1 connector subject and 12 asset subjects + 16 shape triples).");
    }

    @Test
    public void testCatalogDeserialization() {
        Node graph = store.getDefaultGraph();
        Node connector = NodeFactory.createURI("edc://test");
        String catDesc = "{\n" +
                "    \"@id\": \"7291eca6-410d-452d-b7b2-96d292a0aea1\",\n" +
                "    \"@type\": \"dcat:Catalog\",\n" +
                "    \"dcat:dataset\": [\n" +
                "        {\n" +
                "            \"@id\": \"9cb29be7-e0fa-45f4-b575-eb1ec98e7562\",\n" +
                "            \"@type\": \"dcat:Dataset\",\n" +
                "            \"odrl:hasPolicy\": {\n" +
                "                \"@id\": \"https://w3id.org/catenax/ontology/common#Contract?oem:GraphContract:https://w3id.org/catenax/ontology/common#GraphAsset?oem:Diagnosis2022:3e247899-e690-43a9-9c42-91c5a853a78b\",\n" +
                "                \"@type\": \"odrl:Set\",\n" +
                "                \"odrl:permission\": {\n" +
                "                    \"odrl:target\": \"https://w3id.org/catenax/ontology/common#GraphAsset?oem:Diagnosis2022\",\n" +
                "                    \"odrl:action\": {\n" +
                "                        \"odrl:type\": \"USE\"\n" +
                "                    },\n" +
                "                    \"odrl:constraint\": {\n" +
                "                        \"odrl:or\": [\n" +
                "                            {\n" +
                "                                \"odrl:leftOperand\": \"BusinessPartnerNumber\",\n" +
                "                                \"odrl:operator\": \"EQ\",\n" +
                "                                \"odrl:rightOperand\": \"BPNL00000003CPIY\"\n" +
                "                            },\n" +
                "                            {\n" +
                "                                \"odrl:leftOperand\": \"BusinessPartnerNumber\",\n" +
                "                                \"odrl:operator\": \"EQ\",\n" +
                "                                \"odrl:rightOperand\": \"BPNL00000003CQI9\"\n" +
                "                            },\n" +
                "                            {\n" +
                "                                \"odrl:leftOperand\": \"BusinessPartnerNumber\",\n" +
                "                                \"odrl:operator\": \"EQ\",\n" +
                "                                \"odrl:rightOperand\": \"BPNL00000003COJN\"\n" +
                "                            }\n" +
                "                        ]\n" +
                "                    }\n" +
                "                },\n" +
                "                \"odrl:prohibition\": [],\n" +
                "                \"odrl:obligation\": [],\n" +
                "                \"odrl:target\": \"https://w3id.org/catenax/ontology/common#GraphAsset?oem:Diagnosis2022\"\n" +
                "            },\n" +
                "            \"dcat:distribution\": {\n" +
                "                \"@type\": \"dcat:Distribution\",\n" +
                "                \"dct:format\": {\n" +
                "                    \"@id\": \"HttpProxy\"\n" +
                "                },\n" +
                "                \"dcat:accessService\": \"ddd4b79e-f785-4e71-9fe5-4a177b3ccf54\"\n" +
                "            },\n" +
                "            \"edc:version\": \"1.10.2-SNAPSHOT\",\n" +
                "            \"http://www.w3.org/2000/01/rdf-schema#isDefinedBy\": \"<https://w3id.org/catenax/ontology/diagnosis>\",\n" +
                "            \"edc:name\": \"Diagnostic Trouble Code Catalogue Version 2022\",\n" +
                "            \"http://www.w3.org/ns/shacl#shapeGraph\": \"@prefix cx-common: <https://w3id.org/catenax/ontology/common#>. \\n@prefix : <https://w3id.org/catenax/ontology/common#GraphAsset?oem:Diagnosis2022> .\\n@prefix cx-diag: <https://w3id.org/catenax/ontology/diagnosis#> .\\n@prefix owl: <http://www.w3.org/2002/07/owl#> .\\n@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\\n@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\\n@prefix sh: <http://www.w3.org/ns/shacl#> .\\n\\n:OemDTC rdf:type sh:NodeShape ;\\n  sh:targetClass cx-diag:DTC ;\\n  sh:property [\\n        sh:path cx-diag:provisionedBy ;\\n        sh:hasValue <urn:bpn:legal:BPNL00000003COJN> ;\\n    ] ;\\n  sh:property [\\n        sh:path cx-diag:version ;\\n        sh:hasValue 0^^xsd:long ;\\n    ] ;\\n  sh:property [\\n        sh:path cx-diag:affects ;\\n        sh:class :OemDiagnosedParts ;\\n    ] ;\\n\\n:OemDiagnosedParts rdf:type sh:NodeShape ;\\n  sh:targetClass cx-diag:DiagnosedPart ;\\n  sh:property [\\n        sh:path cx-diag:provisionedBy ;\\n        sh:hasValue <urn:bpn:legal:BPNL00000003COJN> ;\\n    ] ;\\n\",\n" +
                "            \"edc:description\": \"A sample graph asset/offering referring to a specific diagnosis resource.\",\n" +
                "            \"https://w3id.org/catenax/ontology/common#publishedUnderContract\": \"cx-common:Contract?oem:Graph\",\n" +
                "            \"edc:id\": \"https://w3id.org/catenax/ontology/common#GraphAsset?oem:Diagnosis2022\",\n" +
                "            \"edc:contenttype\": \"application/json, application/xml\",\n" +
                "            \"https://w3id.org/catenax/ontology/common#isFederated\": \"true\",\n" +
                "            \"https://w3id.org/catenax/ontology/common#implementsProtocol\": \"cx-common:Protocol?w3c:http:SPARQL\",\n" +
                "            \"http://www.w3.org/1999/02/22-rdf-syntax-ns#type\": \"cx-common:GraphAsset\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"@id\": \"f3cbefa3-3ef9-464b-8f99-e342b578e182\",\n" +
                "            \"@type\": \"dcat:Dataset\",\n" +
                "            \"odrl:hasPolicy\": {\n" +
                "                \"@id\": \"https://w3id.org/catenax/ontology/common#Contract?oem:GraphContract:https://w3id.org/catenax/ontology/common#GraphAsset?oem:BehaviourTwin:5d12f720-29dd-40d4-b9ef-f0b81c9740b4\",\n" +
                "                \"@type\": \"odrl:Set\",\n" +
                "                \"odrl:permission\": {\n" +
                "                    \"odrl:target\": \"https://w3id.org/catenax/ontology/common#GraphAsset?oem:BehaviourTwin\",\n" +
                "                    \"odrl:action\": {\n" +
                "                        \"odrl:type\": \"USE\"\n" +
                "                    },\n" +
                "                    \"odrl:constraint\": {\n" +
                "                        \"odrl:or\": [\n" +
                "                            {\n" +
                "                                \"odrl:leftOperand\": \"BusinessPartnerNumber\",\n" +
                "                                \"odrl:operator\": \"EQ\",\n" +
                "                                \"odrl:rightOperand\": \"BPNL00000003CPIY\"\n" +
                "                            },\n" +
                "                            {\n" +
                "                                \"odrl:leftOperand\": \"BusinessPartnerNumber\",\n" +
                "                                \"odrl:operator\": \"EQ\",\n" +
                "                                \"odrl:rightOperand\": \"BPNL00000003CQI9\"\n" +
                "                            },\n" +
                "                            {\n" +
                "                                \"odrl:leftOperand\": \"BusinessPartnerNumber\",\n" +
                "                                \"odrl:operator\": \"EQ\",\n" +
                "                                \"odrl:rightOperand\": \"BPNL00000003COJN\"\n" +
                "                            }\n" +
                "                        ]\n" +
                "                    }\n" +
                "                },\n" +
                "                \"odrl:prohibition\": [],\n" +
                "                \"odrl:obligation\": [],\n" +
                "                \"odrl:target\": \"https://w3id.org/catenax/ontology/common#GraphAsset?oem:BehaviourTwin\"\n" +
                "            },\n" +
                "            \"dcat:distribution\": {\n" +
                "                \"@type\": \"dcat:Distribution\",\n" +
                "                \"dct:format\": {\n" +
                "                    \"@id\": \"HttpProxy\"\n" +
                "                },\n" +
                "                \"dcat:accessService\": \"ddd4b79e-f785-4e71-9fe5-4a177b3ccf54\"\n" +
                "            },\n" +
                "            \"edc:version\": \"CX_RuL_Testdata_v1.0.0\",\n" +
                "            \"http://www.w3.org/2000/01/rdf-schema#isDefinedBy\": \"<https://w3id.org/catenax/ontology/telematics>\",\n" +
                "            \"edc:name\": \"OEM portion of the Behaviour Twin RUL/HI Testdataset.\",\n" +
                "            \"http://www.w3.org/ns/shacl#shapeGraph\": \"@prefix cx-common: <https://w3id.org/catenax/ontology/common#>. \\n@prefix : <https://w3id.org/catenax/ontology/common#GraphAsset?oem:BehaviourTwin> .\\n@prefix cx-tele: <https://w3id.org/catenax/ontology/telematics#> .\\n@prefix owl: <http://www.w3.org/2002/07/owl#> .\\n@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\\n@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\\n@prefix sh: <http://www.w3.org/ns/shacl#> .@prefix : <urn:cx:Graph:oem:BehaviourTwin> .\\n\\nOemLoadSpectrum rdf:type sh:NodeShape ;\\n  sh:targetClass cx-tele:LoadSpectrum ;\\n  sh:property [\\n        sh:path cx-tele:provisionedBy ;\\n        sh:hasValue <urn:bpn:legal:BPNL00000003AYRE> ;\\n    ] ;\\n  sh:property [\\n        sh:path cx-tele:Version ;\\n        sh:hasValue 0^^xsd:long ;\\n    ] ;\\n  sh:property [\\n        sh:path cx-tele:component ;\\n        sh:class SupplierParts ;\\n    ] ;\\n\\nSupplierParts rdf:type sh:NodeShape ;\\n  sh:targetClass cx-tele:VehicleComponent ;\\n  sh:property [\\n        sh:path cx-tele:isProducedBy ;\\n        sh:hasValue <urn:bpn:legal:BPNL00000003B2OM> ;\\n    ] ;\\n\",\n" +
                "            \"edc:description\": \"A graph asset/offering mounting Carena-X Testdata for Behaviour Twin.\",\n" +
                "            \"https://w3id.org/catenax/ontology/common#publishedUnderContract\": \"cx-common:Contract?oem:Graph\",\n" +
                "            \"edc:id\": \"https://w3id.org/catenax/ontology/common#GraphAsset?oem:BehaviourTwin\",\n" +
                "            \"edc:contenttype\": \"application/json, application/xml\",\n" +
                "            \"https://w3id.org/catenax/ontology/common#isFederated\": \"true\",\n" +
                "            \"https://w3id.org/catenax/ontology/common#implementsProtocol\": \"cx-common:Protocol?w3c:http:SPARQL\",\n" +
                "            \"http://www.w3.org/1999/02/22-rdf-syntax-ns#type\": \"cx-common:GraphAsset\"\n" +
                "        }\n" +
                "    ],\n" +
                "    \"dcat:service\": {\n" +
                "        \"@id\": \"ddd4b79e-f785-4e71-9fe5-4a177b3ccf54\",\n" +
                "        \"@type\": \"dcat:DataService\",\n" +
                "        \"dct:terms\": \"connector\",\n" +
                "        \"dct:endpointUrl\": \"http://localhost:8282/api/v1/dsp\"\n" +
                "    },\n" +
                "    \"edc:participantId\": \"anonymous\",\n" +
                "    \"@context\": {\n" +
                "        \"dct\": \"https://purl.org/dc/terms/\",\n" +
                "        \"tx\": \"https://w3id.org/tractusx/v0.0.1/ns/\",\n" +
                "        \"edc\": \"https://w3id.org/edc/v0.0.1/ns/\",\n" +
                "        \"dcat\": \"https://www.w3.org/ns/dcat/\",\n" +
                "        \"odrl\": \"http://www.w3.org/ns/odrl/2/\",\n" +
                "        \"dspace\": \"https://w3id.org/dspace/v0.8/\"\n" +
                "    }\n" +
                "}";
        DcatCatalog cat = JsonLd.processCatalog(catDesc);
        Collection<Quad> results = cat.getDatasets().stream().flatMap(offer -> synchronizer.convertToQuads(graph, connector, offer).stream()).collect(Collectors.toList());
    }

}