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

# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

# Released

## [1.9.5] - 2023-07-31

### Added

- Matchmaking Agent: Possibility to invoke Skills as Services
- Matchmaking Agent: Possibility to steer Delegation through Asset Properties
- Skill Store: Implementation using EDC Control Plane/Asset Catalogue

### Changed

- Adapted all Catena-X namespaces to https://w3id.org/catenax
- Adapted to Tractus-X EDC 0.4 and the v2 Management API
- Adapted to Tractus-X EDC 0.5 and the changed EDR callback
- Upgraded to the latest possible version of dependent libraries
- Eclipse Tractus-X standards and migration

### Removed

- Previous EDC Control Plane Extensions regarding SPARQL/HTTP transfer

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
