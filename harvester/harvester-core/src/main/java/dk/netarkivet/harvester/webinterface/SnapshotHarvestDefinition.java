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

import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Provider;
import javax.servlet.ServletRequest;
import javax.servlet.jsp.PageContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.antiaction.raptor.dao.AttributeBase;
import com.antiaction.raptor.dao.AttributeTypeBase;

import dk.netarkivet.common.distribute.indexserver.IndexClientFactory;
import dk.netarkivet.common.distribute.indexserver.JobIndexCache;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.ForwardedToErrorPage;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.common.utils.I18n;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.webinterface.HTMLUtils;
import dk.netarkivet.harvester.HarvesterSettings;
import dk.netarkivet.harvester.datamodel.DomainDAO;
import dk.netarkivet.harvester.datamodel.FullHarvest;
import dk.netarkivet.harvester.datamodel.HarvestDefinition;
import dk.netarkivet.harvester.datamodel.HarvestDefinitionDAO;
import dk.netarkivet.harvester.datamodel.JobDAO;
import dk.netarkivet.harvester.datamodel.JobStatus;
import dk.netarkivet.harvester.datamodel.JobStatusInfo;
import dk.netarkivet.harvester.datamodel.dao.DAOProviderFactory;
import dk.netarkivet.harvester.datamodel.eav.EAV;
import dk.netarkivet.harvester.datamodel.eav.EAV.AttributeAndType;
import dk.netarkivet.harvester.datamodel.extendedfield.ExtendedFieldDAO;

/**
 * Contains utility methods for supporting GUI for updating snapshot harvests.
 */
public class SnapshotHarvestDefinition {
    protected static final Logger log = LoggerFactory.getLogger(SnapshotHarvestDefinition.class);
    private final Provider<HarvestDefinitionDAO> hdDaoProvider;
    private final Provider<JobDAO> jobDaoProvider;
    private final Provider<ExtendedFieldDAO> extendedFieldDAOProvider;
    private final Provider<DomainDAO> domainDAOProvider;
    private final Provider<EAV> eavDAOProvider;

    /**
     * Constructor.
     * @param hdDaoProvider Provider for HarvestDefinitions
     * @param jobDaoProvider Provider for Jobs
     * @param extendedFieldDAOProvider Provider ExtendedFields 
     * @param domainDAOProvider Provider for Domains
     */
    public SnapshotHarvestDefinition(Provider<HarvestDefinitionDAO> hdDaoProvider, Provider<JobDAO> jobDaoProvider,
            Provider<ExtendedFieldDAO> extendedFieldDAOProvider, Provider<DomainDAO> domainDAOProvider, Provider<EAV> eavDAOProvider) {
        this.hdDaoProvider = hdDaoProvider;
        this.jobDaoProvider = jobDaoProvider;
        this.extendedFieldDAOProvider = extendedFieldDAOProvider;
        this.domainDAOProvider = domainDAOProvider;
        this.eavDAOProvider = eavDAOProvider;
    }

    /**
     * 
     * @return a default SnapshotHarvestDefinition
     */
    public static SnapshotHarvestDefinition createSnapshotHarvestDefinitionWithDefaultDAOs() {
        return new SnapshotHarvestDefinition(DAOProviderFactory.getHarvestDefinitionDAOProvider(),
                DAOProviderFactory.getJobDAOProvider(), DAOProviderFactory.getExtendedFieldDAOProvider(),
                DAOProviderFactory.getDomainDAOProvider(), DAOProviderFactory.getEAVDAOProvider());
    }

