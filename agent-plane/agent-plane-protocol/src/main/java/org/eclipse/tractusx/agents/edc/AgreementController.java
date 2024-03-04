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
package org.eclipse.tractusx.agents.edc;

import jakarta.ws.rs.WebApplicationException;
import org.eclipse.edc.spi.types.domain.edr.EndpointDataReference;

/**
 * Interface to any agreement controller
 */
public interface AgreementController {

    /**
     * check whether an agreement for the asset already exists
     *
     * @param asset id of the asset
     * @return endpoint data reference, null if non-existant
     */
    EndpointDataReference get(String asset);

    /**
     * negotiates an endpoint for the given asset
     *
     * @param remoteUrl the connector
     * @param asset     id of the asset
     * @return endpoint data reference
     * @throws WebApplicationException in case agreement could not be made (in time)
     */
    EndpointDataReference createAgreement(String remoteUrl, String asset) throws WebApplicationException;
}
