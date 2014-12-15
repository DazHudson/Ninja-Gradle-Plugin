/**
 * Copyright (C) 2012-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ninja.plugin.gradle;

import com.sun.nio.file.SensitivityWatchEventModifier;
import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Directory Watcher, and input watcher to restart Ninja if required.
 * 
 * @author dhudson
 */
public class WatchAndRestartMachine {

    private static final long TIMEOUT = 500;

    private final SuperDevConfig theConfig;

    private final RunClassInSeparateJvmMachine ninjaJettyInsideSeparateJvm;

    private final WatchService watchService;

    private final DelayedRestartTrigger restartAfterSomeTimeAndChanges;

    private final Map<WatchKey, Path> mapOfWatchKeysToPaths;

    /**
     * Creates a WatchService and registers the path
     * @param config superdev confg
     * @throws java.io.IOException if unable to register paths
     */
    public WatchAndRestartMachine(SuperDevConfig config) throws IOException {
        theConfig = config;

        ninjaJettyInsideSeparateJvm = new RunClassInSeparateJvmMachine(config);

        restartAfterSomeTimeAndChanges
                = new DelayedRestartTrigger(
                        ninjaJettyInsideSeparateJvm);

        restartAfterSomeTimeAndChanges.start();

        watchService = FileSystems.getDefault().newWatchService();
        mapOfWatchKeysToPaths = new HashMap<>();

        registerAll(config.getDirsToWatch());
    }

    /**
     * Register the given path with the WatchService
     */
    private void register(Path path) throws IOException {

        ////!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
        //// USUALLY THIS IS THE DEFAULT WAY TO REGISTER THE EVENTS:
        ////!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
//        WatchKey watchKey = path.register(
//                watchService, 
//                ENTRY_CREATE, 
//                ENTRY_DELETE,
//                ENTRY_MODIFY);
        ////!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
        //// BUT THIS IS DAMN SLOW (at least on a Mac)
        //// THEREFORE WE USE EVENTS FROM COM.SUN PACKAGES THAT ARE WAY FASTER
        //// THIS MIGHT BREAK COMPATIBILITY WITH OTHER JDKs
        //// MORE: http://stackoverflow.com/questions/9588737/is-java-7-watchservice-slow-for-anyone-else
        ////!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
        WatchKey watchKey = path.register(
                watchService,
                new WatchEvent.Kind[]{
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_MODIFY,
                    StandardWatchEventKinds.ENTRY_DELETE
                },
                SensitivityWatchEventModifier.HIGH);

        mapOfWatchKeysToPaths.put(watchKey, path);
    }

    private void registerAll(final List<String> directories) throws IOException {
        for (String directory : directories) {
            registerDirectory(FileSystems.getDefault().getPath(
                    directory));
        }
    }

    private void registerDirectory(Path path) throws IOException {
        // register directory and sub-directories
        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path path,
                    BasicFileAttributes attrs)
                    throws IOException {
                register(path);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    void watch() {

        if (mapOfWatchKeysToPaths.isEmpty()) {
            try {
                // It is possible that there is nothing to watch in SuperHot mode, so lets just wait a bit
                Thread.sleep(TIMEOUT);
            } catch (InterruptedException ignore) {
            }
            return;
        }

        WatchKey watchKey;
        try {
            watchKey = watchService.poll(TIMEOUT, TimeUnit.MILLISECONDS);

            if (watchKey == null) {
                return;
            }
        } catch (InterruptedException interruptedException) {
            interruptedException.printStackTrace();
            return;
        }

        Path path = mapOfWatchKeysToPaths.get(watchKey);
        if (path == null) {
            System.err.println("WatchKey not recognized!!");
            return;
        }

        for (WatchEvent<?> watchEvent : watchKey.pollEvents()) {
            WatchEvent.Kind watchEventKind = watchEvent.kind();

            // TBD - provide example of how OVERFLOW watchEvent is handled
            if (watchEventKind == OVERFLOW) {
                continue;
            }

            // Context for directory entry watchEvent is the file name of entry
            WatchEvent<Path> ev = (WatchEvent<Path>) watchEvent;
            Path name = ev.context();
            Path child = path.resolve(name);

            // print out watchEvent
            //System.out.format("%s: %s\n", watchEvent.kind().name(), child);
            if (watchEventKind == ENTRY_MODIFY) {

                // we are not interested in events from parent directories...
                if (!child.toFile().isDirectory()) {

                    if (!checkIfMatchesPattern(theConfig.getExcludePatterns(), child.toFile().getAbsolutePath())) {
                        System.out.println(
                                "SuperDev Found file modification - triggering reload:  "
                                + child.toFile().getAbsolutePath());

                        restartAfterSomeTimeAndChanges.triggerRestart();
                    }
                }
            }

            // ninjaJettyInsideSeparateJvm.restartNinjaJetty();
            // if directory is created, then
            // register it and its sub-directories recursively
            if (watchEventKind == ENTRY_CREATE) {
                restartAfterSomeTimeAndChanges.triggerRestart();

                try {
                    if (Files.isDirectory(child, NOFOLLOW_LINKS)) {
                        registerDirectory(child);
                    }
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                    // ignore to keep sample readable
                }
            }
        }

        // reset watchKey and remove from set if directory no longer accessible
        boolean valid = watchKey.reset();
        if (!valid) {
            mapOfWatchKeysToPaths.remove(watchKey);
        }
    }

    void restart() {
        restartAfterSomeTimeAndChanges.triggerRestart();
    }

    private boolean checkIfMatchesPattern(List<String> regexPatterns, String string) {
        for (String regex : regexPatterns) {
            if (string.matches(regex)) {
                return true;
            }
        }

        return false;
    }

}
