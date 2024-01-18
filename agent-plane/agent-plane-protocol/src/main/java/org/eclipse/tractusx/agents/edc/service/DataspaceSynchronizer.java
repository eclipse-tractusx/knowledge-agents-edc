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
import jakarta.json.JsonValue;
import org.apache.jena.atlas.lib.Sink;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.riot.lang.StreamRDFCounting;
import org.apache.jena.riot.system.ErrorHandler;
import org.apache.jena.riot.system.ErrorHandlerFactory;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.riot.system.StreamRDFLib;
import org.apache.jena.sparql.core.Quad;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.tractusx.agents.edc.AgentConfig;
import org.eclipse.tractusx.agents.edc.MonitorWrapper;
import org.eclipse.tractusx.agents.edc.jsonld.JsonLd;
import org.eclipse.tractusx.agents.edc.model.DcatCatalog;
import org.eclipse.tractusx.agents.edc.model.DcatDataset;
import org.eclipse.tractusx.agents.edc.rdf.RdfStore;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * A service which keeps a triple store and
 * the associated dataspace in sync
 */
public class DataspaceSynchronizer implements Runnable {

    /**
     * constants
     */
    protected static final Node CX_ASSET = NodeFactory.createURI("https://w3id.org/catenax/ontology/common#offers");
    protected static final Map<String, Node> ASSET_PROPERTY_MAP = new HashMap<>();
    protected static final QuerySpec FEDERATED_ASSET_QUERY = QuerySpec.Builder.newInstance()
            .filter(List.of(new Criterion("https://w3id.org/catenax/ontology/common#isFederated", "=", "true^^xsd:boolean"))).build();

    protected MonitorWrapper monitorWrapper;

    static {
        ASSET_PROPERTY_MAP.put("https://w3id.org/edc/v0.0.1/ns/id", NodeFactory.createURI("https://w3id.org/catenax/ontology/common#id"));
        ASSET_PROPERTY_MAP.put("https://w3id.org/edc/v0.0.1/ns/name", NodeFactory.createURI("https://w3id.org/catenax/ontology/common#name"));
        ASSET_PROPERTY_MAP.put("https://w3id.org/edc/v0.0.1/ns/description", NodeFactory.createURI("https://w3id.org/catenax/ontology/common#description"));
        ASSET_PROPERTY_MAP.put("https://w3id.org/edc/v0.0.1/ns/version", NodeFactory.createURI("https://w3id.org/catenax/ontology/common#version"));
        ASSET_PROPERTY_MAP.put("https://w3id.org/edc/v0.0.1/ns/contenttype", NodeFactory.createURI("https://w3id.org/catenax/ontology/common#contentType"));
        ASSET_PROPERTY_MAP.put("http://www.w3.org/1999/02/22-rdf-syntax-ns#type", NodeFactory.createURI("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"));
        ASSET_PROPERTY_MAP.put("http://www.w3.org/2000/01/rdf-schema#isDefinedBy", NodeFactory.createURI("http://www.w3.org/2000/01/rdf-schema#isDefinedBy"));
        ASSET_PROPERTY_MAP.put("https://w3id.org/catenax/ontology/common#implementsProtocol", NodeFactory.createURI("https://w3id.org/catenax/ontology/common#implementsProtocol"));
        ASSET_PROPERTY_MAP.put("http://www.w3.org/ns/shacl#shapesGraph", NodeFactory.createURI("http://www.w3.org/ns/shacl#shapesGraph"));
        ASSET_PROPERTY_MAP.put("https://w3id.org/catenax/ontology/common#isFederated", NodeFactory.createURI("https://w3id.org/catenax/ontology/common#isFederated"));
        ASSET_PROPERTY_MAP.put("https://w3id.org/catenax/ontology/common#publishedUnderContract", NodeFactory.createURI("https://w3id.org/catenax/ontology/common#publishedUnderContract"));
        ASSET_PROPERTY_MAP.put("https://w3id.org/catenax/ontology/common#satisfiesRole", NodeFactory.createURI("https://w3id.org/catenax/ontology/common#satisfiesRole"));
    }

    /**
     * service links
     */
    protected final ScheduledExecutorService service;
    protected final AgentConfig config;
    protected final DataManagement dataManagement;
    protected final RdfStore rdfStore;
    protected final Monitor monitor;

    /**
     * internal state
     */
    protected boolean isStarted = false;

    /**
     * creates the synchronizer
     *
     * @param service scheduler
     * @param config edc config
     * @param dataManagement data management service remoting
     * @param rdfStore a triple store for persistance
     * @param monitor logging subsystem
     */
    public DataspaceSynchronizer(ScheduledExecutorService service, AgentConfig config, DataManagement dataManagement, RdfStore rdfStore, Monitor monitor) {
        this.service = service;
        this.config = config;
        this.dataManagement = dataManagement;
        this.rdfStore = rdfStore;
        this.monitor = monitor;
        this.monitorWrapper = new MonitorWrapper(getClass().getName(), monitor);
    }

