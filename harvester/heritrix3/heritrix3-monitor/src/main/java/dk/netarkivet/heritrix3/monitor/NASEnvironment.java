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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Stream;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import com.antiaction.common.templateengine.TemplateMaster;
import com.antiaction.common.templateengine.login.LoginTemplateHandler;
import com.antiaction.common.templateengine.storage.TemplateFileStorageManager;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.DomainUtils;
import dk.netarkivet.common.utils.I18n;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.harvester.Constants;
import dk.netarkivet.harvester.HarvesterSettings;
import dk.netarkivet.heritrix3.monitor.resources.H3JobResource;

public class NASEnvironment {

    private static final String NAS_GROOVY_RESOURCE_PATH = "dk/netarkivet/heritrix3/monitor/nas.groovy";

    public String NAS_GROOVY_SCRIPT;

    /** servletConfig. */
    protected ServletConfig servletConfig = null;

    public TemplateMaster templateMaster = null;

    protected String login_template_name = null;

    protected LoginTemplateHandler<NASUser> loginHandler = null;

    public File tempPath;

    public String h3AdminName;

    public String h3AdminPassword;

    public Heritrix3JobMonitorThread h3JobMonitorThread;

    public static String contextPath;

    public static String servicePath;

    public HttpLocaleHandler httpLocaleUtils;

    public static class StringMatcher {
        public String str;
        public Pattern p;
        public Matcher m;
    }

    public List<StringMatcher> h3HostPortAllowRegexList = new ArrayList<StringMatcher>();

    public final I18n I18N = new I18n(Constants.TRANSLATIONS_BUNDLE);

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

        this.servletConfig = theServletConfig;
        h3JobMonitorThread = new Heritrix3JobMonitorThread(this);
        writeDiagnostics("Initialized " + this.getClass().getName());
    }

    public void start() {
        try {
        	h3JobMonitorThread.init();
            h3JobMonitorThread.start();
        }
        catch (Throwable t) {
        	t.printStackTrace();
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
        String crawlLogPath = h3Job.logFile.getAbsolutePath();
        writeDiagnostics("Trying to getCrawledUrls from job " + jobId + " using cached crawllog '" + crawlLogPath + "'");
        long cachedLines = h3Job.totalCachedLines;

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
