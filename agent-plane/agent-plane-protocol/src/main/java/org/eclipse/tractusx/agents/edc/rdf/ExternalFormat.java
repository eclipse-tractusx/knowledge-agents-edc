// Copyright (c) 2022,2024 Contributors to the Eclipse Foundation
//
// See the NOTICE file(s) distributed with this work for additional
// information regarding copyright ownership.
//
// This program and the accompanying materials are made available under the
// terms of the Apache License, Version 2.0 which is available at
// https://www.apache.org/licenses/LICENSE-2.0.
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
// WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
// License for the specific language governing permissions and limitations
// under the License.
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.tractusx.agents.edc.rdf;

/**
 * lists the various formats that the rdf store can import
 */
public enum ExternalFormat {
    TURTLE("text/turtle"),
    CSV("text/csv");

    private final String contentType;

    /**
     * Constructoe for ExternalFormat
     *
     * @param contentType the mime type
     */
    
    ExternalFormat(final String contentType) {
        this.contentType = contentType;
    }

    /**
     * get the content type
     *
     * @return mode as argument
     */
    
    public String getContentType() {
        return this.contentType;
    }

    /**
     * converts a mime type into a format
     *
     * @param contentType as argument
     * @return respective enum, null if format cannot be deduced
     */
    
    public static ExternalFormat valueOfFormat(String contentType) {
        if (contentType != null) {
            if (contentType.endsWith("turtle")) {
                return TURTLE;
            }
            if (contentType.endsWith("csv")) {
                return CSV;
            }
        }
        return null;
    }
}