    /**
     * starts the synchronizer
     */
    public synchronized void start() {
        if (!isStarted) {
            isStarted = true;
            long interval = config.getDataspaceSynchronizationInterval();
            String[] connectors = config.getDataspaceSynchronizationConnectors();
            if (interval > 0 && connectors != null && connectors.length > 0) {
                monitor.info(String.format("Starting dataspace synchronization on %d connectors with interval %d milliseconds", connectors.length, interval));
                service.schedule(this, interval, TimeUnit.MILLISECONDS);
            }
        }
    }

    /**
     * stops the synchronizer
     */
    public synchronized void shutdown() {
        if (isStarted) {
            monitor.info("Shutting down dataspace synchronization");
            isStarted = false;
            service.shutdown();
        }
    }

    /**
     * runs the synchronizer when scheduled
     */
    @Override
    public void run() {
        monitor.debug("Synchronization run has been started");
        if (isStarted) {
            for (String remote : config.getDataspaceSynchronizationConnectors()) {
                if (isStarted) {
                    monitor.debug(String.format("About to synchronize remote connector %s", remote));
                    rdfStore.startTx();
                    try {
                        DcatCatalog catalog = dataManagement.getCatalog(remote, FEDERATED_ASSET_QUERY);
                        Node graph = rdfStore.getDefaultGraph();
                        Node connector = NodeFactory.createURI(remote.replace("https", "edcs").replace("http", "edc"));
                        Quad findAssets = Quad.create(graph, connector, CX_ASSET, Node.ANY);
                        Iterator<Quad> assetQuads = rdfStore.getDataSet().find(findAssets);
                        int tupleCount = 0;
                        while (assetQuads.hasNext()) {
                            Quad quadAsset = assetQuads.next();
                            Quad findAssetProps = Quad.create(graph, quadAsset.getObject(), Node.ANY, Node.ANY);
                            Iterator<Quad> propQuads = rdfStore.getDataSet().find(findAssetProps);
                            while (propQuads.hasNext()) {
                                Quad quadProp = propQuads.next();
                                if (quadProp.getPredicate().isURI() && quadProp.getPredicate().getURI().equals("http://www.w3.org/ns/shacl#shapesGraph") && quadProp.getObject().isURI()) {
                                    Quad findSubGraphsProps = Quad.create(quadProp.getObject(), Node.ANY, Node.ANY, Node.ANY);
                                    Iterator<Quad> subGraphQuads = rdfStore.getDataSet().find(findSubGraphsProps);
                                    while (subGraphQuads.hasNext()) {
                                        rdfStore.getDataSet().delete(subGraphQuads.next());
                                        tupleCount++;
                                    }
                                }
                                rdfStore.getDataSet().delete(quadProp);
                                tupleCount++;
                            }
                            rdfStore.getDataSet().delete(quadAsset);
                            tupleCount++;
                        }
                        monitor.debug(String.format("About to delete %d old tuples.", tupleCount));
                        List<DcatDataset> offers = catalog.getDatasets();
                        tupleCount = 0;
                        if (offers != null) {
                            monitor.debug(String.format("Found a catalog with %d entries for remote connector %s", offers.size(), remote));
                            for (DcatDataset offer : catalog.getDatasets()) {
                                for (Quad quad : convertToQuads(graph, connector, offer)) {
                                    tupleCount++;
                                    rdfStore.getDataSet().add(quad);
                                }
                            }
                        } else {
                            monitor.warning(String.format("Found an empty catalog for remote connector %s", remote));
                        }
                        monitor.debug(String.format("About to add %d new tuples.", tupleCount));
                        rdfStore.commit();
                    } catch (Throwable io) {
                        monitor.warning(String.format("Could not synchronize remote connector %s because of %s. Going ahead.", remote, io));
                        rdfStore.abort();
                    }
                } else {
                    monitor.debug(String.format("Synchronization is no more active. Skipping all connectors starting from %s.", remote));
                    break;
                }
            } // for
            if (isStarted) {
                monitor.debug("Schedule next synchronization run");
                service.schedule(this, config.getDataspaceSynchronizationInterval(), TimeUnit.MILLISECONDS);
            } else {
                monitor.debug("Synchronization is no more active. Disable next run.");
            }
        }
    }

    /**
     * Workaround the castration of the IDS catalogue
     *
     * @param offer being made
     * @return default props
     */
    public static Map<String, JsonValue> getProperties(DcatDataset offer) {
        Map<String, JsonValue> assetProperties = new HashMap<>(offer.getProperties());
        if (!assetProperties.containsKey("http://www.w3.org/1999/02/22-rdf-syntax-ns#type")) {
            String assetType = JsonLd.asString(assetProperties.getOrDefault("@id", Json.createValue("cx-common:Asset")));
            int indexOfQuestion = assetType.indexOf("?");
            if (indexOfQuestion > 0) {
                assetType = assetType.substring(0, indexOfQuestion - 1);
            }
            assetProperties.put("http://www.w3.org/1999/02/22-rdf-syntax-ns#type", Json.createValue(assetType));
        }
        if (!assetProperties.containsKey("@id")) {
            assetProperties.put("@id", Json.createValue(UUID.randomUUID().toString()));
        }
        return assetProperties;
    }

