- install local jar dependency to mvn  (local runtime)

```
mvn install:install-file -Dfile=vas-integrations-pseudonymisation-0.8.0-SNAPSHOT-jar-with-dependencies.jar -DgroupId=be.smals.vas.integrations -DartifactId=vas-integrations-pseudonymisation -Dversion=0.8.0-SNAPSHOT -Dpackaging=jar
```
- add dependency to pom.xml
```xml  

        <dependency>
            <groupId>be.smals.vas.integrations</groupId>
            <artifactId>vas-integrations-pseudonymisation</artifactId>
            <version>0.8.0-SNAPSHOT</version>
        </dependency>

```
