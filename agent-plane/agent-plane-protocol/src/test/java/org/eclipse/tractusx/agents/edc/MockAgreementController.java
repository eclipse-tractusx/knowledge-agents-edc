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

import jakarta.ws.rs.WebApplicationException;
import org.eclipse.edc.spi.types.domain.edr.EndpointDataReference;

/**
 * mock agreement controller for testing purposes
 * It will "fake" an agreement endpoint that will finally
 * hit the given path/port on localhost
 */
public class MockAgreementController implements AgreementController {

    String path;
    int port;

    /**
     * specific mock controller
     * @param path path of endpoint
     * @param port port of endpoint
     */
    public MockAgreementController(String path, int port) {
        this.path=path;
        this.port=port;
    }

    /**
     * default mock controller
     */
    public MockAgreementController() {
        this.path="sparql";
        this.port=8080;
    }

    /**
     * create a faked endpoint reference
     * @param assetId id of the asset
     * @return a data reference pointing to the internal endpoint
     */
    @Override
    public EndpointDataReference get(String assetId) {
        EndpointDataReference.Builder builder= EndpointDataReference.Builder.newInstance();
        builder.id(assetId).contractId(assetId).endpoint(String.format("http://localhost:%d/%s#%s",port,path,assetId));
        return builder.build();
    }

    /**
     * negotiation case, delegates to get
     * @param remoteUrl the connector
     * @param asset id of the asset
     * @return a data reference pointing to the internal endpoint
     * @throws WebApplicationException in case something goes wrong
     */
    @Override
    public EndpointDataReference createAgreement(String remoteUrl, String asset) throws WebApplicationException {
        return get(asset);
    }

}
