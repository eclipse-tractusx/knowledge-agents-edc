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
package org.eclipse.tractusx.agents.edc.sparql;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import org.apache.jena.atlas.logging.Log;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecException;
import org.apache.jena.query.ResultSet;
import org.apache.jena.riot.ResultSetMgr;
import org.apache.jena.riot.WebContent;
import org.apache.jena.riot.resultset.ResultSetLang;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.OpAsQuery;
import org.apache.jena.sparql.algebra.Transformer;
import org.apache.jena.sparql.algebra.op.OpProject;
import org.apache.jena.sparql.algebra.op.OpSequence;
import org.apache.jena.sparql.algebra.op.OpService;
import org.apache.jena.sparql.algebra.op.OpTable;
import org.apache.jena.sparql.algebra.table.TableData;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.ExecutionContext;
import org.apache.jena.sparql.engine.QueryIterator;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.binding.BindingBuilder;
import org.apache.jena.sparql.engine.http.HttpParams;
import org.apache.jena.sparql.engine.iterator.QueryIter;
import org.apache.jena.sparql.engine.iterator.QueryIter1;
import org.apache.jena.sparql.engine.iterator.QueryIterPlainWrapper;
import org.apache.jena.sparql.exec.RowSet;
import org.apache.jena.sparql.exec.RowSetAdapter;
import org.apache.jena.sparql.exec.http.Params;
import org.apache.jena.sparql.exec.http.QuerySendMode;
import org.apache.jena.sparql.exec.http.Service;
import org.apache.jena.sparql.graph.NodeTransformLib;
import org.apache.jena.sparql.resultset.ResultSetMem;
import org.apache.jena.sparql.service.bulk.ChainingServiceExecutorBulk;
import org.apache.jena.sparql.service.bulk.ServiceExecutorBulk;
import org.apache.jena.sparql.service.single.ServiceExecutor;
import org.apache.jena.sparql.util.Context;
import org.apache.jena.sparql.util.Symbol;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.spi.types.domain.edr.EndpointDataReference;
import org.eclipse.tractusx.agents.edc.AgentConfig;
import org.eclipse.tractusx.agents.edc.AgreementController;
import org.eclipse.tractusx.agents.edc.http.HttpClientAdapter;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A service executor (single and bulk mode) which replaces outgoing http calls
 * by appropriate dataspace agreements/calls.
 * Register in the ServiceExecutorRegistry via
 * - add(...) for single execution
 * - addBulkLink(...) for multiple execution
 */
public class DataspaceServiceExecutor implements ServiceExecutor, ChainingServiceExecutorBulk {

    /**
     * EDC services
     */
    final Monitor monitor;
    final AgreementController agreementController;
    final AgentConfig config;
    final HttpClient client;
    final ExecutorService executor;
    final ObjectMapper objectMapper;

    /**
     * some constants
     */
    public static final Symbol AUTH_KEY_SYMBOL = Symbol.create("https://w3id.org/edc/v0.0.1/ns/authKey");
    public static final Symbol AUTH_CODE_SYMBOL = Symbol.create("https://w3id.org/edc/v0.0.1/ns/authCode");
    public static final Pattern EDC_TARGET_ADDRESS_PATTERN = Pattern.compile("((?<protocol>edc|edcs)://(?<connector>[^#?]*))?(#(?<asset>[^/?]*))?(\\?(?<params>.*))?");
    public static final Symbol TARGET_URL_SYMBOL  = Symbol.create("https://w3id.org/edc/v0.0.1/ns/baseUrl");
    public static final Symbol ASSET_SYMBOL = Symbol.create("https://w3id.org/edc/v0.0.1/ns/id");
    public static final Symbol ALLOW_SYMBOL = Symbol.create("https://w3id.org/catenax/ontology/common#allowServicePattern");
    public static final Symbol DENY_SYMBOL = Symbol.create("https://w3id.org/catenax/ontology/common#denyServicePattern");

    /**
     * create a new executor
     *
     * @param monitor    logging subsystem
     * @param controller dataspace agreement
     */
    public DataspaceServiceExecutor(Monitor monitor, AgreementController controller, AgentConfig config, OkHttpClient client, ExecutorService executor, TypeManager typeManager) {
        this.monitor = monitor;
        this.agreementController = controller;
        this.config = config;
        this.client = new HttpClientAdapter(client);
        this.executor = executor;
        this.objectMapper = typeManager.getMapper();
    }

