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
package org.eclipse.tractusx.agents.edc;

import okhttp3.OkHttpClient;
import org.apache.jena.query.Syntax;
import org.apache.jena.sparql.serializer.SerializerRegistry;
import org.apache.jena.sparql.service.ServiceExecutorRegistry;
import org.eclipse.edc.connector.dataplane.http.params.HttpRequestFactory;
import org.eclipse.edc.connector.dataplane.http.spi.HttpRequestParamsProvider;
import org.eclipse.edc.connector.dataplane.spi.Endpoint;
import org.eclipse.edc.connector.dataplane.spi.iam.PublicEndpointGeneratorService;
import org.eclipse.edc.connector.dataplane.spi.pipeline.PipelineService;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Requires;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.Hostname;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.tractusx.agents.edc.http.AgentController;
import org.eclipse.tractusx.agents.edc.http.DelegationServiceImpl;
import org.eclipse.tractusx.agents.edc.http.HttpClientFactory;
import org.eclipse.tractusx.agents.edc.http.transfer.AgentSourceFactory;
import org.eclipse.tractusx.agents.edc.http.transfer.AgentSourceRequestParamsSupplier;
import org.eclipse.tractusx.agents.edc.rdf.RdfStore;
import org.eclipse.tractusx.agents.edc.service.DataManagement;
import org.eclipse.tractusx.agents.edc.service.DataspaceSynchronizer;
import org.eclipse.tractusx.agents.edc.service.EdcSkillStore;
import org.eclipse.tractusx.agents.edc.sparql.DataspaceServiceExecutor;
import org.eclipse.tractusx.agents.edc.sparql.SparqlQueryProcessor;
import org.eclipse.tractusx.agents.edc.sparql.SparqlQuerySerializerFactory;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * EDC extension that initializes the Agent subsystem (Agent Sources, Agent Endpoint and Federation Callbacks
 */
@Requires(HttpRequestParamsProvider.class)
public class AgentExtension implements ServiceExtension {

    /**
     * static constants
     */
    protected static final String DEFAULT_CONTEXT_ALIAS = "default";
    protected static final String CALLBACK_CONTEXT_ALIAS = "callback";

    /**
     * dependency injection part
     */
    @Inject
    protected WebService webService;


    @Inject
    protected PipelineService pipelineService;

    @Inject
    protected Vault vault;

    @Inject
    protected TypeManager typeManager;

    // we reuse the http settings of the http transfer
    @Inject
    protected EdcHttpClient edcHttpClient;
    @Inject
    protected OkHttpClient httpClient;
    @Inject
    private PublicEndpointGeneratorService generatorService;
    @Inject
    private Hostname hostname;

    /**
     * refers a scheduler
     * TODO maybe reuse an injected scheduler
     */
    protected ScheduledExecutorService executorService;

    /**
     * data synchronization service
     */
    protected DataspaceSynchronizer synchronizer;

    /**
     * access
     *
     * @return name of the extension
     */
    @Override
    public String name() {
        return "Knowledge Agents Extension";
    }

    /**
     * runs on extension initialization
     *
     * @param context EDC bootstrap context
     */
    @Override
    public void initialize(ServiceExtensionContext context) {
        Monitor monitor = context.getMonitor();
        
        monitor.debug(String.format("Initializing %s", name()));

        AgentConfig config = new AgentConfig(monitor, context.getConfig());
        Map.Entry<EdcHttpClient, OkHttpClient> instance = HttpClientFactory.create(edcHttpClient, httpClient, pipelineService, config);
        edcHttpClient = instance.getKey();
        httpClient = instance.getValue();

        DataManagement catalogService = new DataManagement(monitor, typeManager, httpClient, config);

        AgreementControllerImpl agreementController = new AgreementControllerImpl(monitor, config, catalogService);
        monitor.debug(String.format("Registering agreement controller %s", agreementController));
        webService.registerResource(CALLBACK_CONTEXT_ALIAS, agreementController);

        RdfStore rdfStore = new RdfStore(config, monitor);

        executorService = Executors.newScheduledThreadPool(config.getThreadPoolSize());
        synchronizer = new DataspaceSynchronizer(executorService, config, catalogService, rdfStore, monitor);

        // EDC Remoting Support
        ServiceExecutorRegistry reg = new ServiceExecutorRegistry();
        reg.addBulkLink(new DataspaceServiceExecutor(monitor, agreementController, config, httpClient, executorService, typeManager));
        //reg.add(new DataspaceServiceExecutor(monitor,agreementController,config,httpClient));

        // Ontop and other deep nesting-afraid providers/optimizers
        // should be supported by not relying on the Fuseki syntax graph
        SparqlQuerySerializerFactory arqQuerySerializerFactory = new SparqlQuerySerializerFactory();
        SerializerRegistry.get().addQuerySerializer(Syntax.syntaxARQ, arqQuerySerializerFactory);
        SerializerRegistry.get().addQuerySerializer(Syntax.syntaxSPARQL_10, arqQuerySerializerFactory);
        SerializerRegistry.get().addQuerySerializer(Syntax.syntaxSPARQL_11, arqQuerySerializerFactory);

        // the actual sparql engine inside the EDC
        SparqlQueryProcessor processor = new SparqlQueryProcessor(reg, monitor, config, rdfStore, typeManager);

        // stored procedure store and transport endpoint
        SkillStore skillStore = new EdcSkillStore(catalogService, typeManager, config);
        DelegationServiceImpl delegationService = new DelegationServiceImpl(agreementController, monitor, httpClient, typeManager, config);
        AgentController agentController = new AgentController(monitor, agreementController, config, processor, skillStore, delegationService);
        monitor.debug(String.format("Registering agent controller %s", agentController));
        webService.registerResource(DEFAULT_CONTEXT_ALIAS, agentController);

        monitor.debug(String.format("Initialized %s", name()));

        HttpRequestFactory httpRequestFactory = new HttpRequestFactory();
        AgentSourceFactory sparqlSourceFactory = new AgentSourceFactory(AgentProtocol.SPARQL_HTTP.getProtocolId(),
                edcHttpClient,
                new AgentSourceRequestParamsSupplier(vault, typeManager, config, monitor),
                monitor,
                httpRequestFactory,
                processor,
                skillStore,
                typeManager);
        AgentSourceFactory skillSourceFactory = new AgentSourceFactory(AgentProtocol.SKILL_HTTP.getProtocolId(),
                edcHttpClient,
                new AgentSourceRequestParamsSupplier(vault, typeManager, config, monitor),
                monitor,
                httpRequestFactory,
                processor,
                skillStore,
                typeManager);
        pipelineService.registerFactory(sparqlSourceFactory);
        pipelineService.registerFactory(skillSourceFactory);

        var publicEndpoint = context.getSetting("edc.dataplane.api.public.baseurl", null);
        if (publicEndpoint == null) {
            publicEndpoint = String.format("http://%s:%d%s", hostname.get(), context.getSetting("web.http.public.port", 8185), context.getSetting("web.http.public.path", "/api/public"));
        }
        var endpoint = Endpoint.url(publicEndpoint);
        generatorService.addGeneratorFunction(AgentProtocol.SPARQL_HTTP.getProtocolId(), dataAddress -> endpoint);
        generatorService.addGeneratorFunction(AgentProtocol.SKILL_HTTP.getProtocolId(), dataAddress -> endpoint);
    }

    /**
     * start scheduled services
     */
    @Override
    public void start() {
        synchronizer.start();
    }

    /**
     * Signals the extension to release resources and shutdown.
     * stop any schedules services
     */
    @Override
    public void shutdown() {
        synchronizer.shutdown();
    }
}