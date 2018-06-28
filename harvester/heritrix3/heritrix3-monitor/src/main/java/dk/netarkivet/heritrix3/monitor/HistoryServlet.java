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

package dk.netarkivet.heritrix3.monitor;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.antiaction.common.servlet.AutoIncrement;
import com.antiaction.common.servlet.PathMap;

import dk.netarkivet.heritrix3.monitor.HttpLocaleHandler.HttpLocale;
import dk.netarkivet.heritrix3.monitor.resources.ConfigResource;
import dk.netarkivet.heritrix3.monitor.resources.H3BudgetResource;
import dk.netarkivet.heritrix3.monitor.resources.H3CrawlLogCachedResource;
import dk.netarkivet.heritrix3.monitor.resources.H3FilterResource;
import dk.netarkivet.heritrix3.monitor.resources.H3FrontierDeleteResource;
import dk.netarkivet.heritrix3.monitor.resources.H3FrontierResource;
import dk.netarkivet.heritrix3.monitor.resources.H3JobResource;
import dk.netarkivet.heritrix3.monitor.resources.H3ReportResource;
import dk.netarkivet.heritrix3.monitor.resources.H3ScriptResource;
import dk.netarkivet.heritrix3.monitor.resources.IndexResource;

public class HistoryServlet extends HttpServlet implements ResourceManagerAbstract {

    /**
     * UID
     */
    private static final long serialVersionUID = -7452707006494237017L;

    /** The logger for this class. */
    private static final Logger LOG = LoggerFactory.getLogger(HistoryServlet.class);

    public static NASEnvironment environment;

    public static PathMap<Resource> pathMap;

    protected AutoIncrement resourceAutoInc = new AutoIncrement();

    @Override
    public void init(ServletConfig servletConfig) throws ServletException {
        super.init(servletConfig);

        environment = new NASEnvironment(getServletContext(), servletConfig);
        environment.start();

        pathMap = new PathMap<Resource>();

        System.out.println(this.getClass().getClassLoader());

        IndexResource indexResource = new IndexResource();
        indexResource.resources_init(environment);
        indexResource.resources_add(this);

        ConfigResource configResource = new ConfigResource();
        configResource.resources_init(environment);
        configResource.resources_add(this);

        H3JobResource h3JobResource = new H3JobResource();
        h3JobResource.resources_init(environment);
        h3JobResource.resources_add(this);

        H3CrawlLogCachedResource h3CrawlLogCachedResource = new H3CrawlLogCachedResource();
        h3CrawlLogCachedResource.resources_init(environment);
        h3CrawlLogCachedResource.resources_add(this);

        H3FrontierResource h3FrontierResource = new H3FrontierResource();
        h3FrontierResource.resources_init(environment);
        h3FrontierResource.resources_add(this);

        H3FrontierDeleteResource h3FrontierDeleteResource = new H3FrontierDeleteResource();
        h3FrontierDeleteResource.resources_init(environment);
        h3FrontierDeleteResource.resources_add(this);

        H3ReportResource h3ReportResource = new H3ReportResource();
        h3ReportResource.resources_init(environment);
        h3ReportResource.resources_add(this);

        H3ScriptResource h3ScriptResource = new H3ScriptResource();
        h3ScriptResource.resources_init(environment);
        h3ScriptResource.resources_add(this);

        H3FilterResource h3FilterResource = new H3FilterResource();
        h3FilterResource.resources_init(environment);
        h3FilterResource.resources_add(this);

        H3BudgetResource h3BudgetResource = new H3BudgetResource();
        h3BudgetResource.resources_init(environment);
        h3BudgetResource.resources_add(this);
    }

