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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * A service which keeps a triple store and
 * the associated dataspace (partner catalogues) in sync
 */
public class DataspaceSynchronizer implements Runnable {

    /**
     * constants
     */

    public static final String COMMON_NAMESPACE = "https://w3id.org/catenax/ontology/common#";
    public static final String TAXO_NAMESPACE = "https://w3id.org/catenax/taxonomy#";
    public static final String EDC_NAMESPACE = "https://w3id.org/edc/v0.0.1/ns/";
    public static final String RDF_NAMESPACE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
    public static final Node RDF_TYPE = createUri(RDF_NAMESPACE, "type");
    public static final String RDF_SCHEMA_NAMESPACE = "http://www.w3.org/2000/01/rdf-schema#";
    public static final String DC_NAMESPACE = "https://purl.org/dc/terms/";
    public static final String DC_TYPE = DC_NAMESPACE + "type";
    public static final String SHACL_NAMESPACE = "http://www.w3.org/ns/shacl#";
    public static final String SHAPES_GRAPH = SHACL_NAMESPACE + "shapesGraph";
    public static final String CX_SCHEMA_NAMESPACE = "https://w3id.org/catenax/ontology/schema#";
    public static final String XML_SCHEMA_NAMESPACE = "http://www.w3.org/2001/XMLSchema#";

    public static final Node SHAPE_OBJECT = createUri(CX_SCHEMA_NAMESPACE, "shapeObject");
    protected static final Node CX_ASSET = createUri(COMMON_NAMESPACE, "offers");
    protected static final Set<String> EXPECTED_COMPLEX_OBJECTS = new HashSet<>();
    protected static final QuerySpec FEDERATED_ASSET_QUERY = QuerySpec.Builder.newInstance()
            .filter(List.of(new Criterion(COMMON_NAMESPACE + "isFederated", "=", "true^^xsd:boolean"))).build();

    protected static final Map<String, Node> ASSET_PROPERTY_MAP = new HashMap<>();

    static {
        registerPredicate(COMMON_NAMESPACE, "id", false);
        registerPredicate(COMMON_NAMESPACE, "name", false);
        registerPredicate(COMMON_NAMESPACE, "description", false);
        registerPredicate(COMMON_NAMESPACE, "version", false);
        registerPredicate(COMMON_NAMESPACE, "contenttype", false);
        registerPredicate(DC_NAMESPACE, "type", true);
        // map the rdf definition to dublin core
        ASSET_PROPERTY_MAP.put(RDF_NAMESPACE + "type", createUri(DC_NAMESPACE, "type"));
        registerPredicate(RDF_SCHEMA_NAMESPACE, "isDefinedBy", true);
        registerPredicate(COMMON_NAMESPACE, "implementsProtocol", true);
        registerPredicate(SHACL_NAMESPACE, "shapesGraph", false);
        registerPredicate(COMMON_NAMESPACE, "isFederated", true);
        registerPredicate(COMMON_NAMESPACE, "publishedUnderContract", true);
        registerPredicate(COMMON_NAMESPACE, "satisfiesRole", true);
    }

    protected static final Map<String, String> PREDEFINED_NS = new HashMap<>(
    );

    static {
        PREDEFINED_NS.put("cx-common:", COMMON_NAMESPACE);
        PREDEFINED_NS.put("cx-taxo:", TAXO_NAMESPACE);
        PREDEFINED_NS.put("edc:", EDC_NAMESPACE);
        PREDEFINED_NS.put("rdf:", RDF_NAMESPACE);
        PREDEFINED_NS.put("rdfs:", RDF_SCHEMA_NAMESPACE);
        PREDEFINED_NS.put("sh:", SHACL_NAMESPACE);
        PREDEFINED_NS.put("cx-sh:", CX_SCHEMA_NAMESPACE);
        PREDEFINED_NS.put("dct:", DC_NAMESPACE);
        PREDEFINED_NS.put("xsd:", XML_SCHEMA_NAMESPACE);
    }

    /**
     * static helper to create an uri node from a prefix and name
     *
     * @param prefix namespace
     * @param name   entity name
     * @return uri node
     */
    protected static Node createUri(String prefix, String name) {
        return NodeFactory.createURI(prefix + name);
    }

