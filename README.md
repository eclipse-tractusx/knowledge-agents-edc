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

# Tractus-X Knowledge Agents EDC Extensions (KA-EDC)

![GitHub contributors](https://img.shields.io/github/contributors/eclipse-tractusx/knowledge-agents-edc)
![GitHub Org's stars](https://img.shields.io/github/stars/catenax-ng)
![GitHub](https://img.shields.io/github/license/eclipse-tractusx/knowledge-agents-edc)
![GitHub all releases](https://img.shields.io/github/downloads/eclipse-tractusx/knowledge-agents-edc/total)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=eclipse-tractusx_knowledge-agents-edc&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=eclipse-tractusx_knowledge-agents-edc)

KA-EDC is a product of the [Catena-X Knowledge Agents Kit](https://catenax-ng.github.io/product-knowledge) implementing the core "dataspace" modules of the CX-0084 standard (Federated Queries in Dataspaces).

* See the [user documentation](docs/README.md)
* See the [copyright notice](COPYRIGHT.md)
* See the [authors file](AUTHORS.md)
* See the [license file](LICENSE.md)
* See the [code of conduct](CODE_OF_CONDUCT.md)
* See the [contribution guidelines](CONTRIBUTING.md)
* See the [dependencies](DEPENDENCIES.md)

## About the Project 

This repository hosts reference extensions to the [Eclipse Dataspace Components (EDC)](https://github.com/eclipse-edc/Connector).
It provides container images and deployments for a ready-made KA-enabled [Tractus-X EDC](https://github.com/eclipse-tractusx/tractusx-edc).

In particular, KA-EDC consists of

- [Common](common) extensions in order to allow for secure and personalized application access to the EDC infrastructure.
- [Agent (Data) Plane](agent-plane) extensions to ingest, validate, process and delegate federated procedure calls (so-called Skills) on top of data and functional assets. In particular, they implement the [Semantic Web](https://www.w3.org/standards/semanticweb/) [SPARQL](https://www.w3.org/TR/sparql11-query/) protocol. 

Included in this repository are ready-made [Helm charts](charts). 
They can be installed from the [Catena-X Knowledge Agents Kit Helm Repository](https://docs.catenax-ng.github.io/product-knowledge/infrastructure).

## Source Code Layout & Runtime Collaboration

![Source Code](docs/KA-EDC.drawio.svg)

Above is a collaboration map of the main implementation classes found in this repository.

It starts with an application performing a [SPARQL](https://www.w3.org/TR/sparql11-query/) call against the Consumer's [AgentController](agent-plane/agent-plane-protocol/src/main/java/org/eclipse/tractusx/agents/edc/http/AgentController.java) of the [Agent Protocol Data Plane Extension](agent-plane/agent-plane-protocol/README.md). This call may be handled by a [AuthenticationService](https://github.com/eclipse-edc/Connector/blob/main/spi/common/auth-spi/src/main/java/org/eclipse/edc/api/auth/spi/AuthenticationService.java). Using the configuration facilities of the [JWT Auth Extension](common/auth-jwt/README.md) which sets up single [JwtAuthenticationService](common/auth-jwt/src/main/java/org/eclipse/tractusx/edc/auth/JwtAuthenticationService.java) or composed [CompositeAuthenticationService](common/auth-jwt/src/main/java/org/eclipse/tractusx/edc/auth/CompositeAuthenticationService.java) the handler stack may analyses diverse authorisation features of the incoming request, such as checking a JWT-based bearer token for validity against multiple OpenId servers by [CompositeJwsVerifier](common/auth-jwt/src/main/java/org/eclipse/tractusx/edc/auth/CompositeJwsVerifier.java).

The [AgentController](agent-plane/agent-plane-protocol/src/main/java/org/eclipse/tractusx/agents/edc/http/AgentController.java) delegates the call upon preprocessing (e.g. by resolving local Skill Asset references using the [EdcSkillStore](agent-plane/agent-plane-protocol/src/main/java/org/eclipse/tractusx/agents/edc/service/EdcSkillStore.java)) to the actual [SparqlQueryProcessor](agent-plane/agent-plane-protocol/src/main/java/org/eclipse/tractusx/agents/edc/sparql/SparqlQueryProcessor.java) (an instance of an [Apache Jena Sparql Query Processor](https://github.com/apache/jena/blob/main/jena-fuseki2/jena-fuseki-core/src/main/java/org/apache/jena/fuseki/servlets/SPARQLQueryProcessor.java)). The [SparqlQueryProcessor](agent-plane/agent-plane-protocol/src/main/java/org/eclipse/tractusx/agents/edc/sparql/SparqlQueryProcessor.java) is backed by an [RDFStore](gent-plane/agent-plane-protocol/src/main/java/org/eclipse/tractusx/agents/edc/rdf/RDFStore.java) which hosts the Federated Data Catalogue (and that is regularly synchronized by the [DataspaceSynchronizer](gent-plane/agent-plane-protocol/src/main/java/org/eclipse/tractusx/agents/edc/service/DataspaceSynchronizer.java)).

Whenever external SERVICE references in a SPARQL query are to be executed, the [SparqlQueryProcessor](agent-plane/agent-plane-protocol/src/main/java/org/eclipse/tractusx/agents/edc/sparql/SparqlQueryProcessor.java) will ask the [DataspaceServiceExecutor](agent-plane/agent-plane-protocol/src/main/java/org/eclipse/tractusx/agents/edc/sparql/DataspaceServiceExecutor.java) to execute the actual sub-operation. This operation could - depending on the actual query binding context - either point to multiple tenant-internal or public endpoints. The operation could also need to be batched in case that there are too many bindings to transfer in one go (see the maxBatchSize Parameter in the [Agent Protocol Data Plane Extension](agent-plane/agent-plane-protocol/README.md)). The operation could also hint to dataspace addresses (as indicated through URLs starting with the edc:// or edcs:// schemes). In this latter case, [DataspaceServiceExecutor](agent-plane/agent-plane-protocol/src/main/java/org/eclipse/tractusx/agents/edc/sparql/DataspaceServiceExecutor.java) will ask the [AgreementController](agent-plane/agent-plane-protocol/src/main/java/org/eclipse/tractusx/agents/edc/AgreementController.java) for help.

[AgreementController](agent-plane/agent-plane-protocol/src/main/java/org/eclipse/tractusx/agents/edc/AgreementController.java) keeps book about already negotiated Dataspace Assets and corresponding [EndpointDataReferences](https://github.com/eclipse-edc/Connector/blob/main/spi/common/core-spi/src/main/java/org/eclipse/edc/spi/types/domain/edr/EndpointDataReference.java). If such an EDR does not yet exist, it will negotiate one using the EDC control plane with the help of the [DataManagement](agent-plane/agent-plane-protocol/src/main/java/org/eclipse/tractusx/agents/edc/service/DataManagement.java) facade. The resulting EDR will be asynchronously handed out to the [AgreementController](agent-plane/agent-plane-protocol/src/main/java/org/eclipse/tractusx/agents/edc/AgreementController.java) and finally returned to [DataspaceServiceExecutor](agent-plane/agent-plane-protocol/src/main/java/org/eclipse/tractusx/agents/edc/sparql/DataspaceServiceExecutor.java) to perform the Dataspace Call (effectively tunneling the SPARQL protocol through EDC's HttpProxy transfer).

When the call arrives at the Provider's Data Plane, it will hit the [AgentSource](agent-plane/agent-plane-protocol/src/main/java/org/eclipse/tractusx/agents/edc/http/transfer/AgentSource.java). Mirroring the Consumer's [AgentController](agent-plane/agent-plane-protocol/src/main/java/org/eclipse/tractusx/agents/edc/http/AgentController.java), [AgentSource](agent-plane/agent-plane-protocol/src/main/java/org/eclipse/tractusx/agents/edc/http/transfer/AgentSource.java) performs some preprocessing and validity checking before finally delegating to the Provider's [SparqlQueryProcessor](agent-plane/agent-plane-protocol/src/main/java/org/eclipse/tractusx/agents/edc/sparql/SparqlQueryProcessor.java) (from where the recursion may go further ...)

## Getting Started

### Build

To compile, package and containerize the binary artifacts (includes running the unit tests)

```shell
mvn package -Pwith-docker-image
```

To publish the binary artifacts (environment variables GITHUB_ACTOR and GITHUB_TOKEN must be set)

```shell
mvn -s settings.xml publish
```

### Deployment

Deployment can be done
* via [JAR libraries](https://github.com/orgs/eclipse-tractusx/packages?repo_name=knowledge-agents-edc&ecosystem=maven) copied into your Java runtime
* via [Docker images](https://github.com/orgs/eclipse-tractusx/packages?repo_name=knowledge-agents-edc&ecosystem=docker) 
* vial [Helm Charts](https://catenax-ng.github.io/product-knowledge/infrastructure/index.yaml)

See the [user documentation](docs/README.md)


