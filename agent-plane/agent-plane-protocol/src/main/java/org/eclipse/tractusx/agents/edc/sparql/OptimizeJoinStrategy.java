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

import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.op.*;
import org.apache.jena.sparql.algebra.optimize.TransformJoinStrategy;
import org.apache.jena.sparql.engine.main.JoinClassifier;

/**
 * a modified default join strategy which will always linearize right-hand
 * service and union calls in order to obtain bindings from the
 * left part.
 * TODO improve to find independent/asynchronous calls which should be parallelized (cross-join case)
 */
public class OptimizeJoinStrategy extends TransformJoinStrategy {

    /**
     * implement the federated join strategy
     * @param opJoin operator to optimize
     * @param left left-part of join
     * @param right right-part of join
     * @return transformed join operator
     */
    @Override
    public Op transform(OpJoin opJoin, Op left, Op right) {
        boolean canDoLinear = JoinClassifier.isLinear(opJoin);
        if (!canDoLinear) {
            if(right instanceof OpService || right instanceof OpUnion) {
                // join no-matter what with a service or a union
                return OpSequence.create(left, right);
            }
            if(left instanceof OpService || left instanceof OpGraph) {
                // join no matter after service and graph calls
                return OpSequence.create(left, right);
            }
            if(left instanceof OpSequence && right instanceof OpSequence) {
                // join two sequences
                return OpSequence.create(left, right);
            }
        }
        // default transform
        return super.transform(opJoin, left, right);
    }
}
