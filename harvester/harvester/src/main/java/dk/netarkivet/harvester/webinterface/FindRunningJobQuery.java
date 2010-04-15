/* File:       $Id: FindRunningJobQuery.java 752 2009-03-05 18:09:21Z svc $
 * Revision:   $Revision: 752 $
 * Author:     $Author: svc $
 * Date:       $Date: 2009-03-05 19:09:21 +0100 (to, 05 mar 2009) $
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

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.servlet.ServletRequest;

import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.harvester.datamodel.DomainDAO;
import dk.netarkivet.harvester.datamodel.Job;
import dk.netarkivet.harvester.datamodel.JobDAO;
import dk.netarkivet.harvester.datamodel.JobStatus;
import dk.netarkivet.harvester.datamodel.JobStatusInfo;

/**
 * Represents a query for a job ID that would be set to harvest a given domain.
 *
 */
public class FindRunningJobQuery {

	
	/**
	 * Defines the UI fields and their default values.
	 */
	public static enum UI_FIELD {
		DOMAIN_NAME("");
		
		private String defaultValue;
		
		UI_FIELD(String defaultValue) {
			this.defaultValue = defaultValue;
		}
		
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
	
	public FindRunningJobQuery(ServletRequest req) {		
		domainName = UI_FIELD.DOMAIN_NAME.getValue(req);
		
		if (domainName == null || domainName.isEmpty()) {
			return;
		}
		
		if (! DomainDAO.getInstance().exists(domainName)) {
			throw new UnknownID("Domain " + domainName + " is not registered!");
		}
		
		List<JobStatusInfo> startedJobs = 
			JobDAO.getInstance().getStatusInfo(JobStatus.STARTED);
		for (JobStatusInfo jsi : startedJobs) {
			long jobId = jsi.getJobID();
			Job job = JobDAO.getInstance().read(jobId);
			Set<String> domains = job.getDomainConfigurationMap().keySet();
			if (domains.contains(domainName)) {
				runningJobIds.add(jobId);
			}
		}
	}

	public String getDomainName() {
		return domainName;
	}

	public Long[] getRunningJobIds() {
		return (Long[]) runningJobIds.toArray(new Long[runningJobIds.size()]);
	}
	
}
