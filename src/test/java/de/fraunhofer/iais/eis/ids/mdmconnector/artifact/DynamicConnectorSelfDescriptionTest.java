package de.fraunhofer.iais.eis.ids.mdmconnector.artifact;

import de.fraunhofer.iais.eis.Artifact;
import de.fraunhofer.iais.eis.ArtifactBuilder;
import de.fraunhofer.iais.eis.BaseConnectorImpl;
import de.fraunhofer.iais.eis.ids.mdmconnector.infrastructure.DynamicConnectorSelfDescription;
import de.fraunhofer.iais.eis.ids.mdmconnector.artifact.ArtifactIndex;
import de.fraunhofer.iais.eis.util.Util;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

public class DynamicConnectorSelfDescriptionTest {

	@Value("${component.url}")
	private String component = "";

	@Value("${component.maintainer}")
	private String maintainer = "";

	@Value("${component.modelversion}")
	private String modelversion = "";

	@Value("${server.port}")
	private String port = "";


    private DynamicConnectorSelfDescription selfDescription;

    @Before
    public void setUp() throws URISyntaxException, MalformedURLException {
        URI component = new URI(this.component);
        URI maintainer = new URI(this.maintainer);
        selfDescription = new DynamicConnectorSelfDescription(component, maintainer, modelversion);
    }

    @Test
    public void emptyArtifacts() {
        BaseConnectorImpl component = (BaseConnectorImpl) selfDescription.getSelfDescription();
        //Assert.assertTrue(component.getCatalog().getOffers().isEmpty());
        Assert.assertTrue(component.getResourceCatalog().get(0).getOfferedResource().size() >= 1);
    }

    @Test
    public void addArtifact() {
        BaseConnectorImpl component = (BaseConnectorImpl) selfDescription.getSelfDescription();
        //Assert.assertTrue(component.getCatalog().getOffers().isEmpty());
        Assert.assertTrue(component.getResourceCatalog().get(0).getOfferedResource().size() >= 1);

        Artifact hostedArtifact = new ArtifactBuilder().build();
        ArtifactIndex artifactIndex = new InMemoryArtifactIndex();
        artifactIndex.addArtifact(new File("testFile.txt"), hostedArtifact);
        selfDescription.setArtifacts(artifactIndex);

        component = (BaseConnectorImpl) selfDescription.getSelfDescription();
        Assert.assertEquals(1, component.getResourceCatalog().get(0).getOfferedResource().size());
    }
}
