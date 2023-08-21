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

import org.eclipse.tractusx.agents.edc.AgentConfig;
import org.apache.jena.atlas.io.IndentedWriter;
import org.apache.jena.graph.Node;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.QueryIterator;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.iterator.QueryIteratorBase;
import org.apache.jena.sparql.serializer.SerializationContext;
import org.apache.jena.sparql.util.Context;
import org.eclipse.edc.spi.monitor.Monitor;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * A query iterator sitting on a set of future query iterators
 * It will get and produce contextual information in order to collect any
 * errors appearing.
 */
public class QueryIterFutures extends QueryIteratorBase {

    final List<Future<QueryIterator>> futures;
    QueryIterator current;
    Binding lastBinding;
    final Monitor monitor;
    final AgentConfig config;

    final String sourceTenant;
    final String sourceAsset;
    final Node targetNode;
    final Context executionContext;

    /**
     * creates a new future iterator
     * @param config agent config
     * @param monitor logging subsystem
     * @param sourceTenant the name/uri of the source tenant
     * @param targetNode a node (var, the name/uri of the remote tenant
     * @param sourceAsset the name of the calling/consuming graph
     * @param executionContext description of the execution context
     * @param futures list of futures to synchronize on
     */
    public QueryIterFutures(AgentConfig config, Monitor monitor, String sourceTenant, String sourceAsset, Node targetNode, Context executionContext, List<Future<QueryIterator>> futures) {
        this.futures=futures;
        this.monitor=monitor;
        this.config=config;
        this.sourceAsset=sourceAsset;
        this.sourceTenant=sourceTenant;
        this.targetNode=targetNode;
        this.executionContext=executionContext;
    }

    /**
     * @return whether any service has/will produce any binding
     */
    @Override
    protected boolean hasNextBinding() {
        return (current!=null && current.hasNext()) || hasNextInternalBinding();
    }

    /**
     * @return the last service node bindings uri
     */
    protected String getTargetTenant() {
        Node resolvedNode=targetNode;
        if(targetNode.isVariable() && lastBinding!=null) {
            resolvedNode=lastBinding.get((Var) targetNode);
        }
        if(resolvedNode!=null) {
            return resolvedNode.toString(false);
        }
        return "<UNKNOWN>";
    }

    /**
     * @return the last service node bindings asset
     */
    protected String getTargetAsset() {
        return getTargetTenant();
    }

    /**
     * move to the next ready-made future (or sync/poll for the next ready-made one in a recursion)
     * @return whether any service has/will produce any binding
     */
    boolean hasNextInternalBinding() {
        if(!futures.isEmpty()) {
            Optional<Future<QueryIterator>> boundFuture=futures.stream().filter(Future::isDone).findFirst();
            try {
                if(boundFuture.isPresent()) {
                    Future<QueryIterator> currentFuture=boundFuture.get();
                    futures.remove(currentFuture);
                    current = currentFuture.get();
                } else {
                    Thread.sleep(config.getNegotiationPollInterval());
                }
            }  catch(InterruptedException e) {
                List<CatenaxWarning> warnings=CatenaxWarning.getOrSetWarnings(executionContext);
                CatenaxWarning newWarning=new CatenaxWarning();
                newWarning.setSourceAsset(sourceAsset);
                newWarning.setSourceTenant(sourceTenant);
                newWarning.setTargetAsset(getTargetAsset());
                newWarning.setTargetTenant(getTargetTenant());
                newWarning.setContext(String.valueOf(executionContext.hashCode()));
                newWarning.setProblem("Timeout/Interruption invoking a remote batch: Result may be partial.");
                warnings.add(newWarning);
                monitor.warning(String.format("Produced warning %s for context %s",newWarning,executionContext),e);
            } catch(ExecutionException e) {
                List<CatenaxWarning> warnings=CatenaxWarning.getOrSetWarnings(executionContext);
                CatenaxWarning newWarning=new CatenaxWarning();
                newWarning.setSourceAsset(sourceAsset);
                newWarning.setSourceTenant(sourceTenant);
                newWarning.setTargetAsset(getTargetAsset());
                newWarning.setTargetTenant(getTargetTenant());
                newWarning.setContext(String.valueOf(executionContext.hashCode()));
                newWarning.setProblem("Failure invoking a remote batch: Result may be partial.");
                warnings.add(newWarning);
                monitor.warning(String.format("Produced warning %s for context %s",newWarning,executionContext),e);
            }
            return hasNextBinding();
        }
        return false;
    }

    @Override
    protected Binding moveToNextBinding() {
        lastBinding=current.next();
        return lastBinding;
    }

    @Override
    protected void closeIterator() {
        requestCancel();
        if(current!=null) {
            current.close();
            current = null;
        }
    }
    @Override
    protected void requestCancel() {
        futures.forEach( future -> future.cancel(true));
        futures.clear();
    }

    @Override
    public void output(IndentedWriter indentedWriter, SerializationContext serializationContext) {
    }
}
