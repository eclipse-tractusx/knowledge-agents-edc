// Copyright (c) 2023 T-Systems International GmbH
// Copyright (c) 2023 Contributors to the Eclipse Foundation
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

import jakarta.ws.rs.core.Response;

/**
 * A wrapper around the response of a
 * call to another agent in the dataspace
 * Depending on the run mode (provider/consumer),
 * this response could optionally host the text of
 * a skill to be executed locally.
 */
public class DelegationResponse {
    protected final String queryString;
    protected final Response response;

    /**
     * Construct a new wrapper response for runMode = consumer
     *
     * @param queryString downloaded text of the skill
     * @param response    the response Object to return
     */
    public DelegationResponse(String queryString, Response response) {
        this.queryString = queryString;
        this.response = response;
    }

    /**
     * Construct a new wrapper response for runMode = provider
     *
     * @param response the response Object to return
     */
    public DelegationResponse(Response response) {
        this.response = response;
        this.queryString = null;
    }

    /**
     * access
     *
     * @return downloaded text of skill (should be not null if runMode = consumer)
     */
    public String getQueryString() {
        return queryString;
    }

    /**
     * access
     *
     * @return the response Object to return (should be not null in  each case)
     */
    public Response getResponse() {
        return response;
    }


}
