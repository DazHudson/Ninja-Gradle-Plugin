package ninja.plugin.gradle;

import java.io.File;
import java.io.IOException;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

/**
 * Gradle Task to launch Ninja in Superdev mode
 *
 * @author dhudson
 */
public class SuperDevTask extends DefaultTask {

    /**
     * Class path entries from Gradle
     */
    public File[] scanDirs;
    /**
     * Port for Ninja to listen on
     */
    public int port;

    /**
     * Servlet context path
     */
    public String contextPath;

    /**
     * If set to true the build resource folder will be watched as well.
     */
    public boolean watchResources;

    /**
     * If set to true, try and hot swap instead of re-loading
     */
    public boolean hotswapEnable;

    /**
     * specify the debug port, defaults to 5006
     */
    public int debugPort = 5006;

    /**
     * switch on and off the debug, default to debugEnabled
     */
    public boolean debugEnabled = true;

    /**
     * Wait for the debugger to attach
     */
    public boolean suspend = false;
    
//    private final String [] DEFAULT_EXCLUDE_PATTERNS = {
//            "(.*)" + Pattern.quote(File.separator) + NinjaConstant.VIEWS_DIR + Pattern.quote(File.separator) + "(.*)ftl\\.html$",
//            "(.*)" + Pattern.quote(File.separator) + AssetsController.ASSETS_DIR + Pattern.quote(File.separator) + "(.*)"
//        };
    @TaskAction
    public void javaTask() {

        SuperDevConfig config = new SuperDevConfig();
        config.setPort(port);
        config.setContextPath(contextPath);
        config.setDebugPort(debugPort);
        config.setDebugging(debugEnabled);
        config.setSuspend(suspend);

        // Get path of the root gradle project
        File rootDir = getProject().getRootDir();
        String rootPath = rootDir.getPath();

        String path;
        int index;

        String buildLibs = "build" + File.separator + "libs";

        for (File file : scanDirs) {
            path = file.getPath();

            if (hotswapEnable) {
                if (path.contains("springloaded")) {
                    config.setSpringLoadedJar(path);
                    continue;
                }
            }

            if (path.startsWith(rootPath)) {
                // Its a project
                index = path.indexOf(buildLibs);
                if (index > 0) {
                    String buildDir = path.substring(0, index - 1);

                    String classDir = buildDir + File.separator + "build" + File.separator + "classes" + File.separator + "main";
                    config.addClasspathItem(classDir);

                    if (!hotswapEnable) {
                        // Watch the classes
                        addToWatchIfExists(classDir, config);
                    }

                    String resourceDir = buildDir + File.separator + "build" + File.separator + "resources" + File.separator + "main";
                    config.addClasspathItem(resourceDir);

                    if (watchResources) {
                        addToWatchIfExists(resourceDir, config);
                    }

                } else {
                    // It's sub project, so lets just add it
                    if (path.contains("classes")) {
                        if (!hotswapEnable) {
                            addToWatchIfExists(path, config);
                        }
                    }

                    if (path.contains("resources")) {
                        if (watchResources) {
                            addToWatchIfExists(path, config);
                        }
                    }

                    config.addClasspathItem(path);
                }

                continue;
            }

            // Its just a library
            config.addClasspathItem(path);
        }

        try {

            WatchAndRestartMachine watchAndTerminate = new WatchAndRestartMachine(config);

            System.out.println("------------------------------------------------------------------------");
            System.out.println("Launching SuperDevMode with '" + contextPath + "' context path.");
            System.out.println("Yoda Will launch on the following port: " + port);
            System.out.println("Hot swap enabled [" + hotswapEnable + "]");
            System.out.println("Java remote debug enabled [" + config.isDebuggingEnabled() + "]" + (config.isDebuggingEnabled() ? " on port [" + config.getDebugPort() + "]" : ""));
            if (!config.getDirsToWatch().isEmpty()) {
                for (String dirToWatch : config.getDirsToWatch()) {
                    System.out.println("Watching " + dirToWatch);
                }
            }
            System.out.println("Press any key to load the server if required");
            System.out.println("------------------------------------------------------------------------");

            for (;;) {
                watchAndTerminate.watch();
                if (System.in.available() > 0) {
                    // A key has been pressed..
                    System.out.println("SuperDev Restarting due to key press");
                    // Consume the input 
                    while (System.in.available() > 0) {
                        System.in.read();
                    }

                    watchAndTerminate.restart();
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void addToWatchIfExists(String path, SuperDevConfig config) {
        File file = new File(path);
        if (file.exists()) {
            config.addDirectoryToWatchList(path);
        }
    }
}
