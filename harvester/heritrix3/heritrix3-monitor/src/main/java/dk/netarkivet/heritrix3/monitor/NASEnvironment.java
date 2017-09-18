package dk.netarkivet.heritrix3.monitor;

import java.io.File;
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
import dk.netarkivet.common.utils.I18n;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.StringTree;
import dk.netarkivet.harvester.Constants;
import dk.netarkivet.harvester.HarvesterSettings;

public class NASEnvironment {

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

}
