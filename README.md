# Stuff for the website of the project

The publication is done via **maven** with:
```sh
mvn scm-publish:publish-scm
```

This command consumes in particular this part of the `pom.xml`:
```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-scm-publish-plugin</artifactId>
  <version>3.3.0</version>
  <configuration>
    <serverId>github</serverId>
    <scmBranch>gh-pages</scmBranch>
    <checkinComment>Publishing API Docs for version ${project.version}</checkinComment>
    <content>${project.build.directory}/www-docs</content>
    <pubScmUrl>${project.scm.developerConnection}</pubScmUrl>
    <subDirectory>docs</subDirectory>
  </configuration>
</plugin>
```
which requires to have a Personal Access Token (PAT) properly set in the `settings.xml` file on the development machine, as the `<scm>` section of the pom specifies this (https) connection:
```xml
<scm>
  <connection>scm:git:https://github.com/ericmedvet/jnb.git</connection>
  <developerConnection>scm:git:https://github.com/ericmedvet/jnb.git</developerConnection>
  <url>https://github.com/ericmedvet/jnb</url>
</scm>
```

Currently, the only content being published is the API docs, built with javadoc in the `package` phase based on this part of the `pom.xml` (something is omitted, being unrelated):
```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-javadoc-plugin</artifactId>
  <version>3.11.2</version>
  <configuration>
    <outputDirectory>${project.build.directory}/www-docs/${project.version}/</outputDirectory>
  </configuration>
  <executions>
    <execution>
      <id>aggregate</id>
      <goals>
        <goal>aggregate</goal>
      </goals>
      <phase>package</phase>
    </execution>
  </executions>
</plugin>
```

Note that this results in having the *multiversion* docs on the web site.
For example, the one for the version `1.5.0-SNAPSHOT` is reachable at [ericmedvet.github.io/jnb/1.5.0-SNAPSHOT/apidocs/](https://ericmedvet.github.io/jnb/1.5.0-SNAPSHOT/apidocs/).
