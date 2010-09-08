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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.ServletRequest;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.harvester.datamodel.JobStatus;

/**
 * Represents a query for a set of jobs. Filtering can be performed on:
 * <ul>
 * <li>Job Status (multiple)</li>
 * <li>Harvest name (single)</li>
 * <li>Job start date (day of month and year)</li>
 * <li>Job end date (day of month and year)</li>
 * </ul>
 * 
 * The semantics of the date filters is as follows:
 * <ol>
 * <li>If only a start date is specified, will fetch jobs whose start date 
 * is equal or posterior</li>
 * <li>If only an end date is specified, will fetch jobs whose end date 
 * is equal or anterior</li>
 * <li>If both are specified, will fetch jobs whose start and end date are equal 
 * or comprised between the specified bounds.</li>
 * </ol>
 * 
 * The class enforces that end date is set at a date posterior to start date.
 * 
 * Additionally a sort order (applied to job IDs) can be set (ascending or 
 * descending), and the query can be limited to a certain row number and
 * a start index.
 * 
 */
public class HarvestStatusQuery {

    /** The String code to select all states. */
    public static final String JOBSTATUS_ALL = "ALL";
    
    /** The String code to select all harvests. */
    public static final String HARVEST_NAME_ALL = "ALL";
    
    public static final String HARVEST_NAME_WILDCARD= "*";
    public static final long PAGE_SIZE_NONE= 0;
    public static final long DATE_NONE= -1;
    
    public static enum SORT_ORDER {
        ASC, DESC;

        public static SORT_ORDER parse(String order) {
            for (SORT_ORDER o : values()) {
                if (o.name().equals(order)) {
                    return o;
                }
            }
            return null;
        }
    }

    /**
     * Defines the UI fields and their default values.
     */
    public static enum UI_FIELD {
        JOB_STATUS(JobStatus.STARTED.name()), JOB_ID_ORDER("ASC"), HARVEST_NAME(
                HARVEST_NAME_ALL), HARVEST_ID(""), HARVEST_RUN(""), START_DATE(
                ""), END_DATE(""), PAGE_SIZE(Settings.get(
                        CommonSettings.HARVEST_STATUS_DFT_PAGE_SIZE)), 
                        START_PAGE_INDEX(
                "1"), RESUBMIT_JOB_IDS("");

        private String defaultValue;

        UI_FIELD(String defaultValue) {
            this.defaultValue = defaultValue;
        }

        public String[] getValues(ServletRequest req) {
            String[] values = req.getParameterValues(name());
            if (values == null || values.length == 0) {
                return new String[] { this.defaultValue };
            }
            return values;
        }

        /**
         * Extracts the field's value from a servlet request. If the request 
         * does not define the paraeter's value, it is set to the default
         * value. 
         * @param req a servlet request
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
    
    // The calendar widget uses a date format that differs from the patterns
    // used by SimpleDateFormat, hence we need two preperties
    public static final String CALENDAR_UI_DATE_FORMAT = "%Y/%m/%d";
    
    private static final SimpleDateFormat DATE_FORMAT = 
        new SimpleDateFormat("yyyy/MM/dd");

    private Set<JobStatus> jobStatuses = new HashSet<JobStatus>();

    private Long harvestId;

    private Long harvestRunNumber;

    private String harvestName;

    private Date startDate;

    private Date endDate;

    private SORT_ORDER sort;

    private long pageSize;

    private long startPageIndex;

    /**
     * Builds a default query that will select all jobs.
     */
    public HarvestStatusQuery() {

    }

    /**
     * Builds a default query that will find jobs for a given run of a harvest.
     * @param harvestId A given harvestId
     * @param harvestRunNumber a given harvestRunNumber
     */
    public HarvestStatusQuery(long harvestId, long harvestRunNumber) {
        this.harvestId = harvestId;
        this.harvestRunNumber = harvestRunNumber;
    }

