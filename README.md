# maven-metaclass-plugin

#### The aim of the project is to initiate the design of a maven plugin to provide means for the generation of the JPA class model to be compiled in a java project, which this the management of the package for the ORM-JPA will be provided as source code by reverse engineering the "jpa class model".
##### Next steps of this project will be port the code of database schema crawling with model generation, this will be the default "class generation" strategy, other strategy will be export a generated or edited metaclass json file for code generation.
##### The plugin must offer the generation of class with source code optional, or just source code (leting the compiler plugin generate the class file), but source code will be the last stage of the plugin development.
