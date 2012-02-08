/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2012 The Royal Danish Library, the Danish State and
 * University Library, the National Library of France and the Austrian
 * National Library.
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

import java.util.HashSet;
import java.util.Set;

import javax.servlet.ServletRequest;
import javax.servlet.jsp.PageContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.distribute.indexserver.IndexClientFactory;
import dk.netarkivet.common.distribute.indexserver.JobIndexCache;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.ForwardedToErrorPage;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.common.utils.I18n;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.webinterface.HTMLUtils;
import dk.netarkivet.harvester.HarvesterSettings;
import dk.netarkivet.harvester.datamodel.FullHarvest;
import dk.netarkivet.harvester.datamodel.HarvestDefinition;
import dk.netarkivet.harvester.datamodel.HarvestDefinitionDAO;
import dk.netarkivet.harvester.datamodel.JobDAO;
import dk.netarkivet.harvester.datamodel.JobStatus;

/**
 * Contains utility methods for supporting GUI for updating snapshot harvests.
 */
public final class SnapshotHarvestDefinition {
    
    
    /** The logger to use.    */
    protected static final Log log = LogFactory.getLog(
            SnapshotHarvestDefinition.class.getName());
    
    /** Default private constructor to avoid class being instantiated. */
    private SnapshotHarvestDefinition() {

    }

    
    
    /**
     * Extracts all required parameters from the request, checks for
     * any inconsistencies, and passes the requisite data to the
     * updateHarvestDefinition method for processing.  If the "update"
     * parameter is not set, this method does nothing.
     *
     * The parameters in the request are defined in
     * Definitions-edit-snapshot-harvest.jsp.
     *
     * @param context The context of the web request.
     * @param i18n Translation information
     * @throws ForwardedToErrorPage if an error happened that caused a forward
     * to the standard error page, in which case further JSP processing should
     * be aborted.
     */
    public static void processRequest(PageContext context, I18n i18n) {
        ArgumentNotValid.checkNotNull(context, "PageContext context");
        ArgumentNotValid.checkNotNull(i18n, "I18n i18n");

        ServletRequest request = context.getRequest();
        if (request.getParameter(Constants.UPDATE_PARAM) == null) {
            return;
        }

        HTMLUtils.forwardOnEmptyParameter(context, Constants.HARVEST_PARAM);

        String name = request.getParameter(Constants.HARVEST_PARAM);
        String comments = request.getParameter(Constants.COMMENTS_PARAM);

        long objectLimit = HTMLUtils.parseOptionalLong(context,
                Constants.DOMAIN_OBJECTLIMIT_PARAM, dk.netarkivet.harvester
                .datamodel.Constants.DEFAULT_MAX_OBJECTS);
        long byteLimit = HTMLUtils.parseOptionalLong(context,
                Constants.DOMAIN_BYTELIMIT_PARAM,
                dk.netarkivet.harvester.datamodel.Constants.DEFAULT_MAX_BYTES);
        long runningtimeLimit = HTMLUtils.parseOptionalLong(context,
                    Constants.JOB_TIMELIMIT_PARAM,
                    dk.netarkivet.harvester.datamodel.Constants.DEFAULT_MAX_JOB_RUNNING_TIME);

        Long oldHarvestId = HTMLUtils.parseOptionalLong(context,
                Constants.OLDSNAPSHOT_PARAM, null);

        HarvestDefinitionDAO hddao = HarvestDefinitionDAO.getInstance();
        if (oldHarvestId != null && !hddao.exists(oldHarvestId)) {
            HTMLUtils.forwardWithErrorMessage(context, i18n,
                    "errormsg;harvestdefinition.0.does.not.exist",
                    oldHarvestId);
            throw new ForwardedToErrorPage("Old harvestdefinition "
                    + oldHarvestId + " does not exist");
        }

        FullHarvest hd;
        if ((request.getParameter(Constants.CREATENEW_PARAM) != null)) {
            if (hddao.getHarvestDefinition(name) != null) {
                HTMLUtils.forwardWithErrorMessage(context, i18n,
                        "errormsg;harvest.definition.0.already.exists", name);
                throw new ForwardedToErrorPage("Harvest definition '" + name
                        + "' already exists");
            }
            // Note, object/bytelimit set to default values, if not set
            hd = new FullHarvest(name, comments, oldHarvestId, objectLimit,
                                 byteLimit, runningtimeLimit, false);
            hd.setActive(false);
            hddao.create(hd);
        } else {
            hd = (FullHarvest) hddao.getHarvestDefinition(name);
            if (hd == null) {
                HTMLUtils.forwardWithErrorMessage(context, i18n,
                        "errormsg;harvest.0.does.not.exist", name);
                throw new UnknownID("Harvest definition '" + name
                        + "' doesn't exist!");
            }
            long edition = HTMLUtils.parseOptionalLong(context,
                    Constants.EDITION_PARAM, Constants.NO_EDITION);

            if (hd.getEdition() != edition) {
                HTMLUtils.forwardWithRawErrorMessage(context, i18n,
                        "errormsg;harvest.definition.changed.0.retry.1",
                        "<br/><a href=\"Definitions-edit-snapshot-harvest.jsp?"
                                + Constants.HARVEST_PARAM + "="
                                + HTMLUtils.encodeAndEscapeHTML(name)
                                + "\">",
                        "</a>");

                throw new ForwardedToErrorPage("Harvest definition '" + name
                        + "' has changed");
            }

            // MaxBytes is set to
            // dk.netarkivet.harvester.datamodel.Constants.DEFAULT_MAX_BYTES
            // if parameter snapshot_byte_Limit is not defined
            hd.setMaxBytes(byteLimit);

            // MaxCountObjects is set to
            // dk.netarkivet.harvester.datamodel.Constants.DEFAULT_MAX_OBJECTS
            // if parameter snapshot_object_limit is not defined
            hd.setMaxCountObjects(objectLimit);
            
            // MaxJobRunningTime is set to
            // dk.netarkivet.harvester.datamodel.Constants.DEFAULT_MAX_JOB_RUNNING_TIME
            // if parameter snapshot_time_limit is not defined
            hd.setMaxJobRunningTime(runningtimeLimit);
            
            hd.setPreviousHarvestDefinition(oldHarvestId);
            hd.setComments(comments);
            hddao.update(hd);
        }
    }

