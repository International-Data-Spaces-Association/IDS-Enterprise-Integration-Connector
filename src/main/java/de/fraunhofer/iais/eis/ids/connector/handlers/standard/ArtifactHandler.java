package de.fraunhofer.iais.eis.ids.connector.handlers.standard;

import de.fraunhofer.iais.eis.*;
import de.fraunhofer.iais.eis.ids.component.core.MessageAndPayload;
import de.fraunhofer.iais.eis.ids.component.core.MessageHandler;
import de.fraunhofer.iais.eis.ids.component.core.RejectMessageException;
import de.fraunhofer.iais.eis.ids.component.core.TokenRetrievalException;
import de.fraunhofer.iais.eis.ids.component.core.util.CalendarUtil;
import de.fraunhofer.iais.eis.ids.component.ecosystemintegration.daps.DapsSecurityTokenProvider;
import de.fraunhofer.iais.eis.ids.connector.artifact.ArtifactIndex;
import de.fraunhofer.iais.eis.ids.connector.commons.artifact.map.ArtifactRequestMAP;
import de.fraunhofer.iais.eis.ids.connector.commons.artifact.map.ArtifactResponseMAP;
import de.fraunhofer.iais.eis.ids.connector.logging.LoggingInteractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

public class ArtifactHandler implements MessageHandler<ArtifactRequestMAP, ArtifactResponseMAP> {

    private InfrastructureComponent infrastructureComponent;
    private ArtifactIndex artifactIndex; // was: ArtifactFileProvider (which is extended by ArtifactIndex
    private DapsSecurityTokenProvider daps;
    private LoggingInteractor loggingInteractor;
    private Logger logger = LoggerFactory.getLogger(DescriptionRequestMessageBuilder.class);


    public ArtifactHandler(InfrastructureComponent infrastructureComponent,
                           ArtifactIndex artifactIndex, DapsSecurityTokenProvider daps, LoggingInteractor loggingInteractor) {
        this.infrastructureComponent = infrastructureComponent;
        this.artifactIndex = artifactIndex;
        this.daps = daps;
        this.loggingInteractor = loggingInteractor;

    }

    @Override
    public ArtifactResponseMAP handle(ArtifactRequestMAP messageAndPayload) throws RejectMessageException {
        URI artifactUri = messageAndPayload.getMessage().getRequestedArtifact();

        loggingInteractor.logMaP((MessageAndPayload) messageAndPayload);
        //If Artifact has no Contract attached, just Send it
            ArtifactResponseMessage artifactResponseMessage = null;
            try {
                artifactResponseMessage = new ArtifactResponseMessageBuilder()
                        ._issuerConnector_(infrastructureComponent.getId())
                        ._issued_(CalendarUtil.now())
                        ._modelVersion_(infrastructureComponent.getOutboundModelVersion())
                        ._correlationMessage_(messageAndPayload.getMessage().getId())
                        ._securityToken_(daps.getSecurityTokenAsDAT())
                        ._senderAgent_(infrastructureComponent.getMaintainer())
                        .build();

                ArtifactResponseMessage finalArtifactResponseMessage = artifactResponseMessage;
                ArtifactResponseMAP artifactResponseMAP = artifactIndex.getArtifact(artifactUri)
                        .map(file -> new ArtifactResponseMAP(finalArtifactResponseMessage, file))
                        .orElseThrow(() -> new RejectMessageException(RejectionReason.NOT_FOUND));
                loggingInteractor.logMaP((MessageAndPayload) messageAndPayload, true);
                return artifactResponseMAP;
            } catch (TokenRetrievalException e) {
                e.printStackTrace();
                logger.warn(e.getMessage());
                throw new RejectMessageException(RejectionReason.MALFORMED_MESSAGE);
            }
    }

    private URI getContract(URI artifactUri) {
        Optional<Artifact> artifact = artifactIndex.getArtifactAsObject(artifactUri);
        if (artifact.isPresent() && artifactIndex.hasContract(artifact.get())) {
            return artifactIndex.getContract(artifact.get()).getId();
        } else {
            return null;
        }
    }

    private URI getSignedContract(URI artifactUri, URI sender) {
        Optional<Artifact> artifact = artifactIndex.getArtifactAsObject(artifactUri);
        if (artifact.isPresent() && artifactIndex.hasSignedContract(artifact.get(),sender)) {
            return artifactIndex.getSignedContract(artifact.get(), sender).getId();
        } else {
            return null;
        }
    }

    @Override
    public Collection<Class<? extends Message>> getSupportedMessageTypes() {
        return Arrays.asList(ArtifactRequestMessage.class);
    }

}
