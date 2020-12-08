package de.fraunhofer.iais.eis.ids.mdmconnector.artifactdescription;

import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import javax.servlet.http.Part;
import javax.validation.constraints.NotNull;
import javax.xml.datatype.Duration;

import de.fraunhofer.iais.eis.*;
import de.fraunhofer.iais.eis.ids.component.ecosystemintegration.daps.DapsSecurityTokenProvider;
import de.fraunhofer.iais.eis.ids.mdmconnector.shared.DapsSecurityTokenProviderGenerator;
import de.fraunhofer.iais.eis.util.PlainLiteral;
import de.fraunhofer.iais.eis.util.Util;
import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.http.MultiPartFormInputStream;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.exc.InvalidTypeIdException;

import de.fraunhofer.iais.eis.ids.component.core.util.CalendarUtil;
import de.fraunhofer.iais.eis.ids.jsonld.Serializer;
import de.fraunhofer.iais.eis.ids.mdmconnector.main.Main;


/**
 * Testing the MDM connector with Spring MockMvc
 * in order to see if the descriptions are correctly 
 * added to the connector self-description
 * 
 * @author sbader
 *
 */
@SpringBootTest(classes = Main.class)
@RunWith(SpringJUnit4ClassRunner.class)
//@RunWith(SpringRunner.class)
@AutoConfigureMockMvc
@WebAppConfiguration
public class ArtifactDescriptionRequestTest {

	private Logger logger = LoggerFactory.getLogger(ArtifactDescriptionRequestTest.class);

	@Autowired
	private WebApplicationContext webApplicationContext;
	

	@Value("${component.modelversion}")
	private String modelversion = "";


	@Value("${daps.url}")
	private String dapsUrl;

	@Value("${daps.keystore}")
	private String keyStoreFile;

	@Value("${daps.keystorePwd}")
	private String keyStorePwd;

	@Value("${daps.keystoreAlias}")
	private String keyStoreAlias;

	@Value("${daps.UUID}")
	private String dapsUUID;

	private @NotNull URI artifactURI;

    private final static String ARTIFACT_FILENAME = "demoArtifact.xml";
    private final static String ARTIFACT_DESCRIPTION_FILENAME = "demoArtifact-desc.jsonld";

    @Value("${artifact.directory}")
    private String artifactDir;
    
    

	/**
	 * 
	 * @throws Exception
	 */
	@Before
	public void setUp() throws Exception {


		logger.info("Copying demoArtifact.xml to " + artifactDir);
		InputStream artifact = this.getClass().getClassLoader().getResourceAsStream(ARTIFACT_FILENAME);

        //Files.copy(artifact, Paths.get(artifactDir, ARTIFACT_FILENAME).normalize(), StandardCopyOption.REPLACE_EXISTING);
        Files.copy(artifact, Paths.get(artifactDir, ARTIFACT_FILENAME).normalize());

		logger.info("Copying demoArtifact-desc.jsonld to " + artifactDir);
		
        InputStream artifact_desc = this.getClass().getClassLoader().getResourceAsStream(ARTIFACT_DESCRIPTION_FILENAME);
        //Files.copy(artifact_desc, Paths.get(artifactDir, ARTIFACT_DESCRIPTION_FILENAME).normalize(), StandardCopyOption.REPLACE_EXISTING);
        Files.copy(artifact_desc, Paths.get(artifactDir, ARTIFACT_DESCRIPTION_FILENAME).normalize());
        
        logger.info("Copying test resources to " + artifactDir + " finished.");
        
        // wait for the directory watcher to find the files
        Thread.sleep(10000);
        logger.info("Ready for testing.");
	}
	

