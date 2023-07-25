//
// Copyright (C) 2022-2023 Catena-X Association and others. 
// 
// This program and the accompanying materials are made available under the
// terms of the Apache License 2.0 which is available at
// http://www.apache.org/licenses/.
//  
// SPDX-FileType: SOURCE
// SPDX-FileCopyrightText: 2022-2023 Catena-X Association
// SPDX-License-Identifier: Apache-2.0
//
package org.eclipse.tractusx.agents.edc.service;

import com.fasterxml.jackson.databind.node.TextNode;
import jakarta.json.Json;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.tractusx.agents.edc.AgentConfig;
import org.eclipse.tractusx.agents.edc.ISkillStore;
import org.eclipse.tractusx.agents.edc.SkillDistribution;
import org.eclipse.tractusx.agents.edc.jsonld.JsonLd;
import org.eclipse.tractusx.agents.edc.model.Asset;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Implements a skill store based on EDC assets
 */
public class EdcSkillStore implements ISkillStore {

    DataManagement management;
    TypeManager typeManager;
    AgentConfig config;

    public EdcSkillStore(DataManagement management, TypeManager typeManager, AgentConfig config) {
        this.management=management;
        this.typeManager=typeManager;
        this.config=config;
    }

    @Override
    public boolean isSkill(String key) {
        return ISkillStore.matchSkill(key).matches();
    }

    @Override
    public String put(String key, String skill, String name, String description, String version, String contract, SkillDistribution dist, boolean isFederated, String... ontologies) {
        if(name==null) {
            name="No name given";
        }
        if(description==null) {
            description="No description given";
        }
        if(version==null) {
            version="unknown version";
        }
        if(contract==null) {
            contract=config.getDefaultSkillContract();
        }
        String ontologiesString=String.join(",",ontologies);
        try {
            return management.createOrUpdateSkill(
                    key,
                    name,
                    description,
                    version,
                    contract,
                    ontologiesString,
                    dist.getDistributionMode(),
                    isFederated,
                    typeManager.getMapper().writeValueAsString(TextNode.valueOf(skill))
            ).getId();
        } catch (IOException e) {
           return null;
        }
    }

    @Override
    public SkillDistribution getDistribution(String key) {
        return findAsset(key).map( asset -> SkillDistribution.valueOfMode(JsonLd.asString(asset.getPublicProperties().
                get("https://w3id.org/catenax/ontology/common#distributionMode")))).orElse(SkillDistribution.ALL);
    }

    /** finds an asset */
    protected Optional<Asset> findAsset(String key) {
        QuerySpec findAsset=QuerySpec.Builder.newInstance().filter(
                List.of(new Criterion("https://w3id.org/edc/v0.0.1/ns/id","=",key),
                        new Criterion("http://www.w3.org/1999/02/22-rdf-syntax-ns#type","=","cx-common:SkillAsset"))).build();
        try {
            // we need to filter until the criterion really works
            return management.
                    listAssets(findAsset).stream().
                            findFirst();
        } catch(IOException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<String> get(String key) {
        return findAsset(key).map(asset->
                        JsonLd.asString(asset.getPrivateProperties().get("https://w3id.org/catenax/ontology/common#query"))
                    );
    }
}
