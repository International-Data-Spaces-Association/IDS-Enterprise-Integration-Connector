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
import de.fraunhofer.iais.eis.util.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.*;

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
    private URI resourceCatalogId;
    private URI resourceIdInfrastructureComponent;
    private List<String> resourceIds;

    public DescriptionRequestHandler(DynamicConnectorSelfDescription selfDescription, DapsSecurityTokenProvider daps, LoggingInteractor loggingInteractor) {
        this.selfDescription = selfDescription;
        this.daps = daps;
        this.loggingInteractor = loggingInteractor;
    }

    
    @Override
    public DescriptionResponseMAP handle(DescriptionRequestMAP DescriptionRequestMAP) throws RejectMessageException {
        String result;
        DescriptionRequestMAP.getMessage().getRequestedElement();
        List<String> resourceList = new ArrayList<String>();
        loggingInteractor.logMaP((MessageAndPayload) DescriptionRequestMAP);
        //currently it is wrapped inside
        BaseConnector infrastructureComponent = (BaseConnector) selfDescription.getSelfDescription();
        ResourceCatalog resourceCatalog = infrastructureComponent.getResourceCatalog().get(0); //Attention. There must always be a catalog, or this will fail
        resourceIdInfrastructureComponent = resourceCatalog.getOfferedResource().get(0).getId(); //Attention. What if catalog is empty? Exception would be thrown here
        String sampleURIKey = null;
        result = infrastructureComponent.toRdf();
        if (DescriptionRequestMAP.getMessage().getRequestedElement() != null) {

            sampleURIKey = DescriptionRequestMAP.getMessage().getRequestedElement().toString();
            
            
            
            //review 1
            //If asked for artifact 
            //Object sampleObject = (ResourceCatalog)infrastructureComponent.getResourceCatalog().get(0).getOfferedResource().get(0);
       
            List<Resource> resourceIdList = (List<Resource>) infrastructureComponent.getResourceCatalog().get(0).getOfferedResource();
            Iterator<Resource> itr = resourceIdList.iterator();
            while (itr.hasNext() && itr != null) {
                Resource resourceId = (Resource) itr.next();
                resourceList.add(resourceId.getId().toString());
            }
            try {
            	String tempVal = resourceCatalog.getId().toString();        
                if (!tempVal.isEmpty() && resourceList.size()>0) {
                    //fetch response only through resource catalogID
                	//if asked for resource id
                	
                	for (int i = 0; i < resourceList.size(); i++) { 
                		
                		//fetch response through resourceId ,but unable to fetch single element!!!!!!!!!!!
                        //TODO: value ignored
                		result = infrastructureComponent.getResourceCatalog().get(0).getOfferedResource().get(i).toRdf();
                	}
                	
                    //query not sure if i have to change the response
                }
                else {
                	//Return whole response
                	result=  selfDescription.getSelfDescription().toRdf();
                	logger.info("Complete Json Response");
                }
             
            	
            } catch (ConstraintViolationException e) {
                e.printStackTrace();
                logger.warn(e.getMessage());
                throw new RejectMessageException(RejectionReason.NOT_FOUND);
            }
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
        DescriptionResponseMAP answer = new DescriptionResponseMAP(descriptionResponse,  result);
        loggingInteractor.logMaP((MessageAndPayload) answer, true);
        return answer;
        //   HTTPMultipartComponentInteractor interactorArtrifact = new HTTPMultipartComponentInteractor(new URL(connector));
        // DescriptionResponseMAP artRespMap = (ArtifactResponseMAP) interactorArtrifact
        //         .process(DescriptionResponseMAP(DescriptionResponse, infrastructureComponent), RequestType.INFRASTRUCTURE);
    }

        @Override
        public Collection<Class<? extends Message>> getSupportedMessageTypes () {
            return Arrays.asList(DescriptionRequestMessage.class);
        }

}
