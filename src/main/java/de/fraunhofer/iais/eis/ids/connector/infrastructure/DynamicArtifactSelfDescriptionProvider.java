package de.fraunhofer.iais.eis.ids.connector.infrastructure;

import de.fraunhofer.iais.eis.ids.component.core.SelfDescriptionProvider;
import de.fraunhofer.iais.eis.ids.connector.artifact.ArtifactIndex;

public interface DynamicArtifactSelfDescriptionProvider extends SelfDescriptionProvider {

    void setArtifacts(ArtifactIndex artifacts);
}
