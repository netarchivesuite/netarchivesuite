package dk.netarkivet.harvester.webinterface.servlet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.netarchivesuite.heritrix3wrapper.ScriptResult;
import org.netarchivesuite.heritrix3wrapper.StreamResult;
import org.netarchivesuite.heritrix3wrapper.jaxb.Job;
import org.netarchivesuite.heritrix3wrapper.jaxb.Report;

import com.antiaction.common.filter.Caching;
import com.antiaction.common.templateengine.TemplateBuilderFactory;
import com.antiaction.common.templateengine.TemplateBuilderPlaceHolder;
import com.antiaction.common.templateengine.TemplatePlaceHolder;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.Constants;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.harvester.HarvesterSettings;
import dk.netarkivet.harvester.datamodel.HarvestDefinitionDAO;

public class JobResource implements ResourceAbstract {

    private static final String NAS_GROOVY_RESOURCE_PATH = "dk/netarkivet/harvester/webinterface/servlet/nas.groovy";

    private NASEnvironment environment;

    protected int R_JOB = -1;

    protected int R_CRAWLLOG = -1;

    protected int R_FRONTIER = -1;
    
    protected int R_FILTER = -1;
    
    protected int R_BUDGET = -1;

    protected int R_SCRIPT = -1;

    protected int R_REPORT = -1;

    @Override
    public void resources_init(NASEnvironment environment) {
        this.environment = environment;
    }

    @Override
    public void resources_add(ResourceManagerAbstract resourceManager) {
        R_JOB = resourceManager.resource_add(this, "/job/<numeric>/", false);
        R_CRAWLLOG = resourceManager.resource_add(this, "/job/<numeric>/crawllog/", false);
        R_FRONTIER = resourceManager.resource_add(this, "/job/<numeric>/frontier/", false);
        R_FILTER = resourceManager.resource_add(this, "/job/<numeric>/filter/", false);
        R_BUDGET = resourceManager.resource_add(this, "/job/<numeric>/budget/", false);
        R_SCRIPT = resourceManager.resource_add(this, "/job/<numeric>/script/", false);
        R_REPORT = resourceManager.resource_add(this, "/job/<numeric>/report/", false);
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
        if (resource_id == R_JOB) {
            if ("GET".equals(method)) {
                job(req, resp, numerics);
            }
        } else if (resource_id == R_CRAWLLOG) {
            if ("GET".equals(method) || "POST".equals(method)) {
                crawllog_list(req, resp, numerics);
            }
        } else if (resource_id == R_FRONTIER) {
            if ("GET".equals(method) || "POST".equals(method)) {
                frontier_list(req, resp, numerics);
            }
        } else if(resource_id == R_FILTER) {
        	if ("GET".equals(method) || "POST".equals(method)) {
                filter_add(req, resp, numerics);
            }
        } else if(resource_id == R_BUDGET) {
        	if ("GET".equals(method) || "POST".equals(method)) {
                budget_change(req, resp, numerics);
            }
        }else if (resource_id == R_SCRIPT) {
            if ("GET".equals(method) || "POST".equals(method)) {
                script(req, resp, numerics);
            }
        } else if (resource_id == R_REPORT) {
            if ("GET".equals(method)) {
                report(req, resp, numerics);
            }
        }
    }

