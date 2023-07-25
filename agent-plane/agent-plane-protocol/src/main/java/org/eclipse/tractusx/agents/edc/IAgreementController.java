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
package org.eclipse.tractusx.agents.edc;

import jakarta.ws.rs.WebApplicationException;
import org.eclipse.edc.spi.types.domain.edr.EndpointDataReference;

/**
 * Interface to any agreement controller
 */
public interface IAgreementController {

    /**
     * check whether an agreement for the asset already exists
     * @param asset id of the asset
     * @return endpoint data reference, null if non-existant
     */
    EndpointDataReference get(String asset);
    /**
     * negotiates an endpoint for the given asset
     * @param remoteUrl the connector
     * @param asset id of the asset
     * @return endpoint data reference
     * @throws WebApplicationException in case agreement could not be made (in time)
     */
    EndpointDataReference createAgreement(String remoteUrl, String asset) throws WebApplicationException;
}
