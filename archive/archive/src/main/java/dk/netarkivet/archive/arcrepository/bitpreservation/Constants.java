/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2009 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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

/**
 * Constants used in bit preservation.
 *
 */
public class Constants {

    /** Characters used for separating a file identifier 
     * from the checksum in the output from a checksum job.
     */
    public static final String STRING_FILENAME_SEPARATOR = "##";

    /**
     * On min in milliseconds
     */
    public static final long ONE_MIN_IN_MILLIES = 60*1000;
    /**
     * One hour in milliseconds
     */
    public static final long ONE_HOUR_IN_MILLIES = 60*ONE_MIN_IN_MILLIES;

    /**
     * Not to be instantiated.
     */
    private Constants(){
    }
}
