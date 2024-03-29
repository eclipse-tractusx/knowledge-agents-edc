#
#  Copyright (c) 2023 T-Systems International GmbH
#  Copyright (c) 2023 ZF Friedrichshafen AG
#  Copyright (c) 2023 Mercedes-Benz Tech Innovation GmbH
#  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
#  Copyright (c) 2021, 2023 Contributors to the Eclipse Foundation
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
{{- define "txdc.name" -}}
{{- default .Chart.Name .Values.nameOverride | replace "+" "_"  | trunc 63 | trimSuffix "-" -}}
{{- end }}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "txdc.fullname" -}}
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
Create chart name and version as used by the chart label.
*/}}
{{- define "txdc.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Control Common labels
*/}}
{{- define "txdc.labels" -}}
helm.sh/chart: {{ include "txdc.chart" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Control Common labels
*/}}
{{- define "txdc.controlplane.labels" -}}
helm.sh/chart: {{ include "txdc.chart" . }}
{{ include "txdc.controlplane.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
app.kubernetes.io/component: edc-controlplane
app.kubernetes.io/part-of: edc
{{- end }}

{{/*
Data Common labels (Expects the Chart Root to be accessible via .root, the current dataplane via .dataplane)
*/}}
{{- define "txdc.dataplane.labels" -}}
helm.sh/chart: {{ include "txdc.chart" .root }}
{{ include "txdc.dataplane.selectorLabels" . }}
{{- if .root.Chart.AppVersion }}
app.kubernetes.io/version: {{ .root.Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .root.Release.Service }}
app.kubernetes.io/component: edc-dataplane
app.kubernetes.io/part-of: edc
{{- end }}

{{/*
Control Selector labels
*/}}
{{- define "txdc.controlplane.selectorLabels" -}}
app.kubernetes.io/name: {{ include "txdc.name" . }}-controlplane
app.kubernetes.io/instance: {{ .Release.Name }}-controlplane
{{- end }}

{{/*
Data Selector labels (Expects the Chart Root to be accessible via .root, the current dataplane via .dataplane)
*/}}
{{- define "txdc.dataplane.selectorLabels" -}}
app.kubernetes.io/name: {{ include "txdc.name" .root }}-{{ .dataplane.name }}
app.kubernetes.io/instance: {{ .root.Release.Name }}-{{ .dataplane.name }}
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "txdc.controlplane.serviceaccount.name" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "txdc.fullname" . ) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "txdc.dataplane.serviceaccount.name" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "txdc.fullname" . ) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}

{{/*
Control DSP URL
*/}}
{{- define "txdc.controlplane.url.protocol" -}}
{{- if .Values.controlplane.url.protocol }}{{/* if dsp api url has been specified explicitly */}}
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
{{- printf "http://%s-controlplane:%v" ( include "txdc.fullname" $ ) $.Values.controlplane.endpoints.protocol.port -}}
{{- end }}{{/* end if ingress */}}
{{- end }}{{/* end with ingress */}}
{{- end }}{{/* end if .Values.controlplane.url.protocol */}}
{{- end }}

{{/*
Validation URL
*/}}
{{- define "txdc.controlplane.url.validation" -}}
{{- printf "http://%s-controlplane:%v%s/token" ( include "txdc.fullname" $ ) $.Values.controlplane.endpoints.control.port $.Values.controlplane.endpoints.control.path -}}
{{- end }}

{{/*
Data Control URL (Expects the Chart Root to be accessible via .root, the current dataplane via .dataplane)
*/}}
{{- define "txdc.dataplane.url.control" -}}
{{- printf "http://%s-%s:%v%s" (include "txdc.fullname" .root ) .dataplane.name .dataplane.endpoints.control.port .dataplane.endpoints.control.path -}}
{{- end }}

{{/*
Data Public URL (Expects the Chart Root to be accessible via .root, the current dataplane via .dataplane)
*/}}
{{- define "txdc.dataplane.url.public" -}}
{{- $dataplane := .dataplane -}}
{{- $root := .root -}}
{{- if .dataplane.url.public }}{{/* if public api url has been specified explicitly */}}
{{- .dataplane.url.public }}
{{- else }}{{/* else when public api url has not been specified explicitly */}}
{{- with (index .dataplane.ingresses 0) }}
{{- if .enabled }}{{/* if ingress enabled */}}
{{- if .tls.enabled }}{{/* if TLS enabled */}}
{{- printf "https://%s%s" .hostname $dataplane.endpoints.public.path -}}
{{- else }}{{/* else when TLS not enabled */}}
{{- printf "http://%s%s" .hostname $dataplane.endpoints.public.path -}}
{{- end }}{{/* end if tls */}}
{{- else }}{{/* else when ingress not enabled */}}
{{- printf "http://%s-%s:%v%s" (include "txdc.fullname" $root ) $dataplane.name $dataplane.endpoints.public.port $dataplane.endpoints.public.path -}}
{{- end }}{{/* end if ingress */}}
{{- end }}{{/* end with ingress */}}
{{- end }}{{/* end if .url.public */}}
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "txdc.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "txdc.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}