    public void job(HttpServletRequest req, HttpServletResponse resp, List<Integer> numerics) throws IOException {
        Locale locale = resp.getLocale();
        resp.setContentType("text/html; charset=UTF-8");
        ServletOutputStream out = resp.getOutputStream();

        Caching.caching_disable_headers(resp);

        TemplateBuilderFactory<MasterTemplateBuilder> tplBuilder = TemplateBuilderFactory.getInstance(environment.templateMaster, "master.tpl", "UTF-8", MasterTemplateBuilder.class);
        MasterTemplateBuilder masterTplBuilder = tplBuilder.getTemplateBuilder();

        StringBuilder sb = new StringBuilder();
        StringBuilder menuSb = new StringBuilder();

        Heritrix3JobMonitor h3Job = environment.h3JobMonitorThread.getRunningH3Job(numerics.get(0));
        Job job;
        
        HarvestDefinitionDAO dao = HarvestDefinitionDAO.getInstance();   

        if (h3Job != null && !h3Job.bInitialized) {
            h3Job.init();
        }

        if (h3Job != null && h3Job.isReady()) {
            h3Job.update();
            String action = req.getParameter("action");
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

            menuSb.append("<tr><td>&nbsp; &nbsp; &nbsp; <a href=\"");
            menuSb.append(NASEnvironment.servicePath);
            menuSb.append("job/");
            menuSb.append(h3Job.jobId);
            menuSb.append("/");
            menuSb.append("\"> Job ");
            menuSb.append(h3Job.jobId);
            menuSb.append("</a></td></tr>");
            
            sb.append("<div>\n");
            
            
            
            sb.append("<div style=\"float:left;width: 20%;min-width: 300px;}\">\n");

            sb.append("JobId: <a href=\"/History/Harveststatus-jobdetails.jsp?jobID="+h3Job.jobId+"\">");
            sb.append(h3Job.jobId);
            sb.append("</a><br />\n");
            sb.append("JobState: ");
            sb.append(h3Job.job.getStatus().getLocalizedString(Locale.ENGLISH).toUpperCase());
            sb.append("<br />\n");
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
            sb.append("TemplateName: <a href=\"/History/Harveststatus-download-job-harvest-template.jsp?JobID="+h3Job.jobId+"\">");
            sb.append(h3Job.job.getOrderXMLName());
            sb.append("</a><br />\n");
            sb.append("CountDomains: ");
            sb.append(h3Job.job.getCountDomains());
            sb.append("<br />\n");
            sb.append("MaxBytesPerDomain: ");
            sb.append(h3Job.job.getMaxBytesPerDomain());
            sb.append("<br />\n");
            sb.append("MaxObjectsPerDomain: ");
            sb.append(h3Job.job.getMaxObjectsPerDomain());
            sb.append("<br />\n");
            sb.append("MaxJobRunningTime: ");
            sb.append(h3Job.job.getMaxJobRunningTime());
            sb.append(" ms.<br />\n");
            
            sb.append("</div>\n");
            
            /* Heritrix3 WebUI */
            sb.append("<div style=\"float:left;\">\n");
            sb.append("<a href=\"");
            sb.append(h3Job.hostUrl+"/job/"+h3Job.jobname+"/job/"+h3Job.jobname);
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
            sb.append("/History/Harveststatus-download-job-harvest-template.jsp?JobID=");
            sb.append(h3Job.jobId);
            sb.append("\" class=\"btn btn-default\">");
            sb.append("Progression/Queues");
            sb.append("</a>");

            sb.append("&nbsp;");

            /* Crawllog */
            sb.append("<a href=\"");
            sb.append(NASEnvironment.servicePath);
            sb.append("job/");
            sb.append(h3Job.jobId);
            sb.append("/crawllog/");
            sb.append("\" class=\"btn btn-default\">");
            sb.append("Crawllog");
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
                    sb.append(job.availableActions.get(i));
                    sb.append("\" class=\"btn btn-default\">");
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
                sb.append(job.url);
                sb.append("/");
                sb.append("\">");
                sb.append(job.url);
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
        } else {
            sb.append("Job ");
            sb.append(numerics.get(0));
            sb.append(" is not running.");
        }

        if (masterTplBuilder.titlePlace != null) {
            masterTplBuilder.titlePlace.setText("Details and Actions on Running Job "+h3Job.jobId);
        }
        if (masterTplBuilder.menuPlace != null) {
            masterTplBuilder.menuPlace.setText(menuSb.toString());
        }
        if (masterTplBuilder.languagesPlace != null) {
            masterTplBuilder.languagesPlace.setText(environment.generateLanguageLinks(locale));
        }
        if (masterTplBuilder.headingPlace != null) {
            masterTplBuilder.headingPlace.setText("Details and Actions on Running Job "+h3Job.jobId);
        }
        if (masterTplBuilder.contentPlace != null) {
            masterTplBuilder.contentPlace.setText(sb.toString());
        }
        if (masterTplBuilder.versionPlace != null) {
            masterTplBuilder.versionPlace.setText(Constants.getVersionString(true));
        }
        if (masterTplBuilder.environmentPlace != null) {
            masterTplBuilder.environmentPlace.setText(Settings.get(CommonSettings.ENVIRONMENT_NAME));
        }
        if (masterTplBuilder.refreshInterval != null) {
            masterTplBuilder.refreshInterval.setText("<meta http-equiv=\"refresh\" content=\""+Settings.get(HarvesterSettings.HARVEST_MONITOR_REFRESH_INTERVAL)+"\"/>\n");
        }

        masterTplBuilder.write(out);

        out.flush();
        out.close();
    }

