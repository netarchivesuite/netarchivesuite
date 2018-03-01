/*
 * #%L
 * Netarchivesuite - heritrix 3 monitor
 * %%
 * Copyright (C) 2005 - 2018 The Royal Danish Library, 
 *             the National Library of France and the Austrian National Library.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */

package dk.netarkivet.heritrix3.monitor.resources;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.netarchivesuite.heritrix3wrapper.StreamResult;
import org.netarchivesuite.heritrix3wrapper.jaxb.Job;
import org.netarchivesuite.heritrix3wrapper.jaxb.Report;

import com.antiaction.common.filter.Caching;
import com.antiaction.common.templateengine.TemplateBuilderFactory;

import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.harvester.HarvesterSettings;
import dk.netarkivet.heritrix3.monitor.Heritrix3JobMonitor;
import dk.netarkivet.heritrix3.monitor.NASEnvironment;
import dk.netarkivet.heritrix3.monitor.NASUser;
import dk.netarkivet.heritrix3.monitor.ResourceAbstract;
import dk.netarkivet.heritrix3.monitor.ResourceManagerAbstract;
import dk.netarkivet.heritrix3.monitor.HttpLocaleHandler.HttpLocale;

public class H3ReportResource implements ResourceAbstract {

    private NASEnvironment environment;

    protected int R_REPORT = -1;

    @Override
    public void resources_init(NASEnvironment environment) {
        this.environment = environment;
    }

    @Override
    public void resources_add(ResourceManagerAbstract resourceManager) {
        R_REPORT = resourceManager.resource_add(this, "/job/<numeric>/report/", false);
    }

    @Override
    public void resource_service(ServletContext servletContext, NASUser nas_user, HttpServletRequest req, HttpServletResponse resp, HttpLocale httpLocale, int resource_id, List<Integer> numerics, String pathInfo) throws IOException {
        if (NASEnvironment.contextPath == null) {
            NASEnvironment.contextPath = req.getContextPath();
        }
        if (NASEnvironment.servicePath == null) {
            NASEnvironment.servicePath = req.getContextPath() + req.getServletPath() + "/";
        }
        String method = req.getMethod().toUpperCase();
        if (resource_id == R_REPORT) {
            if ("GET".equals(method)) {
                report(req, resp, httpLocale, numerics);
            }
        }
    }

    public void report(HttpServletRequest req, HttpServletResponse resp, HttpLocale httpLocale, List<Integer> numerics) throws IOException {
        Locale locale = httpLocale.locale;
        resp.setContentType("text/html; charset=UTF-8");
        ServletOutputStream out = resp.getOutputStream();
        Caching.caching_disable_headers(resp);

        TemplateBuilderFactory<MasterTemplateBuilder> masterTplBuilderFactory = TemplateBuilderFactory.getInstance(environment.templateMaster, "master.tpl", "UTF-8", MasterTemplateBuilder.class);
        MasterTemplateBuilder masterTplBuilder = masterTplBuilderFactory.getTemplateBuilder();

        StringBuilder sb = new StringBuilder();

        String reportStr = req.getParameter("report");

        long jobId = numerics.get(0);
        Heritrix3JobMonitor h3Job = environment.h3JobMonitorThread.getRunningH3Job(jobId);
        Job job;

        if (h3Job != null && h3Job.isReady()) {
            if (h3Job.jobResult != null && h3Job.jobResult.job != null) {
                job = h3Job.jobResult.job;
                Report report;
                for (int i=0; i<job.reports.size(); ++i) {
                    report = job.reports.get(i);
                    if (i > 0) {
                        sb.append("&nbsp;");
                    }
                    sb.append("<a href=\"");
                    sb.append(NASEnvironment.servicePath);
                    sb.append("job/");
                    sb.append(h3Job.jobId);
                    sb.append("/report/?report=");
                    sb.append(report.className);
                    sb.append("\" class=\"btn btn-default\">");
                    sb.append(report.shortName);
                    sb.append("</a>");
                }
                if (reportStr != null && reportStr.length() > 0) {
                    sb.append("<br />\n");
                    sb.append("<h5>");
                    sb.append(reportStr);
                    sb.append("</h5>");
                    sb.append("<pre>");
                    StreamResult anypathResult = h3Job.h3wrapper.path("job/" + h3Job.jobname + "/report/" + reportStr, null, null);
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
                }
            }
        }

        StringBuilder menuSb = masterTplBuilder.buildMenu(new StringBuilder(), req, locale, h3Job);

        masterTplBuilder.insertContent("Job "+ jobId + " Reports", menuSb.toString(), httpLocale.generateLanguageLinks(), "Job " + jobId + " Reports", sb.toString(),
        		"<meta http-equiv=\"refresh\" content=\""+Settings.get(HarvesterSettings.HARVEST_MONITOR_REFRESH_INTERVAL)+"\"/>\n").write(out);

        out.flush();
        out.close();
    }

}
