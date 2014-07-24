Reorganized NetarchiveSuite sources into a multi-module maven build.

Build with

  mvn clean install


If you do not have Maven configured to use the SBForge nexus repository
manager, you can insert the artifacts missing from Maven Central with
the following commands:

  mvn install:install-file -Dfile=sb-nexus/mg4j-1.0.1.jar -DpomFile=sb-nexus/mg4j-1.0.1.pom
  mvn install:install-file -Dfile=sb-nexus/je-3.3.82.jar -DpomFile=sb-nexus/je-3.3.82.pom
  mvn install:install-file -Dfile=sb-nexus/wayback-core-1.8.0-20130411.jar -DpomFile=sb-nexus/wayback-core-1.8.0-20130411.pom
  mvn install:install-file -Dfile=sb-nexus/ia-web-commons-1.0-20130411.jar -DpomFile=sb-nexus/ia-web-commons-1.0-20130411.pom
  mvn install:install-file -Dfile=sb-nexus/heritrix-1.14.4.jar -DpomFile=sb-nexus/heritrix-1.14.4.pom

Eclipse reports a lot of validation errors which we are not interested in at the moment.

Open Window->Preferences->Validation and click the [Disable All] button to 
disable all validation.  [Ok] then rebuilds the project without errors.

/tra 2014-05-09

Eclipse configuration files moved to build-tools/src/main/resources so
maven-java-formatter-plugin can run from the command line with

  mvn com.googlecode.maven-java-formatter-plugin:maven-java-formatter-plugin:format


Eclipse:
=======

Use File->Import->General->Preferences to import eclipse-xml-settings.epf
Use Preferences->Java->Code Style->Formatter->[Import] to import eclipse-formatter-settings.xml

/tra 2014-07-21

archive-test and harvester-test migrated from junit 3 to junit 4.

/tra 2014-07-24
