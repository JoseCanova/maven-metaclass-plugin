package org.nanotek.metaclass;


import java.lang.reflect.Field;
import java.util.Optional;
import java.util.stream.Stream;

import org.nanotek.EntityPathConfigurableClassLoader;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
//import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;

import jakarta.persistence.Entity;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeDescription.Generic;
import net.bytebuddy.dynamic.DynamicType;

public class DefaultRepositoryClassBuilder   {
	
	
	public DefaultRepositoryClassBuilder() {}

	public RepositoryPair prepareReppositoryForClass(Class<?> clazz, EntityPathConfigurableClassLoader classLoader){
		
		Class<?> idClass = getIdClass(clazz);
		
		Generic typeDescription = TypeDescription.Generic.Builder
										.parameterizedType(JpaRepository.class, clazz , idClass)
										.build()
										.asGenericType();
		Entity theEntity = clazz.getAnnotation(Entity.class);
		Optional.ofNullable(theEntity).orElseThrow();
		String repositoryName =  classLoader
									.getRepositoryPath()
									.replaceAll("[/]", ".")
									.concat(".")
									.concat(theEntity.name())
									.concat("Repository"); //basePackage.concat(theEntity.name()).concat("Repository");
		DynamicType.Unloaded<?> unloaded =   new ByteBuddy(ClassFileVersion.JAVA_V22)
//				.makeInterface(EntityBaseRepository.class)
				.makeInterface(typeDescription)
				.name(repositoryName)
				.annotateType( AnnotationDescription.Builder.ofType(Repository.class)
						.build())
				.annotateType( AnnotationDescription.Builder.ofType(Qualifier.class)
						.define("value",  theEntity.name().concat("Repository"))
						.build())//collectionResourceRel = "people", path = "people"
				.annotateType( AnnotationDescription.Builder.ofType(RepositoryRestResource.class)
						.define("collectionResourceRel",  theEntity.name().toLowerCase())
						.define("path", theEntity.name().toLowerCase())
						.build())
				.make();
			return new RepositoryPair(repositoryName,unloaded);
	}

	 public  Class<?> getIdClass(Class<?> y) {
			return Stream.of(y.getDeclaredFields())
			.filter( f -> hasIdAnnotation(f))
			.map(f -> f.getType())
			.findFirst().orElseThrow();
		}
	 
	 public  boolean hasIdAnnotation(Field f) {
			return Stream.of(f.getAnnotations()).filter(a ->a.annotationType().equals(jakarta.persistence.Id.class)).count()==1;
		}
	
}
