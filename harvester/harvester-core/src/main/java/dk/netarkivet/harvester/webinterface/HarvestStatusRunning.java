package dk.netarkivet.harvester.webinterface;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.JspWriter;

import com.hp.gagawa.java.elements.I;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.utils.I18n;
import dk.netarkivet.common.utils.StringUtils;
import dk.netarkivet.common.utils.TableSort;
import dk.netarkivet.common.webinterface.HTMLUtils;
import dk.netarkivet.harvester.datamodel.RunningJobsInfoDAO;
import dk.netarkivet.harvester.harvesting.monitor.StartedJobInfo;

/**
 * Created by jve on 6/1/16.
 */
public class HarvestStatusRunning {
    private int jobCount = 0;
    private Map<String, List<StartedJobInfo>> infos;
    private FindRunningJobQuery findJobQuery;
    private Long[] jobIdsForDomain;
    private String sortedColumn;
    private String sortedHarvest;
    private HarvestStatusRunningTablesSort tbs;

    /**
     * Constuctor for jsp Harveststatus-running.jsp. Initializes the objects needed to build the page. Code has been moved from the jsp to here to avoid compile errors at
     * runtime in correlation with the upgrade to java 1.8 and introduction of embedded tomcat to handle jsp pages. This was previously done via jetty 6.
     *
     * @param request http request from selvlet
     * @param session http session for page context Harveststatus-running.jsp
     */
    public HarvestStatusRunning(ServletRequest request, HttpSession session)
            throws ArgumentNotValid {
        ArgumentNotValid.checkNotNull(request, "ServletRequest request");
        ArgumentNotValid.checkNotNull(session, "HttpSession session");

        tbs=(HarvestStatusRunningTablesSort)session.getAttribute("TablesSortData");
        if(tbs==null){
            tbs = new HarvestStatusRunningTablesSort();
            session.setAttribute("TablesSortData",tbs);
        }

        sortedColumn=request.getParameter(Constants.COLUMN_PARAM);
        sortedHarvest=request.getParameter(Constants.HARVEST_PARAM);

        if( sortedColumn != null && sortedHarvest != null) {
            tbs.sortByHarvestName(sortedHarvest,Integer.parseInt(sortedColumn)) ;
        }

        //list of information to be shown
        infos = RunningJobsInfoDAO.getInstance().getMostRecentByHarvestName();


        for (List<StartedJobInfo> jobList : infos.values()) {
            jobCount += jobList.size();
        }

        findJobQuery = new FindRunningJobQuery(request);
        jobIdsForDomain = findJobQuery.getRunningJobIds();

    }

