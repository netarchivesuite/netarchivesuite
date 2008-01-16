/* File:        $Id$
 * Date:        $Date$
 * Revision:    $Revision$
 * Author:      $Author$
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

import java.util.ArrayList;
import java.util.List;


/**
 */
public class IntegrityCheckStatus {

    private List<FileStatusCheck> errorList;
    private List<FileStatusCheck> failureList;

    /**
     * Default constructor.
     */
    public IntegrityCheckStatus() {
        this.errorList = new ArrayList<FileStatusCheck>();
        this.failureList = new ArrayList<FileStatusCheck>();
    }

    /**
     * Creates a FileStatusCheck with the given parameters
     * and stores it in a list of errors.
     *
     * @param fileID A identifier for a file
     * @param locationName The name for the bitarchive
     * @param errorMsg The errormessage
     */
    public void addError(String fileID, String locationName,
            String errorMsg) {
        errorList.add(new FileStatusCheck(fileID, locationName, errorMsg));
    }

    /**
     * Creates a FileStatusCheck with the given parameters
     * and stores it in a list of failures.
     *
     * @param fileID A identifier for a file
     * @param locationName The name for the bitarchive
     * @param errorMsg The errormessage
     */
    public void addFailure(String fileID, String locationName,
            String errorMsg) {
        failureList.add(new FileStatusCheck(fileID, locationName, errorMsg));
    }

    /**
     * @return Returns the statusList.
     */
    public List getErrorList() {
        return errorList;
    }

    /**
     * @return Returns the failedList.
     */
    public List getFailureList() {
        return failureList;
    }

    /**
     * @return If the lists contain any errors or failures.
     */
    public boolean isAllOk() {
        return ((errorList.size() & failureList.size()) == 0);
    }
}
