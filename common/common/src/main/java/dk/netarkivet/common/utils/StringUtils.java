/*
* File:     $Id$
* Revision: $Revision$
* Author:   $Author$
* Date:     $Date$
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
package dk.netarkivet.common.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 * Utilities for working with strings.
 */
public class StringUtils {
    /** Utillity class, do not initialise. */
    private StringUtils() {}

    /**
     * Replace all occurrences of oldString with newString in a string.
     * @param sentence the string, where all occurrences of oldString are to be
     *  replaced with newString.
     * @param oldString the oldString.
     * @param newString the newString.
     * @return the resulting string, where all occurrences of oldString are
     *  replaced with newString.
     */
    public static final String replace(String sentence, String oldString,
                                       String newString) {
        StringBuffer newStr = new StringBuffer();
        int found = 0;
        int lastPointer = 0;
        do {
            found = sentence.indexOf(oldString, lastPointer);

            if (found < 0) {
                newStr.append(sentence
                        .substring(lastPointer, sentence.length()));
            } else {
                if (found > lastPointer) {
                    newStr.append(sentence.substring(lastPointer, found));
                newStr.append(newString);
                lastPointer = found + oldString.length();
                }
            }
        } while (found > -1);
        return newStr.toString();
    }

    /** Concatenate all objects in a collection with the given separator
     *  between each.  If the Collection is a List, this method will generate
     *  the conjoined string in list order.  If the objects are not Strings,
     *  the toString method will be used to convert them to strings.
     *
     * @param sep A string to separate the list items.
     * @param objects A collection of object to concatenate as a string.
     * @return The concatenated string, or null if the list was null.
     */
    public static final <T> String conjoin(String sep, Collection<T> objects
    ) {
        if (objects == null) {
            return null;
        }
        StringBuilder res = new StringBuilder();
        for (T o : objects) {
            if (res.length() != 0) {
                res.append(sep);
            }
            res.append(o);
        }
        return res.toString();
    }

    /** Concatenate all strings in a collection with the given separator
     *  between each.
     *
     * @param sep A string to separate the list items.
     * @param strings An array of strings to concatenate.
     * @return The concatenated string, or null if the list was null.
     */
    public static final String conjoin(String sep, String... strings) {
        if (strings == null) {
            return null;
        }
        StringBuilder res = new StringBuilder();
        for (String s : strings) {
            if (res.length() != 0) {
                res.append(sep);
            }
            res.append(s);
        }
        return res.toString();
    }


    /** Concatenate all strings in a collection, with the fixed strings
     * appended and prepended to each.
     *
     * @param strings A list of strings to join up.
     * @param pre A string that will be put in front of each string in the list.
     * @param post A string that will be put after each string in the list.
     * @return The joined string, or null if strings is null.
     */
    public static final String surjoin(List<String> strings,
                                       String pre, String post) {
        if (strings == null) {
            return null;
        }
        StringBuilder res = new StringBuilder();
        for (String s : strings) {
            res.append(pre);
            res.append(s);
            res.append(post);
        }
        return res.toString();
    }

    /** Repeat the string n times.
     *
     * @param s A string to repeat.
     * @param n How many times to repeat it.
     * @return A repeated string.
     * @throws ArgumentNotValid if a negative amount is specified.
     */
    public static final String repeat(String s, int n) {
        ArgumentNotValid.checkNotNegative(n, "int n");
        String s1 = "";
        for (int i = 0; i < n; i++) {
            s1 += s;
        }
        return s1;
    }

    /**
     * Change all Strings to Integers.
     * @param stringArray the given array of Strings to convert.
     * @return a List of Integers.
     */
    public static List<Integer> parseIntList(String[] stringArray) {
        List<Integer> resultList = new ArrayList<Integer>();
        for (String element : stringArray) {
            try {
                resultList.add(Integer.parseInt(element));
            } catch (NumberFormatException e) {
                throw new ArgumentNotValid("Unable to parse '"
                        +  element + "' as int");
            }
        }
        return resultList;
    }

    /**
     * Generate a ellipsis of orgString. If orgString is longer than
     * maxLength, then we return a String containing the first maxLength
     * characters and then append  " ..".
     * @param orgString the original string.
     * @param maxLength the maximum length of the string before ellipsing it.
     * @return an ellipsis of orgString.
     */
    public static String makeEllipsis(String orgString, int maxLength)  {
    	ArgumentNotValid.checkNotNull(orgString, "String orgString");
    	String resultString = orgString;
        if (orgString.length() > maxLength) {
        	resultString = orgString.substring(0, maxLength - 1)
        		+ " ..";
        }
        return resultString;
    }
}