    /** Flip the active status of a harvestdefinition named in the
     * "flipactive" parameter.
     *
     * @param context The context of the web servlet
     * @param i18n Translation information
     * @return True if a harvest definition changed state.
     */
    public static boolean flipActive(PageContext context, I18n i18n) {
        ArgumentNotValid.checkNotNull(context, "PageContext context");
        ArgumentNotValid.checkNotNull(i18n, "I18n i18n");

        ServletRequest request = context.getRequest();
        String flipactive = request.getParameter(Constants.FLIPACTIVE_PARAM);
        // Change activation if requested
        if (flipactive != null) {
            HarvestDefinitionDAO dao = HarvestDefinitionDAO.getInstance();
            HarvestDefinition hd = dao.getHarvestDefinition(flipactive);
            if (hd != null) {
                boolean isActive = hd.getActive();
                boolean useDeduplication = Settings.getBoolean(
                        HarvesterSettings.DEDUPLICATION_ENABLED);
                if (!isActive) {
                    if (hd instanceof FullHarvest) {
                        FullHarvest fhd = (FullHarvest) hd;
                        validatePreHd(fhd, context, i18n);
                        if (useDeduplication) {
                            // The client for requesting job index.
                            JobIndexCache jobIndexCache
                                = IndexClientFactory.getDedupCrawllogInstance();
                            Long harvestId = fhd.getOid();
                            Set<Long> jobSet 
                                = dao.getJobIdsForSnapshotDeduplicationIndex(
                                        harvestId);
                            jobIndexCache.requestIndex(jobSet, harvestId);
                        } else {
                            // If deduplication disabled set indexReady to true 
                            // right now, so the job generation can proceed.
                            fhd.setIndexReady(true);
                        }
                    } else { // hd is not Fullharvest
                        log.warn("Harvestdefinition #" + hd.getOid() 
                                + " is not a FullHarvest "
                                + " but a " + hd.getClass().getName());
                        return false;
                    }
                } 
                hd.setActive(!hd.getActive());
                dao.update(hd);
                return true;
            } else {
                HTMLUtils.forwardWithErrorMessage(context, i18n,
                        "errormsg;harvestdefinition.0.does.not.exist",
                        flipactive);
                throw new ForwardedToErrorPage("Harvest definition "
                        + flipactive + " doesn't exist");
            }
        }
        return false;
    }

    /**
     * Validate the previous harvestDefinition of this FullHarvest.
     * The validation checks, that the given hs arguments represents a
     * completed Fullharvest: 
     * Check 1: It has one or more jobs. 
     * Check 2: None of the jobs have status NEW,SUBMITTED, or STARTED.
     * @param hd A given FullHarvest
     * @param context The context of the web request.
     * @param i18n Translation information
     */
    private static void validatePreHd(FullHarvest hd, 
            PageContext context, I18n i18n) {
        HarvestDefinition preHd = hd.getPreviousHarvestDefinition();
        if (preHd == null) {
            return; // no validation needed
        }
        
        JobDAO dao = JobDAO.getInstance();
        // This query represents check one
        HarvestStatusQuery hsq1 = new HarvestStatusQuery(preHd.getOid(), 0);
        // This query represents check two
        HarvestStatusQuery hsq2 = new HarvestStatusQuery(preHd.getOid(), 0);
        // States needed to update the query for check two.
        Set<JobStatus> chosenStates = new HashSet<JobStatus>();
        chosenStates.add(JobStatus.NEW);
        chosenStates.add(JobStatus.SUBMITTED);
        chosenStates.add(JobStatus.STARTED);
        hsq2.setJobStatus(chosenStates);
        HarvestStatus hs1 = dao.getStatusInfo(hsq1);
        HarvestStatus hs2 = dao.getStatusInfo(hsq2);
        if (hs1.getJobStatusInfo().isEmpty() 
                || !hs2.getJobStatusInfo().isEmpty()) {
            HTMLUtils.forwardWithErrorMessage(context, i18n,
                "errormsg;harvestdefinition.0.is.based.on."
                + "unfinished.definition.1",
                hd.getName(), preHd.getName());
            throw new ForwardedToErrorPage("Harvest definition "
                + hd.getName() 
                + " is based on unfinished definition " 
                + preHd.getName());
        }
    }
}
