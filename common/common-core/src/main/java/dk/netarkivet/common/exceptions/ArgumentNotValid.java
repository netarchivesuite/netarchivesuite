/*
 * #%L
 * Netarchivesuite - common
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
package dk.netarkivet.common.exceptions;

import java.io.File;
import java.util.Collection;

/**
 * Indicates that one or more arguments are invalid.
 */
@SuppressWarnings("serial")
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
     * Constructs new ArgumentNotValid with the specified detail message and cause.
     *
     * @param message The detail message
     * @param cause The cause
     */
    public ArgumentNotValid(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Check if a String argument is null or the empty string.
     *
     * @param val the value to check
     * @param name the name and type of the value being checked
     * @throws ArgumentNotValid if test fails
     */
    public static void checkNotNullOrEmpty(String val, String name) {
        checkNotNull(val, name);

        if (val.isEmpty()) {
            throw new ArgumentNotValid("The value of the variable '" + name + "' must not be an empty string.");
        }
    }

    /**
     * Check if an Object argument is null.
     *
     * @param val the value to check
     * @param name the name and type of the value being checked.
     * @throws ArgumentNotValid if test fails
     */
    public static void checkNotNull(Object val, String name) {
        if (val == null) {
            throw new ArgumentNotValid("The value of the variable '" + name + "' must not be null.");
        }
    }

    /**
     * Check if an int argument is less than 0.
     *
     * @param num argument to check
     * @param name the name and type of the value being checked.
     * @throws ArgumentNotValid if test fails
     */
    public static void checkNotNegative(int num, String name) {
        if (num < 0) {
            throw new ArgumentNotValid("The value of the variable '" + name + "' must be non-negative, but is " + num
                    + ".");
        }
    }

    /**
     * Check if a long argument is less than 0.
     *
     * @param num argument to check
     * @param name the name and type of the value being checked.
     * @throws ArgumentNotValid if test fails
     */
    public static void checkNotNegative(long num, String name) {
        if (num < 0) {
            throw new ArgumentNotValid("The value of the variable '" + name + "' must be non-negative, but is " + num
                    + ".");
        }
    }

    /**
     * Check if an int argument is less than or equal to 0.
     *
     * @param num argument to check
     * @param name the name and type of the value being checked.
     * @throws ArgumentNotValid if test fails
     */
    public static void checkPositive(int num, String name) {
        if (num <= 0) {
            throw new ArgumentNotValid("The value of the variable '" + name + "' must be positive, but is " + num + ".");
        }
    }

    /**
     * Check if a long argument is less than 0.
     *
     * @param num argument to check
     * @param name the name and type of the value being checked.
     * @throws ArgumentNotValid if test fails
     */
    public static void checkPositive(long num, String name) {
        if (num <= 0) {
            throw new ArgumentNotValid("The value of the variable '" + name + "' must be positive, but is " + num + ".");
        }
    }

    /**
     * Check if a List argument is not null and the list is not empty.
     *
     * @param c argument to check
     * @param name the name and type of the value being checked.
     * @throws ArgumentNotValid if test fails
     */
    public static void checkNotNullOrEmpty(Collection<?> c, String name) {
        checkNotNull(c, name);

        if (c.isEmpty()) {
            throw new ArgumentNotValid("The contents of the variable '" + name + "' must not be empty.");
        }
    }

    /**
     * Check that some condition on input parameters is true and throw an ArgumentNotValid if it is false.
     *
     * @param b the condition to check
     * @param s the error message to be reported
     * @throws ArgumentNotValid if b is false
     */
    public static void checkTrue(boolean b, String s) {
        if (!b) {
            throw new ArgumentNotValid(s);
        }
    }

    /**
     * Check, if the given argument is an existing directory.
     *
     * @param aDir a given File object.
     * @param name Name of object
     * @throws ArgumentNotValid If aDir is not an existing directory
     */
    public static void checkExistsDirectory(File aDir, String name) {
        checkNotNull(aDir, name);
        if (!aDir.isDirectory()) {
            String message = "The file '" + aDir.getAbsolutePath() + "' does not exist or is not a directory.";
            throw new ArgumentNotValid(message);
        }
    }

    /**
     * Check, if the given argument is an existing normal file.
     *
     * @param aFile a given File object.
     * @param name Name of object
     * @throws ArgumentNotValid If aDir is not an existing file
     */
    public static void checkExistsNormalFile(File aFile, String name) {
        checkNotNull(aFile, name);
        if (!aFile.isFile()) {
            String message = "The file '" + aFile.getAbsolutePath() + "' does not exist or is not a normal file.";
            throw new ArgumentNotValid(message);
        }
    }
}
