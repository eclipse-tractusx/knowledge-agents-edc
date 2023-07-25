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

import org.apache.jena.sparql.algebra.OpVisitorBase;
import org.apache.jena.sparql.algebra.op.OpService;

/**
 * A visitor that indicates stop when inside a service
 */
public class GraphRewriteVisitor extends OpVisitorBase {
    protected boolean inService=false;

    @Override
    public void visit(OpService opService) {
        super.visit(opService);
        inService=true;
    }
}
