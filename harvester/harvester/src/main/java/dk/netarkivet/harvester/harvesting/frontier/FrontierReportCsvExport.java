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
package dk.netarkivet.harvester.harvesting.frontier;

import java.io.PrintWriter;
import java.lang.reflect.Method;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class FrontierReportCsvExport {

    /** The logger for this class. */
    private static final Log LOG =
        LogFactory.getLog(FrontierReportCsvExport.class);

    private static enum FIELD {
        domainName,
        currentSize,
        totalEnqueues,
        sessionBalance,
        lastCost,
        averageCost,
        lastDequeueTime,
        wakeTime,
        totalSpend,
        totalBudget,
        errorCount,
        lastPeekUri,
        lastQueuedUri;

        private String getterName() {
            return "get" + String.valueOf(name().charAt(0)).toUpperCase()
                + name().substring(1);
        }

        static String genLine(FrontierReportLine l, String separator) {
            String line = "";
            FIELD[] allFields = values();
            for (int i = 0; i < allFields.length; i++) {
                FIELD field = allFields[i];
                try {
                    String getterName = field.getterName();
                    Method getter = l.getClass().getMethod(getterName);
                    Object value = getter.invoke(l);

                    String valueStr = "?";
                    if (value instanceof String) {
                        valueStr = (String) value;
                    } else if (value instanceof Double) {
                        valueStr =
                            getDisplayValue(((Double) value).doubleValue());
                    } else if (value instanceof Long) {
                        valueStr =
                            getDisplayValue(((Long) value).longValue());
                    }

                    line += valueStr
                        + (i < allFields.length -1 ? separator : "");
                } catch (Exception e) {
                    LOG.error("Failed to invoke getter FrontierReportLine#"
                            + field.getterName(), e);
                }
            }
            return line;
        }

        static String genHeaderLine(String separator) {
            String header = "";
            FIELD[] allFields = values();
            for (int i = 0; i < allFields.length; i++) {
                header += allFields[i].name()
                    + (i < allFields.length -1 ? separator : "");
            }
            return header;
        }
    };


    /**
     * Ouputs the report as CSV, using the given writer and the given field
     * separator. Note that writer is not closed by this method.
     * @param pw the writer to ouput to
     * @param separator the field separator.
     */
    public static void outputAsCsv(
            InMemoryFrontierReport report,
            PrintWriter pw,
            String separator) {

        pw.println(FIELD.genHeaderLine(separator));
        for (FrontierReportLine l : report.getLines()) {
             pw.println(FIELD.genLine(l, separator));
        }

    }

    private static String getDisplayValue(long val) {
        return Long.MIN_VALUE == val ?
                FrontierReportLine.EMPTY_VALUE_TOKEN : "" + val;
    }

    private static String getDisplayValue(double val) {
        if (Double.MIN_VALUE == val) {
            return FrontierReportLine.EMPTY_VALUE_TOKEN;
        }
        return (Math.rint(val) == val ?
                Integer.toString((int) val) : "" + Double.toString(val));
    }

}
