package com.gregory.utils.taillog;

import com.jcraft.jsch.JSchException;
import org.atmosphere.cpr.AtmosphereHandler;
import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.AtmosphereResourceEvent;
import org.atmosphere.cpr.Broadcaster;
import org.json.simple.JSONValue;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Gera
 * Date: 15.02.14
 * Time: 1:45
 * To change this template use File | Settings | File Templates.
 */
public class SSHLogViewerHandler implements AtmosphereHandler<HttpServletRequest, HttpServletResponse>, SSHLogHandler{
    //    private final static String FILE_TO_WATCH = "/var/log/";
//    private final static String FILE_TO_WATCH = "D://Gera//GitHub//logviewer//tests";
//    private static Tailer tailer;
    private SSHLogTailer sshListener;
    private Config config;
    private Broadcaster GLOBAL_BROADCASTER = null;

    //private Map<String, Broadcaster> brs = new HashMap<String, Broadcaster>();

//    private static List<String> watchableLogs = new ArrayList<String>();

    public SSHLogViewerHandler() {
        config = Config.load();
//        final File logsDir = new File(FILE_TO_WATCH);
//        if (logsDir.exists() && logsDir.isDirectory()) {
//            File[] logs = logsDir.listFiles();
//            for (File f : logs) {
//                if (f.getName().endsWith(".log")) {
//                    watchableLogs.add(f.getName());
//                }
//            }
//        } else {
//            System.out.println("either logsDir doesn't exist or is not a folder");
//        }

    }

    @Override
    public void onRequest(final AtmosphereResource<HttpServletRequest, HttpServletResponse> event) throws IOException {
        HttpServletRequest req = event.getRequest();
        HttpServletResponse res = event.getResponse();
        res.setContentType("text/html");
        res.addHeader("Cache-Control", "private");
        res.addHeader("Pragma", "no-cache");

        if (req.getMethod().equalsIgnoreCase("GET")) {
            event.suspend();
            if (GLOBAL_BROADCASTER == null) GLOBAL_BROADCASTER = event.getBroadcaster();

            if (config.getLogFiles().size() != 0) {
                GLOBAL_BROADCASTER.broadcast(asJsonArray("servers", config.getLogFiles()));
            }

            res.getWriter().flush();
        } else { // POST

            // Very lame... req.getParameterValues("log")[0] doesn't work
            final String postPayload = req.getReader().readLine();
            String log = null;
            if (postPayload != null && postPayload.contains("log=") && postPayload.contains("host=")) {
                //TODO: get from payload log and host with normal regexp
//                sshListener = SSHLogTailer.create(new File(FILE_TO_WATCH + "//" + postPayload.split("=")[1]), this, 500);
                String[] str = postPayload.split("&");
                String host = str[0].split("=")[1];
                log = str[1].split("=")[1];

                try {
                    sshListener = SSHLogTailer.create(config.getTargetConfig(host, log), this, true);
                } catch (JSchException e) {
                    GLOBAL_BROADCASTER.broadcast(asJson("exception", e.getMessage()));
                }
            }
            GLOBAL_BROADCASTER.broadcast(asJson("filename", log));
            res.getWriter().flush();
        }
    }

    @Override
    public void onStateChange(
            final AtmosphereResourceEvent<HttpServletRequest, HttpServletResponse> event) throws IOException {

        HttpServletResponse res = event.getResource().getResponse();
        if (event.isResuming()) {
            res.getWriter().write("Atmosphere closed<br/>");
            res.getWriter().write("</body></html>");
        } else {
            res.getWriter().write(event.getMessage().toString());
        }
        res.getWriter().flush();
    }

    private static List<String> buffer = new ArrayList<String>();

    @Override
    public void destroy() {
        sshListener.stop();
    }


    public void handle(String line) {
        buffer.add(line);
        if (buffer.size() == 10) {
            GLOBAL_BROADCASTER.broadcast(asJsonArray("tail", buffer));
            buffer.clear();
        }
    }

    protected String asJson(final String key, final String value) {
        return "{\"" + key + "\":\"" + value + "\"}";
    }

    protected String asJsonArray(final String key, final List<String> list) {

        return ("{\"" + key + "\":" + JSONValue.toJSONString(list) + "}");
    }
}
