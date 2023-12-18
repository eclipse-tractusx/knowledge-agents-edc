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

import com.nimbusds.jose.JWSObject;
import jakarta.json.JsonValue;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.types.domain.edr.EndpointDataReference;
import org.eclipse.tractusx.agents.edc.jsonld.JsonLd;
import org.eclipse.tractusx.agents.edc.model.*;
import org.eclipse.tractusx.agents.edc.service.*;

import java.io.IOException;
import java.text.ParseException;
import java.util.*;


/**
 * An endpoint/service that receives information from the control plane
 */
public class AgreementController implements IAgreementController {

    /**
     * which transfer to use
     */
    public static String TRANSFER_TYPE="HttpProxy";

    /**
     * EDC service references
     */
    protected final Monitor monitor;
    protected final DataManagement dataManagement;
    protected final AgentConfig config;

    /**
     * memory store for links from assets to the actual transfer addresses
     * TODO make this a distributed cache
     * TODO let this cache evict invalidate references automatically
     */
    // hosts all pending processes
    protected final Set<String> activeAssets = new HashSet<>();
    // any contract agreements indexed by asset
    protected final Map<String, EndpointDataReference> agreementStore = new HashMap<>();

    /**
     * creates an agreement controller
     *
     * @param monitor        logger
     * @param config         typed config
     * @param dataManagement data management service wrapper
     */
    public AgreementController(Monitor monitor, AgentConfig config, DataManagement dataManagement) {
        this.monitor = monitor;
        this.dataManagement = dataManagement;
        this.config = config;
    }

    /**
     * render nicely
     */
    @Override
    public String toString() {
        return super.toString() + "/endpoint-data-reference";
    }

    /**
     * accesses an active endpoint for the given asset
     *
     * @param assetId id of the agreed asset
     * @return endpoint found, null if not found or invalid
     */
    @Override
    public EndpointDataReference get(String assetId) {
        synchronized (activeAssets) {
            if (!activeAssets.contains(assetId)) {
                monitor.debug(String.format("Asset %s is not active", assetId));
                return null;
            }
            synchronized (agreementStore) {
                EndpointDataReference result = agreementStore.get(assetId);
                if (result != null) {
                    String token = result.getAuthCode();
                    if (token != null) {
                        try {
                            JWSObject jwt = JWSObject.parse(token);
                            Object expiryObject=jwt.getPayload().toJSONObject().get("exp");
                            if(expiryObject instanceof Long) {
                                // token times are in seconds
                                if (!new Date((Long) expiryObject*1000).before(new Date(System.currentTimeMillis() + 30 * 1000))) {
                                    return result;
                                }
                            }
                        } catch (ParseException | NumberFormatException e) {
                            monitor.debug(String.format("Active asset %s has invalid agreement token.", assetId));
                        }
                    }
                    agreementStore.remove(assetId);
                }
                monitor.debug(String.format("Active asset %s has timed out or was not installed.", assetId));
                activeAssets.remove(assetId);
            }
        }
        return null;
    }

    /**
     * sets active
     * @param asset name
     */
    protected void activate(String asset) {
        synchronized (activeAssets) {
            if (activeAssets.contains(asset)) {
                throw new ClientErrorException("Cannot agree on an already active asset.", Response.Status.CONFLICT);
            }
            activeAssets.add(asset);
        }
    }

    /**
     * sets active
     * @param asset name
     */
    protected void deactivate(String asset) {
        synchronized (activeAssets) {
            activeAssets.remove(asset);
        }
        synchronized (agreementStore) {
            agreementStore.remove(asset);
        }
    }

    /**
     * register an agreement
     *
     * @param asset name
     * @param agreement object
     */
    protected EndpointDataReference registerAgreement(String asset, ContractAgreement agreement, Map<String, JsonValue> assetProperties) {
        synchronized (agreementStore) {
            var edrBuilder = EndpointDataReference.Builder.newInstance();
            edrBuilder.authCode(agreement.getAuthCode());
            edrBuilder.authKey(agreement.getAuthKey());
            edrBuilder.endpoint(agreement.getEndpoint());
            edrBuilder.id(agreement.getCId());
            var edr = edrBuilder.build();
            for (Map.Entry<String,JsonValue> prop : assetProperties.entrySet()) {
                edr.getProperties().put(prop.getKey(), JsonLd.asString(prop.getValue()));
            }
            agreementStore.put(asset, edr);
            return edr;
        }
    }

