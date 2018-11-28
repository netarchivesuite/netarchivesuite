/*
 * #%L
 * Netarchivesuite - harvester
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
package dk.netarkivet.harvester.webinterface;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
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
 * <p>
 * The semantics of the date filters is as follows:
 * <ol>
 * <li>If only a start date is specified, will fetch jobs whose start date is equal or posterior</li>
 * <li>If only an end date is specified, will fetch jobs whose end date is equal or anterior</li>
 * <li>If both are specified, will fetch jobs whose start and end date are equal or comprised between the specified
 * bounds.</li>
 * </ol>
 * <p>
 * The class enforces that end date is set at a date posterior to start date.
 * <p>
 * Additionally a sort order (applied to job IDs) can be set (ascending or descending), and the query can be limited to
 * a certain row number and a start index.
 */
public class HarvestStatusQuery {

    /** The String code to select all states. */
    public static final String JOBSTATUS_ALL = "ALL";

    /** The String code to select all harvests. */
    public static final String HARVEST_NAME_ALL = "ALL";
    /** String to check, if there is a wildcard in the harvestname. */
    public static final String HARVEST_NAME_WILDCARD = "*";
    /** Value used to define page size undefined. */
    public static final long PAGE_SIZE_NONE = 0;
    /** Value used to define date undefined. */
    public static final long DATE_NONE = -1L;

    /**
     * Enum class defining the different sort-orders.
     */
    public static enum SORT_ORDER {
        /** Ascending mode. From lowest to highest. */
        ASC,
        /** Descending mode. From highest to lowest. */
        DESC;

        /**
         * Parse the given argument and return a sorting order.
         *
         * @param order a given sorting order as string
         * @return a sorting order representing the given string.
         */
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
        /** jobstatus with default status ALL. */
        JOB_STATUS("ALL"),
        /** JOB ID order. default is ascending. */
        JOB_ID_ORDER("DESC"),
        /** harvest name. default is ALL (i.e all harvests). */
        HARVEST_NAME(HARVEST_NAME_ALL),
        /** The harvest ID. No default. */
        HARVEST_ID(""),
        /** The harvest Run number. No default. */
        HARVEST_RUN(""),
        /** The harvest start date. No default. */
        START_DATE(""),
        /** The harvest end date. No default. */
        END_DATE(""),
        /** The job id range : list of job ids or range separated by commas, for instance:  2,4,8-14. No default. */
        JOB_ID_RANGE(""),

        
        /**
         * The number of results on each page. The default is read from the setting
         * {@link CommonSettings#HARVEST_STATUS_DFT_PAGE_SIZE}.
         */
        PAGE_SIZE(Settings.get(CommonSettings.HARVEST_STATUS_DFT_PAGE_SIZE)),
        /** The starting page. Default is 1. */
        START_PAGE_INDEX("1"),
        /** The number of Jobs to resubmit identified by ID. No default. */
        RESUBMIT_JOB_IDS("");

        /** The default value for this UI-field. */
        private final String defaultValue;

        /**
         * Constructor for the UI_FIELD enum class.
         *
         * @param defaultValue the default value of the field.
         */
        UI_FIELD(String defaultValue) {
            this.defaultValue = defaultValue;
        }

        /**
         * Get the values stored in the request for this UI_FIELD.
         *
         * @param req the servlet request
         * @return the values stored in the request for this UI_FIELD as a string array.
         */
        public String[] getValues(ServletRequest req) {
            String[] values = req.getParameterValues(name());
            if (values == null || values.length == 0) {
                return new String[] {this.defaultValue};
            }
            return values;
        }

        /**
         * Extracts the field's value from a servlet request. If the request does not define the paraeter's value, it is
         * set to the default value.
         *
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
    }

    /**
     * The date format used by the calendar widget. It is actually the same format as the one represented by the
     * DATE_FORMAT.
     */
    public static final String CALENDAR_UI_DATE_FORMAT = "%Y/%m/%d";

    /** The date format used when returning dates as strings. */
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy/MM/dd");

