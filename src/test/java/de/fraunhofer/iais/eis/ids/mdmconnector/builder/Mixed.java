package de.fraunhofer.iais.eis.ids.mdmconnector.builder;

import de.fraunhofer.iais.eis.*;
import de.fraunhofer.iais.eis.ids.component.core.util.CalendarUtil;
import de.fraunhofer.iais.eis.ids.connector.infrastructure.DynamicConnectorSelfDescription;
import de.fraunhofer.iais.eis.util.TypedLiteral;
import de.fraunhofer.iais.eis.util.Util;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Testing Builder Classes within the Connector
 * An Error after an Update of the Components, implies possible Invalidity
 * of Messages, Contracts Descriptions
 * @author dkubitza
 *
 */

public class Mixed {

	private static final Logger logger = LoggerFactory.getLogger(Mixed.class);


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
	public void MessageBuilders () throws URISyntaxException {

		//Broker - Connector Messages
		ConnectorUnavailableMessage mess2 = null;
		ConnectorUpdateMessage mess4 = null;

		ResourceUpdateMessage mess6 = null;
		ResourceUnavailableMessage mess7 = null;

		RejectionMessage mess8 = null;
		ResponseMessage mess9 = null;
		ResultMessage mess11 = null;
		MessageProcessedNotificationMessage mess12 = null;

		QueryMessage mess10 = null;
		DescriptionRequestMessage mess40 = null;
		DescriptionResponseMessage mess41 = null;

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


		mess4 = new ConnectorUpdateMessageBuilder()
				._affectedConnector_(new URI("connector-that-is-registered.uri"))
				._recipientConnector_(Util.asList(new URI("receipient-connector.uri")))
				._recipientAgent_(Util.asList(new URI("receipient-agent.uri")))
				._issuerConnector_(new URI("connector-that-registers.uri"))
				._senderAgent_(new URI("Person-that-registers.uri"))
				._securityToken_(new DynamicAttributeTokenBuilder()
						._tokenFormat_(TokenFormat.JWT)
						._tokenValue_("some-token-value")
						.build())
				._modelVersion_(modelversion)
				._issued_(CalendarUtil.now())
				.build();


		mess10 = new QueryMessageBuilder()
				._issuerConnector_(new URI("connector-that-issues.uri"))
				._senderAgent_(new URI("Person-that-registers.uri"))
				._securityToken_(new DynamicAttributeTokenBuilder()
						._tokenFormat_(TokenFormat.JWT)
						._tokenValue_("some-token-value")
						.build())
				._modelVersion_(modelversion)
				._issued_(CalendarUtil.now())
				.build();


		mess11 = new ResultMessageBuilder()
				._issuerConnector_(new URI("connector-that-issues.uri"))
				._senderAgent_(new URI("Person-that-registers.uri"))
				._securityToken_(new DynamicAttributeTokenBuilder()
						._tokenFormat_(TokenFormat.JWT)
						._tokenValue_("some-token-value")
						.build())
				._modelVersion_(modelversion)
				._issued_(CalendarUtil.now())
				._correlationMessage_(new URI("correlated-message.uri"))
				.build();

		mess22 = new DescriptionRequestMessageBuilder()
				._requestedElement_(new URI("requested_Element.uri"))
				._issuerConnector_(new URI("connector-that-issues.uri"))
				._senderAgent_(new URI("Person-that-registers.uri"))
				._securityToken_(new DynamicAttributeTokenBuilder()
						._tokenFormat_(TokenFormat.JWT)
						._tokenValue_("some-token-value")
						.build())
				._modelVersion_(modelversion)
				._issued_(CalendarUtil.now())
				.build();


		mess15 = new ArtifactRequestMessageBuilder()
				._issuerConnector_(new URI("connector-that-registers.uri"))
				._senderAgent_(new URI("Person-that-registers.uri"))
				._recipientConnector_(Util.asList(new URI("receipient-connector.uri")))
				._recipientAgent_(Util.asList(new URI("receipient-agent.uri")))
				._securityToken_(new DynamicAttributeTokenBuilder()
						._tokenFormat_(TokenFormat.JWT)
						._tokenValue_("some-token-value")
						.build())
				._modelVersion_(modelversion)
				._requestedArtifact_(new URI("resource-that-was-registerd.uri"))
				._issued_(CalendarUtil.now())
				.build();

		mess16 = new ArtifactResponseMessageBuilder()
				._issuerConnector_(new URI("connector-that-issues.uri"))
				._senderAgent_(new URI("Person-that-registers.uri"))
				._securityToken_(new DynamicAttributeTokenBuilder()
						._tokenFormat_(TokenFormat.JWT)
						._tokenValue_("some-token-value")
						.build())
				._modelVersion_(modelversion)
				._issued_(CalendarUtil.now())
				._correlationMessage_(new URI("correlated-message.uri"))
				.build();

		//mess17 = new ContractOfferMessageBuilder().build();
		//mess18 = new ContractRequestMessageBuilder().build();
		//mess19 = new ContractResponseMessageBuilder().build();
		//mess20 = new ContractAgreementMessageBuilder().build();
		//mess21 = new ContractRejectionMessageBuilder().build();

		assert (true);

	}


