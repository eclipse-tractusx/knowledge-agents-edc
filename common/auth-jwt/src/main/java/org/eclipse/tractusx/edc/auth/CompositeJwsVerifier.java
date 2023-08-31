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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jca.JCAContext;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jose.util.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * A composite verifier that
 * sits on top of a set of keys/verifiers
 * provided by different sources
 */
public class CompositeJwsVerifier implements JWSVerifier {

    final protected Map<JWSAlgorithm, JWSVerifier> verifierMap=new HashMap<>();

    /**
     * create a new verifier
     */
    public CompositeJwsVerifier() {
    }

    /**
     * implement token verification by delegating to another jws verifier depending on the used algorithm
     * @param jwsHeader       The JSON Web Signature (JWS) header. Must
     *                     specify a supported JWS algorithm and must not
     *                     be {@code null}.
     * @param bytes The signing input. Must not be {@code null}.
     * @param base64URL    The signature part of the JWS object. Must not
     *                     be {@code null}.
     *
     * @return flag indicating verification success
     * @throws JOSEException
     */
    @Override
    public boolean verify(JWSHeader jwsHeader, byte[] bytes, Base64URL base64URL) throws JOSEException {
        return verifierMap.get(jwsHeader.getAlgorithm()).verify(jwsHeader,bytes,base64URL);
    }

    /**
     * @return the list of supported/delegated algorithms
     */
    @Override
    public Set<JWSAlgorithm> supportedJWSAlgorithms() {
        return verifierMap.keySet();
    }

    /**
     * @return we obtain the jca context by delegating to the first existing delegation service, null if none is registered
     */
    @Override
    public JCAContext getJCAContext() {
        return verifierMap.entrySet().stream().findFirst().map(e->e.getValue().getJCAContext()).orElse(null);
    }

    /**
     * a builder for composite jws verifiers
     */
    public static class Builder {
        protected CompositeJwsVerifier verifier;
        final protected ObjectMapper om;

        /**
         * create a new builder
         * @param om objectmapper for json parsing
         */
        public Builder(ObjectMapper om) {
            verifier=new CompositeJwsVerifier();
            this.om=om;
        }

        /**
         * add a new subverifier/delegating service
         * @param subVerifier the subverifier instance
         * @return this
         */
        public Builder addVerifier(JWSVerifier subVerifier) {
            subVerifier.supportedJWSAlgorithms().forEach(algo -> verifier.verifierMap.put(algo,subVerifier));
            return this;
        }

        /**
         * adds a key as a json node
         * @param key json representation of keys
         * @return this builder
         */
        public Builder addKey(JsonNode key) {
            if (key.has("keys")) {
                key = key.get("keys");
            }
            if (key.isArray()) {
                var keyIterator = key.elements();
                while (keyIterator.hasNext()) {
                    JsonNode nextKey= keyIterator.next();
                    if(nextKey.has("use") && nextKey.get("use").asText().equals("sig")) {
                        addKey(nextKey);
                    }
                }
                return this;
            }
            if ( key.has("kty")) {
                var kty = key.get("kty");
                switch (kty.asText()) {
                    case "RSA":
                        try {
                            var rsaKey = RSAKey.parse(om.writeValueAsString(key));
                            return addVerifier(new RSASSAVerifier(rsaKey));
                        } catch(JOSEException | JsonProcessingException | ParseException e) {
                        }
                    case "EC":
                        try {
                            var ecKey = ECKey.parse(om.writeValueAsString(key));
                            return addVerifier(new ECDSAVerifier(ecKey));
                        } catch(JOSEException | JsonProcessingException | ParseException e) {
                        }
                    default:
                        break;
                }
            }
            return this;
        }

        /**
         * adds s given key
         * @param key maybe a a json definition for a single key or multiple keys, or a url to download a key
         * @return this instance
         */
        public Builder addKey(String key) {
            if(key!=null) {
                try {
                    URL keyUrl = new URL(key);
                    try (InputStream keyStream = keyUrl.openStream()) {
                        key = IOUtils.readInputStreamToString(keyStream);
                    } catch (IOException e) {
                        key = null;
                    }
                } catch (MalformedURLException e) {
                }
            }
            if(key!=null) {
                try {
                    return addKey(om.readTree(key));
                } catch (JsonProcessingException e) {
                }
            }
            return this;
        }

        /**
         * builds the composite verfifier from the builder state
         * @return new composite verifier state
         */
        public CompositeJwsVerifier build() {
            return verifier;
        }
    }
}
