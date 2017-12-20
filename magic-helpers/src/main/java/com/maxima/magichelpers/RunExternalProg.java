/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.maxima.magichelpers;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author optimus
 */
public class RunExternalProg {

    protected Logger out;

    public RunExternalProg(Logger out) {
        this.out = out;
    }

    protected int exec(String path, String executable, String params, boolean captureStdout, String processFileOutput) throws IOException, InterruptedException {
        String cmd = executable + " " + params;
        out.log(Level.INFO, cmd);

        File pathToExecutable = new File(path + "/" + executable);
        List<String> list = new ArrayList<String>();
        list.add(pathToExecutable.getAbsolutePath());
        list.addAll(Arrays.asList(params.split(" ")));
        ProcessBuilder builder = new ProcessBuilder(list);

        // this is where you set the root folder for the executable to run with
        builder.directory(new File(path).getAbsoluteFile());
        builder.redirectErrorStream(true);
        Process process = builder.start();

        Scanner s = new Scanner(process.getInputStream());

        if (captureStdout) {

            StringBuilder text;
            text = new StringBuilder();

            while (s.hasNextLine()) {
                text.append(s.nextLine());
                text.append("\n");
            }

            File fileOutput = new File(processFileOutput);
            BufferedWriter bwr = new BufferedWriter(new FileWriter(fileOutput));
            bwr.write(text.toString());
            bwr.flush();
            bwr.close();

            s.close();
        } else {
            while (s.hasNextLine()) {
                out.log(Level.INFO, s.nextLine());
            }
        }

        int result = process.waitFor();
        return result;
    }
}