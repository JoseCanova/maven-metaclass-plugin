package org.nanotek.metaclass.plugin;


import java.io.File;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.jetbrains.java.decompiler.api.Decompiler;
import org.jetbrains.java.decompiler.main.decompiler.ConsoleFileSaver;
import org.nanotek.ClassConfigurationInitializer;
import org.nanotek.ClassFileSerializer;
import org.nanotek.MetaClassRegistry;
import org.nanotek.MetaClassVFSURLClassLoader;
import org.nanotek.meta.model.rdbms.RdbmsMetaClass;
import org.nanotek.metaclass.BuilderMetaClassRegistry;
import org.nanotek.metaclass.ProcessedForeignKeyRegistry;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;

/**
 * Says "Hello" to a given name.
 */
@Mojo(name = "generate", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class MetaClassJpaGenerator extends AbstractMojo {

public static final FileSystem fileSystem = Jimfs.newFileSystem(Configuration.unix());
	
	public static final MetaClassVFSURLClassLoader byteArrayClassLoader  = new MetaClassVFSURLClassLoader 
																			(MetaClassJpaGenerator.class.getClassLoader() , 
																					false ,fileSystem);

	public static final MetaClassRegistry<?> metaClassRegistry  = new  MetaClassRegistry<>();
	
	
    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;

    /**
     * The name to greet.
     */
    @Parameter(property = "name", defaultValue = "World")
    private String name;

    
    /**
     * The json with metaclass relation to represent as classes.
     */
    @Parameter(property = "fileLocation", defaultValue = "/home/jose/git/plugin/maven-metaclass-plugin/metaclass.plugin/src/test/resources/metaclass.json")
    private String fileLocation;

    
    @Parameter(defaultValue = "${project.build.directory}/", required = true)
    private File outputDirectory;

    @Parameter(property="targetDirectory" , defaultValue = "${project.build.directory}/classes")
    private File targetDirectory;
    
    @Override
    public void execute() throws MojoExecutionException {
    	try {
		        getLog().info("Hello, " + name + "!");
		        ClassConfigurationInitializer cci = new FileLocationConfigurationInitializer();
		        List<RdbmsMetaClass>theList = cci.getMetaClasses(fileLocation);
		        cci.configureMetaClasses(fileLocation, byteArrayClassLoader, metaClassRegistry);
		        metaClassRegistry
		        .getEntityClasses()
		        .forEach(clazz->
		       saveEntityFile(targetDirectory, Class.class.cast(clazz),byteArrayClassLoader));

    	}catch(Exception ex) {
        	ex.printStackTrace();
        	throw new MojoExecutionException(ex);
        }
    }
    
     void saveEntityFile(File fileLocation , Class c, MetaClassVFSURLClassLoader bytearrayclassloader2) {
    	
    	String directoryString = fileLocation.getAbsolutePath() ;
    	
    	String fileName =  c.getName().replaceAll("[.]","/").concat(".class");
    	
    	InputStream is = bytearrayclassloader2.getResourceAsStream(fileName);

    	try 
    	{

    		byte[] classBytes = is.readAllBytes();
    		var className = c.getName();
    		var simpleName = c.getSimpleName();
    		Path dirPath = Paths.get(directoryString, new String[] {});
    		Files.createDirectories(dirPath);
    		var classLocation  = directoryString.concat("/").concat(simpleName).concat(".class");
    		Path classPath = Paths.get(classLocation, new String[] {});
    		if(!Files.exists(classPath, LinkOption.NOFOLLOW_LINKS))
    			Files.createFile(classPath, new FileAttribute[0]);
    		Files.write(classPath, classBytes, StandardOpenOption.WRITE);

    		
    	}catch(Exception ex) {
    		ex.printStackTrace();
    	}
    	
    }
    
	public static void decompileJavaClasses (List<Class<?>> javaClassList) {
        
 		javaClassList.forEach(cl ->{
         	File input = new File("/home/jose/Documents/".concat(cl.getSimpleName()).concat(".class"));
 	        
 	        // Define the output folder where decompiled source files will be written.
 	        File output = new File("/home/jose/Documents/".concat(cl.getSimpleName()).concat(".java"));
 	        
 	        try {
 	        	 if(output.exists()) {
 	        		 output.delete();
 	        	 }
 	        	
 	        	 output.createNewFile();
 	        	 Decompiler decompiler =  Decompiler.builder()
 	        			 .inputs(input)
 	        			 .output(new ConsoleFileSaver(output))
 	        			 .option("decompile-generics", "true")
 	     	        	.build();
 	            decompiler.decompile();
 	            System.out.println("Decompilation completed successfully.");
 	        } catch (Exception e) {
 	            e.printStackTrace();
 	        }
 		});
 	}
}
