package de.fraunhofer.iais.eis.ids.connector.handlers.standard;

import de.fraunhofer.iais.eis.*;
import de.fraunhofer.iais.eis.ids.component.core.MessageAndPayload;
import de.fraunhofer.iais.eis.ids.component.core.MessageHandler;
import de.fraunhofer.iais.eis.ids.component.core.RejectMessageException;
import de.fraunhofer.iais.eis.ids.component.core.TokenRetrievalException;
import de.fraunhofer.iais.eis.ids.component.core.map.DescriptionRequestMAP;
import de.fraunhofer.iais.eis.ids.component.core.map.DescriptionResponseMAP;
import de.fraunhofer.iais.eis.ids.component.core.util.CalendarUtil;
import de.fraunhofer.iais.eis.ids.component.ecosystemintegration.daps.DapsSecurityTokenProvider;
import de.fraunhofer.iais.eis.ids.connector.infrastructure.DynamicConnectorSelfDescription;
import de.fraunhofer.iais.eis.ids.connector.logging.LoggingInteractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * SelfDescription Negotiation, step 1+3 from IDS handshake, provider perspective
 * <p>
 * it's the goal to implement automated negotiation, so we omit {@link RequestInProcessMessage}
 * </p>
 */
public class DescriptionRequestHandler implements MessageHandler<DescriptionRequestMAP, DescriptionResponseMAP> {

    private Logger logger = LoggerFactory.getLogger(DescriptionRequestHandler.class);

    private DynamicConnectorSelfDescription selfDescription;
    private DapsSecurityTokenProvider daps;
    private LoggingInteractor loggingInteractor;

    public DescriptionRequestHandler(DynamicConnectorSelfDescription selfDescription, DapsSecurityTokenProvider daps, LoggingInteractor loggingInteractor) {
        this.selfDescription = selfDescription;
        this.daps = daps;
        this.loggingInteractor = loggingInteractor;
    }


    @Override
    public DescriptionResponseMAP handle(DescriptionRequestMAP DescriptionRequestMAP) throws RejectMessageException {
        String result;
        DescriptionRequestMAP.getMessage().getRequestedElement();
        loggingInteractor.logMaP((MessageAndPayload) DescriptionRequestMAP);
        //currently it is wrapped inside
        BaseConnector infrastructureComponent = (BaseConnector) selfDescription.getSelfDescription();
        ResourceCatalog resourceCatalog = infrastructureComponent.getResourceCatalog().get(0); //Attention. There must always be a catalog, or this will fail
        String URIKey = null;
        result = "empty";
        try{
            URIKey = DescriptionRequestMAP.getMessage().getRequestedElement().toString();
                if(infrastructureComponent.getId().toString().contentEquals(URIKey)){
                    result = infrastructureComponent.toRdf();
                }
                List<ResourceCatalog> CatalogList = (List<ResourceCatalog>) infrastructureComponent.getResourceCatalog();
                Iterator<ResourceCatalog> itr = CatalogList.iterator();
                while (itr.hasNext()) {
                    ResourceCatalog catalog = (ResourceCatalog) itr.next();
                    if (catalog.getId().toString().contentEquals(URIKey))
                    {
                        result = catalog.toRdf();
                    }
                    else {
                        List<Resource> ResourceList = (List<Resource>) catalog.getOfferedResource();
                        Iterator<Resource> itrR = ResourceList.iterator();
                        while (itrR.hasNext()) {
                            Resource resource = (Resource) itrR.next();
                            if (resource.getId().toString().contentEquals(URIKey))
                                result = resource.toRdf();
                        }
                    }
                }
                if (result == "empty")
                {
                    throw new RejectMessageException(RejectionReason.NOT_FOUND, new Throwable("Did not found Requested Element in Resources or CatalogIDs"));
                }

        }
        catch(NullPointerException e)
        {
            result = selfDescription.getSelfDescription().toRdf();
        }


        DescriptionResponseMessage descriptionResponse = null;
        try {
            descriptionResponse = new DescriptionResponseMessageBuilder()
                    ._issuerConnector_(selfDescription.getSelfDescription().getId())
                    ._issued_(CalendarUtil.now())
                    ._correlationMessage_(DescriptionRequestMAP.getMessage().getId())
                    ._securityToken_(daps.getSecurityTokenAsDAT())
                    ._senderAgent_(infrastructureComponent.getCurator()) //It might make sense to change this to a different sender agent
                    ._modelVersion_(infrastructureComponent.getOutboundModelVersion())
                    .build();

        } catch (TokenRetrievalException e) {
            throw new RejectMessageException(RejectionReason.INTERNAL_RECIPIENT_ERROR);

        }
        DescriptionResponseMAP answer = new DescriptionResponseMAP(descriptionResponse, result);
        loggingInteractor.logMaP((MessageAndPayload) answer, true);
        return answer;
        //   HTTPMultipartComponentInteractor interactorArtrifact = new HTTPMultipartComponentInteractor(new URL(connector));
        // DescriptionResponseMAP artRespMap = (ArtifactResponseMAP) interactorArtrifact
        //         .process(DescriptionResponseMAP(DescriptionResponse, infrastructureComponent), RequestType.INFRASTRUCTURE);
    }

    @Override
    public Collection<Class<? extends Message>> getSupportedMessageTypes() {
        return Arrays.asList(DescriptionRequestMessage.class);
    }

}
