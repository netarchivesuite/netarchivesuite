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
package dk.netarkivet.archive.bitarchive.distribute;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.PermissionDenied;
import dk.netarkivet.common.exceptions.UnknownID;

/**
 * Bitarchive container responsible for processing the different classes of message which can be received by a
 * bitarchive and returning appropriate data.
 */
public class AccessBitarchiveServer extends BitarchiveServer {
    /** The logger used by this class. */
    private static final Logger log = LoggerFactory.getLogger(AccessBitarchiveServer.class);


    /** The unique instance of this class. */
    private static AccessBitarchiveServer instance;

    /**
     * Returns the unique instance of this class The server creates an instance of the bitarchive it provides access to
     * and starts to listen to JMS messages on the incomming jms queue
     * <p>
     * Also, heartbeats are sent out at regular intervals to the Bitarchive Monitor, to tell that this bitarchive is
     * alive.
     *
     * @return the instance
     * @throws UnknownID - if there was no heartbeat frequency defined in settings
     * @throws ArgumentNotValid - if the heartbeat frequency in settings is invalid or either argument is null
     */
    public static synchronized AccessBitarchiveServer getInstance() throws ArgumentNotValid, UnknownID {
        if (instance == null) {
            instance = new AccessBitarchiveServer();
        }
        return instance;
    }

    /**
     * The server creates an instance of the bitarchive it provides access to and starts to listen to JMS messages on
     * the incomming jms queue
     * <p>
     * Also, heartbeats are sent out at regular intervals to the Bitarchive Monitor, to tell that this bitarchive is
     * alive.
     *
     * @throws UnknownID - if there was no heartbeat frequency or temp dir defined in settings or if the bitarchiveid
     * cannot be created.
     * @throws PermissionDenied - if the temporary directory or the file directory cannot be written
     */
    private AccessBitarchiveServer() throws UnknownID, PermissionDenied {
    	super();
    }

    @Override
    public void visit(UploadMessage msg) throws ArgumentNotValid {
    	msg.setNotOk(new ArgumentNotValid("Not valid to modify anything to an AccessBitarchive."));
    	log.warn("Will not deal with UploadMessage #{}", msg);
    	con.reply(msg);
    }

    @Override
    public void visit(RemoveAndGetFileMessage msg) throws ArgumentNotValid {
    	msg.setNotOk(new ArgumentNotValid("Not valid to modify anything to an AccessBitarchive."));
    	log.warn("Will not deal with RemoveAndGetFileMessage #{}", msg);
    	con.reply(msg);
    }
}
