package de.fraunhofer.iais.eis.ids.connector.main;

import de.fraunhofer.iais.eis.ids.component.client.broker.RemoteBroker;
import de.fraunhofer.iais.eis.ids.component.core.*;
import de.fraunhofer.iais.eis.ids.component.ecosystemintegration.daps.DapsSecurityTokenProvider;
import de.fraunhofer.iais.eis.ids.component.ecosystemintegration.daps.DapsSecurityTokenVerifier;
import de.fraunhofer.iais.eis.ids.component.ecosystemintegration.daps.JWKSFromIssuer;
import de.fraunhofer.iais.eis.ids.component.ecosystemintegration.daps.JWTClaimsVerifier;
import de.fraunhofer.iais.eis.ids.connector.artifact.ArtifactChangeManager;
import de.fraunhofer.iais.eis.ids.connector.artifact.DirectoryWatcher;
import de.fraunhofer.iais.eis.ids.connector.artifact.InMemoryArtifactIndex;

import de.fraunhofer.iais.eis.ids.connector.handlers.standard.*;
import de.fraunhofer.iais.eis.ids.connector.infrastructure.DynamicConnectorSelfDescription;
import de.fraunhofer.iais.eis.ids.connector.artifact.ArtifactIndex;
import de.fraunhofer.iais.eis.ids.connector.logging.LoggingInteractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.util.Arrays;
import java.util.Map;

class AppConfig {

    final private Logger logger = LoggerFactory.getLogger(AppConfig.class);
    DapsSecurityTokenProvider daps = null;

    Component createConnectorWiring(DynamicConnectorSelfDescription selfDescriptionProvider,
                                    URI participant,
                                    DapsInteractionConfig dapsInteractionConfig,
                                    RemoteBroker remoteBroker,
                                    DirectoryWatcher watcher,
                                    String negotiationServiceURL,
                                    LoggingInteractor logging,
                                    boolean brokerIgnore) throws InfomodelFormalException, URISyntaxException {

        try {
            //InputStream keystore = getClass().getClassLoader().getResourceAsStream(dapsInteractionConfig.keyStoreFile);
            InputStream keystore = Files.newInputStream((new File("")).toPath().resolve(dapsInteractionConfig.keyStoreFile));
            String dapsUrl = dapsInteractionConfig.dapsUrl + "/token";
            daps = new DapsSecurityTokenProvider(keystore,
                    dapsInteractionConfig.keyStorePwd,
                    dapsInteractionConfig.keyStoreAlias,
                    dapsInteractionConfig.UUID,
                    dapsUrl,
                    true, //TODO: replace these two boolean parameters by variables. Once there is a DAPS with a proper X.509, this should be set to false
                    true);
            String securityToken = daps.getSecurityToken();
        } catch (InvalidPathException | IOException | TokenRetrievalException e) {
            e.printStackTrace();
            logger.warn("Could not create security token!", e);
        }

        // Artifact change handling
        ArtifactIndex artifactIndex = new InMemoryArtifactIndex();
        MessageHandler artifactHandler = new ArtifactWithContractHandler(selfDescriptionProvider.getSelfDescription(), artifactIndex, daps, logging);
        ArtifactChangeManager artifactChangeManager = new ArtifactChangeManager(remoteBroker, artifactIndex, selfDescriptionProvider, daps, brokerIgnore);
        watcher.setArtifactListeners(Arrays.asList(artifactChangeManager));


        // Add MessageHandlers
        ContractOfferMessageHandler contractOfferHandler = new ContractOfferMessageHandler(selfDescriptionProvider.getSelfDescription(), participant);
        ContractRequestMessageHandler contractRequestHandler = new ContractRequestMessageHandler(selfDescriptionProvider.getSelfDescription(), daps, artifactIndex);
        ContractAgreementMessageHandler contractAgreementHandler = new ContractAgreementMessageHandler(selfDescriptionProvider.getSelfDescription(), daps, artifactIndex);
        DescriptionRequestHandler selfDescriptionRequestHandler = new DescriptionRequestHandler(selfDescriptionProvider,  daps,logging);
        DefaultComponent component = new DefaultComponent(selfDescriptionProvider, daps, selfDescriptionProvider.getSelfDescription().getId(), true);
        //ContractComponent component = new ContractComponent(selfDescriptionProvider);

        component.addMessageHandler(artifactHandler, RequestType.DATA);
        component.addMessageHandler(contractRequestHandler, RequestType.DATA);
        component.addMessageHandler(contractOfferHandler, RequestType.DATA);
        component.addMessageHandler(contractAgreementHandler, RequestType.DATA);
        component.addMessageHandler(selfDescriptionRequestHandler, RequestType.DATA);
        System.out.print(dapsInteractionConfig.trustedHosts);

        if (dapsInteractionConfig.verify) {
            component.setSecurityTokenVerifier( //TODO: Very recent changes were made here on components side. Please do not provide your own claims verifier, but use the internal one
                    new DapsSecurityTokenVerifier(new JWKSFromIssuer(dapsInteractionConfig.trustedHosts),
                    new JWTClaimsVerifier() {
                        @Override
                        public void verify(Map<String, Object> map) throws TokenVerificationException {
                            System.out.println("verified");
                        }
                    }));
        }



        return component;

    }

}
