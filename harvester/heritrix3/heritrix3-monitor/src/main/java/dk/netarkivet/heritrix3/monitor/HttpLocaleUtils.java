package dk.netarkivet.heritrix3.monitor;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.StringTree;
import dk.netarkivet.heritrix3.monitor.AcceptLanguageParser.AcceptLanguage;

public class HttpLocaleUtils {

    public final Map<String, Locale> localeMap = new HashMap<String, Locale>();

    public final LinkedHashMap<String, Language> languageLHM = new LinkedHashMap<String, Language>();

    public static class Language {
        public String language;
        public String language_name;
        public Locale locale;
    }

    public class HttpLocale {
        public boolean bCookie;
        public Language languageObj;
        public Locale locale;

        public String generateLanguageLinks() {
            String languageStr = languageObj.locale.getLanguage();
            StringBuilder sb = new StringBuilder();
            sb.append("<div class=\"languagelinks\">");
            Iterator<Language> languageIter = languageLHM.values().iterator();
            Language language;
            while (languageIter.hasNext()) {
                language = languageIter.next();
                if (languageStr.equalsIgnoreCase(language.language)) {
                    sb.append("<a href=\"");
                    if (bCookie) {
                        sb.append("#");
                    }
                    else {
                        sb.append("?locale=");
                        sb.append(language.language);
                    }
                    sb.append("\">");
                    sb.append("<b>");
                    sb.append(language.language_name);
                    sb.append("</b>");
                    sb.append("</a>");
                    sb.append("&nbsp;");
                } else {
                    sb.append("<a href=\"");
                    sb.append("?locale=");
                    sb.append(language.language);
                    sb.append("\">");
                    sb.append(language.language_name);
                    sb.append("</a>&nbsp;");
                }
            }
            if (bCookie) {
                sb.append("<a href=\"?locale=");
                sb.append("\">");
                sb.append("(Remove cookie)");
                sb.append("</a>&nbsp;");
            }
            sb.append("</div>");
            return sb.toString();
        }
    }

    public HttpLocaleUtils() {
    }

    public static HttpLocaleUtils getInstance() {
        HttpLocaleUtils httpLocaleUtils = new HttpLocaleUtils();
        Locale[] locales = Locale.getAvailableLocales();
        Locale locale;
        String languageStr;
        String countryStr;
        for ( int i=0; i<locales.length; ++i ) {
            locale = locales[ i ];
            languageStr = locale.getLanguage();
            countryStr = locale.getCountry();
            if (countryStr != null) {
                httpLocaleUtils.localeMap.put(languageStr + '-' + countryStr, locale);
                if (!httpLocaleUtils.localeMap.containsKey(languageStr)) {
                    httpLocaleUtils.localeMap.put(languageStr, locale);
                }
            }
            else {
                httpLocaleUtils.localeMap.put(languageStr, locale);
            }
            httpLocaleUtils.localeMap.put(locale.getLanguage(), locale);
        }

        StringTree<String> webinterfaceSettings = Settings.getTree(CommonSettings.WEBINTERFACE_SETTINGS);
        Language languageObj;
        for (StringTree<String> languageSetting : webinterfaceSettings.getSubTrees(CommonSettings.WEBINTERFACE_LANGUAGE)) {
            languageObj = new Language();
            languageObj.language = languageSetting.getValue(CommonSettings.WEBINTERFACE_LANGUAGE_LOCALE);
            languageObj.language_name = languageSetting.getValue(CommonSettings.WEBINTERFACE_LANGUAGE_NAME);
            languageObj.locale = httpLocaleUtils.localeMap.get(languageObj.language);
            httpLocaleUtils.languageLHM.put(languageObj.language, languageObj);
        }

        return httpLocaleUtils;
    }

    public HttpLocale localeGetSet(HttpServletRequest req, HttpServletResponse resp) {
        HttpLocale httpLocale = new HttpLocale();
        boolean bCookieDeleted = false;
        // Request parameter.
        String languageStr = req.getParameter("locale");
        Language languageObj = null;
        if (languageStr != null) {
            if (languageStr.length() > 0) {
                languageObj = languageLHM.get(languageStr);
                if (languageObj != null) {
                    Cookie cookie = new Cookie("locale", languageStr);
                    cookie.setPath("/");
                    //Keep the cookie for a year
                    cookie.setMaxAge(365 * 24 * 60 * 60);
                    resp.addCookie(cookie);
                    httpLocale.bCookie = true;
                }
            }
            else {
                Cookie cookie = new Cookie("locale", languageStr);
                cookie.setPath("/");
                cookie.setMaxAge(0);
                resp.addCookie(cookie);
                bCookieDeleted = true;
            }
        }
        // Cookie.
        if (languageObj == null && !bCookieDeleted) {
            Cookie[] cookies = req.getCookies();
            if (cookies != null) {
                for (Cookie c : cookies) {
                    if (c.getName().equals("locale")) {
                        languageStr = c.getValue();
                        languageObj = languageLHM.get(languageStr);
                        httpLocale.bCookie = true;
                    }
                }
            }
        }
        // Accept-Language.
        if (languageObj == null) {
            List<AcceptLanguage> acceptLanguages = AcceptLanguageParser.parseHeader(req);
            AcceptLanguage acceptLanguage;
            int idx = 0;
            int len = acceptLanguages.size();
            while (idx < len && languageObj == null) {
                acceptLanguage = acceptLanguages.get(idx);
                languageObj = languageLHM.get(acceptLanguage.locale);
                if (languageObj == null) {
                    languageObj = languageLHM.get(acceptLanguage.language);
                }
                ++idx;
            }
        }
        // Fall back to default.
        if (languageObj == null) {
            languageObj = languageLHM.get("en");
        }
        // Locale.
        httpLocale.languageObj = languageObj;
        if (languageObj != null) {
            httpLocale.locale = languageObj.locale;
            resp.setLocale(httpLocale.locale);
        }
        else {
            httpLocale.locale = resp.getLocale();
        }
        return httpLocale;
    }

}
