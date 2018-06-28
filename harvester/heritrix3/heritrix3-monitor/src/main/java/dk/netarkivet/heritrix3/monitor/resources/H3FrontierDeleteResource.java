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
import dk.netarkivet.heritrix3.monitor.Pagination;
import dk.netarkivet.heritrix3.monitor.ResourceAbstract;
import dk.netarkivet.heritrix3.monitor.ResourceManagerAbstract;
import dk.netarkivet.heritrix3.monitor.HttpLocaleHandler.HttpLocale;

public class H3FrontierDeleteResource implements ResourceAbstract {

    private NASEnvironment environment;

    protected int R_FRONTIER_DELETE = -1;
    
    @Override
    public void resources_init(NASEnvironment environment) {
        this.environment = environment;
    }

    @Override
    public void resources_add(ResourceManagerAbstract resourceManager) {
        R_FRONTIER_DELETE = resourceManager.resource_add(this, "/job/<numeric>/frontier-delete/", false);
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
        if (resource_id == R_FRONTIER_DELETE) {
            if ("GET".equals(method) || "POST".equals(method)) {
                frontier_delete(req, resp, httpLocale, numerics, "POST".equals(method));
            }
        }
    }

    public void frontier_delete(HttpServletRequest req, HttpServletResponse resp, HttpLocale httpLocale, List<Integer> numerics, boolean bPost) throws IOException {
        Locale locale = httpLocale.locale;
        resp.setContentType("text/html; charset=UTF-8");
        ServletOutputStream out = resp.getOutputStream();
        Caching.caching_disable_headers(resp);

        TemplateBuilderFactory<MasterTemplateBuilder> masterTplBuilderFactory = TemplateBuilderFactory.getInstance(environment.templateMaster, "master.tpl", "UTF-8", MasterTemplateBuilder.class);
        MasterTemplateBuilder masterTplBuilder = masterTplBuilderFactory.getTemplateBuilder();

        StringBuilder sb = new StringBuilder();
        String tmpStr;
        String q = null;

        tmpStr = req.getParameter("q");
        if (tmpStr != null && tmpStr.length() > 0 && !tmpStr.equalsIgnoreCase(".*")) {
            q = tmpStr;
        } else {
        	q = "";
        }

        String initials = req.getParameter("initials");
        if (initials == null) {
            initials = "";
        }

        String showStr = req.getParameter("show");
        String deleteStr = req.getParameter("delete");

        long jobId = numerics.get(0);
        Heritrix3JobMonitor h3Job = environment.h3JobMonitorThread.getRunningH3Job(jobId);

        if (h3Job != null && h3Job.isReady()) {
            sb.append("<form class=\"form-horizontal\" action=\"?\" name=\"insert_form\" method=\"post\" enctype=\"application/x-www-form-urlencoded\" accept-charset=\"utf-8\">\n");
            sb.append("<label for=\"q\">URL regex:</label>");
            sb.append("<input type=\"text\" id=\"q\" name=\"q\" value=\"" + q + "\" placeholder=\"regex\" style=\"display:inline;width:350px;\">\n");
            sb.append("<button type=\"submit\" name=\"show\" value=\"1\" class=\"btn btn-success\"><i class=\"icon-white icon-thumbs-up\"></i> Show number of matches</button>\n");
            sb.append("&nbsp;");
            if (bPost && deleteStr != null && "1".equals(deleteStr) && (initials == null || initials.length() == 0)) {
                //sb.append("<span style=\"text-color: red;\">Initials required to delete from the frontier queue!</span><br />\n");
                sb.append("<div class=\"notify notify-red\"><span class=\"symbol icon-error\"></span> Initials required to delete from the frontier queue!</div>");
            }
            sb.append("<label for=\"initials\">User initials:</label>");
            sb.append("<input type=\"text\" id=\"initials\" name=\"initials\" value=\"" + initials  + "\" placeholder=\"initials\">\n");
            sb.append("<button type=\"submit\" name=\"delete\" value=\"1\" class=\"btn btn-danger\"><i class=\"icon-white icon-trash\"></i> Delete matches</button>\n");
            sb.append("</form>\n");

            if (bPost) {
                String script = environment.NAS_GROOVY_SCRIPT;
                if (showStr != null && "1".equals(showStr)) {
                	if (q == null || q.length() == 0) {
                        script += "\n";
                        script += "getNumberOfUrlsInFrontier()";
                        script += "\n";
                	} else {
                        script += "\n";
                        script += "getNumberOfMatchedUrlsInFrontier '" + q + "'";
                        script += "\n";
                	}
                }
                if (deleteStr != null && "1".equals(deleteStr) && initials != null && initials.length() > 0) {
                    script += "\n";
                    script += "initials = \"" + initials + "\"";
                    script += "\n";
                    script += "deleteFromFrontier '" + q + "'";
                    script += "\n";
                }
                ScriptResult scriptResult = h3Job.h3wrapper.ExecuteShellScriptInJob(h3Job.jobResult.job.shortName, "groovy", script);
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
                        sb.append("<div style=\"float:left;margin: 20px 0px;\">\n");
                        /*
                        sb.append("<span>Matching lines: ");
                        sb.append(lines);
                        sb.append(" URIs</span>\n");
                        */
                        sb.append("</div>\n");
                        sb.append("<div style=\"clear:both;\"></div>");
                        sb.append("<div>\n");
                        sb.append("<pre>\n");
                        if (scriptResult != null && scriptResult.script != null) {
                            if (scriptResult.script.htmlOutput != null) {
                                sb.append("<fieldset><!--<legend>htmlOut</legend>-->");
                                sb.append(scriptResult.script.htmlOutput);
                                sb.append("</fieldset><br />\n");
                            }
                            if (scriptResult.script.rawOutput != null) {
                                sb.append("<fieldset><!--<legend>rawOut</legend>-->");
                                sb.append("<pre>");
                                sb.append(scriptResult.script.rawOutput);
                                sb.append("</pre>");
                                sb.append("</fieldset><br />\n");
                            }
                        }
                        sb.append("</pre>\n");
                        sb.append("</div>\n");
                        sb.append("</form>");
                    }
                } else {
                	sb.append("<b>Script did not return any response!</b><br/>\n");
                }
            }
        } else {
            sb.append("Job ");
            sb.append(jobId);
            sb.append(" is not running.");
        }

        StringBuilder menuSb = masterTplBuilder.buildMenu(new StringBuilder(), req, locale, h3Job);

        masterTplBuilder.insertContent("Job " + jobId + " Frontier", menuSb.toString(), httpLocale.generateLanguageLinks(),
        		"Job " + jobId + " Frontier", sb.toString(), "").write(out);

        out.flush();
        out.close();
    }

}
