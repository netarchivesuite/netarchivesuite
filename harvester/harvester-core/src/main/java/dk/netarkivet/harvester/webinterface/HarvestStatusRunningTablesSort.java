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
package dk.netarkivet.harvester.webinterface;

import java.util.HashMap;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.utils.TableSort;

/**
 * Class used to manage the sort of tables in the harvest status running screen.
 * Used only by History/Harveststatus-running.jsp
 */
public class HarvestStatusRunningTablesSort {
    /** list of the column id. */
    public enum ColumnId {
        NONE, ID, HOST, PROGRESS, ELAPSED, QFILES, TOTALQ, ACTIVEQ, EXHAUSTEDQ, RETIREDQ
    }

    ;

    /** map containing the sort data of each table. */
    private HashMap<String, TableSort> sortData;

    /** Constructor. */
    public HarvestStatusRunningTablesSort() {
        sortData = new HashMap<String, TableSort>();
    }

    /**
     * return the ColumnId corresponding to the hash code.
     *
     * @param columnIdInt the hash code
     * @return the ColumnId
     */
    public final ColumnId getColumnIdByHash(final int columnIdInt) {
        if (HarvestStatusRunningTablesSort.ColumnId.ID.hashCode() == columnIdInt) {
            return HarvestStatusRunningTablesSort.ColumnId.ID;
        } else if (HarvestStatusRunningTablesSort.ColumnId.HOST.hashCode() == columnIdInt) {
            return HarvestStatusRunningTablesSort.ColumnId.HOST;
        } else if (HarvestStatusRunningTablesSort.ColumnId.PROGRESS.hashCode() == columnIdInt) {
            return HarvestStatusRunningTablesSort.ColumnId.PROGRESS;
        } else if (HarvestStatusRunningTablesSort.ColumnId.ELAPSED.hashCode() == columnIdInt) {
            return HarvestStatusRunningTablesSort.ColumnId.ELAPSED;
        } else if (HarvestStatusRunningTablesSort.ColumnId.QFILES.hashCode() == columnIdInt) {
            return HarvestStatusRunningTablesSort.ColumnId.QFILES;
        } else if (HarvestStatusRunningTablesSort.ColumnId.TOTALQ.hashCode() == columnIdInt) {
            return HarvestStatusRunningTablesSort.ColumnId.TOTALQ;
        } else if (HarvestStatusRunningTablesSort.ColumnId.ACTIVEQ.hashCode() == columnIdInt) {
            return HarvestStatusRunningTablesSort.ColumnId.ACTIVEQ;
        } else if (HarvestStatusRunningTablesSort.ColumnId.EXHAUSTEDQ.hashCode() == columnIdInt) {
            return HarvestStatusRunningTablesSort.ColumnId.EXHAUSTEDQ;
        } else if (HarvestStatusRunningTablesSort.ColumnId.RETIREDQ.hashCode() == columnIdInt) {
            return HarvestStatusRunningTablesSort.ColumnId.RETIREDQ;
        }

        return HarvestStatusRunningTablesSort.ColumnId.NONE;
    }

    /**
     * return the ColumnId of the sorted table.
     *
     * @param harvestName the harvest name
     * @return the ColumnId
     */
    public final ColumnId getSortedColumnIdentByHarvestName(final String harvestName) {
        ArgumentNotValid.checkNotNull(harvestName, "harvest name can't be null");
        TableSort tbs = getTableSort(harvestName);
        int columnIdInt = tbs.getColumnIdent();

        return getColumnIdByHash(columnIdInt);
    }

    /**
     * return the SortOrder of the sorted table.
     *
     * @param harvestName the harvest name
     * @return the SortOrder
     */
    public final TableSort.SortOrder getSortOrderByHarvestName(final String harvestName) {
        ArgumentNotValid.checkNotNull(harvestName, "harvest name can't be null");
        TableSort.SortOrder order = TableSort.SortOrder.NONE;
        TableSort tbs = getTableSort(harvestName);
        order = tbs.getOrder();

        return order;
    }

    /**
     * effect of a click on a column.
     *
     * @param harvestName the harvest name
     * @param column ColumnId of the clicked column
     */
    public final void sort(final String harvestName, final ColumnId column) {
        TableSort tbs = getTableSort(harvestName);
        TableSort.SortOrder order = tbs.getOrder();

        // another column
        if (tbs.getColumnIdent() != column.hashCode()) {
            order = TableSort.SortOrder.NONE;
            tbs.setColumnIdent(column.hashCode());
        }

        // change order
        if (order == TableSort.SortOrder.NONE) {
            order = TableSort.SortOrder.INCR;
        } else if (order == TableSort.SortOrder.INCR) {
            order = TableSort.SortOrder.DESC;
        } else {
            order = TableSort.SortOrder.NONE;
        }
        tbs.setOrder(order);
    }

    /**
     * effect of a click on a column.
     *
     * @param harvestName the harvest name
     * @param column hashcode of the ColumnId of the clicked column
     */
    public final void sortByHarvestName(final String harvestName, final int column) {

        ColumnId columnId = HarvestStatusRunningTablesSort.ColumnId.NONE;
        columnId = getColumnIdByHash(column);
        sort(harvestName, columnId);
    }

    /**
     * return the TableSort object describing the sort.
     *
     * @param harvestName the harvest name
     * @return the TableSort
     */
    private TableSort getTableSort(final String harvestName) {
        TableSort tbs = sortData.get(harvestName);

        if (tbs == null) {
            tbs = new TableSort(HarvestStatusRunningTablesSort.ColumnId.NONE.hashCode(), TableSort.SortOrder.NONE);
            sortData.put(harvestName, tbs);
        }

        return tbs;
    }
}