    /**
     * creates a new agreement (asynchronously)
     * and waits for the result
     *
     * @param remoteUrl ids endpoint url of the remote connector
     * @param asset     name of the asset to agree upon
     * TODO make this federation aware: multiple assets, different policies
     */
    @Override
    public EndpointDataReference createAgreement(String remoteUrl, String asset) throws WebApplicationException {
        monitor.debug(String.format("About to create an agreement for asset %s at connector %s",asset,remoteUrl));

        activate(asset);

        DcatCatalog contractOffers;

        try {
            contractOffers=dataManagement.findContractOffers(remoteUrl, asset);
        } catch(IOException io) {
            deactivate(asset);
            throw new InternalServerErrorException(String.format("Error when resolving contract offers from %s for asset %s through data management api.",remoteUrl,asset),io);
        }

        if (contractOffers.getDatasets().isEmpty()) {
            deactivate(asset);
            throw new BadRequestException(String.format("There is no contract offer in remote connector %s related to asset %s.", remoteUrl, asset));
        }

        // TODO implement a cost-based offer choice
        DcatDataset contractOffer = contractOffers.getDatasets().get(0);
        Map<String, JsonValue> assetProperties = DataspaceSynchronizer.getProperties(contractOffer);
        OdrlPolicy policy=contractOffer.hasPolicy();
        String offerId= policy.getId();
        JsonValue offerType=assetProperties.get("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
        monitor.debug(String.format("About to create an agreement for contract offer %s (for asset %s of type %s at connector %s)",offerId,asset,
                offerType,remoteUrl));

        var contractOfferDescription = new ContractOfferDescription(
                offerId,
                asset,
                policy
        );
        var contractNegotiationRequest = ContractNegotiationRequest.Builder.newInstance()
                .offerId(contractOfferDescription)
                .connectorId("provider")
                .connectorAddress(String.format(DataManagement.DSP_PATH, remoteUrl))
                .protocol("dataspace-protocol-http")
                .localBusinessPartnerNumber(config.getBusinessPartnerNumber())
                .remoteBusinessPartnerNumber(contractOffers.getParticipantId())
                .build();
        String negotiationId;

        try {
            negotiationId=dataManagement.initiateNegotiation(contractNegotiationRequest);
        } catch(IOException ioe) {
            deactivate(asset);
            throw new InternalServerErrorException(String.format("Error when initiating negotation for offer %s through data management api.",offerId),ioe);
        }

        monitor.debug(String.format("About to check negotiation %s for contract offer %s (for asset %s at connector %s)",negotiationId,offerId,asset,remoteUrl));

        // Check negotiation state
        ContractNegotiation negotiation = null;

        long startTime = System.currentTimeMillis();

        try {
            while ((System.currentTimeMillis() - startTime < config.getNegotiationTimeout())
                    && (negotiation == null ||
                    (!negotiation.getState().equals("NEGOTIATED") && !negotiation.getState().equals("TERMINATED")))) {
                Thread.sleep(config.getNegotiationPollInterval());
                negotiation = dataManagement.getNegotiation(
                        asset
                );
            }
        } catch (InterruptedException e) {
            monitor.info(String.format("Negotiation thread for asset %s negotiation %s has been interrupted. Giving up.", asset, negotiationId),e);
        } catch(IOException e) {
            monitor.warning(String.format("Negotiation thread for asset %s negotiation %s run into problem. Giving up.", asset, negotiationId),e);
        }

        if (negotiation == null || !negotiation.getState().equals("NEGOTIATED")) {
            deactivate(asset);
            if(negotiation!=null) {
                String errorDetail=negotiation.getErrorDetail();
                if(errorDetail!=null) {
                    monitor.severe(String.format("Contract Negotiation %s failed because of %s",negotiationId,errorDetail));
                }
            }
            throw new InternalServerErrorException(String.format("Contract Negotiation %s for asset %s was not successful.", negotiationId, asset));
        }

        monitor.debug(String.format("About to check edr %s for contract offer %s (for asset %s at connector %s)",negotiation.getContractAgreementId(),offerId,asset,remoteUrl));

        ContractAgreement agreement;

        try {
            agreement=dataManagement.getEdr(negotiation.getTransferProcessId());
        } catch(IOException ioe) {
            deactivate(asset);
            throw new InternalServerErrorException(String.format("Error when retrieving agreement %s for negotiation %s.",negotiation.getContractAgreementId(),negotiationId),ioe);
        }

        if (agreement == null) {
            deactivate(asset);
            throw new InternalServerErrorException(String.format("Agreement %s does not refer to asset %s.", negotiation.getContractAgreementId(), asset));
        }

        registerAgreement(asset,agreement, assetProperties);

        // now delegate to the original getter
        return get(asset);
    }

}
