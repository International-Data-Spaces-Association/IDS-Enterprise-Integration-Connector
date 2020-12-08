package de.fraunhofer.iais.eis.ids.mdmconnector.artifact;

import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.servlet.http.Part;
import javax.validation.constraints.NotNull;

import de.fraunhofer.iais.eis.*;
import de.fraunhofer.iais.eis.ids.component.ecosystemintegration.daps.DapsSecurityTokenProvider;
import de.fraunhofer.iais.eis.ids.mdmconnector.shared.DapsSecurityTokenProviderGenerator;
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
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import de.fraunhofer.iais.eis.ids.component.core.util.CalendarUtil;
import de.fraunhofer.iais.eis.ids.jsonld.Serializer;
import de.fraunhofer.iais.eis.ids.mdmconnector.main.Main;


/**
 * Testing the MDM connector with Spring MockMvc
 * 
 * @author sbader
 *
 */
@SpringBootTest(classes = Main.class)
@RunWith(SpringJUnit4ClassRunner.class)
//@RunWith(SpringRunner.class)
@AutoConfigureMockMvc
@WebAppConfiguration
public class SpringRequestArtifactTest {

	private Logger logger = LoggerFactory.getLogger(SpringRequestArtifactTest.class);

	@Autowired
	private WebApplicationContext webApplicationContext;
	

	@Value("${component.modelversion}")
	private String modelversion = "";
	

	private @NotNull URI artifactURI;

    private final static String ARTIFACT_FILENAME = "demoArtifact.xml";

    @Value("${artifact.directory}")
    private String artifactDir;

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

	/**
	 * create a dummy artifact and execute a first request which finds out the URI of it as 
	 * decided by the server.
	 * 
	 * @throws Exception
	 */
	@Before
	public void setUp() throws Exception {
		prepareArtifact();
		getSelfDescriptionTestAndGetArtifactUri();
	}
	

    @After
    public void cleanUp() {
        Path artifactFile = Paths.get(artifactDir, ARTIFACT_FILENAME);
        artifactFile.toFile().delete();
    }
    

    
    private void prepareArtifact() throws IOException {
        InputStream artifact = this.getClass().getClassLoader().getResourceAsStream(ARTIFACT_FILENAME);
        Files.copy(artifact, Paths.get(artifactDir, ARTIFACT_FILENAME).normalize());
    }



	public void getSelfDescriptionTestAndGetArtifactUri() throws Exception {
		DapsSecurityTokenProvider daps	= DapsSecurityTokenProviderGenerator.generate(dapsUrl,keyStoreFile,keyStorePwd,keyStoreAlias,dapsUUID);
		DescriptionRequestMessage sefDescriptionRequestMessage = new DescriptionRequestMessageBuilder(new URI("http://http://example.org/test/message1"))
				._issued_(CalendarUtil.now())
				._issuerConnector_(new URI("http://example.org"))
				._authorizationToken_(new TokenBuilder()
						._tokenValue_("dummy-token")
						._tokenFormat_(TokenFormat.JWT)
						.build())
				._modelVersion_(modelversion)
				._senderAgent_(new URI("http://example.org"))
				._securityToken_(daps.getSecurityTokenAsDAT())
				._modelVersion_(modelversion)
				.build();

		logger.info(sefDescriptionRequestMessage.toRdf());

		MockMultipartFile header = new MockMultipartFile("header", null, "application/json", sefDescriptionRequestMessage.toRdf().getBytes());
		logger.info(sefDescriptionRequestMessage.toRdf());

		MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();


		MvcResult result = mockMvc.perform(MockMvcRequestBuilders.multipart("/data/")
				.file(header))
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
		DescriptionResponseMessage connectorDescriptionMsg = serializer.deserialize(return_header_string, DescriptionResponseMessage.class);
		assertEquals(0, connectorDescriptionMsg.getCorrelationMessage().compareTo(new URI("http://http://example.org/test/message1")) );

		// Test the IDS message header
		serializer = new Serializer();
		writer = new StringWriter();
		IOUtils.copy(return_header.getInputStream(), writer, Charset.defaultCharset());
		writer.close();

		return_header_string = writer.toString();
		connectorDescriptionMsg = serializer.deserialize(return_header_string, DescriptionResponseMessage.class);

		//assertEquals(true, connectorDescriptionMsg.getCorrelationMessage().equals(new URL("http://http://example.org/test/message1")) );

		// Test the IDS message payload
		writer = new StringWriter();
		IOUtils.copy(return_payload.getInputStream(), writer, Charset.defaultCharset());
		String return_payload_string = writer.toString();
		BaseConnector connectorDescription = serializer.deserialize(return_payload_string, BaseConnectorImpl.class);
		if (connectorDescription.getResourceCatalog().get(0).getOfferedResource().get(0) != null) {
            artifactURI = connectorDescription.getResourceCatalog().get(0).getOfferedResource().get(0).getRepresentation().get(0).getInstance().get(0).getId();
        } else {
        	fail("At least the demoArtifact.xml artifact must be loaded!");
        }
	} 


	@Test
	public void testArtifactRequest() throws Exception {
		DapsSecurityTokenProvider daps	= DapsSecurityTokenProviderGenerator.generate(dapsUrl,keyStoreFile,keyStorePwd,keyStoreAlias,dapsUUID);
		Message artifactRequestMessage = new ArtifactRequestMessageBuilder()
				._requestedArtifact_(artifactURI)
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
		
		logger.info(artifactRequestMessage.toRdf());


		MockMultipartFile header = new MockMultipartFile("header", null, "application/json", artifactRequestMessage.toRdf().getBytes());

		MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();


		mockMvc.perform(MockMvcRequestBuilders.multipart("/data/")
				.file(header))
		.andExpect(status().isOk())
		.andDo(MockMvcResultHandlers.print());
	}
	


}
