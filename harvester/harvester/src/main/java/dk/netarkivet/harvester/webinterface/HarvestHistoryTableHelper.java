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
package dk.netarkivet.harvester.webinterface;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Used to manage the sort of tables in the domain harvest history screen.
 * See Harveststatus-perdomain.jsp.
 */
public class HarvestHistoryTableHelper {
    public static final String HARVEST_NAME_FIELD = "hdname";
    public static final String HARVEST_NUMBER_FIELD = "harvest_num";
    public static final String JOB_ID_FIELD = "job_id";
    public static final String CONFIGURATION_NAME_FIELD = "configname";
    public static final String START_TIME_FIELD = "startdate";
    public static final String STOP_TIME_FIELD = "enddate";
    public static final String BYTES_HARVESTED_FIELD = "bytecount";
    public static final String DOCUMENTS_HARVESTED_FIELD = "objectcount";
    public static final String STOPPED_DUE_TO_FIELD = "stopreason";

    private static final String INC_SORT_ARROW = "&uarr;";
    private static final String DEC_SORT_ARROW = "&darr;";
    private static final String NO_SORT_ARROW = "";

    private final String sortField;
    private final String sortOrder;

    /** The log. */
    private static final Log log = LogFactory.getLog(HarvestHistoryTableHelper.class);

    public HarvestHistoryTableHelper(String sortField, String sortOrder) {
        this.sortField = sortField;
        this.sortOrder = sortOrder;
    }

    /**
     * Calculates the sort order arrow for the headers of a sortable table
     * @param sortField The sort field to find a arrow for.
     * @return The relevant arrow for the indicated field. Will be the reverse if the sorting is already
     * on this field else an empty string will be returned
     */
    public String getOrderArrow(String sortField) {
        if (sortField.equals(this.sortField)) {
            return sortOrder.equals(Constants.SORT_ORDER_ASC) ? INC_SORT_ARROW : DEC_SORT_ARROW;
        }
        return NO_SORT_ARROW;
    }

    /**
     * Calculates the reverse sort order for this file. If the field isen't used for ordering,
     * Constants.SORT_ORDER_ASC is returned.
     * @param sortField The sort field to find a new order for for.
     * @return The relevant asc/desc string.
     */
    public String getOrderAfterClick(String sortField) {
        if (sortField.equals(this.sortField)) {
            return sortOrder.equals(Constants.SORT_ORDER_ASC) ? Constants.SORT_ORDER_DESC : Constants.SORT_ORDER_ASC;
        }
        return Constants.SORT_ORDER_ASC;
    }
}
