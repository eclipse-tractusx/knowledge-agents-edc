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
import org.apache.jena.sparql.algebra.optimize.OptimizerStd;
import org.apache.jena.sparql.util.Context;

/**
 * an modified standard optimization strategy which deals with federation and binding
 * of federation-important sparql constructs better at the level of joins
 */
public class Optimizer extends OptimizerStd {
    /**
     * Create a new optimizer
     *
     * @param context query context
     */
    public Optimizer(Context context) {
        super(context);
    }

    /**
     * override to choose the improved join straregy
     *
     * @param op operator to transform
     * @return transformed operator
     */
    @Override
    protected Op transformJoinStrategy(Op op) {
        return apply("Federated Index Join strategy", new OptimizeJoinStrategy(), op);
    }

}
