package org.nanotek.metaclass.plugin;


import java.io.File;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * Says "Hello" to a given name.
 */
@Mojo(name = "generate", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class MetaClassJpaGenerator extends AbstractMojo {

	
    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;

    /**
     * The name to greet.
     */
    @Parameter(property = "name", defaultValue = "World")
    private String name;

    
    @Parameter(defaultValue = "${project.build.directory}/generated-sources/jpa.metaclass", required = true)
    private File outputDirectory;

    @Override
    public void execute() throws MojoExecutionException {
        getLog().info("Hello, " + name + "!");
    }
}
