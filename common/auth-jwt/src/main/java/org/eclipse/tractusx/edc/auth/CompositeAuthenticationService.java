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

    protected final Collection<AuthenticationService> subServices=new ArrayList<>();

    public CompositeAuthenticationService() {
    }

    @Override
    public boolean isAuthenticated(Map<String, List<String>> map) {
        return subServices.stream().noneMatch(service-> !service.isAuthenticated(map));
    }

    public static class Builder {
        CompositeAuthenticationService service;

        public Builder() {
            service=new CompositeAuthenticationService();
        }

        public Builder addService(AuthenticationService subService) {
            service.subServices.add(subService);
            return this;
        }

        public CompositeAuthenticationService build() {
            return service;
        }

    }
}