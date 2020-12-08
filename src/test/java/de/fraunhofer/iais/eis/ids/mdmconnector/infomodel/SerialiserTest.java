package de.fraunhofer.iais.eis.ids.mdmconnector.infomodel;

import de.fraunhofer.iais.eis.ArtifactRequestMessage;
import de.fraunhofer.iais.eis.Message;
import de.fraunhofer.iais.eis.ids.jsonld.Serializer;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

import static org.junit.Assert.assertNotNull;

public class SerialiserTest {


	@Test
	public void testArtifactRequestMessage() throws IOException {

		String messageString  = readResourceToString("ArtifactRequestMessage.jsonld");

		Serializer serializer = new Serializer();
		//serializer.addPreprocessor(new TypeNamePreprocessor());

		ArtifactRequestMessage message = (ArtifactRequestMessage) serializer.deserialize(messageString, Message.class);
		assertNotNull(message.getRequestedArtifact());

		//logger.info(serializer.serialize(message));
		serializer.serialize(message);
	}

	public static String readResourceToString(String resourceName) throws IOException {
		ClassLoader classloader = Thread.currentThread().getContextClassLoader();
		InputStream is = classloader.getResourceAsStream(resourceName);
		StringWriter writer = new StringWriter();
		IOUtils.copy(is, writer, "UTF-8");
		return writer.toString();
	}

	public static String stripWhitespaces(String input) {
		return input.replaceAll("\\s+", "");
	}
}
