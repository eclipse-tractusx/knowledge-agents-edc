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

import java.util.Optional;
import java.util.regex.Matcher;

/**
 * interface to a skill store
 */
public interface SkillStore {

    /**
     * match a given asset
     *
     * @param key asset name
     * @return matcher
     */
    static Matcher matchSkill(String key) {
        return AgentExtension.SKILL_PATTERN.matcher(key);
    }

    /**
     * check a given asset for being a skill
     *
     * @param key asset name
     * @return whether the asset encodes a skill
     */
    boolean isSkill(String key);

    /**
     * register a skill
     *
     * @param key         asset name required
     * @param skill       query text required
     * @param name        of skill optional
     * @param description of skill optional
     * @param version     of skill optional
     * @param contract    of skill optional
     * @param dist        of skill required
     * @param isFederated whether skill maybe synchronized in catalogue
     * @param allowServicePattern regex for service to call in skill
     * @param denyServicePattern regex for services denied in skill
     * @param ontologies  a set of ontologies
     * @return skill id
     */
    String put(String key, String skill, String name, String description, String version, String contract, SkillDistribution dist, boolean isFederated, String allowServicePattern, String denyServicePattern, String... ontologies);

    /**
     * return the skill distribution
     *
     * @param key asset name
     * @return skill distribution mode
     */
    SkillDistribution getDistribution(String key);

    /**
     * return the stored skill text
     *
     * @param key asset name
     * @return optional skill text if registered
     */
    Optional<String> get(String key);
}