    /**
     * bulk execution call - this is the default
     * TODO implement batch size per service and not globally
     *
     * @param opService bound operator
     * @param queryIterator incoming bindings (may set service uri and input params)
     * @param executionContext context
     * @param serviceExecutorBulk bulk executor
     * @return binding generating iterator
     */
    @Override
    public QueryIterator createExecution(OpService opService, QueryIterator queryIterator, ExecutionContext executionContext, ServiceExecutorBulk serviceExecutorBulk) {
        Node serviceNode = opService.getService();
        Set<String> boundVars = new HashSet<>();
        long batchSize = config.getFederationServiceBatchSize();

        //
        // returns an iterator over batches
        //
        return new QueryIter1(queryIterator, executionContext) {

            // the active iterator over the current batch
            private QueryIterator batchIterator;

            /**
             * check whether we still have something left in the current iterator
             * or switch to the next batch
             */
            @Override
            protected boolean hasNextBinding() {
                return (batchIterator != null && batchIterator.hasNext()) || hasNextResultBinding();
            }

            /**
             * switch to the next batch
             *
             * @return whether next batch exists
             */
            public boolean hasNextResultBinding() {
                // do we have additional input bindings
                if (this.getInput().hasNext()) {
                    // yes then read the next batch
                    Map<String, List<Binding>> bindings = new HashMap<>();
                    long batchLength = 0;
                    while (this.getInput().hasNext() && batchLength++ < batchSize) {
                        Binding binding = this.getInput().next();
                        Iterator<Var> vars = binding.vars();
                        while (vars.hasNext()) {
                            boundVars.add(vars.next().getVarName());
                        }
                        // detect the service uri under the current binding
                        Node keyNode = serviceNode;
                        if (keyNode.isVariable()) {
                            keyNode = binding.get((Var) keyNode);
                        }
                        if (keyNode.isURI()) {
                            String key = keyNode.getURI();
                            if (!bindings.containsKey(key)) {
                                bindings.put(key, new ArrayList<>());
                            }
                            bindings.get(key).add(binding);
                        } else {
                            monitor.warning("Omitting a call because of lacking service binding");
                        }
                    }
                    ExecutionContext ctx = this.getExecContext();

                    List<Future<QueryIterator>> futureBindings = bindings.entrySet().stream().map(serviceSpec -> executor.submit(() ->
                            createExecution(opService, serviceSpec.getKey(), boundVars, serviceSpec.getValue(), ctx))).collect(Collectors.toList());

                    batchIterator = new QueryIterFutures(config, monitor, config.getControlPlaneManagementUrl(), config.getDefaultAsset(), serviceNode, ctx.getContext(), futureBindings);
                    return hasNextBinding();
                } else {
                    return false;
                }
            }

            /**
             * the hasNextBinding call has already been done, so we simply call next
             * on the current iterator - it should be there, otherwise it behaves as an orinary
             * iterator who has no next binding
             */
            @Override
            protected Binding moveToNextBinding() {
                return batchIterator.next();
            }

            /**
             * no explicit sub canceling implemented, http is stateless in this respect
             */
            @Override
            protected void requestSubCancel() {

            }

            /**
             * no explicit closing implemented, http is stateless in this respect
             */
            @Override
            protected void closeSubIterator() {
            }

        };
    }

    /**
     * single execution mode - this is not used anymore - batch mode is default
     *
     * @param opExecute  the bound operator (if variable is used in service description)
     * @param opOriginal the unbound operator
     * @param binding    the current binding
     * @param execCxt    the execution context
     * @return a set of query results
     */
    @Override
    public QueryIterator createExecution(OpService opExecute, OpService opOriginal, Binding binding, ExecutionContext execCxt) {
        // it maybe that a subselect has "masked" some input variables
        Node serviceNode = opExecute.getService();
        if (serviceNode.isVariable()) {
            serviceNode = binding.get((Var) serviceNode);
        }
        if (!serviceNode.isURI()) {
            throw new QueryExecException("Service URI not bound: " + opExecute.getService());
        }
        // check whether we need to route over EDC
        String target = serviceNode.getURI();
        Set<String> allowedVars = new HashSet<>();
        Iterator<Var> allAllowedVars = binding.vars();
        while (allAllowedVars.hasNext()) {
            allowedVars.add(allAllowedVars.next().getVarName());
        }
        return createExecution(opOriginal, target, allowedVars, List.of(binding), execCxt);
    }

