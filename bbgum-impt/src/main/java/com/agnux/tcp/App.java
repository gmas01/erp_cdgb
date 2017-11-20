package com.agnux.tcp;

import com.maxima.bbgum.ServerReply;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;


public class App {

    public static void main(String[] args) {
        String host = args[1];
        int port = Integer.getInteger(args[2]);
        sendRequest(host, port, App.readStandartInput(System.in).getBytes());
    }

    private static String readStandartInput(InputStream p) {
        InputStreamReader ir = new InputStreamReader(p);
        BufferedReader rf = new BufferedReader(ir);
        StringBuilder sb = new StringBuilder();
        for (;;) {
            try {
                String l = rf.readLine();
                if ((l = rf.readLine()) == null) {
                    break;
                }

                sb.append(l);
            } catch (IOException ex) {
                System.err.println(ex.getMessage());
                System.exit(1);
            }
        }

        String rs = sb.toString();

        if (rs.length() == 0) {
            System.err.println("Request without content");
            System.exit(1);
        }

        return rs;
    }

    private static void sendRequest(String host, Integer port, byte[] req) {
        try {
            BbgumProxy bbgumProxy = new BbgumProxy();

            ServerReply reply = bbgumProxy.uploadBuff(host, port, req);
            String msg = "core reply code: " + reply.getReplyCode();
            if (reply.getReplyCode() == 0) {
                System.out.println(msg);
                System.exit(0);
            } else {
                System.err.println(msg);
                System.exit(1);
            }
        } catch (BbgumProxyError ex) {
            System.err.println(ex.getMessage());
            System.exit(1);
        }
    }
}
