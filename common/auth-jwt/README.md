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

# Tractus-X Knowledge Agents (Hey Catena!) EDC JWT Auth Extension

This folder hosts an authentication extension to the [Eclipse Dataspace Connector (EDC)](https://projects.eclipse.org/projects/technology.dataspaceconnector).

It allows to configure and build composite/stacked authentication services including the validation of JWT tokens versus
public keys.

It allows to install authentication filters backed by those authentication services to
various web service contexts.

## How to configure this extension

The following is a list of configuration objects and properties that you might set in the corresponding mounted config files

| SETTING                                         | Required | Default/Example                                                | Description                                                                                                                             | 
|-------------------------------------------------|----------|----------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------|
| tractusx.auth.<name>.type                       | no       | jwt                                        |  Introduces a new authentication filter                                                                          |   
| tractusx.auth.<name>.publickey                  | yes, if jwt       |  https://keycloak.instance/auth/realms/REALM/protocol/openid-connect/certs                                      |  download url  for public cert of REALM                                                                       |   
| tractusx.auth.<name>.register                   | no      |  true                                      |   Whether the filter should be registered in the EDC list                                                                     |   
| tractusx.auth.<name>.checkexpiry                | no, if jwt       |  true                                      |   Whether tokens should be checked for expiry                                                                     |   
| tractusx.auth.<name>.paths                | no, if jwt       |  default                                      |   A list of paths in the token claims which should be checked upon existance                                                                    |   

