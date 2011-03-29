/* File:             $Id$
 * Revision:         $Revision$
 * Author:           $Author$
 * Date:             $Date$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2010 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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
package dk.netarkivet.harvester.datamodel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Document;
import org.dom4j.Node;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.utils.Named;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.harvester.HarvesterSettings;
import dk.netarkivet.harvester.harvesting.HeritrixLauncher;

/**
 * This abstract class models the general properties of a harvest definition,
 * i.e. object id , name, comments, and submission date
 * <p/>
 * The specializing classes FullHarvest and PartielHarvest contains the specific
 * properties and operations of snapshot harvestdefinitions and all other kinds
 * of harvestdefinitions, respectively.
 * <p/>
 * Methods exist to generate jobs from this harvest definition.
 *
 */
public abstract class HarvestDefinition implements Named {
    protected Long oid;
    protected String harvestDefName;
    /**
     * The time this harvest definition was first written.
     */
    protected Date submissionDate;
    protected String comments;

    /**
     * Edition is used by the DAO to keep track of changes.
     */
    protected long edition = -1;

    /** Determines if the harvest definition is active and ready
     * for scheduling. When true the jobs should be scheduled
     * otherwise the scheduler should ignore the definition.
     * Initially a definition is assumed active - the original behaviour
     * before the isActive flag was introduced.
     * */
    protected boolean isActive = true;

    /**
     * The number of times this event has already run.
     */
    protected int numEvents;

    /**
     * How many domain configurations to process at a time.
     */
    private final long MAX_CONFIGS_PER_JOB_CREATION =
            Settings.getLong(HarvesterSettings.MAX_CONFIGS_PER_JOB_CREATION);

    /** Is deduplication enabled or disabled. **/
    private final boolean DEDUPLICATION_ENABLED =
        Settings.getBoolean(HarvesterSettings.DEDUPLICATION_ENABLED);
    
    /** Logger for this class. */
    private Log log = LogFactory.getLog(getClass());

    /**
     * Create new instance of a PartialHavest configured according
     * to the properties of the supplied DomainConfiguration.
     *
     * @param domainConfigurations a list of domain configurations
     * @param schedule             the harvest definition schedule
     * @param harvestDefName       the name of the harvest definition
     * @param comments             comments
     * @return the newly created PartialHarvest
     */
    public static PartialHarvest createPartialHarvest(
            List<DomainConfiguration> domainConfigurations,
            Schedule schedule,
            String harvestDefName,
            String comments) {

        return new PartialHarvest(domainConfigurations,
                                  schedule,
                                  harvestDefName,
                                  comments);
    }

    /**
     * Create snapshot harvestdefinition.
     * A snapshot harvestdefinition creates jobs for all domains,
     * using the default configuration for each domain.
     * The HarvestDefinition is scheduled to run once as soon as possible.
     * <p/>
     * When a previous harvest definition is supplied, only domains not
     * completely harvested by the previous harvestdefinition are included
     * in this harvestdefinition.
     *
     * @param harvestDefName  the name of the harvest definition
     * @param comments        description of the harvestdefinition
     * @param prevHarvestOid  an id of a previous harvest to use as
     *                        basis for this definition, ignored when null.
     * @param maxCountObjects the maximum number of objects harvested from
     *                        any domain
     * @param maxBytes        the maximum number of bytes harvested from
     *                        any domain  
     * @param maxJobRunningTime The maximum running time for each job                       
     * @return a snapshot harvestdefinition
     */
    public static FullHarvest createFullHarvest(String harvestDefName,
                                                String comments,
                                                Long prevHarvestOid,
                                                long maxCountObjects,
                                                long maxBytes,
                                                long maxJobRunningTime) {

        return new FullHarvest(harvestDefName, comments,
                               prevHarvestOid, maxCountObjects, 
                               maxBytes, maxJobRunningTime);
    }


    /**
     * Set the object ID of this harvest definition.
     *
     * @param oid The oid
     * @throws ArgumentNotValid if the oid is null
     */
    public void setOid(Long oid) {
        ArgumentNotValid.checkNotNull(oid, "oid");
        this.oid = oid;
    }

    /**
     * Return the object ID of this harvest definition.
     *
     * @return The object id, or null if none.
     */
    public Long getOid() {
        return oid;
    }

    /** Check if this harvestdefinition has an ID set yet (doesn't happen until
     * the DBDAO persists it).
     * @return true, if this harvestdefinition has an ID set
     */
    boolean hasID() {
        return oid != null;
    }

    /**
     * Set the submission date.
     *
     * @param submissionDate the time when the harvestdefinition was created
     */
    public void setSubmissionDate(Date submissionDate) {
        this.submissionDate = submissionDate;
    }