    /**
     * Builds a query from a servlet request. Unspecified fields are set to
     * their default value.
     * 
     * @param req a servlet request
     */
    public HarvestStatusQuery(ServletRequest req) {

        String[] statuses = (String[]) UI_FIELD.JOB_STATUS.getValues(req);
        for (String s : statuses) {
            if (JOBSTATUS_ALL.equals(s)) {
                this.jobStatuses.clear();
                break;
            }
            this.jobStatuses.add(JobStatus.parse(s));
        }

        this.harvestName = (String) UI_FIELD.HARVEST_NAME.getValue(req);
        if (HARVEST_NAME_ALL.equalsIgnoreCase(this.harvestName)) {
            this.harvestName = "";
        }

        String harvestIdStr = (String) UI_FIELD.HARVEST_ID.getValue(req);
        try {
            this.harvestId = Long.parseLong(harvestIdStr);
        } catch (NumberFormatException e) {
            this.harvestId = null;
        }

        String harvestRunStr = (String) UI_FIELD.HARVEST_RUN.getValue(req);
        try {
            this.harvestRunNumber = Long.parseLong(harvestRunStr);
        } catch (NumberFormatException e) {
            this.harvestRunNumber = null;
        }

        String startDateStr = (String) UI_FIELD.START_DATE.getValue(req);
        if (!startDateStr.isEmpty()) {
            try {
                this.startDate = DATE_FORMAT.parse(startDateStr);
            } catch (ParseException e) {
                // Should never happen, date comes from a date selector UI
                throw new ArgumentNotValid("Invalid date specification", e);
            }
        }

        String endDateStr = (String) UI_FIELD.END_DATE.getValue(req);
        if (!endDateStr.isEmpty()) {
            try {
                this.endDate = DATE_FORMAT.parse(endDateStr);
            } catch (ParseException e) {
                // Should never happen, date comes from a date selector UI
                throw new ArgumentNotValid("Invalid date specification", e);
            }
        }

        if (startDate != null && endDate != null) {
            if (endDate.before(this.startDate)) {
                throw new ArgumentNotValid("End date is set after start date!");
            }
        }

        String orderStr = (String) UI_FIELD.JOB_ID_ORDER.getValue(req);
        this.sort = SORT_ORDER.parse(orderStr);
        if (this.sort == null) {
            // Will not happen as value comes from a select
            throw new ArgumentNotValid("Invalid sort order!");
        }

        String pageSizeStr = UI_FIELD.PAGE_SIZE.getValue(req);
        try {
            this.pageSize = Long.parseLong(pageSizeStr);
        } catch (NumberFormatException e) {
            throw new ArgumentNotValid("Invalid number!", e);
        }

        String startPageIndexStr = UI_FIELD.START_PAGE_INDEX.getValue(req);
        try {
            this.startPageIndex = Long.parseLong(startPageIndexStr);
        } catch (NumberFormatException e) {
            throw new ArgumentNotValid("Invalid number!", e);
        }

    }

    public JobStatus[] getSelectedJobStatuses() {
        return (JobStatus[]) jobStatuses.toArray(new JobStatus[jobStatuses
                .size()]);
    }

    public Set<JobStatus> getSelectedJobStatusesAsSet() {
        return jobStatuses;
    }

    public String getHarvestName() {
        if (harvestName == null) {
            return ""; 
        }
        return harvestName;
    }

    public Long getHarvestId() {
        return harvestId;
    }

    public Long getHarvestRunNumber() {
        return harvestRunNumber;
    }

    public long getStartDate() {
        return (startDate == null ? -1 : startDate.getTime());
    }

    public long getEndDate() {
        return (endDate == null ? -1 : endDate.getTime());
    }

    public String getStartDateAsString() {
        if (startDate == null) {
            return "";
        }
        return DATE_FORMAT.format(startDate);
    }

    public String getEndDateAsString() {
        if (endDate == null) {
            return "";
        }
        return DATE_FORMAT.format(endDate);
    }

    public boolean isSortAscending() {
        return SORT_ORDER.ASC.equals(sort);
    }

    public long getPageSize() {
        return pageSize;
    }

    public long getStartPageIndex() {
        return startPageIndex;
    }

}
