package ninja.plugin.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

/**
 * Gradle SuperDev Plugin.
 *
 * @author dhudson
 */
public class SuperDevPlugin implements Plugin<Project> {

    @Override
    public void apply(Project target) {
        target.getTasks().create("javaTask", SuperDevTask.class);
    }
}
