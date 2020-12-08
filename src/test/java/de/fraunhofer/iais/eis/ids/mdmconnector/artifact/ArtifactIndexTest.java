package de.fraunhofer.iais.eis.ids.mdmconnector.artifact;

import de.fraunhofer.iais.eis.Artifact;
import de.fraunhofer.iais.eis.ArtifactBuilder;
import de.fraunhofer.iais.eis.ids.mdmconnector.artifact.ArtifactIndex;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.util.Collection;

public class ArtifactIndexTest {

    private ArtifactIndex index = new InMemoryArtifactIndex();

    @Test
    public void addAndRetrieve() {
        File file = new File("dummyFile.txt");
        Artifact artifact = new ArtifactBuilder().build();

        index.addArtifact(file, artifact);
        Assert.assertTrue(index.getArtifact(artifact.getId()).isPresent());
    }

    @Test
    public void addRemoveAndRetrieve() {
        File file = new File("dummyFile.txt");
        Artifact artifact = new ArtifactBuilder().build();

        index.addArtifact(file, artifact);
        index.removeArtifact(file);
        Assert.assertFalse(index.getArtifact(artifact.getId()).isPresent());
    }

    @Test
    public void getAll() {
        File file1 = new File("dummyFile.txt");
        Artifact artifact1 = new ArtifactBuilder().build();

        File file2 = new File("dummyFile2.txt");
        Artifact artifact2 = new ArtifactBuilder().build();

        index.addArtifact(file1, artifact1);
        index.addArtifact(file2, artifact2);

        Collection<Artifact> allArtifacts = index.getAllArtifacts();
        Assert.assertEquals(2, allArtifacts.size());
    }


    @Test
    public void noDuplicateEntries() {
        File file = new File("dummyFile.txt");
        Artifact artifact1 = new ArtifactBuilder().build();
        Artifact artifact2 = new ArtifactBuilder().build();

        index.addArtifact(file, artifact1);
        index.addArtifact(file, artifact2);

        Collection<Artifact> allArtifacts = index.getAllArtifacts();

        Assert.assertEquals(1, allArtifacts.size());
        Assert.assertEquals(artifact2.getId(), allArtifacts.iterator().next().getId());
    }



}
