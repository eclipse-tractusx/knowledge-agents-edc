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

# Tractus-X Knowledge Agents Common EDC Extensions (KA-EDC-COMMON)

KA-EDC-COMMON is a module of the [Tractus-X Knowledge Agents EDC Extensions](../README.md).

## About this Module

This module hosts common extensions to the [Eclipse Dataspace Components (EDC)](https://github.com/eclipse-edc/Connector) which
may be used in any EDC plane/container for enabling a secure application/end user access to parts of the EDC infrastructure.

It consists of

- [JWT Based Authentication](auth-jwt)

## Getting Started

### Build

To compile and package the binary artifacts (includes running the unit tests)

```shell
mvn package 
```

To publish the binary artifacts (environment variables GITHUB_ACTOR and GITHUB_TOKEN must be set)

```shell
mvn -s ../settings.xml publish
```

### Integrate

If you want to integrate a component into your shading/packaging process, 
add the following dependency to your maven dependencies (gradle should work analogous)

```xml
<project>
    <dependencies>
        <dependency>
          <groupId>org.eclipse.tractusx.edc</groupId>
          <artifactId>auth-jwt</artifactId>
          <version>1.9.5-SNAPSHOT</version>
        </dependency>
    </dependencies>
</project>
```

If you want to use the pre-built binaries, you should also add the repository

```xml
<project>
    <repositories>
        <repository> 
            <id>ka-edc</id>
            <url>https://maven.pkg.github.com/eclipse-tractusx/knowledge-agents-edc</url>
        </repository>
    </repositories>
</project>
```

If you want to add the pre-built binaries directly into an exploded deployment/container, download
the library into your "lib/" folder.

For that purpose, visit [the package](https://github.com/eclipse-tractusx/knowledge-agents-edc/packages/1868799) and choose
the latest jar for downloading.
