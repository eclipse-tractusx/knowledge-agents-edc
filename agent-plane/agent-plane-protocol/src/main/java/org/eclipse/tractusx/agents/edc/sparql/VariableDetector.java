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

import org.apache.jena.graph.Node;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.graph.NodeTransform;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * a pseudo transform which visits every graph node
 * to list all variables. helps us to
 * only serialize the needed portion from
 * consumer to producer
 */
public class VariableDetector implements NodeTransform {

    HashMap<String,Var> variables=new HashMap<>();
    Set<String> allowed;

    public VariableDetector(Set<String> allowed) {
        this.allowed=allowed;
    }

    @Override
    public Node apply(Node node) {
        if(node.isVariable()) {
            Var var = (Var)node;
            String varName= var.getVarName();
            while(Var.isRenamedVar(varName)) {
                varName=varName.substring(1);
                var=Var.alloc(varName);
            }
            if(allowed.contains(varName) && !variables.containsKey(varName)) {
                variables.put(varName, var);
            }
            return var;
        }
        return node;
    }

    public List<Var> getVariables() {
        return variables.values().stream().collect(Collectors.toList());
    }
}
