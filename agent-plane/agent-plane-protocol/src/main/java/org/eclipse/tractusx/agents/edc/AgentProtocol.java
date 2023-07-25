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

/**
 * lists the various protocols supported
 */
public enum AgentProtocol {

    SPARQL_HTTP("cx-common:Protocol?w3c:http:SPARQL"),
    SKILL_HTTP("cx-common:Protocol?w3c:http:SKILL");

    private final String protocolId;

    AgentProtocol(String protocolId) {
        this.protocolId=protocolId;
    }

    public String getProtocolId() {
        return protocolId;
    }
}
