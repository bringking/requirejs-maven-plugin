package com.github.bringking.maven.requirejs;

import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mozilla.javascript.ErrorReporter;

/**
 * Testing Optimizer
 *
 * @author skoranga
 */
public class OptimizerTest {

    private Log log = new SystemStreamLog();

    private Optimizer optimizer = null;

    private ErrorReporter reporter = null;

    private Runner runner;

    @Before
    public void setUp() throws Exception {

        optimizer = new Optimizer();
        reporter = new MojoErrorReporter(log, true);
        runner = new RhinoRunner();
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testBuildConfigFull() throws Exception {
        long start = System.currentTimeMillis();
        optimizer.optimize(loadProfile("testcase1/buildconfig1.js"), reporter, runner);
        long end = System.currentTimeMillis();

        log.debug("total time ::" + (end - start) + "msec");
    }

    // @Test
    // public void testBuildConfigFull2() throws Exception {
    // long start = System.currentTimeMillis();
    // optimizer.optimize( loadProfile( "testcase2/buildconfig2.js" ), reporter,
    // runner );
    // long end = System.currentTimeMillis();
    //
    // log.debug( "total time ::" + (end - start) + "msec" );
    // }
    //
    // @Test
    // public void testBuildConfig2MainConfig() throws Exception {
    // long start = System.currentTimeMillis();
    // optimizer.optimize( loadProfile(
    // "testcase2/buildconfigWithMainConfig2.js" ), reporter, runner );
    // long end = System.currentTimeMillis();
    //
    // log.debug( "total time ::" + (end - start) + "msec" );
    // }
    //
    // @Test
    // public void testBuildConfig2MainConfigNodeJs() throws Exception {
    // String nodeCmd = NodeJsRunner.detectNodeCommand();
    // assumeTrue( nodeCmd != null ); //skip if no node command detected.
    // long start = System.currentTimeMillis();
    // optimizer.optimize( loadProfile(
    // "testcase2/buildconfigWithMainConfig2.js" ), reporter, new NodeJsRunner(
    // nodeCmd ) );
    // long end = System.currentTimeMillis();
    //
    // log.debug( "total time ::" + (end - start) + "msec" );
    // }
    //
    // @Test
    // public void testWithSpaceRhino() throws Exception {
    // long start = System.currentTimeMillis();
    // optimizer.optimize( loadProfile( "testcase space/buildconfig1.js" ),
    // reporter, runner );
    // long end = System.currentTimeMillis();
    //
    // log.debug( "total time ::" + (end - start) + "msec" );
    // }
    //
    //
    @Test
    public void testWithSpaceNode() throws Exception {
        String nodeCmd = NodeJsRunner.detectNodeCommand();
        assumeTrue(nodeCmd != null); // skip if no node command detected.
        long start = System.currentTimeMillis();
        optimizer.optimize(loadProfile("testcase space/buildConfig1.js"), reporter, new NodeJsRunner(nodeCmd));
        long end = System.currentTimeMillis();

        log.debug("total time ::" + (end - start) + "msec");
    }

    @Test
    public void testWithNodeAndSpaceExternalRequire() throws Exception {
        String nodeCmd = NodeJsRunner.detectNodeCommand();
        assumeTrue(nodeCmd != null); // skip if no node command detected.
        long start = System.currentTimeMillis();
        File externalRequire = loadProfile("external space/r.js");
        optimizer.optimize(loadProfile("testcase space/buildConfig1.js"), externalRequire, reporter, new NodeJsRunner(nodeCmd));
        long end = System.currentTimeMillis();

        log.debug("total time ::" + (end - start) + "msec");
    }

    //
    // @Test
    // public void testBuildWithParameters() throws Exception {
    // String[] args = { "optimize=uglify" };
    // long start = System.currentTimeMillis();
    // optimizer.optimize( loadProfile( "testcase3/buildconfig3.js" ), reporter,
    // runner, args );
    // long end = System.currentTimeMillis();
    //
    // log.debug( "total time ::" + (end - start) + "msec" );
    // }
    //
    @Test
    public void testNodeBuildWithParameters() throws Exception {
        String[] args = { "optimize=uglify" };
        String nodeCmd = NodeJsRunner.detectNodeCommand();
        assumeTrue(nodeCmd != null); // skip if no node command detected.
        long start = System.currentTimeMillis();
        optimizer.optimize(loadProfile("testcase3/buildconfigNode3.js"), reporter, new NodeJsRunner(nodeCmd), args);
        long end = System.currentTimeMillis();

        log.debug("total time ::" + (end - start) + "msec");
    }

    @Test
    public void testNashornBuildWithParameters() throws Exception {
        checkNashornAvailability();

        String[] args = { "optimize=uglify" };
        long start = System.currentTimeMillis();
        optimizer.optimize(loadProfile("testcase3/buildconfigNode3.js"), reporter,
                new ScriptEngineRunner(new ScriptEngineManager().getEngineByName("nashorn")), args);
        long end = System.currentTimeMillis();

        log.debug("total time ::" + (end - start) + "msec");
    }

    /**
     * Check if the Nashorn engine is available. If not, halt the test.
     */
    private void checkNashornAvailability() {
        boolean scriptEngineAPIAvailable = false;
        boolean nashornEngineAvailable = false;
        try {
            Class.forName("javax.script.ScriptEngine", false, OptimizerTest.class.getClassLoader());
            scriptEngineAPIAvailable = true;
        } catch (ClassNotFoundException e) {
            scriptEngineAPIAvailable = false;
        }

        if (scriptEngineAPIAvailable) {
            ScriptEngine scriptEngine = new ScriptEngineManager().getEngineByName("nashorn");
            nashornEngineAvailable = scriptEngine != null;
        }

        assumeTrue("Nashorn engine not available.", nashornEngineAvailable);
    }

    private File loadProfile(String filename) throws URISyntaxException {
        URI uri = getClass().getClassLoader().getResource(filename).toURI();
        File buildconfigFile = new File(uri);
        assertTrue(buildconfigFile.exists());
        return buildconfigFile;
    }

}
