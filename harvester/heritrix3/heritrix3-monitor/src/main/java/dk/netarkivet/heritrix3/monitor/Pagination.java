/*
 * #%L
 * Netarchivesuite - heritrix 3 monitor
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

package dk.netarkivet.heritrix3.monitor;

/**
 * Builds a pagination HTML text block using twitter-bootstrap styles.
 *
 * <div class="pagination pull-right">
 *   <ul>
 *     <li class="disabled"><span>Previous</span></li>
 *     <li class="active"><span>1</span></li>
 *     <li><a href="#">2</a></li>
 *     <li><a href="#">3</a></li>
 *     <li class="disabled"><span>...</span></li>
 *     <li><a href="#">8</a></li>
 *     <li><a href="#">9</a></li>
 *     <li><a href="#">10</a></li>
 *     <li><a href="#">Next</a></li>
 *   </ul>
 * </div>
 */
public class Pagination {

    /**
     * Calculate the total number of pages.
     * @param items total number of items
     * @param itemsPerPage items displayed per page
     * @return the total number of pages
     */
    public static long getPages(long items, long itemsPerPage) {
        long pages = (items + itemsPerPage - 1) / itemsPerPage;
        if (pages == 0) {
            pages = 1;
        }
        return pages;
    }

    /**
     * Builds a pagination HTML text block.
     * 
     * @param page current page
     * @param itemsPerPage items displayed per page
     * @param pages total number of pages
     * @return HTML text block
     */
    public static String getPagination(long page, long itemsPerPage, long pages, boolean bShowAll, String additionalParams) {
        if (page < 1) {
            page = 1;
        }
        if (pages == 0) {
            pages = 1;
        }
        if (page > pages) {
            page = pages;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"pagination pull-right\">\n");
        sb.append("<ul>\n");
        // Previous
        if (page > 1) {
            sb.append("<li><a href=\"?page=" + (page - 1) + "&itemsperpage=" + itemsPerPage + additionalParams + "\">Previous</a></li>");
        } else {
            sb.append("<li class=\"disabled\"><span>Previous</span></li>");
        }
        // First.
        if (page == 1) {
            sb.append("<li class=\"active\"><span>" + 1 + "</span></li>");
        } else {
            sb.append("<li><a href=\"?page=" + 1 + "&itemsperpage=" + itemsPerPage + additionalParams + "\">" + 1 + "</a></li>");
        }
        // List.
        long tmpPage = page - 3;
        if (tmpPage > pages - 7) {
            tmpPage = pages - 7;
        }
        if (tmpPage > 2) {
            sb.append("<li class=\"disabled\"><span>...</span></li>");
        } else {
            tmpPage = 2;
        }
        int show = 8;
        while (show > 1 && tmpPage <= pages) {
            if (tmpPage == page) {
                sb.append("<li class=\"active\"><span>" + tmpPage + "</span></li>");
            } else {
                sb.append("<li><a href=\"?page=" + tmpPage + "&itemsperpage=" + itemsPerPage + additionalParams + "\">" + tmpPage + "</a></li>");
            }
            --show;
            tmpPage++;
        }
        // Last
        if (tmpPage <= pages) {
            if (tmpPage < pages) {
                sb.append("<li class=\"disabled\"><span>...</span></li>");
            }
            if (tmpPage == page) {
                sb.append("<li class=\"active\"><span>" + pages + "</span></li>");
            } else {
                sb.append("<li><a href=\"?page=" + pages + "&itemsperpage=" + itemsPerPage + additionalParams  + "\">" + pages + "</a></li>");
            }
        }
        // Next.
        if (page < pages) {
            sb.append("<li><a href=\"?page=" + (page + 1) + "&itemsperpage=" + itemsPerPage + additionalParams + "\">Next</a></li>");
        } else {
            sb.append("<li class=\"disabled\"><span>Next</span></li>");
        }
        // Items per page.
        /*
        sb.append("<li>");
        String[][] options = new String[][] {{"10", "10"}, {"25", "25"}, {"50", "50"}, {"100", "100"}, {"all", "Vis alle"}};
        int selected;
        if (bShowAll) {
            selected = options.length - 1;
        } else {
            switch ((int)itemsPerPage) {
            case 10:
                selected = 0;
                break;
            default:
            case 25:
                selected = 1;
                break;
            case 50:
                selected = 2;
                break;
            case 100:
                selected = 3;
                break;
            }
        }
        sb.append("<select name=\"itemsperpage\" class=\"input-mini\" onchange=\"this.form.submit();\">");
        for (int i=0; i<options.length; ++i) {
            sb.append("<option value=\"");
            sb.append(options[i][0]);
            sb.append("\"");
            if (i == selected) {
                sb.append(" selected=\"1\"");
            }
            sb.append(">");
            sb.append(options[i][1]);
            sb.append("</option>");
        }
        sb.append("</select>");
        sb.append("</li>");
        */
        sb.append("</ul>\n");
        sb.append("</div>\n");
        return sb.toString();
    }

}