    /**
     * convert a given contract offer into quads
     *
     * @param graph default graph
     * @param connector parent connector hosting the offer
     * @param offer the contract offer
     * @return a collection of quads
     */
    public Collection<Quad> convertToQuads(Node graph, Node connector, DcatDataset offer) {
        Map<String, JsonValue> assetProperties = getProperties(offer);

        List<Quad> quads = new ArrayList<>();
        String offerId = assetProperties.get("@id").toString();
        Node assetNode = NodeFactory.createURI(offerId);
        quads.add(Quad.create(graph,
                connector,
                CX_ASSET,
                assetNode));
        for (Map.Entry<String, JsonValue> assetProp : assetProperties.entrySet()) {
            String key = assetProp.getKey();
            Node node = ASSET_PROPERTY_MAP.get(key);
            while (node == null && key.indexOf('.') >= 0) {
                key = key.substring(key.lastIndexOf(".") + 1);
                node = ASSET_PROPERTY_MAP.get(key);
            }
            if (node != null) {
                String pureProperty = JsonLd.asString(assetProp.getValue());
                if (pureProperty != null) {
                    try {
                        switch (key) {
                            case "https://w3id.org/catenax/ontology/common#publishedUnderContract":
                            case "http://www.w3.org/2000/01/rdf-schema#isDefinedBy":
                            case "http://www.w3.org/1999/02/22-rdf-syntax-ns#type":
                            case "https://w3id.org/catenax/ontology/common#isFederated":
                            case "https://w3id.org/catenax/ontology/common#satisfiesRole":
                            case "https://w3id.org/catenax/ontology/common#implementsProtocol":
                                String[] urls = pureProperty.split(",");
                                for (String url : urls) {
                                    Node o;
                                    url = url.trim();
                                    if (url.startsWith("<") && url.endsWith(">")) {
                                        url = url.substring(1, url.length() - 1);
                                        o = NodeFactory.createURI(url);
                                    } else if (url.startsWith("\"") && url.endsWith("\"")) {
                                        url = url.substring(1, url.length() - 1);
                                        o = NodeFactory.createLiteral(url);
                                    } else if (url.startsWith("cx-common:")) {
                                        url = url.substring(10);
                                        o = NodeFactory.createURI("https://w3id.org/catenax/ontology/common#" + url);
                                    } else if (url.contains("^^")) {
                                        int typeAnnotation = url.indexOf("^^");
                                        String type = url.substring(typeAnnotation + 2);
                                        url = url.substring(0, typeAnnotation);
                                        o = NodeFactory.createLiteral(url, NodeFactory.getType(type));
                                    } else {
                                        o = NodeFactory.createLiteral(url);
                                    }
                                    quads.add(Quad.create(graph, assetNode, node, o));
                                }
                                break;
                            case "http://www.w3.org/ns/shacl#shapesGraph":
                                Node newGraph = NodeFactory.createURI("https://w3id.org/catenax/ontology/common#GraphShape?id=" + offerId.hashCode());
                                quads.add(Quad.create(graph, assetNode, node, newGraph));
                                Sink<Quad> quadSink = new Sink<>() {

                                    @Override
                                    public void close() {
                                    }

                                    @Override
                                    public void send(Quad quad) {
                                        quads.add(quad);
                                    }

                                    @Override
                                    public void flush() {
                                    }
                                };
                                StreamRDF dest = StreamRDFLib.sinkQuads(quadSink);
                                StreamRDF graphDest = StreamRDFLib.extendTriplesToQuads(newGraph, dest);
                                StreamRDFCounting countingDest = StreamRDFLib.count(graphDest);
                                ErrorHandler errorHandler = ErrorHandlerFactory.errorHandlerStd(monitorWrapper);
                                RDFParser.create()
                                        .errorHandler(errorHandler)
                                        .source(new StringReader(pureProperty))
                                        .lang(Lang.TTL)
                                        .parse(countingDest);
                                monitor.debug(String.format("Added shapes subgraph %s with %d triples", newGraph, countingDest.countTriples()));
                                break;
                            default:
                                quads.add(Quad.create(graph, assetNode, node, NodeFactory.createLiteral(pureProperty)));
                        } //switch
                    } catch (Throwable t) {
                        monitor.debug(String.format("Could not correctly add asset triples for predicate %s with original value %s because of %s", node, pureProperty, t.getMessage()));
                    }
                } // if property!=null
            } // if node!=null
        } // for
        return quads;
    }

}
