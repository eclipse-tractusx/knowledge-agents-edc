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
package org.eclipse.tractusx.agents.edc.rdf;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.sparql.core.Quad;
import org.eclipse.edc.spi.monitor.ConsoleMonitor;
import org.eclipse.tractusx.agents.edc.AgentConfig;
import org.eclipse.tractusx.agents.edc.TestConfig;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests the rdf store
 */
public class TestRdfStore {

    ConsoleMonitor monitor = new ConsoleMonitor();
    TestConfig config = new TestConfig();
    AgentConfig agentConfig = new AgentConfig(monitor, config);
    RDFStore store = new RDFStore(agentConfig, monitor);

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
    public void testImportTurtleSuccess() {
        String turtle = getTestTurtle();
        long noTuples=store.registerAsset("GraphAsset?consumer=Upload", turtle, ExternalFormat.TURTLE);
        assertEquals(4, noTuples,"Inserted the correct number of tuples");
        checkAsset(noTuples);
    }

    @NotNull
    public static String getTestTurtle() {
        String turtle =
                "@prefix : <GraphAsset?consumer=Upload#> .\n" +
                "@prefix cx-supply: <https://w3id.org/catenax/ontology/supply-chain#> .\n" +
                "@prefix cx-mat: <https://w3id.org/catenax/taxonomy/material#> .\n" +
                "@prefix bpns: <urn:bpn:site:> .\n" +
                "@prefix bpna: <urn:bpn:address:> .\n" +
                "@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\n" +
                "\n" +
                ":Chain1 rdf:type cx-supply:SupplyRelationship;\n" +
                "        cx-supply:material cx-mat:Rubber;\n" +
                "        cx-supply:consumer bpna:BPNL00000003COJN;\n" +
                "        cx-supply:supplier bpns:BPNL00000003CPIY.\n";
        return turtle;
    }

    /**
     * test quad representation of a contract offer
     */
    @Test
    public void testImportCsvSuccess() {
        String csv = getTestCsv();
        long noTuples=store.registerAsset("GraphAsset?consumer=Upload", csv, ExternalFormat.CSV);
        assertEquals(11, noTuples,"Inserted the correct number of tuples");
        checkAsset(noTuples);
    }

    @NotNull
    public static String getTestCsv() {
        String curDir=new File("test").getAbsoluteFile().getParentFile().getAbsolutePath();
        String csv = "IRI,http://www.w3.org/1999/02/22-rdf-syntax-ns#type,https://w3id.org/catenax/ontology/supply-chain#material,https://w3id.org/catenax/ontology/supply-chain#consumer,https://w3id.org/catenax/ontology/supply-chain#supplier\n" +
                     String.format("file://%s/GraphAsset?consumer=Upload#Chain1,<https://w3id.org/catenax/ontology/supply-chain#SupplyRelationship>,<https://w3id.org/catenax/taxonomy/material#Rubber>,<urn:bpn:address:BPNL00000003COJN>,<urn:bpn:site:BPNL00000003CPIY>\n",curDir)+
                     String.format("file://%s/GraphAsset?consumer=Upload#Chain2,<https://w3id.org/catenax/ontology/supply-chain#SupplyRelationship>,<https://w3id.org/catenax/taxonomy/material#Rubber>,<urn:bpn:address:BPNL00000003COJN>,<urn:bpn:site:BPNL00000003CPIY>,toomuch\n",curDir)+
                     String.format("file://%s/GraphAsset?consumer=Upload#Chain3,<https://w3id.org/catenax/ontology/supply-chain#SupplyRelationship>,<https://w3id.org/catenax/taxonomy/material#Rubber>,<urn:bpn:address:BPNL00000003COJN>\n",curDir);
        return csv;
    }

    /**
     * checks the target asset
     * @param noTuples number of tuples in the asset
     */
    protected void checkAsset(long noTuples) {
        Node graphUri=NodeFactory.createURI("http://server/unset-base/GraphAsset?consumer=Upload");
        String curDir=new File("test").getAbsoluteFile().getParentFile().getAbsolutePath();
        Node subjectUri=NodeFactory.createURI(String.format("file://%s/GraphAsset?consumer=Upload#Chain1",curDir));
        Node rdfType=NodeFactory.createURI("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
        Node supplyMaterial=NodeFactory.createURI("https://w3id.org/catenax/ontology/supply-chain#material");
        Node supplyConsumer=NodeFactory.createURI("https://w3id.org/catenax/ontology/supply-chain#consumer");
        Node supplySupplier=NodeFactory.createURI("https://w3id.org/catenax/ontology/supply-chain#supplier");
        checkRelation(graphUri, subjectUri, rdfType, "https://w3id.org/catenax/ontology/supply-chain#SupplyRelationship");
        checkRelation(graphUri, subjectUri, supplyMaterial, "https://w3id.org/catenax/taxonomy/material#Rubber");
        checkRelation(graphUri, subjectUri, supplyConsumer,  "urn:bpn:address:BPNL00000003COJN");
        checkRelation(graphUri, subjectUri, supplySupplier, "urn:bpn:site:BPNL00000003CPIY");
        long noDeletedTuples =store.deleteAsset("GraphAsset?consumer=Upload");
        assertEquals(noTuples, noDeletedTuples,"Deleted the correct number of tuples");
        assertEquals(false,store.getDataSet().find(new Quad(graphUri,Node.ANY,Node.ANY,Node.ANY)).hasNext(),"Graph asset emptied.");
    }

    /**
     * check whether the target relation exists and has the right object
     * @param graphUri graph hosting the relation
     * @param subjectUri subject
     * @param predicate to test
     * @param objectUri expected object
     */
    protected void checkRelation(Node graphUri, Node subjectUri, Node predicate, String objectUri) {
        Iterator<Quad> quads= store.getDataSet().find(new Quad(graphUri, subjectUri, predicate, Node.ANY));
        assertEquals(true,quads.hasNext(),String.format("Found the %s quad",predicate));
        Quad quad=quads.next();
        assertEquals(objectUri,quad.getObject().getURI(),String.format("Got the object for %s", predicate));
        assertEquals(false,quads.hasNext(),String.format("Found the correct relation %s", predicate));
    }


}