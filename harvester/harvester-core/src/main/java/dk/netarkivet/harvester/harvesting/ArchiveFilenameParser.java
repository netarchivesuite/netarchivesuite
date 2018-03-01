/*
 * #%L
 * Netarchivesuite - harvester
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
package dk.netarkivet.harvester.harvesting;

public abstract class ArchiveFilenameParser {

    /** @return the harvestID. */
    public abstract String getHarvestID();

    /**
     * Get the job ID.
     *
     * @return the Job ID.
     */
    public abstract String getJobID();

    /**
     * Get the timestamp.
     *
     * @return the timestamp.
     */
    public abstract String getTimeStamp();

    /**
     * Get the serial number.
     *
     * @return the serial number.
     */
    public abstract String getSerialNo();

    /**
     * Get the filename.
     *
     * @return the filename.
     */
    public abstract String getFilename();

}
