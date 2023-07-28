<!--
 * Copyright (c) 2022,2023 Contributors to the Eclipse Foundation
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

# Notices for Tractus-X Knowledge Agents EDC Extensions (KA-EDC)

This content is produced and maintained by the Eclipse Tractus-X project.

 * Project home: https://projects.eclipse.org/projects/automotive.tractusx

## Trademarks

Eclipse Tractus-X are trademarks of the Eclipse Foundation. Eclipse, and the Eclipse Logo are
registered trademarks of the Eclipse Foundation.

## Copyright

All content is the property of the respective authors or their employers.
For more information regarding authorship of content, please consult the
listed source code repository logs.

## Declared Project Licenses

This program and the accompanying materials are made available under the terms
of the Apache License 2.0 which is available at
https://www.apache.org/licenses/LICENSE-2.0.txt

SPDX-License-Identifier: Apache-2.0

## Source Code

The project maintains the following source code repositories:

 * https://github.com/eclipse-tractusx/knowledge-agents-edc.git

## Third-party Content

We provide an [Eclipse Dash License File](DEPENDENCIES) for each release.

You may recreate an up-to-date DEPENDENCIES file by invoking

```shell
./mvnw org.eclipse.dash:license-tool-plugin:license-check -Ddash.summary=DEPENDENCIES
```

We provide Software-Bill-Of-Material (SBOM) documents for each KA-EDC module for each release:
* [EDC Common JWT Auth](common/auth-jwt/auth-jwt-1.9.5-SNAPSHOT-sbom.json)
* [EDC Data Plane Agent Protocols](agent-plane/agent-plane-protocol/agent-plane-protocol-1.9.5-SNAPSHOT-sbom.json)
* [EDC Agent Plane (Hashicorp Vault)](agent-plane/agent-plane-hashicorp/agent-plane-hashicorp-1.9.5-SNAPSHOT-sbom.json)
* [EDC Agent Plane (Azure Vault)](agent-plane/agent-plane-azure-vault/agent-plane-azure-vault-1.9.5-SNAPSHOT-sbom.json)

You may recreate up-to-date SBOMs by invoking

```shell
./mvnw package -DskipTests
```
Afterwards, you find the current documents under:
* [EDC Common JWT Auth](common/auth-jwt/target/auth-jwt-1.9.5-SNAPSHOT-sbom.json)
* [EDC Data Plane Agent Protocols](agent-plane/agent-plane-protocol/target/agent-plane-protocol-1.9.5-SNAPSHOT-sbom.json)
* [EDC Agent Plane (Hashicorp Vault)](agent-plane/agent-plane-hashicorp/target/agent-plane-hashicorp-1.9.5-SNAPSHOT-sbom.json)
* [EDC Agent Plane (Azure Vault)](agent-plane/agent-plane-azure-vault/target/agent-plane-azure-vault-1.9.5-SNAPSHOT-sbom.json)

The KA-EDC build and runtime platform is relying on:
* [Java Runtime Environment (JRE >=11 - license depends on chosen provider)](https://de.wikipedia.org/wiki/Java-Laufzeitumgebung)
* [Java Development Kit (JDK >=11 - license depends on chosen provider)](https://de.wikipedia.org/wiki/Java_Development_Kit) 
* [Apache Maven >=3.8 (Apache License 2.0)](https://maven.apache.org) 
* [Eclipse Dash (Eclipse Public License 2.0)](https://github.com/eclipse/dash-licenses)
* [CycloneDX 1.4 (Apache License 2.0)](https://github.com/CycloneDX)
* [Docker Engine >= 20.10.17 (Apache License 2.0)]() 
* [Helm (Apache License 2.0)](https://helm.sh/) 

## Cryptography

Content may contain encryption software. The country in which you are currently
may have restrictions on the import, possession, and use, and/or re-export to
another country, of encryption software. BEFORE using any encryption software,
please check the country's laws, regulations and policies concerning the import,
possession, or use, and re-export of encryption software, to see if this is
permitted.
