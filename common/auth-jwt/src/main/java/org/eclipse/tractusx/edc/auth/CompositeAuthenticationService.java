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