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

/**
 * A class used to determine the appropriate locale/language to use for generating a HTTP response.
 * Uses the HTTP request "Accept-Language" header, locale cookie or default locale.
 * Also caches a structure of all available locale combinations in the execution environment.
 */
public class HttpLocaleHandler {

	/** <code>Map</code> of all Locale objects registered by the execution environment. */
    public final Map<String, Locale> localeMap = new HashMap<String, Locale>();

    /** Structure including the */
    public final LinkedHashMap<String, Language> languageLHM = new LinkedHashMap<String, Language>();

    /**
     * Object constructed from language configuration in the settings xml file.
     */
    public static class Language {
    	/** Locate string identifier. */
        public String language;
        /* Localized language description. */
        public String language_name;
        /** Execution environment <code>Locale</code> object retrieved from the identifier. */
        public Locale locale;
    }

    /**
     * HTTP locale object with information about the best matched locale based on Accept-Language header,
     * cookie usage and the NAS language object.
     */
    public class HttpLocale {
    	/** Did the HTTP request include a locale cookie. */
        public boolean bCookie;
        /** Language object based on NAS language configuration. */
        public Language languageObj;
        /** Best match locale object. */
        public Locale locale;

        /**
         * Generate language selection HTML based on the matched locale and cookie usage.
         * @return generated language HTML
         */
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

    /**
     * Constructor should only be used locally or in unit test.
     */
    protected HttpLocaleHandler() {
    }

    /**
     * Construct a HTTP locale handler. Reads all available locates in the execution environment
     * and in the NAS settings XML.
     * @return initialised HTTP locale handler
     */
    public static HttpLocaleHandler getInstance() {
        HttpLocaleHandler httpLocaleHandler = new HttpLocaleHandler();
        Locale[] locales = Locale.getAvailableLocales();
        Locale locale;
        String languageStr;
        String countryStr;
        for ( int i=0; i<locales.length; ++i ) {
            locale = locales[ i ];
            languageStr = locale.getLanguage();
            countryStr = locale.getCountry();
            if (countryStr != null) {
                httpLocaleHandler.localeMap.put(languageStr + '-' + countryStr, locale);
                if (!httpLocaleHandler.localeMap.containsKey(languageStr)) {
                    httpLocaleHandler.localeMap.put(languageStr, locale);
                }
            }
            else {
                httpLocaleHandler.localeMap.put(languageStr, locale);
            }
            httpLocaleHandler.localeMap.put(locale.getLanguage(), locale);
        }

        StringTree<String> webinterfaceSettings = Settings.getTree(CommonSettings.WEBINTERFACE_SETTINGS);
        Language languageObj;
        for (StringTree<String> languageSetting : webinterfaceSettings.getSubTrees(CommonSettings.WEBINTERFACE_LANGUAGE)) {
            languageObj = new Language();
            languageObj.language = languageSetting.getValue(CommonSettings.WEBINTERFACE_LANGUAGE_LOCALE);
            languageObj.language_name = languageSetting.getValue(CommonSettings.WEBINTERFACE_LANGUAGE_NAME);
            languageObj.locale = httpLocaleHandler.localeMap.get(languageObj.language);
            httpLocaleHandler.languageLHM.put(languageObj.language, languageObj);
        }

        return httpLocaleHandler;
    }

    /**
     * Determine the closest locale that matches the information in the HTTP request.
     * Depending on usage it manages a cookie.
     * If no cookie exists reads/parses the HTTP request "Accept-Language" header. 
     * 
     * @param req HTTP request object
     * @param resp HTTP response object
     * @return <code>HttpLocale</code> object determined to be the closest match to what the request wants
     */
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
