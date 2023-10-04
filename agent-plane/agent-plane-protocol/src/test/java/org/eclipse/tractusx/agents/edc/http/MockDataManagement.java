package org.eclipse.tractusx.agents.edc.http;

import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.tractusx.agents.edc.model.Asset;
import org.eclipse.tractusx.agents.edc.model.ContractAgreement;
import org.eclipse.tractusx.agents.edc.model.ContractNegotiation;
import org.eclipse.tractusx.agents.edc.model.ContractNegotiationRequest;
import org.eclipse.tractusx.agents.edc.model.DcatCatalog;
import org.eclipse.tractusx.agents.edc.model.IdResponse;
import org.eclipse.tractusx.agents.edc.model.TransferProcess;
import org.eclipse.tractusx.agents.edc.model.TransferRequest;
import org.eclipse.tractusx.agents.edc.service.DataManagement;

import java.io.IOException;
import java.util.List;

public class MockDataManagement implements DataManagement {
    @Override
    public DcatCatalog findContractOffers(String remoteControlPlaneIdsUrl, String assetId) throws IOException {
        return null;
    }

    @Override
    public DcatCatalog getCatalog(String remoteControlPlaneIdsUrl, QuerySpec spec) throws IOException {
        return null;
    }

    @Override
    public List<Asset> listAssets(QuerySpec spec) throws IOException {
        return null;
    }

    @Override
    public IdResponse createOrUpdateSkill(String assetId, String name, String description, String version, String contract, String ontologies, String distributionMode, boolean isFederated, String query) throws IOException {
        return null;
    }

    @Override
    public IdResponse createOrUpdateGraph(String assetId, String name, String description, String version, String contract, String ontologies, String shape, boolean isFederated) throws IOException {
        return null;
    }

    @Override
    public IdResponse deleteAsset(String assetId) throws IOException {
        return null;
    }

    @Override
    public String initiateNegotiation(ContractNegotiationRequest negotiationRequest) throws IOException {
        return null;
    }

    @Override
    public ContractNegotiation getNegotiation(String negotiationId) throws IOException {
        return null;
    }

    @Override
    public ContractAgreement getAgreement(String agreementId) throws IOException {
        return null;
    }

    @Override
    public String initiateHttpProxyTransferProcess(TransferRequest transferRequest) throws IOException {
        return null;
    }

    @Override
    public TransferProcess getTransfer(String transferProcessId) throws IOException {
        return null;
    }
}
