package uk.co.thinktag.plugin;

import java.io.DataOutputStream;
import java.net.Socket;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

@Mojo(name = "stop", defaultPhase = LifecyclePhase.VERIFY, requiresOnline = false, requiresProject = true, threadSafe = false)
public class StopServerMojo extends AbstractMojo {

	@Parameter(property = "port", required = true)
	protected int port;


	@Parameter( defaultValue = "${project}", readonly = true )
	protected MavenProject project;

	
	public MavenProject getProject() {
		return project;
	}

	public void setProject(MavenProject project) {
		this.project = project;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	@Override
	public void execute() throws MojoExecutionException {
		try (Socket clientSocket = new Socket("localhost", getPort())) {
			DataOutputStream dos = new DataOutputStream(clientSocket.getOutputStream());
			dos.writeBytes("0\n");
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