    public int resource_add(ResourceAbstract resources, String path,
            boolean bSecured) {
        int resource_id = resourceAutoInc.getId();
        Resource resource = new Resource();
        resource.resource_id = resource_id;
        resource.resources = resources;
        resource.bSecured = bSecured;
        pathMap.add(path, resource);
        return resource_id;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.GenericServlet#destroy()
     */
    @Override
    public void destroy() {
        if (environment != null) {
            environment.cleanup();
            environment = null;
        }
        LOG.info("{} destroyed.", this.getClass().getName());
        super.destroy();
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.http.HttpServlet#service(javax.servlet.http.HttpServletRequest
     * , javax.servlet.http.HttpServletResponse)
     */
    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession();
        try {
            NASUser current_user = null;

            // If we have a valid session look for an already logged in current
            // user.
            if (session != null) {
                current_user = (NASUser) session.getAttribute("user");
            }

            // Look for cookies in case of no current user in session.
            if (current_user == null && session != null && session.isNew()) {
                // TODO
                //current_user = environment.loginHandler.loginFromCookie(req, resp, session, this);
            }

            HttpLocale httpLocale = environment.httpLocaleUtils.localeGetSet(req, resp);

            String action = req.getParameter("action");

            // Logout, login or administration.
            if (action != null && "logout".compareToIgnoreCase(action) == 0) {
                // TODO
                //environment.loginHandler.logoff(req, resp, session);
            } else {
                String pathInfo = req.getPathInfo();
                if (pathInfo == null || pathInfo.length() == 0) {
                    pathInfo = "/";
                }

                LOG.trace(req.getMethod() + " " + req.getPathInfo());

                List<Integer> numerics = new ArrayList<Integer>();
                Resource resource = pathMap.get(pathInfo, numerics);

                if (resource != null) {
                    if (resource.bSecured && current_user == null) {
                        // TODO
                        //environment.loginHandler.loginFromForm(req, resp, session, this);
                        resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, pathInfo);
                    } else {
                        resource.resources.resource_service(this.getServletContext(), current_user, req, resp, httpLocale, resource.resource_id, numerics, pathInfo);
                    }
                } else {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND, pathInfo);
                }
            }
        } catch (Throwable t) {
            LOG.error(t.toString(), t);
            StringBuilder sb = new StringBuilder();
            sb.append( "<!DOCTYPE html><html lang=\"en\"><head>" );
            sb.append( "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">" );
            sb.append( "<title>" );
            sb.append( Integer.toString( HttpServletResponse.SC_INTERNAL_SERVER_ERROR ) );
            sb.append( " Internal server error...</title>" );
            sb.append( "</head><body><h1>" );
            sb.append( Integer.toString( HttpServletResponse.SC_INTERNAL_SERVER_ERROR ) );
            sb.append( " Internal server error..." );
            sb.append( "</h1><pre>" );
            throwable_stacktrace_dump( t, sb );
            sb.append( "</pre></body></html>" );
            resp.setContentType("text/html; charset=utf-8");
            resp.setStatus( HttpServletResponse.SC_INTERNAL_SERVER_ERROR );
            OutputStream out = resp.getOutputStream();
            out.write( sb.toString().getBytes( "UTF-8" ) );
            out.flush();
            out.close();
        }
    }

    public static void stacktrace_dump(StackTraceElement[] stackTraceElementArr, StringBuilder sb) {
        StackTraceElement stackTraceElement;
        String fileName;
        if (stackTraceElementArr != null && stackTraceElementArr.length > 0) {
            for (int i=0; i<stackTraceElementArr.length; ++i) {
                stackTraceElement = stackTraceElementArr[i];
                sb.append("\tat ");
                sb.append(stackTraceElement.getClassName());
                sb.append(".");
                sb.append(stackTraceElement.getMethodName());
                sb.append("(");
                fileName = stackTraceElement.getFileName();
                if (fileName != null) {
                    sb.append(fileName);
                    sb.append(":");
                    sb.append(stackTraceElement.getLineNumber());
                } else {
                    sb.append("Unknown source");
                }
                sb.append(")");
                sb.append("\n");
            }
        }
    }

    public static void throwable_stacktrace_dump(Throwable t, StringBuilder sb) {
        String message;
        if (t != null) {
            sb.append(t.getClass().getName());
            message = t.getMessage();
            if (message != null) {
                sb.append(": ");
                sb.append(t.getMessage());
            }
            sb.append("\n");
            stacktrace_dump(t.getStackTrace(), sb);
            while ((t = t.getCause()) != null) {
                sb.append("caused by ");
                sb.append(t.getClass().getName());
                message = t.getMessage();
                if (message != null) {
                    sb.append(": ");
                    sb.append(t.getMessage());
                }
                sb.append("\n");
                stacktrace_dump(t.getStackTrace(), sb);
            }
        }
    }

    public static class Resource {

        public int resource_id;

        public ResourceAbstract resources;

        public boolean bSecured;

    }

}
