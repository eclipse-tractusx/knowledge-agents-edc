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
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.callback.CallbackAddress;
import org.eclipse.edc.spi.types.domain.edr.EndpointDataReference;
import org.eclipse.tractusx.agents.edc.jsonld.JsonLd;
import org.eclipse.tractusx.agents.edc.model.ContractAgreement;
import org.eclipse.tractusx.agents.edc.model.ContractNegotiation;
import org.eclipse.tractusx.agents.edc.model.ContractNegotiationRequest;
import org.eclipse.tractusx.agents.edc.model.ContractOfferDescription;
import org.eclipse.tractusx.agents.edc.model.DcatCatalog;
import org.eclipse.tractusx.agents.edc.model.DcatDataset;
import org.eclipse.tractusx.agents.edc.model.OdrlPolicy;
import org.eclipse.tractusx.agents.edc.model.TransferProcess;
import org.eclipse.tractusx.agents.edc.model.TransferRequest;
import org.eclipse.tractusx.agents.edc.service.DataManagement;
import org.eclipse.tractusx.agents.edc.service.DataManagementImpl;
import org.eclipse.tractusx.agents.edc.service.DataspaceSynchronizer;

import java.io.IOException;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * An endpoint/service that receives information from the control plane
 */
@Consumes({MediaType.APPLICATION_JSON})
@Path("/endpoint-data-reference")
public class AgreementControllerImpl implements AgreementController {

    /**
     * which transfer to use
     */
    public static final String TRANSFER_TYPE = "HttpProxy";

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
    protected final Map<String, ContractAgreement> agreementStore = new HashMap<>();
    // any transfer processes indexed by asset, the current process should
    // always adhere to the above agreement
    protected final Map<String, TransferProcess> processStore = new HashMap<>();
    // at the end of provisioning and endpoint reference will be set
    // that fits to the current transfer process
    protected final Map<String, EndpointDataReference> endpointStore = new HashMap<>();

    /**
     * creates an agreement controller
     *
     * @param monitor        logger
     * @param config         typed config
     * @param dataManagement data management service wrapper
     */
    public AgreementControllerImpl(Monitor monitor, AgentConfig config, DataManagement dataManagement) {
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
     * this is called by the control plane when an agreement has been made
     *
     * @param dataReference contains the actual call token
     */
    @POST
    public void receiveEdcCallback(EndpointDataReference dataReference) {
        var agreementId = dataReference.getId();
        monitor.debug(String.format("An endpoint data reference for agreement %s has been posted.", agreementId));
        synchronized (agreementStore) {
            for (Map.Entry<String, TransferProcess> process : processStore.entrySet()) {
                if (process.getValue().getId().equals(agreementId)) {
                    synchronized (endpointStore) {
                        monitor.debug(String.format("Agreement %s belongs to asset %s.", agreementId, process.getKey()));
                        endpointStore.put(process.getKey(), dataReference);
                        return;
                    }
                }
            }
        }
        monitor.debug(String.format("Agreement %s has no active asset. Guess that came for another plane. Ignoring.", agreementId));
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
            synchronized (endpointStore) {
                EndpointDataReference result = endpointStore.get(assetId);
                if (result != null) {
                    String token = result.getAuthCode();
                    if (token != null) {
                        try {
                            JWSObject jwt = JWSObject.parse(token);
                            Object expiryObject = jwt.getPayload().toJSONObject().get("exp");
                            if (expiryObject instanceof Long) {
                                // token times are in seconds
                                if (!new Date((Long) expiryObject * 1000).before(new Date(System.currentTimeMillis() + 30 * 1000))) {
                                    return result;
                                }
                            }
                        } catch (ParseException | NumberFormatException e) {
                            monitor.debug(String.format("Active asset %s has invalid agreement token.", assetId));
                        }
                    }
                    endpointStore.remove(assetId);
                }
                monitor.debug(String.format("Active asset %s has timed out or was not installed.", assetId));
                synchronized (processStore) {
                    processStore.remove(assetId);
                    synchronized (agreementStore) {
                        ContractAgreement agreement = agreementStore.get(assetId);
                        if (agreement != null && agreement.getContractSigningDate() + 600000L <= System.currentTimeMillis()) {
                            agreementStore.remove(assetId);
                        }
                        activeAssets.remove(assetId);
                    }
                }
            }
        }
        return null;
    }

