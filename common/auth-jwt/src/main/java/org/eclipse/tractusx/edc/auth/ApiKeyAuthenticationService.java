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

import org.eclipse.edc.api.auth.spi.AuthenticationService;

import java.util.List;
import java.util.Map;

/**
 * Implements an Api-Key Based Authentication Service
 * we use a hashcode based comparison not to store a secret
 * in clear text in memory
 */
public class ApiKeyAuthenticationService implements AuthenticationService {
    public static String AUTHENTICATION_HEADER="x-api-key";
    final protected int reference;

    public ApiKeyAuthenticationService(int reference) {
        this.reference=reference;
    }

    @Override
    public boolean isAuthenticated(Map<String, List<String>> map) {
        return map.entrySet().stream()
                .filter( e->e.getKey().equalsIgnoreCase(AUTHENTICATION_HEADER))
                .flatMap( e->e.getValue().stream().map( v -> reference==v.hashCode()))
                .anyMatch(b->b);
    }

    /**
     * a builder for api key authentication services
     */
    public static class Builder {
        protected int reference;

        public Builder() {
        }

        public Builder setReference(int reference) {
            this.reference=reference;
            return this;
        }

        public ApiKeyAuthenticationService build() {
            return new ApiKeyAuthenticationService(reference);
        }
    }
}
