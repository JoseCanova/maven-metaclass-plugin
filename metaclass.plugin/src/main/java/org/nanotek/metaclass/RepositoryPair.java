package org.nanotek.metaclass;


import net.bytebuddy.dynamic.DynamicType;

public record RepositoryPair(String repositoryName , DynamicType.Unloaded<?>unloaded) {}
