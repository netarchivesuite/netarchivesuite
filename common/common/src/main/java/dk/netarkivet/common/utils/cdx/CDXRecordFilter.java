/* $Id$
 * $Date$
 * $Revision$
 * $Author$
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

package dk.netarkivet.common.utils.cdx;


import java.io.Serializable;

/**
 * Interface defining a filter to use in CDXReader when finding CDXRecords.
 */

public interface CDXRecordFilter extends Serializable {

    /**
     * Process one CDXRecord - return true/false.
     * 
     * @param cdxrec
     *            the CDXRecord to be processed.
     * @return true or false on whether the processed CDXRecord is "valid"
     *         according to this filter implementation.
     *         true means this CDXRecord is valid!
     */
    boolean process(CDXRecord cdxrec);

    /**
     * @return the name of the Filter
     */
    String getFilterName();
}
