package com.github.bringking.maven.requirejs;

import org.codehaus.plexus.util.IOUtil;
import org.mozilla.javascript.ErrorReporter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Optimizes js files.
 */
public class Optimizer {

    private static final String CLASSPATH_R_JS = "/r.js";

    /**
     * Optimize using the built-in version of r.js.
     * 
     *
     * @param buildProfile file containing optimizer build profile configuration
     * @param reporter error reporter
     * @param runner Runner which will execute the optimize script
     * @throws IOException if there is a problem reading/writing optimization files
     * @throws OptimizationException if the optimizer script returns an error status
     */
    public void optimize(File buildProfile, ErrorReporter reporter, Runner runner, String [] params) throws IOException, OptimizationException {
        File optimizerFile = getClasspathOptimizerFile();
        optimize(buildProfile, optimizerFile, reporter, runner, params);
    }
    public void optimize(File buildProfile, ErrorReporter reporter, Runner runner) throws IOException, OptimizationException {
        File optimizerFile = getClasspathOptimizerFile();
        optimize(buildProfile, optimizerFile, reporter, runner);
    }

    /**
     * Optimize using an external version of r.js.
     * 
     *
     * @param buildProfile file containing optimizer build profile configuration
     * @param optimizerFile file containing optimizer library
     * @param reporter error reporter
     * @param runner Runner which will execute the optimize script
     * @throws IOException if there is a problem reading/writing optimization files
     * @throws OptimizationException if the optimizer script returns an error status
     */
    public void optimize(File buildProfile, File optimizerFile, ErrorReporter reporter, Runner runner, String[] params) throws IOException, OptimizationException {

        List<String> args = new ArrayList<String>();
        args.add("-o");
        args.add(buildProfile.getAbsolutePath());

        //append user params
        for(int i = 0;i<params.length;++i){
            args.add(params[i]);
        }
        //create array
        String[] newParams = new String[args.size()];
        newParams = args.toArray(newParams);

        ExitStatus status = runner.exec(optimizerFile, newParams, reporter);
        if (!status.success()) {
        	throw new OptimizationException("Optimizer returned non-zero exit status.");
        }
    }
    public void optimize(File buildProfile, File optimizerFile, ErrorReporter reporter, Runner runner) throws IOException, OptimizationException {

        String[] args = new String[2];
        args[0] = "-o";
        args[1] = buildProfile.getAbsolutePath();

        ExitStatus status = runner.exec(optimizerFile, args, reporter);
        if (!status.success()) {
            throw new OptimizationException("Optimizer returned non-zero exit status.");
        }
    }

    private File getClasspathOptimizerFile() throws IOException {
        File optimizerFile = File.createTempFile("build", "js");
        optimizerFile.deleteOnExit();
        FileOutputStream out = null;
        InputStream in = null;
        try {
            in = getClass().getResourceAsStream(CLASSPATH_R_JS);
            out = new FileOutputStream(optimizerFile);
            IOUtil.copy(in, out);
        } finally {
            IOUtil.close(in);
            IOUtil.close(out);
        }
        
        return optimizerFile;
    }

}
