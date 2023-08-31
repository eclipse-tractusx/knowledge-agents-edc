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

# Tractus-X Knowledge Agents EDC Extensions (KA-EDC) Documentation

In the Knowledge Agent Architecture, an Agent is any component which speaks and/or enacts a Semantic Web protocol, such as SPARQL.

The Tractus-X Knowledge Agents EDC Extensions (KA-EDC) introduces support for these protocols (and runnable applications) into the [Eclipse DataSpace Connector](https://github.com/eclipse-edc/Connector) and [Tractus-X EDC](https://github.com/eclipse-tractusx/tractusx-edc).

In particular, KA-EDC implements the so-called Matchmaking Agent endpoint that is able to discover and delegate to business data & functions provided by Binding Agents such as provided by [Knowledge Agents Reference Implementations (KA-RI)](https://github.com/eclipse-tractusx/knowledge-agents).

In contrast to the Binding Agents which are restricted to a subset of the full SPARQL protocol called the KA-BIND profile, KA-EDC implements the KA-MATCH and KA-TRANSFER profiles. The data upon which KA-EDC operates however consists of ontology information and the data catalogue of the respective dataspace tenant.

## How it works

![KA-Enabled EDC Setup](edc_http_0.3.3.drawio.svg)

KA-EDC works as a kind of tunnel/dispatched for federated Semantic Web queries:
- An Agent (a REST endpoint controller) is headed towards a consuming parties intranet applications and speaks a standard query protocol (here: SPARQL in a federated profile called KA-MATCH).
- The Agent talks to the (standard) EDC Control Plane to negotiate/initiate an HttpProxy transfer to a target asset (Graph). It also overtakes the role of the application to manage any resulting Endpoint Data References (EDR).
- On the data provider side, any backend data sources (speaking a simpler, non-federated SPARQL profile called KA-BIND) will be registered using a dedicated asset type (cx-common:Protocol?w3c:http:SPARQL).
- When a graph asset is requested by the Agent, the Control Plane will produce an EDR to the KA-EDC Agent plane which has been registered to handle the corresponding asset types.
- Using the EDR's, the Agent will tunnel the SPARQL request (using the KA-TRANSFER profile) through the Agent Plane(s) where it will not directly hit its final destination.
- Instead, the consumer-side Agent engine will become active to validate, perform preprocessing and finally delegate the simpler KA-BIND calls to the actual endpoints. 
- The scheme is also used to store special query assets (called Skills using the asset type cx-common:Protocol?w3c:http:SKILL) which operate as a kind of stored procedures.

When running an EDC connector from the Tractus-X Knowledge Agents EDC Extensions repository there are three setups to choose from. They only vary by
using different extensions for

- Resolving of Connector-Identities
- Persistence of the Control-Plane-State
- Persistence of Secrets (Vault)

## Connector Setup

The three supported setups are.

- Setup 1: Pure in Memory & Hashicorp Vault **Not intended for production use!**
  - [Control Plane](https://github.com/eclipse-tractusx/edc-controlplane/edc-controlplane-memory-hashicorp-vault/README.md)
  - [Agent Plane](../agent-plane/agentplane-hashicorp/README.md)
      - [Data Plane](https://github.com/eclipse-tractusx/edc-dataplane/edc-dataplane-hashicorp-vault/README.md)
      - [JWT Auth Extension](../common/jwt-auth/README.md)
- Setup 2: PostgreSQL & Azure Vault 
    - [Control Plane](https://github.com/eclipse-tractusx/edc-controlplane/edc-controlplane-postgresql-azure-vault/README.md)
    - [Agent Plane](../agent-plane/agentplane-azure-vault/README.md)
        - [Data Plane](https://github.com/eclipse-tractusx/edc-dataplane/edc-dataplane-azure-vault/README.md)
        - [JWT Auth Extension](../common/jwt-auth/README.md)
- Setup 3: PostgreSQL & HashiCorp Vault
    - [Control Plane](https://github.com/eclipse-tractusx/edc-controlplane/README.md)
    - [Agent Plane](../agent-plane/agentplane-hashicorp/README.md)
        - [Data Plane](https://github.com/eclipse-tractusx/edc-dataplane/edc-dataplane-hashicorp-vault/README.md)
        - [JWT Auth Extension](../common/jwt-auth/README.md)

## Helm Deployment

To install a KA-enabled EDC (Setup 1 - Memory & Hashicorp Vault), add the following lines to the dependency section of your Charts.yaml

```yaml
dependencies:
  
    - name: agent-connector-memory
      repository: https://eclipse-tractusx.github.io/charts/dev
      version: 1.9.5-SNAPSHOT
      alias: my-connector
```

To install a KA-enabled EDC (Setup 2 -Postgresql & Azure Vault), add the following lines to the dependency section of your Charts.yaml

```yaml
dependencies:
  
    - name: agent-connector-azure-vault
      repository: https://eclipse-tractusx.github.io/charts/dev
      version: 1.9.5-SNAPSHOT
      alias: my-connector
```

To install a KA-enabled EDC (Setup 3 -Postgresql & Hashicorp Vault), add the following lines to the dependency section of your Charts.yaml

```yaml
dependencies:
  
    - name: agent-connector
      repository: https://eclipse-tractusx.github.io/charts/dev
      version: 1.9.5-SNAPSHOT
      alias: my-connector
```

The configuration in your values.yaml follows the [Tractux-X EDC Helm Chart](https://github.com/eclipse-tractusx/tractusx-edc/blob/main/charts/tractusx-connector/README.md), but provides for several data planes with different source type profiles including special settings for an Agent Plane.
The agent-connector chart is documented [here](charts/agent-connector/README.md).

```yaml
my-connector:
  participant:
    id: BPNL0000000DUMMY
  nameOverride: my-connector
  fullnameOverride: "my-connector"
  # -- Self-Sovereign Identity Settings
  ssi:
    miw:
      # -- MIW URL
      url: *miwUrl
      # -- The BPN of the issuer authority
      authorityId: *issuerAuthority
    oauth:
      # -- The URL (of KeyCloak), where access tokens can be obtained
      tokenurl: *keyCloakRealm
      client:
        # -- The client ID for KeyCloak
        id: *keyCloakClient
        # -- The alias under which the client secret is stored in the vault.
        secretAlias: "client-secret":
  # -- The Vault Settings can be Azure or Hashicorp
  vault: *vaultSettings
  # -- The Control plane
  controlplane:
    ## Ingress declaration to expose the control plane
    ingresses:
      - enabled: true
        # -- The hostname to be used to precisely map incoming traffic onto the underlying network service
        hostname: "myconnector.public.ip"
        # -- EDC endpoints exposed by this ingress resource
        endpoints:
          - protocol
          - management
          - control
        # -- Enables TLS on the ingress resource
        tls:
          enabled: true
        # -- If you do not have a default cluster issuer
        certManager:
          issuer: my-cluster-issuer
  # -- The Data planes
  dataplanes:
    # -- Default data plane is already an agent plane (has the agent section non-empty)
    dataplane:
      # -- Additional or default resources 
      configs: 
        # -- Overides the default dataspace.ttl to include all important BPNs and connectors
        dataspace.ttl: |-
          ################################################
          # Agent Bootstrap Graph
          ################################################
          @prefix cx-common: <https://w3id.org/catenax/ontology/common#> .
          @prefix bpnl: <bpn:legal:> .
          @prefix : <GraphAsset?local=Dataspace> .
          @base <GraphAsset?local=Dataspace> .

          bpnl:BPNL0000000DUMMY cx-common:hasConnector <edcs://myconnector.public.ip>.
          bpnl:BPNL0000000DUMM2 cx-common:hasConnector <edcs://otherconnector.public.ip>.
      # -- Agent configuration (if non-zero its an agent plane)
      agent:
        # -- Maximal number of tuples processed in one sub-query
        maxbatchsize: 8
        # -- Number of seconds between synchronization runs
        synchronization: 60000
        # -- URLs of the remote connectors to synchronize the catalogue with
        connectors: 
          - https://otherconnector.public.ip
      ## Ingress declaration to expose data plane
      ingresses:
        - enabled: true
          hostname: "myagent.public.ip"
          # -- EDC endpoints exposed by this ingress resource
          endpoints:
            - public
            - default
            - control
            - callback
          # -- Enables TLS on the ingress resource
          tls:
            enabled: true
          # -- If you do not have a default cluster issuer
          certManager:
            issuer: my-cluster-issuer
```

## Recommended Documentation

### This Repository

- [Application: Agent Plane](../agent-plane)
- [Extension: JWT Authentication](../common/auth-jwt/README.md)

### Tractus-X EDC

- [Tractus-X EDC Documentation](https://github.com/eclipse-tractusx/docs/Readme.md)

### Eclipse Dataspace Connector

- [EDC Domain Model](https://github.com/eclipse-edc/Connector/blob/main/docs/developer/architecture/domain-model.md)
- [EDC Open API Spec](https://github.com/eclipse-edc/Connector/blob/main/resources/openapi/openapi.yaml)
- [HTTP Receiver Extension](https://github.com/eclipse-edc/Connector/tree/main/extensions/control-plane/http-receiver)

## NOTICE

This work is licensed under the [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0).

- SPDX-License-Identifier: Apache-2.0
- SPDX-FileCopyrightText: 2022,2023 T-Systems International GmbH
- SPDX-FileCopyrightText: 2022,2023 Contributors to the Eclipse Foundation
- Source URL: https://github.com/eclipse-tractusx/knowledge-agents-edc