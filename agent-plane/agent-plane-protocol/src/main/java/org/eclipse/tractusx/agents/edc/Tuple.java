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
package org.eclipse.tractusx.agents.edc;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * A tuple contains a binding of variables to a single value. 
 */
public class Tuple {

    Map<String,String> bindings;
    
    /**
     * create a fresh tuple
     */
    public Tuple() {
        bindings=new HashMap<>();
    }

    /**
     * create a tuple with existing bindings
     * @param bindings map of variable names to string values
     */
    public Tuple(Map<String,String> bindings) {
        this.bindings=bindings;
    }

    /**
     * adds a binding
     * @param key variable name
     * @param value string-based value
     * @throws Exception in case the variable is already bound
     */
    public void add(String key, String value) throws Exception {
        if(bindings.containsKey(key)) {
            throw new Exception(String.format("Cannot host several values for key %s in simple binding.",key));
        }
        bindings.put(key,value);
    }

    /**
     * access a binding
     * @param key variable name
     * @return bound value (null of not bound)
     */
    public String get(String key) {
        return bindings.get(key);
    }

    /**
     * @return the set of bound variables
     */
    public Set<String> getVariables() {
        return bindings.keySet();
    }

    /**
     * clone this tuple
     * @return a detached tuple with the same bindings
     */
    @Override
    public Tuple clone() {
        return new Tuple(new HashMap<>(bindings));
    }

    /**
     * merges this tuple with another
     * @param other other tuple
     * @return a detached tuple with the combined bindings of this an the other tuple
     */
    public Tuple merge(Tuple other) {
        Map<String,String> newTuple=new HashMap<>(bindings);
        newTuple.putAll(other.bindings);
        return new Tuple(newTuple);
    }

    /**
     * render this tuple
     */
    @Override
    public String toString() {
        return "Tuple("+bindings.toString()+")";
    }
}
