package org.nanotek.metaclass.plugin;


import java.io.File;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.jetbrains.java.decompiler.api.Decompiler;
import org.jetbrains.java.decompiler.main.extern.IResultSaver;
import org.nanotek.ClassConfigurationInitializer;
import org.nanotek.EntityPathConfigurableClassLoader;
import org.nanotek.MetaClassRegistry;
import org.nanotek.metaclass.ClassSerializer;
import org.nanotek.metaclass.DefaultRepositoryClassBuilder;
import org.nanotek.metaclass.RepositoryPair;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;

import jakarta.persistence.Id;

/**
 * 
 * Basic Maven plugin to generate initial JPA class model for a RDBMS Schema.
 * Remembering that adoption of JPA standard can be suffer some opposition since 
 * on day by day corporate life the "database model" usually come "first" or in
 * parallel with "Business Design". So the plugin works as a facilitator speeding up
 * the Object Model which on its turn can take some times depending the size of database model.
 * 
 * @author Jose Carlos Canova
 * 
 */
//TODO: Rename this class when spring data specific annotation will be used 
@Mojo(name = "generate", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class MetaClassJpaGenerator extends AbstractMojo 
implements ClassSerializer{

	public static final FileSystem fileSystem = Jimfs.newFileSystem(Configuration.unix());
	
	public   EntityPathConfigurableClassLoader byteArrayClassLoader;

	public static final MetaClassRegistry<?> metaClassRegistry  = new  MetaClassRegistry<>();
	
	
    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;

    /**
     * Not being used for now.
     */
    @Parameter(property = "name", defaultValue = "")
    private String name;

    
    /**
     * The json with metaclass relation to represent as java classes.
     */
    @Parameter(property = "fileLocation", defaultValue = "")
    private String fileLocation;

    /**
     * define which source provider will be used file or database
     */
    @Parameter(property = "provider", defaultValue = "database")
    private String provider;
    
    /**
     * json file with datasource configuration location for schema crawler.
     */
    @Parameter(property = "dataSourceConfiguration", defaultValue = "")
    private String dataSourceConfiguration;
    
    /**
     * define if the java sources will be generate or just the source files.
     */
    @Parameter(property = "generateSources", defaultValue = "false")
    private boolean generateSources;
    
    /**
     * output directory as default.
     */
    @Parameter(defaultValue = "${project.build.directory}/", required = true)
    private File outputDirectory;

    /**
     * target directory for generated class files.
     */
    @Parameter(property="targetDirectory" , defaultValue = "${project.build.directory}/classes")
    private File targetDirectory;
    
    /**
     * define the location will be serialized java source files.
     */
    @Parameter(property="sourceDirectory" , defaultValue = "${project.build.sourceDirectory}")
    private File sourceDirectory;
    
    /**
     * define the entity package folder hierarchy in directory format.
     */
    @Parameter(property = "entityPackage", defaultValue = "")
    private String entityPackage;
    
    /**
     * define the repository package folder hierarchy in directory format.
     */
    @Parameter(property = "repositoryPackage", defaultValue = "")
    private String repositoryPackage;

    /**
     * not being used for now.
     */
    @Parameter(property = "servicePackage", defaultValue = "")
    private String servicePackage;
    
    /**
     * define if will be created the spring repositories (spring data)
     * remembering that just when generateSource option is select the spring repository option 
     * is available.
     */
    @Parameter(property = "createSpringRepositories", defaultValue = "false")
    private boolean createSpringRepositories;
    
    
    /**
     * define if jakarta validation annotations will be added to field description.
     */
    @Parameter(property = "enableValidation", defaultValue = "true")
    private Boolean enableValidation;
    
    @Override
    public void execute() throws MojoExecutionException {
    	try {
    		
    			getLog().info("configuring classloader");       
    			byteArrayClassLoader = new EntityPathConfigurableClassLoader (fileSystem,entityPackage,repositoryPackage,servicePackage);
    			
    			getLog().info("initializing plugin execution" + name + "!");
		        ClassConfigurationInitializer cci = null;
		        
		        Map<String,Object> configurationParameters = new HashMap<>();
		        
		        configurationParameters.put("enableValidation", enableValidation);
		        
		        if(provider.equals("database") &&  !dataSourceConfiguration.isEmpty())
		        {
		        	getLog().info("generating model from database schema.");
		        	cci = new DatabaseConfigurationInitializer();
		        	cci.configureMetaClasses(dataSourceConfiguration, byteArrayClassLoader, metaClassRegistry,configurationParameters);
		        }else if(provider.equals("file") && !fileLocation.isEmpty())
		        {
		        	getLog().info("generating model from json file.");
		        	cci = new FileLocationConfigurationInitializer();
		        	cci.configureMetaClasses(fileLocation, byteArrayClassLoader, metaClassRegistry,configurationParameters);
		        }
		        
		        metaClassRegistry
		        .getEntityClasses()
		        .forEach(clazz->
		        				serializeClassFile(targetDirectory, clazz,byteArrayClassLoader));
		        //separate the loops because will add a parameter to provide repository generation as optional step.
		        if(createSpringRepositories) {
				        metaClassRegistry
				        .getEntityClasses()
				        .forEach(clazz->
				        				createRestRepository(targetDirectory, clazz,byteArrayClassLoader,metaClassRegistry));
				        metaClassRegistry
				        .getRepositoryClasses()
				        .forEach(clazz->
									serializeClassFile(targetDirectory, clazz,byteArrayClassLoader));
		        }
		        if(generateSources) {
		        	decompileJavaClasses (metaClassRegistry
		        					.getEntityClasses());
		        	if(createSpringRepositories) {
			        	decompileJavaClasses (metaClassRegistry
					        			.getRepositoryClasses());
		        	}
		        }
    	}catch(Exception ex) {
        	ex.printStackTrace();
        	throw new MojoExecutionException(ex);
        }
    }
    
	private void createRestRepository(File targetDirectory2, Class<?> clazz,
			EntityPathConfigurableClassLoader bytearrayclassloader2, MetaClassRegistry<?> metaclassregistry2) {
		
		DefaultRepositoryClassBuilder<Id> repositoryClassBuilder = new DefaultRepositoryClassBuilder<>(Id.class);
		
		RepositoryPair repositoryClass = repositoryClassBuilder.prepareReppositoryForClass(clazz,bytearrayclassloader2);
		Class<?> repo = repositoryClass.unloaded().load(bytearrayclassloader2).getLoaded();
		metaclassregistry2.registryRepositoryClass(clazz, repo);
	}

	public void decompileJavaClasses (List<Class<?>> list) {
        
 		list.forEach(cl ->{
 			
 			String dir = targetDirectory.getAbsolutePath();
 			
 			String packageDir = cl.getPackageName().replaceAll("[.]","/");
 			
         	File input = new File(dir.concat("/").concat(packageDir).concat("/").concat(cl.getSimpleName()).concat(".class"));
 	        
 	        File output = new File(sourceDirectory.getAbsolutePath().concat("/").concat(packageDir).concat("/").concat(cl.getSimpleName()).concat(".java"));
    		

 	        try {
 	        	Files.createDirectories(Paths.get(sourceDirectory.getAbsolutePath().concat("/").concat(packageDir)));
 	        	 if(output.exists()) {
 	        		 output.delete();
 	        	 }
 	        	IResultSaver saver = new JavaFileGenerator(output);
 	        	 output.createNewFile();
 	        	 Decompiler decompiler =  Decompiler.builder()
 	        			 .inputs(input)
 	        			 .output(saver)
 	        			 .option("decompile-generics", "true")
 	     	        	.build();
 	            decompiler.decompile();
 	            getLog().info("Decompilation completed successfully.");
 	        } catch (Exception e) {
 	            e.printStackTrace();
 	        }
 		});
 	}
}