    public void crawllog_list(HttpServletRequest req, HttpServletResponse resp, List<Integer> numerics) throws IOException {
        Locale locale = resp.getLocale();
        resp.setContentType("text/html; charset=UTF-8");
        ServletOutputStream out = resp.getOutputStream();

        TemplateBuilderFactory<MasterTemplateBuilder> tplBuilder = TemplateBuilderFactory.getInstance(environment.templateMaster, "master.tpl", "UTF-8", MasterTemplateBuilder.class);
        MasterTemplateBuilder masterTplBuilder = tplBuilder.getTemplateBuilder();

        long lines;
        long linesPerPage = 100;
        long page = 1;
        long pages = 0;
        String q = null;

        String tmpStr;
        tmpStr = req.getParameter("page");
        if (tmpStr != null && tmpStr.length() > 0) {
            try {
                page = Long.parseLong(tmpStr);
            } catch (NumberFormatException e) {
            }
        }
        tmpStr = req.getParameter("itemsperpage");
        if (tmpStr != null && tmpStr.length() > 0) {
            try {
                linesPerPage = Long.parseLong(tmpStr);
            } catch (NumberFormatException e) {
            }
        }
        
        if (linesPerPage < 25) {
            linesPerPage = 25;
        }
        if (linesPerPage > 1000) {
            linesPerPage = 1000;
        }
        

        tmpStr = req.getParameter("q");
        if (tmpStr != null && tmpStr.length() > 0 && !tmpStr.equalsIgnoreCase(".*")) {
            q = tmpStr;
        }

        StringBuilder sb = new StringBuilder();
        StringBuilder menuSb = new StringBuilder();

        Heritrix3JobMonitor h3Job = environment.h3JobMonitorThread.getRunningH3Job(numerics.get(0));
        Pageable pageable = h3Job;

        if (h3Job != null && h3Job.isReady()) {
            menuSb.append("<tr><td>&nbsp; &nbsp; &nbsp; <a href=\"");
            menuSb.append(NASEnvironment.servicePath);
            menuSb.append("job/");
            menuSb.append(h3Job.jobId);
            menuSb.append("/");
            menuSb.append("\"> Job ");
            menuSb.append(h3Job.jobId);
            menuSb.append("</a></td></tr>");

            String actionStr = req.getParameter("action");
            
            if ("update".equalsIgnoreCase(actionStr)) {
                byte[] tmpBuf = new byte[1024 * 1024];
                h3Job.updateCrawlLog(tmpBuf);
            }
            
            long totalCachedLines = h3Job.getTotalCachedLines();
            long totalCachedSize = h3Job.getLastIndexed();

            SearchResult searchResult = null;
            
            if (q != null) {
            	
                searchResult = h3Job.getSearchResult(q);
                searchResult.update();
                pageable = searchResult;
            }

            lines = pageable.getIndexSize();
            
            if (lines > 0) {
                lines = (lines / 8) - 1;
                pages = Pagination.getPages(lines, linesPerPage);
            } else {
                lines = 0;
            }
            if (page > pages) {
                page = pages;
            }
            sb.append("Total cached lines: ");
            sb.append(totalCachedLines);
            sb.append(" URIs<br />\n");
            sb.append("Total cached size: ");
            sb.append(totalCachedSize);
            sb.append(" bytes<br />\n");
            

            sb.append("<a href=\"");
            sb.append("?action=update");
            sb.append("\" class=\"btn btn-default\">");
            sb.append("Update cache");
            sb.append("</a>");
            //sb.append("the cache manually ");
            sb.append("<br />\n");

            if (q == null) {
                q = ".*";
            }

            sb.append("<form class=\"form-horizontal\" action=\"?\" name=\"insert_form\" method=\"post\" enctype=\"application/x-www-form-urlencoded\" accept-charset=\"utf-8\">");
            sb.append("<label for=\"itemsperpage\">Lines per page to show:</label>");
            sb.append("<input type=\"text\" id=\"itemsperpage\" name=\"itemsperpage\" value=\"" + linesPerPage + "\" placeholder=\"must be &gt; 25 and &lt; 1000 \">\n");
            sb.append("<label for=\"q\">Filter regex:</label>");
            sb.append("<input type=\"text\" id=\"q\" name=\"q\" value=\"" + q + "\" placeholder=\"content-type\">\n");
            sb.append("<button type=\"submit\" name=\"search\" value=\"1\" class=\"btn btn-success\"><i class=\"icon-white icon-thumbs-up\"></i> Search</button>\n");

            sb.append("<br />\n");
            sb.append("<span>Matching lines: ");
            sb.append(lines);
            sb.append(" URIs</span>\n");
            sb.append("<br />\n");
            sb.append(Pagination.getPagination(page, linesPerPage, pages, false));
            sb.append("<div style=\"clear:both;\"></div>");
            sb.append("<div>\n");
            sb.append("<pre>\n");
            if (lines > 0) {
                byte[] pageBytes = pageable.readPage(page, linesPerPage, true);
                sb.append(new String(pageBytes, "UTF-8"));
            }
            sb.append("</pre>\n");
            sb.append("</div>\n");
            sb.append(Pagination.getPagination(page, linesPerPage, pages, false));
            sb.append("</form>");
        } else {
            sb.append("Job ");
            sb.append(numerics.get(0));
            sb.append(" is not running.");
        }

        if (masterTplBuilder.titlePlace != null) {
            masterTplBuilder.titlePlace.setText("Job "+numerics.get(0)+" Crawllog");
        }
        if (masterTplBuilder.menuPlace != null) {
            masterTplBuilder.menuPlace.setText(menuSb.toString());
        }
        if (masterTplBuilder.languagesPlace != null) {
            masterTplBuilder.languagesPlace.setText(environment.generateLanguageLinks(locale));
        }
        if (masterTplBuilder.headingPlace != null) {
            masterTplBuilder.headingPlace.setText("Job "+numerics.get(0)+" Crawllog");
        }
        if (masterTplBuilder.contentPlace != null) {
            masterTplBuilder.contentPlace.setText(sb.toString());
        }
        if (masterTplBuilder.versionPlace != null) {
            masterTplBuilder.versionPlace.setText(Constants.getVersionString(true));
        }
        if (masterTplBuilder.environmentPlace != null) {
            masterTplBuilder.environmentPlace.setText(Settings.get(CommonSettings.ENVIRONMENT_NAME));
        }
        if (masterTplBuilder.refreshInterval != null) {
            masterTplBuilder.refreshInterval.setText("<meta http-equiv=\"refresh\" content=\""+Settings.get(HarvesterSettings.HARVEST_MONITOR_REFRESH_INTERVAL)+"\"/>\n");
        }

        masterTplBuilder.write(out);

        out.flush();
        out.close();
    }

