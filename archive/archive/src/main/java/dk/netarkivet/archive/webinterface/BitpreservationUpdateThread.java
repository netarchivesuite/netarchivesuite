/* File:  $Id$
 * Revision: $Revision$
 * Author:   $Author$
 * Date:     $Date$
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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301 
 *  USA
 */
package dk.netarkivet.archive.webinterface;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.archive.arcrepository.bitpreservation.ActiveBitPreservation;
import dk.netarkivet.archive.arcrepository.bitpreservation.ActiveBitPreservationFactory;
import dk.netarkivet.common.distribute.arcrepository.Replica;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.UnknownID;

/**
 * Class for threading a bitpreservation update into a thread.
 */
public class BitpreservationUpdateThread extends Thread {

    /** The log. */
    private Log log = LogFactory.getLog(
            BitpreservationUpdateThread.class.getName());
    
    /** The ActiveBitPreservation class. */
    private final ActiveBitPreservation preserve;
    
    /** The type of update requested. */
    private final BitpreservationUpdateType type;
    
    /** The replica to work on. */
    private final Replica theReplica;
    
    
    /** Constructor for the BitpreservationUpdateThread. 
     * 
     * @param replica The given replica to work on.
     * @param updateType The type of update requested.
     * @throws ArgumentNotValid If either the Replica or the 
     * BitpreservationUpdateType is null.
     */
    public BitpreservationUpdateThread(Replica replica, 
            BitpreservationUpdateType updateType) throws ArgumentNotValid {
        ArgumentNotValid.checkNotNull(replica, "Replica replica");
        ArgumentNotValid.checkNotNull(updateType, "BitpreservationUpdateType " 
                + "updateType");
        preserve = ActiveBitPreservationFactory.getInstance();
        type = updateType;
        theReplica = replica;
    }
    
    /** Starts the updatethread thread. */
    public void run() {
        try {
            if (type.equals(BitpreservationUpdateType.CHECKSUM)) {
                preserve.findChangedFiles(theReplica);
            } else if (type.equals(BitpreservationUpdateType.FINDMISSING)) {
                preserve.findMissingFiles(theReplica);
            } else {
                throw new UnknownID("Can't handle type '" + type + "' yet");
            }
        } catch (Exception ex) {
            log.warn("Update of Activebitpreservation information failed", ex);
        }    
    }
}
