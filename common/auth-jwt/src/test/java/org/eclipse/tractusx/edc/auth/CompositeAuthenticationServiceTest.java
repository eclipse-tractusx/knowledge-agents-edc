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
package org.eclipse.tractusx.edc.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class CompositeAuthenticationServiceTest {

    @BeforeEach
    public void initialize() {
    }

    @Test
    public void testEmpty() {
        CompositeAuthenticationService service=new CompositeAuthenticationService.Builder().build();
        assertTrue(service.isAuthenticated(Map.of()),"Could authenticate against empy composite service");
    }
}
