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
import org.apache.jena.sparql.algebra.optimize.OptimizerStd;
import org.apache.jena.sparql.util.Context;

/**
 * an modified standard optimization strategy which deals with federation and binding
 * of federation-important sparql constructs better at the level of joins
 */
public class Optimizer extends OptimizerStd {
    /**
     * Create a new optimizer
     * @param context query context
     */
    public Optimizer(Context context) {
        super(context);
    }

    /**
     * override to choose the improved join straregy
     * @param op operator to transform
     * @return transformed operator
     */
    @Override
    protected Op transformJoinStrategy(Op op) {
        return apply("Federated Index Join strategy", new OptimizeJoinStrategy(), op);
    }

}