    /**
     * make sure these asset properties can be defined with or without prefix
     *
     * @param prefix namespace
     * @param name   id
     */
    protected static void registerPredicate(String prefix, String name, boolean isUri) {
        String key = prefix + name;
        if (isUri) {
            EXPECTED_COMPLEX_OBJECTS.add(key);
        }
        Node target = createUri(prefix, name);
        ASSET_PROPERTY_MAP.put(key, target);
        key = EDC_NAMESPACE + name;
        ASSET_PROPERTY_MAP.put(key, target);
    }

    /**
     * service links
     */
    protected final ScheduledExecutorService service;
    protected final AgentConfig config;
    protected final DataManagement dataManagement;
    protected final RdfStore rdfStore;
    protected final Monitor monitor;
    protected final MonitorWrapper monitorWrapper;

    /**
     * internal state
     */
    protected boolean isStarted = false;

    /**
     * creates the synchronizer
     *
     * @param service        scheduler
     * @param config         edc config
     * @param dataManagement data management service remoting
     * @param rdfStore       a triple store for persistance
     * @param monitor        logging subsystem
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
            Map<String, String> connectors = config.getDataspaceSynchronizationConnectors();
            if (interval > 0 && connectors != null && connectors.size() > 0) {
                monitor.info(String.format("Starting dataspace synchronization on %d connectors with interval %d milliseconds", connectors.size(), interval));
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
            for (Map.Entry<String, String> remote : config.getDataspaceSynchronizationConnectors().entrySet()) {
                if (isStarted) {
                    monitor.debug(String.format("About to synchronize remote connector %s", remote));
                    rdfStore.startTx();
                    try {
                        DcatCatalog catalog = dataManagement.getCatalog(remote.getKey(), remote.getValue(), FEDERATED_ASSET_QUERY);
                        Node graph = rdfStore.getDefaultGraph();
                        Node connector = NodeFactory.createURI(remote.getValue().replace("https", "edcs").replace("http", "edc"));
                        deleteConnectorFacts(graph, connector);
                        addConnectorFacts(remote.getValue(), catalog, graph, connector);
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
     * adds new facts about the catalog thats been collected from the given connector
     *
     * @param remote    url of the remote connector
     * @param catalog   retrieved catalogue
     * @param graph     to store the facts
     * @param connector uri node representing the connector
     * @return number of fact triples/quads added
     */
    public int addConnectorFacts(String remote, DcatCatalog catalog, Node graph, Node connector) {
        int tupleCount;
        List<DcatDataset> offers = catalog.getDatasets();
        tupleCount = 0;
        if (offers != null) {
            monitor.debug(String.format("Found a catalog with %d entries for remote connector %s", offers.size(), remote));
            for (DcatDataset offer : catalog.getDatasets()) {
                tupleCount += addOfferFacts(graph, connector, offer);
            }
        } else {
            monitor.warning(String.format("Found an empty catalog for remote connector %s", remote));
        }
        monitor.debug(String.format("About to add %d new tuples.", tupleCount));
        return tupleCount;
    }

    /**
     * adds new facts about a concrete data offer collected from a given connector
     *
     * @param graph     to store the facts
     * @param connector uri node representing the connector
     * @param offer     to convert into facts
     * @return number of facts triples/quads added
     */
    public int addOfferFacts(Node graph, Node connector, DcatDataset offer) {
        int tupleCount = 0;
        var quads = convertToQuads(graph, connector, offer);
        for (Quad quad : quads) {
            tupleCount++;
            rdfStore.getDataSet().add(quad);
        }
        return tupleCount;
    }

    /**
     * deletes existing facts about a connector
     *
     * @param graph     to delete the facts from
     * @param connector to clean up the facts for
     * @return number of fact triples/quads deleted
     */
    public int deleteConnectorFacts(Node graph, Node connector) {
        // find all offers attached to the connector
        Quad findAssets = Quad.create(graph, connector, CX_ASSET, Node.ANY);
        Iterator<Quad> assetQuads = rdfStore.getDataSet().find(findAssets);
        int tupleCount = 0;
        while (assetQuads.hasNext()) {
            Quad quadAsset = assetQuads.next();
            Node assetNode = quadAsset.getObject();
            tupleCount += deleteShaclShapes(graph, assetNode);
            tupleCount += deleteAssetProperties(graph, assetNode);
            rdfStore.getDataSet().delete(quadAsset);
            tupleCount++;
        }
        monitor.debug(String.format("About to delete %d old tuples.", tupleCount));
        return tupleCount;
    }

