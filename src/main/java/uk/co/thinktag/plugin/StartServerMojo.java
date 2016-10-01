package uk.co.thinktag.plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

@Mojo(name = "start", defaultPhase = LifecyclePhase.PROCESS_TEST_CLASSES, requiresOnline = false,
                requiresProject = true, threadSafe = false)
public class StartServerMojo extends AbstractMojo {

    @Parameter(property = "port", required = true)
    protected int port;

    @Parameter(property = "serverClass", required = true)
    protected String serverClass;

    @Parameter(defaultValue = "${project}", readonly = true)
    protected MavenProject project;

    @Parameter(defaultValue = "${localRepository}", readonly = true, required = true)
    private ArtifactRepository local;

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


    public String getServerClass() {
        return serverClass;
    }

    public void setServerClass(String serverClass) {
        this.serverClass = serverClass;
    }


    Thread t;

    @Override
    public void execute() throws MojoExecutionException {
        try {


            File java = new File(new File(System.getProperty("java.home"), "bin"), "java");
            List<String> args = new ArrayList<String>();
            args.add(java.getAbsolutePath());
            args.add("-cp");
            args.add(buildClasspath());
            args.add(getServerClass());

            System.out.println("------------------");
            System.out.println(args);
            System.out.println("------------------");

            Process p = new ProcessBuilder(args).start();
            dumpStream(p.getInputStream(), System.out);
            dumpStream(p.getErrorStream(), System.err);
            addShutdownHook(p);
            waitOnStopCommand(p);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String buildClasspath() throws DependencyResolutionRequiredException {
        final String separator = System.getProperty("path.separator");

        List<String> r1 = project.getRuntimeClasspathElements();
        Set<Artifact> dependencies = project.getDependencyArtifacts();
        List<String> paths = new ArrayList<>();
        for (Artifact artifact : dependencies) {
            // Find the artifact in the local repository.
            Artifact art = local.find(artifact);

            paths.add(art.getFile().getAbsolutePath());
        }

        r1.addAll(paths);

        return String.join(separator, r1);
    }


    private final File getEmbeddedServerLocation() throws ClassNotFoundException {
		final ProtectionDomain pd = Class.forName(getServerClass()).getProtectionDomain();
		assert pd != null;
		final CodeSource cs = pd.getCodeSource();
		assert cs != null;
		final URL location = cs.getLocation();
		assert location != null;
		try {
			return new File(location.toURI());
		} catch (final URISyntaxException wontHappen) {
			throw (InternalError) new InternalError().initCause(wontHappen);
		}
	}

    private String getRuntimeClasspath() throws DependencyResolutionRequiredException {
        final String separator = System.getProperty("path.separator");
        // get the union of compile- and runtime classpath elements
        Set<String> dependencySet = new HashSet();
        dependencySet.addAll(project.getRuntimeClasspathElements());
        dependencySet.addAll(project.getSystemClasspathElements());
        String compileClasspath = String.join(File.pathSeparator, dependencySet);

        return compileClasspath;

    }

    public void waitOnStopCommand(final Process p) throws IOException {

        // !!! cannot write lambdas in plugins it fails
        // java.lang.ArrayIndexOutOfBoundsException: 5377
        t = new Thread(new Runnable() {

            public void run() {
                try (final ServerSocket ssocket = new ServerSocket(getPort())) {
                    boolean flag = false;
                    while (!flag) {
                        Socket connectionSocket = ssocket.accept();
                        flag = true;
                    }
                    p.destroy();
                } catch (IOException e) {
                }
            }
        });
        t.start();
    }

    private void addShutdownHook(final Process p){

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            public void run() {
                try {
                    p.destroy();
                } catch (Exception e) {
                }
            }
        }));

    }
    
    private void dumpStream(final InputStream src, final PrintStream dest) {
        new Thread(new Runnable() {

            public void run() {

                try (final Scanner sc = new Scanner(src)) {
                    while (sc.hasNextLine()) {
                        dest.println(sc.nextLine());
                    }
                }
            }
        }).start();
    }
}