    public void frontier_list(HttpServletRequest req, HttpServletResponse resp, List<Integer> numerics) throws IOException {
        Locale locale = resp.getLocale();
        resp.setContentType("text/html; charset=UTF-8");
        ServletOutputStream out = resp.getOutputStream();

        TemplateBuilderFactory<MasterTemplateBuilder> tplBuilder = TemplateBuilderFactory.getInstance(environment.templateMaster, "master.tpl", "UTF-8", MasterTemplateBuilder.class);
        MasterTemplateBuilder masterTplBuilder = tplBuilder.getTemplateBuilder();

        StringBuilder sb = new StringBuilder();
        StringBuilder menuSb = new StringBuilder();

        String regex = req.getParameter("regex");
        if (regex == null || regex.length() == 0) {
            regex =".*";
        }
        long limit = 1000;
        String limitStr = req.getParameter("limit");
        if (limitStr != null && limitStr.length() > 0) {
            try {
                limit = Long.parseLong(limitStr);
            } catch (NumberFormatException e) {
            }
        }
        String initials = req.getParameter("initials");
        if (initials == null) {
            initials = "";
        }

        String resource = NAS_GROOVY_RESOURCE_PATH;
        InputStream in = JobResource.class.getClassLoader().getResourceAsStream(resource);
        ByteArrayOutputStream bOut = new ByteArrayOutputStream();
        byte[] tmpArr = new byte[8192];
        int read;
        while ((read = in.read(tmpArr)) != -1) {
            bOut.write(tmpArr, 0, read);
        }
        in.close();
        String script = new String(bOut.toByteArray(), "UTF-8");

        /*
        //RandomAccessFile raf = new RandomAccessFile("/home/nicl/workspace-nas-h3/heritrix3-scripts/src/main/java/view-frontier-url.groovy", "r");
        RandomAccessFile raf = new RandomAccessFile("/home/nicl/workspace-nas-h3/heritrix3-scripts/src/main/java/nas.groovy", "r");
        byte[] src = new byte[(int)raf.length()];
        raf.readFully(src);
        raf.close();
        String script = new String(src, "UTF-8");
        */

        String deleteStr = req.getParameter("delete");
        if (deleteStr != null && "1".equals(deleteStr) && initials != null && initials.length() > 0) {
            script += "\n";
            script += "\ninitials = \"" + initials + "\"";
            script += "\ndeleteFromFrontier '" + regex + "'\n";
        } else {
            script += "\n";
            script += "\nlistFrontier '" + regex + "', " + limit + "\n";
        }

        // To use, just remove the initial "//" from any one of these lines.
        //
        //killToeThread  1       //Kill a toe thread by number
        //listFrontier '.*stats.*'    //List uris in the frontier matching a given regexp
        //deleteFromFrontier '.*foobar.*'    //Remove uris matching a given regexp from the frontier
        //printCrawlLog '.*'          //View already crawled lines uris matching a given regexp

        Heritrix3JobMonitor h3Job = environment.h3JobMonitorThread.getRunningH3Job(numerics.get(0));

        if (h3Job != null && h3Job.isReady()) {
            menuSb.append("<tr><td>&nbsp; &nbsp; &nbsp; <a href=\"");
            menuSb.append(NASEnvironment.servicePath);
            menuSb.append("job/");
            menuSb.append(h3Job.jobId);
            menuSb.append("/");
            menuSb.append("\"> Job ");
            menuSb.append(h3Job.jobId);
            menuSb.append("</a></td></tr>");

            if (deleteStr != null && "1".equals(deleteStr) && (initials == null || initials.length() == 0)) {
                //sb.append("<span style=\"text-color: red;\">Initials required to delete from the frontier queue!</span><br />\n");
                sb.append("<div class=\"notify notify-red\"><span class=\"symbol icon-error\"></span> Initials required to delete from the frontier queue!</div>");
            }

            sb.append("<form class=\"form-horizontal\" action=\"?\" name=\"insert_form\" method=\"post\" enctype=\"application/x-www-form-urlencoded\" accept-charset=\"utf-8\">\n");
            sb.append("<label for=\"limit\">Lines to show:</label>");
            sb.append("<input type=\"text\" id=\"limit\" name=\"limit\" value=\"" + limit + "\" placeholder=\"return limit\">\n");
            sb.append("<label for=\"regex\">Filter regex:</label>");
            sb.append("<input type=\"text\" id=\"regex\" name=\"regex\" value=\"" + regex + "\" placeholder=\"regex\" style=\"display:inline;width:350px;\">\n");
            sb.append("<button type=\"submit\" name=\"show\" value=\"1\" class=\"btn btn-success\"><i class=\"icon-white icon-thumbs-up\"></i> Show</button>\n");
            sb.append("&nbsp;");
            sb.append("<label for=\"initials\">Deleter initials:</label>");
            sb.append("<input type=\"text\" id=\"initials\" name=\"initials\" value=\"" + initials  + "\" placeholder=\"initials\">\n");
            sb.append("<button type=\"submit\" name=\"delete\" value=\"1\" class=\"btn btn-success\"><i class=\"icon-white icon-thumbs-up\"></i> Delete</button>\n");
            sb.append("</form>\n");

            ScriptResult scriptResult = h3Job.h3wrapper.ExecuteShellScriptInJob(h3Job.jobResult.job.shortName, "groovy", script);
            //System.out.println(new String(scriptResult.response, "UTF-8"));
            if (scriptResult != null && scriptResult.script != null) {
                if (scriptResult.script.htmlOutput != null) {
                    sb.append("<fieldset><!--<legend>htmlOut</legend>-->");
                    sb.append(scriptResult.script.htmlOutput);
                    sb.append("</fieldset><br />\n");
                }
                if (scriptResult.script.rawOutput != null) {
                    sb.append("<fieldset><legend>rawOut</legend>");
                    sb.append("<pre>");
                    sb.append(scriptResult.script.rawOutput);
                    sb.append("</pre>");
                    sb.append("</fieldset><br />\n");
                }
            }
        } else {
            sb.append("Job ");
            sb.append(numerics.get(0));
            sb.append(" is not running.");
        }

        if (masterTplBuilder.titlePlace != null) {
            masterTplBuilder.titlePlace.setText("Job "+numerics.get(0)+" Frontier");
        }
        if (masterTplBuilder.menuPlace != null) {
            masterTplBuilder.menuPlace.setText(menuSb.toString());
        }
        if (masterTplBuilder.languagesPlace != null) {
            masterTplBuilder.languagesPlace.setText(environment.generateLanguageLinks(locale));
        }
        if (masterTplBuilder.headingPlace != null) {
            masterTplBuilder.headingPlace.setText("Job "+numerics.get(0)+" Frontier");
        }
        if (masterTplBuilder.contentPlace != null) {
            masterTplBuilder.contentPlace.setText(sb.toString());
        }
        if (masterTplBuilder.versionPlace != null) {
            masterTplBuilder.versionPlace.setText(Constants.getVersionString(true));
        }
        if (masterTplBuilder.environmentPlace != null) {
            masterTplBuilder.environmentPlace.setText(Settings.get(CommonSettings.ENVIRONMENT_NAME));
        }
        if (masterTplBuilder.refreshInterval != null) {
            masterTplBuilder.refreshInterval.setText("<meta http-equiv=\"refresh\" content=\""+Settings.get(HarvesterSettings.HARVEST_MONITOR_REFRESH_INTERVAL)+"\"/>\n");
        }

        masterTplBuilder.write(out);

        out.flush();
        out.close();
    }
    
