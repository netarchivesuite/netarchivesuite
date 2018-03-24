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

import org.apache.commons.lang.StringEscapeUtils;
import org.netarchivesuite.heritrix3wrapper.ScriptResult;

import com.antiaction.common.filter.Caching;
import com.antiaction.common.templateengine.TemplateBuilderFactory;

import dk.netarkivet.heritrix3.monitor.Heritrix3JobMonitor;
import dk.netarkivet.heritrix3.monitor.NASEnvironment;
import dk.netarkivet.heritrix3.monitor.NASUser;
import dk.netarkivet.heritrix3.monitor.ResourceAbstract;
import dk.netarkivet.heritrix3.monitor.ResourceManagerAbstract;
import dk.netarkivet.heritrix3.monitor.HttpLocaleHandler.HttpLocale;

public class H3ScriptResource implements ResourceAbstract {

    private NASEnvironment environment;

    protected int R_SCRIPT = -1;

    @Override
    public void resources_init(NASEnvironment environment) {
        this.environment = environment;
    }

    @Override
    public void resources_add(ResourceManagerAbstract resourceManager) {
        R_SCRIPT = resourceManager.resource_add(this, "/job/<numeric>/script/", false);
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
        if (resource_id == R_SCRIPT) {
            if ("GET".equals(method) || "POST".equals(method)) {
                script(req, resp, httpLocale, numerics);
            }
        }
    }

    public void script(HttpServletRequest req, HttpServletResponse resp, HttpLocale httpLocale, List<Integer> numerics) throws IOException {
        Locale locale = httpLocale.locale;
        resp.setContentType("text/html; charset=UTF-8");
        ServletOutputStream out = resp.getOutputStream();
        Caching.caching_disable_headers(resp);

        TemplateBuilderFactory<H3ScriptTemplateBuilder> scriptTplBuilderFactory = TemplateBuilderFactory.getInstance(environment.templateMaster, "h3script.tpl", "UTF-8", H3ScriptTemplateBuilder.class);
        H3ScriptTemplateBuilder scriptTplBuilder = scriptTplBuilderFactory.getTemplateBuilder();

        String engineStr = req.getParameter("engine");
        String scriptStr = req.getParameter("script");
        if (scriptStr == null) {
            scriptStr = "";
        }

        StringBuilder sb = new StringBuilder();

        long jobId = numerics.get(0);
        Heritrix3JobMonitor h3Job = environment.h3JobMonitorThread.getRunningH3Job(jobId);

        if (h3Job != null && h3Job.isReady()) {
            if (engineStr != null && engineStr.length() > 0 && scriptStr != null && scriptStr.length() > 0) {
                ScriptResult scriptResult = h3Job.h3wrapper.ExecuteShellScriptInJob(h3Job.jobResult.job.shortName, engineStr, scriptStr);
                //System.out.println(new String(scriptResult.response, "UTF-8"));
                if (scriptResult != null && scriptResult.script != null) {
                    if (scriptResult.script.failure) {
                    	if (scriptResult.script.stackTrace != null) {
                        	sb.append("<h5>Script failed with the following stacktrace:</h5>\n");
                            sb.append("<pre>\n");
                            sb.append(StringEscapeUtils.escapeHtml(scriptResult.script.stackTrace));
                            sb.append("</pre>\n");
                    	} else if (scriptResult.script.exception != null) {
                        	sb.append("<h5>Script failed with the following message:</h5>\n");
                            sb.append("<pre>\n");
                            sb.append(StringEscapeUtils.escapeHtml(scriptResult.script.exception));
                            sb.append("</pre>\n");
                    	} else {
                            sb.append("<b>Unknown script failure!</b></br>\n");
                    	}
                    	sb.append("<h5>Raw script result Xml:</h5>\n");
                        sb.append("<pre>");
                        sb.append(StringEscapeUtils.escapeHtml(new String(scriptResult.response, "UTF-8")));
                        sb.append("</pre>");
                    } else {
                    	if (scriptResult.script.htmlOutput != null) {
                            sb.append(scriptResult.script.htmlOutput);
                        }
                        if (scriptResult.script.rawOutput != null) {
                            sb.append("<pre>");
                            sb.append(scriptResult.script.rawOutput);
                            sb.append("</pre>");
                        }
                        sb.append("<pre>");
                        sb.append(new String(scriptResult.response, "UTF-8"));
                        sb.append("</pre>");
                    }
                } else {
                	sb.append("<b>Script did not return any response!</b><br/>\n");
                }
            }
        }

        StringBuilder menuSb = scriptTplBuilder.buildMenu(new StringBuilder(), req, locale, h3Job);

        scriptTplBuilder.insertContent("Scripting console", menuSb.toString(), httpLocale.generateLanguageLinks(), "Scripting console", scriptStr, sb.toString(), "").write(out);

        out.flush();
        out.close();
    }

}