    /**
     * (re-) implements the remote http service execution
     *
     * @param opOriginal the unbound operator
     * @param serviceUrl uri of the target service
     * @param boundVars a set of all bound variables
     * @param bindings   the current bindings
     * @param execCxt    the execution context
     * @return a set of query results
     */
    public QueryIterator createExecution(OpService opOriginal, String serviceUrl, Set<String> boundVars, List<Binding> bindings, ExecutionContext execCxt) {
        Context context = execCxt.getContext();

        // we have to only check outgoing URLs which have not already been checked
        String targetUrl = context.get(DataspaceServiceExecutor.TARGET_URL_SYMBOL);
        if (!serviceUrl.equalsIgnoreCase(targetUrl)) {
            // check whether the service url is allowed (in the context, in the default)
            Pattern allowPattern = context.get(ALLOW_SYMBOL, config.getServiceAllowPattern());
            if (!allowPattern.matcher(serviceUrl).matches()) {
                throw new QueryExecException(String.format("The service %s does not match the allowed pattern %s. Aborted execution.", serviceUrl, allowPattern.pattern()));
            }

            // check whether the service url is denied (in the context, in the default)
            Pattern denyPattern = context.get(DENY_SYMBOL, config.getServiceDenyPattern());
            if (denyPattern.matcher(serviceUrl).matches()) {
                throw new QueryExecException(String.format("The service %s matches the denied pattern %s. Aborted execution.", serviceUrl, denyPattern.pattern()));
            }
        }

        boolean silent = opOriginal.getSilent();

        // derive the asset type from the service URL, if possible
        // otherwise we will get it from the endpoint address after a ngotiation
        String assetType = serviceUrl.contains("Skill") ? "cx-common:SkillAsset" : serviceUrl.contains("Graph") ? "cx-common:GraphAsset" : "cx-common:Asset";

        // in case we have an EDC target, we need to negotiate/proxy the transfer
        Matcher edcMatcher = EDC_TARGET_ADDRESS_PATTERN.matcher(serviceUrl);
        if (edcMatcher.matches()) {

            //
            // EDC case: negotiate and proxy the transfer
            //

            monitor.info(String.format("About to execute edc target %s via dataspace", serviceUrl));
            String remoteUrl = edcMatcher.group("connector");
            if (remoteUrl == null || remoteUrl.length() == 0) {
                remoteUrl = config.getControlPlaneIdsUrl();
            } else {
                if ("edcs".equals(edcMatcher.group("protocol"))) {
                    remoteUrl = "https://" + remoteUrl;
                } else {
                    remoteUrl = "http://" + remoteUrl;
                }
            }
            String asset = edcMatcher.group("asset");
            if (asset == null || asset.length() == 0) {
                GraphRewriteVisitor grv = new GraphRewriteVisitor();
                GraphRewrite gr = new GraphRewrite(monitor, bindings, grv);
                Op transformed = Transformer.transform(gr, opOriginal.getSubOp(), grv, null);
                opOriginal = new OpService(opOriginal.getService(), transformed, opOriginal.getSilent());
                Set<String> graphNames = gr.getGraphNames();
                if (graphNames.size() > 1) {
                    throw new QueryExecException("There are several graph assets (currently not supported due to negotiation strategy, please rewrite your query) under EDC-based service: " + serviceUrl);
                } else {
                    Optional<String> graphName = graphNames.stream().findAny();
                    if (graphName.isEmpty()) {
                        throw new QueryExecException("There is no graph asset under EDC-based service: " + serviceUrl);
                    } else {
                        asset = graphName.get();
                    }
                }
            }
            EndpointDataReference endpoint = agreementController.get(asset);
            if (endpoint == null) {
                endpoint = agreementController.createAgreement(remoteUrl, asset);
                if (endpoint == null) {
                    throw new QueryExecException(String.format("Could not get an endpoint calback from connector %s to asset %s - Most likely this was a recursive call and you forgot to setup two control planes.", remoteUrl, asset));
                }
            }
            // the asset type should be annotated in the rdf type property
            assetType = endpoint.getProperties().getOrDefault("http://www.w3.org/1999/02/22-rdf-syntax-ns#type", assetType);

            // put the endpoint information into a new service operator
            // and cater for the EDC public api slash problem
            serviceUrl = endpoint.getEndpoint();
            if (!serviceUrl.endsWith("/")) {
                serviceUrl = serviceUrl + "/";
            }
            if (edcMatcher.group("params") != null) {
                serviceUrl = serviceUrl + "?" + edcMatcher.group("params");
            }
            Map<String, Map<String, List<String>>> allServiceParams = context.get(Service.serviceParams);
            if (allServiceParams == null) {
                allServiceParams = new HashMap<>();
                context.put(Service.serviceParams, allServiceParams);
            }
            Map<String, List<String>> serviceParams = allServiceParams.computeIfAbsent(serviceUrl, k -> new HashMap<>());
            serviceParams.put("cx_accept", List.of("application/json"));
            execCxt.getContext().put(AUTH_KEY_SYMBOL, endpoint.getAuthKey());
            execCxt.getContext().put(AUTH_CODE_SYMBOL, endpoint.getAuthCode());
        } else {
            monitor.info(String.format("About to execute http target %s without dataspace", serviceUrl));
        }

        // Next case distinction: we could either have a query or
        // a direct skill call
        if (!assetType.contains("Skill")) {
            // http execute with headers and such
            try {
                Op opRemote = opOriginal.getSubOp();
                int hashCode = Math.abs(opRemote.hashCode());
                String bindingVarName = "binding" + hashCode;
                Var idVar = Var.alloc(bindingVarName);
                VariableDetector vd = new VariableDetector(boundVars);
                opRemote = NodeTransformLib.transform(vd, opRemote);
                List<Var> neededVars = vd.getVariables();
                Map<String, Binding> resultingBindings = new HashMap<>();
                Map<Node, List<Binding>> newBindings = new HashMap<>();
                for (Binding originalBinding : bindings) {
                    StringBuilder keyBuilder = new StringBuilder();
                    BindingBuilder bb = BindingBuilder.create();
                    for (Var neededVar : neededVars) {
                        Node node = originalBinding.get(neededVar);
                        keyBuilder.append(neededVar.getVarName());
                        keyBuilder.append("#");
                        keyBuilder.append(node.toString());
                        bb.add(neededVar, node);
                    }
                    String key = keyBuilder.toString();
                    Node keyNode;
                    if (resultingBindings.containsKey(key)) {
                        Binding existingBinding = resultingBindings.get(key);
                        keyNode = existingBinding.get(idVar);
                    } else {
                        keyNode = NodeFactory.createLiteral(String.valueOf(resultingBindings.size()));
                        bb.add(idVar, keyNode);
                        newBindings.put(keyNode, new ArrayList<>());
                        Binding newBinding = bb.build();
                        //bb = BindingBuilder.create(newBinding);
                        resultingBindings.put(key, newBinding);
                    }
                    final BindingBuilder bb2 = BindingBuilder.create(originalBinding);
                    bb2.set(idVar, keyNode);
                    newBindings.get(keyNode).add(bb2.build());
                }
                neededVars.add(idVar);
                TableData table = new TableData(neededVars, new ArrayList<>(resultingBindings.values()));
                OpTable opTable = OpTable.create(table);

                Query query;

                // do we have a "sub-select", then we smuggle our binding into it
                if (opRemote instanceof OpProject) {
                    OpProject opRemoteProject = (OpProject) opRemote;
                    Op join = OpSequence.create(opTable, opRemoteProject.getSubOp());
                    List<Var> resultVars = opRemoteProject.getVars();
                    resultVars.add(idVar);
                    query = OpAsQuery.asQuery(new OpProject(join, resultVars));
                } else {
                    Op join = OpSequence.create(opTable, opRemote);
                    query = OpAsQuery.asQuery(join);
                }

                monitor.debug(String.format("Prepared target %s for query %s", serviceUrl, query));

                // -- Setup
                //boolean withCompression = context.isTrueOrUndef(httpQueryCompression);
                long timeoutMillis = config.getReadTimeout();

                // RegistryServiceModifier is applied by QueryExecHTTP
                Params serviceParams = getServiceParamsFromContext(serviceUrl, context);
                HttpClient httpClient = chooseHttpClient(serviceUrl, context);

                QuerySendMode querySendMode = chooseQuerySendMode(serviceUrl, context, QuerySendMode.asGetWithLimitBody);
                // -- End setup

                // Build the execution
                QueryExecutorBuilder queryExecutorBuilder = QueryExecutor.newBuilder()
                        .endpoint(serviceUrl)
                        .timeout(timeoutMillis, TimeUnit.MILLISECONDS)
                        .query(query)
                        .params(serviceParams)
                        .context(context)
                        .httpClient(httpClient)
                        .objectMapper(objectMapper)
                        .agentConfig(config)
                        .sendMode(querySendMode);

                if (context.isDefined(AUTH_KEY_SYMBOL)) {
                    String authKeyProp = context.get(AUTH_KEY_SYMBOL);
                    monitor.debug(String.format("About to use authentication header %s on http target %s", authKeyProp, serviceUrl));
                    String authCodeProp = context.get(AUTH_CODE_SYMBOL);
                    queryExecutorBuilder = queryExecutorBuilder.httpHeader(authKeyProp, authCodeProp);
                }

                try (QueryExecutor qExec = queryExecutorBuilder.build()) {
                    // Detach from the network stream.
                    RowSet rowSet = qExec.select().materialize();
                    QueryIterator queryIterator = QueryIterPlainWrapper.create(rowSet);
                    queryIterator = QueryIter.makeTracked(queryIterator, execCxt);
                    return new QueryIterJoin(queryIterator, newBindings, idVar, execCxt);
                }
            } catch (RuntimeException ex) {
                if (silent) {
                    Log.warn(this, "SERVICE " + serviceUrl + " : " + ex.getMessage());
                    // Return the input
                    return QueryIterPlainWrapper.create(bindings.iterator(), execCxt);
                }
                throw ex;
            }
        } else {
            // Skill call
            try {
                // [QExec] Add getSubOpUnmodified();
                Op opRemote = opOriginal.getSubOp();
                String bindingVarName = "binding";
                Var idVar = Var.alloc(bindingVarName);
                SkillVariableDetector vd = new SkillVariableDetector(boundVars);
                opRemote = Transformer.transform(vd, opRemote);
                Map<String, Node> neededVars = vd.getVariables();
                var parameterSet = new ResultSetMem() {
                    public void setVarNames(List<String> vars) {
                        this.varNames = vars;
                    }

                    public List<Binding> getRows() {
                        return this.rows;
                    }

                };
                List<String> vars = new ArrayList<>();
                vars.add(bindingVarName);
                neededVars.forEach((key1, value) -> vars.add(key1));
                parameterSet.setVarNames(vars);
                Map<String, Binding> resultingBindings = new HashMap<>();
                Map<Node, List<Binding>> newBindings = new HashMap<>();
                for (Binding originalBinding : bindings) {
                    StringBuilder keyBuilder = new StringBuilder();
                    BindingBuilder bb = BindingBuilder.create();
                    for (Map.Entry<String, Node> neededVar : neededVars.entrySet()) {
                        Node node = neededVar.getValue();
                        if (node.isVariable()) {
                            node = originalBinding.get((Var) node);
                        }
                        if (node != null) {
                            keyBuilder.append(neededVar.getKey());
                            keyBuilder.append("#");
                            keyBuilder.append(node);
                            bb.add(Var.alloc(neededVar.getKey()), node);
                        }
                    }
                    String key = keyBuilder.toString();
                    Node keyNode;
                    if (resultingBindings.containsKey(key)) {
                        Binding existingBinding = resultingBindings.get(key);
                        keyNode = existingBinding.get(idVar);
                    } else {
                        keyNode = NodeFactory.createLiteral(String.valueOf(resultingBindings.size()));
                        bb.add(idVar, keyNode);
                        newBindings.put(keyNode, new ArrayList<>());
                        Binding newBinding = bb.build();
                        //bb=BindingBuilder.create(newBinding);
                        resultingBindings.put(key, newBinding);
                    }
                    final BindingBuilder bb2 = BindingBuilder.create(originalBinding);
                    bb2.set(idVar, keyNode);
                    newBindings.get(keyNode).add(bb2.build());
                }
                parameterSet.getRows().addAll(resultingBindings.values());
                parameterSet.reset();
                long timeoutMillis = config.getReadTimeout();
                HttpClient httpClient = chooseHttpClient(serviceUrl, context);

                String bindingSet = ResultSetMgr.asString(parameterSet, ResultSetLang.RS_JSON);
                HttpRequest.Builder skillRequest = HttpRequest.newBuilder()
                        .uri(new URI(serviceUrl))
                        .header("Content-Type", WebContent.contentTypeResultsJSON)
                        .timeout(Duration.ofMillis(timeoutMillis))
                        .header("Accept", WebContent.contentTypeResultsJSON)
                        .POST(HttpRequest.BodyPublishers.ofString(bindingSet));

                if (context.isDefined(AUTH_KEY_SYMBOL)) {
                    String authKeyProp = context.get(AUTH_KEY_SYMBOL);
                    monitor.debug(String.format("About to use authentication header %s on http target %s", authKeyProp, serviceUrl));
                    String authCodeProp = context.get(AUTH_CODE_SYMBOL);
                    skillRequest = skillRequest.header(authKeyProp, authCodeProp);
                }

                HttpResponse<InputStream> remoteCall = httpClient.send(skillRequest.build(), HttpResponse.BodyHandlers.ofInputStream());
                if (remoteCall.statusCode() >= 200 && remoteCall.statusCode() < 300) {
                    ResultSet result = ResultSetMgr.read(remoteCall.body(), ResultSetLang.RS_JSON);
                    RowSet rowSet = new RowSetAdapter(result);
                    QueryIterator queryIterator = QueryIterPlainWrapper.create(rowSet);
                    queryIterator = QueryIter.makeTracked(queryIterator, execCxt);
                    return new QueryIterJoin(queryIterator, newBindings, idVar, execCxt);
                } else {
                    Log.warn(this, "SERVICE " + serviceUrl + " resulted in status code " + remoteCall.statusCode());
                    remoteCall.body().close();
                    // Return the input
                    return QueryIterPlainWrapper.create(bindings.iterator(), execCxt);
                }
            } catch (URISyntaxException | IOException | InterruptedException | RuntimeException ex) {
                if (silent) {
                    Log.warn(this, "SERVICE " + serviceUrl + " : " + ex.getMessage());
                    // Return the input
                    return QueryIterPlainWrapper.create(bindings.iterator(), execCxt);
                }
                throw new RuntimeException("Could not invoke remote skill", ex);
            }
        }
    }

