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
package org.eclipse.tractusx.agents.edc;

/**
 * enumerates the various skill distribution/run modes
 */
public enum SkillDistribution {
    CONSUMER("consumer"),
    PROVIDER("provider"),
    ALL("all");

    private final String mode;

    /**
     * access
     *
     * @param mode the textual mode
     */
    SkillDistribution(final String mode) {
        this.mode = mode;
    }

    /**
     * access
     *
     * @return mode a semantic value
     */
    public String getDistributionMode() {
        return "cx-common:SkillDistribution?run=" + this.mode;
    }

    /**
     * access
     *
     * @return mode as argument
     */
    public String getMode() {
        return this.mode;
    }

    /**
     * access
     *
     * @param mode as argument
     * @return respective enum (or ALL if it does not fir)
     */
    public static SkillDistribution valueOfMode(String mode) {
        if (mode != null) {
            if (mode.endsWith("consumer")) {
                return CONSUMER;
            } else if (mode.endsWith("provider")) {
                return PROVIDER;
            }
        }
        return ALL;
    }

}