    /**
     * Returns the submission date.
     *
     * @return the submission date
     */
    public Date getSubmissionDate() {
        return submissionDate;
    }

    /**
     * Returns the name of the harvest definition.
     *
     * @return the harvest definition name
     */
    public String getName() {
        return harvestDefName;
    }

    /**
     * Returns the comments for this harvest definition.
     * @return the comments for this harvest definition.
     */
    public String getComments() {
        return comments;
    }

    /**
     * Set the comments for this harvest definition.
     *
     * @param comments A user-entered string.
     */
    public void setComments(String comments) {
        ArgumentNotValid.checkNotNull(comments, "comments");
        this.comments = comments;
    }

    /**
     * Get the edition number.
     * @return The edition number
     */
    public long getEdition() {
        return edition;
    }

    /**
     * Set the edition number.
     *
     * @param theEdition the new edition of the harvestdefinition
     */
    public void setEdition(long theEdition) {
        edition = theEdition;
    }


    /**
     * Get the number of times this harvest definition has been run so far.
     *
     * @return That number
     */
    public int getNumEvents() {
        return numEvents;
    }

    /**
     * Set the number of times this harvest definition has been run so far.
     *
     * @param numEvents The number.
     * @throws ArgumentNotValid if numEvents is negative
     */
    void setNumEvents(int numEvents) {
        ArgumentNotValid.checkNotNegative(numEvents, "numEvents");
        this.numEvents = numEvents;
    }

    /**
     * Set's activation status. Only active harvestdefinitions should
     * be scheduled.
     * @param active new activation status
     */
    public void setActive(boolean active) {
        isActive = active;
    }

    /**
     * Returns the activation status.
     * @return activation status
     */
    public boolean getActive() {
        return isActive;
    }

    /**
     * Returns a iterator of domain configurations for this harvest definition.
     *
     * @return Iterator containing information about the domain configurations
     */
    public abstract Iterator<DomainConfiguration> getDomainConfigurations();


    /**
     * Create Jobs from the configurations in this harvestdefinition
     * and the current value of the limits in Settings.
     * The following values are used:
     * dk.netarkivet.datamodel.jobs.maxRelativeSizeDifference:
     * The maximum relative difference between the smallest and largest
     * number of objects expected in a job
     * <p/>
     * dk.netarkivet.datamodel.jobs.minAbsolutSizeDifference
     * Size differences below this threshold are ignored even if
     * the relative difference exceeds maxRelativeSizeDifference
     * <p/>
     * dk.netarkivet.datamodel.jobs.maxTotalSize
     * The upper limit on the total number of objects that a job may
     * retrieve
     *
     * @return The number of jobs created
     */
    public int createJobs() {
        int jobsMade = 0;
        final Iterator<DomainConfiguration> domainConfigurations
                = getDomainConfigurations();

        while (domainConfigurations.hasNext()) {
            List<DomainConfiguration> smallerList
                    = new ArrayList<DomainConfiguration>();
            while (domainConfigurations.hasNext()
                   && smallerList.size() < MAX_CONFIGS_PER_JOB_CREATION) {
                smallerList.add(domainConfigurations.next());
            }
            Collections.sort(smallerList, new CompareConfigsDesc(
                    getMaxCountObjects(),
                    getMaxBytes()));
            jobsMade += makeJobs(smallerList.iterator());
        }
        setNumEvents(numEvents + 1);
        return jobsMade;
    }


    /**
     * Create new jobs from a collection of configurations.
     * All configurations must use the same order.xml file.
     *
     * @param cfglist the configurations to use to create the jobs
     * @return The number of jobs created
     * @throws ArgumentNotValid if any of the parameters is null
     *                          or if the cfglist does not contain any
     *                          configurations
     */
    protected int makeJobs(Iterator<DomainConfiguration> cfglist) {
        int jobsMade = 0;
        Job job = null;
        JobDAO dao = JobDAO.getInstance();
        while (cfglist.hasNext()) {
            DomainConfiguration cfg = cfglist.next();
            // Do we need to create a new Job or is the current job ok
            if ((job == null) || (!job.canAccept(cfg))) {
                if (job != null) {
                    // If we're done with a job, write it out
                    jobsMade++;
                    dao.create(job);
                }
                job = getNewJob(cfg);
            } else {
                job.addConfiguration(cfg);
            }
        }
        if (job != null) {
            jobsMade++;
            Document doc = job.getOrderXMLdoc();
            if (DEDUPLICATION_ENABLED) {
               // Check that the Deduplicator element is present in the 
               //OrderXMl and enabled. If missing or disabled log a warning
                if (!HeritrixLauncher.isDeduplicationEnabledInTemplate(doc)) {
                    log.warn("Unable to perform deduplication for this job" 
                            + " as the required DeDuplicator element is "
                            + "disabled or missing from template");
                }
            } else {
                // Remove deduplicator Element from OrderXML if present
                Node xpathNode = doc.selectSingleNode(
                        HeritrixTemplate.DEDUPLICATOR_XPATH);
                if (xpathNode != null) {
                    xpathNode.detach();
                    job.setOrderXMLDoc(doc);
                    log.info("Removed DeDuplicator element because " 
                            + "Deduplication is disabled");
                } 
            }
            dao.create(job);
            log.debug("Generated job: '" + job.toString() + "'");
            if (log.isDebugEnabled()) {
                StringBuilder logMsg
                        = new StringBuilder("Job configurations:");
                for(Map.Entry<String, String> config
                        : job.getDomainConfigurationMap().entrySet()) {
                    logMsg.append("\n ")
                            .append(config.getKey())
                            .append(":")
                            .append(config.getValue());
                }
                log.debug(logMsg);
            }
        }
        return jobsMade;
    }

