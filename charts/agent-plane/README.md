<!--
 * Copyright (c) 2024 T-Systems International GmbH
 * Copyright (c) 2024 Contributors to the Eclipse Foundation
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

# agent-plane

![Version: 1.13.21-SNAPSHOT](https://img.shields.io/badge/Version-1.12.19--SNAPSHOT-informational?style=flat-square) ![Type: application](https://img.shields.io/badge/Type-application-informational?style=flat-square) ![AppVersion: 1.13.21-SNAPSHOT](https://img.shields.io/badge/AppVersion-1.12.19--SNAPSHOT-informational?style=flat-square)

A Helm chart for an Agent-Enabled Tractus-X Data Plane which registers at a running
Control Plane.

This chart is intended for use with an _existing_ HashiCorp Vault and Tractusx Connector.

**Homepage:** <https://github.com/eclipse-tractusx/knowledge-agents-edc/>

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
helm install my-release eclipse-tractusx/agent-plane --version 1.13.21-SNAPSHOT
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
| https://charts.bitnami.com/bitnami | postgresql(postgresql) | 15.2.1 |
| https://helm.releases.hashicorp.com | vault(vault) | 0.27.0 |

## Values

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| affinity | object | `{}` |  |
| agent | object | `{"connectors":{},"default":["dataspace.ttl","https://w3id.org/catenax/ontology.ttl"],"maxbatchsize":"9223372036854775807","services":{"allow":"(edcs?://.*)|(https://query\\\\.wikidata\\\\.org/sparql)","asset":{"allow":"(edcs?://.*)","deny":"https?://.*"},"deny":"http://.*"},"skillcontract":"Contract?partner=Skill","synchronization":-1}` | Agent-Specific Settings |
| agent.connectors | object | `{}` | A map of partner ids to remote connector IDS URLs to synchronize with |
| agent.default | list | `["dataspace.ttl","https://w3id.org/catenax/ontology.ttl"]` | A list of local or remote graph descriptions to build the default meta-graph/federated data catalogue |
| agent.maxbatchsize | string | `"9223372036854775807"` | Sets the maximal batch size when delegating to agents and services |
| agent.services | object | `{"allow":"(edcs?://.*)|(https://query\\\\.wikidata\\\\.org/sparql)","asset":{"allow":"(edcs?://.*)","deny":"https?://.*"},"deny":"http://.*"}` | A set of configs for regulating outgoing service calls |
| agent.services.allow | string | `"(edcs?://.*)|(https://query\\\\.wikidata\\\\.org/sparql)"` | A regular expression which outgoing service URLs must match (unless overwritten by a specific asset property) |
| agent.services.asset | object | `{"allow":"(edcs?://.*)","deny":"https?://.*"}` | A set of configs for regulating outgoing service calls when providing an asset (when no specific asset property is given) |
| agent.services.asset.allow | string | `"(edcs?://.*)"` | A regular expression which outgoing service URLs must match (unless overwritten by a specific asset property) |
| agent.services.asset.deny | string | `"https?://.*"` | A regular expression which outgoing service URLs must not match (unless overwritten by a specific asset property) |
| agent.services.deny | string | `"http://.*"` | A regular expression which outgoing service URLs must not match (unless overwritten by a specific asset property) |
| agent.skillcontract | string | `"Contract?partner=Skill"` | Names the visible contract under which new skills are published (if not otherwise specified) |
| agent.synchronization | int | `-1` | The synchronization interval in ms to update the federated data catalogue |
| auth | object | `{"default":{"apiCode":"69609650","checkExpiry":true,"context":"default","exclude":".*/(check|validation).*","publicKey":null,"register":false,"type":"api-key","vaultKey":null}}` | Data Plane Authentication using the KA-EDC-AUTH-JWT extension, any entry has a type (api-key, jwt or composite) and a (set of) path contexts (see endpoints) followed by type-specific entries |
| auth.default | object | `{"apiCode":"69609650","checkExpiry":true,"context":"default","exclude":".*/(check|validation).*","publicKey":null,"register":false,"type":"api-key","vaultKey":null}` | the default authentication service |
| auth.default.apiCode | string | `"69609650"` | specific api-code associated to the default api-key 'Hello', Change this when type=api-key or use the vault-key property instead. Althugh this represents a number, remember to use quotes not to confuse rendering into the chart. |
| auth.default.checkExpiry | bool | `true` | controls whether the expiry date of jwt tokens is checked when type=jwt |
| auth.default.context | string | `"default"` | the context(s) of the default authentication service separated by commas |
| auth.default.exclude | string | `".*/(check|validation).*"` | excluded paths for liveness checks and validation |
| auth.default.publicKey | string | `nil` | public key for checking the validity of jwt tokens, set this when type=jwt |
| auth.default.register | bool | `false` | controls whether this service should be registered as the default EDC authentication service globally |
| auth.default.type | string | `"api-key"` | the type of the default authentication service (api-key, jwt or composite) |
| auth.default.vaultKey | string | `nil` | vault key for obtaining the API key, Set this when type=api-key or use the api-code property instead |
| autoscaling.enabled | bool | `false` | Enables [horizontal pod autoscaling](https://kubernetes.io/docs/tasks/run-application/horizontal-pod-autoscale/https://kubernetes.io/docs/tasks/run-application/horizontal-pod-autoscale/) |
| autoscaling.maxReplicas | int | `100` | Maximum replicas if resource consumption exceeds resource threshholds |
| autoscaling.minReplicas | int | `1` | Minimal replicas if resource consumption falls below resource threshholds |
| autoscaling.targetCPUUtilizationPercentage | int | `80` | targetAverageUtilization of cpu provided to a pod |
| autoscaling.targetMemoryUtilizationPercentage | int | `80` | targetAverageUtilization of memory provided to a pod |
| aws.accessKeyId | string | `""` |  |
| aws.endpointOverride | string | `""` |  |
| aws.secretAccessKey | string | `""` |  |
| configs | object | `{"dataspace.ttl":"#################################################################\n# Catena-X Agent Bootstrap Graph in TTL/RDF/OWL FORMAT\n#################################################################\n@prefix : <GraphAsset?local=Dataspace> .\n@base <GraphAsset?local=Dataspace> .\n"}` | A set of additional configuration files |
| configs."dataspace.ttl" | string | `"#################################################################\n# Catena-X Agent Bootstrap Graph in TTL/RDF/OWL FORMAT\n#################################################################\n@prefix : <GraphAsset?local=Dataspace> .\n@base <GraphAsset?local=Dataspace> .\n"` | An example of an empty graph in ttl syntax |
| connector | string | `""` | Name of the connector deployment |
| controlplane | object | `{"endpoints":{"control":{"path":"/control","port":8083},"management":{"authKey":"","path":"/management","port":8081},"protocol":{"path":"/api/v1/dsp","port":8084}},"ingresses":[{"enabled":false}]}` | References to the control plane deployment |
| controlplane.endpoints.control | object | `{"path":"/control","port":8083}` | control api, used for internal control calls. can be added to the internal ingress, but should probably not |
| controlplane.endpoints.control.path | string | `"/control"` | path for incoming api calls |
| controlplane.endpoints.control.port | int | `8083` | port for incoming api calls |
| controlplane.endpoints.management | object | `{"authKey":"","path":"/management","port":8081}` | data management api, used by internal users, can be added to an ingress and must not be internet facing |
| controlplane.endpoints.management.authKey | string | `""` | authentication key, must be attached to each 'X-Api-Key' request header |
| controlplane.endpoints.management.path | string | `"/management"` | path for incoming api calls |
| controlplane.endpoints.management.port | int | `8081` | port for incoming api calls |
| controlplane.endpoints.protocol | object | `{"path":"/api/v1/dsp","port":8084}` | dsp api, used for inter connector communication and must be internet facing |
| controlplane.endpoints.protocol.path | string | `"/api/v1/dsp"` | path for incoming api calls |
| controlplane.endpoints.protocol.port | int | `8084` | port for incoming api calls |
| customLabels | object | `{}` | To add some custom labels |
| debug.enabled | bool | `false` |  |
| debug.port | int | `1044` |  |
| debug.suspendOnStart | bool | `false` |  |
| destinationTypes | string | `"HttpProxy,AmazonS3"` | a comma-separated list of supported transfer types |
| endpoints.callback.path | string | `"/callback"` |  |
| endpoints.callback.port | int | `8087` |  |
| endpoints.default.path | string | `"/api"` |  |
| endpoints.default.port | int | `8080` |  |
| endpoints.public.path | string | `"/api/public"` |  |
| endpoints.public.port | int | `8081` |  |
| endpoints.signaling.path | string | `"/api/signaling"` |  |
| endpoints.signaling.port | int | `8083` |  |
| env | object | `{}` |  |
| envConfigMapNames | list | `[]` |  |
| envSecretNames | list | `[]` |  |
| envValueFrom | object | `{}` |  |
| fullnameOverride | string | `""` |  |
| iatp.id | string | `"did:web:changeme"` |  |
| iatp.sts.dim.url | string | `nil` |  |
| iatp.sts.oauth.client.id | string | `nil` |  |
| iatp.sts.oauth.client.secret_alias | string | `nil` |  |
| iatp.sts.oauth.token_url | string | `nil` |  |
| iatp.trustedIssuers | list | `[]` | Configures the trusted issuers for this runtime |
| image.pullPolicy | string | `"IfNotPresent"` | [Kubernetes image pull policy](https://kubernetes.io/docs/concepts/containers/images/#image-pull-policy) to use |
| image.repository | string | `""` | Which derivate of the data plane to use. when left empty the deployment will select the correct image automatically |
| image.tag | string | `""` | Overrides the image tag whose default is the chart appVersion |
| imagePullSecrets | list | `[]` | Existing image pull secret to use to [obtain the container image from private registries](https://kubernetes.io/docs/concepts/containers/images/#using-a-private-registry) |
| imageRegistry | string | `"docker.io/"` | Image registry to use |
| ingresses[0].annotations | string | `nil` | Additional ingress annotations to add, for example when supporting more demanding use cases you may set { nginx.org/proxy-connect-timeout: "30s", nginx.org/proxy-read-timeout: "360s", nginx.org/client-max-body-size: "10m"} |
| ingresses[0].certManager.clusterIssuer | string | `""` | If preset enables certificate generation via cert-manager cluster-wide issuer |
| ingresses[0].certManager.issuer | string | `""` | If preset enables certificate generation via cert-manager namespace scoped issuer |
| ingresses[0].className | string | `""` | Defines the [ingress class](https://kubernetes.io/docs/concepts/services-networking/ingress/#ingress-class)  to use |
| ingresses[0].enabled | bool | `false` |  |
| ingresses[0].endpoints | list | `["public"]` | EDC endpoints exposed by this ingress resource |
| ingresses[0].hostname | string | `"edc-data.local"` | The hostname to be used to precisely map incoming traffic onto the underlying network service |
| ingresses[0].tls | object | `{"enabled":false,"secretName":""}` | TLS [tls class](https://kubernetes.io/docs/concepts/services-networking/ingress/#tls) applied to the ingress resource |
| ingresses[0].tls.enabled | bool | `false` | Enables TLS on the ingress resource |
| ingresses[0].tls.secretName | string | `""` | If present overwrites the default secret name |
| initContainers | list | `[]` |  |
| install.postgresql | bool | `false` |  |
| install.vault | bool | `false` |  |
| limits.cpu | float | `1.5` |  |
| limits.memory | string | `"1024Mi"` |  |
| livenessProbe.enabled | bool | `true` | Whether to enable kubernetes [liveness-probe](https://kubernetes.io/docs/tasks/configure-pod-container/configure-liveness-readiness-startup-probes/) |
| livenessProbe.failureThreshold | int | `6` | when a probe fails kubernetes will try 6 times before giving up |
| livenessProbe.initialDelaySeconds | int | `30` | seconds to wait before performing the first liveness check |
| livenessProbe.periodSeconds | int | `10` | this fields specifies that kubernetes should perform a liveness check every 10 seconds |
| livenessProbe.successThreshold | int | `1` | number of consecutive successes for the probe to be considered successful after having failed |
| livenessProbe.timeoutSeconds | int | `5` | number of seconds after which the probe times out |
| logging | string | `".level=INFO\norg.eclipse.edc.level=ALL\nhandlers=java.util.logging.ConsoleHandler\njava.util.logging.ConsoleHandler.formatter=java.util.logging.SimpleFormatter\njava.util.logging.ConsoleHandler.level=ALL\njava.util.logging.SimpleFormatter.format=[%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS] [%4$-7s] %5$s%6$s%n"` | configuration of the [Java Util Logging Facade](https://docs.oracle.com/javase/7/docs/technotes/guides/logging/overview.html) |
| name | string | `"agentplane"` | the name of the dataplane |
| nameOverride | string | `""` |  |
| nodeSelector | object | `{}` |  |
| opentelemetry | string | `"otel.javaagent.enabled=false\notel.javaagent.debug=false"` | configuration of the [Open Telemetry Agent](https://opentelemetry.io/docs/instrumentation/java/automatic/agent-config/) to collect and expose metrics |
| participant.id | string | `""` | BPN Number |
| podAnnotations | object | `{}` | additional annotations for the pod |
| podLabels | object | `{}` | additional labels for the pod |
| podSecurityContext | object | `{"fsGroup":10001,"runAsGroup":10001,"runAsUser":10001,"seccompProfile":{"type":"RuntimeDefault"}}` | The [pod security context](https://kubernetes.io/docs/tasks/configure-pod-container/security-context/#set-the-security-context-for-a-pod) defines privilege and access control settings for a Pod within the deployment |
| podSecurityContext.fsGroup | int | `10001` | The owner for volumes and any files created within volumes will belong to this guid |
| podSecurityContext.runAsGroup | int | `10001` | Processes within a pod will belong to this guid |
| podSecurityContext.runAsUser | int | `10001` | Runs all processes within a pod with a special uid |
| podSecurityContext.seccompProfile.type | string | `"RuntimeDefault"` | Restrict a Container's Syscalls with seccomp |
| postgresql.auth.database | string | `"edc"` |  |
| postgresql.auth.password | string | `"password"` |  |
| postgresql.auth.username | string | `"user"` |  |
| postgresql.jdbcUrl | string | `"jdbc:postgresql://{{ .Release.Name }}-postgresql:5432/edc"` |  |
| postgresql.primary.persistence.enabled | bool | `false` |  |
| postgresql.readReplicas.persistence.enabled | bool | `false` |  |
| readinessProbe.enabled | bool | `true` | Whether to enable kubernetes [readiness-probes](https://kubernetes.io/docs/tasks/configure-pod-container/configure-liveness-readiness-startup-probes/) |
| readinessProbe.failureThreshold | int | `6` | when a probe fails kubernetes will try 6 times before giving up |
| readinessProbe.initialDelaySeconds | int | `30` | seconds to wait before performing the first readiness check |
| readinessProbe.periodSeconds | int | `10` | this fields specifies that kubernetes should perform a liveness check every 10 seconds |
| readinessProbe.successThreshold | int | `1` | number of consecutive successes for the probe to be considered successful after having failed |
| readinessProbe.timeoutSeconds | int | `5` | number of seconds after which the probe times out |
| replicaCount | int | `1` |  |
| requests.cpu | string | `"500m"` |  |
| requests.memory | string | `"128Mi"` |  |
| resources | object | `{}` | [resource management](https://kubernetes.io/docs/concepts/configuration/manage-resources-containers/) for the container |
| securityContext.allowPrivilegeEscalation | bool | `false` | Controls [Privilege Escalation](https://kubernetes.io/docs/concepts/security/pod-security-policy/#privilege-escalation) enabling setuid binaries changing the effective user ID |
| securityContext.capabilities.add | list | `[]` | Specifies which capabilities to add to issue specialized syscalls |
| securityContext.capabilities.drop | list | `["ALL"]` | Specifies which capabilities to drop to reduce syscall attack surface |
| securityContext.readOnlyRootFilesystem | bool | `true` | Whether the root filesystem is mounted in read-only mode |
| securityContext.runAsNonRoot | bool | `true` | Requires the container to run without root privileges |
| securityContext.runAsUser | int | `10001` | The container's process will run with the specified uid |
| service.port | int | `80` |  |
| service.type | string | `"ClusterIP"` | [Service type](https://kubernetes.io/docs/concepts/services-networking/service/#publishing-services-service-types) to expose the running application on a set of Pods as a network service. |
| serviceAccount.annotations | object | `{}` |  |
| serviceAccount.create | bool | `true` |  |
| serviceAccount.imagePullSecrets | list | `[]` | Existing image pull secret bound to the service account to use to [obtain the container image from private registries](https://kubernetes.io/docs/concepts/containers/images/#using-a-private-registry) |
| serviceAccount.name | string | `""` |  |
| sourceTypes | string | `"cx-common:Protocol?w3c:http:SPARQL,cx-common:Protocol?w3c:http:SKILL,HttpData,AmazonS3"` | a comma-separated list of supported asset types |
| tests | object | `{"hookDeletePolicy":"before-hook-creation,hook-succeeded"}` | Configurations for Helm tests |
| tests.hookDeletePolicy | string | `"before-hook-creation,hook-succeeded"` | Configure the hook-delete-policy for Helm tests |
| token.refresh.expiry_seconds | int | `300` |  |
| token.refresh.expiry_tolerance_seconds | int | `10` |  |
| token.refresh.refresh_endpoint | string | `nil` |  |
| token.signer.privatekey_alias | string | `nil` |  |
| token.verifier.publickey_alias | string | `nil` |  |
| tolerations | list | `[]` |  |
| url.public | string | `""` | Explicitly declared url for reaching the public api (e.g. if ingresses not used) |
| vault | object | `{"hashicorp":{"healthCheck":{"enabled":true,"standbyOk":true},"paths":{"health":"/v1/sys/health","secret":"/v1/secret"},"timeout":30,"token":"","url":"http://{{ .Release.Name }}-vault:8200"},"injector":{"enabled":false},"secretNames":{"transferProxyTokenEncryptionAesKey":null,"transferProxyTokenSignerPrivateKey":null,"transferProxyTokenSignerPublicKey":null},"server":{"dev":{"devRootToken":"root","enabled":true},"postStart":null}}` | Standard settings for persistence, "jdbcUrl", "username" and "password" need to be overridden |
| vault.hashicorp.paths.health | string | `"/v1/sys/health"` | Default health api |
| vault.hashicorp.paths.secret | string | `"/v1/secret"` | Path to secrets needs to be changed if install.vault=false |
| vault.hashicorp.token | string | `""` | Access token to the vault service needs to be changed if install.vault=false |
| vault.hashicorp.url | string | `"http://{{ .Release.Name }}-vault:8200"` | URL to the vault service, needs to be changed if install.vault=false |
| vault.secretNames.transferProxyTokenEncryptionAesKey | string | `nil` | encrypt handed out tokens with this symmetric key |
| vault.secretNames.transferProxyTokenSignerPrivateKey | string | `nil` | sign handed out tokens with this key |
| vault.secretNames.transferProxyTokenSignerPublicKey | string | `nil` | sign handed out tokens with this certificate |
| volumeMounts | list | `[]` | declare where to mount [volumes](https://kubernetes.io/docs/concepts/storage/volumes/) into the container |
| volumes | list | `[]` | [volume](https://kubernetes.io/docs/concepts/storage/volumes/) directories |

----------------------------------------------
Autogenerated from chart metadata using [helm-docs v1.11.2](https://github.com/norwoodj/helm-docs/releases/v1.11.2)
