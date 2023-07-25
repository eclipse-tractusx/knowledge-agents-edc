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
package org.eclipse.tractusx.agents.edc.model;

import org.eclipse.edc.policy.model.Policy;

public class ContractOfferDescription {
    private final String offerId;
    private final String assetId;
    private final OdrlPolicy policy;

    public ContractOfferDescription(String offerId,
                                    String assetId,
                                    OdrlPolicy policy) {
        this.offerId = offerId;
        this.assetId = assetId;
        this.policy = policy;
    }

    public String getOfferId() {
        return this.offerId;
    }

    public String getAssetId() {
        return this.assetId;
    }

    public OdrlPolicy getPolicy() {
        return this.policy;
    }
}