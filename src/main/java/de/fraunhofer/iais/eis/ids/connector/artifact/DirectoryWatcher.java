package de.fraunhofer.iais.eis.ids.connector.artifact;

import de.fraunhofer.iais.eis.ids.component.core.InfomodelFormalException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Collection;

public class DirectoryWatcher implements Runnable {

    final private Logger logger = LoggerFactory.getLogger(DirectoryWatcher.class);

    private WatchService watchService;
    private String watchDirectory;
    private Collection<ArtifactListener> listeners = new ArrayList<>();

    public DirectoryWatcher(String watchDirectory) throws IOException {
        this.watchDirectory = watchDirectory;
        watchService = FileSystems.getDefault().newWatchService();
        Path path = createDirIfNotExists(watchDirectory);
        path.register(watchService,
                StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_MODIFY,
                StandardWatchEventKinds.ENTRY_DELETE);

        logger.info("Watching directory '" + watchDirectory + "' for artifacts.");
    }

    private Path createDirIfNotExists(String watchDirectory) {
        Path path = Paths.get(watchDirectory);
        if (!path.toFile().exists()) path.toFile().mkdirs();
        return path;
    }

    @Override
    public void run() {
        WatchKey key;
        try {
            // runs until the program is shut down or access to the folder is revoked
            while (true) {
                key = this.watchService.take();
                for (WatchEvent<?> event : key.pollEvents()) {
                    File affectedFile = new File(watchDirectory + File.separator + event.context());
                    if (event.kind().equals(StandardWatchEventKinds.ENTRY_CREATE) && affectedFile.isFile()) {
                        listeners.forEach(listener -> {
                            try {
                                listener.notifyAdd(affectedFile);
                            } catch (InfomodelFormalException | IOException e) {
                                logger.error("Could not add new artifact information at broker.", e);
                                //e.printStackTrace();
                            }
                        });
                    }
                    else if (event.kind().equals(StandardWatchEventKinds.ENTRY_MODIFY) && affectedFile.isFile()) {
                        listeners.forEach(listener -> {
                            try {
                                listener.notifyChange(affectedFile);
                            } catch (InfomodelFormalException | IOException e) {
                                logger.error("Could not update new artifact information at broker.", e);
                                //e.printStackTrace();
                            }
                        });
                    }
                    else if (event.kind().equals(StandardWatchEventKinds.ENTRY_DELETE)) {
                            listeners.forEach(listener -> {
                                try {
                                    listener.notifyRemove(affectedFile);
                                } catch (InfomodelFormalException | IOException e) {
                                    logger.error("Could not delete artifact information at broker.", e);
                                    //e.printStackTrace();
                                }
                            });
                        }

                }
                boolean valid = key.reset(); //Checks the validity of the Key in case the system permits access.
                if (!valid){
                    break;
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.info("Watching for artifacts interrupted.");
        }
    }

    public void setArtifactListeners(Collection<ArtifactListener> listeners) {
        this.listeners = listeners;
        notifyListenersOfExistingFiles(listeners);
    }

    private void notifyListenersOfExistingFiles(Collection<ArtifactListener> listeners) {
        File dir = new File(watchDirectory);
        for (File oldFile : dir.listFiles()) {
            listeners.forEach(listener -> {
                try {
                    listener.notifyAdd(oldFile);
                } catch (InfomodelFormalException | IOException e) {
                    logger.error("Could not add existing artifact information to broker.", e);
                    e.printStackTrace();
                }
            });
        }
    }

}
