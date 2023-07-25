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
package org.eclipse.tractusx.agents.edc.rdf;

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
        this.dataset = DatasetGraphFactory.createTxnMem();
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
