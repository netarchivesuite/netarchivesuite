package dk.netarkivet.harvester.webinterface.servlet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.antiaction.common.filter.Caching;
import com.antiaction.common.templateengine.Template;
import com.antiaction.common.templateengine.TemplateParts;
import com.antiaction.common.templateengine.TemplatePlaceBase;
import com.antiaction.common.templateengine.TemplatePlaceHolder;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.Constants;
import dk.netarkivet.common.utils.Settings;

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

        Caching.caching_disable_headers(resp);

        Template template = environment.templateMaster.getTemplate("master.tpl");

        TemplatePlaceHolder titlePlace = TemplatePlaceBase.getTemplatePlaceHolder("title");
        TemplatePlaceHolder headingPlace = TemplatePlaceBase.getTemplatePlaceHolder("heading");
        TemplatePlaceHolder contentPlace = TemplatePlaceBase.getTemplatePlaceHolder("content");
        TemplatePlaceHolder versionPlace = TemplatePlaceBase.getTemplatePlaceHolder("version");
        TemplatePlaceHolder environmentPlace = TemplatePlaceBase.getTemplatePlaceHolder("environment");

        List<TemplatePlaceBase> placeHolders = new ArrayList<TemplatePlaceBase>();
        placeHolders.add(titlePlace);
        placeHolders.add(headingPlace);
        placeHolders.add(contentPlace);
        placeHolders.add(versionPlace);
        placeHolders.add(environmentPlace);

        TemplateParts templateParts = template.filterTemplate(placeHolders, resp.getCharacterEncoding());

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
        }

        if (titlePlace != null) {
            titlePlace.setText("Runnings job monitor");
        }

        if (headingPlace != null) {
            headingPlace.setText("Runnings job monitor");
        }

        if (contentPlace != null) {
            contentPlace.setText(sb.toString());
        }

        if (versionPlace != null) {
            versionPlace.setText(Constants.getVersionString());
        }

        if (environmentPlace != null) {
            environmentPlace.setText(Settings.get(CommonSettings.ENVIRONMENT_NAME));
        }

        for (int i = 0; i < templateParts.parts.size(); ++i) {
            out.write(templateParts.parts.get(i).getBytes());
        }
        out.flush();
        out.close();

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

}