    /**
     * Get a new Job suited for this type of HarvestDefinition.
     *
     * @param cfg The configuration to use when creating the job
     * @return a new job
     */
    protected abstract Job getNewJob(DomainConfiguration cfg);

    /**
     * Return a human-readable string representation of this object.
     * @return A human-readable string representation of this object
     */
    public String toString() {
        return "HD #" + oid + ": '" + getName() + "'";
    }

    /**
     * Tests whether some other object is "equal to" this HarvestDefinition.
     * Cfr. documentation of java.lang.Object.equals()
     *
     * @param o
     * @return True or false, indicating equality.
     */
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof HarvestDefinition)) return false;

        final HarvestDefinition harvestDefinition = (HarvestDefinition) o;

        if (!comments.equals(harvestDefinition.comments)) return false;
        if (!harvestDefName.equals(harvestDefinition.harvestDefName))
            return false;
        if (oid != null ? !oid.equals(harvestDefinition.oid)
                : harvestDefinition.oid != null) return false;

        return true;
    }

    /**
     * Returns a hashcode of this object generated on fields oid,
     * harvestDefName, and comments.
     *
     * @return the hashCode
     */
    public int hashCode() {
        int result;
        result = (oid != null ? oid.hashCode() : 0);
        result = 29 * result + harvestDefName.hashCode();
        result = 29 * result + comments.hashCode();
        return result;
    }

    /**
     * Check if this harvest definition should be run, given the time now.
     *
     * @param now The current time
     * @return true if harvest definition should be run
     */
    public abstract boolean runNow(Date now);

    /**
     * Used to check if a harvestdefinition is a snapshot harvestdefinition.
     *
     * @return true if this harvestdefinition defines a snapshot harvest
     */
    public abstract boolean isSnapShot();

    /** Returns how many objects to harvest per domain, or 0 for no limit.
     * @return how many objects to harvest per domain
     */
    protected abstract long getMaxCountObjects();

    /** Returns how many bytes to harvest per domain, or -1 for no limit.
     * @return how many bytes to harvest per domain
     */
    protected abstract long getMaxBytes();

    /**
     * Compare two configurations using the following order:
     * 1) Harvest template
     * 2) Byte limit
     * 3) expected number of object a harvest of the configuration will produce.
     * The comparison will put the largest configuration first (with respect
     * to 2) and 3))
     */
    private static class CompareConfigsDesc
            implements Comparator<DomainConfiguration> {
        private long objectLimit;
        private long byteLimit;

        CompareConfigsDesc(long objectLimit, long byteLimit) {
            this.objectLimit = objectLimit;
            this.byteLimit = byteLimit;
        }

        public int compare(DomainConfiguration cfg1, DomainConfiguration cfg2) {
            //Compare order xml names
            int cmp = cfg1.getOrderXmlName().compareTo(cfg2.getOrderXmlName());
            if (cmp != 0) {
                return cmp;
            }

            //Compare byte limits
            long bytelimit1 = NumberUtils.minInf(cfg1.getMaxBytes(), byteLimit);
            long bytelimit2 = NumberUtils.minInf(cfg2.getMaxBytes(), byteLimit);
            cmp = NumberUtils.compareInf(bytelimit2, bytelimit1);
            if (cmp != 0) {
                return cmp;
            }

            //Compare expected sizes
            long expectedsize1 = cfg1.getExpectedNumberOfObjects(objectLimit,
                                                                 byteLimit);
            long expectedsize2 = cfg2.getExpectedNumberOfObjects(objectLimit,
                                                                 byteLimit);
            long res = expectedsize2 - expectedsize1;
            if (res != 0L) {
                return res < 0L ? -1 : 1;
            }

            return 0;
        }
    }
}
