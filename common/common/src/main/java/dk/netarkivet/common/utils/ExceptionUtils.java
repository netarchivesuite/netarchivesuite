/* File:        $Id$
 * Revision:    $Revision$
 * Date:        $Date$
 * Author:      $Author$
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
package dk.netarkivet.common.utils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

/**
 * Utilities for reading a stacktrace.
 */
public class ExceptionUtils {
    /**
     * Utility class, do not instantiate.
     */
    private ExceptionUtils() {
    }

    /**
     * Prints the stacktrace of an exception to a String. Why this
     * functionality is not included in the standard java libraries
     * is anybody's guess.
     * @param aThrowable An exception
     * @return String containing a stacktrace of exception aThrowable. Will
     * return the string "null" and a linebreak if aThrowable is null.
     */
    public static String getStackTrace(Throwable aThrowable) {
        final Writer result = new StringWriter();
        final PrintWriter printWriter = new PrintWriter(result);
        if (aThrowable != null) {
            aThrowable.printStackTrace(printWriter);
        } else {
            printWriter.println("null");
        }
        return result.toString();
    }

}
