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

import java.util.Date;

import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 * Sparse version of PartialHarvest to be used for GUI purposes only.
 * Immutable.
 *
 * @see PartialHarvest
 */
public class SparsePartialHarvest {
    /**
     * ID of this harvest.
     */
    private final Long oid;
    /**
     * Name of this harvest.
     */
    private final String name;
    /**
     * Comments on this harvest.
     */
    private final String comments;
    /**
     * Number of times this harvest has run.
     */
    private final int numEvents;
    /**
     * True if harvest is active.
     */
    private final boolean active;
    /**
     * Current edition of harvest.
     */
    private final long edition;
    /**
     * Submitted date.
     */
    private final Date submissionDate;
    /**
     * Schedule for harvest definition.
     */
    private final String scheduleName;
    /**
     * Next date to run.
     */
    private final Date nextDate;

    /**
     * Create new instance of SparsePartialHarvest.
     *
     * @param oid       id of this  harvest.
     * @param name      the name of the harvest definition.
     * @param comments  comments.
     * @param numEvents Number of times this harvest has run.
     * @param submissionDate The submission date.
     * @param active    Whether this harvest definition is active.
     * @param edition   DAO edition of harvest. used to create this
     *                  Fullharvest definition.
     * @param schedule  name of schedule for this harvest.
     * @param nextDate  next time this harvest will run (null for never).
     * @throws ArgumentNotValid if oid, name or comments, or schedule is null,
     *                          or name or schedule is empty.
     */
    public SparsePartialHarvest(Long oid, String name, String comments,
                                int numEvents,
                                Date submissionDate, boolean active,
                                long edition, String schedule,
                                Date nextDate) {
        ArgumentNotValid.checkNotNull(oid, "Long oid");
        ArgumentNotValid.checkNotNullOrEmpty(name, "name");
        ArgumentNotValid.checkNotNull(comments, "comments");
        ArgumentNotValid.checkNotNullOrEmpty(schedule, "schedule");
        this.oid = oid;
        this.name = name;
        this.comments = comments;
        this.numEvents = numEvents;
        this.submissionDate = submissionDate;
        this.active = active;
        this.edition = edition;
        this.scheduleName = schedule;
        this.nextDate = nextDate;
    }


    /**
     * Next date this harvest will run (null for never).
     *
     * @return Returns the nextDate.
     */
    public Date getNextDate() {
        return nextDate;
    }

    /**
     * Whether this definition is active.
     *
     * @return Returns whether active.
     */
    public boolean isActive() {
        return active;
    }


    /**
     * Get comments for domain.
     *
     * @return Returns the comments.
     */
    public String getComments() {
        return comments;
    }


    /**
     * Get edition.
     *
     * @return Returns the edition.
     */
    public long getEdition() {
        return edition;
    }


    /**
     * Get name.
     *
     * @return Returns the name.
     */
    public String getName() {
        return name;
    }


    /**
     * Number of events this harvest definition has run.
     *
     * @return Returns the numEvents.
     */
    public int getNumEvents() {
        return numEvents;
    }


    /**
     * Name of schedule for harvest definition.
     *
     * @return Returns the scheduleName.
     */
    public String getScheduleName() {
        return scheduleName;
    }


    /**
     * Submission date.
     *
     * @return Returns the submissionDate.
     */
    public Date getSubmissionDate() {
        return submissionDate;
    }

    /**
     * ID of harvest definition.
     *
     * @return Returns the harvestdefinition ID
     */
    public Long getOid() {
        return oid;
    }
}






