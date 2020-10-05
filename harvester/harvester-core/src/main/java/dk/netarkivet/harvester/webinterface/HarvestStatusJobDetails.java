package dk.netarkivet.harvester.webinterface;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.ForwardedToErrorPage;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.common.utils.I18n;
import dk.netarkivet.common.utils.StringUtils;
import dk.netarkivet.common.webinterface.HTMLUtils;
import dk.netarkivet.common.webinterface.SiteSection;
import dk.netarkivet.harvester.datamodel.DomainDAO;
import dk.netarkivet.harvester.datamodel.HarvestDefinitionDAO;
import dk.netarkivet.harvester.datamodel.HarvestInfo;
import dk.netarkivet.harvester.datamodel.Job;
import dk.netarkivet.harvester.datamodel.JobDAO;
import dk.netarkivet.harvester.datamodel.JobStatus;

/**
 * Created by jve on 5/31/16.
 */
public class HarvestStatusJobDetails {
    /** The job id for job the extracted from query parameter. */
    private long jobID;
    /** The job for the current page. */
    private Job job;
    /** Name of the havest. */
    private String harvestname;
    /** Url of the havest. */
    private String harvesturl;
    /** Url to job details. */
    private String jobdetailsUrl;
    /** Havest name for td content. */
    private String harvestnameTdContents;
    /** Job status td content. */
    private String jobstatusTdContents;
    /** Indicate weather definition sitesection is deployed. */
    private boolean definitionsSitesectionDeployed;
    /** Id of the havest */
    private Long harvestID;

    /**
     * Constuctor for jsp Harveststatus-jobdetails.jsp. Initializes the objects needed to build the page. Code has been moved from the jsp to here to avoid compile errors at
     * runtime in correlation with the upgrade to java 1.8 and introduction of embedded tomcat to handle jsp pages. This was previously done via jetty 6
     *
     * @param response http response to servlet request
     * @param pageContext context object from jsp
     * @param I18N the internationlization object from the jsp caller
     */
    public HarvestStatusJobDetails(HttpServletResponse response, PageContext pageContext, I18n I18N)
            throws ArgumentNotValid {
        ArgumentNotValid.checkNotNull(response, "ServletRequest response");
        ArgumentNotValid.checkNotNull(pageContext, "PageContext pageContext");
        ArgumentNotValid.checkNotNull(I18N, "I18n I18N");

        try {
            jobID = HTMLUtils.parseAndCheckInteger(pageContext, Constants.JOB_PARAM, 1, Integer.MAX_VALUE);
        } catch (ForwardedToErrorPage e) {
            return;
        }

        try {
            job = JobDAO.getInstance().read(jobID);
        } catch (UnknownID e) {
            HTMLUtils.forwardWithErrorMessage(pageContext, I18N, "errormsg;job.unknown.id.0", jobID);
            return;
        }

        harvesturl= "/HarvestDefinition/Definitions-selective-harvests.jsp";
        jobdetailsUrl = "/History/Harveststatus-jobdetails.jsp";
        definitionsSitesectionDeployed = SiteSection.isDeployed(Constants.DEFINITIONS_SITESECTION_DIRNAME);
        harvestID = job.getOrigHarvestDefinitionID();
        try {
            harvestname = HarvestDefinitionDAO.getInstance().getHarvestName(harvestID);
            // define harvesturl, only if we have deployed the Definitions sitesection
            if (definitionsSitesectionDeployed) {
                if (HarvestDefinitionDAO.getInstance().isSnapshot(harvestID)) {
                    harvesturl =
                            "/HarvestDefinition/Definitions-edit-snapshot-harvest.jsp?"
                                    + Constants.HARVEST_PARAM + "="
                                    + HTMLUtils.encode(harvestname);
                } else {
                    harvesturl = "/HarvestDefinition/Definitions-edit-selective-harvest.jsp?"
                            + Constants.HARVEST_PARAM + "="
                            + HTMLUtils.encode(harvestname);
                }
            }
        } catch (UnknownID e) {
            // If no harvestdefinition is known with ID=harvestID
            // Set harvestname = an internationalized version of
            // "Unknown harvest" + harvestID
            harvestname = I18N.getString(response.getLocale(), "unknown.harvest.0", harvestID);
        }
        harvestnameTdContents = HTMLUtils.escapeHtmlValues(harvestname);
        if (definitionsSitesectionDeployed) {
            harvestnameTdContents = "<a href=\"" + HTMLUtils.escapeHtmlValues(harvesturl)
                    + "\">" + HTMLUtils.escapeHtmlValues(harvestname) + "</a>";
        }
        jobstatusTdContents = job.getStatus().getLocalizedString(response.getLocale());
        // If the status of the job is RESUBMITTED (and the new job is known),
        // add a link to the new job
        // Note: this information was only available from release 3.8.0
        // So for historical jobs generated with previous versions of NetarchiveSuite,
        // this information is not available.
        if (job.getStatus().equals(JobStatus.RESUBMITTED)
                && job.getResubmittedAsJob() != null) {
            jobstatusTdContents += "<br/>(<a href=\"" + jobdetailsUrl + "?"
                    + Constants.JOB_PARAM + "=" + job.getResubmittedAsJob() + "\">"
                    + "Job " + job.getResubmittedAsJob() + "</a>" + ")";
        }
    }

