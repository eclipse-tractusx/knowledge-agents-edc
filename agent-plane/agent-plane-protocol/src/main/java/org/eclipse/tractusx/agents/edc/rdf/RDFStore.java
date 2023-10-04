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

import org.apache.jena.sparql.core.Quad;
import org.apache.jena.tdb.TDBFactory;
import org.eclipse.tractusx.agents.edc.AgentConfig;
import org.eclipse.tractusx.agents.edc.MonitorWrapper;
import org.apache.jena.fuseki.server.DataAccessPoint;
import org.apache.jena.fuseki.server.DataService;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.TxnType;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.riot.lang.StreamRDFCounting;
import org.apache.jena.riot.system.ErrorHandler;
import org.apache.jena.riot.system.ErrorHandlerFactory;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.riot.system.StreamRDFLib;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.DatasetGraphFactory;
import org.eclipse.edc.spi.monitor.Monitor;

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * a service sitting on a local RDF store/graph
 * (which hosts the ontology and the federated dataspace
 * representation)
 */
public class RDFStore {

    // we need a single data access point (with its default graph)
    protected final DatasetGraph dataset;
    protected final DataAccessPoint api;
    protected final DataService service;
    protected final Monitor monitor;
    protected final AgentConfig config;

    protected final MonitorWrapper monitorWrapper;

    /**
     * create a new RDF store (and initialise with a given ttl file)
     * @param config EDC config
     * @param monitor logging subsystem
     */
    public RDFStore(AgentConfig config, Monitor monitor) {
        this.config=config;
        String rdfStore = config.getRdfStore();
        if (rdfStore == null) {
        	this.dataset = DatasetGraphFactory.createTxnMem();
        } else {
        	this.dataset = TDBFactory.createDatasetGraph(rdfStore);
        }
        DataService.Builder dataService = DataService.newBuilder(dataset);
        this.service=dataService.build();
        api=new DataAccessPoint(config.getAccessPoint(), service);
        this.monitor=monitor;
        this.monitorWrapper=new MonitorWrapper(getClass().getName(),monitor);
        monitor.debug(String.format("Activating data service %s under access point %s",service,api));
        service.goActive();
        // read file with ontology, share this dataset with the catalogue sync procedure
        if(config.getAssetFiles()!=null) {
            startTx();
            StreamRDF dest = StreamRDFLib.dataset(dataset);
            StreamRDF graphDest = StreamRDFLib.extendTriplesToQuads(getDefaultGraph(),dest);
            StreamRDFCounting countingDest = StreamRDFLib.count(graphDest);
            ErrorHandler errorHandler = ErrorHandlerFactory.errorHandlerStd(monitorWrapper);
            for(String assetFile : config.getAssetFiles()) {
                RDFParser.create()
                        .errorHandler(errorHandler)
                        .source(assetFile)
                        .lang(Lang.TTL)
                        .parse(countingDest);
                monitor.debug(String.format("Initialised asset %s with file %s resulted in %d triples",config.getDefaultAsset(),assetFile,countingDest.countTriples()));
            }
            commit();
            monitor.info(String.format("Initialised asset %s with %d triples from %d files",config.getDefaultAsset(),countingDest.countTriples(),config.getAssetFiles().length));
        } else {
            monitor.info(String.format("Initialised asset %s with 0 triples.",config.getDefaultAsset()));
        }
    }

    public final static String CSV_REGEX="(\"[^\"]*\")?([^%s]*)";

