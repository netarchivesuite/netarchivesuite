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
package dk.netarkivet.harvester.datamodel;

import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.inject.Provider;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.utils.Named;
import dk.netarkivet.harvester.datamodel.dao.DAOProviderFactory;
import dk.netarkivet.harvester.datamodel.extendedfield.ExtendableEntity;
import dk.netarkivet.harvester.datamodel.extendedfield.ExtendedFieldDAO;
import dk.netarkivet.harvester.datamodel.extendedfield.ExtendedFieldTypes;
import dk.netarkivet.harvester.datamodel.extendedfield.ExtendedFieldValue;

/**
 * This abstract class models the general properties of a harvest definition, i.e. object id , name, comments, and
 * submission date
 * <p>
 * The specializing classes FullHarvest and PartielHarvest contains the specific properties and operations of snapshot
 * harvestdefinitions and all other kinds of harvestdefinitions, respectively.
 * <p>
 * Methods exist to generate jobs from this harvest definition.
 */
public abstract class HarvestDefinition extends ExtendableEntity implements Named {

    protected Long oid;
    protected String harvestDefName;
    /** The intended audience for the harvest. */
    protected String audience;

    /** The time this harvest definition was first written. */
    protected Date submissionDate;
    protected String comments;

    /** Edition is used by the DAO to keep track of changes. */
    protected long edition = -1;

    /**
     * Determines if the harvest definition is active and ready for scheduling. When true the jobs should be scheduled
     * otherwise the scheduler should ignore the definition. Initially a definition is assumed active - the original
     * behaviour before the isActive flag was introduced.
     */
    protected boolean isActive = true;

    /** The number of times this event has already run. */
    protected int numEvents;

    /** The id of the associated harvest channel, or null if the default one is to be used. */
    protected Long channelId;

    protected HarvestDefinition(Provider<ExtendedFieldDAO> extendedFieldDAO) {
        super(extendedFieldDAO);
    }

    /**
     * Create new instance of a PartialHavest configured according to the properties of the supplied
     * DomainConfiguration.
     *
     * @param domainConfigurations a list of domain configurations
     * @param schedule the harvest definition schedule
     * @param harvestDefName the name of the harvest definition
     * @param comments comments
     * @return the newly created PartialHarvest
     */
    public static PartialHarvest createPartialHarvest(List<DomainConfiguration> domainConfigurations,
            Schedule schedule, String harvestDefName, String comments, String audience) {

        return new PartialHarvest(domainConfigurations, schedule, harvestDefName, comments, audience);
    }

    /**
     * Create snapshot harvestdefinition. A snapshot harvestdefinition creates jobs for all domains, using the default
     * configuration for each domain. The HarvestDefinition is scheduled to run once as soon as possible.
     * <p>
     * When a previous harvest definition is supplied, only domains not completely harvested by the previous
     * harvestdefinition are included in this harvestdefinition. indexready set to false.
     *
     * @param harvestDefName the name of the harvest definition
     * @param comments description of the harvestdefinition
     * @param prevHarvestOid an id of a previous harvest to use as basis for this definition, ignored when null.
     * @param maxCountObjects the maximum number of objects harvested from any domain
     * @param maxBytes the maximum number of bytes harvested from any domain
     * @param maxJobRunningTime The maximum running time for each job
     * @return a snapshot harvestdefinition
     */
    public static FullHarvest createFullHarvest(String harvestDefName, String comments, Long prevHarvestOid,
            long maxCountObjects, long maxBytes, long maxJobRunningTime) {

        return new FullHarvest(harvestDefName, comments, prevHarvestOid, maxCountObjects, maxBytes, maxJobRunningTime,
                false, DAOProviderFactory.getHarvestDefinitionDAOProvider(), DAOProviderFactory.getJobDAOProvider(),
                DAOProviderFactory.getExtendedFieldDAOProvider(), DAOProviderFactory.getDomainDAOProvider());
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

    /**
     * Check if this harvestdefinition has an ID set yet (doesn't happen until the DBDAO persists it).
     *
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
     *
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
     *
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
    public void setNumEvents(int numEvents) {
        ArgumentNotValid.checkNotNegative(numEvents, "numEvents");
        this.numEvents = numEvents;
    }

    /**
     * Set's activation status. Only active harvestdefinitions should be scheduled.
     *
     * @param active new activation status
     */
    public void setActive(boolean active) {
        isActive = active;
    }

    /**
     * Returns the activation status.
     *
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
     * Return a human-readable string representation of this object.
     *
     * @return A human-readable string representation of this object
     */
    public String toString() {
        return "HD #" + oid + ": '" + getName() + "'";
    }

    /**
     * Tests whether some other object is "equal to" this HarvestDefinition. Cfr. documentation of
     * java.lang.Object.equals()
     *
     * @param o
     * @return True or false, indicating equality.
     */
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof HarvestDefinition)) {
            return false;
        }

        final HarvestDefinition harvestDefinition = (HarvestDefinition) o;

        if (!comments.equals(harvestDefinition.comments)) {
            return false;
        }
        if (!harvestDefName.equals(harvestDefinition.harvestDefName)) {
            return false;
        }
        if (oid != null ? !oid.equals(harvestDefinition.oid) : harvestDefinition.oid != null) {
            return false;
        }

        if ((extendedFieldValues == null && harvestDefinition.getExtendedFieldValues() != null)
                || (extendedFieldValues != null && harvestDefinition.getExtendedFieldValues() == null)) {
            return false;
        }

        if (extendedFieldValues != null && harvestDefinition.getExtendedFieldValues() != null) {
            if (extendedFieldValues.size() != harvestDefinition.getExtendedFieldValues().size()) {
                return false;
            }

            for (int i = 0; i < extendedFieldValues.size(); i++) {
                ExtendedFieldValue e1 = extendedFieldValues.get(i);
                ExtendedFieldValue e2 = harvestDefinition.getExtendedFieldValues().get(i);

                if ((e1 == null && e2 != null) || (e1 != null && e2 == null)) {
                    return false;
                }

                if (e1 != null && e2 != null) {
                    if (!e1.equals(e2)) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    /**
     * Returns a hashcode of this object generated on fields oid, harvestDefName, and comments.
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

    /**
     * Returns how many objects to harvest per domain, or 0 for no limit.
     *
     * @return how many objects to harvest per domain
     */
    public abstract long getMaxCountObjects();

    /**
     * Returns how many bytes to harvest per domain, or -1 for no limit.
     *
     * @return how many bytes to harvest per domain
     */
    public abstract long getMaxBytes();

    /**
     * @return the intended audience for this harvest.
     */
    public String getAudience() {
        return this.audience;
    }

    /**
     * Set the audience.
     *
     * @param audience the audience.
     */
    public void setAudience(String audience) {
        this.audience = audience;
    }

    public Long getChannelId() {
        return channelId;
    }

    protected void setChannelId(Long channelId) {
        this.channelId = channelId;
    }

    /**
     * All derived classes allow ExtendedFields from Type ExtendedFieldTypes.HARVESTDEFINITION
     *
     * @return ExtendedFieldTypes.HARVESTDEFINITION
     */
    protected int getExtendedFieldType() {
        return ExtendedFieldTypes.HARVESTDEFINITION;
    }
    
    /**
     * Change the name of the Harvestdefinition to newName. 
     * @param newName The new name of the Harvestdefinition
     */
    public void setName(String newName) {
    	this.harvestDefName = newName;
    }
}
