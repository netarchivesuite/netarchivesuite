/*
 * #%L
 * Netarchivesuite - archive
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
package dk.netarkivet.archive.arcrepository.bitpreservation;

import dk.netarkivet.archive.distribute.ArchiveMessage;
import dk.netarkivet.archive.distribute.ArchiveMessageVisitor;
import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.distribute.arcrepository.ReplicaStoreState;

/**
 * Class encapsulating a request to update AdminData. The message has two different types: changestorestate-type, and
 * changechecksum-type. There is a constructor for each type.
 */
@SuppressWarnings({"serial"})
public class AdminDataMessage extends ArchiveMessage {

    /** The filename to be updated in AdminData. */
    private String fileName;
    /** The id of the replica, where the file resides. */
    private String replicaId;
    /**
     * the new storestate for the filename. Used only when changestorestate is true.
     */
    private ReplicaStoreState newvalue;
    /**
     * the new checksum for the filename. Used only when changechecksum is true.
     */
    private String checksum;
    /** change storestate flag. default = false. */
    private boolean changestorestate = false;
    /** change checksum flag. default = false. */
    private boolean changechecksum = false;

    /**
     * Constructor used when you change the BitarchiveStoreState.
     *
     * @param theFileName The filename you want to give a new BitarchiveStoreState.
     * @param theReplicaId The ID for the bitarchive where the file resides
     * @param newval The new BitarchiveStoreState
     */
    public AdminDataMessage(String theFileName, String theReplicaId, ReplicaStoreState newval) {
        super(Channels.getTheRepos(), Channels.getThisReposClient());
        this.fileName = theFileName;
        this.replicaId = theReplicaId;
        this.newvalue = newval;
        this.changestorestate = true;
    }

    /**
     * Constructor used when you want to change the checksum for the given filename.
     *
     * @param theFileName the given filename
     * @param theChecksum the new checksum for the filename
     */
    public AdminDataMessage(String theFileName, String theChecksum) {
        super(Channels.getTheRepos(), Channels.getThisReposClient());
        fileName = theFileName;
        checksum = theChecksum;
        changechecksum = true;
    }

    /**
     * Should be implemented as a part of the visitor pattern. fx.: public void accept(ArchiveMessageVisitor v) {
     * v.visit(this); }
     *
     * @param v A message visitor
     */
    public void accept(ArchiveMessageVisitor v) {
        v.visit(this);
    }

    /**
     * Method for retrieving the replica id from this message.
     *
     * @return Returns the replicaId.
     */
    public String getReplicaId() {
        return replicaId;
    }

    /**
     * Method for retrieving the name of the file which are refered to in this message.
     *
     * @return Returns the fileName.
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * @return Returns the fileName.
     */
    public String getChecksum() {
        return checksum;
    }

    /**
     * Return the state of the changestorestate - flag.
     *
     * @return true, if this message is a changestorestate message
     */
    public boolean isChangeStoreState() {
        return changestorestate;
    }

    /**
     * Return the state of the changechecksum - flag.
     *
     * @return true, if this message is a changechecksum message
     */
    public boolean isChangeChecksum() {
        return changechecksum;
    }

    /**
     * @return Returns the newvalue.
     */
    public ReplicaStoreState getNewvalue() {
        return newvalue;
    }
}
