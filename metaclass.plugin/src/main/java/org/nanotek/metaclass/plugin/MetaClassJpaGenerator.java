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
import org.jetbrains.java.decompiler.main.extern.IResultSaver;
import org.nanotek.ClassConfigurationInitializer;
import org.nanotek.EntityPathConfigurableClassLoader;
import org.nanotek.MetaClassRegistry;
import org.nanotek.metaclass.RepositoryClassBuilder;
import org.nanotek.metaclass.RepositoryPair;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;

@Mojo(name = "generate", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class MetaClassJpaGenerator extends AbstractMojo {

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
     * define where will be serialized java source files.
     */
    @Parameter(property="sourceDirectory" , defaultValue = "${project.build.sourceDirectory}")
    private File sourceDirectory;
    
    /**
     * define the entity package hierarchy in directory format.
     */
    @Parameter(property = "entityPackage", defaultValue = "")
    private String entityPackage;
    
    /**
     * define the repository package hierarchy in directory format.
     */
    @Parameter(property = "repositoryPackage", defaultValue = "")
    private String repositoryPackage;

    /**
     * not being used for now.
     */
    @Parameter(property = "servicePackage", defaultValue = "")
    private String servicePackage;
    
    /**
     * define if will be created the spring repositories remembering that 
     * just when generateSource option is select the spring repository option 
     * is available.
     */
    @Parameter(property = "createSpringRepositories", defaultValue = "false")
    private boolean createSpringRepositories;
    
    @Override
    public void execute() throws MojoExecutionException {
    	try {
    		
    			getLog().info("configuring classloader");       
    			byteArrayClassLoader = new EntityPathConfigurableClassLoader (fileSystem,entityPackage,repositoryPackage,servicePackage);
    			
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
		
		RepositoryPair repositoryClass = RepositoryClassBuilder.prepareReppositoryForClass(clazz,bytearrayclassloader2);
		Class<?> repo = repositoryClass.unloaded().load(bytearrayclassloader2).getLoaded();
		metaclassregistry2.registryRepositoryClass(clazz, repo);
	}

	void serializeClassFile(File fileLocation , Class c, EntityPathConfigurableClassLoader bytearrayclassloader2) {
    	
    	String directoryString = fileLocation.getAbsolutePath() ;
    	
    	String fileName =  c.getName().replaceAll("[.]","/").concat(".class");
    	
    	InputStream is = bytearrayclassloader2.getResourceAsStream(fileName);

    	try 
    	{
    		String packageDir = c.getPackageName().replaceAll("[.]","/");
    		byte[] classBytes = is.readAllBytes();
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
