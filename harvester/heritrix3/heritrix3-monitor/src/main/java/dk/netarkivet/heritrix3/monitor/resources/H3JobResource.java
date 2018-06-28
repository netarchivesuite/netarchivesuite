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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.netarchivesuite.heritrix3wrapper.jaxb.Job;

import com.antiaction.common.filter.Caching;
import com.antiaction.common.templateengine.TemplateBuilderFactory;

import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.harvester.HarvesterSettings;
import dk.netarkivet.harvester.datamodel.HarvestDefinitionDAO;
import dk.netarkivet.harvester.datamodel.JobStatus;
import dk.netarkivet.heritrix3.monitor.Heritrix3JobMonitor;
import dk.netarkivet.heritrix3.monitor.Heritrix3JobMonitorThread;
import dk.netarkivet.heritrix3.monitor.NASEnvironment;
import dk.netarkivet.heritrix3.monitor.NASUser;
import dk.netarkivet.heritrix3.monitor.ResourceAbstract;
import dk.netarkivet.heritrix3.monitor.ResourceManagerAbstract;
import dk.netarkivet.heritrix3.monitor.HttpLocaleHandler.HttpLocale;

public class H3JobResource implements ResourceAbstract {

    private NASEnvironment environment;

    protected int R_JOB = -1;

    @Override
    public void resources_init(NASEnvironment environment) {
        this.environment = environment;
    }

    @Override
    public void resources_add(ResourceManagerAbstract resourceManager) {
        R_JOB = resourceManager.resource_add(this, "/job/<numeric>/", false);
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
        if (resource_id == R_JOB) {
            if ("GET".equals(method)) {
                job(req, resp, httpLocale, numerics);
            }
        }
    }

