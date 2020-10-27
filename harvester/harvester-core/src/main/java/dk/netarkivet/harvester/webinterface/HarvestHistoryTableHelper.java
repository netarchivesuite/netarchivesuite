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

import java.util.List;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.harvester.datamodel.DomainDAO;
import dk.netarkivet.harvester.datamodel.DomainHarvestInfo;

/**
 * Used to manage the model used in the domain harvest history page. See Harveststatus-perdomain.jsp.
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

    private final String domainName;
    private final int pageIndex;
    private final long definedPageSize;
    private final long currentPageSize;
    private long startIndex;

    private long endIndex;
    private List<DomainHarvestInfo> harvestInfoList;

    public HarvestHistoryTableHelper(String domainNameParameter, String sortFieldParameter, String sortOrderParameter,
            String pageIndexParameter) {
        ArgumentNotValid.checkNotNull(domainNameParameter, "domainName");
        domainName = domainNameParameter;
        if (sortFieldParameter != null) {
            sortField = sortFieldParameter;
        } else {
            sortField = HarvestHistoryTableHelper.START_TIME_FIELD;
        }
        if (sortOrderParameter != null) {
            sortOrder = sortOrderParameter;
        } else {
            sortOrder = Constants.SORT_ORDER_ASC;
        }
        if (pageIndexParameter != null) {
            pageIndex = Integer.parseInt(pageIndexParameter);
        } else {
            pageIndex = 1;
        }
        harvestInfoList = DomainDAO.getInstance().listDomainHarvestInfo(domainName, sortField,
                sortOrder.equals(Constants.SORT_ORDER_ASC) ? true : false);

        definedPageSize = Settings.getLong(CommonSettings.HARVEST_STATUS_DFT_PAGE_SIZE);
        currentPageSize = (definedPageSize == 0 ? harvestInfoList.size() : definedPageSize);
        if (harvestInfoList.size() > 0) {
            startIndex = ((pageIndex - 1) * currentPageSize);
            endIndex = Math.min(startIndex + currentPageSize, getNumberOfResults());
        } else {
            // Dont's show "Search results: 0, displaying results 1 to 0"
            // but "Search results: 0, displaying results 0 to 0"
            startIndex = -1;
            endIndex = 0;
        }
    }

    /**
     * @return Return the list of DomainHarvestInfos for the current page.
     */
    public List<DomainHarvestInfo> listCurrentPageHarvestHistory() {
        return harvestInfoList.subList((int) startIndex, (int) endIndex);
    }

    /**
     * @return the index of the first result on the current page. The result is the full list of
     * <code>DomainHarvestInfo</code> objects for this domain for the selected sorting.
     */
    public long getStartIndex() {
        return startIndex;
    }

    /**
     * @return the index of the last result on the current page. The result is the full list of
     * <code>DomainHarvestInfo</code> objects for this domain for the selected sorting.
     */
    public long getEndIndex() {
        return endIndex;
    }

    /**
     * @return The index of the current page.
     */
    public int getPageIndex() {
        return pageIndex;
    }

    /**
     * @return The total number of <code>DomainHarvestInfo</code> objects in the db for this domain.
     */
    public long getNumberOfResults() {
        return harvestInfoList.size();
    }

    /**
     * @return <code>true</code> if the next page is available, else <code>false</code>
     */
    public boolean isNextPageAvailable() {
        return HarvestStatus.isNextLinkActive(currentPageSize, getNumberOfResults(), endIndex);
    }

    /**
     * @return <code>true</code> if the previous page is available, else <code>false</code>
     */
    public boolean isPreviousPageAvailable() {
        return HarvestStatus.isPreviousLinkActive(currentPageSize, getNumberOfResults(), startIndex);
    }

    /**
     * @return A string representing the parameters for the javascripting next/previous link functionality.
     */
    public String generateParameterStringForPaging() {
        return "'" + Constants.DOMAIN_SEARCH_PARAM + "'," + "'" + domainName + "'," + "'" + Constants.SORT_FIELD_PARAM
                + "'," + "'" + sortField + "'," + "'" + Constants.SORT_ORDER_PARAM + "'," + "'" + sortOrder + "'";
    }

    /**
     * Calculates the sort order arrow for the headers of a sortable table
     *
     * @param sortField The sort field to find a arrow for.
     * @return The relevant arrow for the indicated field. Will be the reverse if the sorting is already on this field
     * else an empty string will be returned
     */
    public String getOrderArrow(String sortField) {
        ArgumentNotValid.checkNotNull(sortField, "sortField");
        if (sortField.equals(this.sortField)) {
            return sortOrder.equals(Constants.SORT_ORDER_ASC) ? INC_SORT_ARROW : DEC_SORT_ARROW;
        }
        return NO_SORT_ARROW;
    }

    /**
     * Calculates the reverse sort order for this file. If the field isn't used for ordering, Constants.SORT_ORDER_ASC
     * is returned.
     *
     * @param sortField The sort field to find a new order for.
     * @return The relevant asc/desc string.
     */
    public String getOrderAfterClick(String sortField) {
        ArgumentNotValid.checkNotNull(sortField, "sortField");
        if (sortField.equals(this.sortField)) {
            return sortOrder.equals(Constants.SORT_ORDER_ASC) ? Constants.SORT_ORDER_DESC : Constants.SORT_ORDER_ASC;
        }
        return Constants.SORT_ORDER_ASC;
    }
}
