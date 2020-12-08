package de.fraunhofer.iais.eis.ids.mdmconnector.components;


import de.fraunhofer.iais.eis.ConnectorUnavailableMessageBuilder;
import de.fraunhofer.iais.eis.ConnectorUpdateMessageBuilder;
import de.fraunhofer.iais.eis.DynamicAttributeToken;
import de.fraunhofer.iais.eis.InfrastructureComponent;
import de.fraunhofer.iais.eis.Message;
import de.fraunhofer.iais.eis.ids.component.client.RemoteComponentInteractor;
import de.fraunhofer.iais.eis.ids.component.client.broker.BrokerException;
import de.fraunhofer.iais.eis.ids.component.core.MessageAndPayload;
import de.fraunhofer.iais.eis.ids.component.core.RequestType;
import de.fraunhofer.iais.eis.ids.component.core.map.DefaultSuccessMAP;
import de.fraunhofer.iais.eis.ids.component.core.util.CalendarUtil;
import de.fraunhofer.iais.eis.ids.connector.commons.broker.map.InfrastructureComponentMAP;
import java.io.IOException;

import de.fraunhofer.iais.eis.ids.mdmconnector.logging.LoggingInteractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class RemoteBrokerLogging extends de.fraunhofer.iais.eis.ids.component.client.broker.RemoteBroker {

    private static enum BrokerOperation {
        REGISTER,
        UNREGISTER,
        UPDATE;

        private BrokerOperation() {
        }
    }

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private RemoteComponentInteractor remoteComponentInteractor;
    private LoggingInteractor loggingInteractor;

    public RemoteBrokerLogging(RemoteComponentInteractor remoteComponentInteractor, LoggingInteractor loggingInteractor) {
        super(remoteComponentInteractor);
        this.loggingInteractor = loggingInteractor;
    }

    private MessageAndPayload issueRequest(Message message, InfrastructureComponent source, BrokerOperation operation) throws BrokerException, IOException {
        MessageAndPayload map = new InfrastructureComponentMAP(message, source);
        loggingInteractor.logMaP(map, true);
        MessageAndPayload response = (MessageAndPayload)this.remoteComponentInteractor.process(map, RequestType.INFRASTRUCTURE);
        if (!(response instanceof DefaultSuccessMAP)) {
            throw new BrokerException("Error during remote broker operation " + operation.name(), response);
        } else {
            this.logger.info("Success for remote broker operation " + operation.name());
            loggingInteractor.logMaP(map);
            return response;
        }
    }


}
