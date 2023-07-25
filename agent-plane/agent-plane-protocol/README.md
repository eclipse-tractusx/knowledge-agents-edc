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
# Catena-X Knowledge Agents (Hey Catena!) EDC Agent Protocols

This folder hosts the [Agent Data Protocols for the Eclipse Dataspace Connector (EDC)](https://projects.eclipse.org/projects/technology.dataspaceconnector).

## Architecture

This extension
- introduces a direct endpoint for submitting (federated) queries in the supported inference languages (currently: SparQL)
- may negotiate further agreements for delegating sub-queries on the fly
- implements special Sinks and Sources for dealing with specific endpoint protocols, such as SparQL-Over-Http.
- hosts a synchronisation schedule which regulary requests the catalogue from configured partner connectors and includes them into the default graph

The SparQL implementation currently relies on Apache Jena Fuseki as the SparQL engine.

### Security

There are three types of exposed interfaces:
* Interaction with the tenant (API) is usually shielded with a data plane api key (but may include oauth2 against a consumer-specific SSO)
* Interaction with the control plane (Management, Callback) uses the data plane api key
* Interaction with other data planes (Transfer) uses the "ordinary" synchronous transfer with the DAPS token mechanism

There are three types of called interfaces 
* Interaction with the control plane uses the control plane api key
* Interaction with the persistent storage layer of the embedded SparQL engine uses filesystem mounting and permissions
* Interaction with backend agents uses their individual security settings (typically given in the private data address of the assets, in addition to the DAPS token attachements)

## Building

You could invoke the following command to compile and test the EDC Agent extensions

```console
mvn -s ../../../settings.xml install
```

## Deployment & Usage

### Step 1: Dependency

Add the following dependency to your control-plane artifact pom:

```xml
        <dependency>
            <groupId>io.catenax.knowledge.dataspace.edc.control-plane</groupId>
            <artifactId>agent-plane-protocol</artifactId>
            <version>1.9.5-SNAPSHOT</version>
        </dependency>
```

and the following repo to your repositories section

```xml
    <repository>
      <id>github</id>
      <name>Catena-X Maven Repository on Github</name>
      <url>https://maven.pkg.github.com/catenax-ng/product-knowledge</url>
    </repository> 
```

### Step 1 (Alternative): Adding Jar

see the upcoming io.catenax.knowledge.dataspace.edc.agent-plane.agent-plane-protocol-complete module

### Step 2: Configuration  

The following is a list of configuration objects and properties that you might set in the corresponding mounted config files

| CONFIG FILE                   | SETTING                                         | Required | Default/Example                                                | Description                                                                                                                             | List |
|-------------------------------|-------------------------------------------------|----------|----------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------|------|
| /app/configuration.properties | cx.agent.asset.default                          |          | urn:x-arq:DefaultGraph                                         | Name of the default (local) graph (federated data catalogue)                                                                            |      | 
| /app/configuration.properties | cx.agent.asset.file                             |          | https://www.w3id.org/catenax/ontology,dataspace.ttl            | A comma-separated list of initial knowledge/triples  for the default graph                                                              |      | 
| /app/configuration.properties | cx.agent.accesspoint.name                       |          | api                                                            | Internal name in Fuseki for the agent endpoint                                                                                          |      | 
| /app/configuration.properties | cx.agent.controlplane.protocol                  | (X)      | http://oem-control-plane:8182                                  | Protocol Endpoint of the providing control plane (for easy access to local graphs/skills)                                               |      | 
| /app/configuration.properties | cx.agent.controlplane.management                | X        | http://oem-control-plane:8181/management/v2                    | Data Management Endpoint of the consuming control plane                                                                                 |      | 
| /app/configuration.properties | edc.participant.id                              | (X)      | BPNL00000DUMMY                                                 | business partner number under which the consuming control plane operates                                                                |      | 
| /app/configuration.properties | edc.api.auth.code                               | (X)      | X-Api-Key                                                      | Authentication Header for consuming control plane                                                                                       |      | 
| /app/configuration.properties | edc.api.auth.key                                | (X)      | ****                                                           | Authentication Secret for consuming control plane                                                                                       |      | 
| /app/configuration.properties | edc.dataplane.token.validation.endpoint         | X        | http://localhost:8082/api/validation/                          | Token validation endpoint for consuming/providing plane (if single control plane) or the address of the integrated switching validator) |      | 
| /app/configuration.properties | edc.dataplane.token.validation.endpoints.<name> | (X)      | http://oem-control-plane:9999/control/token                    | Additional token validation endpoints to switch between (if multiple control planes)                                                    |      | 
| /app/configuration.properties | web.http.callback.port                          | X        | 8187                                                           | Callback endpoint port                                                                                                                  |      | 
| /app/configuration.properties | web.http.callback.path                          | X        | /callback                                                      | Callback endpoint path prefix                                                                                                           |      | 
| /app/configuration.properties | cx.agent.callback                               | X        | http://oem-data-plane:8187/callback/endpoint-data-reference    | Callback endpoint full address for control plane feedback (see above)                                                                   |      | 
| /app/configuration.properties | cx.agent.dataspace.synchronization              |          | -1/60000                                                       | If positive, number of seconds between each catalogue synchronization attempt                                                           |      | 
| /app/configuration.properties | cx.agent.dataspace.remotes                      |          | http://consumer-edc-control:8282,http://tiera-edc-control:8282 | Comma-separated list of Business Partner Control Plane Urls (which host the IDS catalogue endpoint)                                     |      | 
| /app/configuration.properties | cx.agent.sparql.verbose                         |          | false                                                          | Controls the verbosity of the SparQL Engine)                                                                                            |      | 
| /app/configuration.properties | cx.agent.threadpool.size                        |          | 4                                                              | Number of threads for batch/synchronisation processing                                                                                  |      | 
| /app/configuration.properties | cx.agent.federation.batch.max                   |          | 9223372036854775807                                            | Maximal number of tuples to send in one query                                                                                           |      | 
| /app/configuration.properties | cx.agent.negotiation.poll                       |          |                                                                | Number of milliseconds between negotiation status checks                                                                                |      | 
| /app/configuration.properties | cx.agent.negotiation.timeout                    |          |                                                                | Number of milliseconds after which a pending negotiation is regarded as stale                                                           |      | 
| /app/configuration.properties | cx.agent.connect.timeout                        |          |                                                                | Number of milliseconds after which a connection attempt is regarded as stale                                                            |      | 
| /app/configuration.properties | cx.agent.read.timeout                           |          | 1080000                                                        | Number of milliseconds after which a reading attempt is regarded as stale                                                               |      | 
| /app/configuration.properties | cx.agent.call.timeout                           |          |                                                                | Number of milliseconds after which a complete call is regarded as stale                                                                 |      | 
| /app/configuration.properties | cx.agent.write.timeout                          |          |                                                                | Number of milliseconds after which a write attempt is regarded as stale                                                                 |      | 

