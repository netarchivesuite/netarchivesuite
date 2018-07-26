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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Stream;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.antiaction.common.templateengine.TemplateMaster;
import com.antiaction.common.templateengine.login.LoginTemplateHandler;
import com.antiaction.common.templateengine.storage.TemplateFileStorageManager;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.DomainUtils;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.harvester.HarvesterSettings;
import dk.netarkivet.heritrix3.monitor.resources.H3JobResource;
import dk.netarkivet.heritrix3.monitor.resources.MasterTemplateBuilder;

/**
 * This class contains all the configuration and common objects used by the HistoryServlet and all of the resource pages. 
 * It is constructed when the HistoryServlet is first constructed.
 */
public class NASEnvironment {

    /** The logger for this class. */
    private static final Logger LOG = LoggerFactory.getLogger(NASEnvironment.class);

    /** Location of the Groovy script used by some of the resources. */
    private static final String NAS_GROOVY_RESOURCE_PATH = "dk/netarkivet/heritrix3/monitor/nas.groovy";

    /** Cached Groovy script. */
    public String NAS_GROOVY_SCRIPT;

    /** servletConfig. */
    protected ServletConfig servletConfig = null;

    /** Template master used to generate HTML. */
    public TemplateMaster templateMaster = null;

    /** Template file used on the login page. */
    protected String login_template_name = null;

    /** Login template handler used by the login mechanism. */
    protected LoginTemplateHandler<NASUser> loginHandler = null;

    /** Temporary path where cached crawllog and frontier dump files are stored. */
    public File tempPath;

    /** Remote H3 admin user. */
    public String h3AdminName;

    /** Remote H3 admin password. */
    public String h3AdminPassword;

    /** */
    public NASJobWrapper nasJobWrapper;

    /** Reference to running H3 job monitor thread. */
    public Heritrix3JobMonitorThread h3JobMonitorThread;

    /** Context path where the History Servlet is running. */
    public static String contextPath;

    /** Full path (servlet-mapping) to the History Servlet. */
    public static String servicePath;

    /** Utility class to handle HTTP locale stuff and HTML generation. */
    public HttpLocaleHandler httpLocaleUtils;

    /** Cache crawllog when the frontierqueue gets above this threshold. (H3 monitor) */ 
    public long cacheCrawllogFrontierQueueThreshold = 0;

    /** Pause job when frontierqueue gets above this threshold. (H3 monitor) */
    public long pauseFrontierQueueThreshold = 0;

    /** Dump frontierqueue after it is paused. (H3 monitor) */
    public boolean bFrontierQueueDump = false;

    /** Cache frontierqueue after is has been dumped. (H3 monitor) */
    public boolean bFrontierQueueCache = false;

    public static class StringMatcher {
        public String str;
        public Pattern p;
        public Matcher m;
    }

    public final List<StringMatcher> h3HostPortAllowRegexList = new ArrayList<StringMatcher>();

    /**
     * 
     * @param resource path to the requested resource
     * @return resource read in to a String
     * @throws IOException if an I/O exception occurs while reading the resource
     */
    public String getResourceAsString(String resource) throws IOException {
        InputStream in = H3JobResource.class.getClassLoader().getResourceAsStream(resource);
        ByteArrayOutputStream bOut = new ByteArrayOutputStream();
        byte[] tmpArr = new byte[8192];
        int read;
        while ((read = in.read(tmpArr)) != -1) {
            bOut.write(tmpArr, 0, read);
        }
        in.close();
        return new String(bOut.toByteArray(), "UTF-8");
    }

    /**
     * Get a localized message for a given locale and label, and optionally arguments.
     * @param locale The locale to get the string for
     * @param label The label of the string in the resource bundle
     * @param args Any args required for formatting the label
     * @return The localised string, or the label if the string could not be found or the format is invalid or does not match the args.
     * @throws ArgumentNotValid on null or empty local or label.
     */
    public String getString(Locale locale, String label, Object... args) {
        return nasJobWrapper.getString(locale, label, args);
    }

