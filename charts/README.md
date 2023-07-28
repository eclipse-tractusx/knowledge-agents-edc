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

# Helm Charts

## Chart Linting

Chart linting is performed using [helm's CT tool](https://github.com/helm/chart-testing).

Configuration files for [CT](config/chart-testing-config.yaml) have been provided.

## Generate Chart Readme's

To generate chart README.md files from its respective values.yaml file we use the [helm-docs tool](https://github.com/norwoodj/helm-docs):

```shell
docker run --rm --volume "$(pwd):/helm-docs" -u $(id -u) jnorwood/helm-docs:v1.10.0
```

## Confidential Settings

Some settings should better not be part of the actual deployment (like credentials to the database or the vault). Therefore, it is possible to deploy a secret with these confidential settings beforehand, and make it known to the deployment by setting the secret name in the `envSecretName` field of the respective deployments.
