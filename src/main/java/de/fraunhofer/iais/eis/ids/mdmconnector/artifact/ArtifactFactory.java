package de.fraunhofer.iais.eis.ids.mdmconnector.artifact;

import com.fasterxml.jackson.databind.type.TypeFactory;
import de.fraunhofer.iais.eis.Artifact;
import de.fraunhofer.iais.eis.ArtifactBuilder;
import de.fraunhofer.iais.eis.ArtifactImpl;
import de.fraunhofer.iais.eis.ids.component.core.util.CalendarUtil;
import de.fraunhofer.iais.eis.util.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.io.File;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.GregorianCalendar;

class ArtifactFactory {

    final private Logger logger = LoggerFactory.getLogger(ArtifactFactory.class);

    private URI componentId;

    public ArtifactFactory(URI componentId) {
        this.componentId = componentId;
    }

    Artifact createArtifact(File file) throws ConstraintViolationException, MalformedURLException {
        int fileSize = Math.toIntExact(file.length());
        XMLGregorianCalendar greg = CalendarUtil.now();
        try {
            GregorianCalendar gc = new GregorianCalendar();
            gc.setTimeInMillis(file.lastModified());
            greg = DatatypeFactory.newInstance().newXMLGregorianCalendar(gc);
        }
        catch (Exception e) {
        logger.warn("Error while creating artifact description with Gregorian Calendar");
        }
        Artifact artifact = new ArtifactBuilder(createArtifactUri(file.getName()))
                ._creationDate_(greg)
                ._fileName_(file.getName())
                ._byteSize_(new BigInteger(String.valueOf(fileSize)))
                .build();
        return artifact;
    }

    private URI createArtifactUri(String name) {
        String componentId = this.componentId.toString();
        if (!componentId.endsWith("/")) componentId += "/";
        String artifactId = componentId + "artifact/" + name;
        try {
            return new URI(artifactId);
        } catch (URISyntaxException e) {
            logger.error("Unable to create artifact URL", e);
            return null;
        }
    }

}