    public void PrintPageContent(JspWriter out, I18n I18N, Locale locale) throws IOException, ArgumentNotValid {
        ArgumentNotValid.checkNotNull(out, "JspWriter out");
        ArgumentNotValid.checkNotNull(I18N, "I18n I18N");
        ArgumentNotValid.checkNotNull(locale, "Locale locale");

        //Job details table
        out.println("<h3 class=\"page_heading\">" + I18N.getString(locale, "pagetitle;details.for.job.0", jobID) + "</h3>");
        out.println("<table class=\"selection_table\">");
        out.println("<tr>");
        out.println("<th>" + I18N.getString(locale, "table.job.jobid") + "</th>");
        out.println("<th>" + I18N.getString(locale, "table.job.type") + "</th>");
        out.println("<th>" + I18N.getString(locale, "table.job.harvestname") + "</th>");
        out.println("<th>" + I18N.getString(locale, "table.job.harvestnumber") + "</th>");
        out.println("<th>" + I18N.getString(locale, "table.job.creationtime") + "</th>");
        out.println("<th>" + I18N.getString(locale, "table.job.starttime") + "</th>");
        out.println("<th>" + I18N.getString(locale, "table.job.stoptime") + "</th>");
        out.println("<th>" + I18N.getString(locale, "table.job.jobstatus") + "</th>");
        out.println("<th>" + I18N.getString(locale, "table.job.harvesterror") + "</th>");
        out.println("<th>" + I18N.getString(locale, "table.job.uploaderror") + "</th>");
        out.println("<th>" + I18N.getString(locale, "table.job.objectlimit") + "</th>");
        out.println("<th>" + I18N.getString(locale, "table.job.bytelimit") + "</th>");
        out.println("</tr>");
        out.println("<tr>");
        out.println("<td>" + job.getJobID() + "</td>");
        out.println("<td>" + job.getChannel() + "</td>");
        out.println("<td>" + harvestnameTdContents + "</td>");
        out.println("<td>" + dk.netarkivet.harvester.webinterface.HarvestStatus.makeHarvestRunLink(harvestID, job.getHarvestNum()) + "</td>");
        out.println("<td>" + HTMLUtils.parseDate(job.getCreationDate()) + "</td>");
        out.println("<td>" + HTMLUtils.parseDate(job.getActualStart()) + "</td>");
        out.println("<td>" + HTMLUtils.parseDate(job.getActualStop()) + "</td>");
        out.println("<td>" + jobstatusTdContents + "</td>");
        out.println("<td><a href=\"#harvesterror\">" + HTMLUtils.escapeHtmlValues(job.getHarvestErrors()) + "</a></td>");
        out.println("<td><a href=\"#uploaderror\">" + HTMLUtils.escapeHtmlValues(job.getUploadErrors()) + "</a></td>");
        out.println("<td>" + job.getMaxObjectsPerDomain() + "</td>");
        out.println("<td>" + job.getMaxBytesPerDomain() + "</td>");
        out.println("</tr>");
        out.println("</table>");
        //display the searchFilter if QA webpages are deployed--

        if (SiteSection.isDeployed(Constants.QA_SITESECTION_DIRNAME)) {
            out.println("<h3>" + I18N.getString(locale, "subtitle.job.qa.selection") + "</h3>");
            out.println("<table class=\"selection_table\"><tr><td>");
            out.println("<p><a href=\"/" + Constants.QA_SITESECTION_DIRNAME + "/QA-changeIndex.jsp?" + Constants.JOB_PARAM + "=" +
            job.getJobID() + "&" + Constants.INDEXLABEL_PARAM + "=" + HTMLUtils.escapeHtmlValues(HTMLUtils.encode(I18N.getString(
                    locale, "job.0", job.getJobID()))) + "\">" + I18N.getString(locale, "select.job.for.qa.with.viewerproxy") + "</a></p>");
            out.println("</td></tr>");
            out.println("<tr><td>" + I18N.getString(locale, "helptext;select.job.for.qa.with.viewerproxy") + "</td></tr></table>");
        }
        out.println("<h3>" + I18N.getString(locale, "subtitle.job.domainconfigurations") + "</h3>");
        out.println("<table class=\"selection_table\"><tr>");
        out.println("<th>" + I18N.getString(locale, "table.job.domainconfigurations.domain") + "</th>");
        out.println("<th>" + I18N.getString(locale, "table.job.domainconfigurations.configuration") + "</th>");
        out.println("<th>" + I18N.getString(locale, "table.job.domainconfigurations.bytesharvested") + "</th>");
        out.println("<th>" + I18N.getString(locale, "table.job.domainconfigurations.documentsharvested") + "</th>");
        out.println("<th>" + I18N.getString(locale, "table.job.domainconfigurations.stopreason") + "</th>");
        out.println("</tr>");

        DomainDAO ddao = DomainDAO.getInstance();
        int rowcount = 0;
        for (Map.Entry<String, String> conf :
                job.getDomainConfigurationMap().entrySet()) {
            String domainLink;
            String configLink;
            String domainName = conf.getKey();
            String configName = conf.getValue();
            if (SiteSection.isDeployed(
                    Constants.DEFINITIONS_SITESECTION_DIRNAME)) {
                domainLink =
                        "<a href=\"/HarvestDefinition/Definitions-edit-domain.jsp?"
                                + Constants.DOMAIN_PARAM + "="
                                + HTMLUtils.encodeAndEscapeHTML(domainName) + "\">"
                                + HTMLUtils.escapeHtmlValues(domainName) + "</a>";
                configLink =
                        "<a href=\"/HarvestDefinition/Definitions-edit-domain-config.jsp?"
                                + Constants.DOMAIN_PARAM + "="
                                + HTMLUtils.encodeAndEscapeHTML(domainName) + "&amp;"
                                + Constants.CONFIG_NAME_PARAM + "="
                                + HTMLUtils.encodeAndEscapeHTML(configName) + "&amp;"
                                + Constants.EDIT_CONFIG_PARAM + "=1\">"
                                + HTMLUtils.escapeHtmlValues(configName)  + "</a>";
            } else {
                domainLink = HTMLUtils.escapeHtmlValues(domainName);
                configLink = HTMLUtils.escapeHtmlValues(configName);
            }
            HarvestInfo hi = ddao.getDomainJobInfo(job, domainName,
                    configName);

            out.println("<tr class=" + HTMLUtils.getRowClass(rowcount++) + ">");
            out.println("<td>" + domainLink + "</td>");
            out.println("<td>" + configLink + "</td>");
            if (hi == null) {
                out.println("<td>-</td><td>-</td><td>-</td>");
                } else {
                out.println("<td>" + hi.getSizeDataRetrieved() + "</td>");
                out.println("<td>" + hi.getCountObjectRetrieved() + "</td>");
                out.println("<td>" + hi.getStopReason().getLocalizedString(locale) + "</td>");
                out.println("</tr>");
            }
        }

        out.println("</table>");
        out.println("<h3>" + I18N.getString(locale, "subtitle.job.seedlist") + "</h3>");
        out.println("<p>");

        for (String seed : job.getSortedSeedList()) {
            String url;
            if (!seed.matches(Constants.PROTOCOL_REGEXP)) {
                url = "http://" + seed;
            } else {
                url = seed;
            }
            // If length of seed exceeds Constants.MAX_SHOWN_SIZE_OF_URL
            // show only Constants.MAX_SHOWN_SIZE_OF_URL of the seed, and append
            // the string " .."
            String shownSeed = StringUtils.makeEllipsis(seed,
                    Constants.MAX_SHOWN_SIZE_OF_URL);

            out.println("<a target=\"viewerproxy\" href=\"" + HTMLUtils.escapeHtmlValues(url) + "\">" +
                    HTMLUtils.escapeHtmlValues(shownSeed) + "</a><br/>");
        }
        out.println("</p>");


        if (SiteSection.isDeployed(Constants.QA_SITESECTION_DIRNAME)
                && job.getStatus().ordinal() > JobStatus.STARTED.ordinal()) {
            //make links to reports from harvest, extracted from viewerproxy.
            String harvestprefix = job.getHarvestFilenamePrefix();

            out.println("<h3>" +  I18N.getString(locale, "subtitle;reports.for.job") + "</h3>");
            out.println("<p><a href=\"/QA/QA-getreports.jsp?jobid=" + jobID + "\">" + I18N.getString(locale, "harvest.reports") + "</a></p>");
            out.println("<p><a href=\"/QA/QA-getfiles.jsp?jobid=" + jobID + "&harvestprefix=" + harvestprefix + "\">" + I18N.getString(locale, "harvest.files") + "</a></p>");

            //search in crawl-logs
            out.println("<p>");
            out.println("<form method=\"post\" action=\"/QA/QA-searchcrawllog.jsp\">");
            out.println("<input type=\"hidden\" name=\"" + dk.netarkivet.viewerproxy.webinterface.Constants.JOBID_PARAM + "\" value=\"" + jobID + "\" />");
            out.println("<input type=\"submit\" value=\"" + I18N.getString(locale, "display.crawl.log.lines.matching.regexp") + "\" />");
            out.println("<input type=\"text\" name=\"" + dk.netarkivet.viewerproxy.webinterface.Constants.REGEXP_PARAM + "\" size=\"60\"/>");
            out.println("</form></p>");

            //make submit button for recalling crawl.log relevant for the specific domains.
            out.println("<p>");

            out.println("<form method=\"post\" action=\"/QA/QA-searchcrawllog.jsp\">");
            out.println("<input type=\"hidden\" name=\"" + dk.netarkivet.viewerproxy.webinterface.Constants.JOBID_PARAM + "\" value=\"" + jobID + "\" />");
            out.println("<input type=\"submit\" value=\"" + I18N.getString(locale, "crawl.log.lines.for.domain") + "\" />");
            out.println("<select name=\"" + dk.netarkivet.viewerproxy.webinterface.Constants.DOMAIN_PARAM + "\">");
            for (String domain : job.getDomainConfigurationMap().keySet()) {
                out.println("<option value=" + HTMLUtils.escapeHtmlValues(domain) + ">" + HTMLUtils.escapeHtmlValues(domain) + "</option>");
            }
            out.println("</select>");
            out.println("</form>");
            out.println("</p>");
        }


        out.println("<h3>" + I18N.getString(locale, "subtitle.job.harvesttemplate",
        HTMLUtils.escapeHtmlValues((job.getOrderXMLName()))) + "</h3>");

        // make link to harvest template for job
        String link = "/History/Harveststatus-download-job-harvest-template.jsp?"
                + "JobID=" + job.getJobID();
        String linkWithrequestedType = link + "&requestedContentType=text/plain";

        out.println("<a href=\"" + link + "\">" + I18N.getString(locale, "show.job.0.harvesttemplate", job.getJobID()) + "</a>&nbsp;(<a href=\"" +
                linkWithrequestedType + "\">text/plain</a>)");

        if (job.getUploadErrors() != null && job.getUploadErrors().length() != 0) {
            out.println("<a id=\"uploaderror\"></a>");
            out.println("<h3>" + I18N.getString(locale, "subtitle.job.uploaderror.details") + "</h3>");
            out.println("<pre>" + HTMLUtils.escapeHtmlValues(job.getUploadErrorDetails()) + "</pre>");
        }
        if (job.getHarvestErrors() != null && job.getHarvestErrors().length() != 0) {
            out.println("<a id=\"harvesterror\"></a>");
            out.println("<h3>" + I18N.getString(locale, "subtitle.job.harvesterror.details") + "</h3>");
            out.println("<pre>" + HTMLUtils.escapeHtmlValues(job.getHarvestErrorDetails()) + "</pre>");
        }
    }
}
