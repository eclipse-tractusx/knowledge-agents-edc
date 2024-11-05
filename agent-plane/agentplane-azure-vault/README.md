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
# Tractus-X Knowledge Agent Plane Azure Vault (KA-EDC-AGENT-AZR)

This folder hosts the [Default Agent (Data) Plane with Azure Vault access for the Eclipse Dataspace Connector (EDC)](https://projects.eclipse.org/projects/technology.dataspaceconnector).

The module sets up/depends on
* an EDC Data Plane with a secret store tailored to the Azure Vault
* supports HttpData and AmazonS3 sources (such as used by AAS submodels)
* supports possibly multiple Http sub-protocols, currently
  * urn:cx:Protocol:w3c:Http#SPARQL for Graph-Based sources by means of the Apache Jena Fuseki engine
  * urn:cx:Protocol:w3c:Http#SKILL for downloading or delegating pre-defined queries
* allows to build up a graph-based federated data catalogue
* includes the JWT-AUTH extension which may shield any endpoint with additional layers of authentication, such as against Oauth2 IDPs

For the configuration options, please see the [Agent Plane Extension](../agent-plane-protocol/README.md#step-2-configuration)

For a sample configuration including Azure Vault, see [this sample properties file](resources/dataplane.properties)

## Building

You could invoke the following command to compile and test the Agent Plane

```console
mvn -s ../../../settings.xml install
```

## Deployment & Usage

### Containerizing 

You could invoke the following command to build the Agent Plane

```console
mvn -s ../../../settings.xml install -Pwith-docker-image
```

Alternatively, after a sucessful [build](#building) the docker image of the Agent Plane is created using

```console
docker build -t tractusx//agentplane-azure-vault:1.14.24-SNAPSHOT -f src/main/docker/Dockerfile .
```

To run the docker image, you could invoke this command

```console
docker run -p 8082:8082 \
  -v $(pwd)/resources/agent.ttl:/app/agent.ttl \
  -v $(pwd)/resources/dataspace.ttl:/app/dataspace.ttl \
  -v $(pwd)/resources/dataplane.properties:/app/configuration.properties \
  -v $(pwd)/resources/opentelemetry.properties:/app/opentelemetry.properties \
  -v $(pwd)/resources/logging.properties:/app/logging.properties \
  tractusx/agentplane-azure-vault:latest
````

Afterwards, you should be able to access the [local SparQL endpoint](http://localhost:8082/api/agent) via
the browser or by directly invoking a query

```console
curl --request GET 'http://localhost/api/agent?asset=urn:graph:cx:Dataspace&query=SELECT ?senseOfLife WHERE { VALUES (?senseOfLife) { ("42"^^xsd:int) } }' \
--header 'X-Api-Key: foo'
```

### Notice for Docker Image

DockerHub: https://hub.docker.com/r/tractusx/agentplane-azure-vault

Eclipse Tractus-X product(s) installed within the image:
GitHub: https://github.com/eclipse-tractusx/knowledge-agents-edc/tree/main/agent-plane/agentplane-azure-vault
Project home: https://projects.eclipse.org/projects/automotive.tractusx
Dockerfile: https://github.com/eclipse-tractusx/knowledge-agents-edc/blob/main/agent-plane/agentplane-azure-vault/src/main/docker/Dockerfile
Project license: Apache License, Version 2.0


**Used base image**

- [eclipse-temurin:22-jre-alpine](https://github.com/adoptium/containers)
- Official Eclipse Temurin DockerHub page: https://hub.docker.com/_/eclipse-temurin
- Eclipse Temurin Project: https://projects.eclipse.org/projects/adoptium.temurin
- Additional information about the Eclipse Temurin images: https://github.com/docker-library/repo-info/tree/master/repos/eclipse-temurin

As with all Docker images, these likely also contain other software which may be under other licenses (such as Bash, etc from the base distribution, along with any direct or indirect dependencies of the primary software being contained).

As for any pre-built image usage, it is the image user's responsibility to ensure that any use of this image complies with any relevant licenses for all software contained within.