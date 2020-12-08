package de.fraunhofer.iais.eis.ids.mdmconnector.client.echo;

import de.fraunhofer.iais.eis.ids.component.core.MessageAndPayload;

public class EchoException extends Exception {

    private MessageAndPayload response;

	public EchoException(String message, MessageAndPayload response) {
        super(message);
        this.response = response;
	}
	
	public EchoException(String message) {
        super(message);
	}

}
