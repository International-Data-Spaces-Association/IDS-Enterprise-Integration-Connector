package de.fraunhofer.iais.eis.ids.connector.client.echo;

import de.fraunhofer.iais.eis.*;
import de.fraunhofer.iais.eis.ids.component.client.HTTPMultipartComponentInteractor;
import de.fraunhofer.iais.eis.ids.component.core.MessageAndPayload;
import de.fraunhofer.iais.eis.ids.component.core.util.CalendarUtil;
import de.fraunhofer.iais.eis.ids.component.core.RequestType;
import de.fraunhofer.iais.eis.ids.connector.commons.artifact.map.ArtifactRequestMAP;
import de.fraunhofer.iais.eis.util.ConstraintViolationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class RemoteEcho {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Value("${component.url}")
	private String component = "";

	@Value("${component.maintainer}")
	private String maintainer = "";

	@Value("${component.modelversion}")
	private String modelversion = "";

	@Value("${server.port}")
	private String port = "";
    
    private HTTPMultipartComponentInteractor httpComponentInteractor;
	private URI componentUri;
	private URI maintainerUri;
	

    public RemoteEcho(HTTPMultipartComponentInteractor httpComponentInteractor, URI componentUri, URI maintainer, String modelVersion) {
        this.httpComponentInteractor = httpComponentInteractor;
        this.componentUri = componentUri;
        this.maintainerUri = maintainer;
        this.modelversion = modelVersion;
    }

    public MessageAndPayload sendEchoMessage() throws EchoException, IOException, ConstraintViolationException, URISyntaxException {
    	ArtifactRequestMessage artifactRequest = new ArtifactRequestMessageBuilder()
                ._issued_(CalendarUtil.now())
                ._requestedArtifact_(new URI("http://example.org/artifacts/dummy/"))
                ._issuerConnector_(componentUri)
                ._modelVersion_(modelversion)
                .build();
        

        MessageAndPayload map = new ArtifactRequestMAP(artifactRequest);
        System.out.println(map.getMessage());
        MessageAndPayload response =  httpComponentInteractor.process(map, RequestType.DATA); //RequestType.ECHO
        
        if (!(response instanceof MessageAndPayload<?, ?>)) {
            throw new EchoException("Error speaking with Echo Connector: " + response.getMessage());
        }

        logger.info("Successfully connected to Echo Connector ().");
        return response;
    }

}
