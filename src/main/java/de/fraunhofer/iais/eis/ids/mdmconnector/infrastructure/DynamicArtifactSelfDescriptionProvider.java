package de.fraunhofer.iais.eis.ids.mdmconnector.infrastructure;

import de.fraunhofer.iais.eis.ids.component.core.SelfDescriptionProvider;
import de.fraunhofer.iais.eis.ids.mdmconnector.artifact.ArtifactIndex;

public interface DynamicArtifactSelfDescriptionProvider extends SelfDescriptionProvider {

    void setArtifacts(ArtifactIndex artifacts);
}
