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

import org.eclipse.edc.spi.system.configuration.ConfigImpl;
import java.util.Map;

/**
 * test config impl
 */
public class TestConfig extends ConfigImpl {

    public TestConfig() {
        super("edc", Map.of("edc.cx.agent.controlplane.ids","test-tenant"));
    }
    
}
