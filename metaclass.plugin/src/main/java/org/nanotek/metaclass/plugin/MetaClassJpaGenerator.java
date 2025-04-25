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
import org.nanotek.Base;
import org.nanotek.ClassConfigurationInitializer;
import org.nanotek.ClassFileSerializer;
import org.nanotek.MetaClassRegistry;
import org.nanotek.MetaClassVFSURLClassLoader;
import org.nanotek.meta.model.rdbms.RdbmsMetaClass;
import org.nanotek.metaclass.BuilderMetaClassRegistry;
import org.nanotek.metaclass.ProcessedForeignKeyRegistry;

import org.nanotek.metaclass.schema.crawler.*;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;

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
    @Parameter(property = "name", defaultValue = "")
    private String name;

    
    /**
     * The json with metaclass relation to represent as java classes.
     */
    @Parameter(property = "fileLocation", defaultValue = "")
    private String fileLocation;

    @Parameter(property = "provider", defaultValue = "database")
    private String provider;
    
    @Parameter(property = "dataSourceConfiguration", defaultValue = "")
    private String dataSourceConfiguration;
    
    @Parameter(property = "generateSources", defaultValue = "false")
    private boolean generateSources;
    
    @Parameter(defaultValue = "${project.build.directory}/", required = true)
    private File outputDirectory;

    @Parameter(property="targetDirectory" , defaultValue = "${project.build.directory}/classes")
    private File targetDirectory;
    
    @Parameter(property="sourceDirectory" , defaultValue = "${project.build.directory}/generated-sources")
    private File sourceDirectory;
    
    @Override
    public void execute() throws MojoExecutionException {
    	try {
		        getLog().info("initializing plugin execution" + name + "!");
		        ClassConfigurationInitializer cci = null;
		        
		        if(provider.equals("database") &&  !dataSourceConfiguration.isEmpty())
		        {
		        	getLog().info("generating model from database schema.");
		        	cci = new DatabaseConfigurationInitializer();
		        	cci.configureMetaClasses(dataSourceConfiguration, byteArrayClassLoader, metaClassRegistry);
		        }else if(provider.equals("file") && !fileLocation.isEmpty())
		        {
		        	getLog().info("generating model from json file.");
		        	cci = new FileLocationConfigurationInitializer();
		        	cci.configureMetaClasses(fileLocation, byteArrayClassLoader, metaClassRegistry);
		        }
		        
		        metaClassRegistry
		        .getEntityClasses()
		        .forEach(clazz->
		        				saveEntityFile(targetDirectory, Class.class.cast(clazz),byteArrayClassLoader));
		        if(generateSources) {
		        	metaClassRegistry
		        	.getEntityClasses()
		        	.forEach(clazz -> decompileClassFile(clazz));
		        }
    	}catch(Exception ex) {
        	ex.printStackTrace();
        	throw new MojoExecutionException(ex);
        }
    }
    
     private void decompileClassFile(Class<Base<?>> clazz) {
		return;
	}

	void saveEntityFile(File fileLocation , Class c, MetaClassVFSURLClassLoader bytearrayclassloader2) {
    	
    	String directoryString = fileLocation.getAbsolutePath() ;
    	
    	String fileName =  c.getName().replaceAll("[.]","/").concat(".class");
    	
    	InputStream is = bytearrayclassloader2.getResourceAsStream(fileName);

    	try 
    	{
    		String packageDir = c.getPackageName().replaceAll("[.]","/");
    		byte[] classBytes = is.readAllBytes();
    		var className = c.getName();
    		var simpleName = c.getSimpleName();
    		String finalLocationDir = directoryString.concat("/").concat(packageDir);
    		Path dirPath = Paths.get(finalLocationDir, new String[] {});
    		Files.createDirectories(dirPath);
    		var classLocation  = finalLocationDir.concat("/").concat(simpleName).concat(".class");
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
