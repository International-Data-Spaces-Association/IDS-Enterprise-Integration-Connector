package de.fraunhofer.iais.eis.ids.mdmconnector.contract;

import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import javax.servlet.http.Part;
import javax.validation.constraints.NotNull;

import de.fraunhofer.iais.eis.*;
import de.fraunhofer.iais.eis.ids.component.ecosystemintegration.daps.DapsSecurityTokenProvider;
import de.fraunhofer.iais.eis.ids.mdmconnector.daps.DapsRetrievalTest;
import de.fraunhofer.iais.eis.ids.mdmconnector.shared.DapsSecurityTokenProviderGenerator;
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
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
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
import de.fraunhofer.iais.eis.ids.mdmconnector.artifact.SpringRequestArtifactTest;
import de.fraunhofer.iais.eis.ids.mdmconnector.main.Main;
import de.fraunhofer.iais.eis.util.ConstraintViolationException;



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
public class ContractRequestTest {
	private Logger logger = LoggerFactory.getLogger(ContractRequestTest.class);

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
	 * 
	 * @throws Exception
	 */
	@Before
	public void setUp() throws Exception {
		// prepare
	}
	

    @After
    public void cleanUp() {
        // clean up
    }
    
    
	@Test
	public void testNonExisitingContract() throws Exception {
		DapsSecurityTokenProvider daps	= DapsSecurityTokenProviderGenerator.generate(dapsUrl,keyStoreFile,keyStorePwd,keyStoreAlias,dapsUUID);
		URI contractRequestMessageUri = new URI("http://http://example.org/test/contract-request-message-1");
		ContractRequestMessage contractRequestMessage = new ContractRequestMessageBuilder(contractRequestMessageUri)
				._issued_(CalendarUtil.now())
				._issuerConnector_(new URI("http://example.org"))
				._authorizationToken_(new TokenBuilder()
						._tokenValue_("dummy-token")
						._tokenFormat_(TokenFormat.JWT)
						.build())
				._modelVersion_(modelversion)
				._senderAgent_(new URI("http://example.org"))
				._securityToken_(daps.getSecurityTokenAsDAT())
				.build();

		logger.info(contractRequestMessage.toRdf());
		
		
		URI requestContractUri = new URI("http://http://example.org/test/contract-request-1");
		ContractRequest requestContract = new ContractRequestBuilder(requestContractUri)
				._consumer_(new URI("http://example.org/me"))
				._provider_(new URI("http://example.org/you"))
				._permission_(Util.asList(new PermissionBuilder()
						._action_(Util.asList(Action.READ))
						._target_(new URI("http://example.org/nonExistingArtifact"))
						.build()))
				.build();
		logger.info(requestContract.toRdf());
		

		MockMultipartFile header = new MockMultipartFile("header", null, "application/json", contractRequestMessage.toRdf().getBytes());
		MockMultipartFile payload = new MockMultipartFile("payload", null, "application/json", requestContract.toRdf().getBytes());


		MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();


		MvcResult result = mockMvc.perform(MockMvcRequestBuilders.multipart("/data/")
				.file(header)
				.file(payload))
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
		
		
		RejectionMessage respondingMessage;
		try {
			respondingMessage = serializer.deserialize(return_header_string, RejectionMessage.class);
			System.out.println(respondingMessage.getRejectionReason().getSerializedId().toString());
			System.out.println(RejectionReason.NOT_FOUND.toString());
			assertTrue( respondingMessage.getRejectionReason().getSerializedId().toString().contains("NOT_FOUND"));
		} catch (InvalidTypeIdException e) {
		}

		// test the original request message URI


	}

}
