package org.nanotek.metaclass.plugin;

import java.util.List;
import java.util.Optional;

import org.nanotek.ClassConfigurationInitializer;
import org.nanotek.meta.model.rdbms.RdbmsMetaClass;
import org.nanotek.metaclass.schema.crawler.SchemaCrawlerDataSourceService;
import org.nanotek.metaclass.schema.crawler.SchemaCrawlerRdbmsMetaClassService;
import org.nanotek.metaclass.schema.crawler.SchemaCrawlerService;

/**
 * Initiaze the creation of the metaclass abstraction model based on RDBMS-Schema implementing
 * the method getMetaClasses from interface ClassConfigurationInitializer.
 */
public class DatabaseConfigurationInitializer 
implements ClassConfigurationInitializer{

	private SchemaCrawlerService schemaCrawlerService;
	private SchemaCrawlerDataSourceService dataSourceService;
	private SchemaCrawlerRdbmsMetaClassService schemaCrawlerRdbmsMetaClassService;
	public DatabaseConfigurationInitializer() {
	}

	private void prepareServices(String dataSourceConfiguration) {
		Optional
		.ofNullable(dataSourceConfiguration)
		.ifPresentOrElse(file ->{
			dataSourceService = SchemaCrawlerDataSourceService.loadFromFile(file);
			schemaCrawlerService =  new SchemaCrawlerService(dataSourceService);
			schemaCrawlerRdbmsMetaClassService = new SchemaCrawlerRdbmsMetaClassService(schemaCrawlerService);
		}, ()-> new RuntimeException("no configuration file provided"));
	}

	@Override
	public List<RdbmsMetaClass> getMetaClasses(String dataSourceConfiguration) {
		prepareServices(dataSourceConfiguration);
		return schemaCrawlerRdbmsMetaClassService.getMetaClassList();
	}

}
