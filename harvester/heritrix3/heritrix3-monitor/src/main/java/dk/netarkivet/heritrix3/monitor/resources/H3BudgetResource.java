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

import org.netarchivesuite.heritrix3wrapper.ScriptResult;

import com.antiaction.common.filter.Caching;
import com.antiaction.common.templateengine.TemplateBuilderFactory;

import dk.netarkivet.heritrix3.monitor.Heritrix3JobMonitor;
import dk.netarkivet.heritrix3.monitor.NASEnvironment;
import dk.netarkivet.heritrix3.monitor.NASUser;
import dk.netarkivet.heritrix3.monitor.ResourceAbstract;
import dk.netarkivet.heritrix3.monitor.ResourceManagerAbstract;
import dk.netarkivet.heritrix3.monitor.HttpLocaleHandler.HttpLocale;

public class H3BudgetResource implements ResourceAbstract {

    private NASEnvironment environment;

    protected int R_BUDGET = -1;

    @Override
    public void resources_init(NASEnvironment environment) {
        this.environment = environment;
    }

    @Override
    public void resources_add(ResourceManagerAbstract resourceManager) {
        R_BUDGET = resourceManager.resource_add(this, "/job/<numeric>/budget/", false);
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
        if(resource_id == R_BUDGET) {
        	if ("GET".equals(method) || "POST".equals(method)) {
                budget_change(req, resp, httpLocale, numerics);
            }
        }
    }