    /** The job states selected in this query. */
    private Set<JobStatus> jobStatuses = new HashSet<JobStatus>();
    /** The harvestID. */
    private Long harvestId;
    /** The harvest run number. */
    private Long harvestRunNumber;
    /** The harvest name. */
    private String harvestName;
    /** The start date. */
    private Date startDate;
    /** The end date. */
    private Date endDate;
    /** The job id range : list of job ids or range separated by commas, for instance:  2,4,8-14. No default. */
    private String jobIdRange;
    private List<String> jobIdRangeList;
    /** The sort order. */
    private SORT_ORDER sortingOrder;
    /** The page-size. */
    private long pageSize;
    /** The start page. */
    private long startPageIndex;
    /** Is the harvest name case sensitive. The default is yes. */
    private boolean caseSensitiveHarvestName = true;

    /**
     * Builds a default query that will select all jobs.
     */
    public HarvestStatusQuery() {
    	jobIdRangeList = new ArrayList<String>();
    }

    /**
     * Builds a default query that will find jobs for a given run of a harvest.
     *
     * @param harvestId A given harvestId
     * @param harvestRunNumber a given harvestRunNumber
     */
    public HarvestStatusQuery(long harvestId, long harvestRunNumber) {
        this.harvestId = harvestId;
        this.harvestRunNumber = harvestRunNumber;
        jobIdRangeList = new ArrayList<String>();
    }

