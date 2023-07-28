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
 */
public class MockAgreementController implements IAgreementController {

    @Override
    public EndpointDataReference get(String assetId) {
        EndpointDataReference.Builder builder= EndpointDataReference.Builder.newInstance();
        builder.endpoint("http://localhost:8080/sparql#"+assetId);
        return builder.build();
    }

    @Override
    public EndpointDataReference createAgreement(String remoteUrl, String asset) throws WebApplicationException {
        return get(asset);
    }

}