    /**
     * deletes properties of a given offer/asset
     *
     * @param graph     where facts are stored
     * @param assetNode to delete properties from
     * @return number of property facts deleted
     */
    public int deleteAssetProperties(Node graph, Node assetNode) {
        int tupleCount = 0;
        // remove all remaining properties associated to the offer/asset
        Quad findAssetProps = Quad.create(graph, assetNode, Node.ANY, Node.ANY);
        Iterator<Quad> propQuads = rdfStore.getDataSet().find(findAssetProps);
        while (propQuads.hasNext()) {
            Quad quadProp = propQuads.next();
            rdfStore.getDataSet().delete(quadProp);
            tupleCount++;
        }
        return tupleCount;
    }

    /**
     * delete all shacl shapes associated to an asset node
     *
     * @param graph     where the shapes are stored
     * @param assetNode to delete the shapes from
     * @return number of associated shapes (and their properties)
     */
    public int deleteShaclShapes(Node graph, Node assetNode) {
        int tupleCount = 0;
        // remove all shacl shapes associated to the offer/asset
        Quad findAssetShapes = Quad.create(graph, assetNode, SHAPE_OBJECT, Node.ANY);
        Iterator<Quad> shapesQuad = rdfStore.getDataSet().find(findAssetShapes);
        while (shapesQuad.hasNext()) {
            Quad shapeQuad = shapesQuad.next();
            Node shapesObject = shapeQuad.getObject();
            Quad findShapesPredicates = Quad.create(graph, shapesObject, Node.ANY, Node.ANY);
            Iterator<Quad> shapesFacts = rdfStore.getDataSet().find(findShapesPredicates);
            while (shapesFacts.hasNext()) {
                rdfStore.getDataSet().delete(shapesFacts.next());
                tupleCount++;
            }
            rdfStore.getDataSet().delete(shapeQuad);
            tupleCount++;
        }
        return tupleCount;
    }

    /**
     * Workaround the castration of the IDS catalogue
     *
     * @param offer being made
     * @return default props
     */
    public static Map<String, JsonValue> getProperties(DcatDataset offer) {
        Map<String, JsonValue> assetProperties = new HashMap<>(offer.getProperties());
        if (!assetProperties.containsKey(DC_TYPE) && !assetProperties.containsKey(RDF_TYPE.getURI()) && !assetProperties.containsKey(EDC_NAMESPACE + "type")) {
            String assetType = JsonLd.asString(assetProperties.getOrDefault("@id", Json.createValue("cx-common:Asset")));
            int indexOfQuestion = assetType.indexOf("?");
            if (indexOfQuestion > 0) {
                assetType = assetType.substring(0, indexOfQuestion - 1);
            }
            assetProperties.put(DC_TYPE, Json.createValue(assetType));
        }
        if (!assetProperties.containsKey("@id")) {
            assetProperties.put("@id", Json.createValue(UUID.randomUUID().toString()));
        }
        return assetProperties;
    }

    /**
     * convert a given contract offer into quads
     *
     * @param graph     default graph
     * @param connector parent connector hosting the offer
     * @param offer     the contract offer
     * @return a collection of quads
     */
    public Collection<Quad> convertToQuads(Node graph, Node connector, DcatDataset offer) {
        Map<String, JsonValue> assetProperties = getProperties(offer);
        List<Quad> quads = new ArrayList<>();
        String offerId = JsonLd.asString(assetProperties.get("@id"));
        Node assetNode = NodeFactory.createURI(offerId);
        quads.add(Quad.create(graph,
                connector,
                CX_ASSET,
                assetNode));
        for (Map.Entry<String, JsonValue> assetProp : assetProperties.entrySet()) {
            String key = assetProp.getKey();
            Node node = ASSET_PROPERTY_MAP.get(key);
            // strip off language modifiers
            if (node == null && key.indexOf("@") >= 0) {
                String langSuffix = key.substring(key.lastIndexOf("@"));
                key = key.substring(0, key.lastIndexOf("@"));
                node = ASSET_PROPERTY_MAP.get(key);
                if (node != null) {
                    node = createUri(node.getURI(), "@" + langSuffix);
                }
            }
            // did we find some result
            if (node != null) {
                String pureProperty = JsonLd.asString(assetProp.getValue());
                if (pureProperty != null) {
                    try {
                        if (SHAPES_GRAPH.equals(key)) {
                            addShapesFacts(graph, quads, assetNode, pureProperty);
                        } else if (EXPECTED_COMPLEX_OBJECTS.contains(node.getURI())) {
                            addComplexFacts(graph, quads, assetNode, node, pureProperty);
                        } else {
                            quads.add(Quad.create(graph, assetNode, node, NodeFactory.createLiteral(pureProperty)));
                        }
                    } catch (Throwable t) {
                        monitor.debug(String.format("Could not correctly add asset triples for predicate %s with original value %s because of %s", node, pureProperty, t.getMessage()));
                    }
                } // if property!=null
            } // if node!=null
        } // for
        return quads;
    }

