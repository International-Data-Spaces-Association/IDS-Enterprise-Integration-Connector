package de.fraunhofer.iais.eis.ids.connector.handlers.standard;

import de.fraunhofer.iais.eis.*;
import de.fraunhofer.iais.eis.ids.component.core.MessageHandler;
import de.fraunhofer.iais.eis.ids.component.core.RejectMessageException;
import de.fraunhofer.iais.eis.ids.component.core.util.CalendarUtil;
import de.fraunhofer.iais.eis.ids.component.ecosystemintegration.daps.DapsSecurityTokenProvider;
import de.fraunhofer.iais.eis.ids.connector.commons.contract.map.*;
import de.fraunhofer.iais.eis.ids.jsonld.Serializer;
import de.fraunhofer.iais.eis.ids.connector.artifact.ArtifactIndex;


import java.net.URI;
import java.util.*;
import java.util.Collection;

import de.fraunhofer.iais.eis.ids.connector.components.ContractUtil;
import de.fraunhofer.iais.eis.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Contract Negotiation Handler From Provider Perspective.
 * This Class Implements a ContractRequestMessage Handler with a strict non
 * @author dennis.oliver.kubitza@iais.fraunhofer.de
 * @since 2020-01-21
 * @version 2020-09-23
 */
public class ContractRequestMessageHandler implements MessageHandler<ContractRequestMAP, ContractMAP<Message, Contract>> {

	private static final Serializer serializer = new Serializer();
	private Logger logger = LoggerFactory.getLogger(ContractRequestMessageBuilder.class);
	private DapsSecurityTokenProvider daps;



	private InfrastructureComponent infrastructureComponent;
	private ArtifactIndex artifactIndex;
	private ContractUtil contractUtil;

	public ContractRequestMessageHandler(InfrastructureComponent infrastructureComponent,DapsSecurityTokenProvider daps, ArtifactIndex artifactIndex) {
		this.infrastructureComponent = infrastructureComponent;
		this.artifactIndex = artifactIndex ;
		//this.provider = new URI("http://mdm-connector.ids.isst.fraunhofer.de/provider");
		contractUtil = new ContractUtil(artifactIndex, infrastructureComponent);
		this.daps = daps;
	}

	/**
	 * Contract Offer Map Defintion
	 */
	@Override
	public ContractMAP handle(ContractRequestMAP contractRequestMAP) throws RejectMessageException {

		try {
			// Interactor with the Negotiation Service App
			ContractRequestMessage message = contractRequestMAP.getMessage();
			logger.info("Incoming ContractRequest:");
			logger.info(message.toRdf());

			// --------------------------------------------------------
			// 1) Validation Tests
			// --------------------------------------------------------


			// 1.1) Test if the Message Payload is present.
			logger.info("Test if the Message Payload is present...");
			if (!contractRequestMAP.getPayload().isPresent()) {
				logger.warn("No Payload found in Multipart Message.");
				throw new RejectMessageException(RejectionReason.MALFORMED_MESSAGE, new Exception("No Payload found in Multipart Message."));
			}
			ContractRequest requestedContract = (ContractRequest) contractRequestMAP.getPayload().get();
			logger.info("...passed:");
			logger.info(requestedContract.toRdf());


			// 1.2) Test if artifact from request is available
			logger.info("Test if artifact from request is available...");
			URI targetUri = null;
			try{
				targetUri = requestedContract.getPermission().get(0).getTarget();
				System.out.println(targetUri);
				artifactIndex.getArtifact(targetUri).get();

			} catch(Exception e) {
				// can not find requested artifact in my catalog
				logger.warn("Can not find requested artifact in my catalog");
				throw new RejectMessageException(RejectionReason.NOT_FOUND, e);
			}
			logger.info("...passed");

			// 1.3) Test if contains information about the Agent
			ContractRequest requestContract = (ContractRequest) contractRequestMAP.getPayload().get();
			try{
				if((message.getSenderAgent() == null)){
					logger.warn("can not find SenderAgent");
					throw new RejectMessageException(RejectionReason.NOT_AUTHENTICATED);
				};
			} catch(Exception e) {
				// can not find requested artifact in my catalog
				logger.warn("can not parse SenderAgent");
				throw new RejectMessageException(RejectionReason.MALFORMED_MESSAGE, e);
			}
			logger.info("...passed");

			// --------------------------------------------------------
			// 2) ImplementBehaviour
			// --------------------------------------------------------


			// 2.1  if a ContractRequest has been received and contract Matches Existing one, send ContractAgreementMessage
			if (contractUtil.compareWithIndex(requestedContract, message.getSenderAgent()))
			{
				return acceptContractOffer(contractRequestMAP);
			}
			Artifact targetArtifact = artifactIndex.getArtifactAsObject(targetUri).get();


			// 2.3) if a ContractRequest has been received, but contract does not match existing ones
			return createDefaultOffer(contractRequestMAP);


		} catch (Exception e) {
			if ( e instanceof RejectMessageException)
			{
				throw (RejectMessageException) e;
			}
			logger.error("Internal Error during negotiation handling: ", e);
			throw new RejectMessageException(RejectionReason.INTERNAL_RECIPIENT_ERROR, e);
		}
	}

