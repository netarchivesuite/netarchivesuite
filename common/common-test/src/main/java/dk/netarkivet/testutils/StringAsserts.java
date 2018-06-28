/*
 * #%L
 * Netarchivesuite - common - test
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

package dk.netarkivet.testutils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import junit.framework.TestCase;

/**
 * More complex asserts for strings
 */

public class StringAsserts {
    /**
     * Assert that one string contains another.
     *
     * @param msg the error message to show in case of failure
     * @param toFind The string to find.
     * @param toCheck The string to look in
     */
    public static void assertStringContains(String msg, String toFind, String toCheck) {
        if (!toCheck.contains(toFind)) {
            TestCase.fail(msg + ": String '" + toFind + "' not in target:\n" + toCheck);
        }
    }

    /**
     * Asserts that one string contains a set of substrings
     *
     * @param msg the error message to show in case of failure
     * @param toCheck The string to look in
     * @param contents the strings to look for
     */
    public static void assertStringContains(String msg, String toCheck, String... contents) {
        List<String> failures = new ArrayList<String>();
        for (String content : contents) {
            if (!toCheck.contains(content)) {
                failures.add(content);
            }
        }
        if (!failures.isEmpty()) {
            StringBuffer message = new StringBuffer("'" + toCheck + "' should contain ");
            for (String failure : failures) {
                message.append("'" + failure + "'");
            }
            message.append(" but doesn't");
            TestCase.fail(msg + ": " + message);
        }
    }

    /**
     * Assert that a string matches a regular expression.
     *
     * @param msg the error message to show in case of failure
     * @param toFind The string to find
     * @param toCheck The string to look in
     */
    public static void assertStringMatches(String msg, String toFind, String toCheck) {
        if (!Pattern.compile(toFind, Pattern.MULTILINE | Pattern.DOTALL).matcher(toCheck).find()) {
            TestCase.fail(msg + ": Regex '" + toFind + "' not in target:\n" + toCheck);
        }
    }

    /**
     * Assert that one string doesn not contain another.
     *
     * @param msg the error message to show in case of failure
     * @param toFind The string to find.
     * @param toCheck The string to look in
     */
    public static void assertStringNotContains(String msg, String toFind, String toCheck) {
        if (toCheck.contains(toFind)) {
            TestCase.fail(msg + ": String '" + toFind + "' in target:\n" + toCheck);
        }

    }

}
