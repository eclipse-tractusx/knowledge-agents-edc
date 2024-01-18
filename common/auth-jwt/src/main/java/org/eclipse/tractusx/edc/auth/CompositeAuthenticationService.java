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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class CompositeAuthenticationService implements AuthenticationService {

    protected final Collection<AuthenticationService> subServices;
    protected final CompositeAuthenticationMode mode;

    public CompositeAuthenticationService(CompositeAuthenticationMode mode, Collection<AuthenticationService> subServices) {
        this.mode = mode;
        this.subServices = subServices;
    }

    @Override
    public boolean isAuthenticated(Map<String, List<String>> map) {
        switch (mode) {
            case ONE:
                return subServices.stream().anyMatch(service -> service.isAuthenticated(map));
            case ALL:
            default:
                return subServices.stream().allMatch(service -> service.isAuthenticated(map));
        }
    }

    public static class Builder {
        Collection<AuthenticationService> subServices = new ArrayList<>();
        CompositeAuthenticationMode mode = CompositeAuthenticationMode.ALL;

        public Builder() {
        }

        public Builder addService(AuthenticationService subService) {
            subServices.add(subService);
            return this;
        }

        public Builder setMode(CompositeAuthenticationMode mode) {
            this.mode = mode;
            return this;
        }


        public CompositeAuthenticationService build() {
            return new CompositeAuthenticationService(mode, subServices);
        }

    }
}