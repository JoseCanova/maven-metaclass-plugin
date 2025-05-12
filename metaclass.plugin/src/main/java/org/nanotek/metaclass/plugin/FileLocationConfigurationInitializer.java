package org.nanotek.metaclass.plugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.util.ArrayList;
import java.util.List;

import org.nanotek.ClassConfigurationInitializer;
import org.nanotek.meta.model.rdbms.RdbmsMetaClass;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Initiaze the creation of the metaclass abstraction model based on json file 
 * of metaclasses representation implementing
 * the method getMetaClasses from interface ClassConfigurationInitializer.
 */
public class FileLocationConfigurationInitializer 
implements ClassConfigurationInitializer{

	private ObjectMapper objectMapper;
	private FileSystem fileSystem;
	
	public FileLocationConfigurationInitializer() {
		postConstruct();
	}

	private void postConstruct() {
		this.objectMapper = new ObjectMapper();
		fileSystem = FileSystems.getDefault();
	}

	@Override
	public List<RdbmsMetaClass> getMetaClasses(String uriEndpont) {
		try (InputStream is = new FileInputStream(new File(uriEndpont))){
			List<RdbmsMetaClass> al = new ArrayList<>();
			objectMapper.readValue
	    			(is
	    					, List.class)
	    					.stream()
	    					.forEach(mc -> al.add(objectMapper.convertValue(mc,RdbmsMetaClass.class)));
			return al;
		}catch (Exception ex) {
			ex.printStackTrace();
			throw new RuntimeException("Bad Metaclass File");
		}
	}

}
