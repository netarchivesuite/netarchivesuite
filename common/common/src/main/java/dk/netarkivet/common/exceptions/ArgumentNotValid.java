/*$Id$
* $Revision$
* $Date$
* $Author$
*
* The Netarchive Suite - Software to harvest and preserve websites
* Copyright 2004-2009 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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
package dk.netarkivet.common.exceptions;

import java.util.Collection;

/**
 * Indicates that one or more arguments are invalid.
 */
public class ArgumentNotValid extends NetarkivetException {
    /**
     * Constructs new ArgumentNotValid with the specified detail message.
     *
     * @param message The detail message
     */
    public ArgumentNotValid(String message) {
        super(message);
    }

    /**
     * Constructs new ArgumentNotValid with the specified detail
     * message and cause.
     *
     * @param message The detail message
     * @param cause   The cause
     */
    public ArgumentNotValid(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Check if a String argument is null or the empty string.
     *
     * @param val  the value to check
     * @param name the name of the value being checked
     * @throws ArgumentNotValid if test fails
     */
    public static void checkNotNullOrEmpty(String val, String name) {
        checkNotNull(val, name);

        if (val.isEmpty()) {
            throw new ArgumentNotValid("The value of the variable '" + name
                    + "' must not be an empty string.");
        }
    }

    /**
     * Check if an Object argument is null.
     *
     * @param val  the value to check
     * @param name the name of the value being checked
     * @throws ArgumentNotValid if test fails
     */
    public static void checkNotNull(Object val, String name) {
        if (val == null) {
            throw new ArgumentNotValid("The value of the variable '" + name
                    + "' must not be null.");
        }
    }

    /**
     * Check if an int argument is less than 0.
     *
     * @param num  argument to check
     * @param name the name of the value being checked
     * @throws ArgumentNotValid if test fails
     */
    public static void checkNotNegative(int num, String name) {
        if (num < 0) {
            throw new ArgumentNotValid("The value of the variable '" + name
                    + "' must be non-negative, but is " + num + ".");
        }
    }

    /**
     * Check if a long argument is less than 0.
     *
     * @param num argument to check
     * @param name the name of the value being checked
     * @throws ArgumentNotValid if test fails
     */
    public static void checkNotNegative(long num, String name) {
        if (num < 0) {
            throw new ArgumentNotValid("The value of the variable '" + name
                    + "' must be non-negative, but is " + num + ".");
        }
    }

    /**
     * Check if an int argument is less than or equal to 0.
     *
     * @param num  argument to check
     * @param name the name of the value being checked
     * @throws ArgumentNotValid if test fails
     */
    public static void checkPositive(int num, String name) {
        if (num <= 0) {
            throw new ArgumentNotValid("The value of the variable '" + name
                    + "' must be positive, but is " + num + ".");
        }
    }

    /**
     * Check if a long argument is less than 0.
     *
     * @param num argument to check
     * @param name the name of the value being checked
     * @throws ArgumentNotValid if test fails
     */
    public static void checkPositive(long num, String name) {
        if (num <= 0) {
            throw new ArgumentNotValid("The value of the variable '" + name
                    + "' must be positive, but is " + num + ".");
        }
    }

    /**
     * Check if a List argument is not null and the list is not empty.
     *
     * @param c argument to check
     * @param name the name of the value being checked
     * @throws ArgumentNotValid if test fails
     */
    public static void checkNotNullOrEmpty(Collection c, String name) {
        checkNotNull(c, name);

        if (c.isEmpty()) {
            throw new ArgumentNotValid("The contents of the variable '" + name
                        + "' must not be empty.");
        }
    }

    /**
     * Check that some condition on input parameters is true and throw an
     * ArgumentNotValid if it is false.
     * @param b the condition to check
     * @param s the error message to be reported
     * @throws ArgumentNotValid if b is false
     */
    public static void checkTrue(boolean b, String s) {
        if (!b) {
            throw new ArgumentNotValid(s);
        }
    }
}
