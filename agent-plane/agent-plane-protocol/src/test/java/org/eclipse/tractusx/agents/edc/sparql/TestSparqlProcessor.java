// Copyright (c) 2022,2024 Contributors to the Eclipse Foundation
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
package org.eclipse.tractusx.agents.edc.sparql;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.apache.jena.sparql.service.ServiceExecutorRegistry;
import org.eclipse.edc.json.JacksonTypeManager;
import org.eclipse.edc.spi.monitor.ConsoleMonitor;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.tractusx.agents.edc.*;
import org.eclipse.tractusx.agents.edc.rdf.RdfStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the sparql processor
 */
public class TestSparqlProcessor {
    
    ConsoleMonitor monitor=new ConsoleMonitor();
    TestConfig config=new TestConfig();
    AgentConfig agentConfig=new AgentConfig(monitor,config);
    ServiceExecutorRegistry serviceExecutorReg=new ServiceExecutorRegistry();
    OkHttpClient client=new OkHttpClient();
    AgreementController mockController = new MockAgreementController();
    ExecutorService threadedExecutor= Executors.newSingleThreadExecutor();
    TypeManager typeManager = new JacksonTypeManager();
    DataspaceServiceExecutor exec=new DataspaceServiceExecutor(monitor,mockController,agentConfig,client,threadedExecutor,typeManager);
    RdfStore store = new RdfStore(agentConfig,monitor);

    SparqlQueryProcessor processor=new SparqlQueryProcessor(serviceExecutorReg,monitor,agentConfig,store, typeManager);

    AutoCloseable mocks=null;

    @BeforeEach
    public void setUp()  {
        mocks=MockitoAnnotations.openMocks(this);
        //serviceExecutorReg.add(exec);
        serviceExecutorReg.addBulkLink(exec);
    }

    @AfterEach
    public void tearDown() throws Exception {
        if(mocks!=null) {
            mocks.close();
            mocks=null;
            serviceExecutorReg.remove(exec);
            serviceExecutorReg.removeBulkLink(exec);
        }
    }

    ObjectMapper mapper=new ObjectMapper();

    /**
     * test federation call - will only work with a local oem provider running
     * @throws IOException in case of an error
     */
    @Test
    @Tag("online")
    public void testFederatedGraph() throws IOException {
        String query="PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> SELECT ?what WHERE { SERVICE<http://localhost:8898/match> { " +
                "GRAPH <urn:cx:Graph:4711> { VALUES (?subject) { (<urn:cx:AnonymousSerializedPart#GB4711>)} } } }";
        Request.Builder builder=new Request.Builder();
        builder.url("http://localhost:8080");
        builder.addHeader("Accept","application/json");
        builder.put(RequestBody.create(query, MediaType.parse("application/sparql-query")));
        try (Response response=processor.execute(builder.build(),null,null,Map.of())) {
            JsonNode root = mapper.readTree(Objects.requireNonNull(response.body()).string());
            JsonNode whatBinding0 = root.get("results").get("bindings").get(0).get("subject");
            assertEquals("urn:cx:AnonymousSerializedPart#GB4711", whatBinding0.get("value").asText(), "Correct binding");
        }
    }

    /**
     * test federation call - will only work with a local oem provider running
     * @throws IOException in case of an error
     */
    @Test
    @Tag("online")
    public void testFederatedServiceChain() throws IOException {
        String query="PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> SELECT ?subject WHERE { VALUES (?chain1) { (<http://localhost:8898/match#urn:cx:Graph:1>)} SERVICE ?chain1 { " +
                "VALUES (?chain2) { (<http://localhost:8098/match>)} SERVICE ?chain2 { VALUES (?subject) { (<urn:cx:AnonymousSerializedPart#GB4711>)} } } }";
        Request.Builder builder=new Request.Builder();
        builder.url("http://localhost:8080");
        builder.addHeader("Accept","application/sparql-results+json");
        builder.put(RequestBody.create(query, MediaType.parse("application/sparql-query")));
        try(Response response=processor.execute(builder.build(),null,null,Map.of())) {
            assertTrue(response.isSuccessful(), "Response was successful");
            JsonNode root = mapper.readTree(Objects.requireNonNull(response.body()).string());
            JsonNode whatBinding0 = root.get("results").get("bindings").get(0).get("subject");
            assertEquals("urn:cx:AnonymousSerializedPart#GB4711", whatBinding0.get("value").asText(), "Correct binding");
        }
    }

