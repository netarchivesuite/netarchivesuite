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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.antiaction.common.filter.Caching;
import com.antiaction.common.templateengine.TemplateBuilderFactory;

import dk.netarkivet.heritrix3.monitor.NASEnvironment;
import dk.netarkivet.heritrix3.monitor.NASEnvironment.StringMatcher;
import dk.netarkivet.heritrix3.monitor.NASUser;
import dk.netarkivet.heritrix3.monitor.ResourceAbstract;
import dk.netarkivet.heritrix3.monitor.ResourceManagerAbstract;
import dk.netarkivet.heritrix3.monitor.HttpLocaleHandler.HttpLocale;

public class ConfigResource implements ResourceAbstract {

    private NASEnvironment environment;

    protected int R_CONFIG = -1;

    @Override
    public void resources_init(NASEnvironment environment) {
        this.environment = environment;
    }

    @Override
    public void resources_add(ResourceManagerAbstract resourceManager) {
        R_CONFIG = resourceManager.resource_add(this, "/config/", false);
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
        if (resource_id == R_CONFIG) {
            if ("GET".equals(method) || "POST".equals(method)) {
                config(req, resp, httpLocale, numerics);
            }
        }
    }

    public void config(HttpServletRequest req, HttpServletResponse resp, HttpLocale httpLocale, List<Integer> numerics) throws IOException {
        Locale locale = httpLocale.locale;
        resp.setContentType("text/html; charset=UTF-8");
        ServletOutputStream out = resp.getOutputStream();
        Caching.caching_disable_headers(resp);

        TemplateBuilderFactory<ConfigTemplateBuilder> configTplBuilderFactory = TemplateBuilderFactory.getInstance(environment.templateMaster, "h3config.tpl", "UTF-8", ConfigTemplateBuilder.class);
        ConfigTemplateBuilder configTplBuilder = configTplBuilderFactory.getTemplateBuilder();

        StringBuilder sb = new StringBuilder();
        StringBuilder enabledhostsSb = new StringBuilder();
        List<String> invalidPatternsList = new LinkedList<String>();

        String method = req.getMethod().toUpperCase();
        if ("POST".equals(method)) {
            String enabledhostsStr = req.getParameter("enabledhosts");
            String tmpStr;
            if (enabledhostsStr != null) {
                BufferedReader reader = new BufferedReader(new StringReader(enabledhostsStr));
                List<String> enabledhostsList = new LinkedList<String>();
                while ((tmpStr = reader.readLine()) != null) {
                    enabledhostsList.add(tmpStr);
                }
                reader.close();
                environment.replaceH3HostnamePortRegexList(enabledhostsList, invalidPatternsList);
                environment.h3JobMonitorThread.updateH3HostnamePortFilter();
            }
        }

        synchronized (environment.h3HostPortAllowRegexList) {
            StringMatcher stringMatcher;
            for (int i=0; i<environment.h3HostPortAllowRegexList.size(); ++i) {
                stringMatcher = environment.h3HostPortAllowRegexList.get(i);
                enabledhostsSb.append(stringMatcher.str);
                enabledhostsSb.append("\n");
            }
        }

        if (invalidPatternsList.size() > 0) {
            sb.append("<h5>");
            sb.append(environment.I18N.getString(locale, "h5.invalid.regexes"));
            sb.append(":</h5>\n");
        	for (int i=0; i<invalidPatternsList.size(); ++i) {
                sb.append(invalidPatternsList.get(i));
                sb.append("<br />\n");
        	}
        }

        synchronized (environment.h3JobMonitorThread.h3HostnamePortEnabledList) {
            sb.append("<h5>");
            sb.append(environment.I18N.getString(locale, "running.jobs.monitor.crawllog.cache.enabled.for"));
            sb.append(":</h5>\n");
            if (environment.h3JobMonitorThread.h3HostnamePortEnabledList.size() > 0) {
                for (int i=0; i<environment.h3JobMonitorThread.h3HostnamePortEnabledList.size(); ++i) {
                    sb.append(environment.h3JobMonitorThread.h3HostnamePortEnabledList.get(i));
                    sb.append("<br />\n");
                }
            } else {
                sb.append("<p>");
                sb.append(environment.I18N.getString(locale, "running.jobs.monitor.no.hosts.enabled"));
                sb.append("</p>\n");
            }
        }
        sb.append("<br />\n");
        synchronized (environment.h3JobMonitorThread.h3HostnamePortDisabledList) {
            sb.append("<h5>");
            sb.append(environment.I18N.getString(locale, "running.jobs.monitor.crawllog.cache.disabled.for"));
            sb.append("</h5>\n");
            if (environment.h3JobMonitorThread.h3HostnamePortDisabledList.size() > 0) {
                for (int i=0; i<environment.h3JobMonitorThread.h3HostnamePortDisabledList.size(); ++i) {
                    sb.append(environment.h3JobMonitorThread.h3HostnamePortDisabledList.get(i));
                    sb.append("<br />\n");
                }
            } else {
                sb.append("<p>");
                sb.append(environment.I18N.getString(locale, "running.jobs.monitor.no.hosts.disable"));
                sb.append("</p>\n");
            }
        }

        StringBuilder menuSb = configTplBuilder.buildMenu(new StringBuilder(), req, locale, null);

        configTplBuilder.insertContent("H3 Remote Access Config", menuSb.toString(), httpLocale.generateLanguageLinks(), "H3 Remote Access Config",
        		enabledhostsSb.toString(), sb.toString(), "").write(out);

        out.flush();
        out.close();
    }

}
