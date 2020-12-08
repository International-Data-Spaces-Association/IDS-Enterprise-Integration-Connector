package de.fraunhofer.iais.eis.ids.mdmconnector.artifact;

import de.fraunhofer.iais.eis.Artifact;
import de.fraunhofer.iais.eis.Contract;
import de.fraunhofer.iais.eis.ContractAgreement;
import de.fraunhofer.iais.eis.Resource;
import de.fraunhofer.iais.eis.ids.mdmconnector.artifact.ArtifactIndex;
import net.minidev.json.JSONObject;

import javax.validation.constraints.NotNull;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class InMemoryArtifactIndex implements ArtifactIndex {

    private Map<File, Artifact> index = new HashMap<>();
    private Map<Artifact, Contract> contractIndex = new HashMap<>();
    private Map<Artifact, Resource> descriptionIndex = new HashMap<>();
    private Map<Artifact, JSONObject> negotiationIndex = new HashMap<>();
    private Map<Artifact, HashMap<URI, ContractAgreement>> signedContractIndex = new HashMap<Artifact, HashMap<URI, ContractAgreement>>();


    @Override
    public Optional<File> getArtifact(URI artifactUri) {
        return index.keySet().stream()
                .filter(file -> index.get(file).getId().equals(artifactUri))
                .findFirst();
    }

    @Override
    public @NotNull URI getArtifactId(File artifact) {
        return index.get(artifact).getId();
    }

    //@Override
    public Optional<File> getArtifact(URL artifactUrl) {
        return index.keySet().stream()
                .filter(file -> {
                    URI artifactURI = null;
                    try {
                        artifactURI = artifactUrl.toURI();
                    } catch (URISyntaxException e) {
                        e.printStackTrace();
                    }
                    return index.get(file).getId().equals(artifactURI);
                })
                .findFirst();
    }

    @Override
    public Optional<Artifact> getArtifactAsObject(URI artifactUri) {
        return index.values().stream()
                .filter(artifact -> artifact.getId().equals(artifactUri))
                .findFirst();
    }

    @Override
    public void addArtifact(File artifact, Artifact description) {
        index.put(artifact, description);
    }

    @Override
    public void removeArtifact(File artifact) {
        index.remove(artifact);
    }

    @Override
    public Collection<Artifact> getAllArtifacts() {
        return index.values();
    }

    @Override
    public boolean exists(File artifact) {
        return index.containsKey(artifact);
    }

    @Override
    public void addContract(Artifact artifact, Contract contract) {
        contractIndex.put(artifact, contract);
    }

    @Override
    public void removeContract(Artifact artifact) {
        contractIndex.remove(artifact);
    }

    @Override
    public boolean hasContract(Artifact artifact) {
        return contractIndex.containsKey(artifact);
    }

    @Override
    public Contract getContract(Artifact artifact) {
        return contractIndex.get(artifact);
    }

    @Override
    public void addNegotiation(Artifact artifact, JSONObject negotiation) {
        negotiationIndex.put(artifact, negotiation);
    }

    @Override
    public void removeNegotiation(Artifact artifact) {
        negotiationIndex.remove(artifact);
    }

    @Override
    public boolean hasNegotiation(Artifact artifact) {
        return negotiationIndex.containsKey(artifact);
    }

    @Override
    public JSONObject getNegotiation(Artifact artifact) {
        return negotiationIndex.get(artifact);
    }

    @Override
    public void addDescription(Artifact artifact, Resource description) {
        descriptionIndex.put(artifact, description);
    }

    @Override
    public void removeDescription(Artifact artifact) {
        descriptionIndex.remove(artifact);
    }

    @Override
    public boolean hasDescription(Artifact artifact) {
        return descriptionIndex.containsKey(artifact);
    }

    @Override
    public Resource getDescription(Artifact artifact) {
        return descriptionIndex.get(artifact);
    }

    @Override
    public void addSignedContract(Artifact artifact, URI otherParty, ContractAgreement contract) {
        if (signedContractIndex.containsKey(artifact)) {
            signedContractIndex.get(artifact).put(otherParty, contract);
        } else {
            signedContractIndex.put(artifact, new HashMap<>());
            signedContractIndex.get(artifact).put(otherParty, contract);
        }
    }

    @Override
    public void removeSignedContract(Artifact artifact, URI otherParty) {
        signedContractIndex.get(artifact).remove(otherParty);
    }

    @Override
    public boolean hasSignedContract(Artifact artifact, URI otherParty) {
        if (signedContractIndex.containsKey(artifact)) {
            if (signedContractIndex.get(artifact).containsKey(otherParty)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public ContractAgreement getSignedContract(Artifact artifact, URI otherParty) {
        if (signedContractIndex.containsKey(artifact)) {
            if (signedContractIndex.get(artifact).containsKey(otherParty)) {
                return signedContractIndex.get(artifact).get(otherParty);
            }
        }
        return null;
    }
}
