package dk.netarkivet.common.utils;

import dk.netarkivet.common.exceptions.ArgumentNotValid;


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
        ArgumentNotValid.checkTrue(
                sortOrder == TableSort.SortOrder.DESC
                || sortOrder == TableSort.SortOrder.INCR
                || sortOrder == TableSort.SortOrder.NONE
                , "set order invalid");

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
        ArgumentNotValid.checkTrue(
                sortorder == TableSort.SortOrder.DESC
                || sortorder == TableSort.SortOrder.INCR
                || sortorder == TableSort.SortOrder.NONE
                , "set order invalid");
        order = sortorder;
    }
}
