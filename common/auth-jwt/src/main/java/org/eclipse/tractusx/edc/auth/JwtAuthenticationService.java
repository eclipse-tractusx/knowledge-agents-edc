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
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.JWSVerifier;
import org.eclipse.edc.api.auth.spi.AuthenticationService;

import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implements a JWT (Java Web Tokens) Based Authentication Service
 */
public class JwtAuthenticationService implements AuthenticationService {
    public static String AUTHENTICATION_HEADER="authorization";
    public static Pattern BEARER_TOKEN_VALUE=Pattern.compile("^Bearer ([a-zA-Z0-9+/\\-_]+\\.[a-zA-Z0-9+/\\-_]+\\.[a-zA-Z0-9+/\\-_]+)$");

    final protected  JWSVerifier verifier;
    final protected boolean checkExpiry;
    
    public JwtAuthenticationService(JWSVerifier verifier, boolean checkExpiry) {
        this.verifier=verifier;
        this.checkExpiry=checkExpiry;
    }

    @Override
    public boolean isAuthenticated(Map<String, List<String>> map) {
        return map.entrySet().stream()
                .filter( e->e.getKey().equalsIgnoreCase(AUTHENTICATION_HEADER))
                .flatMap( e->e.getValue().stream().map( v -> BEARER_TOKEN_VALUE.matcher(v)).filter(Matcher::matches)).
                    anyMatch(r->checkToken(r.group(1)));
    }

    protected boolean checkToken(String token) {
        try {
            JWSObject jwsObject = JWSObject.parse(token);
            boolean isVerified=jwsObject.verify(verifier);
            if(isVerified) {
                if(checkExpiry) {
                    Object expiryObject=jwsObject.getPayload().toJSONObject().get("exp");
                    if(expiryObject instanceof Long) {
                        // token times are in seconds
                        return !new Date((Long) expiryObject*1000).before(new Date());
                    } else {
                        return true;
                    }
                } else {
                    return true;
                }
            } else  {
                return false;
            }
        } catch(ParseException | JOSEException e) {
            return false;
        }
    }

    /**
     * a builder for jws authentication services
     */
    public static class Builder {
        protected JWSVerifier verifier;
        protected boolean checkExpiry=true;

        public Builder() {
        }

        public Builder setVerifier(JWSVerifier verifier) {
            this.verifier=verifier;
            return this;
        }

        public Builder setCheckExpiry(boolean check) {
            this.checkExpiry=check;
            return this;
        }

        public JwtAuthenticationService build() {
            assert Objects.nonNull(verifier);
            return new JwtAuthenticationService(verifier,checkExpiry);
        }
    }
}
