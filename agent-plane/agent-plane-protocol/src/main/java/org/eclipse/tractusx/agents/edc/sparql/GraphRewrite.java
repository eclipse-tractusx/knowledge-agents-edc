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

import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.TransformCopy;
import org.apache.jena.sparql.algebra.op.OpGraph;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;
import org.eclipse.edc.spi.monitor.Monitor;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * A rewriter which replaces graphs by their bindings
 * and collects the unique names. Is used for EDC calls
 * in order to deduce the necessary assets to negotiate
 * and replace variables for later exchange with the
 * backend service.
 */
public class GraphRewrite extends TransformCopy {

    protected final List<Binding> bindings;
    protected final Set<String> graphNames = new HashSet<>();

    protected final Monitor monitor;

    protected final GraphRewriteVisitor visitor;

    public GraphRewrite(Monitor monitor, List<Binding> bindings, GraphRewriteVisitor visitor) {
        this.bindings = bindings;
        this.monitor = monitor;
        this.visitor = visitor;
    }


    @Override
    public OpGraph transform(OpGraph op, Op subOp) {
        if (!visitor.inService) {
            Node graphNode = op.getNode();
            if (graphNode.isURI()) {
                String graphString = graphNode.getURI();
                if (graphString.startsWith(SparqlQueryProcessor.UNSET_BASE)) {
                    graphString = graphString.substring(SparqlQueryProcessor.UNSET_BASE.length());
                    op = new OpGraph(NodeFactory.createURI(graphString), subOp);
                }
                graphNames.add(graphString);
            } else if (graphNode.isLiteral()) {
                String graphString = String.valueOf(graphNode.getLiteralValue());
                if (graphString.startsWith(SparqlQueryProcessor.UNSET_BASE)) {
                    graphString = graphString.substring(SparqlQueryProcessor.UNSET_BASE.length());
                    op = new OpGraph(NodeFactory.createURI(graphString), subOp);
                }
                graphNames.add(graphString);
            } else if (graphNode.isVariable()) {
                Var graphVar = (Var) graphNode;
                if (bindings == null || bindings.isEmpty()) {
                    monitor.warning(String.format("Found a graph node %s which is a variable but no binding. Ignoring.", graphVar));
                } else {
                    Iterator<Binding> allBindings = bindings.iterator();
                    Node bound = null;
                    while (bound == null && allBindings.hasNext()) {
                        Binding binding = allBindings.next();
                        if (binding.contains(graphVar)) {
                            bound = binding.get(graphVar);
                        }
                    }
                    if (bound != null) {
                        if (bound.isURI()) {
                            String graphString = bound.getURI();
                            if (graphString.startsWith(SparqlQueryProcessor.UNSET_BASE)) {
                                graphString = graphString.substring(SparqlQueryProcessor.UNSET_BASE.length());
                            }
                            graphNames.add(graphString);
                            op = new OpGraph(NodeFactory.createURI(graphString), subOp);
                        } else if (bound.isLiteral()) {
                            String graphString = String.valueOf(bound.getLiteralValue());
                            if (graphString.startsWith(SparqlQueryProcessor.UNSET_BASE)) {
                                graphString = graphString.substring(SparqlQueryProcessor.UNSET_BASE.length());
                            }
                            graphNames.add(graphString);
                            op = new OpGraph(NodeFactory.createURI(graphString), subOp);
                        } else {
                            monitor.warning(String.format("Found a graph node binding %s which is no uri or literal. Ignoring.", bound));
                        }
                    } else {
                        monitor.warning(String.format("Found a graph node %s that is not bound. Ignoring.", bound));
                    }
                }
            } else {
                monitor.warning(String.format("Found a graph node %s which is neither uri, literal or variable. Ignoring.", graphNode));
            }
        }
        return op;
    }

    /**
     * access
     *
     * @return set of graph names/assets found
     */
    public Set<String> getGraphNames() {
        return graphNames;
    }

}
