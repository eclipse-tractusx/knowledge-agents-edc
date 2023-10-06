// Copyright (c) 2022,2023 Contributors to the Eclipse Foundation
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
package org.eclipse.tractusx.agents.edc.http;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

/**
 * interface to a service that may
 * delegate agent http calls into the
 * dataspace
 */
public interface IDelegationService {
    /**
     * delegate the given call into the dataspace
     * @param remoteUrl target EDC
     * @param skill name of the remote skill (may be empty, then graph must be set)
     * @param graph name of the remote graph (may be empty, then skill must be set)
     * @param headers url call headers
     * @param request url request
     * @param response final response
     * @param uri original uri
     * @return an intermediate response which may contain a textual skill to be executed locally otherwise the actual result has already been put into the final response's state
     */
    DelegationResponse executeQueryRemote(String remoteUrl, String skill, String graph, HttpHeaders headers, HttpServletRequest request, HttpServletResponse response, UriInfo uri);
}
