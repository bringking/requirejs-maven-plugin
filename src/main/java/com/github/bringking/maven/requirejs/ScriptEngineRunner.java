/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is Rhino code, released
 * May 6, 1998.
 *
 * The Initial Developer of the Original Code is
 * Netscape Communications Corporation.
 * Portions created by the Initial Developer are Copyright (C) 1997-1999
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 *
 * Alternatively, the contents of this file may be used under the terms of
 * the GNU General Public License Version 2 or later (the "GPL"), in which
 * case the provisions of the GPL are applicable instead of those above. If
 * you wish to allow use of your version of this file only under the terms of
 * the GPL and not to allow others to use your version of this file under the
 * MPL, indicate your decision by deleting the provisions above and replacing
 * them with the notice and other provisions required by the GPL. If you do
 * not delete the provisions above, a recipient may use your version of this
 * file under either the MPL or the GPL.
 *
 * ***** END LICENSE BLOCK ***** */
package com.github.bringking.maven.requirejs;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;

import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;

import org.codehaus.plexus.util.IOUtil;
import org.mozilla.javascript.ErrorReporter;

/**
 * Class for running a single js file. This runner uses a provided ScriptEngine
 * to execute the script.
 *
 * @author Stratehm
 */
public class ScriptEngineRunner implements Runner {

    private static final String CLASSPATH_READFULLY_NASHORN_JS = "/readFullyNashorn.js";

    private ScriptEngine scriptEngine;

    public ScriptEngineRunner(ScriptEngine scriptEngine) {
        this.scriptEngine = scriptEngine;
    }

    @Override
    public ExitStatus exec(File mainScript, String[] args, ErrorReporter reporter) {
        final ExitStatus status = new ExitStatus();

        try {
            scriptEngine.getContext().getBindings(ScriptContext.ENGINE_SCOPE).put("arguments", args);
            Compilable compilable = (Compilable) scriptEngine;

            CompiledScript compileReadFully = compilable.compile(new FileReader(getClasspathReadFullyFile()));
            CompiledScript compileMainScript = compilable.compile(new FileReader(mainScript));

            compileReadFully.eval();
            compileMainScript.eval();

        } catch (Exception e) {
            throw new ScriptEngineRunnerException(e.getMessage(), e);
        }

        return status;
    }

    private File getClasspathReadFullyFile() throws IOException {
        File readFullyFile = File.createTempFile("readFullyNashorn", "js");
        readFullyFile.deleteOnExit();
        FileOutputStream out = null;
        InputStream in = null;
        try {
            in = getClass().getResourceAsStream(CLASSPATH_READFULLY_NASHORN_JS);
            out = new FileOutputStream(readFullyFile);
            IOUtil.copy(in, out);
        } finally {
            IOUtil.close(in);
            IOUtil.close(out);
        }

        return readFullyFile;
    }

}
