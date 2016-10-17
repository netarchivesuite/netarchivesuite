package dk.netarkivet.harvester.webinterface.servlet;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class IndexResource implements ResourceAbstract {

    private NASEnvironment environment;

    protected int R_INDEX = -1;

    @Override
    public void resources_init(NASEnvironment environment) {
        this.environment = environment;
    }

    @Override
    public void resources_add(ResourceManagerAbstract resourceManager) {
        R_INDEX = resourceManager.resource_add(this, "/", false);
    }

    @Override
    public void resource_service(ServletContext servletContext, NASUser nas_user, HttpServletRequest req, HttpServletResponse resp, int resource_id, List<Integer> numerics, String pathInfo) throws IOException {
        if (NASEnvironment.contextPath == null) {
            NASEnvironment.contextPath = req.getContextPath();
        }
        if (NASEnvironment.servicePath == null) {
            NASEnvironment.servicePath = req.getContextPath() + req.getServletPath() + "/";
        }
        String method = req.getMethod().toUpperCase();
        if (resource_id == R_INDEX) {
            if ("GET".equals(method)) {
                index(req, resp, numerics);
            }
        }
    }

    public void index(HttpServletRequest req, HttpServletResponse resp, List<Integer> numerics) throws IOException {
        resp.setContentType("text/html; charset=UTF-8");
        ServletOutputStream out = resp.getOutputStream();

        StringBuilder sb = new StringBuilder();

        sb.append("Heritrix3 command control center.");
        sb.append("<br />\n");
        sb.append("<br />\n");

        List<Heritrix3JobMonitor> h3JobsList = environment.h3JobMonitorThread.getRunningH3Jobs();
        Heritrix3JobMonitor h3Jobmonitor;

        sb.append("Runnings jobs:");
        sb.append(h3JobsList.size());
        sb.append("<br />\n");

        Iterator<Heritrix3JobMonitor> iter = h3JobsList.iterator();
        while (iter.hasNext()) {
            h3Jobmonitor = iter.next();
            sb.append("jobId: ");
            sb.append("<a href=\"");
            sb.append(NASEnvironment.servicePath);
            sb.append("job/");
            sb.append(h3Jobmonitor.jobId);
            sb.append("/");
            sb.append("\">");
            sb.append(h3Jobmonitor.jobId);
            sb.append("</a>");
            sb.append("<br />\n");
            sb.append("Channel: ");
            sb.append(h3Jobmonitor.job.getChannel());
            sb.append("<br />\n");
            sb.append("isSnapshop: ");
            sb.append(h3Jobmonitor.job.isSnapshot());
            sb.append("<br />\n");
            sb.append("Heritrix3 WebUI: ");
            sb.append("<a href=\"");
            sb.append(h3Jobmonitor.hostUrl);
            sb.append("/");
            sb.append("\">");
            sb.append(h3Jobmonitor.hostUrl);
            sb.append("</a>");
            sb.append("<br />\n");
            long lines = (h3Jobmonitor.idxFile.length() / 8) - 1;
            sb.append("CrawlLog number of cached lines: ");
            sb.append(lines);
            sb.append("<br />\n");
            sb.append("<br />\n");

            /*
            sb.append("<pre>");

            AnypathResult anypathResult = h3wrapper.anypath(jobResult.job.crawlLogFilePath, null, null);
            byte[] tmpBuf = new byte[8192];
            int read;
            try {
                while ((read = anypathResult.in.read(tmpBuf)) != -1) {
                    sb.append(new String(tmpBuf, 0, read));
                }
                anypathResult.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            sb.append("</pre>");
*/

        }

        out.write(sb.toString().getBytes("UTF-8"));
        out.flush();
        out.close();
    }

}
