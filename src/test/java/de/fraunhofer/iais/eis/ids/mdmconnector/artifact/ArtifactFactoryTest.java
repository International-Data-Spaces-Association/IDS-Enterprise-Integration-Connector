package de.fraunhofer.iais.eis.ids.mdmconnector.artifact;

import de.fraunhofer.iais.eis.Artifact;
import de.fraunhofer.iais.eis.util.ConstraintViolationException;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

public class ArtifactFactoryTest {

	@Value("${component.url}")
	private String component = "";

	@Value("${component.maintainer}")
	private String maintainer = "";

	@Value("${component.modelversion}")
	private String modelversion = "";

	@Value("${server.port}")
	private String port = "";

	
    @Test
    public void artifactIsValid() throws URISyntaxException, ConstraintViolationException, MalformedURLException {
        ArtifactFactory artifactFactory = new ArtifactFactory(new URI(component));
        File someFile = new File(this.getClass().getClassLoader().getResource("logback.xml").toURI());
        Artifact artifact = artifactFactory.createArtifact(someFile);
    }

    @Test
    public void validArtifactURL() throws URISyntaxException, ConstraintViolationException, MalformedURLException {
        ArtifactFactory artifactFactory = new ArtifactFactory(new URI(component));
        Artifact artifact = artifactFactory.createArtifact(
                new File(this.getClass().getClassLoader().getResource("logback.xml").toURI()));

        Assert.assertEquals(-1, artifact.getId().toString().indexOf("//", 6));

        artifactFactory = new ArtifactFactory(new URI(component));
        artifact = artifactFactory.createArtifact(
                new File(this.getClass().getClassLoader().getResource("logback.xml").toURI()));
        Assert.assertEquals(-1, artifact.getId().toString().indexOf("//", 6));
    }

}
