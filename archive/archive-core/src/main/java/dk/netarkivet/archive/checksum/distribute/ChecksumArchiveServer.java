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
package dk.netarkivet.archive.checksum.distribute;

import dk.netarkivet.archive.bitarchive.distribute.UploadMessage;
import dk.netarkivet.archive.distribute.ArchiveMessageHandler;
import dk.netarkivet.common.distribute.ChannelID;
import dk.netarkivet.common.distribute.JMSConnection;
import dk.netarkivet.common.utils.CleanupIF;

/**
 * Any subclass must be invoked through a method called 'getInstance'.
 */
public abstract class ChecksumArchiveServer extends ArchiveMessageHandler implements CleanupIF {

    /**
     * The JMS connection.
     */
    protected JMSConnection jmsCon;

    /**
     * The unique id for this instance.
     */
    protected String checksumAppId;

    /**
     * The channel used for communication.
     */
    protected ChannelID theCR;

    /**
     * Method for closing.
     */
    abstract void close();

    /**
     * Method for cleaning up.
     */
    public abstract void cleanup();

    /**
     * Method for retrieving the application id.
     *
     * @return The application id.
     */
    public abstract String getAppId();

    /**
     * Requiring all inheritors of this interface to handle the UploadMessage. The data should be fetched and put into
     * the archive.
     *
     * @param msg The UploadMessage to be handled.
     */
    public abstract void visit(UploadMessage msg);

    /**
     * Requiring all inheritors of this interface to handle the CorrectMessage. If an entry in the archive corresponds
     * to the 'wrong' entry described in the CorrectMessage, then the file in the CorrectMessage should replace the
     * current entry in the archive.
     *
     * @param msg The CorrectMessage to be handled.
     */
    public abstract void visit(CorrectMessage msg);

    /**
     * Requiring all inheritors of this interface to handle the GetChecksumMessage. The checksum of the wanted entry in
     * the archive should be fetched and returned.
     *
     * @param msg The GetChecksumMessage to be handled.
     */
    public abstract void visit(GetChecksumMessage msg);

    /**
     * Requiring all inheritors of this interface to handle the GetAllChecksumMessage. The entire archive should be put
     * into a file corresponding to a ChecksumJob file, then made into a remote file and sent back through the reply.
     *
     * @param msg The GetAllChecksumMessage to be handled.
     */
    public abstract void visit(GetAllChecksumsMessage msg);

    /**
     * Requiring all inheritors of this interface to handle the GetAllFilenamesMessage. The filenames of all the entries
     * in the archive should be placed in a file corresponding to a FilelistJob and sent back through the reply.
     *
     * @param msg The GetAllFilenamesMessage to be handled.
     */
    public abstract void visit(GetAllFilenamesMessage msg);
}