    /**
     * Extracts all required parameters from the request, checks for any inconsistencies, and passes the requisite data
     * to the updateHarvestDefinition method for processing. If the "update" parameter is not set, this method does
     * nothing.
     * <p>
     * The parameters in the request are defined in Definitions-edit-snapshot-harvest.jsp.
     *
     * @param context The context of the web request.
     * @param i18n Translation information
     * @throws ForwardedToErrorPage if an error happened that caused a forward to the standard error page, in which case
     * further JSP processing should be aborted.
     */
    public void processRequest(PageContext context, I18n i18n) {
        ArgumentNotValid.checkNotNull(context, "PageContext context");
        ArgumentNotValid.checkNotNull(i18n, "I18n i18n");

        ServletRequest request = context.getRequest();
        if (request.getParameter(Constants.UPDATE_PARAM) == null) {
            return;
        }

        HTMLUtils.forwardOnEmptyParameter(context, Constants.HARVEST_PARAM);

        String oldname = request.getParameter(Constants.HARVEST_OLD_PARAM);
        if (oldname == null) {
            oldname = "";
        }
        String name = request.getParameter(Constants.HARVEST_PARAM);
        String comments = request.getParameter(Constants.COMMENTS_PARAM);

        long objectLimit = HTMLUtils.parseOptionalLong(context, Constants.DOMAIN_OBJECTLIMIT_PARAM,
                dk.netarkivet.harvester.datamodel.Constants.DEFAULT_MAX_OBJECTS);
        long byteLimit = HTMLUtils.parseOptionalLong(context, Constants.DOMAIN_BYTELIMIT_PARAM,
                dk.netarkivet.harvester.datamodel.Constants.DEFAULT_MAX_BYTES);
        long runningtimeLimit = HTMLUtils.parseOptionalLong(context, Constants.JOB_TIMELIMIT_PARAM,
                dk.netarkivet.harvester.datamodel.Constants.DEFAULT_MAX_JOB_RUNNING_TIME);

        Long oldHarvestId = HTMLUtils.parseOptionalLong(context, Constants.OLDSNAPSHOT_PARAM, null);

        if (oldHarvestId != null && !hdDaoProvider.get().exists(oldHarvestId)) {
            HTMLUtils.forwardWithErrorMessage(context, i18n, "errormsg;harvestdefinition.0.does.not.exist",
                    oldHarvestId);
            throw new ForwardedToErrorPage("Old harvestdefinition " + oldHarvestId + " does not exist");
        }

        FullHarvest hd;
        if ((request.getParameter(Constants.CREATENEW_PARAM) != null)) {
            if (hdDaoProvider.get().getHarvestDefinition(name) != null) {
                HTMLUtils.forwardWithErrorMessage(context, i18n, "errormsg;harvest.definition.0.already.exists", name);
                throw new ForwardedToErrorPage("Harvest definition '" + name + "' already exists");
            }
            // Note, object/bytelimit set to default values, if not set
            hd = new FullHarvest(name, comments, oldHarvestId, objectLimit, byteLimit, runningtimeLimit, false,
                    hdDaoProvider, jobDaoProvider, extendedFieldDAOProvider, domainDAOProvider);
            hd.setActive(false);
            hdDaoProvider.get().create(hd);
        } else {
            if (oldname.equals(name)) { // name is unchanged
                hd = (FullHarvest) hdDaoProvider.get().getHarvestDefinition(name);
            } else {
                // test that the name does not exist already
                if (hdDaoProvider.get().exists(name)) {
                    HTMLUtils.forwardWithErrorMessage(context, i18n, "errormsg;harvest.definition.0.already.exists", name);
                    throw new ForwardedToErrorPage("Harvest definition '" + name + "' already exists");
                } else {
                    hd = (FullHarvest) hdDaoProvider.get().getHarvestDefinition(oldname);
                    hd.setName(name);
                }
            }
            if (hd == null) {
                HTMLUtils.forwardWithErrorMessage(context, i18n, "errormsg;harvest.0.does.not.exist", name);
                throw new UnknownID("Harvest definition '" + name + "' doesn't exist!");
            }
            long edition = HTMLUtils.parseOptionalLong(context, Constants.EDITION_PARAM, Constants.NO_EDITION);

            if (hd.getEdition() != edition) {
                HTMLUtils.forwardWithRawErrorMessage(context, i18n, "errormsg;harvest.definition.changed.0.retry.1",
                        "<br/><a href=\"Definitions-edit-snapshot-harvest.jsp?" + Constants.HARVEST_PARAM + "="
                                + HTMLUtils.encodeAndEscapeHTML(name) + "\">", "</a>");

                throw new ForwardedToErrorPage("Harvest definition '" + name + "' has changed");
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
            hdDaoProvider.get().update(hd);
        }

        // EAV
        try {
        	Long entity_id = hd.getOid();
        	if (entity_id == null) {
        		entity_id = 0L;
        	}
        	EAV eav = eavDAOProvider.get();
            List<AttributeAndType> attributesAndTypes = eav.getAttributesAndTypes(EAV.SNAPSHOT_TREE_ID, (int)((long)entity_id));
            AttributeAndType attributeAndType;
            AttributeTypeBase attributeType;
            AttributeBase attribute;
            for (int i=0; i<attributesAndTypes.size(); ++i) {
            	attributeAndType = attributesAndTypes.get(i);
            	attributeType = attributeAndType.attributeType;
            	attribute = attributeAndType.attribute;
            	if (attribute == null) {
                	attribute = attributeType.instanceOf();
                	attribute.entity_id = (int)((long)entity_id);
            	}
            	switch (attributeType.viewtype) {
            	case 1:
                	long l = HTMLUtils.parseOptionalLong(context, attributeType.name, (long)attributeType.def_int);
                	attribute.setInteger((int)l);
            		break;
            	case 5:
            	case 6:
                    String paramValue = context.getRequest().getParameter(attributeType.name);
                    int intVal = 0;
                    if (paramValue != null && !"0".equals(paramValue)) {
                    	intVal = 1;
                    }
                	attribute.setInteger(intVal);
            		break;
            	}
            	eav.saveAttribute(attribute);
            }
        } catch (SQLException e) {
        	throw new RuntimeException("Unable to store EAV data!", e);
        }
    }
    

    /**
     * Flip the active status of a harvestdefinition named in the "flipactive" parameter.
     *
     * @param context The context of the web servlet
     * @param i18n Translation information
     * @return True if a harvest definition changed state.
     */
    public boolean flipActive(PageContext context, I18n i18n) {
        ArgumentNotValid.checkNotNull(context, "PageContext context");
        ArgumentNotValid.checkNotNull(i18n, "I18n i18n");

        ServletRequest request = context.getRequest();
        String flipactive = request.getParameter(Constants.FLIPACTIVE_PARAM);
        // Change activation if requested
        if (flipactive != null) {
            HarvestDefinition hd = hdDaoProvider.get().getHarvestDefinition(flipactive);
            if (hd != null) {
                boolean isActive = hd.getActive();
                boolean useDeduplication = Settings.getBoolean(HarvesterSettings.DEDUPLICATION_ENABLED);
                if (!isActive) {
                    if (hd instanceof FullHarvest) {
                        FullHarvest fhd = (FullHarvest) hd;
                        validatePreviousHd(fhd, context, i18n);
                        if (useDeduplication) {
                            // The client for requesting job index.
                            JobIndexCache jobIndexCache = IndexClientFactory.getDedupCrawllogInstance();
                            Long harvestId = fhd.getOid();
                            Set<Long> jobSet = hdDaoProvider.get().getJobIdsForSnapshotDeduplicationIndex(harvestId);
                            jobIndexCache.requestIndex(jobSet, harvestId);
                        } else {
                            // If deduplication disabled set indexReady to true
                            // right now, so the job generation can proceed.
                            fhd.setIndexReady(true);
                        }
                    } else { // hd is not Fullharvest
                        log.warn("Harvestdefinition #" + hd.getOid() + " is not a FullHarvest " + " but a "
                                + hd.getClass().getName());
                        return false;
                    }
                }
                hd.setActive(!hd.getActive());
                hdDaoProvider.get().update(hd);
                return true;
            } else {
                HTMLUtils.forwardWithErrorMessage(context, i18n, "errormsg;harvestdefinition.0.does.not.exist",
                        flipactive);
                throw new ForwardedToErrorPage("Harvest definition " + flipactive + " doesn't exist");
            }
        }
        return false;
    }

    /**
     * Validate the previous harvestDefinition of this FullHarvest. The validation checks, that the given hs arguments
     * represents a completed Fullharvest: Check 1: It has one or more jobs. Check 2: None of the jobs have status
     * NEW,SUBMITTED, or STARTED.
     *
     * @param hd A given FullHarvest
     * @param context The context of the web request.
     * @param i18n Translation information
     */
    private void validatePreviousHd(FullHarvest hd, PageContext context, I18n i18n) {
        HarvestDefinition preHd = hd.getPreviousHarvestDefinition();
        if (preHd == null) {
            return; // no validation needed
        }

        // This query represents check one
        HarvestStatusQuery hsq1 = new HarvestStatusQuery(preHd.getOid(), 1);
        // This query represents check two
        HarvestStatusQuery hsq2 = new HarvestStatusQuery(preHd.getOid(), 1);
        // States needed to update the query for check two.
        Set<JobStatus> chosenStates = new HashSet<JobStatus>();
        chosenStates.add(JobStatus.NEW);
        chosenStates.add(JobStatus.SUBMITTED);
        chosenStates.add(JobStatus.STARTED);
        hsq2.setJobStatus(chosenStates);
        HarvestStatus hs1 = jobDaoProvider.get().getStatusInfo(hsq1);
        HarvestStatus hs2 = jobDaoProvider.get().getStatusInfo(hsq2);
        if (hs1.getJobStatusInfo().isEmpty() || !hs2.getJobStatusInfo().isEmpty()) {
            if (hs1.getJobStatusInfo().isEmpty()) {
                log.debug("Cannot base snapshot job on old job, because no jobs generated for " + preHd.getName());
            }
            if (!hs2.getJobStatusInfo().isEmpty()) {
                for (JobStatusInfo jobStatusInfo: hs2.getJobStatusInfo()) {
                     log.debug("Cannot activate new jobs for {} because found job {} in state  {}.", hd.getName(), jobStatusInfo.getJobID(),jobStatusInfo.getStatus().name());
                }
            }
            HTMLUtils.forwardWithErrorMessage(context, i18n, "errormsg;harvestdefinition.0.is.based.on."
                    + "unfinished.definition.1", hd.getName(), preHd.getName());
            throw new ForwardedToErrorPage("Harvest definition " + hd.getName() + " is based on unfinished definition "
                    + preHd.getName());
        }
    }
}
