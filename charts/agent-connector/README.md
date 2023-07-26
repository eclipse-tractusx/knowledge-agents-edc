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

# agent-connector

![Version: 1.9.5-SNAPSHOT](https://img.shields.io/badge/Version-1.9.5--SNAPSHOT-informational?style=flat-square) ![Type: application](https://img.shields.io/badge/Type-application-informational?style=flat-square) ![AppVersion: 1.9.5-SNAPSHOT](https://img.shields.io/badge/AppVersion-1.9.5--SNAPSHOT-informational?style=flat-square)

A Helm chart for an Agent-Enabled Tractus-X Eclipse Data Space Connector. The connector deployment consists of two runtime consists of a
Control Plane and a Data Plane. Note that _no_ external dependencies such as a PostgreSQL database and HashiCorp Vault are included.

This chart is intended for use with an _existing_ PostgreSQL database and an _existing_ HashiCorp Vault.

**Homepage:** <https://github.com/eclipse-tractusx/knowledge-agents-edc/tree/main/charts/agent-connector>

## Setting up SSI

### Preconditions

- the [Managed Identity Walled (MIW)](https://github.com/catenax-ng/tx-managed-identity-wallets) must be running and reachable via network
- the necessary set of VerifiableCredentials for this participant must be pushed to MIW. This is typically done by the
  Portal during participant onboarding
- KeyCloak must be running and reachable via network
- an account with KeyCloak must be created for this BPN and the connector must be able to obtain access tokens
- the client ID and client secret corresponding to that account must be known

### Preparatory work

- store your KeyCloak client secret in the HashiCorp vault. The exact procedure will depend on your deployment of HashiCorp Vault and
  is out of scope of this document. But by default, Tractus-X EDC expects to find the secret under `secret/client-secret`.

### Configure the chart

Be sure to provide the following configuration entries to your Tractus-X EDC Helm chart:
- `controlplane.ssi.miw.url`: the URL
- `controlplane.ssi.miw.authorityId`: the BPN of the issuer authority
- `controlplane.ssi.oauth.tokenurl`: the URL (of KeyCloak), where access tokens can be obtained
- `controlplane.ssi.oauth.client.id`: client ID for KeyCloak
- `controlplane.ssi.oauth.client.secretAlias`: the alias under which the client secret is stored in the vault. Defaults to `client-secret`.

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
helm repo add product-knowledge https://catenax-ng.github.io/product-knowledge/infrastructure
helm install my-release product-knowledge/agent-connector --version 1.9.5-SNAPSHOT
```

## Source Code

* <https://github.com/eclipse-tractusx/knowledge-agents-edc/tree/main/charts/agent-connector>

## Requirements

| Repository | Name | Version |
|------------|------|---------|
| https://charts.bitnami.com/bitnami | postgresql(postgresql) | 12.1.6 |
| https://helm.releases.hashicorp.com | vault(vault) | 0.20.0 |

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
| controlplane.ssi.oauth.client.secretAlias | string | `"client-secret"` | The alias under which the client secret is stored in the vault. |
| controlplane.ssi.oauth.tokenurl | string | `""` | The URL (of KeyCloak), where access tokens can be obtained |
| controlplane.tolerations | list | `[]` |  |
| controlplane.url.protocol | string | `""` | Explicitly declared url for reaching the dsp api (e.g. if ingresses not used) |
| controlplane.volumeMounts | list | `[]` | declare where to mount [volumes](https://kubernetes.io/docs/concepts/storage/volumes/) into the container |
| controlplane.volumes | list | `[]` | [volume](https://kubernetes.io/docs/concepts/storage/volumes/) directories |
| customLabels | object | `{}` | To add some custom labels |
| dataplanes.dataplane.affinity | object | `{}` |  |
| dataplanes.dataplane.agent | object | `{"connectors":[],"default":["dataspace.ttl","https://raw.githubusercontent.com/catenax-ng/product-ontology/main/ontology.ttl"],"maxbatchsize":"9223372036854775807","skillcontract":"Contract?partner=Skill","synchronization":-1}` | Agent-Specific Settings |
| dataplanes.dataplane.agent.connectors | list | `[]` | The list of remote connector IDS URLs to synchronize with |
| dataplanes.dataplane.agent.default | list | `["dataspace.ttl","https://raw.githubusercontent.com/catenax-ng/product-ontology/main/ontology.ttl"]` | A list of local or remote graph descriptions to build the default meta-graph/federated data catalogue |
| dataplanes.dataplane.agent.maxbatchsize | string | `"9223372036854775807"` | Sets the maximal batch size when delegating to agents and services |
| dataplanes.dataplane.agent.skillcontract | string | `"Contract?partner=Skill"` | Names the visible contract under which new skills are published (if not otherwise specified) |
| dataplanes.dataplane.agent.synchronization | int | `-1` | The synchronization interval in ms to update the federated data catalogue |
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
| dataplanes.dataplane.ingresses[0].annotations | object | `{}` | Additional ingress annotations to add |
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
| install.postgresql | bool | `false` |  |
| install.vault | bool | `false` |  |
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
| vault.hashicorp.healthCheck.enabled | bool | `true` |  |
| vault.hashicorp.healthCheck.standbyOk | bool | `true` |  |
| vault.hashicorp.paths.health | string | `"/v1/sys/health"` |  |
| vault.hashicorp.paths.secret | string | `"/v1/secret"` |  |
| vault.hashicorp.timeout | int | `30` |  |
| vault.hashicorp.token | string | `""` |  |
| vault.hashicorp.url | string | `"http://{{ .Release.Name }}-vault:8200"` |  |
| vault.injector.enabled | bool | `false` |  |
| vault.secretNames.transferProxyTokenEncryptionAesKey | string | `"transfer-proxy-token-encryption-aes-key"` |  |
| vault.secretNames.transferProxyTokenSignerPrivateKey | string | `nil` |  |
| vault.secretNames.transferProxyTokenSignerPublicKey | string | `nil` |  |
| vault.server.dev.devRootToken | string | `"root"` |  |
| vault.server.dev.enabled | bool | `true` |  |
| vault.server.postStart | string | `nil` |  |

----------------------------------------------
Autogenerated from chart metadata using [helm-docs v1.11.0](https://github.com/norwoodj/helm-docs/releases/v1.11.0)
