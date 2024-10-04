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
# Tractus-X Knowledge Agents EDC Protocols Extension (KA-EDC-AGENT-PROT)

This folder hosts the [Agent Data Protocols Extension for the Eclipse Dataspace Connector (EDC)](https://projects.eclipse.org/projects/technology.dataspaceconnector).

## Architecture

This extension
- introduces or interfaces to a tenant-internal endpoint (Matchmaking Agent) for submitting possibly federated queries (called Skills) in the supported inference languages (currently: SparQL)
- hosts a synchronisation schedule which regulary requests the catalogue from configured partner connectors and includes them into the default graph
- may negotiate further agreements for delegating sub-queries on the fly, for which purpose it also operates as an EDR callback for the control plane 
- may operate as a validation proxy in case that this data plane is attached to several control planes (e.g. a consuming and a providing control plane)
- implements special Sources for dealing with http-based transfer protocols, such as SparQL-Over-Http and Skill-Over-Http

The SparQL implementation currently relies on Apache Jena Fuseki as the SparQL engine.

see the [Overall Source Code Layout](../../README.md#source-code-layout--runtime-collaboration)

### Security

There are three types of incoming interfaces:
* Interaction with the tenant (the internal default API endpoint) is usually shielded with a token-based or api key based authentication mechanism.
* Interaction with the control plane (the internal management and callback endpoints) typically uses api keys
* Interaction with other data planes (the public transfer endpoint) uses the "ordinary" Dataspace token-based authentication mechamism

There are three types of called interfaces 
* Interaction with the control plane (internal) uses the control plane api key
* Interaction with the persistent storage layer (internal) of the embedded SparQL engine uses filesystem mounting and permissions
* Interaction with backend agents (internal) uses their individual security settings (typically given in the private data address of the assets)
* Interaction with other data planes (the public transfer endpoint) uses the "ordinary" Dataspace token-based authentication mechamism

## Building

You could invoke the following command to compile and test the EDC Agent extensions

```console
mvn -s ../../../settings.xml install
```

## Deployment & Usage

### Step 1: Dependency

Add the following dependency to your data-plane artifact pom:

```xml
        <dependency>
            <groupId>org.eclipse.tractusx.agents.edc</groupId>
            <artifactId>agent-plane-protocol</artifactId>
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

These examples are for maven-based projects, the gradle build settings are analoguous.

### Step 2: Configuration  

The following is a list of configuration properties (or environment variables) that you might set. The environment variables key is obtained by upper-casing the property name and replacing dots with underscores, e.g. 'cx.agent.asset.file' becomes 'CX_AGENT_ASSET_FILE'. When the property is marked as 'X' in the 'Required' column, the extension would not work when it is not set. When the property is marked as '(X)' it means that the extension would work, but with restrictions. When the property is marked as 'L' in the 'List' column, it accepts a comma-separated list of values. When the property is marked as '*' in the 'List' column, then this indicates that you may have multiple instances of the property (by replacing the <id> in the property name by a unique id).

For a list of environment variables to configure the behaviour of the data plane, we refer to [the Tractus-X EDC documentation](https://github.com/eclipse-tractusx/tractusx-edc).

See [this sample configuration file](resources/dataplane.properties)

| Property                                  | Required | Default/Example                                                                | Description                                                                                                                                                   | List |
|-------------------------------------------|----------|--------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------|------|
| cx.agent.matchmaking                      |          | http://matchmaking-agent.internal                                                         | URL of the matchmaking agent (use internal one if null)                                                                                                       |      | 
| cx.agent.asset.default                    |          | urn:x-arq:DefaultGraph                                                         | IRI of the default graph (federated data catalogue)                                                                                                           |      | 
| cx.agent.asset.file                       |          | https://www.w3id.org/catenax/ontology,dataspace.ttl                            | Initial triples for the default graph (federated data catalogue)                                                                                              | L    | 
| cx.agent.accesspoint.name                 |          | api                                                                            | Matchmaking agent endpoint name (internal)                                                                                                                    |      | 
| cx.agent.controlplane.protocol            | (X)      | http://oem-control-plane:8182                                                  | Protocol Endpoint of the providing control plane (needed if you want to access local graphs/skills without absolute address)                                  |      | 
| cx.agent.controlplane.management          | X        | http://oem-control-plane2:8181/management                                      | Data Management Endpoint of the consuming control plane                                                                                                       |      | 
| cx.agent.controlplane.management.provider | (X)      | http://oem-control-plane:8181/management                                       | Data Management Endpoint of the providing control plane (only if different from the consuming control plane)                                                  |      | 
| edc.participant.id                        | X        | BPNL00000DUMMY                                                                 | business partner number under which the consuming control plane operates                                                                                      |      | 
| edc.api.auth.code                         | (X)      | X-Api-Key                                                                      | Authentication Header for consuming control plane  (if any)                                                                                                   |      | 
| edc.api.auth.key                          | (X)      | ****                                                                           | Authentication Secret for consuming control plane (if any)                                                                                                    |      | 
| web.http.callback.port                    | X        | 8187                                                                           | Callback endpoint port                                                                                                                                        |      | 
| web.http.callback.path                    | X        | /callback                                                                      | Callback endpoint path prefix                                                                                                                                 |      | 
| cx.agent.callback                         | X        | http://oem-data-plane:8187/callback/transfer-process-started                    | Callback endpoint full address as seen from the consuming control plane                                                                                       |      | 
| cx.agent.skill.contract                   |          | cx.agent.skill.contract.default=Contract?partner=Skill                         | Id/IRI of the default contract put in the cx-common:publishedUnderContract property for new skills                                                            |      |
| cx.agent.dataspace.synchronization        |          | -1 / 60000                                                                     | If positive, number of seconds between each catalogue synchronization attempt                                                                                 |      | 
| cx.agent.service.allow                    |          | (http&#124;edc)s?://.*                                                         | Regular expression for determining which IRIs are allowed in SERVICE calls (on top level/federated data catalogue)                                            |      | 
| cx.agent.service.deny                     |          | ^$                                                                             | Regular expression for determining which IRIs are denied in SERVICE calls (on top level/federated data catalogue)                                             |      |                                                                                                                                                                       | 
| cx.agent.service.asset.allow              |          | (http&#124;edc)s://.*                                                          | Regular expression for determining which IRIs are allowed in delegated SERVICE calls (if not overriden by the cx-common:allowServicePattern address property) |      | 
| cx.agent.service.asset.deny               |          | ^$                                                                             | Regular expression for determining which IRIs are denied in delegated SERVICE calls (it not overridden by the cx-common:denyServicePattern address property)  |      |                                                                                                                                                                       | 
| cx.agent.service.connector.allow          |          | (http&#124;edc)s://.*                                                          | Regular expression for determining which URLs are allowed in remote asset calls (if not overriden by the cx-common:allowServicePattern address property)      |      | 
| cx.agent.service.connector.deny           |          | ^$                                                                             | Regular expression for determining which URLs are denied in remote asset calls (it not overridden by the cx-common:denyServicePattern address property)       |      |                                                                                                                                                                       | 
| cx.agent.dataspace.remotes                |          | BPNL00000003COJN=http://oem-control-plane:8084,BPNL00000003CPIY=http://tiera-control-plane:8084                 | business partner control plane protocol urls wkth associated partner ids to synchronize with (if using internal matchmaking)                                  | L    | 
| cx.agent.sparql.verbose                   |          | false                                                                          | Controls the verbosity of the SparQL Engine                                                                                                                   |      | 
| cx.agent.threadpool.size                  |          | 4                                                                              | Number of threads pooled for any concurrent batch calls and synchronisation actions                                                                           |      | 
| cx.agent.federation.batch.max             |          | 9223372036854775807 / 8                                                        | Maximal number of tuples to send in one query                                                                                                                 |      | 
| cx.agent.negotiation.poll                 |          | 1000                                                                           | Number of milliseconds between negotiation status checks                                                                                                      |      | 
| cx.agent.negotiation.timeout              |          | 30000                                                                          | Number of milliseconds after which a pending negotiation is regarded as stale                                                                                 |      | 
| cx.agent.connect.timeout                  |          |                                                                                | Number of milliseconds after which a connection attempt is regarded as stale                                                                                  |      | 
| cx.agent.read.timeout                     |          | 1080000                                                                        | Number of milliseconds after which a reading attempt is regarded as stale                                                                                     |      | 
| cx.agent.call.timeout                     |          |                                                                                | Number of milliseconds after which a complete call is regarded as stale                                                                                       |      | 
| cx.agent.write.timeout                    |          |                                                                                | Number of milliseconds after which a write attempt is regarded as stale                                                                                       |      | 
| cx.agent.edc.version                      |          | 0.7.0                                                                          | Version of the TX EDC that is used (in case that management/transfer API changes)                                                                             |      | 

