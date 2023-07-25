<!--
 * Copyright (C) 2022-2023 Catena-X Association and others. 
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Apache License 2.0 which is available at
 * http://www.apache.org/licenses/.
 * 
 * SPDX-FileType: DOCUMENTATION
 * SPDX-FileCopyrightText: 2022-2023 Catena-X Association
 * SPDX-License-Identifier: Apache-2.0
-->

# Dependencies of Tractus-X Knowledge Agents EDC Extensions (KA-EDC)

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