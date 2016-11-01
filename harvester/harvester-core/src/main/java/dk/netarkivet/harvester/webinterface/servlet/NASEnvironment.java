package dk.netarkivet.harvester.webinterface.servlet;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import com.antiaction.common.templateengine.TemplateMaster;
import com.antiaction.common.templateengine.login.LoginTemplateHandler;
import com.antiaction.common.templateengine.storage.TemplateFileStorageManager;

import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.harvester.HarvesterSettings;

public class NASEnvironment {

    /** servletConfig. */
    protected ServletConfig servletConfig = null;

    protected TemplateMaster templateMaster = null;

    protected String login_template_name = null;

    protected LoginTemplateHandler<NASUser> loginHandler = null;

    public File tempPath;

    public String h3AdminName;

    public String h3AdminPassword;

    protected Heritrix3JobMonitorThread h3JobMonitorThread;

    public static String contextPath;

    public static String servicePath;

    public static class StringMatcher {
        public String str;
        public Pattern p;
        public Matcher m;
    }

    public List<StringMatcher> h3HostPortAllowRegexList = new ArrayList<StringMatcher>();

    public NASEnvironment(ServletContext servletContext, ServletConfig theServletConfig) throws ServletException {
        login_template_name = "login.html";

        templateMaster = TemplateMaster.getInstance("default");
        templateMaster.addTemplateStorage(TemplateFileStorageManager.getInstance(servletContext.getRealPath("/"), "UTF-8"));

        loginHandler = new LoginTemplateHandler<NASUser>();
        loginHandler.templateMaster = templateMaster;
        loginHandler.templateName = login_template_name;
        loginHandler.title = "Webdanica - Login";
        loginHandler.adminPath = "/";

        try {
            tempPath = Settings.getFile(HarvesterSettings.HERITRIX3_MONITOR_TEMP_PATH);
        } catch (Exception e) {
            //This is normal if tempPath is unset, so system directory is used.
            tempPath = new File(System.getProperty("java.io.tmpdir"));
        }
        if (tempPath == null || !tempPath.isDirectory() || !tempPath.isDirectory()) {
            tempPath = new File(System.getProperty("java.io.tmpdir"));
        }

        h3AdminName = Settings.get(HarvesterSettings.HERITRIX_ADMIN_NAME);
        h3AdminPassword = Settings.get(HarvesterSettings.HERITRIX_ADMIN_PASSWORD);

        this.servletConfig = theServletConfig;
        h3JobMonitorThread = new Heritrix3JobMonitorThread();
    }

    public void start() {
        h3JobMonitorThread.start();
    }

    /**
     * Do some cleanup. This waits for the different workflow threads to stop running.
     */
    public void cleanup() {
        servletConfig = null;
    }

    public void replaceH3HostnamePortRegexList(List<String> h3HostnamePortRegexList) {
        String regex;
        StringMatcher stringMatcher;
        synchronized (h3HostPortAllowRegexList) {
            h3HostPortAllowRegexList.clear();
            for (int i=0; i<h3HostnamePortRegexList.size(); ++i) {
                regex = h3HostnamePortRegexList.get(i);
                stringMatcher = new StringMatcher();
                stringMatcher.str = regex;
                stringMatcher.p = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
                stringMatcher.m = stringMatcher.p.matcher("42");
                h3HostPortAllowRegexList.add(stringMatcher);
            }
        }
    }

    public boolean isH3HostnamePortEnabled(String h3HostnamePort) {
        boolean bAllowed = false;
        synchronized (h3HostPortAllowRegexList) {
            StringMatcher stringMatcher;
            int idx = 0;
            while (!bAllowed && idx < h3HostPortAllowRegexList.size()) {
                stringMatcher = h3HostPortAllowRegexList.get(idx++);
                stringMatcher.m.reset(h3HostnamePort);
                bAllowed = stringMatcher.m.matches();
            }
        }
        return bAllowed;
    }

}
