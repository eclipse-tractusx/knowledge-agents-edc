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
package org.eclipse.tractusx.edc.auth;

import com.nimbusds.jose.JOSEException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * tests the composite service
 */
public class CompositeAuthenticationServiceTest {


    @BeforeEach
    public void initialize() {
    }

    @Test
    public void testEmptyDefault() {
        CompositeAuthenticationService service = new CompositeAuthenticationService.Builder().build();
        assertTrue(service.isAuthenticated(Map.of()), "Should authenticate against empty default composite service");
    }

    @Test
    public void testEmptyAll() {
        CompositeAuthenticationService service = new CompositeAuthenticationService.Builder().setMode(CompositeAuthenticationMode.ALL).build();
        assertTrue(service.isAuthenticated(Map.of()), "Should authenticate against empty ALL composite service");
    }

    @Test
    public void testEmptyOne() {
        CompositeAuthenticationService service = new CompositeAuthenticationService.Builder().setMode(CompositeAuthenticationMode.ONE).build();
        assertFalse(service.isAuthenticated(Map.of()), "Should not authenticate against empty ONE composite service");
    }

    @Test
    public void testOne() {
        String key1 = UUID.randomUUID().toString();
        String key2 = UUID.randomUUID().toString();
        CompositeAuthenticationService service = new CompositeAuthenticationService.Builder().setMode(CompositeAuthenticationMode.ONE).
                addService(new ApiKeyAuthenticationService.Builder().setReference(key1.hashCode()).build()).
                addService(new ApiKeyAuthenticationService.Builder().setReference(key2.hashCode()).build()).build();
        assertTrue(service.isAuthenticated(Map.of("x-api-key", List.of(key1))), "Should authenticate against ONE composite service");
        assertTrue(service.isAuthenticated(Map.of("x-api-key", List.of(key2))), "Should authenticate against ONE composite service");
        assertFalse(service.isAuthenticated(Map.of("x-api-key", List.of(UUID.randomUUID().toString()))), "Should not authenticate against ONE composite service");
        assertFalse(service.isAuthenticated(Map.of("api-key", List.of(key1))), "Should not authenticate against ONE composite service");
    }

    @Test
    public void testAll() throws JOSEException {
        String key1 = UUID.randomUUID().toString();
        JwtAuthenticationServiceTest jwsTest;
        jwsTest = new JwtAuthenticationServiceTest();
        jwsTest.initialize();
        CompositeAuthenticationService service = new CompositeAuthenticationService.Builder().setMode(CompositeAuthenticationMode.ALL).
                addService(new ApiKeyAuthenticationService.Builder().setReference(key1.hashCode()).build()).
                addService(jwsTest.getService()).build();
        assertTrue(service.isAuthenticated(Map.of("x-api-key", List.of(key1), "Authorization", List.of("Bearer " + jwsTest.getToken()))), "Should authenticate against ALL composite service");
        assertFalse(service.isAuthenticated(Map.of("x-api-key", List.of(key1))), "Should authenticate against ALL composite service");
        assertFalse(service.isAuthenticated(Map.of("Authorization", List.of("Bearer " + jwsTest.getToken()))), "Should authenticate against ALL composite service");
    }

}
