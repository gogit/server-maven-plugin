# xserver-maven-plugin
A maven plugin that can be used to start and stop any server (like embedded artemis) within a maven test cycle.

##Example usage in pom.xml

~~~~
<code>
    <plugin>
	<groupId>uk.co.thinktag.plugin</groupId>
	<artifactId>xserver-maven-plugin</artifactId>
	<configuration>
		<port>9092</port>
		<serverClass>uk.co.thinktag.embedded.EmbeddedServer</serverClass>
	</configuration>
	<executions>
		<execution>
			<id>Spawn a new Artemis server</id>
			<phase>process-test-classes</phase>
			<goals>
				<goal>start</goal>
			</goals>
		</execution>
		<execution>
			<id>Stop a spawned Artemis server</id>
			<phase>verify</phase>
			<goals>
				<goal>stop</goal>
			 </goals>
		</execution>
	</executions>
    </plugin>
</code>
~~~~
The plugin starts the server using the supplied serverClass during the process-test-classes phase and shuts it down during the verify phase.

##Running the plugin
<code>
mvn verify
</code>

Checkout the xserver-test-artemis repository for a concrete example
