package org.nanotek.metaclass.plugin;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.nanotek.EntityPathConfigurableClassLoader;
import org.nanotek.MetaClassRegistry;
import org.nanotek.meta.model.rdbms.RdbmsMetaClass;

import com.fasterxml.jackson.core.exc.StreamWriteException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;

/**
 * This unit test is designed to succeed in a json  domain model
 * that does not have join keys representation, otherwise the test assertion 
 * will fails since for now join keys representation are converted into "JoinTable"
 * annotations not on JPA Entities
 */
public class FileConfigurationInitializerTest {

	public static final FileSystem fileSystem = Jimfs.newFileSystem(Configuration.unix());
	
	public static   EntityPathConfigurableClassLoader byteArrayClassLoader = new EntityPathConfigurableClassLoader(fileSystem,"org/nanotek/entity","org/nanotek/repository","org/nanotek/service");

	public static final MetaClassRegistry<?> metaClassRegistry  = new  MetaClassRegistry<>();
	
	@Test
	void testDatabaseConfigurationInitializer() {
		String jsonMetaclass  = getClass().getResource("/metaclasses.json").getPath();
		var configurationParameters = new HashMap<String,Object>();
    	try {
    		var cci = new FileLocationConfigurationInitializer();
    		List<RdbmsMetaClass> resultMetaClasses = cci.getMetaClasses(jsonMetaclass ); 
			List<Class<?>> generatedCkasses = cci.configureMetaClasses(jsonMetaclass , byteArrayClassLoader, metaClassRegistry,configurationParameters);
			assertTrue (resultMetaClasses.size() == generatedCkasses.size());
			//saveJsonFile(resultMetaClasses);
    	} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	private void saveJsonFile(List<RdbmsMetaClass> resultMetaClasses) throws StreamWriteException, DatabindException, IOException {
		String targetClassPath= getClass().getResource("/").getPath();
		String jsonFilePath = targetClassPath.concat("/").concat("metaclasses.json");
		File jsonFile = new File(jsonFilePath);
		ObjectMapper mapper = new ObjectMapper();
		mapper.writeValue(jsonFile,resultMetaClasses);
	}
	
}