	private ContractAgreementMAP acceptContractNegotiation(ContractRequestMAP contractRequestMAP) throws RejectMessageException {

		logger.info("Accepting Contract Request");
		try {
			// Load the Topics of Negotiation
			ContractRequestMessage message = contractRequestMAP.getMessage();
			ContractRequest requestedContract = (ContractRequest) contractRequestMAP.getPayload().get();
			URI targetUri = requestedContract.getPermission().get(0).getTarget();
			Artifact targetArtifact = artifactIndex.getArtifactAsObject(targetUri).get();
			URI consumer = 	message.getSenderAgent();
			ContractAgreementMessage contractAgreementMessage = new ContractAgreementMessageBuilder()
					._issuerConnector_(infrastructureComponent.getId())
					._issued_(CalendarUtil.now())
					._senderAgent_(infrastructureComponent.getMaintainer())
					._securityToken_(daps.getSecurityTokenAsDAT())
					._modelVersion_(infrastructureComponent.getOutboundModelVersion())
					._correlationMessage_(message.getId())
					._recipientConnector_(Util.asList(message.getSenderAgent()))
					._recipientAgent_(Util.asList(consumer))
					._transferContract_(targetUri)
					.build();

			String offer = requestedContract.toRdf().replace("Request", "Agreement");
			ContractAgreement finalContractAgreement= new Serializer().deserialize(offer, ContractAgreement.class);
			logger.info("Sending Contract Agreement to Artifact Index");
			ContractAgreementMAP cap = new ContractAgreementMAP(contractAgreementMessage, finalContractAgreement);
			artifactIndex.addSignedContract(targetArtifact, consumer, finalContractAgreement);
			return cap;

		} catch (Exception e) {
			logger.error("Error during the creation of the Default Contract Offer.", e);
			throw new RejectMessageException(RejectionReason.INTERNAL_RECIPIENT_ERROR, new Exception("Error during the creation of the Default Contract Offer.", e));
		}
	}

	private ContractAgreementMAP acceptContractOffer(ContractRequestMAP contractRequestMAP) throws RejectMessageException {

		logger.info("Accepting Contract Request");
		try {
			// Load the Topics of Negotiation
			ContractRequestMessage message = contractRequestMAP.getMessage();
			ContractRequest requestedContract = (ContractRequest) contractRequestMAP.getPayload().get();
			URI targetUri = requestedContract.getPermission().get(0).getTarget();
			Artifact targetArtifact = artifactIndex.getArtifactAsObject(targetUri).get();
			URI consumer = 	message.getSenderAgent();
			ContractAgreementMessage contractAgreementMessage = new ContractAgreementMessageBuilder()
					._issuerConnector_(infrastructureComponent.getId())
					._issued_(CalendarUtil.now())
					._senderAgent_(infrastructureComponent.getMaintainer())
					._securityToken_(daps.getSecurityTokenAsDAT())
					._modelVersion_(infrastructureComponent.getOutboundModelVersion())
					._correlationMessage_(message.getId())
					._recipientConnector_(Util.asList(message.getSenderAgent()))
					._recipientAgent_(Util.asList(consumer))
					._transferContract_(targetUri)
					.build();

			ContractAgreement finalContractAgreement = contractUtil.createNewContractAgreement(targetArtifact, consumer);;
			logger.info("Sending Contract Agreement to Artifact Index");
			ContractAgreementMAP cap = new ContractAgreementMAP(contractAgreementMessage, finalContractAgreement);
			artifactIndex.addSignedContract(targetArtifact, consumer, finalContractAgreement);
			return cap;

		} catch (Exception e) {
			logger.error("Error during the creation of the Default Contract Offer.", e);
			throw new RejectMessageException(RejectionReason.INTERNAL_RECIPIENT_ERROR, new Exception("Error during the creation of the Default Contract Offer.", e));
		}
	}


	private ContractResponseMAP createDefaultOffer(ContractRequestMAP contractRequestMAP) throws RejectMessageException {

		logger.info("Create Default Contract:");

		try {
			// Load the Topics of Negotiation
			ContractRequestMessage message = contractRequestMAP.getMessage();
			ContractRequest requestedContract = (ContractRequest) contractRequestMAP.getPayload().get();
			URI targetUri = requestedContract.getPermission().get(0).getTarget();
			Artifact targetArtifact = artifactIndex.getArtifactAsObject(targetUri).get();
			URI consumer = 	message.getSenderAgent();
			ContractResponseMessage contractResponseMessage = new ContractResponseMessageBuilder()
									._issuerConnector_(infrastructureComponent.getId())
									._issued_(CalendarUtil.now())
									._senderAgent_(infrastructureComponent.getMaintainer())
									._securityToken_(daps.getSecurityTokenAsDAT())
									._modelVersion_(infrastructureComponent.getOutboundModelVersion())
									._correlationMessage_(message.getId())
									._recipientConnector_(Util.asList(message.getSenderAgent()))
									._recipientAgent_(Util.asList(consumer))
									._transferContract_(targetUri)
									.build();

			ContractOffer finalContractOffer = contractUtil.createNewContractOffer(targetArtifact, consumer);
			logger.info("Sending Default Contract Offer from Artifact Index");
			return new ContractResponseMAP(contractResponseMessage, finalContractOffer);


		} catch (Exception e) {
			logger.error("Error during the creation of the Default Contract Offer.", e);
			throw new RejectMessageException(RejectionReason.INTERNAL_RECIPIENT_ERROR, new Exception("Error during the creation of the Default Contract Offer.", e));
		}
	}



	@Override
	public Collection<Class<? extends Message>> getSupportedMessageTypes() {
		return Arrays.asList(ContractRequestMessage.class);
	}

}