    public void filter_add(HttpServletRequest req, HttpServletResponse resp, List<Integer> numerics) throws IOException {
        resp.setContentType("text/html; charset=UTF-8");
        ServletOutputStream out = resp.getOutputStream();

        TemplateBuilderFactory<MasterTemplateBuilder> tplBuilder = TemplateBuilderFactory.getInstance(environment.templateMaster, "master.tpl", "UTF-8", MasterTemplateBuilder.class);
        MasterTemplateBuilder masterTplBuilder = tplBuilder.getTemplateBuilder();

        StringBuilder sb = new StringBuilder();
        StringBuilder menuSb = new StringBuilder();

        String regex = req.getParameter("regex");
        if (regex == null) {
            regex = "";
        }
        String[] removeIndexes = req.getParameterValues("removeIndex");
        if(removeIndexes == null) {
        	removeIndexes = new String[0];
        }

        String resource = NAS_GROOVY_RESOURCE_PATH;
        InputStream in = JobResource.class.getClassLoader().getResourceAsStream(resource);
        ByteArrayOutputStream bOut = new ByteArrayOutputStream();
        byte[] tmpArr = new byte[8192];
        int read;
        while ((read = in.read(tmpArr)) != -1) {
            bOut.write(tmpArr, 0, read);
        }
        in.close();
        String script = new String(bOut.toByteArray(), "UTF-8");

        if (regex.length() > 0) {
        	String[] lines = regex.split(System.getProperty("line.separator"));
        	for(String line : lines) {
        		if(line.endsWith(System.getProperty("line.separator")) || line.endsWith("\r") || line.endsWith("\n")) {
        			line = line.substring(0, line.length() - 1);
        		}
	        	script += "\n";
	            script += "\naddFilter '" + line + "'\n";
        	}
        }
        if(removeIndexes != null && removeIndexes.length > 0) {
            script += "\nremoveFilters("+Arrays.toString(removeIndexes)+")\n";
        }
        script += "\nshowFilters()\n";

        Heritrix3JobMonitor h3Job = environment.h3JobMonitorThread.getRunningH3Job(numerics.get(0));

        if (h3Job != null && h3Job.isReady()) {
            menuSb.append("<tr><td>&nbsp; &nbsp; &nbsp; <a href=\"");
            menuSb.append(NASEnvironment.servicePath);
            menuSb.append("job/");
            menuSb.append(h3Job.jobId);
            menuSb.append("/");
            menuSb.append("\"> Job ");
            menuSb.append(h3Job.jobId);
            menuSb.append("</a></td></tr>");
            
            /*
            if (regex == null || regex.length() == 0) {
                sb.append("<div class=\"notify notify-red\"><span class=\"symbol icon-error\"></span> Regex required to be added as a filter!</div>");
            }
            */
            
            sb.append("<h4>Job "+h3Job.jobId+" status "+h3Job.jobResult.job.crawlControllerState+"</h4>");
            sb.append("<p>All URIs matching any of the following regular expressions will be rejected from the current job.</p>");

            sb.append("<form class=\"form-horizontal\" action=\"?\" name=\"insert_form\" method=\"post\" enctype=\"application/x-www-form-urlencoded\" accept-charset=\"utf-8\">\n");
            sb.append("<label for=\"regex\">Regular expressions :</label>");
            sb.append("<textarea rows=\"4\" cols=\"100\" id=\"regex\" name=\"regex\" placeholder=\"regex\"></textarea>\n");
            sb.append("<button type=\"submit\" name=\"add-filter\" value=\"1\" class=\"btn btn-success\"><i class=\"icon-white icon-thumbs-up\"></i> Add</button>\n");
            sb.append("&nbsp;");

            ScriptResult scriptResult = h3Job.h3wrapper.ExecuteShellScriptInJob(h3Job.jobResult.job.shortName, "groovy", script);

            if (scriptResult != null && scriptResult.script != null && scriptResult.script.htmlOutput != null) {
            	sb.append(scriptResult.script.htmlOutput);
            }
            
            sb.append("</form>\n");
        } else {
            sb.append("Job ");
            sb.append(numerics.get(0));
            sb.append(" is not running.");
        }

        if (masterTplBuilder.titlePlace != null) {
            masterTplBuilder.titlePlace.setText("Job "+numerics.get(0)+" RejectRules");
        }

        if (masterTplBuilder.menuPlace != null) {
            masterTplBuilder.menuPlace.setText(menuSb.toString());
        }

        if (masterTplBuilder.headingPlace != null) {
            masterTplBuilder.headingPlace.setText("Job "+numerics.get(0)+" RejectRules");
        }

        if (masterTplBuilder.contentPlace != null) {
            masterTplBuilder.contentPlace.setText(sb.toString());
        }

        if (masterTplBuilder.versionPlace != null) {
            masterTplBuilder.versionPlace.setText(Constants.getVersionString(true));
        }

        if (masterTplBuilder.environmentPlace != null) {
            masterTplBuilder.environmentPlace.setText(Settings.get(CommonSettings.ENVIRONMENT_NAME));
        }
        
        if (masterTplBuilder.refreshInterval != null) {
            masterTplBuilder.refreshInterval.setText("<meta http-equiv=\"refresh\" content=\""+Settings.get(HarvesterSettings.HARVEST_MONITOR_REFRESH_INTERVAL)+"\"/>\n");
        }

        masterTplBuilder.write(out);

        out.flush();
        out.close();
    }
    