    public NASEnvironment(ServletContext servletContext, ServletConfig theServletConfig) throws ServletException {
    	httpLocaleUtils = HttpLocaleHandler.getInstance();

        try {
            NAS_GROOVY_SCRIPT = getResourceAsString(NAS_GROOVY_RESOURCE_PATH);
        } catch (IOException e) {
        	throw new ServletException("Resource missing: " + NAS_GROOVY_RESOURCE_PATH);
        }

        login_template_name = "login.html";

        templateMaster = TemplateMaster.getInstance("default");
        templateMaster.addTemplateStorage(TemplateFileStorageManager.getInstance(
                        servletContext.getRealPath("/"), "UTF-8"));

        loginHandler = new LoginTemplateHandler<NASUser>();
        loginHandler.templateMaster = templateMaster;
        loginHandler.templateName = login_template_name;
        loginHandler.title = "Webdanica - Login";
        loginHandler.adminPath = "/";

        try {
            tempPath = Settings.getFile(HarvesterSettings.HERITRIX3_MONITOR_TEMP_PATH);
            writeDiagnostics("Trying to use tempPath '" + tempPath.getAbsolutePath() + "' as read from setting: " +
                    HarvesterSettings.HERITRIX3_MONITOR_TEMP_PATH);
            if (!tempPath.isDirectory()) { // Try to create tempPath if it doesn't exist already
                tempPath.mkdirs();
            }
        } catch (Exception e) {
            //This is normal if tempPath is unset, so system directory is used.
            tempPath = new File(System.getProperty("java.io.tmpdir"));
        }
        // Fallback to System.getProperty("java.io.tmpdir")
        if (tempPath == null || !tempPath.isDirectory()) {
            tempPath = new File(System.getProperty("java.io.tmpdir"));
        }
        writeDiagnostics("Using dir '" + tempPath.getAbsolutePath() + "' as tempPath");
        h3AdminName = Settings.get(HarvesterSettings.HERITRIX_ADMIN_NAME);
        h3AdminPassword = Settings.get(HarvesterSettings.HERITRIX_ADMIN_PASSWORD);

        cacheCrawllogFrontierQueueThreshold = Settings.getLong(HarvesterSettings.HERITRIX3_MONITOR_CACHE_CRAWLLOG_FRONTIERQUEUE_THRESHOLD);
        pauseFrontierQueueThreshold = Settings.getLong(HarvesterSettings.HERITRIX3_MONITOR_PAUSE_FRONTIERQUEUE_THRESHOLD);
        bFrontierQueueDump = Settings.getBoolean(HarvesterSettings.HERITRIX3_MONITOR_DUMP_FRONTIERQUEUE);
        bFrontierQueueCache = Settings.getBoolean(HarvesterSettings.HERITRIX3_MONITOR_CACHE_FRONTIERQUEUE);

        nasJobWrapper = new NASJobWrapper();

        MasterTemplateBuilder.versionString = nasJobWrapper.versionString;
        MasterTemplateBuilder.environmentName = nasJobWrapper.environmentName;

        this.servletConfig = theServletConfig;
        h3JobMonitorThread = new Heritrix3JobMonitorThread(this);
        writeDiagnostics("Initialized " + this.getClass().getName());
    }

    /**
     * Initialise and start the H3 job monitor background thread.
     */
    public void start() {
        try {
        	h3JobMonitorThread.init();
            h3JobMonitorThread.start();
        }
        catch (Throwable t) {
        	LOG.error("H3 monitor thread could not be start!", t);
        }
    }
    