    /**
     * sets active
     *
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
     *
     * @param asset name
     */
    protected void deactivate(String asset) {
        synchronized (activeAssets) {
            activeAssets.remove(asset);
        }
        synchronized (agreementStore) {
            agreementStore.remove(asset);
        }
        synchronized (processStore) {
            processStore.remove(asset);
        }
    }

    /**
     * register an agreement
     *
     * @param asset name
     * @param agreement object
     */
    protected void registerAgreement(String asset, ContractAgreement agreement) {
        synchronized (agreementStore) {
            agreementStore.put(asset, agreement);
        }
    }

    /**
     * register a process
     *
     * @param asset name
     * @param process object
     */
    protected void registerProcess(String asset, TransferProcess process) {
        synchronized (processStore) {
            processStore.put(asset, process);
        }
    }

    /**
     * creates a new agreement (asynchronously)
     * and waits for the result
     * TODO make this federation aware: multiple assets, different policies
     *
     * @param remoteUrl ids endpoint url of the remote connector
     * @param asset name of the asset to agree upon
     */
    @Override
    public EndpointDataReference createAgreement(String remoteUrl, String asset) throws WebApplicationException {
        monitor.debug(String.format("About to create an agreement for asset %s at connector %s", asset, remoteUrl));

        activate(asset);

        DcatCatalog contractOffers;

        try {
            contractOffers = dataManagement.findContractOffers(remoteUrl, asset);
        } catch (IOException io) {
            deactivate(asset);
            throw new InternalServerErrorException(String.format("Error when resolving contract offers from %s for asset %s through data management api.", remoteUrl, asset), io);
        }

        if (contractOffers.getDatasets().isEmpty()) {
            deactivate(asset);
            throw new BadRequestException(String.format("There is no contract offer in remote connector %s related to asset %s.", remoteUrl, asset));
        }

        // TODO implement a cost-based offer choice
        DcatDataset contractOffer = contractOffers.getDatasets().get(0);
        Map<String, JsonValue> assetProperties = DataspaceSynchronizer.getProperties(contractOffer);
        OdrlPolicy policy = contractOffer.hasPolicy();
        String offerId = policy.getId();
        JsonValue offerType = assetProperties.get("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
        monitor.debug(String.format("About to create an agreement for contract offer %s (for asset %s of type %s at connector %s)", offerId, asset,
                offerType, remoteUrl));

        var contractOfferDescription = new ContractOfferDescription(
                offerId,
                asset,
                policy
        );
        var contractNegotiationRequest = ContractNegotiationRequest.Builder.newInstance()
                .offerId(contractOfferDescription)
                .connectorId("provider")
                .connectorAddress(String.format(DataManagementImpl.DSP_PATH, remoteUrl))
                .protocol("dataspace-protocol-http")
                .localBusinessPartnerNumber(config.getBusinessPartnerNumber())
                .remoteBusinessPartnerNumber(contractOffers.getParticipantId())
                .build();
        String negotiationId;

        try {
            negotiationId = dataManagement.initiateNegotiation(contractNegotiationRequest);
        } catch (IOException ioe) {
            deactivate(asset);
            throw new InternalServerErrorException(String.format("Error when initiating negotation for offer %s through data management api.", offerId), ioe);
        }

        monitor.debug(String.format("About to check negotiation %s for contract offer %s (for asset %s at connector %s)", negotiationId, offerId, asset, remoteUrl));

        // Check negotiation state
        ContractNegotiation negotiation = null;

        long startTime = System.currentTimeMillis();

        try {
            while ((System.currentTimeMillis() - startTime < config.getNegotiationTimeout()) &&
                    (negotiation == null ||
                    (!negotiation.getState().equals("FINALIZED") && !negotiation.getState().equals("TERMINATED")))) {
                Thread.sleep(config.getNegotiationPollInterval());
                negotiation = dataManagement.getNegotiation(
                        negotiationId
                );
            }
        } catch (InterruptedException e) {
            monitor.info(String.format("Negotiation thread for asset %s negotiation %s has been interrupted. Giving up.", asset, negotiationId), e);
        } catch (IOException e) {
            monitor.warning(String.format("Negotiation thread for asset %s negotiation %s run into problem. Giving up.", asset, negotiationId), e);
        }

        if (negotiation == null || !negotiation.getState().equals("FINALIZED")) {
            deactivate(asset);
            if (negotiation != null) {
                String errorDetail = negotiation.getErrorDetail();
                if (errorDetail != null) {
                    monitor.severe(String.format("Contract Negotiation %s failed because of %s", negotiationId, errorDetail));
                }
            }
            throw new InternalServerErrorException(String.format("Contract Negotiation %s for asset %s was not successful.", negotiationId, asset));
        }

        monitor.debug(String.format("About to check agreement %s for contract offer %s (for asset %s at connector %s)", negotiation.getContractAgreementId(), offerId, asset, remoteUrl));

        ContractAgreement agreement;

        try {
            agreement = dataManagement.getAgreement(negotiation.getContractAgreementId());
        } catch (IOException ioe) {
            deactivate(asset);
            throw new InternalServerErrorException(String.format("Error when retrieving agreement %s for negotiation %s.", negotiation.getContractAgreementId(), negotiationId), ioe);
        }

        if (agreement == null || !agreement.getAssetId().endsWith(asset)) {
            deactivate(asset);
            throw new InternalServerErrorException(String.format("Agreement %s does not refer to asset %s.", negotiation.getContractAgreementId(), asset));
        }

        registerAgreement(asset, agreement);

        DataAddress dataDestination = DataAddress.Builder.newInstance()
                .type(TRANSFER_TYPE)
                .build();

        CallbackAddress address =
                CallbackAddress.Builder.newInstance().uri(config.getCallbackEndpoint()).build();

        TransferRequest transferRequest = TransferRequest.Builder.newInstance()
                .assetId(asset)
                .contractId(agreement.getId())
                .connectorId(config.getBusinessPartnerNumber())
                .connectorAddress(String.format(DataManagementImpl.DSP_PATH, remoteUrl))
                .protocol("dataspace-protocol-http")
                .dataDestination(dataDestination)
                .managedResources(false)
                .callbackAddresses(List.of(address))
                .build();

        monitor.debug(String.format("About to initiate transfer for agreement %s (for asset %s at connector %s)", negotiation.getContractAgreementId(), asset, remoteUrl));

        String transferId;

        try {
            transferId = dataManagement.initiateHttpProxyTransferProcess(transferRequest);
        } catch (IOException ioe) {
            deactivate(asset);
            throw new InternalServerErrorException(String.format("HttpProxy transfer for agreement %s could not be initiated.", agreement.getId()), ioe);
        }

        monitor.debug(String.format("About to check transfer %s (for asset %s at connector %s)", transferId, asset, remoteUrl));

        // Check negotiation state
        TransferProcess process = null;

        startTime = System.currentTimeMillis();

        // EDC 0.5.1 has a problem with the checker configuration and wont process to COMPLETED
        String expectedTransferState = config.isPrerelease() ? "COMPLETED" : "STARTED";

        try {
            while ((System.currentTimeMillis() - startTime < config.getNegotiationTimeout()) && (process == null || !process.getState().equals(expectedTransferState))) {
                Thread.sleep(config.getNegotiationPollInterval());
                process = dataManagement.getTransfer(
                        transferId
                );
                registerProcess(asset, process);
            }
        } catch (InterruptedException e) {
            monitor.info(String.format("Process thread for asset %s transfer %s has been interrupted. Giving up.", asset, transferId), e);
        } catch (IOException e) {
            monitor.warning(String.format("Process thread for asset %s transfer %s run into problem. Giving up.", asset, transferId), e);
        }

        if (process == null || !process.getState().equals(expectedTransferState)) {
            deactivate(asset);
            throw new InternalServerErrorException(String.format("Transfer process %s for agreement %s and asset %s could not be provisioned.", transferId, agreement.getId(), asset));
        }

        // finally wait a bit for the endpoint data reference in case
        // that the process was signalled earlier than the callbacks
        startTime = System.currentTimeMillis();

        EndpointDataReference reference = null;

        try {
            while ((System.currentTimeMillis() - startTime < config.getNegotiationTimeout()) && (reference == null)) {
                Thread.sleep(config.getNegotiationPollInterval());
                synchronized (endpointStore) {
                    reference = endpointStore.get(asset);
                }
            }
        } catch (InterruptedException e) {
            monitor.info(String.format("Wait thread for reference to asset %s has been interrupted. Giving up.", asset), e);
        }

        // mark the type in the endpoint
        if (reference != null) {
            for (Map.Entry<String, JsonValue> prop : assetProperties.entrySet()) {
                reference.getProperties().put(prop.getKey(), JsonLd.asString(prop.getValue()));
            }
        }

        // now delegate to the original getter
        return get(asset);
    }

}
