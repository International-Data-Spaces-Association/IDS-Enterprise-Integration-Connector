package de.fraunhofer.iais.eis.ids.mdmconnector.daps;

import de.fraunhofer.iais.eis.ids.mdmconnector.main.Main;
import de.fraunhofer.iais.eis.ids.mdmconnector.shared.DapsSecurityTokenProviderGenerator;
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
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.Assert.*;


/**
 * Testing the DAP-Token generation and provding functions for the other tests
 * 
 * @author cmader, dkubitza
 *
 */

@SpringBootTest(classes = Main.class)
@RunWith(SpringJUnit4ClassRunner.class)
//@RunWith(SpringRunner.class)
@AutoConfigureMockMvc
@WebAppConfiguration
public class DapsRetrievalTest {
	private Logger logger = LoggerFactory.getLogger(DapsRetrievalTest.class);

	@Autowired
	private WebApplicationContext webApplicationContext;

	@Value("${component.modelversion}")
	private String modelversion = "";
	
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

    @Before
	public void setUp() throws Exception {
	}

    @After
    public void cleanUp() {
    }

	@Test
	public void testDapsGeneration() throws Exception {
		assertNotNull(DapsSecurityTokenProviderGenerator.generate(dapsUrl,keyStoreFile,keyStorePwd,keyStoreAlias,dapsUUID));

	}

}
