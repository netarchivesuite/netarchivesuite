package dk.netarkivet.heritrix3.monitor.resources;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Locale;

import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.antiaction.common.filter.Caching;
import com.antiaction.common.templateengine.TemplateBuilderFactory;

import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.harvester.HarvesterSettings;
import dk.netarkivet.heritrix3.monitor.Heritrix3JobMonitor;
import dk.netarkivet.heritrix3.monitor.NASEnvironment;
import dk.netarkivet.heritrix3.monitor.NASUser;
import dk.netarkivet.heritrix3.monitor.Pageable;
import dk.netarkivet.heritrix3.monitor.Pagination;
import dk.netarkivet.heritrix3.monitor.ResourceAbstract;
import dk.netarkivet.heritrix3.monitor.ResourceManagerAbstract;
import dk.netarkivet.heritrix3.monitor.SearchResult;

public class H3CrawlLogCachedResource implements ResourceAbstract {

    private NASEnvironment environment;

    protected int R_CRAWLLOG = -1;

    @Override
    public void resources_init(NASEnvironment environment) {
        this.environment = environment;
    }

    @Override
    public void resources_add(ResourceManagerAbstract resourceManager) {
        R_CRAWLLOG = resourceManager.resource_add(this, "/job/<numeric>/crawllog/", false);
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
        if (resource_id == R_CRAWLLOG) {
            if ("GET".equals(method) || "POST".equals(method)) {
                crawllog_list(req, resp, numerics);
            }
        }
    }

    public void crawllog_list(HttpServletRequest req, HttpServletResponse resp, List<Integer> numerics) throws IOException {
        Locale locale = resp.getLocale();
        resp.setContentType("text/html; charset=UTF-8");
        ServletOutputStream out = resp.getOutputStream();
        Caching.caching_disable_headers(resp);

        TemplateBuilderFactory<MasterTemplateBuilder> masterTplBuilderFactory = TemplateBuilderFactory.getInstance(environment.templateMaster, "master.tpl", "UTF-8", MasterTemplateBuilder.class);
        MasterTemplateBuilder masterTplBuilder = masterTplBuilderFactory.getTemplateBuilder();

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

        String additionalParams;

        tmpStr = req.getParameter("q");
        if (tmpStr != null && tmpStr.length() > 0 && !tmpStr.equalsIgnoreCase(".*")) {
            q = tmpStr;
            additionalParams = "&q=" + URLEncoder.encode(q, "UTF-8");
        } else {
        	additionalParams = "";
        }

        StringBuilder sb = new StringBuilder();

        long jobId = numerics.get(0);
        Heritrix3JobMonitor h3Job = environment.h3JobMonitorThread.getRunningH3Job(jobId);
        Pageable pageable = h3Job;

        if (h3Job != null && h3Job.isReady()) {
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
            } else  {
                q = ".*";
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

            sb.append("<div style=\"margin-bottom:20px;\">\n");
            sb.append("<div style=\"float:left;min-width:180px;\">\n");
            sb.append("Total cached lines: ");
            sb.append(totalCachedLines);
            sb.append(" URIs<br />\n");
            sb.append("Total cached size: ");
            sb.append(totalCachedSize);
            sb.append(" bytes\n");
            sb.append("</div>\n");
            
            sb.append("<div style=\"float:left;\">\n");
            sb.append("<a href=\"");
            sb.append("?action=update");
            sb.append("\" class=\"btn btn-default\">");
            sb.append("Update cache");
            sb.append("</a>");
            //sb.append("the cache manually ");
            sb.append("</div>\n");

            sb.append("<div style=\"clear:both;\"></div>\n");
            sb.append("</div>\n");

            sb.append("<div style=\"margin-bottom:20px;\">\n");

            sb.append("<form class=\"form-horizontal\" action=\"?\" name=\"insert_form\" method=\"post\" enctype=\"application/x-www-form-urlencoded\" accept-charset=\"utf-8\">");
            sb.append("<label for=\"itemsperpage\">Lines per page:</label>");
            sb.append("<input type=\"text\" id=\"itemsperpage\" name=\"itemsperpage\" value=\"" + linesPerPage + "\" placeholder=\"must be &gt; 25 and &lt; 1000 \">\n");
            sb.append("<label for=\"q\">Filter regex:</label>");
            sb.append("<input type=\"text\" id=\"q\" name=\"q\" value=\"" + q + "\" placeholder=\"content-type\" style=\"display:inline;width:350px;\">\n");
            sb.append("<button type=\"submit\" name=\"search\" value=\"1\" class=\"btn btn-success\"><i class=\"icon-white icon-thumbs-up\"></i> Search</button>\n");

            sb.append("</div>\n");
            
            sb.append("<div style=\"float:left;margin: 20px 0px;\">\n");
            sb.append("<span>Matching lines: ");
            sb.append(lines);
            sb.append(" URIs</span>\n");
            sb.append("</div>\n");
            sb.append(Pagination.getPagination(page, linesPerPage, pages, false, additionalParams));
            sb.append("<div style=\"clear:both;\"></div>");
            sb.append("<div>\n");
            sb.append("<pre>\n");
            if (lines > 0) {
                byte[] pageBytes = pageable.readPage(page, linesPerPage, true);
                sb.append(new String(pageBytes, "UTF-8"));
            }
            sb.append("</pre>\n");
            sb.append("</div>\n");
            sb.append(Pagination.getPagination(page, linesPerPage, pages, false, additionalParams));
            sb.append("</form>");
        } else {
            sb.append("Job ");
            sb.append(jobId);
            sb.append(" is not running.");
        }

        StringBuilder menuSb = masterTplBuilder.buildMenu(new StringBuilder(), h3Job);

        masterTplBuilder.insertContent("Job " + jobId + " Crawllog", menuSb.toString(), environment.generateLanguageLinks(locale),
        		"Job " + jobId + " Crawllog", sb.toString(),
        		"<meta http-equiv=\"refresh\" content=\""+Settings.get(HarvesterSettings.HARVEST_MONITOR_REFRESH_INTERVAL)+"\"/>\n").write(out);

        out.flush();
        out.close();
    }

}
