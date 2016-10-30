package dk.netarkivet.harvester.webinterface.servlet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.netarchivesuite.heritrix3wrapper.ScriptResult;
import org.netarchivesuite.heritrix3wrapper.jaxb.Job;

import com.antiaction.common.filter.Caching;
import com.antiaction.common.templateengine.TemplateBuilderFactory;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.Constants;
import dk.netarkivet.common.utils.Settings;

public class JobResource implements ResourceAbstract {

    private static final String NAS_GROOVY_RESOURCE_PATH = "dk/netarkivet/harvester/webinterface/servlet/nas.groovy";

    private NASEnvironment environment;

    protected int R_JOB = -1;

    protected int R_CRAWLLOG = -1;

    protected int R_FRONTIER = -1;

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
        } else if (resource_id == R_SCRIPT) {
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
        resp.setContentType("text/html; charset=UTF-8");
        ServletOutputStream out = resp.getOutputStream();

        Caching.caching_disable_headers(resp);

        TemplateBuilderFactory<MasterTemplateBuilder> tplBuilder = TemplateBuilderFactory.getInstance(environment.templateMaster, "master.tpl", "UTF-8", MasterTemplateBuilder.class);
        MasterTemplateBuilder masterTplBuilder = tplBuilder.getTemplateBuilder();

        StringBuilder sb = new StringBuilder();
        StringBuilder menuSb = new StringBuilder();

        Heritrix3JobMonitor h3Job = environment.h3JobMonitorThread.getRunningH3Job(numerics.get(0));
        Job job;

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
            menuSb.append("\"> ");
            menuSb.append(h3Job.jobId);
            menuSb.append("</a></td></tr>");

            sb.append("JobId: ");
            sb.append(h3Job.jobId);
            sb.append("<br />\n");
            sb.append("HarvestNum: ");
            sb.append(h3Job.job.getHarvestNum());
            sb.append("<br />\n");
            sb.append("Snapshop: ");
            sb.append(h3Job.job.isSnapshot());
            sb.append("<br />\n");
            sb.append("Channel: ");
            sb.append(h3Job.job.getChannel());
            sb.append("<br />\n");
            sb.append("OrderXMLName: ");
            sb.append(h3Job.job.getOrderXMLName());
            sb.append("<br />\n");
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

            sb.append("<br />\n");

            sb.append("<a href=\"");
            sb.append(NASEnvironment.servicePath);
            sb.append("job/");
            sb.append(h3Job.jobId);
            sb.append("/crawllog/");
            sb.append("\" class=\"btn btn-default\">");
            sb.append("Show/filter crawllog");
            sb.append("</a>");

            sb.append("&nbsp;");

            sb.append("<a href=\"");
            sb.append(NASEnvironment.servicePath);
            sb.append("job/");
            sb.append(h3Job.jobId);
            sb.append("/frontier/");
            sb.append("\" class=\"btn btn-default\">");
            sb.append("Show/delete frontier queue");
            sb.append("</a>");

            sb.append("&nbsp;");

            sb.append("<a href=\"");
            sb.append(NASEnvironment.servicePath);
            sb.append("job/");
            sb.append(h3Job.jobId);
            sb.append("/script/");
            sb.append("\" class=\"btn btn-default\">");
            sb.append("Open scripting console");
            sb.append("</a>");

            sb.append("&nbsp;");

            sb.append("<a href=\"");
            sb.append(NASEnvironment.servicePath);
            sb.append("job/");
            sb.append(h3Job.jobId);
            sb.append("/report/");
            sb.append("\" class=\"btn btn-default\">");
            sb.append("Show report");
            sb.append("</a>");

            sb.append("&nbsp;");

            sb.append("<a href=\"");
            sb.append(h3Job.hostUrl);
            sb.append("\" class=\"btn btn-default\">");
            sb.append("Heritrix3 WebUI");
            sb.append("</a>");

            sb.append("<br />\n");

            if (h3Job.jobResult != null && h3Job.jobResult.job != null) {
                job = h3Job.jobResult.job;
                for (int i=0; i<job.availableActions.size(); ++i) {
                    if (i > 0) {
                        sb.append("&nbsp;");
                    }
                    //  disabled="disabled"
                    sb.append("<a href=\"?action=");
                    sb.append(job.availableActions.get(i));
                    sb.append("\" class=\"btn btn-default\">");
                    sb.append(job.availableActions.get(i));
                    sb.append("</a>");
                }
                sb.append("<br />\n");
                sb.append("<br />\n");
            }

            if (h3Job.jobResult != null && h3Job.jobResult.job != null) {
                job = h3Job.jobResult.job;
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
            masterTplBuilder.titlePlace.setText("Running job");
        }

        if (masterTplBuilder.menuPlace != null) {
            masterTplBuilder.menuPlace.setText(menuSb.toString());
        }

        if (masterTplBuilder.headingPlace != null) {
            masterTplBuilder.headingPlace.setText("Running job");
        }

        if (masterTplBuilder.contentPlace != null) {
            masterTplBuilder.contentPlace.setText(sb.toString());
        }

        if (masterTplBuilder.versionPlace != null) {
            masterTplBuilder.versionPlace.setText(Constants.getVersionString());
        }

        if (masterTplBuilder.environmentPlace != null) {
            masterTplBuilder.environmentPlace.setText(Settings.get(CommonSettings.ENVIRONMENT_NAME));
        }

        masterTplBuilder.write(out);

        out.flush();
        out.close();
    }

    public void crawllog_list(HttpServletRequest req, HttpServletResponse resp, List<Integer> numerics) throws IOException {
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
            menuSb.append("\"> ");
            menuSb.append(h3Job.jobId);
            menuSb.append("</a></td></tr>");

            String actionStr = req.getParameter("action");
            if ("update".equalsIgnoreCase(actionStr)) {
                byte[] tmpBuf = new byte[1024 * 1024];
                h3Job.updateCrawlLog(tmpBuf);
            }

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
            sb.append("Cached lines: ");
            sb.append(lines);
            sb.append("<br />\n");
            sb.append("Cached size: ");
            sb.append(h3Job.lastIndexed);
            sb.append("<br />\n");

            sb.append("<a href=\"");
            sb.append("?action=update");
            sb.append("\" class=\"btn btn-default\">");
            sb.append("Update cache");
            sb.append("</a>");
            sb.append("<br />\n");

            if (q == null) {
                q = ".*";
            }
            sb.append("<form class=\"form-horizontal\" action=\"?\" name=\"insert_form\" method=\"post\" enctype=\"application/x-www-form-urlencoded\" accept-charset=\"utf-8\">");
            sb.append("<input type=<\"text\" id=\"q\" name=\"q\" value=\"" + q + "\" placeholder=\"content-type\">\n");
            sb.append("<button type=\"submit\" name=\"search\" value=\"1\" class=\"btn btn-success\"><i class=\"icon-white icon-thumbs-up\"></i> Search</button>\n");
            sb.append("</form>");

            sb.append("<br />\n");
            sb.append("<br />\n");
            sb.append(Pagination.getPagination(page, linesPerPage, pages, false));
            sb.append("<div>\n");
            sb.append("<pre>\n");
            if (lines > 0) {
                byte[] pageBytes = pageable.readPage(page, linesPerPage, true);
                sb.append(new String(pageBytes, "UTF-8"));
            }
            sb.append("</pre>\n");
            sb.append("</div>\n");
            sb.append(Pagination.getPagination(page, linesPerPage, pages, false));
        } else {
            sb.append("Job ");
            sb.append(numerics.get(0));
            sb.append(" is not running.");
        }

        if (masterTplBuilder.titlePlace != null) {
            masterTplBuilder.titlePlace.setText("Crawllog");
        }

        if (masterTplBuilder.menuPlace != null) {
            masterTplBuilder.menuPlace.setText(menuSb.toString());
        }

        if (masterTplBuilder.headingPlace != null) {
            masterTplBuilder.headingPlace.setText("Crawllog");
        }

        if (masterTplBuilder.contentPlace != null) {
            masterTplBuilder.contentPlace.setText(sb.toString());
        }

        if (masterTplBuilder.versionPlace != null) {
            masterTplBuilder.versionPlace.setText(Constants.getVersionString());
        }

        if (masterTplBuilder.environmentPlace != null) {
            masterTplBuilder.environmentPlace.setText(Settings.get(CommonSettings.ENVIRONMENT_NAME));
        }

        masterTplBuilder.write(out);

        out.flush();
        out.close();
    }

    public void frontier_list(HttpServletRequest req, HttpServletResponse resp, List<Integer> numerics) throws IOException {
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

        String tmpStr = req.getParameter("delete");
        if (tmpStr != null && "1".equals(tmpStr) ) {
            script += "\n\ndeleteFromFrontier '" + regex + "'\n";
        } else {
            script += "\n\nlistFrontier '" + regex + "'\n";
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
            menuSb.append("\"> ");
            menuSb.append(h3Job.jobId);
            menuSb.append("</a></td></tr>");

            sb.append("<form class=\"form-horizontal\" action=\"?\" name=\"insert_form\" method=\"post\" enctype=\"application/x-www-form-urlencoded\" accept-charset=\"utf-8\">");
            sb.append("<input type=<\"text\" id=\"regex\" name=\"regex\" value=\"" + regex + "\" placeholder=\"content-type\">\n");
            sb.append("<button type=\"submit\" name=\"show\" value=\"1\" class=\"btn btn-success\"><i class=\"icon-white icon-thumbs-up\"></i> Show</button>\n");
            sb.append("<button type=\"submit\" name=\"delete\" value=\"1\" class=\"btn btn-success\"><i class=\"icon-white icon-thumbs-up\"></i> Delete</button>\n");
            sb.append("</form>");

            ScriptResult scriptResult = h3Job.h3wrapper.ExecuteShellScriptInJob(h3Job.jobResult.job.shortName, "groovy", script);
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
            }
        } else {
            sb.append("Job ");
            sb.append(numerics.get(0));
            sb.append(" is not running.");
        }

        if (masterTplBuilder.titlePlace != null) {
            masterTplBuilder.titlePlace.setText("Frontier queue");
        }

        if (masterTplBuilder.menuPlace != null) {
            masterTplBuilder.menuPlace.setText(menuSb.toString());
        }

        if (masterTplBuilder.headingPlace != null) {
            masterTplBuilder.headingPlace.setText("Frontier queue");
        }

        if (masterTplBuilder.contentPlace != null) {
            masterTplBuilder.contentPlace.setText(sb.toString());
        }

        if (masterTplBuilder.versionPlace != null) {
            masterTplBuilder.versionPlace.setText(Constants.getVersionString());
        }

        if (masterTplBuilder.environmentPlace != null) {
            masterTplBuilder.environmentPlace.setText(Settings.get(CommonSettings.ENVIRONMENT_NAME));
        }

        masterTplBuilder.write(out);

        out.flush();
        out.close();
    }

    public void script(HttpServletRequest req, HttpServletResponse resp, List<Integer> numerics) throws IOException {
        resp.setContentType("text/html; charset=UTF-8");
        ServletOutputStream out = resp.getOutputStream();

        TemplateBuilderFactory<MasterTemplateBuilder> tplBuilder = TemplateBuilderFactory.getInstance(environment.templateMaster, "h3script.tpl", "UTF-8", MasterTemplateBuilder.class);
        MasterTemplateBuilder masterTplBuilder = tplBuilder.getTemplateBuilder();

        String engineStr = req.getParameter("engine");
        String scriptStr = req.getParameter("script");

        StringBuilder sb = new StringBuilder();
        StringBuilder menuSb = new StringBuilder();

        Heritrix3JobMonitor h3Job = environment.h3JobMonitorThread.getRunningH3Job(numerics.get(0));

        if (h3Job != null && h3Job.isReady()) {
            menuSb.append("<tr><td>&nbsp; &nbsp; &nbsp; <a href=\"");
            menuSb.append(NASEnvironment.servicePath);
            menuSb.append("job/");
            menuSb.append(h3Job.jobId);
            menuSb.append("/");
            menuSb.append("\"> ");
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
                }
            }
        }

        if (masterTplBuilder.titlePlace != null) {
            masterTplBuilder.titlePlace.setText("Scripting console");
        }

        if (masterTplBuilder.menuPlace != null) {
            masterTplBuilder.menuPlace.setText(menuSb.toString());
        }

        if (masterTplBuilder.headingPlace != null) {
            masterTplBuilder.headingPlace.setText("Scripting console");
        }

        if (masterTplBuilder.contentPlace != null) {
            masterTplBuilder.contentPlace.setText(sb.toString());
        }

        if (masterTplBuilder.versionPlace != null) {
            masterTplBuilder.versionPlace.setText(Constants.getVersionString());
        }

        if (masterTplBuilder.environmentPlace != null) {
            masterTplBuilder.environmentPlace.setText(Settings.get(CommonSettings.ENVIRONMENT_NAME));
        }

        masterTplBuilder.write(out);

        out.flush();
        out.close();
    }

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

    public void report(HttpServletRequest req, HttpServletResponse resp, List<Integer> numerics) throws IOException {
        resp.setContentType("text/html; charset=UTF-8");
        ServletOutputStream out = resp.getOutputStream();

        TemplateBuilderFactory<MasterTemplateBuilder> tplBuilder = TemplateBuilderFactory.getInstance(environment.templateMaster, "master.tpl", "UTF-8", MasterTemplateBuilder.class);
        MasterTemplateBuilder masterTplBuilder = tplBuilder.getTemplateBuilder();

        StringBuilder sb = new StringBuilder();
        StringBuilder menuSb = new StringBuilder();

        Heritrix3JobMonitor h3Job = environment.h3JobMonitorThread.getRunningH3Job(numerics.get(0));

        if (h3Job != null && h3Job.isReady()) {
            menuSb.append("<tr><td>&nbsp; &nbsp; &nbsp; <a href=\"");
            menuSb.append(NASEnvironment.servicePath);
            menuSb.append("job/");
            menuSb.append(h3Job.jobId);
            menuSb.append("/");
            menuSb.append("\"> ");
            menuSb.append(h3Job.jobId);
            menuSb.append("</a></td></tr>");
        }

        if (masterTplBuilder.titlePlace != null) {
            masterTplBuilder.titlePlace.setText("Scripting console");
        }

        if (masterTplBuilder.menuPlace != null) {
            masterTplBuilder.menuPlace.setText(menuSb.toString());
        }

        if (masterTplBuilder.headingPlace != null) {
            masterTplBuilder.headingPlace.setText("Scripting console");
        }

        if (masterTplBuilder.contentPlace != null) {
            masterTplBuilder.contentPlace.setText(sb.toString());
        }

        if (masterTplBuilder.versionPlace != null) {
            masterTplBuilder.versionPlace.setText(Constants.getVersionString());
        }

        if (masterTplBuilder.environmentPlace != null) {
            masterTplBuilder.environmentPlace.setText(Settings.get(CommonSettings.ENVIRONMENT_NAME));
        }

        masterTplBuilder.write(out);

        out.flush();
        out.close();
    }

}
