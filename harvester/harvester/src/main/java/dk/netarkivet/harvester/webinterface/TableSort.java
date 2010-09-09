/* File:       $Id$
 * Revision:   $Revision$
 * Author:     $Author$
 * Date:       $Date$
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
package dk.netarkivet.harvester.webinterface;


/** contains the data about how a table is sorted.*/
public class TableSort {
    /**list of the sort order.*/
    public enum SortOrder { NONE, INCR, DESC };
    /** id of the sorted column.*/
    private int columnIdent = -1;
    /** order of the sort.*/
    private SortOrder order = TableSort.SortOrder.NONE;

    /**constructor.
     * @param columnId the id of the sorted column
     * @param sortOrder the order of the sort
     * */
    public TableSort(final int columnId, final SortOrder sortOrder) {
        columnIdent = columnId;
        order = sortOrder;
    }

    /** return the id of the sorted column.
     * @return the id of the sorted column
     * */
    public final int getColumnIdent() {
        return columnIdent;
    }

    /** set the id of the sorted column.
     * @param columnident the id of the sorted column
     * */
    public final void setColumnIdent(final int columnident) {
        columnIdent = columnident;
    }
    /** return the order of the sort.
     * @return the order of the sort
     * */
    public final SortOrder getOrder() {
        return order;
    }

    /** set the order of the sort.
     * @param sortorder the order of the sort
     * */
    public final void setOrder(final SortOrder sortorder) {
        order = sortorder;
    }
}
