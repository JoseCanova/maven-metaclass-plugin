package org.nanotek.metaclass.plugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.nanotek.ClassConfigurationInitializer;
import org.nanotek.meta.model.rdbms.RdbmsMetaClass;

import com.fasterxml.jackson.databind.ObjectMapper;

public class FileLocationConfigurationInitializer 
implements ClassConfigurationInitializer{

	private ObjectMapper objectMapper;
	
	public FileLocationConfigurationInitializer() {
		postConstruct();
	}

	private void postConstruct() {
		this.objectMapper = new ObjectMapper();
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
