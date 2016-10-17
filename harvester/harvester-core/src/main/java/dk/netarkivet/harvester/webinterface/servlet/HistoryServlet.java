package dk.netarkivet.harvester.webinterface.servlet;

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

        pathMap = new PathMap<Resource>();

        IndexResource indexResource = new IndexResource();
        indexResource.resources_init(environment);
        indexResource.resources_add(this);

        JobResource jobResource = new JobResource();
        jobResource.resources_init(environment);
        jobResource.resources_add(this);
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
                        resource.resources.resource_service(this.getServletContext(), current_user, req, resp, resource.resource_id, numerics, pathInfo);
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
            //resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, sb.toString());
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
