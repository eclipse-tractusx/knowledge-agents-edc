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

# Tractus-X Knowledge Agents Data Plane EDC Extensions (KA-EDC-AGENT-PLANE)

KA-EDC-AGENT-PLANE (Agent Plane) is a module of the [Tractus-X Knowledge Agents EDC Extensions](../README.md).

## About this Module

This module hosts data plane extensions to the [Eclipse Dataspace Components (EDC)](https://github.com/eclipse-edc/Connector) and final artifacts based on [Tractus-X EDC](https://github.com/eclipse-tractusx/tractusx-edc) 
in order to enable [Semantic Web](https://www.w3.org/standards/semanticweb/) data exchange, in particular by employing 
the [SPARQL](https://www.w3.org/TR/sparql11-query/) protocol.

The EDC is usually deployed as at least two components, the Control Plane (which does the actual contracting, negotiations and state handling/validation) and several data planes
which perform the actual data transfer. 

The present Agent Plane is a variant of the [Http Data Plane](https://github.com/eclipse-tractusx/tractusx-edc/tree/main/edc-dataplane) 
and can be either employed standalone or as a companion to other protocol planes.

This module consists of

- [Agent Http Procotols Extension](agent-plane-protocol)
- [Ready-Made Agent Plane (Azure Vault)](agentplane-azure-vault)
- [Ready-Made Agent Plane (Hashicorp Vault)](agentplane-hashicorp)

## Getting Started

### Build

To compile and package the binary artifacts (includes running the unit tests)

```shell
mvn package 
```

To publish the binary artifacts (environment variables GITHUB_ACTOR and GITHUB_TOKEN must be set)

```shell
mvn -s ../settings.xml publish
```

### Containerizing

You can either build the docker images using a maven profile

```shell
mvn package -Pwith-docker-image
```

Alternatively, after a successful build, you can invoke docker yourself 

```console
docker build -t ghcr.io/catenax-ng/product-agents-edc/agentplane-azure-vault:1.9.5-SNAPSHOT -f agentplane-azure-vault/src/main/docker/Dockerfile .
```

```console
docker build -t ghcr.io/catenax-ng/product-agents-edc/agentplane-hashicorp:1.9.5-SNAPSHOT -f agentplane-hashicorp/src/main/docker/Dockerfile .
```

