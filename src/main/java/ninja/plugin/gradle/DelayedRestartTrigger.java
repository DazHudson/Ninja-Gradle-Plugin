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

import static java.lang.Thread.sleep;

/**
 * Trigger to restart Superdev Thread
 * @author dhudson
 */
class DelayedRestartTrigger extends Thread {

    private volatile boolean restart;

    private final RunClassInSeparateJvmMachine runClassInSeparateJvmMachine;

    DelayedRestartTrigger(
            RunClassInSeparateJvmMachine runClassInSeparateJvmMachine) {

        restart = false;
        this.runClassInSeparateJvmMachine = runClassInSeparateJvmMachine;
    }

    @Override
    public void run() {
        while (true) {
            try {
                // this.wait(50);
                sleep(50);
                if (restart) {
                    restart = false;

                    System.out.println("Restarting SuperDevMode");
                    runClassInSeparateJvmMachine.restartNinjaJetty();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void triggerRestart() {
        restart = true;
    }

}
