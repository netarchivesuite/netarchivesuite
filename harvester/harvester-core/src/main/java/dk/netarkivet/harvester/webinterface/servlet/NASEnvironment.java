package dk.netarkivet.harvester.webinterface.servlet;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import com.antiaction.common.templateengine.TemplateMaster;
import com.antiaction.common.templateengine.login.LoginTemplateHandler;
import com.antiaction.common.templateengine.storage.TemplateFileStorageManager;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.DomainUtils;
import dk.netarkivet.common.utils.I18n;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.StringTree;
import dk.netarkivet.harvester.Constants;
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

    public final I18n I18N = new I18n(Constants.TRANSLATIONS_BUNDLE);

    public final LinkedHashMap<String, Language> laguangeLHM = new LinkedHashMap<String, Language>();

    public static class Language {
        String language;
        String language_name;
        Locale locale;
    }

    public NASEnvironment(ServletContext servletContext, ServletConfig theServletConfig) throws ServletException {
        Map<String, Locale> localeMap = new HashMap<String, Locale>();
        Locale[] locales = Locale.getAvailableLocales();
        Locale locale;
        String languageStr;
        String countryStr;
        for ( int i=0; i<locales.length; ++i ) {
            locale = locales[ i ];
            languageStr = locale.getLanguage();
            countryStr = locale.getCountry();
            if (countryStr != null) {
                localeMap.put(languageStr + '_' + countryStr, locale);
                if (!localeMap.containsKey(languageStr)) {
                    localeMap.put(languageStr, locale);
                }
            }
            else {
                localeMap.put(languageStr, locale);
            }
            localeMap.put(locale.getLanguage(), locale);
        }

        StringTree<String> webinterfaceSettings = Settings.getTree(CommonSettings.WEBINTERFACE_SETTINGS);
        Language languageObj;
        for (StringTree<String> languageSetting : webinterfaceSettings.getSubTrees(CommonSettings.WEBINTERFACE_LANGUAGE)) {
            languageObj = new Language();
            languageObj.language = languageSetting.getValue(CommonSettings.WEBINTERFACE_LANGUAGE_LOCALE);
            languageObj.language_name = languageSetting.getValue(CommonSettings.WEBINTERFACE_LANGUAGE_NAME);
            languageObj.locale = localeMap.get(languageObj.language);
            laguangeLHM.put(languageObj.language, languageObj);
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
        h3JobMonitorThread = new Heritrix3JobMonitorThread(this);
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

    public String generateLanguageLinks(Locale locale) {
        String languageStr = locale.getLanguage();
        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"languagelinks\">");
        Iterator<Language> languageIter = laguangeLHM.values().iterator();
        Language language;
        while (languageIter.hasNext()) {
            language = languageIter.next();
            if (languageStr.equalsIgnoreCase(language.language)) {
                sb.append("<a href=\"#\">");
                sb.append("<b>");
                sb.append(language.language_name);
                sb.append("</b>");
                sb.append("</a>&nbsp;");
            } else {
                sb.append("<a href=\"?locale=");
                sb.append(language.language);
                sb.append("\">");
                sb.append(language.language_name);
                sb.append("</a>&nbsp;");
            }
        }
        sb.append("</div>");
        return sb.toString();
    }

    /**
     * Get the (attempted) crawled URLs of the crawllog for the running job with the given job id
     *
     * @param jobId Id of the running job
     * @return The (attempted) crawled URLs of the crawllog for given job
     */
    public List<String> getCrawledUrls(long jobId, Heritrix3JobMonitor h3Job) {
        if (h3Job == null) {
            h3Job = h3JobMonitorThread.getRunningH3Job(jobId);
        }
        String crawlLogPath = h3Job.crawlLogFilePath;

        List<String> crawledUrls = new ArrayList<>();
        try (
                InputStream fis = new FileInputStream(crawlLogPath);
                InputStreamReader isr = new InputStreamReader(fis, Charset.forName("UTF-8"));
                BufferedReader br = new BufferedReader(isr)
        ) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] columns = line.split("\\s+");
                if (columns.length < 4) {
                    continue;
                }
                String fetchStatusCode = columns[1];
                String harvestedUrl = columns[3];

                // Do not include URLs with a negative Fetch Status Code (coz they're not even attempted crawled)
                if (Integer.parseInt(fetchStatusCode) < 0) {
                    continue;
                }

                // Do not include dns-look-ups from the crawllog
                if (harvestedUrl.startsWith("dns:")) {
                    continue;
                }

                crawledUrls.add(harvestedUrl);
            }
        } catch (java.io.IOException e) {
            throw new IOFailure("Could not open crawllog file", e);
        }
        return crawledUrls;
    }

    /**
     * Normalizes input URL so that only the domain part remains.
     *
     * @param url URL intended to be stripped to it's domain part
     * @return The domain part of the input URL, or "" if URL was malformed
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
        return DomainUtils.domainNameFromHostname(domainHost);
    }

    /**
     * Find out whether the given job harvests given domain.
     *
     * @param jobId The job
     * @param domainName The domain
     * @return whether the given job harvests given domain
     */
    public boolean jobHarvestsDomain(long jobId, String domainName) {
        List<String> crawledUrls = getCrawledUrls(jobId, null);

        // Normalize search URL
        String searchedDomain = normalizeDomainUrl(domainName);

        for (String crawledUrl : crawledUrls) {
            // Normalize crawled URL
            String crawledDomain = normalizeDomainUrl(crawledUrl);

            if (searchedDomain.equalsIgnoreCase(crawledDomain)) {
                return true;
            }
        }
        return false;
    }

}
