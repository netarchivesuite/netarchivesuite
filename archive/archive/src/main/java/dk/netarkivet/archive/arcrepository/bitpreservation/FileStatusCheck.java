/* File:                $Id$
 * Revision:            $Revision$
 * Author:              $Author$
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

package dk.netarkivet.archive.arcrepository.bitpreservation;

public class FileStatusCheck {

    private String fileID;
    private String locationName;
    private String errorMsg;

    /**
     * @param fileID A given File-identifier
     * @param locationName Represents a given bitarchive
     * @param errorMsg A given errormessage
     */
    public FileStatusCheck(String fileID, String locationName, String errorMsg) {
        super();
        this.fileID = fileID;
        this.locationName = locationName;
        this.errorMsg = errorMsg;
    }

    /**
     * @return Returns the bitarchive.
     */
    public String getBitarchive() {
        return locationName;
    }

    /**
     * @param locationName
     *            The bitarchive to set.
     */
    public void setBitarchive(String locationName) {
        this.locationName = locationName;
    }

    /**
     * @return Returns the errorMsg.
     */
    public String getErrorMsg() {
        return errorMsg;
    }

    /**
     * @param errorMsg
     *            The errorMsg to set.
     */
    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }

    /**
     * @return Returns the fileID.
     */
    public String getFileID() {
        return fileID;
    }

    /**
     * @param fileID
     *            The fileID to set.
     */
    public void setFileID(String fileID) {
        this.fileID = fileID;
    }
}