    /**
     * test remote call with non-existing target
     * @throws IOException in case of an error
     */
    @Test
    public void testRemoteError() throws IOException {
        String query="PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> SELECT ?what WHERE { SERVICE <http://does-not-resolve/sparql#urn:cx:Graph:1> { VALUES (?what) { (\"42\"^^xsd:int) } } }";
        Request.Builder builder=new Request.Builder();
        builder.url("http://localhost:8080");
        builder.addHeader("Accept","application/sparql-results+json");
        builder.put(RequestBody.create(query, MediaType.parse("application/sparql-query")));
        try(Response response=processor.execute(builder.build(),null,null,Map.of())) {
            assertTrue(response.isSuccessful(), "Response was successful");
            JsonNode root = mapper.readTree(Objects.requireNonNull(response.body()).string());
            assertEquals(0, root.get("results").get("bindings").size());
            String warnings = response.header("cx_warnings");
            JsonNode warningsJson = mapper.readTree(warnings);
            assertEquals(1, warningsJson.size(), "got remote warnings");
        }
    }

    /**
     * test remote call with matchmaking agent
     * @throws IOException in case of an error
     */
    @Test
    @Tag("online")
    public void testRemoteWarning() throws IOException {
        String query="PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> SELECT ?what WHERE { SERVICE <http://localhost:8898/match?asset=urn%3Acx%3AGraphAsset%23Test> { VALUES (?what) { (\"42\"^^xsd:int) } } }";
        Request.Builder builder=new Request.Builder();
        builder.url("http://localhost:8080");
        builder.addHeader("Accept","application/sparql-results+json");
        builder.put(RequestBody.create(query, MediaType.parse("application/sparql-query")));
        try(Response response=processor.execute(builder.build(),null,null,Map.of())) {
            assertTrue(response.isSuccessful(), "Response was successful");
            JsonNode root = mapper.readTree(Objects.requireNonNull(response.body()).string());
            assertEquals(1, root.get("results").get("bindings").size());
            String warnings = response.header("cx_warnings");
            JsonNode warningsJson = mapper.readTree(warnings);
            assertEquals(1, warningsJson.size(), "got remote warnings");
        }
    }

    /**
     * test remote call with matchmaking agent
     * @throws IOException in case of an error
     */
    @Test
    @Tag("online")
    public void testRemoteTransfer() throws IOException {
        String query="PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> SELECT ?what WHERE { SERVICE <http://localhost:8898/match?asset=urn%3Acx%3AGraphAsset%23Test> { VALUES (?what) { (\"42\"^^xsd:int) } } }";
        Request.Builder builder=new Request.Builder();
        builder.url("http://localhost:8080");
        builder.addHeader("Accept","application/sparql-results+json");
        builder.put(RequestBody.create(query, MediaType.parse("application/sparql-query")));
        try (Response response=processor.execute(builder.build(),null,null,Map.of())) {
            assertTrue(response.isSuccessful(), "Response was successful");
            JsonNode root = mapper.readTree(Objects.requireNonNull(response.body()).string());
            assertEquals(1, root.get("results").get("bindings").size());
            String warnings = response.header("cx_warnings");
            JsonNode warningsJson = mapper.readTree(warnings);
            assertEquals(1, warningsJson.size(), "got remote warnings");
        }
    }

    /**
     * test federation call - will only work with a local oem provider running
     * @throws IOException in case of an error
     */
    @Test
    @Tag("online")
    public void testBatchFederation() throws IOException {
        String query="PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> "+
                "SELECT ?chain1 ?what ?output WHERE { " +
                "  VALUES (?chain1 ?what) { "+
                "   (<http://localhost:8080/sparql#urn:cx:Graph:1> \"42\"^^xsd:int) "+
                "   (<http://localhost:8080/sparql#urn:cx:Graph:2> \"21\"^^xsd:int) "+
                "   (<http://localhost:8080/sparql#urn:cx:Graph:1> \"84\"^^xsd:int) "+
                "  } "+
                "  SERVICE ?chain1 { " +
                "    BIND(?what as ?output) "+
                "  } "+
                "}";
        Request.Builder builder=new Request.Builder();
        builder.url("http://localhost:8080");
        builder.addHeader("Accept","application/sparql-results+json");
        builder.put(RequestBody.create(query, MediaType.parse("application/sparql-query")));
        try (Response response=processor.execute(builder.build(),null,null,Map.of())) {
            assertTrue(response.isSuccessful(), "Successful result");
            JsonNode root = mapper.readTree(Objects.requireNonNull(response.body()).string());
            JsonNode bindings = root.get("results").get("bindings");
            assertEquals(3, bindings.size(), "Correct number of result bindings.");
            JsonNode whatBinding0 = bindings.get(0).get("output");
            assertEquals("21", whatBinding0.get("value").asText(), "Correct binding");
            JsonNode whatBinding1 = bindings.get(1).get("output");
            assertEquals("42", whatBinding1.get("value").asText(), "Correct binding");
            JsonNode whatBinding2 = bindings.get(2).get("output");
            assertEquals("84", whatBinding2.get("value").asText(), "Correct binding");
        }
    }


