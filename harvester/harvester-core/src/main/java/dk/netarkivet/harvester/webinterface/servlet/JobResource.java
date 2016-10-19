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

public class JobResource implements ResourceAbstract {

    private static final String NAS_GROOVY_RESOURCE_PATH = "dk/netarkivet/harvester/webinterface/servlet/nas.groovy";

    private NASEnvironment environment;

    protected int R_JOB = -1;

    protected int R_FRONTIER = -1;

    protected int R_CRAWLLOG = -1;

    @Override
    public void resources_init(NASEnvironment environment) {
        this.environment = environment;
    }

    @Override
    public void resources_add(ResourceManagerAbstract resourceManager) {
        R_JOB = resourceManager.resource_add(this, "/job/<numeric>/", false);
        R_FRONTIER = resourceManager.resource_add(this, "/job/<numeric>/frontier/", false);
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
        if (resource_id == R_JOB) {
            if ("GET".equals(method)) {
                job(req, resp, numerics);
            }
        }
        if (resource_id == R_FRONTIER) {
            if ("GET".equals(method) || "POST".equals(method)) {
                frontier_list(req, resp, numerics);
            }
        }
        else if (resource_id == R_CRAWLLOG) {
            if ("GET".equals(method) || "POST".equals(method)) {
                crawllog_list(req, resp, numerics);
            }
        }
    }

    public void job(HttpServletRequest req, HttpServletResponse resp, List<Integer> numerics) throws IOException {
        resp.setContentType("text/html; charset=UTF-8");
        ServletOutputStream out = resp.getOutputStream();

        StringBuilder sb = new StringBuilder();

        Heritrix3JobMonitor h3Job = environment.h3JobMonitorThread.getRunningH3Job(numerics.get(0));

        if (h3Job != null && h3Job.isReady()) {
            sb.append("Job: ");
            sb.append(h3Job.jobId);
            sb.append("<br />\n");
            sb.append("<br />\n");
            sb.append("<a href=\"");
            sb.append(NASEnvironment.servicePath);
            sb.append("job/");
            sb.append(h3Job.jobId);
            sb.append("/frontier/");
            sb.append("\">");
            sb.append("Frontier queue");
            sb.append("</a>");
            sb.append("<br />\n");
            sb.append("<a href=\"");
            sb.append(NASEnvironment.servicePath);
            sb.append("job/");
            sb.append(h3Job.jobId);
            sb.append("/crawllog/");
            sb.append("\">");
            sb.append("CrawlLog");
            sb.append("</a>");
            sb.append("<br />\n");
        } else {
            sb.append("Job ");
            sb.append(numerics.get(0));
            sb.append(" is not running.");
        }

        out.write(sb.toString().getBytes("UTF-8"));
        out.flush();
        out.close();
    }

    public void frontier_list(HttpServletRequest req, HttpServletResponse resp, List<Integer> numerics) throws IOException {
        resp.setContentType("text/html; charset=UTF-8");
        ServletOutputStream out = resp.getOutputStream();

        StringBuilder sb = new StringBuilder();

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
                    sb.append(scriptResult.script.rawOutput);
                }
            }
        } else {
            sb.append("Job ");
            sb.append(numerics.get(0));
            sb.append(" is not running.");
        }

        out.write(sb.toString().getBytes("UTF-8"));
        out.flush();
        out.close();
    }

    public void crawllog_list(HttpServletRequest req, HttpServletResponse resp, List<Integer> numerics) throws IOException {
        resp.setContentType("text/html; charset=UTF-8");
        ServletOutputStream out = resp.getOutputStream();

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
        if (tmpStr != null && tmpStr.length() > 0 && tmpStr.equalsIgnoreCase(".*")) {
            q = tmpStr;
        }
        if (q == null) {
            q = ".*";
        }

        StringBuilder sb = new StringBuilder();

        Heritrix3JobMonitor h3Job = environment.h3JobMonitorThread.getRunningH3Job(numerics.get(0));
        Pageable pageable = h3Job;

        if (h3Job != null && h3Job.isReady()) {
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
            }
            if (page > pages) {
                page = pages;
            }
            sb.append("Log lines: ");
            sb.append(lines);
            sb.append("<br />\n");
            sb.append("Page: ");
            sb.append(page);
            sb.append(" of ");
            sb.append(pages);
            sb.append("<br />\n");

            sb.append("<form class=\"form-horizontal\" action=\"?\" name=\"insert_form\" method=\"post\" enctype=\"application/x-www-form-urlencoded\" accept-charset=\"utf-8\">");
            sb.append("<input type=<\"text\" id=\"regex\" name=\"regex\" value=\"" + q + "\" placeholder=\"content-type\">\n");
            sb.append("<button type=\"submit\" name=\"search\" value=\"1\" class=\"btn btn-success\"><i class=\"icon-white icon-thumbs-up\"></i> Search</button>\n");
            sb.append("</form>");

            sb.append("<br />\n");
            sb.append("<br />\n");
            sb.append(Pagination.getPagination(page, linesPerPage, pages, false));
            sb.append("<pre>\n");
            if (lines > 0) {
                byte[] bytes = pageable.readPage(page, linesPerPage, true);
                sb.append(new String(bytes, "UTF-8"));
            }
            sb.append("</pre>\n");
        } else {
            sb.append("Job ");
            sb.append(numerics.get(0));
            sb.append(" is not running.");
        }

        out.write(sb.toString().getBytes("UTF-8"));
        out.flush();
        out.close();
    }

}
