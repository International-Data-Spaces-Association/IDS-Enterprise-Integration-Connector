package de.fraunhofer.iais.eis.ids.mdmconnector.artifact;

import org.apache.commons.io.FileUtils;
import org.junit.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class DirectoryWatcherTest {

    private static String tmpDir;
    private DirectoryWatcher watcher;
    private ExecutorService executor;
    private LoggingArtifactListener listener;

    private class LoggingArtifactListener implements ArtifactListener {

        Collection<String> addedFiles = new ArrayList<>();
        Collection<String> removedFiles = new ArrayList<>();

        @Override
        public void notifyAdd(File artifact) {
            addedFiles.add(artifact.getName());
        }

        @Override
        public void notifyRemove(File artifact) {
            removedFiles.add(artifact.getName());
        }
    }

    @BeforeClass
    public static void setUp() {
        tmpDir = "/tmp/" +System.currentTimeMillis();
        File dir = new File(tmpDir);
        dir.mkdir();
    }

    @AfterClass
    public static void cleanUp() throws IOException {
        FileUtils.deleteDirectory(new File(tmpDir));
    }

    @Before
    public void init() throws IOException {
        watcher = new DirectoryWatcher(tmpDir);
        listener = new LoggingArtifactListener();
        watcher.setArtifactListeners(Arrays.asList(listener));

        executor = Executors.newSingleThreadExecutor();
        executor.submit(watcher);
    }

    @Test
    public void watchAsync() throws InterruptedException, IOException {
        File newFile = new File(tmpDir + File.separator + "newFile.txt");
        newFile.createNewFile();
        executor.awaitTermination(1, TimeUnit.SECONDS);
        executor.shutdownNow();

        Assert.assertEquals(1, listener.addedFiles.size());
    }

}
