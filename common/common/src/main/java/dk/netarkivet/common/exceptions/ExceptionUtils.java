/* File:        $Id: License.txt,v $
 * Revision:    $Revision: 1.4 $
 * Author:      $Author: csr $
 * Date:        $Date: 2005/04/11 16:29:16 $
 *
 * Copyright Det Kongelige Bibliotek og Statsbiblioteket, Danmark
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */

package dk.netarkivet.common.exceptions;

import java.sql.SQLException;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Utility methods for exceptions
 *
 * @author csr
 * @since Jan 15, 2009
 */

public class ExceptionUtils {

    /**
     * SQLExceptions have their own stack of causes accessed via the
     * getNextException() method. This utility provides a string representation of those causes for
     * use in logging or rethrowing
     * @param e  the original top-level exception
     * @return a String describing the exception
     */
    public static String getSQLExceptionCause(SQLException e) {
        StringBuffer message = new StringBuffer("SQLException trace:\n");
        do {
            message.append(getSingleSQLExceptionCause(e));
            e = e.getNextException();
            if (e != null) {
                message.append("NextException:\n");
            }
        } while (e != null);
        message.append("End of SQLException trace");
        return message.toString();
    }

    private static StringBuffer getSingleSQLExceptionCause(SQLException e) {
        StringBuffer message = new StringBuffer();
        message.append("SQL State:").append(e.getSQLState()).append("\n");
        message.append("Error Code:").append(e.getErrorCode()).append("\n");
        StringWriter string_writer = new StringWriter();
        PrintWriter writer = new PrintWriter(string_writer);
        e.printStackTrace(writer);
        message.append(string_writer.getBuffer());
        return message;
    }

}
