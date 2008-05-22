/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
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
package dk.netarkivet.harvester.harvesting.distribute;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.harvester.datamodel.StopReason;

import java.io.Serializable;

/** Tuple class to hold domain harvest statistics for a single domain. */

public class DomainStats implements Serializable {

    /** Count of how many objects have been harvested from this domain. */
    private long objectCount;
    /** Count of how many bytes have been harvested from this domain . */
    private long byteCount;
    /** The reason why we 'only' harvested byteCount bytes
     * or objectCount objects. */
    private StopReason stopReason;

    /**
     * Constructor for a DomainStats object.
     * @param initObjectCount Start counting objects from this number
     * @param initByteCount  Start counting bytes from this number
     * @param defaultStopReason The default StopReason for a given domain.
     * @throws ArgumentNotValid If initObjectCount < 0, initByteCount < 0, or
     *          defaultStopReason is null.
     */
    public DomainStats(long initObjectCount, long initByteCount,
            StopReason defaultStopReason) {
        ArgumentNotValid.checkNotNegative(initObjectCount, "initObjectCount");
        ArgumentNotValid.checkNotNegative(initByteCount, "initByteCount");
        ArgumentNotValid.checkNotNull(defaultStopReason, "defaultStopReason");
        this.objectCount = initObjectCount;
        this.byteCount = initByteCount;
        this.stopReason = defaultStopReason;
    }

    /**
     * @return the byteCount.
     */
    public long getByteCount() {
        return byteCount;
    }

    /**
     * @param byteCount The byteCount to set.
     * @throws ArgumentNotValid If byteCount is a negative number.
     */
    public void setByteCount(long byteCount) {
        ArgumentNotValid.checkNotNegative(byteCount, "byteCount");
        this.byteCount = byteCount;
    }

    /**
     * @return the objectCount.
     */
    public long getObjectCount() {
        return objectCount;
    }

    /**
     * Set objectcount to something new.
     * @param objectCount The objectCount to set.
     * @throws ArgumentNotValid If objectCount is a negative number.
     */
    public void setObjectCount(long objectCount) {
        ArgumentNotValid.checkNotNegative(objectCount, "objectCount");
        this.objectCount = objectCount;
    }

    /**
     * @return the stopReason.
     */
    public StopReason getStopReason() {
        return stopReason;
    }

    /**
     * Set stopreason to something new.
     * @param stopReason The stopReason to set.
     * @throws ArgumentNotValid If argument is null
     */
    public void setStopReason(StopReason stopReason) {
        ArgumentNotValid.checkNotNull(stopReason, "stopReason");
        this.stopReason = stopReason;
    }

}
