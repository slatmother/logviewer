package com.gregory.utils.taillog;

import com.jcraft.jsch.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Created with IntelliJ IDEA.
 * User: Gera
 * Date: 15.02.14
 * Time: 1:49
 * To change this template use File | Settings | File Templates.
 */
public class SSHLogTailer implements Runnable {
    private List<String> buffer = new ArrayList<String>();
    private boolean debug;
    private SSHLogHandler handler = null;
    private Channel channel;
    private Session session;

    public static SSHLogTailer create(Properties config, SSHLogHandler handler) throws JSchException {
        SSHLogTailer tailer = new SSHLogTailer();

        JSch jsCh = new JSch();
        Session session = jsCh.getSession(
                config.getProperty("host"),
                config.getProperty("user"),
                22);
        session.setConfig("PreferredAuthentications", "password");
        //TODO: encrypt password
        session.setPassword(config.getProperty("passwd"));
        session.connect();

        Channel channel = session.openChannel("exec");
        ((ChannelExec) channel).setCommand("tail -f " + config.getProperty("log"));

        tailer.setSession(session);
        tailer.setHandler(handler);
        return tailer;
    }

    public static SSHLogTailer create(Properties config, SSHLogHandler handler, boolean debug) throws JSchException {
        SSHLogTailer t = create(config, handler);

        if (debug) {
            t.debug = true;
        }

        return t;
    }

    @Override
    public void run() {
        try {
            channel.connect();
            InputStream in = channel.getInputStream();

            byte[] tmp = new byte[1024];
            while (true) {
                while (in.available() > 0) {
                    int i = in.read(tmp, 0, 1024);

                    if (i < 0) {
                        break;
                    }
                    handler.handle(new String(tmp, 0, i));
                }

                if (channel.isClosed()) {
                    if (debug) handler.handle("exit-status: " + channel.getExitStatus());
                    break;
                }

                try {
                    Thread.sleep(1000);
                } catch (Exception ee) {
                    handler.handle(ee.getMessage());
                }
            }
        } catch (JSchException e) {
            handler.handle(e.getMessage());
        } catch (IOException e) {
            handler.handle(e.getMessage());
        }
    }

    public void stop() {
        if (channel != null && channel.isConnected() && !channel.isClosed()) {
            channel.disconnect();
            session.disconnect();
        }
    }

    private void setHandler(SSHLogHandler handler) {
        this.handler = handler;
    }

    private void setChannel(Channel channel) {
        this.channel = channel;
    }

    private void setSession(Session session) {
        this.session = session;
    }
}
