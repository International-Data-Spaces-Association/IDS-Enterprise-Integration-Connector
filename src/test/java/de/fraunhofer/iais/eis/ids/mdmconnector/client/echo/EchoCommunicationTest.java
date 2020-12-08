package de.fraunhofer.iais.eis.ids.mdmconnector.client.echo;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import de.fraunhofer.iais.eis.ids.component.client.HTTPMultipartComponentInteractor;
import de.fraunhofer.iais.eis.ids.component.core.InfomodelFormalException;
import de.fraunhofer.iais.eis.ids.mdmconnector.infrastructure.DynamicConnectorSelfDescription;


public class EchoCommunicationTest {
	
	private static final Logger logger = LoggerFactory.getLogger(EchoCommunicationTest.class);


	@Value("${component.url}")
	private String component = "";

	@Value("${component.maintainer}")
	private String maintainer = "";

	@Value("${component.modelversion}")
	private String modelversion = "";

	@Value("${server.port}")
	private String port = "";

	private String echoConnectorUrl = "https://echo.ids.isst.fraunhofer.de";

    private DynamicConnectorSelfDescription selfDescription;
    
    
    @Autowired
    private MockMvc mvc;


    
    
    
	@Test
	public void test() throws IOException, URISyntaxException {

		// test the echo connector
		HTTPMultipartComponentInteractor httpComponentInteractor = new HTTPMultipartComponentInteractor(new URL(echoConnectorUrl));
		RemoteEcho echo = new RemoteEcho(httpComponentInteractor, new URI(component), new URI(maintainer), modelversion);
//		try {
//			// TODO: Does not make sense as the HTTPMultipartComponentInteractor has a bug: assumes always a payload
//			echo.sendEchoMessage();
//			logger.info("Successfully connected to the Echo Connector (" + echoConnectorUrl + ").");
//		} catch (InfomodelFormalException | EchoException e) {
//			logger.warn("Could not connect to the Echo Connector (" + echoConnectorUrl + ")." , e);
//			e.printStackTrace();
//		}
		
	}

}
