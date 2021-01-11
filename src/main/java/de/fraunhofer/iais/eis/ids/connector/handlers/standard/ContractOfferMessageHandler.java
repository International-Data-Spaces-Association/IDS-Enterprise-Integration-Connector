package de.fraunhofer.iais.eis.ids.connector.handlers.standard;

import de.fraunhofer.iais.eis.*;
import de.fraunhofer.iais.eis.ids.component.core.MessageHandler;
import de.fraunhofer.iais.eis.ids.component.core.RejectMessageException;
import de.fraunhofer.iais.eis.ids.connector.commons.contract.map.ContractMAP;
import de.fraunhofer.iais.eis.ids.connector.commons.contract.map.ContractOfferMAP;
import de.fraunhofer.iais.eis.ids.jsonld.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Arrays;
import java.util.Collection;

public class ContractOfferMessageHandler implements MessageHandler<ContractOfferMAP, ContractMAP<Message, Contract>> {

	/**
	 * Contract Negotiation, Requester perspective
	 * <p>
	 *
	 */
	private Logger logger = LoggerFactory.getLogger(ContractRequestMessageBuilder.class);
	private static final Serializer serializer = new Serializer();

	private String negotiationServiceURL; 
	private URI consumer;

	private InfrastructureComponent infrastructureComponent;
	
	private DynamicAttributeToken dummyDAT;


	public ContractOfferMessageHandler(InfrastructureComponent infrastructureComponent, URI consumer) {
		this.infrastructureComponent = infrastructureComponent;
		this.consumer = consumer;
		this.dummyDAT = new DynamicAttributeTokenBuilder()._tokenFormat_(TokenFormat.JWT)._tokenValue_("ey").build();
	}

	@Override
	public ContractMAP<Message, Contract> handle(ContractOfferMAP contractOfferMAP) throws RejectMessageException {

		throw( new RejectMessageException(RejectionReason.MESSAGE_TYPE_NOT_SUPPORTED));
	}


	@Override
	public Collection<Class<? extends Message>> getSupportedMessageTypes() {
		return Arrays.asList(ContractOfferMessage.class);
	}


}

