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
package dk.netarkivet.archive.webinterface;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    //private Log log = LogFactory.getLog(BitpreservationUpdateThread.class.getName());
    protected static final Logger log = LoggerFactory.getLogger(BitpreservationUpdateThread.class);

    /** The ActiveBitPreservation class. */
    private final ActiveBitPreservation preserve;

    /** The type of update requested. */
    private final BitpreservationUpdateType type;

    /** The replica to work on. */
    private final Replica theReplica;

    /**
     * Constructor for the BitpreservationUpdateThread.
     *
     * @param replica The given replica to work on.
     * @param updateType The type of update requested.
     * @throws ArgumentNotValid If either the Replica or the BitpreservationUpdateType is null.
     */
    public BitpreservationUpdateThread(Replica replica, BitpreservationUpdateType updateType) throws ArgumentNotValid {
        ArgumentNotValid.checkNotNull(replica, "Replica replica");
        ArgumentNotValid.checkNotNull(updateType, "BitpreservationUpdateType " + "updateType");
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
