package com.github.bringking.maven.requirejs;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.filtering.MavenFileFilter;
import org.apache.maven.shared.filtering.MavenFilteringException;
import org.mozilla.javascript.ErrorReporter;
import org.mozilla.javascript.EvaluatorException;

/**
 * Mojo for running r.js optimization.
 *
 * @goal optimize
 * @phase process-classes
 */
public class OptimizeMojo extends AbstractMojo {

    /**
     * @component role="org.apache.maven.shared.filtering.MavenFileFilter"
     * role-hint="default"
     * @required
     */
    private MavenFileFilter mavenFileFilter;

    /**
     * @parameter default-value="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * @parameter default-value="${project.build.directory}"
     * @required
     * @readonly
     */
    private File buildDirectory;

    /**
     * @parameter expression="${session}"
     * @required
     * @readonly
     */
    protected MavenSession session;

    /**
     * Path to optimizer script.
     *
     * @parameter
     */
    private File optimizerFile;

    /**
     * Paths to optimizer json config.
     *
     * @parameter
     * @required
     */
    private List<File> configFiles;

    /**
     * Whether or not the config file should
     * be maven filtered for token replacement.
     *
     * @parameter default-value=false
     */
    private boolean filterConfig;

    /**
     * If the 'deps' parameter in the config file should be generated
     * automatically from the contents of this folder
     * 
     * @parameter
     * @required
     */
    private File fillDepsFromFolder;

    /**
     * Skip optimization when this parameter is true.
     *
     * @parameter expression="${requirejs.optimize.skip}" default-value=false
     */
    private boolean skip;

    /**
     * Defines which javascript engine to use. Possible values: rhino or nodejs.
     *
     * @parameter expression="${requirejs.optimize.runner}" default-value=nodejs
     */
    private String runner = "nodejs";

    /**
     * Defines the location of the NodeJS executable to use.
     *
     * @parameter
     */
    private String nodeExecutable;

    /**
     * Defines the command line parameters to pass to the optimizer
     *
     * @parameter
     */
    private String[] optimizerParameters;

    /**
     * Optimize files.
     *
     * @throws MojoExecutionException if there is a problem optimizing files.
     */
    public void execute() throws MojoExecutionException {
        if (skip) {
            getLog().info("Optimization is skipped.");
            return;
        }

        Runner runner;
        String nodeCommand = getNodeCommand();
        
        if (nodeCommand != null) {
            getLog().info("Running with Node @ " + nodeCommand);
            runner = new NodeJsRunner(nodeCommand);
        } else {
            getLog().info("Node not detected. Falling back to rhino");
            runner = new RhinoRunner();
        }

        try {
            Optimizer builder = new Optimizer();
            ErrorReporter reporter = new MojoErrorReporter(getLog(), true);

            List<File> buildProfiles = createBuildProfile();
            for (File buildProfile : buildProfiles) {
                if (optimizerFile != null) {
                    if (this.optimizerParameters != null) {
                        builder.optimize(buildProfile, optimizerFile, reporter, runner, this.optimizerParameters);
                    } else builder.optimize(buildProfile, optimizerFile, reporter, runner);

                } else {

                    if (this.optimizerParameters != null) {
                        builder.optimize(buildProfile, reporter, runner, this.optimizerParameters);
                    } else builder.optimize(buildProfile, reporter, runner);
                }
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to read r.js", e);
        } catch (EvaluatorException e) {
            throw new MojoExecutionException("Failed to execute r.js", e);
        } catch (OptimizationException e) {
            throw new MojoExecutionException("r.js exited with an error.");
        }
    }
    /**
     * Returns the node command if node is available and it is the runner which should be used.
     * 
     * @return the command or <code>null</code>
     */
    private String getNodeCommand() {
        if ("nodejs".equalsIgnoreCase(runner)) {
            return getNodeJsPath();
        }
        
        return null;
    }

	@SuppressWarnings("rawtypes")
    public Map getPluginContext() {
        return super.getPluginContext();
    }

    @SuppressWarnings("rawtypes")
    private List<File> createBuildProfile() throws MojoExecutionException {
        if (filterConfig) {
            String scannedDepList = null;
            if (fillDepsFromFolder != null) {
                scannedDepList = scanChildren(fillDepsFromFolder.toURI(), fillDepsFromFolder);
            }
            List<File> filteredConfig = new ArrayList<File>();
            for (File configFile : configFiles) {
                try {
                    File profileDir = new File(buildDirectory, "requirejs-config/");
                    profileDir.mkdirs();
                    File currentFilteredConfig = new File(profileDir, "filtered-build.js");
                    if (!currentFilteredConfig.exists()) {
                        currentFilteredConfig.createNewFile();
                    }
                    //TODO hardcoded encoding
                    mavenFileFilter.copyFile(configFile, currentFilteredConfig, true, project, new ArrayList(), true, "UTF8", session);
                    if (scannedDepList != null) {
                        RandomAccessFile raf = new RandomAccessFile(
                                currentFilteredConfig, "rw");
                        byte[] buffer = new byte[(int) raf.length()];
                        raf.readFully(buffer);
                        buffer = new String(buffer).replace("${scanFolder}",
                                scannedDepList).getBytes();
                        raf.seek(0);
                        raf.write(buffer);
                        raf.close();
                    }
                    filteredConfig.add(currentFilteredConfig);
                } catch (IOException e) {
                    throw new MojoExecutionException("Error creating filtered build file.", e);
                } catch (MavenFilteringException e) {
                    throw new MojoExecutionException("Error filtering config file.", e);
                }
            }
            return filteredConfig;
        } else {
            return configFiles;
        }
    }

    private String scanChildren(URI referenceURI, File currentFolder) {
        File[] files = currentFolder.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.getName().endsWith(".js") || file.isDirectory();
            }
        });

        StringBuilder ret = new StringBuilder();
        String separator = "";
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    ret.append(separator).append(
                            scanChildren(referenceURI, file));
                } else {
                    String relativePath = referenceURI.relativize(file.toURI())
                            .getPath();
                    String relativePathWithoutExtension = relativePath
                            .substring(0, relativePath.lastIndexOf('.'));
                    ret.append(separator).append("\"")
                            .append(relativePathWithoutExtension).append("\"");
                }
                separator = ",";
            }
        }

        return ret.toString();
    }

    private String getNodeJsPath() {
        if (nodeExecutable != null) {
            return nodeExecutable;
        } else {
            return NodeJsRunner.detectNodeCommand();
        }
    }
}
