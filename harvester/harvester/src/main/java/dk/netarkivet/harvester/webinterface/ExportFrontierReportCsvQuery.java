/* File:       $Id$
 * Revision:   $Revision$
 * Author:     $Author$
 * Date:       $Date$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2007 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package dk.netarkivet.harvester.webinterface;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.PageContext;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.ForwardedToErrorPage;
import dk.netarkivet.common.utils.I18n;
import dk.netarkivet.common.webinterface.HTMLUtils;
import dk.netarkivet.harvester.datamodel.RunningJobsInfoDAO;
import dk.netarkivet.harvester.harvesting.frontier.FrontierReportCsvExport;
import dk.netarkivet.harvester.harvesting.frontier.InMemoryFrontierReport;
import dk.netarkivet.harvester.harvesting.frontier.TopTotalEnqueuesFilter;

public class ExportFrontierReportCsvQuery {

    /**
     * Defines the UI fields and their default values.
     */
    public static enum UI_FIELD {
        JOB_ID("");

        private String defaultValue;

        UI_FIELD(String defaultValue) {
            this.defaultValue = defaultValue;
        }

        /**
         * Extracts the field's value from a servlet request. If the request
         * does not define the paraeter's value, it is set to the default value.
         *
         * @param req
         *            a servlet request
         * @return the field's value
         */
        public String getValue(ServletRequest req) {
            String value = req.getParameter(name());
            if (value == null || value.isEmpty()) {
                return this.defaultValue;
            }
            return value;
        }

        public Object getDefaultValue() {
            return defaultValue;
        }

    }

    private final long jobId;

    public ExportFrontierReportCsvQuery(ServletRequest req) {

        String jobIdStr = UI_FIELD.JOB_ID.getValue(req);
        ArgumentNotValid.checkNotNullOrEmpty(jobIdStr, UI_FIELD.JOB_ID.name());

        jobId = Long.parseLong(jobIdStr);
    }

    public void doExport(PageContext context, I18n i18n) {

        String filterId = new TopTotalEnqueuesFilter().getFilterId();

        RunningJobsInfoDAO dao = RunningJobsInfoDAO.getInstance();
        InMemoryFrontierReport report = dao.getFrontierReport(jobId, filterId);

        HttpServletResponse resp =
            (HttpServletResponse) context.getResponse();
        resp.setHeader("Content-Type", "text/plain");
        resp.setHeader("Content-Disposition", "Attachment; filename="
                + filterId + "-" + report.getJobName() + ".csv");

        PrintWriter pw;
        try {
            pw = new PrintWriter(resp.getOutputStream());
        } catch (IOException e) {
            HTMLUtils.forwardWithErrorMessage(context, i18n, e,
                "errorMsg;running.job.details.frontier.exportAsCsv");
            throw new ForwardedToErrorPage(
                    "Error in frontier report CSV export", e);
        }

        FrontierReportCsvExport.outputAsCsv(report, pw, ";");
        pw.close();
    }


}