/* $Id$
 * $Revision$
 * $Author$
 * $Date$
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
package dk.netarkivet.harvester.harvesting.distribute;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.List;

import dk.netarkivet.common.distribute.ChannelID;
import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.harvester.datamodel.Job;
import dk.netarkivet.harvester.distribute.HarvesterMessage;
import dk.netarkivet.harvester.distribute.HarvesterMessageVisitor;


/**
 * Container for doOneCrawl request.
 * Contains the crawler job definition.
 */
public class DoOneCrawlMessage extends HarvesterMessage implements Serializable {
    /** the Job to crawl.    */
    private Job submittedJob;
    /** prefix to identify this message type. */
    private static final String IDPREFIX = "DoOneCrawl";
    /** Extra metadata associated with the crawl-job. */
    private List<MetadataEntry> metadata;

    /**
     * A NetarkivetMessage that contains a Job for Heritrix.
     *
     * @param submittedJob    the Job to crawl
     * @param to      the ChannelID for the Server
     * @param metadata A list of job-metadata
     * @throws ArgumentNotValid when sJob is null
     */
    public DoOneCrawlMessage(Job submittedJob, ChannelID to,
                             List<MetadataEntry> metadata)
    throws ArgumentNotValid {
        super(to, Channels.getError(), IDPREFIX);
        ArgumentNotValid.checkNotNull(submittedJob, "sJob");
        ArgumentNotValid.checkNotNull(metadata, "metadata");
        this.submittedJob = submittedJob;
        this.metadata = metadata;
    }

    /**
     * @return the Job
     */
    public Job getJob() {
        return submittedJob;
    }

    /**
     * @return Returns the metadata.
     */
    public List<MetadataEntry> getMetadata() {
        return metadata;
    }

    /**
     * Should be implemented as a part of the visitor pattern. fx.: public void
     * accept(HarvesterMessageVisitor v) { v.visit(this); }
     *
     * @param v A message visitor
     */
    public void accept(HarvesterMessageVisitor v) {
        v.visit(this);
    }


    /**
     * @return a String that represents the message - only for debugging !
     */
    public String toString() {
        return super.toString()
               + " Job: " + submittedJob
               + ", metadata: " +  metadata;
    }

    /**
     * Method needed to de-serializable an object of this class.
     * @param s an ObjectInputStream
     * @throws ClassNotFoundException
     * @throws IOException
     */
    private void readObject(ObjectInputStream s)
    throws ClassNotFoundException, IOException {
        s.defaultReadObject();
    }

    /**
     * Method needed to serializable an object of this class.
     * @param s an ObjectOutputStream
     * @throws ClassNotFoundException
     * @throws IOException
     */
    private void writeObject(ObjectOutputStream s)
    throws ClassNotFoundException, IOException {
        s.defaultWriteObject();
    }

}
