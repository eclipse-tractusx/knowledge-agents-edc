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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.eclipse.edc.spi.monitor.ConsoleMonitor;
import org.eclipse.edc.spi.monitor.Monitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * tests the jwt service
 */
public class JwtAuthenticationServiceTest {
    ObjectMapper om;
    Monitor monitor;

    CompositeJwsVerifier verifier;

    JwtAuthenticationService service;

    String token;
    String token2;

    public org.eclipse.tractusx.edc.auth.JwtAuthenticationService getService() {
        return service;
    }

    public String getToken2() {
        return token2;
    }

    public String getToken() {
        return token;
    }

    @BeforeEach
    public void initialize() throws JOSEException {
        monitor=new ConsoleMonitor();
        om = new ObjectMapper();
        RSAKey rsaJWK = new RSAKeyGenerator(2048)
                .keyID("123")
                .generate();
        RSAKey rsaPublicJWK = rsaJWK.toPublicJWK();
        JWSSigner signer = new RSASSASigner(rsaJWK);
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject("alice")
                .issuer("https://c2id.com")
                .expirationTime(new Date(1683529307))
                .issueTime(new Date(1683529007))
                .build();
        SignedJWT signedJWT = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(rsaJWK.getKeyID()).build(),
                claimsSet);
        signedJWT.sign(signer);
        token = signedJWT.serialize();
        RSAKey rsaJWK2 = new RSAKeyGenerator(2048)
                .keyID("456")
                .generate();
        JWSSigner signer2 = new RSASSASigner(rsaJWK2);
        SignedJWT signedJWT2 = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(rsaJWK2.getKeyID()).build(),
                claimsSet);
        signedJWT2.sign(signer2);
        token2 = signedJWT2.serialize();
        verifier = new CompositeJwsVerifier.Builder(om,monitor).addKey(rsaPublicJWK.toJSONString()).build();
        service = new JwtAuthenticationService(verifier, false);
    }

    @Test
    public void testValidJwtToken() {
        var headers = Map.of("Authorization", List.of("Bearer " + token));
        assertTrue(service.isAuthenticated(headers), "Could not authenticate using valid token");
    }

    @Test
    public void testValidLowercaseJwtToken() {
        var headers = Map.of("authorization", List.of("Bearer " + token));
        assertTrue(service.isAuthenticated(headers), "Could not authenticate using valid token");
    }

    @Test
    public void testValidOtherJwtToken() {
        var headers = Map.of("Authorization", List.of("Bearer " + token2));
        assertFalse(service.isAuthenticated(headers), "Could not authenticate using valid token");
    }

    @Test
    public void testInvalidJwtToken() {
        var headers = Map.of("Authorization", List.of("Bearer " + token.substring(10, 20)));
        assertFalse(service.isAuthenticated(headers), "Could authenticate using invalid token");
    }

    @Test
    public void testInvalidHeader() {
        var headers = Map.of("Authorization", List.of("bullshit"));
        assertFalse(service.isAuthenticated(headers), "Could authenticate using invalid header");
    }

    @Test
    public void testExpiredJwtToken() {
        JwtAuthenticationService secondService = new JwtAuthenticationService(verifier, true);
        var headers = Map.of("Authorization", List.of("Bearer " + token));
        assertFalse(secondService.isAuthenticated(headers), "Could authenticate using expired token");
    }
}
