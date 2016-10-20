package dk.netarkivet.harvester.webinterface.servlet;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import com.antiaction.common.templateengine.TemplateMaster;
import com.antiaction.common.templateengine.login.LoginTemplateHandler;
import com.antiaction.common.templateengine.storage.TemplateFileStorageManager;

public class NASEnvironment {

    /** servletConfig. */
    protected ServletConfig servletConfig = null;

    protected TemplateMaster templateMaster = null;

    protected String login_template_name = null;

    protected LoginTemplateHandler<NASUser> loginHandler = null;

    protected Heritrix3JobMonitorThread h3JobMonitorThread;

    public static String contextPath;

    public static String servicePath;

    public NASEnvironment(ServletContext servletContext, ServletConfig theServletConfig) throws ServletException {
        login_template_name = "login.html";

        templateMaster = TemplateMaster.getInstance("default");
        templateMaster.addTemplateStorage(TemplateFileStorageManager.getInstance(servletContext.getRealPath("/"), "UTF-8"));

        loginHandler = new LoginTemplateHandler<NASUser>();
        loginHandler.templateMaster = templateMaster;
        loginHandler.templateName = login_template_name;
        loginHandler.title = "Webdanica - Login";
        loginHandler.adminPath = "/";

        this.servletConfig = theServletConfig;
        h3JobMonitorThread = new Heritrix3JobMonitorThread();
        h3JobMonitorThread.start();
    }

    /**
     * Do some cleanup. This waits for the different workflow threads to stop running.
     */
    public void cleanup() {
        servletConfig = null;
    }

}
