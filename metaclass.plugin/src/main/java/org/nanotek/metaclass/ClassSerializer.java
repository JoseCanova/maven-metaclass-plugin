package org.nanotek.metaclass;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;

import org.nanotek.EntityPathConfigurableClassLoader;

/**
 * 
 * Simple serialization method to export a class to a file system.
 * 
 */
public interface ClassSerializer {

	
		@SuppressWarnings("rawtypes")
		default void serializeClassFile(File fileLocation , Class c, EntityPathConfigurableClassLoader bytearrayclassloader2) {
		    	
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
	
}
