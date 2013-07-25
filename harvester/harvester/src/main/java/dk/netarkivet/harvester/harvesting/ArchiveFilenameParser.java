/* File:       $Id$
 * Revision:   $Revision$
 * Author:     $Author$
 * Date:       $Date$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2012 The Royal Danish Library, the Danish State and
 * University Library, the National Library of France and the Austrian
 * National Library.
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
package dk.netarkivet.harvester.harvesting;

public abstract class ArchiveFilenameParser {
    
    /** @return the harvestID. */
    public abstract String getHarvestID();
    
    /** 
     * Get the job ID.
     * @return the Job ID.
     */
    public abstract String getJobID();
    
    /** 
     * Get the timestamp.
     * @return the timestamp.
     */
    public abstract String getTimeStamp();
    
    /** 
     * Get the serial number.
     * @return the serial number.
     */
    public abstract String getSerialNo();
    
    /** 
     * Get the filename.
     * @return the filename.
     */
    public abstract String getFilename();
}
