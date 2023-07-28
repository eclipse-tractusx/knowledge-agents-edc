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
package org.eclipse.tractusx.agents.edc.http;

import java.lang.reflect.*;

import org.eclipse.edc.spi.monitor.Monitor;

/**
 * An invocation handler which maps all jakarta objects
 * to a javax.servlet level
 */
public class JakartaAdapter implements InvocationHandler, IJakartaAdapter<Object> {
    
    Object jakartaDelegate;
    Monitor monitor;

    public JakartaAdapter(Object jakartaDelegate, Monitor monitor) {
        this.jakartaDelegate=jakartaDelegate;
        this.monitor=monitor;
    }

    @Override
    public Object getDelegate() {
        return jakartaDelegate;
    }

    @Override
    public Monitor getMonitor() {
        return monitor;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Class[] types=method.getParameterTypes();
        args= IJakartaAdapter.unwrap(types,args);
        Method targetMethod=jakartaDelegate.getClass().getMethod(method.getName(),types);
        Object result=targetMethod.invoke(jakartaDelegate,args);
        //monitor.debug(String.format("Jakarta wrapper mapped method %s to target method %s on args %s with result %s",method,targetMethod,Arrays.toString(args),result));
        if((!method.getReturnType().isAssignableFrom(targetMethod.getReturnType())) && result!=null) {
            result= IJakartaAdapter.javaxify(result,method.getReturnType(),monitor);
        }
        return result;
    }

}
