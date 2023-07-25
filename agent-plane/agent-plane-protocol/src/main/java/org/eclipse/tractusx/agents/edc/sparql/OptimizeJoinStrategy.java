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
