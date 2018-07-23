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

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.servlet.ServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.harvester.datamodel.DomainDAO;
import dk.netarkivet.harvester.datamodel.Job;
import dk.netarkivet.harvester.datamodel.JobDAO;
import dk.netarkivet.harvester.datamodel.JobStatus;
import dk.netarkivet.harvester.datamodel.JobStatusInfo;

/**
 * Represents a query for job IDs that would be set to harvest a given domain.
 */
public class FindRunningJobQuery {

	static final Logger log = LoggerFactory.getLogger(FindRunningJobQuery.class);
	
    /**
     * Defines the UI fields and their default values.
     */
    public static enum UI_FIELD {
        DOMAIN_NAME("");

        private String defaultValue;

        UI_FIELD(String defaultValue) {
            this.defaultValue = defaultValue;
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

        public Object getDefaultValue() {
            return defaultValue;
        }

    }

    /**
     * The domain name being searched for.
     */
    private String domainName;

    /**
     * The currently running job harvesting the given domain.
     */
    private Set<Long> runningJobIds = new TreeSet<Long>();

    /**
     * Builds a request to find a running job. UI field values will be extracted from the given {@link ServletRequest}.
     *
     * @param req the {@link ServletRequest} to parse.
     */
    public FindRunningJobQuery(ServletRequest req) {
        domainName = UI_FIELD.DOMAIN_NAME.getValue(req);

        if (domainName == null || domainName.isEmpty()) {
            return;
        }

        if (!DomainDAO.getInstance().exists(domainName)) {
            throw new UnknownID("Domain " + domainName + " is not registered!");
        }

        List<JobStatusInfo> startedJobs = JobDAO.getInstance().getStatusInfo(JobStatus.STARTED);
        for (JobStatusInfo jsi : startedJobs) {
            long jobId = jsi.getJobID();
            Job job = JobDAO.getInstance().read(jobId);
            Set<String> domains = job.getDomainConfigurationMap().keySet();
            if (domains.contains(domainName)) {
                runningJobIds.add(jobId);
            }
            log.info("Found {} jobs in status STARTED harvesting domain {}", runningJobIds.size(), domainName);
        }
    }
    /**
     * 
     * @param jobId The jobId to match in the list of runningJobIds. 
     * @return true, if the given jobId is contained in list of runningJobIds, otherwise false
     */
    public boolean found(Long jobId) {
        return runningJobIds.contains(jobId);
    }

    /**
     * @return the domain name to search for.
     */
    public String getDomainName() {
        return domainName;
    }

    /**
     * @return the IDs of the currently running jobs whose configurations include the given domain.
     */
    public Long[] getRunningJobIds() {
        return (Long[]) runningJobIds.toArray(new Long[runningJobIds.size()]);
    }

}
