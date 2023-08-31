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

import org.eclipse.edc.api.auth.spi.AuthenticationRequestFilter;
import org.eclipse.edc.api.auth.spi.AuthenticationService;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provides;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.web.spi.WebService;

/**
 * Service extension that introduces the configurable composite authentication service
 * including JWT authentication as well as its registration via authentication filters
 */
@Provides(AuthenticationService.class)
@Extension(value = "Extended authentication")
public class AuthenticationExtension implements ServiceExtension {

    @Setting(
            value = "Defines a set/list of authentication services."
    )
    public static String AUTH_SETTING="tractusx.auth";

    @Setting(
            value = "Whether the auth service should be registered.",
            defaultValue = "false",
            type="boolean"
    )
    public static String REGISTER_SETTING="register";

    @Setting(
            value = "The type of authentication service to use. Maybe jwt or composite"
    )
    public static String TYPE_SETTING="type";

    @Setting(
            value = "On which paths should the corresponding filter be installed."
    )
    public static String PATH_SETTING="paths";

    @Setting(
            value = "The BASE64 encoded public key or a url where to obtain it."
    )
    public static String KEY_SETTING="publickey";

    @Setting(
            value = "URL indicating where to get the public key for verifying the token.",
            defaultValue = "true",
            type="boolean"
    )
    public static String EXPIRE_SETTING="checkexpiry";

    @Setting(
            value = "embedded authentication services."
    )
    public static String SERVICE_SETTING="service";

    @Setting(
            value = "api key in vault."
    )
    public static String VAULT_SETTING="vault-key";

    @Setting(
            value = "api key hashcode."
    )
    public static String API_CODE_SETTING="api-code";

    @Setting(
            value = "composite mode."
    )
    public static String MODE_SETTING="mode";

    /**
     * dependency injection part
     */
    @Inject
    protected WebService webService;
    @Inject
    protected TypeManager typeManager;
    @Inject
    protected Vault vault;


    @Override
    public void initialize(ServiceExtensionContext ctx) {
        ctx.getConfig(AUTH_SETTING).partition().forEach( authenticationServiceConfig ->
                createAuthenticationService(ctx,authenticationServiceConfig));
    }

    public AuthenticationService createAuthenticationService(ServiceExtensionContext ctx, Config authenticationServiceConfig) {
        String type=authenticationServiceConfig.getString(TYPE_SETTING);
        AuthenticationService newService=null;
        if("jwt".equals(type)) {
            CompositeJwsVerifier.Builder jwsVerifierBuilder = new CompositeJwsVerifier.Builder(typeManager.getMapper());
            String key = authenticationServiceConfig.getString(KEY_SETTING);
            if (key != null) {
                jwsVerifierBuilder.addKey(key);
            }
            newService = new JwtAuthenticationService.Builder().
                    setVerifier(jwsVerifierBuilder.build()).
                    setCheckExpiry(authenticationServiceConfig.getBoolean(EXPIRE_SETTING, true)).
                    build();
        } else if("api-key".equals(type)) {
            int reference;
            if(authenticationServiceConfig.hasKey(VAULT_SETTING)) {
                reference=vault.resolveSecret(authenticationServiceConfig.getString(VAULT_SETTING)).hashCode();
            } else {
                reference=authenticationServiceConfig.getInteger(API_CODE_SETTING);
            }
            newService = new ApiKeyAuthenticationService.Builder().setReference(reference).build();
        } else if("composite".equals(type)) {
            CompositeAuthenticationService.Builder builder=new CompositeAuthenticationService.Builder();
            builder.setMode(Enum.valueOf(CompositeAuthenticationMode.class,authenticationServiceConfig.getString(MODE_SETTING,CompositeAuthenticationMode.ALL.name())));
            authenticationServiceConfig.getConfig(SERVICE_SETTING).partition().forEach( subServiceConfig ->
                    builder.addService(createAuthenticationService(ctx, subServiceConfig))
            );
            newService=builder.build();
        }
        if(newService!=null) {
            String[] paths = authenticationServiceConfig.getString(PATH_SETTING, "").split(",");
            for (String path : paths) {
                webService.registerResource(path, new AuthenticationRequestFilter(newService));
            }
            if (authenticationServiceConfig.getBoolean(REGISTER_SETTING, false)) {
                ctx.registerService(AuthenticationService.class, newService);
            }
        }
        return newService;
    }

}
