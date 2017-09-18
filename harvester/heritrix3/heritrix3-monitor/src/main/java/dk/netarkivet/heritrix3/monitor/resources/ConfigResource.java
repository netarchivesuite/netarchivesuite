package dk.netarkivet.heritrix3.monitor.resources;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.antiaction.common.filter.Caching;
import com.antiaction.common.templateengine.TemplateBuilderFactory;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.Constants;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.heritrix3.monitor.NASEnvironment;
import dk.netarkivet.heritrix3.monitor.NASEnvironment.StringMatcher;
import dk.netarkivet.heritrix3.monitor.NASUser;
import dk.netarkivet.heritrix3.monitor.ResourceAbstract;
import dk.netarkivet.heritrix3.monitor.ResourceManagerAbstract;

public class ConfigResource implements ResourceAbstract {

    private NASEnvironment environment;

    protected int R_CONFIG = -1;

    @Override
    public void resources_init(NASEnvironment environment) {
        this.environment = environment;
    }

    @Override
    public void resources_add(ResourceManagerAbstract resourceManager) {
        R_CONFIG = resourceManager.resource_add(this, "/config/", false);
    }

    @Override
    public void resource_service(ServletContext servletContext, NASUser nas_user, HttpServletRequest req, HttpServletResponse resp, int resource_id, List<Integer> numerics, String pathInfo) throws IOException {
        if (NASEnvironment.contextPath == null) {
            NASEnvironment.contextPath = req.getContextPath();
        }
        if (NASEnvironment.servicePath == null) {
            NASEnvironment.servicePath = req.getContextPath() + req.getServletPath() + "/";
        }
        String method = req.getMethod().toUpperCase();
        if (resource_id == R_CONFIG) {
            if ("GET".equals(method) || "POST".equals(method)) {
                config(req, resp, numerics);
            }
        }
    }

    public void config(HttpServletRequest req, HttpServletResponse resp, List<Integer> numerics) throws IOException {
        Locale locale = resp.getLocale();
        resp.setContentType("text/html; charset=UTF-8");
        ServletOutputStream out = resp.getOutputStream();
        Caching.caching_disable_headers(resp);

        TemplateBuilderFactory<ConfigTemplateBuilder> configTplBuilderFactory = TemplateBuilderFactory.getInstance(environment.templateMaster, "h3config.tpl", "UTF-8", ConfigTemplateBuilder.class);
        ConfigTemplateBuilder configTplBuilder = configTplBuilderFactory.getTemplateBuilder();

        StringBuilder sb = new StringBuilder();
        StringBuilder enabledhostsSb = new StringBuilder();

        String method = req.getMethod().toUpperCase();
        if ("POST".equals(method)) {
            String enabledhostsStr = req.getParameter("enabledhosts");
            String tmpStr;
            if (enabledhostsStr != null) {
                BufferedReader reader = new BufferedReader(new StringReader(enabledhostsStr));
                List<String> enabledhostsList = new LinkedList<String>();
                while ((tmpStr = reader.readLine()) != null) {
                    enabledhostsList.add(tmpStr);
                }
                reader.close();
                environment.replaceH3HostnamePortRegexList(enabledhostsList);
                environment.h3JobMonitorThread.updateH3HostnamePortFilter();
            }
        }

        synchronized (environment.h3HostPortAllowRegexList) {
            StringMatcher stringMatcher;
            for (int i=0; i<environment.h3HostPortAllowRegexList.size(); ++i) {
                stringMatcher = environment.h3HostPortAllowRegexList.get(i);
                enabledhostsSb.append(stringMatcher.str);
                enabledhostsSb.append("\n");
            }
        }

        synchronized (environment.h3JobMonitorThread.h3HostnamePortEnabledList) {
            sb.append("<h5>");
            sb.append(environment.I18N.getString(locale, "running.jobs.monitor.crawllog.cache.enabled.for"));
            sb.append(":</h5>\n");
            if (environment.h3JobMonitorThread.h3HostnamePortEnabledList.size() > 0) {
                for (int i=0; i<environment.h3JobMonitorThread.h3HostnamePortEnabledList.size(); ++i) {
                    sb.append(environment.h3JobMonitorThread.h3HostnamePortEnabledList.get(i));
                    sb.append("<br />\n");
                }
            } else {
                sb.append("<p>");
                sb.append(environment.I18N.getString(locale, "running.jobs.monitor.no.hosts.enabled"));
                sb.append("</p>\n");
            }
        }
        sb.append("<br />\n");
        synchronized (environment.h3JobMonitorThread.h3HostnamePortDisabledList) {
            sb.append("<h5>");
            sb.append(environment.I18N.getString(locale, "running.jobs.monitor.crawllog.cache.disabled.for"));
            sb.append("</h5>\n");
            if (environment.h3JobMonitorThread.h3HostnamePortDisabledList.size() > 0) {
                for (int i=0; i<environment.h3JobMonitorThread.h3HostnamePortDisabledList.size(); ++i) {
                    sb.append(environment.h3JobMonitorThread.h3HostnamePortDisabledList.get(i));
                    sb.append("<br />\n");
                }
            } else {
                sb.append("<p>");
                sb.append(environment.I18N.getString(locale, "running.jobs.monitor.no.hosts.disable"));
                sb.append("</p>\n");
            }
        }

        if (configTplBuilder.titlePlace != null) {
            configTplBuilder.titlePlace.setText("H3 Remote Access Config");
        }
        if (configTplBuilder.languagesPlace != null) {
            configTplBuilder.languagesPlace.setText(environment.generateLanguageLinks(locale));
        }
        if (configTplBuilder.headingPlace != null) {
            configTplBuilder.headingPlace.setText("H3 Remote Access Config");
        }
        if (configTplBuilder.enabledhostsPlace != null) {
            configTplBuilder.enabledhostsPlace.setText(enabledhostsSb.toString());
        }
        if (configTplBuilder.contentPlace != null) {
            configTplBuilder.contentPlace.setText(sb.toString());
        }
        if (configTplBuilder.versionPlace != null) {
            configTplBuilder.versionPlace.setText(Constants.getVersionString(true));
        }
        if (configTplBuilder.environmentPlace != null) {
            configTplBuilder.environmentPlace.setText(Settings.get(CommonSettings.ENVIRONMENT_NAME));
        }

        configTplBuilder.write(out);

        out.flush();
        out.close();
    }

}
