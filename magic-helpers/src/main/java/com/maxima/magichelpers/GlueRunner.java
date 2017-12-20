/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.maxima.magichelpers;

import java.io.IOException;
import java.util.logging.Logger;

/**
 *
 * @author optimus
 */
public class GlueRunner extends RunExternalProg {

    private String glueDir = null;
    

    public GlueRunner(final String glueDir, Logger log) {
        super(log);
        this.glueDir = glueDir;
    }

    public int go(String script, String params, boolean captureStdout, String processFileOutput) throws IOException, InterruptedException{
        return this.exec(this.glueDir, script, params, captureStdout, processFileOutput);
    }
}