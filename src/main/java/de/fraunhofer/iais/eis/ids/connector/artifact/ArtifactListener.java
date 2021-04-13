package de.fraunhofer.iais.eis.ids.connector.artifact;

import de.fraunhofer.iais.eis.ids.component.core.InfomodelFormalException;

import java.io.File;
import java.io.IOException;

public interface ArtifactListener {

    void notifyAdd(File artifact) throws InfomodelFormalException, IOException;

    void notifyRemove(File artifact) throws InfomodelFormalException, IOException;

    void notifyChange(File artifact) throws InfomodelFormalException, IOException;


}
