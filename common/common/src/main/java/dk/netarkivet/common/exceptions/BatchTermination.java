/* File:        $Id: IllegalState.java 1338 2010-03-17 15:27:53Z svc $
 * Revision:    $Revision: 1338 $
 * Author:      $Author: svc $
 * Date:        $Date: 2010-03-17 16:27:53 +0100 (Wed, 17 Mar 2010) $
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

package dk.netarkivet.common.exceptions;

/**
 * Exception to tell running batchjobs to terminate.
 */
public class BatchTermination extends NetarkivetException {
    /**
     * Constructs new BatchTermination exception with the given message.
     * @param message The exception message.
     */
    public BatchTermination(String message) {
        super(message);
    }

    /**
     * Constructs new BatchTermination exception with the given message and
     * cause.
     * @param message The exception message.
     * @param cause The cause of the exception.
     */
    public BatchTermination(String message, Throwable cause) {
        super(message, cause);
    }
}
