package com.github.bringking.maven.requirejs;

import java.io.File;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.mozilla.javascript.ErrorReporter;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeTrue;
import static org.mockito.Mockito.mock;

public class NodeJsRunnerTest {

    private static String fakeNode = null;
    private static String fakeNodeSpace = null;
    private static File mainScript = null;
    private static boolean shAvailable = false;

    @BeforeClass
    public static void setUpClass() throws Exception {
        fakeNode = new File(NodeJsRunnerTest.class.getResource("/testcase1/node").toURI()).getAbsolutePath();
        fakeNodeSpace = new File(NodeJsRunnerTest.class.getResource("/testcase space/node").toURI()).getAbsolutePath();
        mainScript = new File(NodeJsRunnerTest.class.getResource("/testcase1/buildconfig1.js").toURI());

        CommandLine cmdLine = CommandLine.parse("sh --version");
        try {
            if (new DefaultExecutor().execute(cmdLine) == 0) {
                shAvailable = true;
            }
        } catch (Exception e) {
            shAvailable = false;
        }
    }

    private NodeJsRunner runner;
    private ErrorReporter errorReporter;

    @Before
    public void setUp() {
        assumeTrue(shAvailable);
        runner = new NodeJsRunner(fakeNode);
        errorReporter = mock(ErrorReporter.class);
    }

    @Test
    public void testExec() {
        String[] args = { "0", "arg1", "arg2" };
        ExitStatus exitStatus = runner.exec(mainScript, args, errorReporter);
        assertEquals(0, exitStatus.getExitCode());
    }

    @Test
    public void testExec_NonZeroExitCode() {
        String[] args = { "2", "arg1", "arg2" };
        ExitStatus exitStatus = runner.exec(mainScript, args, errorReporter);
        assertEquals(1, exitStatus.getExitCode());
    }

    @Test
    public void testExec_MissingNode() {
        String[] args = { "arg0", "arg1", "arg2" };
        runner = new NodeJsRunner(fakeNode + "_x");
        ExitStatus exitStatus = runner.exec(mainScript, args, errorReporter);
        assertEquals(1, exitStatus.getExitCode());
    }

    @Test
    public void testExec_NodeSpace() {
        String[] args = { "0", "arg1", "arg2" };
        runner = new NodeJsRunner(fakeNodeSpace);
        ExitStatus exitStatus = runner.exec(mainScript, args, errorReporter);
        assertEquals(0, exitStatus.getExitCode());
    }

}