	@Test
	public void ResourceBuilders () throws URISyntaxException{
		Artifact exampleArtifact = new ArtifactBuilder()._fileName_("EM_FCD_UI_City.csv ").build();

		Resource test1 = new ResourceBuilder()
				._title_(Util.asList(new TypedLiteral("FCD Data - current Status")))
				._description_(Util.asList(new TypedLiteral("FCD Data of the last 15 minutes.")))
				._contentType_(ContentType.SCHEMA_DEFINITION)
				._language_(Util.asList(Language.DE))
				._keyword_(Util.asList(new TypedLiteral("MobiDS")))
				/*._resourceEndpoint_(Util.asList(new StaticEndpointBuilder()
								._path_("a Static endpoint path")
								._inboundPath_("a static inbound path")
								._outboundPath_("a static outbound path")
								._endpointHost_(new HostBuilder()._accessUrl_(new URI("http://staticAccessURL.com"))._pathPrefix_("pathprefix")._protocol_(Protocol.HTTP).build())
								._endpointArtifact_(new ArtifactBuilder()._byteSize_(150)._checkSum_("artifact checksum")._fileName_("staticfile.txt").build())
							.build()))*/
				//._version_("1.-a%!_?#\\/§\"'*~´^`") //Does not pass validation, probably due to the \". Who knows
				.build();


		Resource test2 = new ResourceBuilder()
				._title_(Util.asList(new TypedLiteral("FCD Data - Historic Entries")))
				._description_(Util.asList(new TypedLiteral("Collection of Historic FCD Data - Accessible as Map")))
				._contentType_(ContentType.SCHEMA_DEFINITION)
				._language_(Util.asList(Language.DE))
				//._version_("1.-a%!_?#\\/§\"'*~´^`") //Does not pass validation, probably due to the \". Who knows
				.build();

		System.out.println(test2.toRdf());
		assert(true);

	}

	@Test
	public void ContractBuilders() throws IOException, URISyntaxException {

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

		ContractOffer test0 = new ContractOfferBuilder()
				._permission_(Util.asList(new PermissionBuilder()
						._target_(exampleArtifact.getId())
						._assignee_(Util.asList(Consumer.getId()))
						._assigner_(Util.asList(Provider.getId()))
						._action_(Util.asList(Action.USE))
						.build()))
				._contractStart_(CalendarUtil.now())
				.build();

		System.out.println(test0.toRdf());
		ContractAgreement test1 = new ContractAgreementBuilder()
				._consumer_(Consumer.getId())
				._contractStart_(CalendarUtil.now())
				._permission_(Util.asList(new PermissionBuilder()
						._target_(exampleArtifact.getId())
						._assignee_(Util.asList(Consumer.getId()))
						._assigner_(Util.asList(Provider.getId()))
						._action_(Util.asList(Action.USE))
						.build()))
				._prohibition_(Util.asList(new ProhibitionBuilder()
						._target_(exampleArtifact.getId())
						._assignee_(Util.asList(Consumer.getId()))
						._assigner_(Util.asList(Provider.getId()))
						._action_(Util.asList(Action.DISTRIBUTE))
						.build()))
				._provider_(Provider.getId())
				.build();

		System.out.println(test1.toRdf());


		ContractOffer test2 = new ContractOfferBuilder()
				._consumer_(Consumer.getId())
				._permission_(Util.asList(new PermissionBuilder()
						._target_(exampleArtifact.getId())
						._assignee_(Util.asList(Consumer.getId()))
						._assigner_(Util.asList(Provider.getId()))
						._action_(Util.asList(Action.USE))
						._postDuty_(Util.asList(new DutyBuilder()
								._action_(Util.asList(Action.ANONYMIZE))
								._constraint_(Util.asList(new ConstraintBuilder()
										._leftOperand_(LeftOperand.ABSOLUTE_SPATIAL_POSITION)
										._operator_(BinaryOperator.INSIDE)
										._rightOperandReference_(new URI("https://www.wikidata.org/wiki/Q3308569"))
										.build()))
								.build()))
						.build()))
				._provider_(Provider.getId())
				.build();

		System.out.println(test2.toRdf());

		ContractOffer test3 = new ContractOfferBuilder()
				._consumer_(Consumer.getId())
				._permission_(Util.asList(new PermissionBuilder()
								._target_(exampleArtifact.getId())
								._assignee_(Util.asList(Consumer.getId()))
								._assigner_(Util.asList(Provider.getId()))
								._action_(Util.asList(Action.USE))
								._constraint_(Util.asList(new ConstraintBuilder()
												._leftOperand_(LeftOperand.ABSOLUTE_SPATIAL_POSITION)
												._operator_(BinaryOperator.INSIDE)
												._rightOperandReference_(new URI("https://www.wikidata.org/wiki/Q3308569"))
												.build()
										)
								)
								.build()
						)
				)
				._provider_(Provider.getId())
				.build();


		System.out.println(test3.toRdf());
		assert(true);
	}

}