    /**
     * registers a new asset
     * @param asset asset iri
     * @param content stream for rdf data
     * @param format the format of the stream
     * @return number of resulting triples
     */
    public long registerAsset(String asset, String content, ExternalFormat format) {
        if(!asset.contains("/")) {
            asset="http://server/unset-base/"+asset;
        }
        monitor.info(String.format("Upserting asset %s with turtle source.",asset));
        startTx();
        StreamRDF dest = StreamRDFLib.dataset(dataset);
        StreamRDF graphDest = StreamRDFLib.extendTriplesToQuads(NodeFactory.createURI(asset),dest);
        StreamRDFCounting countingDest = StreamRDFLib.count(graphDest);
        ErrorHandler errorHandler = ErrorHandlerFactory.errorHandlerStd(monitorWrapper);
        switch(format) {
            case TURTLE:
                RDFParser.create()
                        .errorHandler(errorHandler)
                        .source(new ByteArrayInputStream(content.getBytes()))
                        .lang(Lang.TTL)
                        .parse(countingDest);
                break;
            case CSV:
                countingDest.start();
                Pattern csvCell= Pattern.compile(String.format(CSV_REGEX,","));
                try(BufferedReader reader=new BufferedReader(new StringReader(content))) {
                    String header=reader.readLine();
                    List<Node> predicates=new ArrayList<>();
                    if(header!=null) {
                        int position=0;
                        Matcher headerMatcher = csvCell.matcher(header);
                        while(position<=header.length() && headerMatcher.find(position)) {
                            predicates.add(NodeFactory.createURI(headerMatcher.group()));
                            position=headerMatcher.end()+1;
                        }
                        reader.lines().forEach( factLine -> {
                           int fposition=0;
                           Matcher factMatcher =  csvCell.matcher(factLine);
                           if(factMatcher.find(fposition)) {
                               Node subject = NodeFactory.createURI(factMatcher.group());
                               fposition=factMatcher.end()+1;
                               for(int fact=1;fact<predicates.size() && fposition<=factLine.length() && factMatcher.find(fposition);fact++) {
                                   Node object = parseObject(factMatcher.group());
                                   countingDest.triple(NodeFactory.createTripleNode(subject,predicates.get(fact),object).getTriple());
                                   fposition=factMatcher.end()+1;
                               }
                           }
                        });
                    }
                } catch(IOException e) {
                    monitor.warning("An exception has occurred while parsing a CSV stream. Ignoring some/all data.",e);
                }
                countingDest.finish();
                break;
        }
        long numberOfTriples=countingDest.countTriples();
        monitor.debug(String.format("Upserting asset %s resulted in %d triples",asset,numberOfTriples));
        commit();
        return numberOfTriples;
    }

    /**
     * parses a given rdf snippet into a node
     * @param group rdf snippet node
     * @return a parsed node
     */
    private Node parseObject(String group) {
        if(group.startsWith("<")) {
            group=group.replaceAll("[\\<\\>]","");
            return NodeFactory.createURI(group);
        } else if(group.contains("^^")) {
            int index = group.lastIndexOf("^^");
            String type = group.substring(index + 2);
            group = group.substring(0, index - 1);
        }
        return NodeFactory.createLiteral(group);
    }

    /**
     * deletes an asset
     * @param asset asset iri
     * @return number of deleted triples
     */
    public long deleteAsset(String asset) {
        if(!asset.contains("/")) {
            asset="http://server/unset-base/"+asset;
        }
        monitor.info(String.format("Deleting asset %s.",asset));
        startTx();
        Quad findAssets = Quad.create(NodeFactory.createURI(asset),Node.ANY,Node.ANY,Node.ANY);
        Iterator<Quad> assetQuads= getDataSet().find(findAssets);
        int tupleCount=0;
        while(assetQuads.hasNext()) {
            getDataSet().delete(assetQuads.next());
            tupleCount++;
        }
        monitor.debug(String.format("Deleting asset %s resulted in %d triples",asset,tupleCount));
        commit();
        return tupleCount;
    }

    /**
     * @return name of the default graph
     */
    public Node getDefaultGraph() {
        return NodeFactory.createURI(config.getDefaultAsset());
    }

    /**
     * @return access point to the graph
     */
    public DataAccessPoint getDataAccessPoint() {
        return api;
    }

    /**
     * @return dataservice shielding the graph
     */
    public DataService getDataService() {
        return service;
    }

    /**
     * @return the actual graph store
     */
    public DatasetGraph getDataSet() {
        return dataset;
    }

    /**
     * starts a write transaction
     */
    public void startTx() {
        dataset.begin(TxnType.WRITE);
    }

    /**
     * commits the current transaction
     */
    public void commit() {
        dataset.commit();
    }

    /**
     * rollback the current transaction
     */
    public void abort() {
        dataset.abort();
    }
}
