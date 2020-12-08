package de.fraunhofer.iais.eis.ids.mdmconnector.logging;

import de.fraunhofer.iais.eis.Message;
import de.fraunhofer.iais.eis.ids.component.core.MessageAndPayload;
import de.fraunhofer.iais.eis.ids.component.ecosystemintegration.daps.DapsSecurityTokenProvider;
import de.fraunhofer.iais.eis.ids.component.interaction.multipart.Multipart;
import de.fraunhofer.iais.eis.ids.connector.commons.artifact.map.ArtifactResponseMAP;
import de.fraunhofer.iais.eis.ids.mdmconnector.infrastructure.DynamicConnectorSelfDescription;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.Header;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.NotNull;
import java.net.URI;
import java.util.concurrent.atomic.AtomicReference;


/**
 * Class for sending Message And Payload to an External Logging Service,
 * Further Inclusion of DAPS might be possible here.
 *
 * @author dennis.oliver.kubitza@iais.fraunhofer.de
 * @since 2020-01-21
 * @version 2020-09-23
 */
public class LoggingInteractor {

    final private Logger logger = LoggerFactory.getLogger(DynamicConnectorSelfDescription.class);

    private URI loggingIngoing;
    private URI loggingOutgoing;
    private boolean ignoreLogging;
    private DapsSecurityTokenProvider daps;

    /**
     * Constructor
     *
     * @param loggingIngoing URI of logger for Ingoing Messages
     * @param loggingOutgoing URI of logger for Outgoing Messages
     * @param daps DapsSecurtyTokenProvider for further validation.
     */

    public LoggingInteractor(@NotNull @NotNull URI loggingIngoing, URI loggingOutgoing, DapsSecurityTokenProvider daps, boolean ignoreLogging) {
        this.ignoreLogging = ignoreLogging;
        this.loggingIngoing = loggingIngoing;
        this.loggingOutgoing = loggingOutgoing;
        this.daps = daps;
    }

    /**
     * Sends a Message to the Logger for Ingoing Messages
     *
     * @param map A generic Message and Payload
     */
    public void logMaP(MessageAndPayload<Message, Object> map) {
        logMaP(map, false);
    }

    /**
     * Sends a Message to the Logger for Ingoing/Outgoing Messages
     * @param map A generic Message and Payload
     * @param outgoing Boolean, true for outgoing endpoint/ false for incoming endpoint
     */
    public void logMaP(MessageAndPayload map, boolean outgoing) {
        if (!ignoreLogging) {
            URI target;
            if (outgoing)
                target = this.loggingOutgoing;
            else target = this.loggingIngoing;
            try {

                Multipart multipart = new Multipart(map);

                MultipartEntityBuilder builder = MultipartEntityBuilder
                        .create()
                        .setBoundary("msgpart")
                        .addTextBody("header", multipart.getHeader(), ContentType.parse(multipart.getHeaderContentType()));
                if (map.getPayload().isPresent()) builder.addBinaryBody("payload",
                        multipart.getSerializedPayload().getSerialization(),
                        ContentType.parse(multipart.getSerializedPayload().getContentType()),
                        multipart.getSerializedPayload().getFilename());

                HttpEntity entity = builder.build();

                CloseableHttpClient httpclient = HttpClients.createDefault();
                HttpPost httpPost = new HttpPost(target);
                httpPost.setEntity(entity);

                //Logger Specific Formats in Http Header
                httpPost.addHeader("Authorization", "Bearer " + daps.getSecurityTokenAsDAT().getTokenValue());

                CloseableHttpResponse response = httpclient.execute(httpPost);
                if (response.getStatusLine().getStatusCode() == 200) {
                    logger.info("External Logging Service succeeded for Message:" + map.getMessage().toRdf());
                } else {
                    logger.warn("External Logging Service failed with:" + response.getStatusLine().getStatusCode() + " for Message:" + map.getMessage().toRdf());
                }
            } catch (Exception e) {
                logger.warn("External Logging Service Error");
                e.printStackTrace();

            }
        }
    }
}