    /**
     * Builds a query from a servlet request. Unspecified fields are set to their default value.
     *
     * @param req a servlet request
     */
    public HarvestStatusQuery(ServletRequest req) {

        String[] statuses = UI_FIELD.JOB_STATUS.getValues(req);
        for (String s : statuses) {
            if (JOBSTATUS_ALL.equals(s)) {
                this.jobStatuses.clear();
                break;
            }
            this.jobStatuses.add(JobStatus.parse(s));
        }

        this.harvestName = UI_FIELD.HARVEST_NAME.getValue(req);
        if (HARVEST_NAME_ALL.equalsIgnoreCase(this.harvestName)) {
            this.harvestName = "";
        }

        String harvestIdStr = UI_FIELD.HARVEST_ID.getValue(req);
        try {
            this.harvestId = Long.parseLong(harvestIdStr);
        } catch (NumberFormatException e) {
            this.harvestId = null;
        }

        String harvestRunStr = UI_FIELD.HARVEST_RUN.getValue(req);
        try {
            this.harvestRunNumber = Long.parseLong(harvestRunStr);
        } catch (NumberFormatException e) {
            this.harvestRunNumber = null;
        }

        String startDateStr = UI_FIELD.START_DATE.getValue(req);
        if (!startDateStr.isEmpty()) {
            try {
                this.startDate = DATE_FORMAT.parse(startDateStr);
            } catch (ParseException e) {
                // Should never happen, date comes from a date selector UI
                throw new ArgumentNotValid("Invalid date specification", e);
            }
        }

        String endDateStr = UI_FIELD.END_DATE.getValue(req);
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
        
        jobIdRangeList = new ArrayList<String>();
        String jobIdRange = UI_FIELD.JOB_ID_RANGE.getValue(req);
        if(!jobIdRange.isEmpty()) {
	        try {
	        	String[] splittedRange = jobIdRange.replaceAll("\\s+","").split(",");
	        	for(String s : splittedRange) {
	        		if(s.contains("-")) {
	        			//if it's a range eg 11-27
	        			String[] range = s.split("-");
	        			if(range.length != 2) {
	        				throw new ArgumentNotValid("Invalid Job IDs range (1-10 or 1,2,3)");
	                    }
	        			//check if it's a number
	        			Long.parseLong(range[0]);
	        			Long.parseLong(range[1]);
	        		} else {
	        			//check if it's a number
	        			Long.parseLong(s);
	        		}
	        		jobIdRangeList.add(s);
	        	}
	        	this.jobIdRange = jobIdRange;
	        } catch (NumberFormatException e) {
	            this.jobIdRange = null;
	            throw new ArgumentNotValid("Job IDs must be digits", e);
	        }
        }

        String orderStr = UI_FIELD.JOB_ID_ORDER.getValue(req);
        this.sortingOrder = SORT_ORDER.parse(orderStr);
        if (this.sortingOrder == null) {
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

    /**
     * @return the selected job states as an array.
     */
    public JobStatus[] getSelectedJobStatuses() {
        return jobStatuses.toArray(new JobStatus[jobStatuses.size()]);
    }

    /**
     * @return the selected job states as a set..
     */
    public Set<JobStatus> getSelectedJobStatusesAsSet() {
        return jobStatuses;
    }

    /**
     * @return the harvest name.
     */
    public String getHarvestName() {
        if (harvestName == null) {
            return "";
        }
        return harvestName;
    }

    /**
     * Set the harvest name.
     *
     * @param harvestName The harvest name
     */
    public void setHarvestName(String harvestName) {
        this.harvestName = harvestName;
    }

    /**
     * @return the harvest ID.
     */
    public Long getHarvestId() {
        return harvestId;
    }

    /**
     * @return the harvest run number.
     */
    public Long getHarvestRunNumber() {
        return harvestRunNumber;
    }

    /**
     * @return the start date as milliseconds since Epoch or {@link HarvestStatusQuery#DATE_NONE} if start date is
     * undefined
     */
    public long getStartDate() {
        return (startDate == null ? DATE_NONE : startDate.getTime());
    }

    /**
     * @return the end date as milliseconds since Epoch, or {@link HarvestStatusQuery#DATE_NONE} if end date is
     * undefined
     */
    public long getEndDate() {
        return (endDate == null ? DATE_NONE : endDate.getTime());
    }

    /**
     * @return the job ids range as String
     */
    public String getJobIdRange() {
    	if(jobIdRange == null) {
    		return "";
    	}
		return jobIdRange;
	}

    /**
     * return only the ids or only the range
     * if isRange is true : 2,3,5-9,14-18 -> 5-9,14-18
     * if isRange is false : 2,3,5-9,14-18 -> 2,3
     * @return the job ids range as List, only the ids or only the ranges
     */
    public List<String> getPartialJobIdRangeAsList(boolean isRange) {
    	List<String> list = new ArrayList<String>();
    	for(String s : jobIdRangeList) {
    		if(s.contains("-") == isRange) {
    			list.add(s);
    		}
    	}
		return list;
	}

	/**
     * @return the start date as a string, or an empty string if start date is undefined
     */
    public String getStartDateAsString() {
        if (startDate == null) {
            return "";
        }
        return DATE_FORMAT.format(startDate);
    }

    /**
     * @return the end date as a string, or an empty string if end date is undefined
     */
    public String getEndDateAsString() {
        if (endDate == null) {
            return "";
        }
        return DATE_FORMAT.format(endDate);
    }

    /**
     * @return true, if the sorting order is Ascending, otherwise false.
     */
    public boolean isSortAscending() {
        return SORT_ORDER.ASC.equals(sortingOrder);
    }

    /**
     * @return the page size, i.e. the number of results on each page.
     */
    public long getPageSize() {
        return pageSize;
    }

    /**
     * Sets the page size.
     *
     * @param pageSize a number > 0.
     */
    public void setPageSize(long pageSize) {
        ArgumentNotValid.checkNotNegative(pageSize, "pageSize");
        this.pageSize = pageSize;
    }

    /**
     * @return the start page
     */
    public long getStartPageIndex() {
        return startPageIndex;
    }

    /**
     * Define whether or not the harvest name is case sensitive.
     *
     * @param isHarvestNameCaseSensitive If true, harvestname is case sensitive, otherwise not.
     */
    public void setCaseSensitiveHarvestName(boolean isHarvestNameCaseSensitive) {
        this.caseSensitiveHarvestName = isHarvestNameCaseSensitive;
    }

    /**
     * @return true, if the harvest name is case sensitive, otherwise false
     */
    public boolean getCaseSensitiveHarvestName() {
        return caseSensitiveHarvestName;
    }

    /**
     * Set the selected states in the query.
     *
     * @param chosenStates the set of selected states.
     */
    public void setJobStatus(Set<JobStatus> chosenStates) {
        this.jobStatuses = chosenStates;
    }
}