    public void job(HttpServletRequest req, HttpServletResponse resp, HttpLocale httpLocale, List<Integer> numerics) throws IOException {
        Locale locale = httpLocale.locale;
        NumberFormat numberFormat = NumberFormat.getInstance(locale);
        resp.setContentType("text/html; charset=UTF-8");
        ServletOutputStream out = resp.getOutputStream();
        Caching.caching_disable_headers(resp);

        TemplateBuilderFactory<MasterTemplateBuilder> masterTplBuilderFactory = TemplateBuilderFactory.getInstance(environment.templateMaster, "master.tpl", "UTF-8", MasterTemplateBuilder.class);
        MasterTemplateBuilder masterTplBuilder = masterTplBuilderFactory.getTemplateBuilder();

        StringBuilder sb = new StringBuilder();

        long jobId = numerics.get(0);
        Heritrix3JobMonitor h3Job = environment.h3JobMonitorThread.getRunningH3Job(jobId);
        Job job;
        
        HarvestDefinitionDAO dao = HarvestDefinitionDAO.getInstance();   

        if (h3Job != null && !h3Job.bInitialized) {
            h3Job.init();
        }

    	dk.netarkivet.harvester.datamodel.Job nasJob = Heritrix3JobMonitorThread.jobDAO.read(jobId);

    	String action = req.getParameter("action");
        if (action != null && action.length() > 0) {
            if ("failed".equalsIgnoreCase(action)) {
            	if (nasJob != null && (h3Job == null || !h3Job.isReady())) {
            		nasJob.setStatus(JobStatus.FAILED);
                    Heritrix3JobMonitorThread.jobDAO.update(nasJob);
            	}
            }
        }

        if (nasJob == null || nasJob.getStatus() == JobStatus.DONE || nasJob.getStatus() == JobStatus.FAILED || nasJob.getStatus() == JobStatus.FAILED_REJECTED) {
            sb.append("NAS Job ");
            sb.append(jobId);
            sb.append(" is in state ");
            sb.append(nasJob.getStatus().toString());
            sb.append(".");
        } else {
            if (h3Job == null || !h3Job.isReady()) {
                sb.append("Job ");
                sb.append(jobId);
                sb.append(" is not currently monitored. Maybe Heritrix 3 is not running at this point in time.");
                if (nasJob != null && nasJob.getStatus() == JobStatus.STARTED && (h3Job == null || !h3Job.isReady())) {
                    sb.append("<br />\n");
                    sb.append("<a href=\"?action=");
                    sb.append("failed");
                    sb.append("\"");
                	sb.append(" onclick=\"return confirm('Are you sure you wish to fail the job currently being crawled ?')\"");
                    sb.append(" class=\"btn btn-danger\">");
                    sb.append("<i class=\\\"icon-white icon-trash\\\"></i>");
                    sb.append("Set job status to failed!");
                    sb.append("</a>");
            	}
            } else {
                if (action != null && action.length() > 0) {
                    if ("build".equalsIgnoreCase(action)) {
                        h3Job.h3wrapper.buildJobConfiguration(h3Job.jobname);
                    }
                    if ("launch".equalsIgnoreCase(action)) {
                        h3Job.h3wrapper.launchJob(h3Job.jobname);
                    }
                    if ("pause".equalsIgnoreCase(action)) {
                        h3Job.h3wrapper.pauseJob(h3Job.jobname);
                    }
                    if ("unpause".equalsIgnoreCase(action)) {
                        h3Job.h3wrapper.unpauseJob(h3Job.jobname);
                    }
                    if ("checkpoint".equalsIgnoreCase(action)) {
                        h3Job.h3wrapper.checkpointJob(h3Job.jobname);
                    }
                    if ("terminate".equalsIgnoreCase(action)) {
                        h3Job.h3wrapper.terminateJob(h3Job.jobname);
                    }
                    if ("teardown".equalsIgnoreCase(action)) {
                        h3Job.h3wrapper.teardownJob(h3Job.jobname);
                    }
                }

                //job.setStatus(JobStatus.FAILED);

                h3Job.update();

                sb.append("<div>\n");

                sb.append("<div style=\"float:left;min-width: 300px;\">\n");

                sb.append("JobId: <a href=\"/History/Harveststatus-jobdetails.jsp?jobID="+h3Job.jobId+"\">");
                sb.append(h3Job.jobId);
                sb.append("</a><br />\n");
                if (h3Job.jobResult != null && h3Job.jobResult.job != null) {
                	sb.append("JobState: ");
                	sb.append(h3Job.jobResult.job.crawlControllerState);
                	sb.append("<br />\n");
                }
                String harvestName = dao.getHarvestName(h3Job.job.getOrigHarvestDefinitionID());
                sb.append("OrigHarvestDefinitionName: <a href=\"/HarvestDefinition/Definitions-edit-selective-harvest.jsp?harvestname="+harvestName.replace(' ', '+')+"\">");
                sb.append(harvestName);
                sb.append("</a><br />\n");
                sb.append("HarvestNum: ");
                sb.append(h3Job.job.getHarvestNum());
                sb.append("<br />\n");
                sb.append("Snapshot: ");
                sb.append(h3Job.job.isSnapshot());
                sb.append("<br />\n");
                sb.append("Channel: ");
                sb.append(h3Job.job.getChannel());
                sb.append("<br />\n");
                sb.append("TemplateName: <a href=\"");
                sb.append("/History/Harveststatus-download-job-harvest-template.jsp?JobID=");
                sb.append(h3Job.jobId);
                sb.append("\">");
                sb.append(h3Job.job.getOrderXMLName());
                sb.append("</a><br />\n");
                sb.append("CountDomains: ");
                sb.append(h3Job.job.getCountDomains());
                sb.append("<br />\n");
                sb.append("MaxBytesPerDomain: ");
                sb.append(numberFormat.format(h3Job.job.getMaxBytesPerDomain()));
                sb.append("<br />\n");
                sb.append("MaxObjectsPerDomain: ");
                sb.append(numberFormat.format(h3Job.job.getMaxObjectsPerDomain()));
                sb.append("<br />\n");
                sb.append("MaxJobRunningTime: ");
                sb.append(h3Job.job.getMaxJobRunningTime());
                sb.append(" ms.<br />\n");
                
                sb.append("</div>\n");
                
                /* Heritrix3 WebUI */
                sb.append("<div style=\"float:left;position: absolute;left:600px;\">\n");
                sb.append("<a href=\"");
                sb.append(h3Job.hostUrl);
                sb.append("\" class=\"btn btn-default\">");
                sb.append("Heritrix3 WebUI");
                sb.append("</a>");
                
                sb.append("</div>\n");
                
                sb.append("<div style=\"clear:both;\"></div>");
                sb.append("</div>");
                
                /* line 1 */
                
                sb.append("<h4>Job details</h4>\n");
                sb.append("<div>\n");
                
                /* Progression/Queues */
                sb.append("<a href=\"");
                sb.append("/History/Harveststatus-running-jobdetails.jsp?jobID=");
                sb.append(h3Job.jobId);
                sb.append("\" class=\"btn btn-default\">");
                sb.append("Progression/Queues");
                sb.append("</a>");

                sb.append("&nbsp;");
                
                /* Show Crawllog on H3 GUI*/
                URL url1 = new URL(h3Job.hostUrl);
                sb.append("<a href=\"");
                sb.append("https://"+url1.getHost()+":"+url1.getPort()+"/engine/anypath/");
                sb.append(h3Job.crawlLogFilePath);
                sb.append("?format=paged&pos=-1&lines=-1000&reverse=y");
                sb.append("\" class=\"btn btn-default\">");
                sb.append("Remote H3 Crawllog viewer");
                sb.append("</a>");
                
                sb.append("&nbsp;");

                /* Crawllog */
                sb.append("<a href=\"");
                sb.append(NASEnvironment.servicePath);
                sb.append("job/");
                sb.append(h3Job.jobId);
                sb.append("/crawllog/");
                sb.append("\" class=\"btn btn-default\">");
                sb.append("View/Search in cached Crawllog");
                sb.append("</a>");

                sb.append("&nbsp;");
                
                /* Reports */
                sb.append("<a href=\"");
                sb.append(NASEnvironment.servicePath);
                sb.append("job/");
                sb.append(h3Job.jobId);
                sb.append("/report/");
                sb.append("\" class=\"btn btn-default\">");
                sb.append("Reports");
                sb.append("</a>");

                sb.append("&nbsp;");

                sb.append("</div>\n");
                
                /* line 2 */
                
                sb.append("<h4>Queue actions</h4>\n");
                sb.append("<div>\n");
                
                /* Show/delete Frontier */
                sb.append("<a href=\"");
                sb.append(NASEnvironment.servicePath);
                sb.append("job/");
                sb.append(h3Job.jobId);
                sb.append("/frontier/");
                sb.append("\" class=\"btn btn-default\">");
                sb.append("Show/delete Frontier");
                sb.append("</a>");

                sb.append("&nbsp;");
                
                /* Delete from Frontier */
                sb.append("<a href=\"");
                sb.append(NASEnvironment.servicePath);
                sb.append("job/");
                sb.append(h3Job.jobId);
                sb.append("/frontier-delete/");
                sb.append("\" class=\"btn btn-default\">");
                sb.append("Delete from Frontier");
                sb.append("</a>");

                sb.append("&nbsp;");
                
                /* Add RejectRules */
                sb.append("<a href=\"");
                sb.append(NASEnvironment.servicePath);
                sb.append("job/");
                sb.append(h3Job.jobId);
                sb.append("/filter/");
                sb.append("\" class=\"btn btn-default\">");
                sb.append("Add RejectRules");
                sb.append("</a>");

                sb.append("&nbsp;");
                
                /* Modify Budget */
                sb.append("<a href=\"");
                sb.append(NASEnvironment.servicePath);
                sb.append("job/");
                sb.append(h3Job.jobId);
                sb.append("/budget/");
                sb.append("\" class=\"btn btn-default\">");
                sb.append("Modify budget");
                sb.append("</a>");

                sb.append("&nbsp;");
                
                sb.append("</div>\n");

                if (h3Job.jobResult != null && h3Job.jobResult.job != null) {
                	
                	/* line 3 */
                    
                    sb.append("<h4>Job actions</h4>\n");
                    sb.append("<div style=\"margin-bottom:30px;\">\n");
                	
                    job = h3Job.jobResult.job;

                    for (int i=0; i<job.availableActions.size(); ++i) {
                        if (i > 0) {
                            sb.append("&nbsp;");
                        }
                        //  disabled="disabled"
                        sb.append("<a href=\"?action=");
                        String thisAction = job.availableActions.get(i);
                        sb.append(thisAction);
                        sb.append("\"");
                        if("terminate".equals(thisAction) || "teardown".equals(thisAction)) {
                        	sb.append(" onclick=\"return confirm('Are you sure you wish to ");
                        	sb.append(thisAction);
                        	sb.append(" the job currently being crawled ?')\"");
                            sb.append(" class=\"btn btn-danger\">");
                            sb.append("<i class=\\\"icon-white icon-trash\\\"></i>");
                        } else {
                            sb.append(" class=\"btn btn-default\">");
                        }
                        sb.append(job.availableActions.get(i).substring(0, 1).toUpperCase()+job.availableActions.get(i).substring(1));
                        sb.append("</a>");
                    }
                    
                    sb.append("&nbsp;");
                    
                    /* Open Scripting Console */
                    sb.append("<a href=\"");
                    sb.append(NASEnvironment.servicePath);
                    sb.append("job/");
                    sb.append(h3Job.jobId);
                    sb.append("/script/");
                    sb.append("\" class=\"btn btn-default\">");
                    sb.append("Open Scripting Console");
                    sb.append("</a>");

                    sb.append("&nbsp;");
                    
                    /* View scripting_events.log */
                    File logDir = new File(h3Job.crawlLogFilePath);
                    
                    sb.append("<a href=\"");
                    URL url = new URL(h3Job.hostUrl);
                    sb.append("https://"+url.getHost()+":"+url.getPort()+"/engine/anypath"+logDir.getParentFile().getAbsolutePath()+"/scripting_events.log");
                    sb.append("\" class=\"btn btn-default\">");
                    sb.append("View scripting_events.log");
                    sb.append("</a>");
                    
                    sb.append("</div>\n");

                    sb.append("shortName: ");
                    sb.append(job.shortName);
                    sb.append("<br />\n");
                    sb.append("crawlControllerState: ");
                    sb.append(job.crawlControllerState);
                    sb.append("<br />\n");
                    sb.append("crawlExitStatus: ");
                    sb.append(job.crawlExitStatus);
                    sb.append("<br />\n");
                    sb.append("statusDescription: ");
                    sb.append(job.statusDescription);
                    sb.append("<br />\n");
                    sb.append("url: ");
                    sb.append("<a href=\"");
                    sb.append(h3Job.hostUrl+"/job/"+h3Job.jobname);
                    sb.append("/");
                    sb.append("\">");
                    sb.append(h3Job.hostUrl+"/job/"+h3Job.jobname);
                    sb.append("</a>");
                    sb.append("<br />\n");
                    if (job.jobLogTail != null) {
                        for (int i =0; i<job.jobLogTail.size(); ++i) {
                            sb.append("jobLogTail[");
                            sb.append(i);
                            sb.append("]: ");
                            sb.append(job.jobLogTail.get(i));
                            sb.append("<br />\n");
                        }
                    }
                    if (job.uriTotalsReport != null) {
                        sb.append("uriTotalsReport.downloadedUriCount: ");
                        sb.append(job.uriTotalsReport.downloadedUriCount);
                        sb.append("<br />\n");
                        sb.append("uriTotalsReport.queuedUriCount: ");
                        sb.append(job.uriTotalsReport.queuedUriCount);
                        sb.append("<br />\n");
                        sb.append("uriTotalsReport.totalUriCount: ");
                        sb.append(job.uriTotalsReport.totalUriCount);
                        sb.append("<br />\n");
                        sb.append("uriTotalsReport.futureUriCount: ");
                        sb.append(job.uriTotalsReport.futureUriCount);
                        sb.append("<br />\n");
                    }
                    if (job.sizeTotalsReport != null) {
                        sb.append("sizeTotalsReport.dupByHash: ");
                        sb.append(job.sizeTotalsReport.dupByHash);
                        sb.append("<br />\n");
                        sb.append("sizeTotalsReport.dupByHashCount: ");
                        sb.append(job.sizeTotalsReport.dupByHashCount);
                        sb.append("<br />\n");
                        sb.append("sizeTotalsReport.novel: ");
                        sb.append(job.sizeTotalsReport.novel);
                        sb.append("<br />\n");
                        sb.append("sizeTotalsReport.novelCount: ");
                        sb.append(job.sizeTotalsReport.novelCount);
                        sb.append("<br />\n");
                        sb.append("sizeTotalsReport.notModified: ");
                        sb.append(job.sizeTotalsReport.notModified);
                        sb.append("<br />\n");
                        sb.append("sizeTotalsReport.notModifiedCount: ");
                        sb.append(job.sizeTotalsReport.notModifiedCount);
                        sb.append("<br />\n");
                        sb.append("sizeTotalsReport.total: ");
                        sb.append(job.sizeTotalsReport.total);
                        sb.append("<br />\n");
                        sb.append("sizeTotalsReport.totalCount: ");
                        sb.append(job.sizeTotalsReport.totalCount);
                        sb.append("<br />\n");
                    }
                    if (job.rateReport != null) {
                        sb.append("rateReport.currentDocsPerSecond: ");
                        sb.append(job.rateReport.currentDocsPerSecond);
                        sb.append("<br />\n");
                        sb.append("rateReport.averageDocsPerSecond: ");
                        sb.append(job.rateReport.averageDocsPerSecond);
                        sb.append("<br />\n");
                        sb.append("rateReport.currentKiBPerSec: ");
                        sb.append(job.rateReport.currentKiBPerSec);
                        sb.append("<br />\n");
                        sb.append("rateReport.averageKiBPerSec: ");
                        sb.append(job.rateReport.averageKiBPerSec);
                        sb.append("<br />\n");
                    }
                    if (job.loadReport != null) {
                        sb.append("loadReport.busyThreads: ");
                        sb.append(job.loadReport.busyThreads);
                        sb.append("<br />\n");
                        sb.append("loadReport.totalThreads: ");
                        sb.append(job.loadReport.totalThreads);
                        sb.append("<br />\n");
                        sb.append("loadReport.congestionRatio: ");
                        sb.append(job.loadReport.congestionRatio);
                        sb.append("<br />\n");
                        sb.append("loadReport.averageQueueDepth: ");
                        sb.append(job.loadReport.averageQueueDepth);
                        sb.append("<br />\n");
                        sb.append("loadReport.deepestQueueDepth: ");
                        sb.append(job.loadReport.deepestQueueDepth);
                        sb.append("<br />\n");
                    }
                    if (job.elapsedReport != null) {
                        sb.append("elapsedReport.elapsed: ");
                        sb.append(job.elapsedReport.elapsedPretty);
                        sb.append(" (");
                        sb.append(job.elapsedReport.elapsedMilliseconds);
                        sb.append("ms)");
                        sb.append("<br />\n");
                    }
                    if (job.threadReport != null) {
                        sb.append("threadReport.toeCount: ");
                        sb.append(job.threadReport.toeCount);
                        sb.append("<br />\n");
                        if (job.threadReport.steps != null) {
                            for (int i =0; i<job.threadReport.steps.size(); ++i) {
                                sb.append("threadReport.steps[");
                                sb.append(i);
                                sb.append("]: ");
                                sb.append(job.threadReport.steps.get(i));
                                sb.append("<br />\n");
                            }
                        }
                        if (job.threadReport.processors != null) {
                            for (int i =0; i<job.threadReport.processors.size(); ++i) {
                                sb.append("threadReport.processors[");
                                sb.append(i);
                                sb.append("]: ");
                                sb.append(job.threadReport.processors.get(i));
                                sb.append("<br />\n");
                            }
                        }
                    }
                    if (job.frontierReport != null) {
                        sb.append("frontierReport.totalQueues: ");
                        sb.append(job.frontierReport.totalQueues);
                        sb.append("<br />\n");
                        sb.append("frontierReport.inProcessQueues: ");
                        sb.append(job.frontierReport.inProcessQueues);
                        sb.append("<br />\n");
                        sb.append("frontierReport.readyQueues: ");
                        sb.append(job.frontierReport.readyQueues);
                        sb.append("<br />\n");
                        sb.append("frontierReport.snoozedQueues: ");
                        sb.append(job.frontierReport.snoozedQueues);
                        sb.append("<br />\n");
                        sb.append("frontierReport.activeQueues: ");
                        sb.append(job.frontierReport.activeQueues);
                        sb.append("<br />\n");
                        sb.append("frontierReport.inactiveQueues: ");
                        sb.append(job.frontierReport.inactiveQueues);
                        sb.append("<br />\n");
                        sb.append("frontierReport.ineligibleQueues: ");
                        sb.append(job.frontierReport.ineligibleQueues);
                        sb.append("<br />\n");
                        sb.append("frontierReport.retiredQueues: ");
                        sb.append(job.frontierReport.retiredQueues);
                        sb.append("<br />\n");
                        sb.append("frontierReport.exhaustedQueues: ");
                        sb.append(job.frontierReport.exhaustedQueues);
                        sb.append("<br />\n");
                        sb.append("frontierReport.lastReachedState: ");
                        sb.append(job.frontierReport.lastReachedState);
                        sb.append("<br />\n");
                    }
                    if (job.crawlLogTail != null) {
                        for (int i =0; i<job.crawlLogTail.size(); ++i) {
                            sb.append("crawlLogTail[");
                            sb.append(i);
                            sb.append("]: ");
                            sb.append(job.crawlLogTail.get(i));
                            sb.append("<br />\n");
                        }
                    }
                    sb.append("isRunning: ");
                    sb.append(job.isRunning);
                    sb.append("<br />\n");
                    sb.append("isLaunchable: ");
                    sb.append(job.isLaunchable);
                    sb.append("<br />\n");
                    sb.append("alertCount: ");
                    sb.append(job.alertCount);
                    sb.append("<br />\n");
                    sb.append("alertLogFilePath: ");
                    sb.append(job.alertLogFilePath);
                    sb.append("<br />\n");
                    sb.append("crawlLogFilePath: ");
                    sb.append(job.crawlLogFilePath);
                    sb.append("<br />\n");
                    if (job.heapReport != null) {
                        sb.append("heapReport.usedBytes: ");
                        sb.append(job.heapReport.usedBytes);
                        sb.append("<br />\n");
                        sb.append("heapReport.totalBytes: ");
                        sb.append(job.heapReport.totalBytes);
                        sb.append("<br />\n");
                        sb.append("heapReport.maxBytes: ");
                        sb.append(job.heapReport.maxBytes);
                        sb.append("<br />\n");
                    }
                }
            }
        }

        StringBuilder menuSb = masterTplBuilder.buildMenu(new StringBuilder(), req, locale, h3Job);

        masterTplBuilder.insertContent("Details and Actions on Running Job " + jobId, menuSb.toString(), httpLocale.generateLanguageLinks(),
        		"Details and Actions on Running Job " + jobId, sb.toString(),
        		"<meta http-equiv=\"refresh\" content=\""+Settings.get(HarvesterSettings.HARVEST_MONITOR_REFRESH_INTERVAL)+"\"/>\n").write(out);

        out.flush();
        out.close();
    }

}
