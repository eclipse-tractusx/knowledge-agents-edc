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
package org.eclipse.tractusx.agents.edc.service;

import org.eclipse.tractusx.agents.edc.SkillDistribution;
import org.eclipse.tractusx.agents.edc.SkillStore;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * An in-memory store for local skills
 */
public class InMemorySkillStore implements SkillStore {

    // temporary local skill store
    protected final Map<String, String> skills = new HashMap<>();

    /**
     * create the store
     */
    public InMemorySkillStore() {
    }

    @Override
    public boolean isSkill(String key) {
        return SkillStore.matchSkill(key).matches();
    }

    @Override
    public String put(String key, String skill, String name, String description, String version, String contract, SkillDistribution dist, boolean isFederated, String allowServicePattern, String denyServicePattern, String... ontologies) {
        skills.put(key, skill);
        return key;
    }

    @Override
    public SkillDistribution getDistribution(String key) {
        return SkillDistribution.ALL;
    }

    @Override
    public Optional<String> get(String key) {
        if (!skills.containsKey(key)) {
            return Optional.empty();
        } else {
            return Optional.of(skills.get(key));
        }
    }
}
