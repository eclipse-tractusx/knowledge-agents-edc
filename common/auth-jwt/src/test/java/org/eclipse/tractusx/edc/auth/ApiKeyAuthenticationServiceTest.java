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
 * Tests the api-key service
 */

public class ApiKeyAuthenticationServiceTest {
    ApiKeyAuthenticationService service;

    String key;

    @BeforeEach
    public void initialize() throws JOSEException {
        key = UUID.randomUUID().toString();
        service = new ApiKeyAuthenticationService(key.hashCode());
    }

    @Test
    public void testValidKey() {
        var headers = Map.of("x-api-key", List.of(key));
        assertTrue(service.isAuthenticated(headers), "Could not authenticate using valid api key");
    }

    @Test
    public void testInvalidKey() {
        var headers = Map.of("x-api-key", List.of(UUID.randomUUID().toString()));
        assertFalse(service.isAuthenticated(headers), "Could authenticate using invalid key");
    }

    @Test
    public void testInvalidHeader() {
        var headers = Map.of("api-key", List.of(key));
        assertFalse(service.isAuthenticated(headers), "Could authenticate using invalid header");
    }

}
