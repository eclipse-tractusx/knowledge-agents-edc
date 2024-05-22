// Copyright (c) 2022,2024 Contributors to the Eclipse Foundation
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
package org.eclipse.tractusx.agents.edc.http.transfer;

import org.eclipse.edc.connector.dataplane.http.params.decorators.BaseCommonHttpParamsDecorator;
import org.eclipse.edc.connector.dataplane.http.params.decorators.BaseSinkHttpParamsDecorator;
import org.eclipse.edc.connector.dataplane.http.spi.HttpDataAddress;
import org.eclipse.edc.connector.dataplane.http.spi.HttpParamsDecorator;
import org.eclipse.edc.connector.dataplane.http.spi.HttpRequestParams;
import org.eclipse.edc.connector.dataplane.http.spi.HttpRequestParamsProvider;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;
import org.eclipse.tractusx.agents.edc.AgentConfig;

import java.util.ArrayList;
import java.util.List;


/**
 * request params supplier which correctly double encodes
 * valid url symbols in the parameter section, extracts
 * original headers like accept from special parameters and caters for
 * translating url-encoded form bodies into their
 * "normal" param+body form
 */
public class AgentSourceRequestParamsSupplier implements HttpRequestParamsProvider {
    protected final List<HttpParamsDecorator> sourceDecorators = new ArrayList<>();
    protected final List<HttpParamsDecorator> sinkDecorators = new ArrayList<>();

    /**
     * the edc config section
     */
    protected final AgentConfig config;

    /**
     * logging subsystem
     */
    protected  Monitor monitor;

    /**
     * creates a supplier
     *
     * @param vault secret host
     * @param config edc config section
     * @param monitor logging reference
     */
    public AgentSourceRequestParamsSupplier(Vault vault, TypeManager typeManager, AgentConfig config, Monitor monitor) {
        BaseCommonHttpParamsDecorator commonHttpParamsDecorator = new BaseCommonHttpParamsDecorator(vault, typeManager);
        this.registerSinkDecorator(commonHttpParamsDecorator);
        this.registerSourceDecorator(commonHttpParamsDecorator);
        this.registerSourceDecorator(new AgentSourceHttpParamsDecorator(config, monitor));
        this.registerSinkDecorator(new BaseSinkHttpParamsDecorator());
        this.config = config;
        this.monitor = monitor;
    }

    @Override
    public void registerSourceDecorator(HttpParamsDecorator decorator) {
        this.sourceDecorators.add(decorator);
    }

    @Override
    public void registerSinkDecorator(HttpParamsDecorator decorator) {
        this.sinkDecorators.add(decorator);
    }

    @Override
    public HttpRequestParams provideSourceParams(DataFlowStartMessage request) {
        HttpRequestParams.Builder params = HttpRequestParams.Builder.newInstance();
        HttpDataAddress address = HttpDataAddress.Builder.newInstance().copyFrom(request.getSourceDataAddress()).build();
        this.sourceDecorators.forEach((decorator) -> decorator.decorate(request, address, params));
        return params.build();
    }

    @Override
    public HttpRequestParams provideSinkParams(DataFlowStartMessage request) {
        HttpRequestParams.Builder params = HttpRequestParams.Builder.newInstance();
        HttpDataAddress address = HttpDataAddress.Builder.newInstance().copyFrom(request.getDestinationDataAddress()).build();
        this.sinkDecorators.forEach((decorator) -> decorator.decorate(request, address, params));
        return params.build();
    }
    
    /**
     * provides the address of a matchmaking agent fitting 
     * to the data flow request
     *
     * @param request the data flow request
     * 
     * @return a fitting matchmaking agent address
     */
    public String provideMatchmakingUrl(DataFlowStartMessage request) {
        return config.getMatchmakingAgentUrl();
    }
    
}
