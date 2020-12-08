package de.fraunhofer.iais.eis.ids.mdmconnector.components;

import de.fraunhofer.iais.eis.DynamicAttributeToken;
import de.fraunhofer.iais.eis.MessageProcessedNotificationMessage;
import de.fraunhofer.iais.eis.MessageProcessedNotificationMessageBuilder;
import de.fraunhofer.iais.eis.ids.component.core.MessageAndPayload;
import de.fraunhofer.iais.eis.ids.component.core.SerializedPayload;
import de.fraunhofer.iais.eis.ids.component.core.util.CalendarUtil;
import de.fraunhofer.iais.eis.util.Util;

import java.net.URI;
import java.util.Optional;

public class DefaultSuccessMAP implements MessageAndPayload<MessageProcessedNotificationMessage, Void> {
    private MessageProcessedNotificationMessage message;

    public DefaultSuccessMAP(MessageProcessedNotificationMessage message) {
        this.message = message;
    }

    public MessageProcessedNotificationMessage getMessage() {
        return this.message;
    }

    public Optional<Void> getPayload() {
        return Optional.empty();
    }

    public SerializedPayload serializePayload() {
        return SerializedPayload.EMPTY;
    }
}
