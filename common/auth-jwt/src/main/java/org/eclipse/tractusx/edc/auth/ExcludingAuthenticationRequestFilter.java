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

import jakarta.ws.rs.container.ContainerRequestContext;
import org.eclipse.edc.api.auth.spi.AuthenticationRequestFilter;
import org.eclipse.edc.api.auth.spi.AuthenticationService;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An authentication request filter with optional paths excluded
 */
public class ExcludingAuthenticationRequestFilter extends AuthenticationRequestFilter {

    /**
     * the regex describing the excluded paths
     */
    protected final Pattern excludePattern;

    /**
     * creates a new authentication request filter
     * @param authenticationService the actual authentication service
     * @param excludePattern the parsed regular expression of excluded paths, null if none
     */
    public ExcludingAuthenticationRequestFilter(AuthenticationService authenticationService, Pattern excludePattern) {
        super(authenticationService);
        this.excludePattern = excludePattern;
    }

    /**
     * delegates to parent implementation only if excluded path regex does match uri
     * @param requestContext request context.
     */
    @Override
    public void filter(ContainerRequestContext requestContext) {
        if(excludePattern != null) {
            Matcher excludeMatcher = excludePattern.matcher(
                    requestContext.getUriInfo().getAbsolutePath().toString()
            );
            if (excludeMatcher.matches()) {
                return;
            }
        }
        super.filter(requestContext);
    }
}