    /**
     * converts the given object string into complex facts
     *
     * @param graph        to store the facts
     * @param quads        list to add the facts to
     * @param assetNode    subject to attach the facts to
     * @param node         the predicated under which the facts are holding
     * @param objectString the actual value(s) to attach the predicate to
     */
    private static void addComplexFacts(Node graph, List<Quad> quads, Node assetNode, Node node, String objectString) {
        // do we expect our predicate to hit a uri
        String[] urls = objectString.split(",");
        for (String url : urls) {
            Node o = null;
            url = url.trim();
            if (url.startsWith("<") && url.endsWith(">")) {
                url = url.substring(1, url.length() - 1);
                o = NodeFactory.createURI(url);
            } else if (url.startsWith("\"") && url.endsWith("\"")) {
                url = url.substring(1, url.length() - 1);
                o = NodeFactory.createLiteral(url);
            } else if (url.contains("^^")) {
                int typeAnnotation = url.indexOf("^^");
                String type = url.substring(typeAnnotation + 2);
                url = url.substring(0, typeAnnotation);
                o = NodeFactory.createLiteral(url, NodeFactory.getType(type));
            } else {
                for (var entry : PREDEFINED_NS.entrySet()) {
                    if (url.startsWith(entry.getKey())) {
                        url = url.substring(entry.getKey().length());
                        o = NodeFactory.createURI(entry.getValue() + url);
                        break;
                    }
                }
                if (o == null) {
                    o = NodeFactory.createLiteral(url);
                }
            }
            quads.add(Quad.create(graph, assetNode, node, o));
            // make sure both rdf:type and dc:type are set
            if (node.getURI().equals(DC_TYPE)) {
                quads.add(Quad.create(graph, assetNode, RDF_TYPE, o));
            }
        }
    }

    /**
     * converts the given object string into a shapes graph
     *
     * @param graph             to store the facts in
     * @param quads             to add the facts to
     * @param assetNode         asset to attach the shapes to
     * @param shapesDescription a hopefully valid shacl shape turtle
     */
    private void addShapesFacts(Node graph, List<Quad> quads, Node assetNode, String shapesDescription) {
        StreamRDF dest = StreamRDFLib.sinkQuads(new Sink<>() {

            protected Set<String> connectedSubjects = new HashSet<>();

            @Override
            public void close() {
            }

            @Override
            public void send(Quad quad) {
                String subject = quad.getSubject().toString(false);
                if (!connectedSubjects.contains(subject)) {
                    quads.add(new Quad(graph, assetNode, SHAPE_OBJECT, quad.getSubject()));
                    connectedSubjects.add(subject);
                }
                quads.add(quad);
            }

            @Override
            public void flush() {
            }
        });
        StreamRDF graphDest = StreamRDFLib.extendTriplesToQuads(graph, dest);
        StreamRDFCounting countingDest = StreamRDFLib.count(graphDest);
        ErrorHandler errorHandler = ErrorHandlerFactory.errorHandlerStd(monitorWrapper);
        RDFParser.create()
                .errorHandler(errorHandler)
                .source(new StringReader(shapesDescription))
                .lang(Lang.TTL)
                .parse(countingDest);
        monitor.debug(String.format("Added shapes subgraph to asset %s with %d triples", assetNode, countingDest.countTriples()));
    }

}
