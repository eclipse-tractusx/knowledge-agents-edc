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


# Changelog

All notable changes to this product will be documented in this file.

# Released

## [1.11.16] - 2024-02-20

### Added

### Changed

- Bug fixes related to Federated Catalogue and Skill Provisioning

### Removed

## [1.10.15] - 2023-11-22

### Added

- Transfer: Possibility to download skill asset code
- Matchmaking Agent: Update of already registered Skill Assets
- Federated Catalogue: Support multiple offers of same asset
- Auth JWT: allow to exclude certain paths (e.g. liveness)

### Changed

- Adapted to Tractus-X EDC 0.5.3 and the new Asset Management v3
- Avoid double checking (allow/deny) of target service urls
- Possibility to run against older EDC versions
- Upgraded to the latest possible version of dependent libraries
- Introduce new configuration property compatible with environment variable standards

### Removed

- Cyclone DX BOMs (we have Dash)
- Deprecate some configuration property names as not being compatible with environment variable standards

## [1.9.8] - 2023-09-04

### Added

- Matchmaking Agent: Possibility to invoke Skills as Services according to KA-MATCH
- Matchmaking Agent: Possibility to steer Delegation through Asset Properties
- Matchmaking Agent: Possibility to allow/deny service requests based on URL pattern
- Transfer: Possibility to annotate assets with service request allow/deny patterns
- Transfer: Implement Skill Protocol of KA-TRANSFER
- Federated Data Catalogue: Embedding Shapes Properties as Named Graphs
- Skill Store: Implementation using EDC Control Plane/Asset Catalogue

### Changed

- Adapted all Catena-X namespaces to https://w3id.org/catenax
- Adapted to Tractus-X EDC 0.5 and the changed EDR callback
- Adapted to Tractus-X EDC 0.4 and the v2 Management and Catalogue APIs
- Upgraded to the latest possible version of dependent libraries
- Eclipse Tractus-X standards and migration

### Removed

- Previous EDC Control Plane Extensions regarding SPARQL/HTTP transfer

# Unreleased

## [Unreleased]

## [0.8.6] - 2023-05-19

### Added

- Support for SPARQL KA-transfer profile including the cx_warnings header

### Removed

- Registration of Additional Callback Handlers

### Changed

- Based on Tractus-X EDC 0.3.3

### Removed

## [0.7.4] - 2023-02-20

### Added

- Necessary documentation markdown for Eclipse Standard
- Helm Sub-Charts for Umbrella Embedding
- Postman Collection with Integration Tests
- Eclipse Dataspace Connector Extensions (Control Plane & Data Plane)

### Changed

- Move patched code into extensions
- Based on Tractus-X EDC 0.2.0

### Removed

## [0.6.4] - 2022-12-15

### Added

### Changed

- Based on a patched Tractus-X EDC 0.1.0

### Removed

## [0.5.5] - 2022-08-10

### Added

- Splitted Tractus-X Branch into Dataspace and UX submodules

### Changed

- Based on a patched Tractus-X EDC 0.0.1-SNAPSHOT

### Removed

- Tractus-X and Jena Links
- Spike Data

## [0.4.6] - 2022-05-13

### Added

- Submodules to Apache Jena and Tractus-X
- Based on Tractus-X EDC 0.0.1-SNAPSHOT
- Helm Chart and Docker Compose Deployment

### Changed

### Removed
