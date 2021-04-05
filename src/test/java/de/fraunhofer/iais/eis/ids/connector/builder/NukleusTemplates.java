package de.fraunhofer.iais.eis.ids.connector.builder;

import de.fraunhofer.iais.eis.*;
import de.fraunhofer.iais.eis.ids.component.core.util.CalendarUtil;
import de.fraunhofer.iais.eis.util.Util;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.web.servlet.MockMvc;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Testing Builder Classes within the Connector
 * An Error after an Update of the Components, implies possible Invalidity
 * of Messages, Contracts Descriptions
 * @author dkubitza
 *
 */

public class NukleusTemplates {
	
	private static final Logger logger = LoggerFactory.getLogger(NukleusTemplates.class);


	@Value("${component.url}")
	private String component = "";

	@Value("${component.maintainer}")
	private String maintainer = "";

	@Value("${component.modelversion}")
	private String modelversion = "";

	@Value("${server.port}")
	private String port = "";


    @Autowired
    private MockMvc mvc;

	@Test
	public void MessageBuilders () throws URISyntaxException {
		//Connector - Connector Messages
		ResponseMessage mess13 = null;
		RejectionMessage mess14 = null;
		ArtifactRequestMessage mess15 = null;
		ArtifactResponseMessage mess16 = null;
		ContractOfferMessage mess17 = null;
		ContractRequestMessage mess18 = null;
		ContractResponseMessage mess19 = null;
		ContractAgreementMessage mess20 = null;
		ContractRejectionMessage mess21 = null;
		DescriptionRequestMessage mess22 = null;
		DescriptionResponseMessage mess23 = null;
		MessageProcessedNotificationMessage mess24 = null;

		//Create an Example Provider
		Participant Provider = new ParticipantBuilder(new URI("http://UrbanSoftwareInstitute.mobids.de"))
				._corporateHomepage_(new URI("https://www.ui.city/us/"))
				._corporateEmailAddress_(Util.asList("markus.bachleitner@ui.city"))
				.build();

		//Create an Example Consumer
		Participant Consumer = new ParticipantBuilder(new URI("www.example.org/YourURI"))
				._corporateHomepage_(new URI("www.example.org/ExampleConsumer"))
				._corporateEmailAddress_(Util.asList("mail@example.org"))
				.build();
		//Create an Example Artifact
		Artifact exampleArtifact = new ArtifactBuilder()._fileName_("EM_FCD_UI_City.csv ").build();

		//Create an Example Contract

		ContractAgreement test0 = new ContractAgreementBuilder()
				._permission_(Util.asList(new PermissionBuilder()
						._target_(exampleArtifact.getId())
						._assignee_(Util.asList(Consumer.getId()))
						._assigner_(Util.asList(Provider.getId()))
						._action_(Util.asList(Action.USE))
						.build()))
				._contractStart_(CalendarUtil.now())
				.build();

		assert(true);
	}

}