    /**
     * Build and print the page content for Harveststatus-running.jsp.
     *
     * @param out http session for page context Harveststatus-running.jsp
     * @param request http request from selvlet
     * @param I18N the internationlization object from the jsp caller
     * @param locale the locale used by the jsp page
     */
    public void PrintPageContent(JspWriter out, ServletRequest request, I18n I18N, Locale locale) throws IOException, ArgumentNotValid {
        ArgumentNotValid.checkNotNull(out, "JspWriter out");
        ArgumentNotValid.checkNotNull(request, "ServletRequest request");
        ArgumentNotValid.checkNotNull(I18N, "I18n I18N");
        ArgumentNotValid.checkNotNull(locale, "Locale locale");

        out.println("<h3 class=\"page_heading\">" + I18N.getString(locale, "pagetitle;all.jobs.running") + "</h3>");

        if (infos.size() == 0) {
            out.println(I18N.getString(locale, "table.job.no.jobs"));
        } else { //Make table with found jobs
            out.println(I18N.getString(locale, "running.jobs.nbrunning") + " " + jobCount);
            out.println("<table class=\"selection_table\">");

            for (String harvestName : infos.keySet()) {

                String harvestDetailsLink = "Harveststatus-perhd.jsp?"
                        + Constants.HARVEST_PARAM + "="
                        + HTMLUtils.encode(harvestName);

                //gestion des fleche de trie
                String incSortPic = "&uarr;";
                String descSortPic = "&darr;";
                String noSortPic = "";
                String tabArrow[] = new String[10];
                for( int i=0;i<10;i++) {
                    tabArrow[i] =  noSortPic;
                }
                String arrow = noSortPic;
                HarvestStatusRunningTablesSort.ColumnId cid = tbs.getSortedColumnIdentByHarvestName(harvestName);
                if(cid != HarvestStatusRunningTablesSort.ColumnId.NONE){

                    TableSort.SortOrder order = tbs.getSortOrderByHarvestName(harvestName);
                    if( order == TableSort.SortOrder.INCR){
                        arrow = incSortPic;
                    }
                    if( order == TableSort.SortOrder.DESC){
                        arrow = descSortPic;
                    }
                    tabArrow[cid.ordinal()] = arrow;
                }

                String sortBaseLink="Harveststatus-running.jsp?"
                        + Constants.HARVEST_PARAM + "="
                        + HTMLUtils.encode(harvestName)
                        + "&"
                        +Constants.COLUMN_PARAM + "=" ;
                String sortLink;
                String columnId;

                out.println("<tr class=\"spacerRowBig\"><td colspan=\"12\">&nbsp;</td></tr>");
                out.println("<tr><th colspan=\"13\">");
                out.println(I18N.getString(locale, "table.running.jobs.harvestName") + "&nbsp;<a href=\"" + harvestDetailsLink + "\">" + harvestName + "</a>");
                out.println("</th></tr>");
                out.println("<tr class=\"spacerRowSmall\"><td colspan=\"12\">&nbsp;</td></tr>");
                out.println("<tr>");
                out.println("<th class=\"harvestHeader\" rowspan=\"2\">");
                sortLink=sortBaseLink + HarvestStatusRunningTablesSort.ColumnId.ID.hashCode();
                out.println("<a href=\"" + sortLink + "\">" + I18N.getString(locale, "table.running.jobs.jobId") +
                        tabArrow[HarvestStatusRunningTablesSort.ColumnId.ID.ordinal()] + "</a>");
                out.println("</th>");
                out.println("<th class=\"harvestHeader\" rowspan=\"2\">");
                sortLink=sortBaseLink  + HarvestStatusRunningTablesSort.ColumnId.HOST.hashCode();
                out.println("<a href=\"" + sortLink + "\">" + I18N.getString(locale, "table.running.jobs.host") +
                        tabArrow[HarvestStatusRunningTablesSort.ColumnId.HOST.ordinal()] + "</a>");
                out.println("</th>");
                out.println("<th class=\"harvestHeader\" rowspan=\"2\">");
                sortLink=sortBaseLink + HarvestStatusRunningTablesSort.ColumnId.PROGRESS.hashCode();
                out.println("<a href=\"" + sortLink + "\">" + I18N.getString(locale, "table.running.jobs.progress") +
                        tabArrow[HarvestStatusRunningTablesSort.ColumnId.PROGRESS.ordinal()] + "</a>");
                out.println("</th>");

                out.println("<th class=\"harvestHeader\" rowspan=\"2\">");
                sortLink=sortBaseLink + HarvestStatusRunningTablesSort.ColumnId.ELAPSED.hashCode();
                out.println("<a href=\"" + sortLink + "\">" + I18N.getString(locale, "table.running.jobs.elapsedTime") +
                        tabArrow[HarvestStatusRunningTablesSort.ColumnId.ELAPSED.ordinal()] + "</a>");
                out.println("</th>");
                out.println("<th class=\"harvestHeader\" colspan=\"5\">" + I18N.getString(locale, "table.running.jobs.queues") + "</th>");
                out.println("<th class=\"harvestHeader\" colspan=\"3\">" + I18N.getString(locale, "table.running.jobs.performance") + "</th>");
                out.println("<th class=\"harvestHeader\" rowspan=\"2\">" + I18N.getString(locale, "table.running.jobs.alerts") + "</th>");
                out.println("</tr>");
                out.println("<tr>");
                out.println("<th class=\"harvestHeader\" >");
                sortLink=sortBaseLink + HarvestStatusRunningTablesSort.ColumnId.QFILES.hashCode();
                out.println("<a href=\"" + sortLink + "\">" + I18N.getString(locale, "table.running.jobs.queuedFiles") +
                        tabArrow[HarvestStatusRunningTablesSort.ColumnId.QFILES.ordinal()] + "</a>");
                out.println("</th>");
                out.println("<th class=\"harvestHeader\" >");
                sortLink=sortBaseLink  + HarvestStatusRunningTablesSort.ColumnId.TOTALQ.hashCode();
                out.println("<a href=\"" + sortLink + ">" + I18N.getString(locale, "table.running.jobs.totalQueues") +
                        tabArrow[HarvestStatusRunningTablesSort.ColumnId.TOTALQ.ordinal()] + "</a>");
                out.println("</th>");
                out.println("<th class=\"harvestHeader\" >");
                sortLink=sortBaseLink + HarvestStatusRunningTablesSort.ColumnId.ACTIVEQ.hashCode();
                out.println("<a href=\"" + sortLink + "\">" + I18N.getString(locale, "table.running.jobs.activeQueues") +
                        tabArrow[HarvestStatusRunningTablesSort.ColumnId.ACTIVEQ.ordinal()] + "</a>");
                out.println("</th>");
                out.println("<th class=\"harvestHeader\">");
                sortLink=sortBaseLink + HarvestStatusRunningTablesSort.ColumnId.RETIREDQ.hashCode();
                out.println("<a href=\"" + sortLink + "\"" + I18N.getString(locale, "table.running.jobs.retiredQueues") +
                        tabArrow[HarvestStatusRunningTablesSort.ColumnId.RETIREDQ.ordinal()] + "</a>");
                out.println("</th>");
                out.println("<th class=\"harvestHeader\" >");
                sortLink=sortBaseLink + HarvestStatusRunningTablesSort.ColumnId.EXHAUSTEDQ.hashCode();
                out.println("<a href=\"" + sortLink + "\">" + I18N.getString(locale, "table.running.jobs.exhaustedQueues") +
                        tabArrow[HarvestStatusRunningTablesSort.ColumnId.EXHAUSTEDQ.ordinal()] + "</a>");
                out.println("</th>");
                out.println("<th class=\"harvestHeader\">" + I18N.getString(locale, "table.running.jobs.currentProcessedDocsPerSec") + "</th>");
                out.println("<th class=\"harvestHeader\">" + I18N.getString(locale, "table.running.jobs.currentProcessedKBPerSec") + "</th>");
                out.println("<th class=\"harvestHeader\">" + I18N.getString(locale, "table.running.jobs.toeThreads") + "</th>");
                out.println("</tr>");

                int rowcount = 0;

                //recup list
                List<StartedJobInfo> infoList = infos.get(harvestName);

                //trie de la List
                HarvestStatusRunningTablesSort.ColumnId cidSort= tbs.getSortedColumnIdentByHarvestName(harvestName);

                if(cidSort != HarvestStatusRunningTablesSort.ColumnId.NONE){

                    for (StartedJobInfo info : infoList) {
                        if(cidSort == HarvestStatusRunningTablesSort.ColumnId.ID){
                            info.chooseCompareCriteria(StartedJobInfo.Criteria.JOBID);
                        }
                        if(cidSort == HarvestStatusRunningTablesSort.ColumnId.HOST){
                            info.chooseCompareCriteria(StartedJobInfo.Criteria.HOST);
                        }
                        if(cidSort == HarvestStatusRunningTablesSort.ColumnId.ELAPSED){
                            info.chooseCompareCriteria(StartedJobInfo.Criteria.ELAPSED);
                        }
                        if(cidSort == HarvestStatusRunningTablesSort.ColumnId.PROGRESS){
                            info.chooseCompareCriteria(StartedJobInfo.Criteria.PROGRESS);
                        }
                        if(cidSort == HarvestStatusRunningTablesSort.ColumnId.EXHAUSTEDQ){
                            info.chooseCompareCriteria(StartedJobInfo.Criteria.EXHAUSTEDQ);
                        }
                        if(cidSort == HarvestStatusRunningTablesSort.ColumnId.ACTIVEQ){
                            info.chooseCompareCriteria(StartedJobInfo.Criteria.ACTIVEQ);
                        }
                        if(cidSort == HarvestStatusRunningTablesSort.ColumnId.TOTALQ){
                            info.chooseCompareCriteria(StartedJobInfo.Criteria.TOTALQ);
                        }
                        if(cidSort == HarvestStatusRunningTablesSort.ColumnId.QFILES){
                            info.chooseCompareCriteria(StartedJobInfo.Criteria.QFILES);
                        }
                    }

                    TableSort.SortOrder order = tbs.getSortOrderByHarvestName(harvestName);

                    if( order == TableSort.SortOrder.INCR){
                        Collections.sort(infoList);
                    }
                    if( order == TableSort.SortOrder.DESC){
                        Collections.sort(infoList, Collections.reverseOrder());
                    }
                }

                for (StartedJobInfo info : infoList) {
                    long jobId = info.getJobId();

                    String jobDetailsLink = "Harveststatus-running-jobdetails.jsp?"
                            + Constants.JOB_PARAM + "=" + jobId;


                    out.println("<tr class=\"" + HTMLUtils.getRowClass(rowcount++) + "\">");
                    out.println("<td><a href=\"" + jobDetailsLink + "\"><%=jobId%></a></td>");
                    out.println("<td class=\"crawlerHost\">&nbsp;");

                    String altStatus = "?";
                    String bullet = "?";
                    switch (info.getStatus()) {
                    case PRE_CRAWL:
                        altStatus = "table.running.jobs.status.preCrawl";
                        bullet = "bluebullet.png";
                        break;
                    case CRAWLER_ACTIVE:
                        altStatus = "table.running.jobs.status.crawlerRunning";
                        bullet = "greenbullet.png";
                        break;
                    case CRAWLER_PAUSING:
                        altStatus = "table.running.jobs.status.crawlerPausing";
                        bullet = "yellowbullet.png";
                        break;
                    case CRAWLER_PAUSED:
                        altStatus = "table.running.jobs.status.crawlerPaused";
                        bullet = "redbullet.png";
                        break;
                    case CRAWLING_FINISHED:
                        altStatus = "table.running.jobs.status.crawlFinished";
                        bullet = "greybullet.png";
                        break;

                    }

                    out.println("<img src=\"" + bullet + "\" alt=\"" + I18N.getString(locale, altStatus) + "\"/>&nbsp;<a href=\"" +
                            info.getHostUrl() + "\" target=\"_blank\">" + info.getHostName() + "</a>");
                    out.println("</td>");
                    out.println("<td align=\"right\">" + StringUtils.formatPercentage(info.getProgress()) + "</td>");
                    out.println("<td align=\"right\">" + info.getElapsedTime() + "</td>");
                    out.println("<td align=\"right\">" + info.getQueuedFilesCount() + "</td>");
                    out.println("<td align=\"right\">" + info.getTotalQueuesCount() + "</td>");
                    out.println("<td align=\"right\">" + info.getActiveQueuesCount() + "</td>");
                    out.println("<td align=\"right\">" + info.getRetiredQueuesCount() + "</td>");
                    out.println("<td align=\"right\">" + info.getExhaustedQueuesCount() + "</td>");
                    out.println("<td align=\"right\">" + StringUtils.formatNumber(info.getCurrentProcessedDocsPerSec())
                            + " (" + StringUtils.formatNumber(info.getProcessedDocsPerSec())
                            + ")</td>");
                    out.println("<td align=\"right\">" + StringUtils.formatNumber(info.getCurrentProcessedKBPerSec())
                            + " (" + StringUtils.formatNumber(info.getProcessedKBPerSec())
                            + ")</td>");
                    out.println("<td align=\"right\">" + info.getActiveToeCount() + "</td>");
                    out.println("<td align=\"right\">" + info.getAlertsCount() + "</td>");
                    out.println("</tr>");
                }
            }

            out.println("</table>");
            out.println("<br/><br/>&nbsp;" + I18N.getString(locale, "table.running.jobs.legend"));
            out.println("<img src=\"bluebullet.png\" alt=\"" + I18N.getString(locale, "table.running.jobs.status.preCrawl") + "\"/>");
            out.println("<img src=\"greenbullet.png\" alt=\"" + I18N.getString(locale, "table.running.jobs.status.crawlerRunning") + "\"/>");
            out.println("<img src=\"yellowbullet.png\" alt=\"" + I18N.getString(locale, "table.running.jobs.status.crawlerPausing") + "\"/>");
            out.println("<img src=\"redbullet.png\" alt=\"" + I18N.getString(locale, "table.running.jobs.status.crawlerPaused") + "\"/>");
            out.println("<img src=\"greybullet.png\" alt=\"" + I18N.getString(locale, "table.running.jobs.status.crawlFinished") + "\"/>");

            out.println("<br/><br/>");
            out.println("<form method=\"get\" name=\"findJobForDomainForm\" action=\"Harveststatus-running.jsp\">");
            out.println("<input type=\"hidden\" name=\"searchDone\" value=\"1\"/>");
            out.println(I18N.getString(locale, "running.jobs.finder.inputGroup"));
            out.println("<input type=\"text\" name=\"" + FindRunningJobQuery.UI_FIELD.DOMAIN_NAME.name() + " size=\"30\" value=\"\"/>");
            out.println("<input type=\"submit\" name=\"search\" value=\"" + I18N.getString(locale ,"running.jobs.finder.submit") + "\"/>");
            out.println("</form>");

            if (jobIdsForDomain.length > 0) {
                out.println("<br/>");
                out.println("<table class=\"selection_table_small\">");
                out.println("<tr>");
                out.println("<th>" + I18N.getString(locale, "running.jobs.finder.table.jobId") + "</th>");
                out.println("</tr>");
                for (long jobId : jobIdsForDomain) {
                    String jobDetailsLink = "Harveststatus-jobdetails.jsp?"
                            + Constants.JOB_PARAM + "=" + jobId;

                    out.println("<tr><td><a href=\"" + jobDetailsLink + "\">" + jobId + "</a></td></tr>");
                }
                out.println("</table>");
            } else {
                //after using the search button "searchDone" !=null
                String searchDone = request.getParameter("searchDone");
                if (searchDone != null) {
                    I18N.getString(locale, "table.job.no.jobs");
                }
            }
        }
    }
}