    public void budget_change(HttpServletRequest req, HttpServletResponse resp, List<Integer> numerics) throws IOException {
        resp.setContentType("text/html; charset=UTF-8");
        ServletOutputStream out = resp.getOutputStream();

        TemplateBuilderFactory<MasterTemplateBuilder> tplBuilder = TemplateBuilderFactory.getInstance(environment.templateMaster, "master.tpl", "UTF-8", MasterTemplateBuilder.class);
        MasterTemplateBuilder masterTplBuilder = tplBuilder.getTemplateBuilder();

        StringBuilder sb = new StringBuilder();
        StringBuilder menuSb = new StringBuilder();

        String budget = req.getParameter("budget");
        if (budget == null) {
        	budget = "";
        }
        String key = req.getParameter("key");
        if (key == null) {
        	key = "";
        }
        String submitValue = req.getParameter("submitButton");
        

        String resource = NAS_GROOVY_RESOURCE_PATH;
        InputStream in = JobResource.class.getClassLoader().getResourceAsStream(resource);
        ByteArrayOutputStream bOut = new ByteArrayOutputStream();
        byte[] tmpArr = new byte[8192];
        int read;
        while ((read = in.read(tmpArr)) != -1) {
            bOut.write(tmpArr, 0, read);
        }
        in.close();
        String script = new String(bOut.toByteArray(), "UTF-8");
        String originalScript = script;

        script += "\n";
        if(submitValue != null) {
	        if (submitValue.equals("1") && budget != null && !budget.trim().isEmpty() && key != null && !key.trim().isEmpty()) {
	            script += "\nchangeBudget ('" + key+ "',"+ budget +")\n";
	        } else {
	        	budget = req.getParameter(submitValue+"-budget");
	        	script += "\nchangeBudget ('" + submitValue+ "',"+ budget +")\n";
	        }
        }
        script += "\n";
        script += "\nshowModBudgets()\n";
        
        originalScript += "\ngetQueueTotalBudget()\n";


        Heritrix3JobMonitor h3Job = environment.h3JobMonitorThread.getRunningH3Job(numerics.get(0));

        if (h3Job != null && h3Job.isReady()) {
            menuSb.append("<tr><td>&nbsp; &nbsp; &nbsp; <a href=\"");
            menuSb.append(NASEnvironment.servicePath);
            menuSb.append("job/");
            menuSb.append(h3Job.jobId);
            menuSb.append("/");
            menuSb.append("\"> Job ");
            menuSb.append(h3Job.jobId);
            menuSb.append("</a></td></tr>");

            /*
            if (budget == null || budget.length() == 0) {
                sb.append("<div class=\"notify notify-red\"><span class=\"symbol icon-error\"></span> New budget required!</div>");
            }
            */
            
            sb.append("<h4>Job "+h3Job.jobId+" status "+h3Job.jobResult.job.crawlControllerState+"</h4>");
            
            ScriptResult scriptResult = h3Job.h3wrapper.ExecuteShellScriptInJob(h3Job.jobResult.job.shortName, "groovy", originalScript);
            if (scriptResult != null && scriptResult.script != null && scriptResult.script.htmlOutput != null) {
            	sb.append("<p>Budget defined in job configuration: queue-total-budget of ");
            	sb.append(scriptResult.script.htmlOutput);
            	sb.append(" URIs.</p>");
            }
            
            try {
            	if (budget != null && budget.length() > 0) {
            		Integer.parseInt(budget);
            	}
            } catch(NumberFormatException e) {
            	sb.append("<div class=\"notify notify-red\"><span class=\"symbol icon-error\"></span> Budget must be a number!</div>");
            }

            sb.append("<form class=\"form-horizontal\" action=\"?\" name=\"insert_form\" method=\"post\" enctype=\"application/x-www-form-urlencoded\" accept-charset=\"utf-8\">\n");

            scriptResult = h3Job.h3wrapper.ExecuteShellScriptInJob(h3Job.jobResult.job.shortName, "groovy", script);

            if (scriptResult != null && scriptResult.script != null && scriptResult.script.htmlOutput != null) {
            	sb.append(scriptResult.script.htmlOutput);
            }
            
            sb.append("<label for=\"budget\">New domain/host :</label>");
            sb.append("<input type=\"text\" id=\"key\" name=\"key\" value=\"\" placeholder=\"key\">\n");
            sb.append("<input type=\"text\" id=\"budget\" name=\"budget\" value=\"\" placeholder=\"budget\">\n");
  
            
            sb.append("<button type=\"submit\" name=\"submitButton\" value=\"1\" class=\"btn btn-success\"><i class=\"icon-white icon-thumbs-up\"></i> Save</button>\n");
            sb.append("&nbsp;");
            
            sb.append("</form>\n");
        } else {
            sb.append("Job ");
            sb.append(numerics.get(0));
            sb.append(" is not running.");
        }

        if (masterTplBuilder.titlePlace != null) {
            masterTplBuilder.titlePlace.setText("Job "+numerics.get(0)+" Budget");
        }

        if (masterTplBuilder.menuPlace != null) {
            masterTplBuilder.menuPlace.setText(menuSb.toString());
        }

        if (masterTplBuilder.headingPlace != null) {
            masterTplBuilder.headingPlace.setText("Job "+numerics.get(0)+" Budget");
        }

        if (masterTplBuilder.contentPlace != null) {
            masterTplBuilder.contentPlace.setText(sb.toString());
        }

        if (masterTplBuilder.versionPlace != null) {
            masterTplBuilder.versionPlace.setText(Constants.getVersionString(true));
        }

        if (masterTplBuilder.environmentPlace != null) {
            masterTplBuilder.environmentPlace.setText(Settings.get(CommonSettings.ENVIRONMENT_NAME));
        }
        
        if (masterTplBuilder.refreshInterval != null) {
            masterTplBuilder.refreshInterval.setText("<meta http-equiv=\"refresh\" content=\""+Settings.get(HarvesterSettings.HARVEST_MONITOR_REFRESH_INTERVAL)+"\"/>\n");
        }

        masterTplBuilder.write(out);

        out.flush();
        out.close();
    }

