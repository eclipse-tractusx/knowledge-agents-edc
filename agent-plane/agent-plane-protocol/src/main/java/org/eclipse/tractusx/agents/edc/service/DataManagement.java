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

import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.tractusx.agents.edc.model.Asset;
import org.eclipse.tractusx.agents.edc.model.ContractAgreement;
import org.eclipse.tractusx.agents.edc.model.ContractNegotiation;
import org.eclipse.tractusx.agents.edc.model.ContractNegotiationRequest;
import org.eclipse.tractusx.agents.edc.model.DcatCatalog;
import org.eclipse.tractusx.agents.edc.model.IdResponse;
import org.eclipse.tractusx.agents.edc.model.TransferProcess;
import org.eclipse.tractusx.agents.edc.model.TransferRequest;

import java.io.IOException;
import java.util.List;


/**
 * DataManagement
 * is a service wrapper around the management endpoint
 * of the EDC control plane
 */
public interface DataManagement {

    /**
     * Search for a dedicated asset
     *
     * @param remoteControlPlaneIdsUrl url of the remote control plane ids endpoint
     * @param assetId (connector-unique) identifier of the asset
     * @return a collection of contract options to access the given asset
     * @throws IOException in case that the remote call did not succeed
     */
    DcatCatalog findContractOffers(String remoteControlPlaneIdsUrl, String assetId) throws IOException; 

    /**
     * Access the catalogue
     *
     * @param remoteControlPlaneIdsUrl url of the remote control plane ids endpoint
     * @param spec query specification
     * @return catalog object
     * @throws IOException in case something went wrong
     */
    DcatCatalog getCatalog(String remoteControlPlaneIdsUrl, QuerySpec spec) throws IOException;

    /**
     * Access the (provider control plane) catalogue
     *
     * @param spec query specification
     * @return catalog object
     * @throws IOException in case something went wrong
     */
    List<Asset> listAssets(QuerySpec spec) throws IOException;

    /**
     * creates or updates a given skill asset
     *
     * @param assetId key
     * @param name of skill
     * @param description of skill
     * @param version of skill
     * @param contract of skill
     * @param ontologies of skill
     * @param distributionMode of skill
     * @param isFederated whether it should be distributed
     * @param query of skill
     * @return idresponse
     * @throws IOException in case interaction with EDC went wrong
     */
    IdResponse createOrUpdateSkill(String assetId, String name, String description, String version, String contract, String ontologies, String distributionMode, boolean isFederated, String query) throws IOException;

    /**
     * creates or updates a given graph asset
     *
     * @param assetId key
     * @param name of graph
     * @param description of graph
     * @param version of graph
     * @param contract of graph
     * @param ontologies of graph
     * @param shape of graph
     * @param isFederated whether it should be distributed
     * @return idresponse
     * @throws IOException in case interaction with EDC went wrong
     */
    IdResponse createOrUpdateGraph(String assetId, String name, String description, String version, String contract, String ontologies, String shape, boolean isFederated) throws IOException;

    /**
     * deletes an existing aseet
     *
     * @param assetId key of the asset
     * @return idresponse
     */

    IdResponse deleteAsset(String assetId) throws IOException;

    /**
     * initiates negotation
     *
     * @param negotiationRequest outgoing request
     * @return negotiation id
     * @throws IOException in case something went wronf
     */
    String initiateNegotiation(ContractNegotiationRequest negotiationRequest) throws IOException;

    /**
     * return state of contract negotiation
     *
     * @param negotiationId id of the negotation to inbestigate
     * @return status of the negotiation
     * @throws IOException in case something went wrong
     */
    ContractNegotiation getNegotiation(String negotiationId) throws IOException;

    /**
     * return the contract agreement
     *
     * @param agreementId id of the agreement
     * @return contract agreement
     * @throws IOException something wild happens
     */
    ContractAgreement getAgreement(String agreementId) throws IOException;

    /**
     * Initiates a transfer
     *
     * @param transferRequest request
     * @return transfer id
     * @throws IOException in case something went wrong
     */
    String initiateHttpProxyTransferProcess(TransferRequest transferRequest) throws IOException;

    /**
     * return state of transfer process
     *
     * @param transferProcessId id of the transfer process
     * @return state of the transfer process
     * @throws IOException in case something went wrong
     */
    TransferProcess getTransfer(String transferProcessId) throws IOException;

}