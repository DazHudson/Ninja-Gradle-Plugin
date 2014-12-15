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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Create a process and run it.
 *
 * @author dhudson
 */
public class RunClassInSeparateJvmMachine {

    private Process processCurrentlyActive;
    private final SuperDevConfig theConfig;

    /**
     * Construct a Process Runner.
     *
     * @param config superdev config
     */
    RunClassInSeparateJvmMachine(
            SuperDevConfig config) {
        theConfig = config;

        // initial startup
        try {
            processCurrentlyActive = startNewNinjaJetty();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                processCurrentlyActive.destroy();
                try {
                    processCurrentlyActive.waitFor();
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
        });

    }

    synchronized void restartNinjaJetty() {
        try {
            processCurrentlyActive.destroy();
            processCurrentlyActive.waitFor();
            processCurrentlyActive = startNewNinjaJetty();
        } catch (InterruptedException | IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private Process startNewNinjaJetty() throws IOException,
            InterruptedException {

        List<String> commandLine = new ArrayList<>();

        String javaHome = System.getProperty("java.home");
        String javaBin = javaHome + File.separator + "bin" + File.separator
                + "java";

        commandLine.add(javaBin);

        if (theConfig.isDebuggingEnabled()) {
            commandLine.add("-Xdebug");
            if (theConfig.isSuspended()) {
                commandLine.add("-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=" + theConfig.getDebugPort());
            } else {
                commandLine.add("-Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=" + theConfig.getDebugPort());
            }
        }

        if (theConfig.getSpringloadJar() != null) {
            // Add Spring Loaded to the jars
            commandLine.add("-javaagent:" + theConfig.getSpringloadJar());
            commandLine.add("-noverify");
        }

        String systemPropertyDevMode
                = "-D" + "ninja.mode" + "=" + "dev";

        commandLine.add(systemPropertyDevMode);

        String portSelection
                = "-D" + "ninja.port" + "=" + Integer.toString(theConfig.getPort());

        commandLine.add(portSelection);

        if (theConfig.getContextPath() != null) {
            String systemPropertyContextPath = "-Dninja.context=" + theConfig.getContextPath();
            commandLine.add(systemPropertyContextPath);
        }

        StringBuilder classpathAsString = new StringBuilder(1000);
        for (String path : theConfig.getClasspathItems()) {
            classpathAsString.append(path);
            classpathAsString.append(File.pathSeparator);
        }

        commandLine.add("-cp");
        commandLine.add(classpathAsString.toString());
        commandLine.add(theConfig.getMainClassToRun());

        ProcessBuilder builder = new ProcessBuilder(commandLine);
        Process process = builder.start();

        StreamGobbler outputGobbler = new StreamGobbler(
                process.getInputStream());
        outputGobbler.start();

        return process;
    }

    /**
     * Just a stupid StreamGobbler that will print out all stuff from the "other" process...
     */
    private static class StreamGobbler extends Thread {

        private final InputStream inputStream;

        private StreamGobbler(InputStream is) {
            inputStream = is;
        }

        @Override
        public void run() {
            try {
                InputStreamReader isr = new InputStreamReader(inputStream);
                BufferedReader br = new BufferedReader(isr);
                String line;
                while ((line = br.readLine()) != null) {
                    System.out.println(line);
                }
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }
}