    private synchronized void writeDiagnostics(String logEntry) {
        File logFile = new File(tempPath, "h3monitor.log");
        String dateStamp= "[" + new Date() + "] ";
        try (FileWriter logFileWriter = new FileWriter(logFile, true);) {
            logFileWriter.write(dateStamp);
            logFileWriter.write(logEntry);
            logFileWriter.write(System.lineSeparator());
            logFileWriter.flush();
            logFileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Do some cleanup. This waits for the different workflow threads to stop running.
     */
    public void cleanup() {
        servletConfig = null;
    }

    public void replaceH3HostnamePortRegexList(List<String> h3HostnamePortRegexList, List<String> invalidPatternsList) {
        String regex;
        StringMatcher stringMatcher;
        synchronized (h3HostPortAllowRegexList) {
            h3HostPortAllowRegexList.clear();
            for (int i=0; i<h3HostnamePortRegexList.size(); ++i) {
                regex = h3HostnamePortRegexList.get(i);
                try {
                    stringMatcher = new StringMatcher();
                    stringMatcher.str = regex;
                    stringMatcher.p = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
                    stringMatcher.m = stringMatcher.p.matcher("42");
                    h3HostPortAllowRegexList.add(stringMatcher);
                } catch (PatternSyntaxException e) {
                    invalidPatternsList.add(regex);
                }
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

    /**
     * Determine whether URL in given crawllog line is attempted harvested
     * @param crawllogLine Line from the crawllog under consideration
     * @return whether the given crawllog line contains an URL that is attempted harvested
     */
    private boolean urlInLineIsAttemptedHarvested(String crawllogLine) {
        String[] columns = crawllogLine.split("\\s+");
        if (columns.length < 4) {
            return false;
        }
        String fetchStatusCode = columns[1];
        String harvestedUrl = columns[3];

        // Do not include URLs with a negative Fetch Status Code (coz they're not even attempted crawled)
        if (Integer.parseInt(fetchStatusCode) < 0) {
            return false;
        }

        // Do not include dns-look-ups from the crawllog
        if (harvestedUrl.startsWith("dns:")) {
            return false;
        }

        return true;
    }

    /**
     * Get the (attempted) crawled URLs of the crawllog for the running job with the given job id
     *
     * @param jobId Id of the running job
     * @param h3Job Heritrix3JobMonitor from which to get the job for the given jobId
     * @return The (attempted) crawled URLs of the crawllog for given job
     */
    public Stream<String> getCrawledUrls(long jobId, Heritrix3JobMonitor h3Job) {
        if (h3Job == null) {
            h3Job = h3JobMonitorThread.getRunningH3Job(jobId);
            if (h3Job == null) {
                // There were no running jobs
                return Stream.empty();
            }
        }
        String crawlLogPath = h3Job.indexedCrawllog.textFile.getAbsolutePath();
        writeDiagnostics("Trying to getCrawledUrls from job " + jobId + " using cached crawllog '" + crawlLogPath + "'");
        long cachedLines = h3Job.indexedCrawllog.indexedTextLines;

        if (cachedLines == 0) {
            writeDiagnostics("No cached crawllog-lines for job " + jobId);
            return Stream.empty();
        } else {
            writeDiagnostics("Number of cached crawllog-lines for job " + jobId + ": " +  cachedLines);
        }
        try {
            // test that the crawllog-exists if not, return an empty stream 
            if (!Paths.get(crawlLogPath).toFile().isFile()) {
                writeDiagnostics("The file '" + crawlLogPath + "' doesn't correspond to a file. returning an empty stream");
                return Stream.empty();
            }
            Stream<String> attemptedHarvestedUrlsFromCrawllog = Files.lines(Paths.get(crawlLogPath),
                    Charset.forName("UTF-8"))
                    .filter(line -> urlInLineIsAttemptedHarvested(line))
                    .map(line -> line.split("\\s+")[3]);

            return attemptedHarvestedUrlsFromCrawllog;
        } catch (java.io.IOException e) {
            throw new IOFailure("Could not open crawllog file", e);
        }
    }

    /**
     * Normalizes input URL so that only the domain part remains.
     *
     * @param url URL intended to be stripped to it's domain part
     * @return The domain part of the input URL
     * @throws ArgumentNotValid if URL was malformed
     */
    private String normalizeDomainUrl(String url) {
        if (!url.toLowerCase().matches("^\\w+://.*")) {
            // URL has no protocol part, so let's add one
            url = "http://" + url;
        }
        URL domainUrl;
        try {
            domainUrl = new URL(url);
        } catch (MalformedURLException e) {
            return "";
        }
        String domainHost = domainUrl.getHost();
        String normalizedDomainUrl = DomainUtils.domainNameFromHostname(domainHost);
        if (normalizedDomainUrl == null) {
            // Invalid domain
            throw new ArgumentNotValid(url + " is not a valid domain name.");
        }
        return normalizedDomainUrl;
    }

    /**
     * Find out whether the given job harvests given domain.
     *
     * @param jobId The job
     * @param domainName The domain
     * @return whether the given job harvests given domain
     */
    public boolean jobHarvestsDomain(long jobId, String domainName, Heritrix3JobMonitor h3Job) {
        // Normalize search URL
        String searchedDomain = normalizeDomainUrl(domainName);

        // Return whether or not the crawled URLs contain the searched URL
        return getCrawledUrls(jobId, h3Job)
                .map(url -> normalizeDomainUrl(url))
                .anyMatch(url -> searchedDomain.equalsIgnoreCase(url));
    }

}
