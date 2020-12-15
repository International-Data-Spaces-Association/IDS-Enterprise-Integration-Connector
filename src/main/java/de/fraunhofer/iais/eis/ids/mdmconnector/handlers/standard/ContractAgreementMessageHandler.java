package de.fraunhofer.iais.eis.ids.mdmconnector.handlers.standard;

import de.fraunhofer.iais.eis.*;
import de.fraunhofer.iais.eis.ids.component.core.MessageAndPayload;
import de.fraunhofer.iais.eis.ids.component.core.MessageHandler;
import de.fraunhofer.iais.eis.ids.component.core.RejectMessageException;
import de.fraunhofer.iais.eis.ids.component.core.map.DefaultSuccessMAP;
import de.fraunhofer.iais.eis.ids.connector.commons.contract.map.ContractRejectionMAP;
import de.fraunhofer.iais.eis.ids.mdmconnector.artifact.ArtifactIndex;
import de.fraunhofer.iais.eis.ids.mdmconnector.components.ContractUtil;
import de.fraunhofer.iais.eis.ids.component.core.util.CalendarUtil;
import de.fraunhofer.iais.eis.ids.component.ecosystemintegration.daps.DapsSecurityTokenProvider;
import de.fraunhofer.iais.eis.ids.connector.commons.contract.map.ContractAgreementMAP;


import java.net.URI;
import java.security.InvalidParameterException;
import java.util.Arrays;
import java.util.Collection;

import de.fraunhofer.iais.eis.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContractAgreementMessageHandler implements MessageHandler<ContractAgreementMAP, MessageAndPayload<Message, Object>> {


	Logger logger = LoggerFactory.getLogger(this.getClass());


	private InfrastructureComponent infrastructureComponent;
	private boolean isProvider;
	private URI participant;
	private DapsSecurityTokenProvider daps;
	private ArtifactIndex artifactIndex;
	private ContractUtil contractUtil;

	/**
	 * 
	 * @param infrastructureComponent
	 * @param negotiationServiceURL
	 */
	public ContractAgreementMessageHandler(InfrastructureComponent infrastructureComponent, DapsSecurityTokenProvider daps, ArtifactIndex artifactIndex) {
		this.infrastructureComponent = infrastructureComponent;
		this.daps =daps ;
		this.artifactIndex = artifactIndex ;
		this.contractUtil = new ContractUtil(artifactIndex, infrastructureComponent);
	}



	@Override
	public MessageAndPayload handle(ContractAgreementMAP contractAgreementMAP) throws RejectMessageException {

		try {
			// Interactor with the Negotiation Service App
			ContractAgreementMessage message = contractAgreementMAP.getMessage();
			logger.info("Incoming ContractAgreement:");
			logger.info(message.toRdf());


			// --------------------------------------------------------
			// 1) Validation Tests
			// ---------------------------------------

			// 1.1) Test if the Message Payload is present.
			logger.info("Test if the Message Payload is present...");
			if (!contractAgreementMAP.getPayload().isPresent()) {
				logger.warn("No Payload found in Multipart Message.");
				throw new RejectMessageException(RejectionReason.MALFORMED_MESSAGE, new Exception("No Payload found in Multipart Message."));
			}

			ContractAgreement agreementContract = (ContractAgreement) contractAgreementMAP.getPayload().get();
			logger.info("...passed:");
				if (contractUtil.compareWithIndex(agreementContract, message.getSenderAgent())) {
					MessageAndPayload successMessage =
							new DefaultSuccessMAP(infrastructureComponent.getId(), infrastructureComponent.getOutboundModelVersion(), message.getId(), daps.getSecurityTokenAsDAT(), infrastructureComponent.getMaintainer());

					URI targetUri = agreementContract.getPermission().get(0).getTarget();
					Artifact targetArtifact = artifactIndex.getArtifactAsObject(targetUri).get();
					artifactIndex.addSignedContract(targetArtifact, message.getSenderAgent(), agreementContract);

					return successMessage;
				} else {
					return new ContractRejectionMAP(new ContractRejectionMessageBuilder()
							._issuerConnector_(infrastructureComponent.getId())
							._issued_(CalendarUtil.now())
							._senderAgent_(infrastructureComponent.getMaintainer())
							._securityToken_(daps.getSecurityTokenAsDAT())
							._modelVersion_(infrastructureComponent.getOutboundModelVersion())
							._correlationMessage_(message.getId())
							._recipientConnector_(Util.asList(message.getIssuerConnector()))
							._recipientAgent_(Util.asList(message.getSenderAgent()))
							.build());
				}



		} catch (Exception e) {
			logger.warn("Can not accept ContractAgreement as something went wrong: 4", e);
			throw new RejectMessageException(RejectionReason.INTERNAL_RECIPIENT_ERROR, new InvalidParameterException("Can not accept to your ContractAgreement as something went wrong."));
		}



	}

	@Override
	public Collection<Class<? extends Message>> getSupportedMessageTypes() {
		return Arrays.asList(ContractAgreementMessage.class);
	}
}