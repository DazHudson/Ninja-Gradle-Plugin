package ninja.plugin.gradle;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple POJO to hold super dev config
 *
 * @author dhudson - 13-Aug-2014
 * @since 1.0
 */
class SuperDevConfig {

    private String theClassNameWithMainToRun = "ninja.standalone.NinjaJetty";
    private final List<String> theDirsToWatch;
    private final List<String> theClasspathItems;
    private final List<String> theExcludeRegexPatterns;
    private String theContextPath;
    private int thePort;
    private String theSpringLoadedJar;
    private boolean enableDebugggin = true;
    private int debugPort;
    
    // Wait for the debugger to connect before continuing.
    // Useful with Yoda startup issues.
    private boolean isSuspend;
    
    SuperDevConfig() {
        theClasspathItems = new ArrayList<>(50);
        theDirsToWatch = new ArrayList<>();
        theExcludeRegexPatterns = new ArrayList(2);
    }

    List<String> getExcludePatterns() {
        return theExcludeRegexPatterns;
    }
    
    void setMainClassToRun(String mainClass) {
        theClassNameWithMainToRun = mainClass;
    }

    String getMainClassToRun() {
        return theClassNameWithMainToRun;
    }

    void addDirectoryToWatchList(String dir) {
        theDirsToWatch.add(dir);
    }

    List<String> getDirsToWatch() {
        return theDirsToWatch;
    }

    void addClasspathItem(String item) {
        theClasspathItems.add(item);
    }

    List<String> getClasspathItems() {
        return theClasspathItems;
    }

    void setContextPath(String contextPath) {
        theContextPath = contextPath;
    }

    String getContextPath() {
        return theContextPath;
    }

    void setPort(int port) {
        thePort = port;
    }

    int getPort() {
        return thePort;
    }

    void setSpringLoadedJar(String jarPath) {
        theSpringLoadedJar = jarPath;
    }

    String getSpringloadJar() {
        return theSpringLoadedJar;
    }

    void setDebugging(boolean value) {
        enableDebugggin = value;
    }
    
    boolean isDebuggingEnabled() {
        return enableDebugggin;
    }

    int getDebugPort() {
        return debugPort;
    }

    void setDebugPort(int debugPort) {
        this.debugPort = debugPort;
    }

    void setSuspend(boolean suspend) {
        isSuspend = suspend;
    }
    
    boolean isSuspended() {
        return isSuspend;
    }
    
}