    /**
     * test not allowed calls
     * @throws IOException in case of an error
     */
    @Test
    public void testNotAllowedService() throws IOException {
        String query="PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> "+
                "SELECT ?chain1 ?what ?output WHERE { " +
                "  VALUES (?chain1 ?what) { "+
                "   (<http://localhost:8080/sparql#urn:cx:Graph:1> \"42\"^^xsd:int) "+
                "   (<http://localhost:8080/sparql#urn:cx:Graph:2> \"21\"^^xsd:int) "+
                "   (<http://localhost:8080/sparql#urn:cx:Graph:1> \"84\"^^xsd:int) "+
                "  } "+
                "  SERVICE ?chain1 { " +
                "    BIND(?what as ?output) "+
                "  } "+
                "}";
        Request.Builder builder=new Request.Builder();
        builder.url("http://localhost:8080");
        builder.addHeader("Accept","application/sparql-results+json");
        builder.put(RequestBody.create(query, MediaType.parse("application/sparql-query")));
        try (Response response=processor.execute(builder.build(),null,null,Map.of(DataspaceServiceExecutor.ALLOW_SYMBOL.getSymbol(),"https://.*"))) {
            assertTrue(response.isSuccessful(), "Successful result");
            JsonNode root = mapper.readTree(Objects.requireNonNull(response.body()).string());
            JsonNode bindings = root.get("results").get("bindings");
            assertEquals(0, bindings.size(), "Correct number of result bindings.");
            JsonNode warnings = mapper.readTree(response.header("cx_warnings", "[]"));
            assertTrue(warnings.isArray(), "Got a warnings array");
            assertEquals(warnings.size(), 2, "Got correct service warnings number");
        }
    }

    /**
     * test not allowed calls
     * @throws IOException in case of an error
     */
    @Test
    public void testDeniedService() throws IOException {
        String query="PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> "+
                "SELECT ?chain1 ?what ?output WHERE { " +
                "  VALUES (?chain1 ?what) { "+
                "   (<http://localhost:8080/sparql#urn:cx:Graph:1> \"42\"^^xsd:int) "+
                "   (<http://localhost:8080/sparql#urn:cx:Graph:2> \"21\"^^xsd:int) "+
                "   (<http://localhost:8080/sparql#urn:cx:Graph:1> \"84\"^^xsd:int) "+
                "  } "+
                "  SERVICE ?chain1 { " +
                "    BIND(?what as ?output) "+
                "  } "+
                "}";
        Request.Builder builder=new Request.Builder();
        builder.url("http://localhost:8080");
        builder.addHeader("Accept","application/sparql-results+json");
        builder.put(RequestBody.create(query, MediaType.parse("application/sparql-query")));
        try(Response response=processor.execute(builder.build(),null,null,Map.of(DataspaceServiceExecutor.DENY_SYMBOL.getSymbol(),"http://localhost.*"))) {
            assertTrue(response.isSuccessful(), "Successful result");
            JsonNode root = mapper.readTree(Objects.requireNonNull(response.body()).string());
            JsonNode bindings = root.get("results").get("bindings");
            assertEquals(0, bindings.size(), "Correct number of result bindings.");
            JsonNode warnings = mapper.readTree(response.header("cx_warnings", "[]"));
            assertTrue(warnings.isArray(), "Got a warnings array");
            assertEquals(warnings.size(), 2, "Got correct service warnings number");
        }
    }

     /**
     * test standard allowance
     * @throws IOException in case of an error
     */
    @Test
    public void testDefaultService() throws IOException {
        String query="PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> "+
                "SELECT ?chain1 ?what ?output WHERE { " +
                "  VALUES (?chain1 ?what) { "+
                "   (<http://localhost:8080/sparql#urn:cx:Graph:1> \"42\"^^xsd:int) "+
                "   (<https://query.wikidata.org/sparql> \"21\"^^xsd:int) "+
                "   (<http://localhost:8080/sparql#urn:cx:Graph:1> \"84\"^^xsd:int) "+
                "  } "+
                "  SERVICE ?chain1 { " +
                "    BIND(?what as ?output) "+
                "  } "+
                "}";
        Request.Builder builder=new Request.Builder();
        builder.url("http://localhost:8080");
        builder.addHeader("Accept","application/sparql-results+json");
        builder.put(RequestBody.create(query, MediaType.parse("application/sparql-query")));
        try (Response response=processor.execute(builder.build(),null,null,Map.of(DataspaceServiceExecutor.ALLOW_SYMBOL.getSymbol(),"(http|edc)s://.*"))) {
            assertTrue(response.isSuccessful(), "Successful result");
            JsonNode root = mapper.readTree(Objects.requireNonNull(response.body()).string());
            JsonNode bindings = root.get("results").get("bindings");
            assertEquals(1, bindings.size(), "Correct number of result bindings.");
            JsonNode warnings = mapper.readTree(response.header("cx_warnings", "[]"));
            assertTrue(warnings.isArray(), "Got a warnings array");
            assertEquals(warnings.size(), 1, "Got correct service warnings number");
        }
    }

}
