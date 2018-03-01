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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.antiaction.common.filter.Caching;
import com.antiaction.common.templateengine.TemplateBuilderFactory;

import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.harvester.HarvesterSettings;
import dk.netarkivet.harvester.datamodel.HarvestChannel;
import dk.netarkivet.heritrix3.monitor.Heritrix3JobMonitor;
import dk.netarkivet.heritrix3.monitor.Heritrix3JobMonitorThread;
import dk.netarkivet.heritrix3.monitor.HistoryServlet;
import dk.netarkivet.heritrix3.monitor.NASEnvironment;
import dk.netarkivet.heritrix3.monitor.NASUser;
import dk.netarkivet.heritrix3.monitor.ResourceAbstract;
import dk.netarkivet.heritrix3.monitor.ResourceManagerAbstract;
import dk.netarkivet.heritrix3.monitor.HttpLocaleHandler.HttpLocale;

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
    public void resource_service(ServletContext servletContext, NASUser nas_user, HttpServletRequest req, HttpServletResponse resp, HttpLocale httpLocale, int resource_id, List<Integer> numerics, String pathInfo) throws IOException {
        if (NASEnvironment.contextPath == null) {
            NASEnvironment.contextPath = req.getContextPath();
        }
        if (NASEnvironment.servicePath == null) {
            NASEnvironment.servicePath = req.getContextPath() + req.getServletPath() + "/";
        }
        String method = req.getMethod().toUpperCase();
        if (resource_id == R_INDEX) {
            if ("GET".equals(method)) {
                index(req, resp, httpLocale, numerics);
            }
        }
    }

    public static class HarvestChannelStructure {
        public HarvestChannel hc;
        public List<Heritrix3JobMonitor> h3JobList = new ArrayList<Heritrix3JobMonitor>();
        public HarvestChannelStructure(HarvestChannel hc) {
            this.hc = hc;
        }
    }

    public void index(HttpServletRequest req, HttpServletResponse resp, HttpLocale httpLocale, List<Integer> numerics) throws IOException {
        Locale locale = httpLocale.locale;
        resp.setContentType("text/html; charset=UTF-8");
        ServletOutputStream out = resp.getOutputStream();
        Caching.caching_disable_headers(resp);

        TemplateBuilderFactory<MasterTemplateBuilder> masterTplBuilderFactory = TemplateBuilderFactory.getInstance(environment.templateMaster, "master.tpl", "UTF-8", MasterTemplateBuilder.class);
        MasterTemplateBuilder masterTplBuilder = masterTplBuilderFactory.getTemplateBuilder();

        StringBuilder sb = new StringBuilder();

        List<Heritrix3JobMonitor> h3JobsList = environment.h3JobMonitorThread.getRunningH3Jobs();
        Heritrix3JobMonitor h3Job;

        // Restart H3 job monitor thread.
        String action = req.getParameter("action");
        if (action != null && action.length() > 0) {
            if ("restart".equalsIgnoreCase(action)) {
                if (environment.h3JobMonitorThread.thread != null) {
                    synchronized (environment.h3JobMonitorThread.thread) {
                        if (!environment.h3JobMonitorThread.thread.isAlive()) {
                            // The h3JobMonitorThread is dead. Restart it.
                            Heritrix3JobMonitorThread newH3JobMonitor = new Heritrix3JobMonitorThread(environment);
                            try {
                                newH3JobMonitor.init();
                                newH3JobMonitor.start();
                            }
                            catch (Throwable t) {
                            	t.printStackTrace();
                            }
                            environment.h3JobMonitorThread = newH3JobMonitor; 
                        }
                    }
                }
            }
        }

        sb.append("<a href=\"");
        sb.append(NASEnvironment.servicePath);
        sb.append("config/");
        sb.append("\" class=\"btn btn-default\">");
        sb.append(environment.I18N.getString(locale, "configure"));
        sb.append("</a>");
        sb.append("<br />\n");
        sb.append("<br />\n");

        // Check if H3 job monitor thread is still running.
        if (environment.h3JobMonitorThread.thread != null) {
            synchronized (environment.h3JobMonitorThread.thread) {
                if (!environment.h3JobMonitorThread.thread.isAlive()) {
                    sb.append("The H3 job monitor thread is not running anymore. ");
                    sb.append("<a href=\"?action=restart");
                    sb.append("\"");
                    sb.append(" class=\"btn btn-default\">");
                    sb.append("Restart");
                    sb.append("</a>");
                    sb.append("<br />\n");
                    sb.append("<pre>");
                    sb.append("Stacktrace[]:");
                    HistoryServlet.throwable_stacktrace_dump(environment.h3JobMonitorThread.throwable, sb);
                    sb.append("</pre>");
                    sb.append("<br />\n");
                    sb.append("<br />\n");
                }
            }
        }

        List<HarvestChannelStructure> hcList = new ArrayList<HarvestChannelStructure>();
        Map<String, HarvestChannelStructure> hcMap = new HashMap<String, HarvestChannelStructure>();

        Iterator<HarvestChannel> hcIter = Heritrix3JobMonitorThread.harvestChannelDAO.getAll(true);
        HarvestChannel hc;
        HarvestChannelStructure hcs;
        while (hcIter.hasNext()) {
            hc = hcIter.next();
            hcs = new HarvestChannelStructure(hc);
            hcList.add(hcs);
            hcMap.put(hc.getName(), hcs);
        }

        Iterator<Heritrix3JobMonitor> j3JobIter = h3JobsList.iterator();
        String channelStr;
        while (j3JobIter.hasNext()) {
            h3Job = j3JobIter.next();
            if (h3Job.job != null) {
            	channelStr = h3Job.job.getChannel();
            	if (channelStr != null) {
                    hcs = hcMap.get(channelStr);
                    hcs.h3JobList.add(h3Job);
            	}
            }
        }

        for (int i=0; i<hcList.size(); ++i) {
            hcs = hcList.get(i);
            sb.append("<h5>");
            sb.append(hcs.hc.getName());
            if (hcs.hc.isDefault()) {
                sb.append("*");
            }
            sb.append("&nbsp;");
            sb.append("(");
            sb.append(environment.I18N.getString(locale, "harvest.channel.type"));
            sb.append(": ");
            if (hcs.hc.isSnapshot()) {
                sb.append(environment.I18N.getString(locale, "harvest.channel.type.broad"));
            } else {
                sb.append(environment.I18N.getString(locale, "harvest.channel.type.focused"));
            }
            sb.append(")");
            sb.append("</h5>\n");
            if (hcs.h3JobList.size() > 0) {
                for (int j=0; j<hcs.h3JobList.size(); ++j) {
                    h3Job = hcs.h3JobList.get(j);
                    if (j > 0) {
                        sb.append("&nbsp;");
                    }
                    sb.append("<a href=\"");
                    sb.append(NASEnvironment.servicePath);
                    sb.append("job/");
                    sb.append(h3Job.jobId);
                    sb.append("/");
                    sb.append("\" class=\"btn btn-default\">");
                    sb.append("Job ");
                    sb.append(h3Job.jobId);
                    long lines = (h3Job.idxFile.length() / 8) - 1;
                    if (lines > 0) {
                        sb.append(" (");
                        sb.append(lines);
                        sb.append(")");
                    }
                    sb.append("</a>\n");
                }
            } else {
                sb.append("<p>");
                sb.append(environment.I18N.getString(locale, "running.jobs.monitor.not.on.this.channel"));
                sb.append("</p>\n");
            }
        }

        StringBuilder menuSb = masterTplBuilder.buildMenu(new StringBuilder(), req, locale, null);
        //String url = req.getPathInfo();
        //HTMLUtils.generateNavigationTree(menuSb, url, locale);

        masterTplBuilder.insertContent("H3 Remote Access", menuSb.toString(), httpLocale.generateLanguageLinks(),
        		environment.I18N.getString(locale, "pagetitle;h3.remote.access"), sb.toString(),
        		"<meta http-equiv=\"refresh\" content=\""+Settings.get(HarvesterSettings.HARVEST_MONITOR_REFRESH_INTERVAL)+"\"/>\n").write(out);

        out.flush();
        out.close();
    }

}