    public static class ScriptTemplateBuilder extends MasterTemplateBuilder {

        @TemplateBuilderPlaceHolder("script")
        public TemplatePlaceHolder scriptPlace;

    }

    public void script(HttpServletRequest req, HttpServletResponse resp, List<Integer> numerics) throws IOException {
        Locale locale = resp.getLocale();
        resp.setContentType("text/html; charset=UTF-8");
        ServletOutputStream out = resp.getOutputStream();

        TemplateBuilderFactory<ScriptTemplateBuilder> tplBuilder = TemplateBuilderFactory.getInstance(environment.templateMaster, "h3script.tpl", "UTF-8", ScriptTemplateBuilder.class);
        ScriptTemplateBuilder masterTplBuilder = tplBuilder.getTemplateBuilder();

        String engineStr = req.getParameter("engine");
        String scriptStr = req.getParameter("script");
        if (scriptStr == null) {
            scriptStr = "";
        }

        StringBuilder sb = new StringBuilder();
        StringBuilder menuSb = new StringBuilder();

        Heritrix3JobMonitor h3Job = environment.h3JobMonitorThread.getRunningH3Job(numerics.get(0));

        if (h3Job != null && h3Job.isReady()) {
            menuSb.append("<tr><td>&nbsp; &nbsp; &nbsp; <a href=\"");
            menuSb.append(NASEnvironment.servicePath);
            menuSb.append("job/");
            menuSb.append(h3Job.jobId);
            menuSb.append("/");
            menuSb.append("\"> Job ");
            menuSb.append(h3Job.jobId);
            menuSb.append("</a></td></tr>");

            if (engineStr != null && engineStr.length() > 0 && scriptStr != null && scriptStr.length() > 0) {
                ScriptResult scriptResult = h3Job.h3wrapper.ExecuteShellScriptInJob(h3Job.jobResult.job.shortName, engineStr, scriptStr);
                //System.out.println(new String(scriptResult.response, "UTF-8"));
                if (scriptResult != null && scriptResult.script != null) {
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
            }
        }

        if (masterTplBuilder.titlePlace != null) {
            masterTplBuilder.titlePlace.setText("Scripting console");
        }
        if (masterTplBuilder.menuPlace != null) {
            masterTplBuilder.menuPlace.setText(menuSb.toString());
        }
        if (masterTplBuilder.languagesPlace != null) {
            masterTplBuilder.languagesPlace.setText(environment.generateLanguageLinks(locale));
        }
        if (masterTplBuilder.headingPlace != null) {
            masterTplBuilder.headingPlace.setText("Scripting console");
        }
        if (masterTplBuilder.scriptPlace != null) {
            masterTplBuilder.scriptPlace.setText(scriptStr);
        }
        if (masterTplBuilder.contentPlace != null) {
            masterTplBuilder.contentPlace.setText(sb.toString());
        }
        if (masterTplBuilder.versionPlace != null) {
            masterTplBuilder.versionPlace.setText(Constants.getVersionString(true));
        }
        if (masterTplBuilder.environmentPlace != null) {
            masterTplBuilder.environmentPlace.setText(Settings.get(CommonSettings.ENVIRONMENT_NAME));
        }
        
        if (masterTplBuilder.refreshInterval != null) {
            masterTplBuilder.refreshInterval.setText("<meta http-equiv=\"refresh\" content=\""+Settings.get(HarvesterSettings.HARVEST_MONITOR_REFRESH_INTERVAL)+"\"/>\n");
        }

        masterTplBuilder.write(out);

        out.flush();
        out.close();
    }

    public void report(HttpServletRequest req, HttpServletResponse resp, List<Integer> numerics) throws IOException {
        Locale locale = resp.getLocale();
        resp.setContentType("text/html; charset=UTF-8");
        ServletOutputStream out = resp.getOutputStream();

        TemplateBuilderFactory<MasterTemplateBuilder> tplBuilder = TemplateBuilderFactory.getInstance(environment.templateMaster, "master.tpl", "UTF-8", MasterTemplateBuilder.class);
        MasterTemplateBuilder masterTplBuilder = tplBuilder.getTemplateBuilder();

        StringBuilder sb = new StringBuilder();
        StringBuilder menuSb = new StringBuilder();

        String reportStr = req.getParameter("report");

        Heritrix3JobMonitor h3Job = environment.h3JobMonitorThread.getRunningH3Job(numerics.get(0));
        Job job;

        if (h3Job != null && h3Job.isReady()) {
            menuSb.append("<tr><td>&nbsp; &nbsp; &nbsp; <a href=\"");
            menuSb.append(NASEnvironment.servicePath);
            menuSb.append("job/");
            menuSb.append(h3Job.jobId);
            menuSb.append("/");
            menuSb.append("\"> Job ");
            menuSb.append(h3Job.jobId);
            menuSb.append("</a></td></tr>");

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

        if (masterTplBuilder.titlePlace != null) {
            masterTplBuilder.titlePlace.setText("Job "+numerics.get(0)+" Reports");
        }
        if (masterTplBuilder.menuPlace != null) {
            masterTplBuilder.menuPlace.setText(menuSb.toString());
        }
        if (masterTplBuilder.languagesPlace != null) {
            masterTplBuilder.languagesPlace.setText(environment.generateLanguageLinks(locale));
        }
        if (masterTplBuilder.headingPlace != null) {
            masterTplBuilder.headingPlace.setText("Job "+numerics.get(0)+" Reports");
        }
        if (masterTplBuilder.contentPlace != null) {
            masterTplBuilder.contentPlace.setText(sb.toString());
        }
        if (masterTplBuilder.versionPlace != null) {
            masterTplBuilder.versionPlace.setText(Constants.getVersionString(true));
        }
        if (masterTplBuilder.environmentPlace != null) {
            masterTplBuilder.environmentPlace.setText(Settings.get(CommonSettings.ENVIRONMENT_NAME));
        }
        if (masterTplBuilder.refreshInterval != null) {
            masterTplBuilder.refreshInterval.setText("<meta http-equiv=\"refresh\" content=\""+Settings.get(HarvesterSettings.HARVEST_MONITOR_REFRESH_INTERVAL)+"\"/>\n");
        }

        masterTplBuilder.write(out);

        out.flush();
        out.close();
    }

}
