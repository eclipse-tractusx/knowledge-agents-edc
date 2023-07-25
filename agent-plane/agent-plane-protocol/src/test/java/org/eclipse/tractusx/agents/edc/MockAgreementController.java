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
