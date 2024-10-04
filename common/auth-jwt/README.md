<!--
 * Copyright (c) 2022,2024 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
-->

# Tractus-X Knowledge Agents (Not Only) JWT-Based Authentication Stack EDC Extension (KA-EDC-JWT-AUTH)

This folder hosts an authentication extension to the [Eclipse Dataspace Connector (EDC)](https://projects.eclipse.org/projects/technology.dataspaceconnector).

It allows to configure and build [Authentication Services](https://github.com/eclipse-edc/Connector/blob/main/spi/common/auth-spi/src/main/java/org/eclipse/edc/api/auth/spi/AuthenticationService.java), such as the validation of 
- [API Keys](src/main/java/org/eclipse/tractusx/edc/auth/ApiKeyAuthenticationService.java)
- [JWT Tokens](src/main/java/org/eclipse/tractusx/edc/auth/JwtAuthenticationService.java) or 
- [Combinations of Authentication Services](src/main/java/org/eclipse/tractusx/edc/auth/CompositeAuthenticationService.java)

It allows to install authentication filters that are backed by those authentication services into various web service contexts 
(in addition to or in place of other authentication mechanisms).

## How to enable this extension

Add the following dependency to your EDC artifact pom:

```xml
        <dependency>
            <groupId>org.eclipse.tractusx.agents.edc</groupId>
            <artifactId>auth-jwt</artifactId>
            <version>1.14.23-SNAPSHOT</version>
        </dependency>
```

and the following repo to your repositories section

```xml
    <repository>
      <id>github</id>
      <name>Tractus-X KA-EDC Maven Repository on Github</name>
      <url>https://maven.pkg.github.com/eclipse-tractusx/knowledge-agents-edc</url>
    </repository> 
```

## How to configure this extension

The following is a list of configuration properties (or environment variables) that you might set. The environment variables key is obtained by upper-casing the property name and replacing dots with underscores, e.g. 'cx.agent.asset.file' becomes 'CX_AGENT_ASSET_FILE'. When the property is marked as 'X' in the 'Required' column, the extension would not work when it is not set. When the property is marked as '(X)' it means that the extension would work, but with restrictions. When the property is marked as 'L' in the 'List' column, it accepts a comma-separated list of values. When the property is marked as '*' in the 'List' column, then this indicates that you may have multiple instances of the property (by replacing the <id> in the property name by a unique id).

| SETTING                                     | Required                  | Default/Example                                                           | Description                                                                                                                            | 
|---------------------------------------------|---------------------------|---------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------|
| tractusx.auth.<name>.type                   | no                        | jwt                                                                       | Introduces a new authentication filter ('jwt', 'api-key' or 'composite')                                                               |   
| tractusx.auth.<name>.register               | no                        | true                                                                      | Whether the filter should be registered in the EDC list                                                                                |   
| tractusx.auth.<name>.paths                  | no                        | default                                                                   | A list of web service paths which should be secured using that service                                                                 |   
| tractusx.auth.<name>.exclude                | no                        | .*(/check/).*                                                             | A regular expression excluding particular paths from authentication                                                                    |                                                                           | A list of web service paths which should be secured using that service                                                                                     |   
| tractusx.auth.<name>.publickey              | yes, if type = 'jwt'      | https://keycloak.instance/auth/realms/REALM/protocol/openid-connect/certs | download url  for public cert of REALM                                                                                                 |   
| tractusx.auth.<name>.checkexpiry            | no, if type = 'jwt'       | true                                                                      | Whether tokens should be checked for expiry                                                                                            |   
| tractusx.auth.<name>.apicode                | no, if type = 'api-key'   | 69609650                                                                  | Hashcode for the api key (here :'Hello') - alternatively use vault-key                                                                 |   
| tractusx.auth.<name>.vaultkey               | no, if type = 'api-key'   | edc-api-key                                                               | Key for the api-key in the configured vault - alternatively use api-code                                                               |   
| tractusx.auth.<name>.mode                   | no, if type = 'composite' | ALL                                                                       | Determines the mode of composition, 'ALL' means that all subservices need to be successful, 'ONE' means that one of the subservices needs to be successful |   
| tractusx.auth.<name>.service.<subname>.type | no, if type = 'composite' | api-key                                                                   | Adds a sub-service to a composite authentication service                                                                               |   