    @After
    public void cleanUp() {
    	logger.info("Cleaning " + artifactDir);
    	
        Path artifactFile = Paths.get(artifactDir, ARTIFACT_FILENAME);
        artifactFile.toFile().delete();

        Path artifactDescFile = Paths.get(artifactDir, ARTIFACT_DESCRIPTION_FILENAME);
        artifactDescFile.toFile().delete();
        
    	logger.info("Cleaning " + artifactDir + " successful.");
    }
    
    
	@Test
	public void retriveArtifactDescriptionTest() throws Exception {
		DapsSecurityTokenProvider daps	= DapsSecurityTokenProviderGenerator.generate(dapsUrl,keyStoreFile,keyStorePwd,keyStoreAlias,dapsUUID);
		Message descriptionRequestMessage = new DescriptionRequestMessageBuilder()
				._issued_(CalendarUtil.now())
				._issuerConnector_(new URI("http://example.org"))
				._authorizationToken_(new TokenBuilder()
						._tokenValue_("dummy-token")
						._tokenFormat_(TokenFormat.JWT)
						.build())
				._senderAgent_(new URI("http://example.org"))
				._securityToken_(daps.getSecurityTokenAsDAT())
				._modelVersion_(modelversion)
				.build();
		URI requestMessageUri = descriptionRequestMessage.getId();
		logger.info(descriptionRequestMessage.toRdf());


		MockMultipartFile header = new MockMultipartFile("header", null, "application/json", descriptionRequestMessage.toRdf().getBytes());


		MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

		MvcResult result = mockMvc.perform(MockMvcRequestBuilders.multipart("/data/")
				.file(header))
				//.file(payload)
				.andExpect(status().isOk())
				.andDo(MockMvcResultHandlers.print())
				.andReturn();


		String resultMultipartMessage = result.getResponse().getContentAsString();
		InputStream message_body_stream = new ByteArrayInputStream(resultMultipartMessage.getBytes(Charset.defaultCharset()));


        MultiPartFormInputStream multiPartFormInputStream = new MultiPartFormInputStream(message_body_stream, result.getResponse().getContentType(), null, null);
        Part return_header = multiPartFormInputStream.getPart("header");
        Part return_payload = multiPartFormInputStream.getPart("payload");

        
		// Test the IDS message header
		Serializer serializer = new Serializer();
		StringWriter writer = new StringWriter();
		IOUtils.copy(return_header.getInputStream(), writer, Charset.defaultCharset());
		writer.close();
		String return_header_string = writer.toString();

		writer = new StringWriter();
		IOUtils.copy(return_payload.getInputStream(), writer, Charset.defaultCharset());
		writer.close();
		String return_payload_string = writer.toString();
		
		
		Message respondingMessage;
		Connector connector = null;
		try {
			respondingMessage = serializer.deserialize(return_header_string, DescriptionResponseMessage.class);
			connector = serializer.deserialize(return_payload_string, Connector.class);
			System.out.println("TEST RESULTS ----------------");

		} catch (InvalidTypeIdException e) {
			respondingMessage = serializer.deserialize(return_header_string, RejectionMessageImpl.class);
			e.printStackTrace();
		}

		// test the original request message URI
		assertEquals(true, respondingMessage.getCorrelationMessage().equals(requestMessageUri) );

		//URI resourceUri = new URI("http://iais.fraunhofer.de/test/resource/demoXML");
		//assertEquals(0, connector.getCatalog().getOffers().get(0).getId().compareTo(resourceUri));

		//List<String> keywords = Arrays.asList("test", "IDS", "demo");
		//assertTrue(keywords.contains(connector.getCatalog().getOffer().get(0).getContractOffer())));


		//URI representationUri = new URI("http://iais.fraunhofer.de/test/representation/demoXML");
		//assertEquals(0, connector.getCatalog().getOffers().get(0).getDefaultRepresentation().get(0).getId().compareTo(representationUri));
		//assertTrue(connector.getCatalog().getOffers().get(0).getDefaultRepresentation().get(0).getMediaType().equals(IANAMediaType.TEXT_XML));


		//URI artifactUri = new URI("http://iais.fraunhofer.de/test/artifact/demoXML");

		//Artifact artifact = (Artifact) connector.getCatalog().getOffer().get(0).getDefaultRepresentation().get(0).getInstance().get(0);
		//assertEquals(0, artifact.getId().compareTo(artifactUri));
		//assertEquals("demoArtifact.xml", artifact.getFileName());

	}

}