    public void budget_change(HttpServletRequest req, HttpServletResponse resp, HttpLocale httpLocale, List<Integer> numerics) throws IOException {
        Locale locale = httpLocale.locale;
    	resp.setContentType("text/html; charset=UTF-8");
        ServletOutputStream out = resp.getOutputStream();
        Caching.caching_disable_headers(resp);

        TemplateBuilderFactory<MasterTemplateBuilder> tplBuilder = TemplateBuilderFactory.getInstance(environment.templateMaster, "master.tpl", "UTF-8", MasterTemplateBuilder.class);
        MasterTemplateBuilder masterTplBuilder = tplBuilder.getTemplateBuilder();

        StringBuilder sb = new StringBuilder();

        String budget = req.getParameter("budget");
        if (budget == null) {
        	budget = "";
        }
        String key = req.getParameter("key");
        if (key == null) {
        	key = "";
        }
        
        String submit1 = req.getParameter("submitButton1");
        String submit2 = req.getParameter("submitButton2");
        
        String initials = "";
        if(submit1 != null) {
        	initials = req.getParameter("initials1");
        } else if(submit2 != null) {
        	initials = req.getParameter("initials2");
        }
        if (initials == null) {
    		initials = "";
    	}
        if(submit1 == null) {
        	submit1 = "";
        }
        if(submit2 == null) {
        	submit2 = "";
        }
        
        boolean isNumber = true;

        String script = environment.NAS_GROOVY_SCRIPT;
        String originalScript = script;

        script += "\n";
        if((!submit1.isEmpty() || !submit2.isEmpty()) && !initials.isEmpty()) {
        	/* case new budget change */
	        if (!submit1.isEmpty() && !budget.trim().isEmpty() && !key.trim().isEmpty()) {
	        	script += "\ninitials = \"" + initials + "\"";
	            script += "\nchangeBudget ('" + key+ "',"+ budget +")\n";
	        } else {
	        	if(!submit2.isEmpty()) {
	        		String[] queues = req.getParameterValues("queueName");
	        		if(queues != null && queues.length > 0) {
	        			script += "\ninitials = \"" + initials + "\"";
		        		for(int i = 0; i < queues.length; i++) {
		        			budget = req.getParameter(queues[i]+"-budget");
		        			if(budget != null && !budget.isEmpty()) {
			        			try {
			        				Integer.parseInt(budget);
			        				script += "\nchangeBudget ('" + queues[i]+ "',"+ budget +")\n";
			        			} catch(NumberFormatException e) {
			        				isNumber = false;
			        			}
		        			}
		        		}
	        		}
	        	}
	        }
        }
        script += "\n";
        script += "\nshowModBudgets()\n";
        
        originalScript += "\ngetQueueTotalBudget()\n";

        long jobId = numerics.get(0);
        Heritrix3JobMonitor h3Job = environment.h3JobMonitorThread.getRunningH3Job(jobId);

        if (h3Job != null && h3Job.isReady()) {
            /* form control */
            boolean submitWithInitials = true;
            if ((!submit1.isEmpty() || !submit2.isEmpty()) && initials.isEmpty()) {
                sb.append("<div class=\"notify notify-red\"><span class=\"symbol icon-error\"></span> Initials required to modify a queue budget!</div>");

            }

            if(!submit1.isEmpty() && initials.isEmpty()) {
                submitWithInitials = false;
            }

            try {
            	if (budget != null && budget.length() > 0) {
            		Integer.parseInt(budget);
            	}
            } catch(NumberFormatException e) {
            	sb.append("<div class=\"notify notify-red\"><span class=\"symbol icon-error\"></span> Budget must be a number!</div>");
            }
            
            if(isNumber == false) {
            	sb.append("<div class=\"notify notify-red\"><span class=\"symbol icon-error\"></span> Budget must be a number!</div>");
            }

            ScriptResult scriptResult = h3Job.h3wrapper.ExecuteShellScriptInJob(h3Job.jobResult.job.shortName, "groovy", originalScript);
            if (scriptResult != null && scriptResult.script != null && scriptResult.script.htmlOutput != null) {
            	sb.append("<p>Budget defined in job configuration: queue-total-budget of ");
            	sb.append(scriptResult.script.htmlOutput);
            	sb.append(" URIs.</p>");
            }

            sb.append("<form class=\"form-horizontal\" action=\"?\" name=\"insert_form\" method=\"post\" enctype=\"application/x-www-form-urlencoded\" accept-charset=\"utf-8\">\n");

            scriptResult = h3Job.h3wrapper.ExecuteShellScriptInJob(h3Job.jobResult.job.shortName, "groovy", script);

            /* Budget to modify */
            sb.append("<label style=\"cursor: default;\">Budget to modify:</label>");
            sb.append("<input type=\"text\" id=\"key\" name=\"key\" value=\"");
            if(!submitWithInitials) {
            	sb.append(key);
            }
            sb.append("\" style=\"width: 306px;\" placeholder=\"domain/host name\">\n");
            sb.append("<input type=\"text\" id=\"budget\" name=\"budget\" value=\"");
            if(!submitWithInitials) {
            	sb.append(budget);
            }
            sb.append("\" style=\"width:100px\" placeholder=\"new budget\">\n");
            
            /* User initials */
            sb.append("<label style=\"cursor: default;\">User initials:</label>");
            sb.append("<input type=\"text\" id=\"initials1\" name=\"initials1\" value=\"" + initials  + "\" placeholder=\"initials\">\n");
  
            sb.append("<button type=\"submit\" name=\"submitButton1\" value=\"submitButton1\" class=\"btn btn-success\"><i class=\"icon-white icon-thumbs-up\"></i> Save</button>\n");
            sb.append("<br/>\n");
            
            if (scriptResult != null && scriptResult.script != null && scriptResult.script.htmlOutput != null) {
            	sb.append("<div style=\"font-size: 14px; font-weight: normal; line-height: 20px;\">\n");
            	sb.append(scriptResult.script.htmlOutput);
            	sb.append("<div>\n");
            	/* User initials */
                sb.append("<label style=\"cursor: default;\">User initials:</label>");
                sb.append("<input type=\"text\" id=\"initials2\" name=\"initials2\" value=\"" + initials  + "\" placeholder=\"initials\">\n");
                
                /* save button*/
                sb.append("<button type=\"submit\" name=\"submitButton2\" value=\"submitButton2\" class=\"btn btn-success\"><i class=\"icon-white icon-thumbs-up\"></i> Save</button>\n");
            }

            sb.append("</form>\n");
        } else {
            sb.append("Job ");
            sb.append(jobId);
            sb.append(" is not running.");
        }

        StringBuilder menuSb = masterTplBuilder.buildMenu(new StringBuilder(), req, locale, h3Job);

        masterTplBuilder.insertContent("Job " + jobId + " Budget", menuSb.toString(), httpLocale.generateLanguageLinks(), "Job " + jobId + " Budget", sb.toString(), "").write(out);

        out.flush();
        out.close();
    }

}
