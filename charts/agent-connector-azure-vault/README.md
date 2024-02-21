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

# agent-connector-azure-vault

![Version: 1.12.18-SNAPSHOT](https://img.shields.io/badge/Version-1.12.18--SNAPSHOT-informational?style=flat-square) ![Type: application](https://img.shields.io/badge/Type-application-informational?style=flat-square) ![AppVersion: 1.12.18-SNAPSHOT](https://img.shields.io/badge/AppVersion-1.12.18--SNAPSHOT-informational?style=flat-square)

A Helm chart for an Agent-Enabled Tractus-X Eclipse Data Space Connector configured against Azure Vault. This is a variant of [the Tractus-X Azure Vault Connector Helm Chart](https://github.com/eclipse-tractusx/tractusx-edc/tree/main/charts/tractusx-connector-azure-vault) which allows
to deal with several data (and agent) planes. The connector deployment consists of at least two runtime consists of a
Control Plane and one or several Data Planes. Note that _no_ external dependencies such as a PostgreSQL database and Azure KeyVault are included.

This chart is intended for use with an _existing_ PostgreSQL database and an _existing_ Azure KeyVault.

**Homepage:** <https://github.com/eclipse-tractusx/knowledge-agents-edc/>

## Setting up your BPNL and the Control Plane's Management API Key

The secure API-Key that is shared between control and agent plane is configured in the following property:
- 'controlplane.endpoints.management.authKey': Cleartext API Key as used to secure the control planes management api (and is used by the agent plane to synchronize assets and negotiate calls).

You should set your BPNL in the folloing property:
- 'participant.id': 'BPNL' followed by 12 alphanumerical characters as handed out to you during onboarding.

## Setting up Azure Vault

You should set your BPNL in the folloing property:
- 'vault.azure.name': Name of the vault
- 'vault.azure.client': Id of the registered application that this EDC represents
- 'vault.azure.tenant': Id of the subscription that the vault runs into
- 'vault.azure.secret' or 'vault.azure.certificate': the secret/credential to use when interacting with Azure Vault

### Setting up the transfer token encryption

Transfer tokens handed out from the provider to the consumer should be signed and encrypted. For that purpose
you should setup a private/public certificate as well as a symmetric AES key.

- 'vault.secretNames.transferProxyTokenSignerPrivateKey':
- 'vault.secretNames.transferProxyTokenSignerPublicKey':
- 'vault.secretNames.transferProxyTokenEncryptionAesKey':

## Setting up SSI

### Preconditions

- the [Managed Identity Walled (MIW)](https://github.com/eclipse-tractusx/managed-identity-wallet) must be running and reachable via network
- the necessary set of VerifiableCredentials for this participant must be pushed to MIW. This is typically done by the
  Portal during participant onboarding
- KeyCloak must be running and reachable via network
- an account with KeyCloak must be created for this BPN and the connector must be able to obtain access tokens
- the client ID and client secret corresponding to that account must be known

### Preparatory work

- store your KeyCloak client secret in the Azure KeyVault. The exact procedure is as follows:
 ```bash
 az keyvault secret set --vault-name <YOUR_VAULT_NAME> --name client-secret --value "$YOUR_CLIENT_SECRET"
 ```
 By default, Tractus-X EDC expects to find the secret under `client-secret`.

### Configure the chart

Be sure to provide the following configuration entries to your Tractus-X EDC Helm chart:
- `controlplane.ssi.miw.url`: the URL
- `controlplane.ssi.miw.authorityId`: the BPN of the issuer authority
- `controlplane.ssi.oauth.tokenurl`: the URL (of KeyCloak), where access tokens can be obtained
- `controlplane.ssi.oauth.client.id`: client ID for KeyCloak
- `controlplane.ssi.oauth.client.secretAlias`: the alias under which the client secret is stored in the vault. Defaults to `client-secret`.

## Setting up the Agent Planes

Make sure to adapt the Agent Plane's application-facing endpoint security:
- 'dataplanes.agentplane.auth.default.type': The type of authentication service to use (defaults to api-key, you could also use jwt)
- 'dataplanes.agentplane.auth.default.apiCode': If type is api-key, this is the hash of the accepted api key
- 'dataplanes.agentplane.auth.default.vaultKey': If type is api-key, this is the key where the api key can be retrieved from the configured vault
- 'dataplanes.agentplane.auth.default.publicKey': If type is jwt, this is a url where the public key to verify token with can be found
- 'dataplanes.agentplane.auth.default.checkExpiry': If type is jwt, determines whether token expiry is checked (default: true)

Be sure to review the Agent Plane's service delegation filter which regulates with which external Agent's (SERVICE) this instance may interact. These properties form typical allow/deny conditions. Because of the nature of SPARQL, interacting with such a service may not only mean to import data from there, but you must take into account bound variables in the SERVICE contexts are also exported to there. So you should be rather prohibitive here. 
- 'dataplanes.agentplane.agent.services.allow': A regular expression of allowed Agent/Sparql SERVICE contexts in the default graph (federated data catalogue). The default graph only contains meta-data and can only be invoked by any in-house application, so usually you can be a bit more relaxed on this level. For example, you might be tempted to allow to mix your application logic and data with some universal service, such as Wikidata.
- 'dataplanes.agentplane.agent.services.deny': A regular expression of denied outgoing Agent/Sparql SERVICE contexts in the default graph (federated data catalogue). Typically you would restrict any unsecured http call by this properties.
- 'dataplanes.agentplane.agent.services.assets.allow': A regular expression of allowed Agent/Sparql SERVICE contexts when inside a data graph/asset (unless there are more specific settings in the asset itself). Since this affects how you can spice up your business data, you would only allow connections to trusted business partners connectors.
- 'dataplanes.agentplane.agent.services.assets.deny': A regular expression of denied Agent/Sparql SERVICE contexts. Use this to filter out unsecure protocols such as edc and http as well as to implement blacklists.

Be sure to adapt the agent configuration
- 'dataplanes.agentplane.configs.dataspace.ttl': additional TTL text resource which lists the partner BPNs and their associated connectors.
- 'dataplanes.agentplane.agent.maxbatchsize': Should be restricted to a smaller number of tuples (10-100) if you intend to communicate over larger datasets.
- 'dataplanes.agentplane.agent.synchronization': Should be set to a positive number of seconds to activate the automatic synchronization of federated data catalogues.
- 'dataplanes.agentplane.agent.connectors': Should be a list of partner connector addresses which will be synchronized in the federated data catalogue.

### Launching the application

As an easy starting point, please consider using [this example configuration](https://github.com/eclipse-tractusx/tractusx-edc/blob/main/edc-tests/deployment/src/main/resources/helm/tractusx-connector-test.yaml)
to launch the application. The configuration values mentioned above (`controlplane.ssi.*`) will have to be adapted manually.
Combined, run this shell command to start the in-memory Tractus-X EDC runtime:

```shell
helm repo add eclipse-tractusx https://eclipse-tractusx.github.io/charts/dev
helm install my-release eclipse-tractusx/agent-connector-azure-vault --version 1.12.18-SNAPSHOT\
     -f <path-to>/tractusx-connector-azure-vault-test.yaml \
     --set vault.azure.name=$AZURE_VAULT_NAME \
     --set vault.azure.client=$AZURE_CLIENT_ID \
     --set vault.azure.secret=$AZURE_CLIENT_SECRET \
     --set vault.azure.tenant=$AZURE_TENANT_ID
```

## Maintainers

| Name | Email | Url |
| ---- | ------ | --- |
| Tractus-X Knowledge Agents Team |  |  |

## Source Code

* <https://github.com/eclipse-tractusx/knowledge-agents-edc/tree/main/charts/agent-connector>

## Requirements

| Repository | Name | Version |
|------------|------|---------|
| https://charts.bitnami.com/bitnami | postgresql(postgresql) | 12.1.6 |

## Values

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| backendService.httpProxyTokenReceiverUrl | string | `""` |  |
| controlplane.affinity | object | `{}` |  |
| controlplane.autoscaling.enabled | bool | `false` | Enables [horizontal pod autoscaling](https://kubernetes.io/docs/tasks/run-application/horizontal-pod-autoscale/https://kubernetes.io/docs/tasks/run-application/horizontal-pod-autoscale/) |
| controlplane.autoscaling.maxReplicas | int | `100` | Maximum replicas if resource consumption exceeds resource threshholds |
| controlplane.autoscaling.minReplicas | int | `1` | Minimal replicas if resource consumption falls below resource threshholds |
| controlplane.autoscaling.targetCPUUtilizationPercentage | int | `80` | targetAverageUtilization of cpu provided to a pod |
| controlplane.autoscaling.targetMemoryUtilizationPercentage | int | `80` | targetAverageUtilization of memory provided to a pod |
| controlplane.businessPartnerValidation.log.agreementValidation | bool | `true` |  |
| controlplane.debug.enabled | bool | `false` |  |
| controlplane.debug.port | int | `1044` |  |
| controlplane.debug.suspendOnStart | bool | `false` |  |
| controlplane.endpoints | object | `{"control":{"path":"/control","port":8083},"default":{"path":"/api","port":8080},"management":{"authKey":"","path":"/management","port":8081},"metrics":{"path":"/metrics","port":9090},"protocol":{"path":"/api/v1/dsp","port":8084}}` | endpoints of the control plane |
| controlplane.endpoints.control | object | `{"path":"/control","port":8083}` | control api, used for internal control calls. can be added to the internal ingress, but should probably not |
| controlplane.endpoints.control.path | string | `"/control"` | path for incoming api calls |
| controlplane.endpoints.control.port | int | `8083` | port for incoming api calls |
| controlplane.endpoints.default | object | `{"path":"/api","port":8080}` | default api for health checks, should not be added to any ingress |
| controlplane.endpoints.default.path | string | `"/api"` | path for incoming api calls |
| controlplane.endpoints.default.port | int | `8080` | port for incoming api calls |
| controlplane.endpoints.management | object | `{"authKey":"","path":"/management","port":8081}` | data management api, used by internal users, can be added to an ingress and must not be internet facing |
| controlplane.endpoints.management.authKey | string | `""` | authentication key, must be attached to each 'X-Api-Key' request header |
| controlplane.endpoints.management.path | string | `"/management"` | path for incoming api calls |
| controlplane.endpoints.management.port | int | `8081` | port for incoming api calls |
| controlplane.endpoints.metrics | object | `{"path":"/metrics","port":9090}` | metrics api, used for application metrics, must not be internet facing |
| controlplane.endpoints.metrics.path | string | `"/metrics"` | path for incoming api calls |
| controlplane.endpoints.metrics.port | int | `9090` | port for incoming api calls |
| controlplane.endpoints.protocol | object | `{"path":"/api/v1/dsp","port":8084}` | dsp api, used for inter connector communication and must be internet facing |
| controlplane.endpoints.protocol.path | string | `"/api/v1/dsp"` | path for incoming api calls |
| controlplane.endpoints.protocol.port | int | `8084` | port for incoming api calls |
| controlplane.env.EDC_JSONLD_HTTPS_ENABLED | string | `"true"` |  |
| controlplane.envConfigMapNames | list | `[]` |  |
| controlplane.envSecretNames | list | `[]` |  |
| controlplane.envValueFrom | object | `{}` |  |
| controlplane.image.pullPolicy | string | `"IfNotPresent"` | [Kubernetes image pull policy](https://kubernetes.io/docs/concepts/containers/images/#image-pull-policy) to use |
| controlplane.image.repository | string | `""` | Which derivate of the control plane to use. when left empty the deployment will select the correct image automatically |
| controlplane.image.tag | string | `""` | Overrides the image tag whose default is the chart appVersion |
| controlplane.ingresses[0].annotations | object | `{}` | Additional ingress annotations to add |
| controlplane.ingresses[0].certManager.clusterIssuer | string | `""` | If preset enables certificate generation via cert-manager cluster-wide issuer |
| controlplane.ingresses[0].certManager.issuer | string | `""` | If preset enables certificate generation via cert-manager namespace scoped issuer |
| controlplane.ingresses[0].className | string | `""` | Defines the [ingress class](https://kubernetes.io/docs/concepts/services-networking/ingress/#ingress-class)  to use |
| controlplane.ingresses[0].enabled | bool | `false` |  |
| controlplane.ingresses[0].endpoints | list | `["protocol"]` | EDC endpoints exposed by this ingress resource |
| controlplane.ingresses[0].hostname | string | `"edc-control.local"` | The hostname to be used to precisely map incoming traffic onto the underlying network service |
| controlplane.ingresses[0].tls | object | `{"enabled":false,"secretName":""}` | TLS [tls class](https://kubernetes.io/docs/concepts/services-networking/ingress/#tls) applied to the ingress resource |
| controlplane.ingresses[0].tls.enabled | bool | `false` | Enables TLS on the ingress resource |
| controlplane.ingresses[0].tls.secretName | string | `""` | If present overwrites the default secret name |
| controlplane.ingresses[1].annotations | object | `{}` | Additional ingress annotations to add |
| controlplane.ingresses[1].certManager.clusterIssuer | string | `""` | If preset enables certificate generation via cert-manager cluster-wide issuer |
| controlplane.ingresses[1].certManager.issuer | string | `""` | If preset enables certificate generation via cert-manager namespace scoped issuer |
| controlplane.ingresses[1].className | string | `""` | Defines the [ingress class](https://kubernetes.io/docs/concepts/services-networking/ingress/#ingress-class)  to use |
| controlplane.ingresses[1].enabled | bool | `false` |  |
| controlplane.ingresses[1].endpoints | list | `["management","control"]` | EDC endpoints exposed by this ingress resource |
| controlplane.ingresses[1].hostname | string | `"edc-control.intranet"` | The hostname to be used to precisely map incoming traffic onto the underlying network service |
| controlplane.ingresses[1].tls | object | `{"enabled":false,"secretName":""}` | TLS [tls class](https://kubernetes.io/docs/concepts/services-networking/ingress/#tls) applied to the ingress resource |
| controlplane.ingresses[1].tls.enabled | bool | `false` | Enables TLS on the ingress resource |
| controlplane.ingresses[1].tls.secretName | string | `""` | If present overwrites the default secret name |
| controlplane.initContainers | list | `[]` |  |
| controlplane.livenessProbe.enabled | bool | `true` | Whether to enable kubernetes [liveness-probe](https://kubernetes.io/docs/tasks/configure-pod-container/configure-liveness-readiness-startup-probes/) |
| controlplane.livenessProbe.failureThreshold | int | `6` | when a probe fails kubernetes will try 6 times before giving up |
| controlplane.livenessProbe.initialDelaySeconds | int | `30` | seconds to wait before performing the first liveness check |
| controlplane.livenessProbe.periodSeconds | int | `10` | this fields specifies that kubernetes should perform a liveness check every 10 seconds |
| controlplane.livenessProbe.successThreshold | int | `1` | number of consecutive successes for the probe to be considered successful after having failed |
| controlplane.livenessProbe.timeoutSeconds | int | `5` | number of seconds after which the probe times out |
| controlplane.logging | string | `".level=INFO\norg.eclipse.edc.level=ALL\nhandlers=java.util.logging.ConsoleHandler\njava.util.logging.ConsoleHandler.formatter=java.util.logging.SimpleFormatter\njava.util.logging.ConsoleHandler.level=ALL\njava.util.logging.SimpleFormatter.format=[%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS] [%4$-7s] %5$s%6$s%n"` | configuration of the [Java Util Logging Facade](https://docs.oracle.com/javase/7/docs/technotes/guides/logging/overview.html) |
| controlplane.nodeSelector | object | `{}` |  |
| controlplane.opentelemetry | string | `"otel.javaagent.enabled=false\notel.javaagent.debug=false"` | configuration of the [Open Telemetry Agent](https://opentelemetry.io/docs/instrumentation/java/automatic/agent-config/) to collect and expose metrics |
| controlplane.podAnnotations | object | `{}` | additional annotations for the pod |
| controlplane.podLabels | object | `{}` | additional labels for the pod |
| controlplane.podSecurityContext | object | `{"fsGroup":10001,"runAsGroup":10001,"runAsUser":10001,"seccompProfile":{"type":"RuntimeDefault"}}` | The [pod security context](https://kubernetes.io/docs/tasks/configure-pod-container/security-context/#set-the-security-context-for-a-pod) defines privilege and access control settings for a Pod within the deployment |
| controlplane.podSecurityContext.fsGroup | int | `10001` | The owner for volumes and any files created within volumes will belong to this guid |
| controlplane.podSecurityContext.runAsGroup | int | `10001` | Processes within a pod will belong to this guid |
| controlplane.podSecurityContext.runAsUser | int | `10001` | Runs all processes within a pod with a special uid |
| controlplane.podSecurityContext.seccompProfile.type | string | `"RuntimeDefault"` | Restrict a Container's Syscalls with seccomp |
| controlplane.readinessProbe.enabled | bool | `true` | Whether to enable kubernetes [readiness-probes](https://kubernetes.io/docs/tasks/configure-pod-container/configure-liveness-readiness-startup-probes/) |
| controlplane.readinessProbe.failureThreshold | int | `6` | when a probe fails kubernetes will try 6 times before giving up |
| controlplane.readinessProbe.initialDelaySeconds | int | `30` | seconds to wait before performing the first readiness check |
| controlplane.readinessProbe.periodSeconds | int | `10` | this fields specifies that kubernetes should perform a readiness check every 10 seconds |
| controlplane.readinessProbe.successThreshold | int | `1` | number of consecutive successes for the probe to be considered successful after having failed |
| controlplane.readinessProbe.timeoutSeconds | int | `5` | number of seconds after which the probe times out |
| controlplane.replicaCount | int | `1` |  |
| controlplane.resources | object | `{}` | [resource management](https://kubernetes.io/docs/concepts/configuration/manage-resources-containers/) for the container |
| controlplane.securityContext.allowPrivilegeEscalation | bool | `false` | Controls [Privilege Escalation](https://kubernetes.io/docs/concepts/security/pod-security-policy/#privilege-escalation) enabling setuid binaries changing the effective user ID |
| controlplane.securityContext.capabilities.add | list | `[]` | Specifies which capabilities to add to issue specialized syscalls |
| controlplane.securityContext.capabilities.drop | list | `["ALL"]` | Specifies which capabilities to drop to reduce syscall attack surface |
| controlplane.securityContext.readOnlyRootFilesystem | bool | `true` | Whether the root filesystem is mounted in read-only mode |
| controlplane.securityContext.runAsNonRoot | bool | `true` | Requires the container to run without root privileges |
| controlplane.securityContext.runAsUser | int | `10001` | The container's process will run with the specified uid |
| controlplane.service.annotations | object | `{}` |  |
| controlplane.service.type | string | `"ClusterIP"` | [Service type](https://kubernetes.io/docs/concepts/services-networking/service/#publishing-services-service-types) to expose the running application on a set of Pods as a network service. |
| controlplane.ssi.miw.authorityId | string | `""` | The BPN of the issuer authority |
| controlplane.ssi.miw.url | string | `""` | MIW URL |
| controlplane.ssi.oauth.client.id | string | `""` | The client ID for KeyCloak |
| controlplane.ssi.oauth.client.secretAlias | string | `""` | The alias under which the client secret is stored in the vault. |
| controlplane.ssi.oauth.tokenurl | string | `""` | The URL (of KeyCloak), where access tokens can be obtained |
| controlplane.tolerations | list | `[]` |  |
| controlplane.url.protocol | string | `""` | Explicitly declared url for reaching the dsp api (e.g. if ingresses not used) |
| controlplane.volumeMounts | list | `[]` | declare where to mount [volumes](https://kubernetes.io/docs/concepts/storage/volumes/) into the container |
| controlplane.volumes | list | `[]` | [volume](https://kubernetes.io/docs/concepts/storage/volumes/) directories |
| customLabels | object | `{}` | To add some custom labels |
| dataplanes.dataplane.affinity | object | `{}` |  |
| dataplanes.dataplane.agent | object | `{"connectors":[],"default":["dataspace.ttl","https://w3id.org/catenax/ontology.ttl"],"maxbatchsize":"9223372036854775807","services":{"allow":"(edcs?://.*)|(https://query\\\\.wikidata\\\\.org/sparql)","asset":{"allow":"(edcs?://.*)","deny":"https?://.*"},"deny":"http://.*"},"skillcontract":"Contract?partner=Skill","synchronization":-1}` | Agent-Specific Settings |
| dataplanes.dataplane.agent.connectors | list | `[]` | The list of remote connector IDS URLs to synchronize with |
| dataplanes.dataplane.agent.default | list | `["dataspace.ttl","https://w3id.org/catenax/ontology.ttl"]` | A list of local or remote graph descriptions to build the default meta-graph/federated data catalogue |
| dataplanes.dataplane.agent.maxbatchsize | string | `"9223372036854775807"` | Sets the maximal batch size when delegating to agents and services |
| dataplanes.dataplane.agent.services | object | `{"allow":"(edcs?://.*)|(https://query\\\\.wikidata\\\\.org/sparql)","asset":{"allow":"(edcs?://.*)","deny":"https?://.*"},"deny":"http://.*"}` | A set of configs for regulating outgoing service calls |
| dataplanes.dataplane.agent.services.allow | string | `"(edcs?://.*)|(https://query\\\\.wikidata\\\\.org/sparql)"` | A regular expression which outgoing service URLs must match (unless overwritten by a specific asset property) |
| dataplanes.dataplane.agent.services.asset | object | `{"allow":"(edcs?://.*)","deny":"https?://.*"}` | A set of configs for regulating outgoing service calls when providing an asset (when no specific asset property is given) |
| dataplanes.dataplane.agent.services.asset.allow | string | `"(edcs?://.*)"` | A regular expression which outgoing service URLs must match (unless overwritten by a specific asset property) |
| dataplanes.dataplane.agent.services.asset.deny | string | `"https?://.*"` | A regular expression which outgoing service URLs must not match (unless overwritten by a specific asset property) |
| dataplanes.dataplane.agent.services.deny | string | `"http://.*"` | A regular expression which outgoing service URLs must not match (unless overwritten by a specific asset property) |
| dataplanes.dataplane.agent.skillcontract | string | `"Contract?partner=Skill"` | Names the visible contract under which new skills are published (if not otherwise specified) |
| dataplanes.dataplane.agent.synchronization | int | `-1` | The synchronization interval in ms to update the federated data catalogue |
| dataplanes.dataplane.auth | object | `{"default":{"apiCode":"69609650","checkExpiry":true,"context":"default","exclude":".*/(check|validation).*","publicKey":null,"register":false,"type":"api-key","vaultKey":null}}` | Data Plane Authentication using the KA-EDC-AUTH-JWT extension, any entry has a type (api-key, jwt or composite) and a (set of) path contexts (see endpoints) followed by type-specific entries |
| dataplanes.dataplane.auth.default | object | `{"apiCode":"69609650","checkExpiry":true,"context":"default","exclude":".*/(check|validation).*","publicKey":null,"register":false,"type":"api-key","vaultKey":null}` | the default authentication service |
| dataplanes.dataplane.auth.default.apiCode | string | `"69609650"` | specific api-code associated to the default api-key 'Hello', Change this when type=api-key or use the vault-key property instead. Althugh this represents a number, remember to use quotes not to confuse rendering into the chart. |
| dataplanes.dataplane.auth.default.checkExpiry | bool | `true` | controls whether the expiry date of jwt tokens is checked when type=jwt |
| dataplanes.dataplane.auth.default.context | string | `"default"` | the context(s) of the default authentication service separated by commas |
| dataplanes.dataplane.auth.default.exclude | string | `".*/(check|validation).*"` | excluded paths for liveness checks and validation |
| dataplanes.dataplane.auth.default.publicKey | string | `nil` | public key for checking the validity of jwt tokens, set this when type=jwt |
| dataplanes.dataplane.auth.default.register | bool | `false` | controls whether this service should be registered as the default EDC authentication service globally |
| dataplanes.dataplane.auth.default.type | string | `"api-key"` | the type of the default authentication service (api-key, jwt or composite) |
| dataplanes.dataplane.auth.default.vaultKey | string | `nil` | vault key for obtaining the API key, Set this when type=api-key or use the api-code property instead |
| dataplanes.dataplane.autoscaling.enabled | bool | `false` | Enables [horizontal pod autoscaling](https://kubernetes.io/docs/tasks/run-application/horizontal-pod-autoscale/https://kubernetes.io/docs/tasks/run-application/horizontal-pod-autoscale/) |
| dataplanes.dataplane.autoscaling.maxReplicas | int | `100` | Maximum replicas if resource consumption exceeds resource threshholds |
| dataplanes.dataplane.autoscaling.minReplicas | int | `1` | Minimal replicas if resource consumption falls below resource threshholds |
| dataplanes.dataplane.autoscaling.targetCPUUtilizationPercentage | int | `80` | targetAverageUtilization of cpu provided to a pod |
| dataplanes.dataplane.autoscaling.targetMemoryUtilizationPercentage | int | `80` | targetAverageUtilization of memory provided to a pod |
| dataplanes.dataplane.aws.accessKeyId | string | `""` |  |
| dataplanes.dataplane.aws.endpointOverride | string | `""` |  |
| dataplanes.dataplane.aws.secretAccessKey | string | `""` |  |
| dataplanes.dataplane.configs | object | `{"dataspace.ttl":"#################################################################\n# Catena-X Agent Bootstrap Graph in TTL/RDF/OWL FORMAT\n#################################################################\n@prefix : <GraphAsset?local=Dataspace> .\n@base <GraphAsset?local=Dataspace> .\n"}` | A set of additional configuration files |
| dataplanes.dataplane.configs."dataspace.ttl" | string | `"#################################################################\n# Catena-X Agent Bootstrap Graph in TTL/RDF/OWL FORMAT\n#################################################################\n@prefix : <GraphAsset?local=Dataspace> .\n@base <GraphAsset?local=Dataspace> .\n"` | An example of an empty graph in ttl syntax |
| dataplanes.dataplane.debug.enabled | bool | `false` |  |
| dataplanes.dataplane.debug.port | int | `1044` |  |
| dataplanes.dataplane.debug.suspendOnStart | bool | `false` |  |
| dataplanes.dataplane.destinationTypes | string | `"HttpProxy,AmazonS3"` | a comma-separated list of supported transfer types |
| dataplanes.dataplane.endpoints.callback.path | string | `"/callback"` |  |
| dataplanes.dataplane.endpoints.callback.port | int | `8087` |  |
| dataplanes.dataplane.endpoints.control.path | string | `"/api/dataplane/control"` |  |
| dataplanes.dataplane.endpoints.control.port | int | `8083` |  |
| dataplanes.dataplane.endpoints.default.path | string | `"/api"` |  |
| dataplanes.dataplane.endpoints.default.port | int | `8080` |  |
| dataplanes.dataplane.endpoints.metrics.path | string | `"/metrics"` |  |
| dataplanes.dataplane.endpoints.metrics.port | int | `9090` |  |
| dataplanes.dataplane.endpoints.proxy.path | string | `"/proxy"` |  |
| dataplanes.dataplane.endpoints.proxy.port | int | `8186` |  |
| dataplanes.dataplane.endpoints.public.path | string | `"/api/public"` |  |
| dataplanes.dataplane.endpoints.public.port | int | `8081` |  |
| dataplanes.dataplane.env | object | `{}` |  |
| dataplanes.dataplane.envConfigMapNames | list | `[]` |  |
| dataplanes.dataplane.envSecretNames | list | `[]` |  |
| dataplanes.dataplane.envValueFrom | object | `{}` |  |
| dataplanes.dataplane.image.pullPolicy | string | `"IfNotPresent"` | [Kubernetes image pull policy](https://kubernetes.io/docs/concepts/containers/images/#image-pull-policy) to use |
| dataplanes.dataplane.image.repository | string | `""` | Which derivate of the data plane to use. when left empty the deployment will select the correct image automatically |
| dataplanes.dataplane.image.tag | string | `""` | Overrides the image tag whose default is the chart appVersion |
| dataplanes.dataplane.ingresses[0].annotations | string | `nil` | Additional ingress annotations to add, for example when supporting more demanding use cases you may set { nginx.org/proxy-connect-timeout: "30s", nginx.org/proxy-read-timeout: "360s", nginx.org/client-max-body-size: "10m"} |
| dataplanes.dataplane.ingresses[0].certManager.clusterIssuer | string | `""` | If preset enables certificate generation via cert-manager cluster-wide issuer |
| dataplanes.dataplane.ingresses[0].certManager.issuer | string | `""` | If preset enables certificate generation via cert-manager namespace scoped issuer |
| dataplanes.dataplane.ingresses[0].className | string | `""` | Defines the [ingress class](https://kubernetes.io/docs/concepts/services-networking/ingress/#ingress-class)  to use |
| dataplanes.dataplane.ingresses[0].enabled | bool | `false` |  |
| dataplanes.dataplane.ingresses[0].endpoints | list | `["public"]` | EDC endpoints exposed by this ingress resource |
| dataplanes.dataplane.ingresses[0].hostname | string | `"edc-data.local"` | The hostname to be used to precisely map incoming traffic onto the underlying network service |
| dataplanes.dataplane.ingresses[0].tls | object | `{"enabled":false,"secretName":""}` | TLS [tls class](https://kubernetes.io/docs/concepts/services-networking/ingress/#tls) applied to the ingress resource |
| dataplanes.dataplane.ingresses[0].tls.enabled | bool | `false` | Enables TLS on the ingress resource |
| dataplanes.dataplane.ingresses[0].tls.secretName | string | `""` | If present overwrites the default secret name |
| dataplanes.dataplane.initContainers | list | `[]` |  |
| dataplanes.dataplane.livenessProbe.enabled | bool | `true` | Whether to enable kubernetes [liveness-probe](https://kubernetes.io/docs/tasks/configure-pod-container/configure-liveness-readiness-startup-probes/) |
| dataplanes.dataplane.livenessProbe.failureThreshold | int | `6` | when a probe fails kubernetes will try 6 times before giving up |
| dataplanes.dataplane.livenessProbe.initialDelaySeconds | int | `30` | seconds to wait before performing the first liveness check |
| dataplanes.dataplane.livenessProbe.periodSeconds | int | `10` | this fields specifies that kubernetes should perform a liveness check every 10 seconds |
| dataplanes.dataplane.livenessProbe.successThreshold | int | `1` | number of consecutive successes for the probe to be considered successful after having failed |
| dataplanes.dataplane.livenessProbe.timeoutSeconds | int | `5` | number of seconds after which the probe times out |
| dataplanes.dataplane.logging | string | `".level=INFO\norg.eclipse.edc.level=ALL\nhandlers=java.util.logging.ConsoleHandler\njava.util.logging.ConsoleHandler.formatter=java.util.logging.SimpleFormatter\njava.util.logging.ConsoleHandler.level=ALL\njava.util.logging.SimpleFormatter.format=[%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS] [%4$-7s] %5$s%6$s%n"` | configuration of the [Java Util Logging Facade](https://docs.oracle.com/javase/7/docs/technotes/guides/logging/overview.html) |
| dataplanes.dataplane.name | string | `"agentplane"` | the name of the dataplane |
| dataplanes.dataplane.nodeSelector | object | `{}` |  |
| dataplanes.dataplane.opentelemetry | string | `"otel.javaagent.enabled=false\notel.javaagent.debug=false"` | configuration of the [Open Telemetry Agent](https://opentelemetry.io/docs/instrumentation/java/automatic/agent-config/) to collect and expose metrics |
| dataplanes.dataplane.podAnnotations | object | `{}` | additional annotations for the pod |
| dataplanes.dataplane.podLabels | object | `{}` | additional labels for the pod |
| dataplanes.dataplane.podSecurityContext | object | `{"fsGroup":10001,"runAsGroup":10001,"runAsUser":10001,"seccompProfile":{"type":"RuntimeDefault"}}` | The [pod security context](https://kubernetes.io/docs/tasks/configure-pod-container/security-context/#set-the-security-context-for-a-pod) defines privilege and access control settings for a Pod within the deployment |
| dataplanes.dataplane.podSecurityContext.fsGroup | int | `10001` | The owner for volumes and any files created within volumes will belong to this guid |
| dataplanes.dataplane.podSecurityContext.runAsGroup | int | `10001` | Processes within a pod will belong to this guid |
| dataplanes.dataplane.podSecurityContext.runAsUser | int | `10001` | Runs all processes within a pod with a special uid |
| dataplanes.dataplane.podSecurityContext.seccompProfile.type | string | `"RuntimeDefault"` | Restrict a Container's Syscalls with seccomp |
| dataplanes.dataplane.readinessProbe.enabled | bool | `true` | Whether to enable kubernetes [readiness-probes](https://kubernetes.io/docs/tasks/configure-pod-container/configure-liveness-readiness-startup-probes/) |
| dataplanes.dataplane.readinessProbe.failureThreshold | int | `6` | when a probe fails kubernetes will try 6 times before giving up |
| dataplanes.dataplane.readinessProbe.initialDelaySeconds | int | `30` | seconds to wait before performing the first readiness check |
| dataplanes.dataplane.readinessProbe.periodSeconds | int | `10` | this fields specifies that kubernetes should perform a liveness check every 10 seconds |
| dataplanes.dataplane.readinessProbe.successThreshold | int | `1` | number of consecutive successes for the probe to be considered successful after having failed |
| dataplanes.dataplane.readinessProbe.timeoutSeconds | int | `5` | number of seconds after which the probe times out |
| dataplanes.dataplane.replicaCount | int | `1` |  |
| dataplanes.dataplane.resources | object | `{}` | [resource management](https://kubernetes.io/docs/concepts/configuration/manage-resources-containers/) for the container |
| dataplanes.dataplane.securityContext.allowPrivilegeEscalation | bool | `false` | Controls [Privilege Escalation](https://kubernetes.io/docs/concepts/security/pod-security-policy/#privilege-escalation) enabling setuid binaries changing the effective user ID |
| dataplanes.dataplane.securityContext.capabilities.add | list | `[]` | Specifies which capabilities to add to issue specialized syscalls |
| dataplanes.dataplane.securityContext.capabilities.drop | list | `["ALL"]` | Specifies which capabilities to drop to reduce syscall attack surface |
| dataplanes.dataplane.securityContext.readOnlyRootFilesystem | bool | `true` | Whether the root filesystem is mounted in read-only mode |
| dataplanes.dataplane.securityContext.runAsNonRoot | bool | `true` | Requires the container to run without root privileges |
| dataplanes.dataplane.securityContext.runAsUser | int | `10001` | The container's process will run with the specified uid |
| dataplanes.dataplane.service.port | int | `80` |  |
| dataplanes.dataplane.service.type | string | `"ClusterIP"` | [Service type](https://kubernetes.io/docs/concepts/services-networking/service/#publishing-services-service-types) to expose the running application on a set of Pods as a network service. |
| dataplanes.dataplane.sourceTypes | string | `"cx-common:Protocol?w3c:http:SPARQL,cx-common:Protocol?w3c:http:SKILL,HttpData,AmazonS3"` | a comma-separated list of supported asset types |
| dataplanes.dataplane.tolerations | list | `[]` |  |
| dataplanes.dataplane.url.public | string | `""` | Explicitly declared url for reaching the public api (e.g. if ingresses not used) |
| dataplanes.dataplane.volumeMounts | list | `[]` | declare where to mount [volumes](https://kubernetes.io/docs/concepts/storage/volumes/) into the container |
| dataplanes.dataplane.volumes | list | `[]` | [volume](https://kubernetes.io/docs/concepts/storage/volumes/) directories |
| fullnameOverride | string | `""` |  |
| imagePullSecrets | list | `[]` | Existing image pull secret to use to [obtain the container image from private registries](https://kubernetes.io/docs/concepts/containers/images/#using-a-private-registry) |
| imageRegistry | string | `"docker.io/"` | Image registry to use |
| install.postgresql | bool | `true` |  |
| nameOverride | string | `""` |  |
| networkPolicy.controlplane | object | `{"from":[{"namespaceSelector":{}}]}` | Configuration of the controlplane component |
| networkPolicy.controlplane.from | list | `[{"namespaceSelector":{}}]` | Specify from rule network policy for cp (defaults to all namespaces) |
| networkPolicy.dataplane | object | `{"from":[{"namespaceSelector":{}}]}` | Configuration of the dataplane component |
| networkPolicy.dataplane.from | list | `[{"namespaceSelector":{}}]` | Specify from rule network policy for dp (defaults to all namespaces) |
| networkPolicy.enabled | bool | `false` | If `true` network policy will be created to restrict access to control- and dataplane |
| participant.id | string | `""` | BPN Number |
| postgresql | object | `{"auth":{"database":"edc","password":"password","username":"user"},"jdbcUrl":"jdbc:postgresql://{{ .Release.Name }}-postgresql:5432/edc","primary":{"persistence":{"enabled":false}},"readReplicas":{"persistence":{"enabled":false}}}` | Standard settings for persistence, "jdbcUrl", "username" and "password" need to be overridden |
| serviceAccount.annotations | object | `{}` |  |
| serviceAccount.create | bool | `true` |  |
| serviceAccount.imagePullSecrets | list | `[]` | Existing image pull secret bound to the service account to use to [obtain the container image from private registries](https://kubernetes.io/docs/concepts/containers/images/#using-a-private-registry) |
| serviceAccount.name | string | `""` |  |
| tests | object | `{"hookDeletePolicy":"before-hook-creation,hook-succeeded"}` | Configurations for Helm tests |
| tests.hookDeletePolicy | string | `"before-hook-creation,hook-succeeded"` | Configure the hook-delete-policy for Helm tests |
| vault.azure.certificate | string | `nil` |  |
| vault.azure.client | string | `""` |  |
| vault.azure.name | string | `""` |  |
| vault.azure.secret | string | `nil` |  |
| vault.azure.tenant | string | `""` |  |
| vault.secretNames.transferProxyTokenEncryptionAesKey | string | `nil` |  |
| vault.secretNames.transferProxyTokenSignerPrivateKey | string | `nil` |  |
| vault.secretNames.transferProxyTokenSignerPublicKey | string | `nil` |  |

----------------------------------------------
Autogenerated from chart metadata using [helm-docs v1.11.2](https://github.com/norwoodj/helm-docs/releases/v1.11.2)
