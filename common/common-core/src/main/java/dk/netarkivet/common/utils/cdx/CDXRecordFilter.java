/*
 * #%L
 * Netarchivesuite - common
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

package dk.netarkivet.common.utils.cdx;

import java.io.Serializable;

/**
 * Interface defining a filter to use in CDXReader when finding CDXRecords.
 */
public interface CDXRecordFilter extends Serializable {

    /**
     * Process one CDXRecord - return true/false.
     *
     * @param cdxrec the CDXRecord to be processed.
     * @return true or false on whether the processed CDXRecord is "valid" according to this filter implementation. true
     * means this CDXRecord is valid!
     */
    boolean process(CDXRecord cdxrec);

    /**
     * @return the name of the Filter
     */
    String getFilterName();

}
