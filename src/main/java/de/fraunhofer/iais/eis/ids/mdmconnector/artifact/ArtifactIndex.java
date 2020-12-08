package de.fraunhofer.iais.eis.ids.mdmconnector.artifact;

import de.fraunhofer.iais.eis.Artifact;
import de.fraunhofer.iais.eis.Contract;
import de.fraunhofer.iais.eis.ContractAgreement;
import de.fraunhofer.iais.eis.Resource;
import de.fraunhofer.iais.eis.ids.connector.commons.artifact.ArtifactFileProvider;
import net.minidev.json.JSONObject;

import java.io.File;
import java.net.URI;
import java.util.Collection;
import java.util.Optional;

public interface ArtifactIndex extends ArtifactFileProvider {

    Optional<File> getArtifact(URI artifactUri);
	URI getArtifactId(File artifact);
    Optional<Artifact> getArtifactAsObject(URI artifactUri);
    Collection<Artifact> getAllArtifacts();

    void addArtifact(File artifact, Artifact description);
    void removeArtifact(File artifact);
    boolean exists(File artifact);

    void addContract(Artifact artifact, Contract contract);
    void removeContract(Artifact artifact);
    boolean hasContract(Artifact artifact);
    Contract getContract(Artifact artifact);

    void addNegotiation(Artifact artifact, JSONObject negotiation);
    void removeNegotiation(Artifact artifact);
    boolean hasNegotiation(Artifact artifact);
    JSONObject getNegotiation(Artifact artifact);


    void addSignedContract(Artifact artifact, URI otherParty, ContractAgreement contract);
    void removeSignedContract(Artifact artifact, URI otherParty);
    boolean hasSignedContract(Artifact artifact, URI otherParty);
    ContractAgreement getSignedContract(Artifact artifact, URI otherParty);

    void addDescription(Artifact artifact, Resource description);
    void removeDescription(Artifact artifact);
    boolean hasDescription(Artifact artifact);
    Resource getDescription(Artifact artifact);
    
}
