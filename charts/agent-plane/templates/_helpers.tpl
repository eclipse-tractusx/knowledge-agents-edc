#
#  Copyright (c) 2023,2024 T-Systems International GmbH
#  Copyright (c) 2023 ZF Friedrichshafen AG
#  Copyright (c) 2023 Mercedes-Benz Tech Innovation GmbH
#  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
#  Copyright (c) 2021,2024 Contributors to the Eclipse Foundation
#
#  See the NOTICE file(s) distributed with this work for additional
#  information regarding copyright ownership.
#
#  This program and the accompanying materials are made available under the
#  terms of the Apache License, Version 2.0 which is available at
#  https://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
#  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
#  License for the specific language governing permissions and limitations
#  under the License.
#
#  SPDX-License-Identifier: Apache-2.0
#
{{/*
Expand the name of the chart.
*/}}
{{- define "txap.name" -}}
{{- default .Chart.Name .Values.nameOverride | replace "+" "_"  | trunc 63 | trimSuffix "-" -}}
{{- end }}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "txap.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $name := default .Chart.Name .Values.nameOverride }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}
{{- end }}

{{/*
Create a default fully qualified app name for the connector.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "txap.connector.fullname" -}}
{{- if .Values.connector }}
{{- printf "%s-%s" .Release.Name .Values.connector | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s" .Release.Name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "txap.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Control Common labels
*/}}
{{- define "txap.labels" -}}
helm.sh/chart: {{ include "txap.chart" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Data Common labels (Expects the Chart Root to be accessible via .root, the current dataplane via .dataplane)
*/}}
{{- define "txap.dataplane.labels" -}}
helm.sh/chart: {{ include "txap.chart" . }}
{{ include "txap.dataplane.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
app.kubernetes.io/component: edc-dataplane
app.kubernetes.io/part-of: edc
{{- end }}

{{/*
Data Selector labels (Expects the Chart Root to be accessible via .root, the current dataplane via .dataplane)
*/}}
{{- define "txap.dataplane.selectorLabels" -}}
app.kubernetes.io/name: {{ include "txap.name" . }}-{{ .Values.name }}
app.kubernetes.io/instance: {{ .Release.Name }}-{{ .Values.name }}
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "txap.dataplane.serviceaccount.name" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "txap.fullname" . ) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}

{{/*
Control DSP URL
*/}}
{{- define "txap.controlplane.url.protocol" -}}
{{- if (and .Values.controlplane.url .Values.controlplane.url.protocol) }}{{/* if dsp api url has been specified explicitly */}}
{{- .Values.controlplane.url.protocol }}
{{- else }}{{/* else when dsp api url has not been specified explicitly */}}
{{- with (index .Values.controlplane.ingresses 0) }}
{{- if .enabled }}{{/* if ingress enabled */}}
{{- if .tls.enabled }}{{/* if TLS enabled */}}
{{- printf "https://%s" .hostname -}}
{{- else }}{{/* else when TLS not enabled */}}
{{- printf "http://%s" .hostname -}}
{{- end }}{{/* end if tls */}}
{{- else }}{{/* else when ingress not enabled */}}
{{- printf "http://%s-controlplane:%v" ( include "txap.connector.fullname" $ ) $.Values.controlplane.endpoints.protocol.port -}}
{{- end }}{{/* end if ingress */}}
{{- end }}{{/* end with ingress */}}
{{- end }}{{/* end if .Values.controlplane.url.protocol */}}
{{- end }}

{{/*
Validation URL
*/}}
{{- define "txap.controlplane.url.control" -}}
{{- printf "http://%s-controlplane:%v%s" ( include "txap.connector.fullname" $ ) .Values.controlplane.endpoints.control.port .Values.controlplane.endpoints.control.path -}}
{{- end }}

{{/*
Validation URL
*/}}
{{- define "txap.controlplane.url.management" -}}
{{- printf "http://%s-controlplane:%v%s" ( include "txap.connector.fullname" $ ) .Values.controlplane.endpoints.management.port .Values.controlplane.endpoints.management.path -}}
{{- end }}

{{/*
Data Control URL (Expects the Chart Root to be accessible via .root, the current dataplane via .dataplane)
*/}}
{{- define "txap.dataplane.url.signaling" -}}
{{- printf "http://%s-%s:%v%s" (include "txap.fullname" . ) .Values.name .Values.endpoints.signaling.port .Values.endpoints.signaling.path -}}
{{- end }}

{{/*
Data Control URL (Expects the Chart Root to be accessible via .root, the current dataplane via .dataplane)
*/}}
{{- define "txap.dataplane.url.callback" -}}
{{- printf "http://%s-%s:%v%s" (include "txap.fullname" . ) .Values.name .Values.endpoints.callback.port .Values.endpoints.callback.path -}}
{{- end }}

{{/*
Data Public URL
*/}}
{{- define "txap.dataplane.url.public" -}}
{{- if .Values.url.public }}{{/* if public api url has been specified explicitly */}}
{{- .Values.url.public }}
{{- else }}{{/* else when public api url has not been specified explicitly */}}
{{- with (index  .Values.ingresses 0) }}
{{- if .enabled }}{{/* if ingress enabled */}}
{{- if .tls.enabled }}{{/* if TLS enabled */}}
{{- printf "https://%s%s" .hostname $.Values.endpoints.public.path -}}
{{- else }}{{/* else when TLS not enabled */}}
{{- printf "http://%s%s" .hostname $.Values.endpoints.public.path -}}
{{- end }}{{/* end if tls */}}
{{- else }}{{/* else when ingress not enabled */}}
{{- printf "http://%s-%s:%v%s" (include "txap.fullname" $ ) $.Values.name $.Values.endpoints.public.port $.Values.endpoints.public.path -}}
{{- end }}{{/* end if ingress */}}
{{- end }}{{/* end with ingress */}}
{{- end }}{{/* end if .Values.url.public */}}
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "txap.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "txap.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}

{{/*
join a map
*/}}
{{- define "txap.remotes" -}}
{{- $res := dict "servers" (list) -}}
{{- range $bpn, $connector := .Values.agent.connectors -}}
{{- $noop := printf "%s=%s" $bpn $connector | append $res.servers | set $res "servers" -}}
{{- end -}}
{{- join "," $res.servers }}
{{- end -}}