    /**
     * choose an appropriate client
     *
     * @param serviceUrl target url
     * @param context    query context
     * @return http client
     */
    protected HttpClient chooseHttpClient(String serviceUrl, Context context) {
        if (context == null) {
            monitor.warning(String.format("Context is null when obtaining http client for %s", serviceUrl));
        }
        return client;
    }

    /**
     * choose an appropriate send mode
     *
     * @param serviceUrl target url
     * @param context    query content
     * @param dftValue   default send mode of dft
     * @return decided send mode
     */
    protected QuerySendMode chooseQuerySendMode(String serviceUrl, Context context, QuerySendMode dftValue) {
        if (dftValue != QuerySendMode.asPost) {
            monitor.warning(String.format("Default send mode %s for %s is not post", dftValue, serviceUrl));
        }
        if (context == null) {
            monitor.warning(String.format("Context is null when obtaining send mode for %s", serviceUrl));
        }
        return QuerySendMode.asPost;
    }

    /**
     * extract http params from query
     *
     * @param serviceUrl target url
     * @param context    query context
     * @return query params
     * @throws QueryExecException in case there is something wrong
     */
    protected Params getServiceParamsFromContext(String serviceUrl, Context context) throws QueryExecException {
        Params params = Params.create();

        Object obj = context.get(Service.serviceParams);

        if (obj == null) {
            return params;
        }

        // Old style.
        try {
            @SuppressWarnings("unchecked")
            Map<String, Map<String, List<String>>> serviceParams = (Map<String, Map<String, List<String>>>) obj;
            Map<String, List<String>> paramsMap = serviceParams.get(serviceUrl);
            if (paramsMap != null) {
                for (String param : paramsMap.keySet()) {
                    if (HttpParams.pQuery.equals(param)) {
                        throw new QueryExecException("ARQ serviceParams overrides the 'query' SPARQL protocol parameter");
                    }
                    List<String> values = paramsMap.get(param);
                    for (String value : values) {
                        params.add(param, value);
                    }
                }
            }
            return params;
        } catch (Throwable ex) {
            monitor.warning("Failed to process " + obj + " : context value of ARQ.serviceParams");
            return null;
        }
    }

}