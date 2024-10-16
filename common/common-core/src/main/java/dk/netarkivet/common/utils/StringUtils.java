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
package dk.netarkivet.common.utils;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 * Utilities for working with strings.
 */
public final class StringUtils {

    /**
     * Utility class, do not initialise.
     */
    private StringUtils() {
    }

    /**
     * Replace all occurrences of oldString with newString in a string.
     *
     * @param sentence the string, where all occurrences of oldString are to be replaced with newString.
     * @param oldString the oldString.
     * @param newString the newString.
     * @return the resulting string, where all occurrences of oldString are replaced with newString.
     */
    public static String replace(String sentence, String oldString, String newString) {
        StringBuilder newStr = new StringBuilder();
        int found = 0;
        int lastPointer = 0;
        do {
            found = sentence.indexOf(oldString, lastPointer);

            if (found < 0) {
                newStr.append(sentence.substring(lastPointer, sentence.length()));
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

    /**
     * Concatenate all objects in a collection with the given separator between each. If the Collection is a List, this
     * method will generate the conjoined string in list order. If the objects are not Strings, the toString method will
     * be used to convert them to strings.
     *
     * @param sep A string to separate the list items.
     * @param objects A collection of object to concatenate as a string.
     * @param <T> The type of objects to conjoin.
     * @return The concatenated string, or null if the list was null.
     */
    public static <T> String conjoin(String sep, Collection<T> objects) {
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

    /**
     * Concatenate the string representation of a maximum number of objects in a collection with a given separator
     * between them. If the Collection is a List, this method will generate the conjoined string in list order. If the
     * objects are not Strings, the toString method will be used to convert them to strings.
     *
     * @param <T> The type of collection.
     * @param separator The string to separate the entries in the collection with. This is allowed to be the empty
     * string.
     * @param objects The collection to have the string representation of its entries concatenated.
     * @param max The maximum number of objects in the collection to concatenate. If this number is 0 or below only the
     * first entry in the collection is returned.
     * @return The concatenation of the string representation of a limited amount of entries in the collection.
     * @throws ArgumentNotValid If the separator or the objects are null.
     */
    public static <T> String conjoin(String separator, Collection<T> objects, int max) throws ArgumentNotValid {
        ArgumentNotValid.checkNotNull(separator, "String separator");
        ArgumentNotValid.checkNotNull(objects, "Collection<T> objects");

        StringBuilder res = new StringBuilder();
        int index = 0;
        // go through all the objects.
        for (T o : objects) {
            if (res.length() != 0) {
                res.append(separator);
            }
            res.append(o);

            // check if max is reached.
            ++index;
            if (index > max) {
                break;
            }
        }

        return res.toString();
    }

    /**
     * Concatenate all strings in a collection with the given separator between each.
     *
     * @param sep A string to separate the list items.
     * @param strings An array of strings to concatenate.
     * @return The concatenated string, or null if the list was null.
     */
    public static String conjoin(String sep, String... strings) {
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

    /**
     * Concatenate all strings in a collection, with the fixed strings appended and prepended to each.
     *
     * @param strings A list of strings to join up.
     * @param pre A string that will be put in front of each string in the list.
     * @param post A string that will be put after each string in the list.
     * @return The joined string, or null if strings is null.
     */
    public static String surjoin(List<String> strings, String pre, String post) {
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

    /**
     * Repeat the string n times.
     *
     * @param s A string to repeat.
     * @param n How many times to repeat it.
     * @return A repeated string.
     * @throws ArgumentNotValid if a negative amount is specified.
     */
    public static String repeat(String s, int n) {
        ArgumentNotValid.checkNotNegative(n, "int n");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            sb.append(s);
        }
        return sb.toString();
    }

    /**
     * Change all Strings to Integers.
     *
     * @param stringArray the given array of Strings to convert.
     * @return a List of Integers.
     */
    public static List<Integer> parseIntList(String[] stringArray) {
        List<Integer> resultList = new ArrayList<Integer>();
        for (String element : stringArray) {
            try {
                resultList.add(Integer.parseInt(element));
            } catch (NumberFormatException e) {
                throw new ArgumentNotValid("Unable to parse '" + element + "' as int");
            }
        }
        return resultList;
    }

    /**
     * Generate a ellipsis of orgString. If orgString is longer than maxLength, then we return a String containing the
     * first maxLength characters and then append " ..".
     *
     * @param orgString the original string.
     * @param maxLength the maximum length of the string before ellipsing it.
     * @return an ellipsis of orgString.
     */
    public static String makeEllipsis(String orgString, int maxLength) {
        ArgumentNotValid.checkNotNull(orgString, "String orgString");
        String resultString = orgString;
        if (orgString.length() > maxLength) {
            resultString = orgString.substring(0, maxLength - 1) + " ..";
        }
        return resultString;
    }

    /** A day in seconds. */
    private static final long DAY = 60 * 60 * 24;
    /** An hour in seconds. */
    private static final long HOUR = 60 * 60;
    /** A minute in seconds. */
    private static final long MINUTE = 60;
    /** Decimal pattern. */
    private static String DECIMAL_PATTERN = "###,###.##";
    /** Formats a decimal number. */
    private static final DecimalFormat DECIMAL = new DecimalFormat(DECIMAL_PATTERN);

    /** Default date format : yyyy/MM/dd HH:mm:ss */
    private static final SimpleDateFormat DEFAULT_DATE = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

    /**
     * Formats a duration in seconds as a string of the form "3d 04:12:56".
     *
     * @param seconds A duration in seconds
     * @return a formatted string of the form "3d 04:12:56"
     */
    public static String formatDuration(long seconds) {
        if (seconds > 0L) {
            long lRest;

            String strDays = formatDurationLpad(String.valueOf(seconds / DAY)) + "d ";
            lRest = seconds % DAY;

            String strHours = formatDurationLpad(String.valueOf(lRest / HOUR)) + ":";
            lRest %= HOUR;

            String strMinutes = formatDurationLpad(String.valueOf(lRest / MINUTE)) + ":";
            lRest %= MINUTE;

            String strSeconds = formatDurationLpad(String.valueOf(lRest));

            return strDays + strHours + strMinutes + strSeconds;

        } else if (seconds == 0L) {
            return "0d 00:00:00";
        } else {
            return "-1";
        }
    }

    /**
     * Leftpad the string with "0", if the string is only one character long.
     *
     * @param s The given string
     * @return Return a string leftpadded with a "0" if the string is only one character long, Otherwise just return the
     * string.
     */
    private static String formatDurationLpad(final String s) {
        return (s.length() == 1 ? "0" + s : s);
    }

    /**
     * Formats a numeric percentage, as a decimal number with at most 2 digits.
     *
     * @param percentage the numeric percentage to format.
     * @return a formatted percentage string.
     */
    public static String formatPercentage(double percentage) {
        return formatNumber(percentage, null) + "%";
    }

    /**
     * Formats a numeric percentage, as a decimal number with at most 2 digits.
     *
     * @param percentage the numeric percentage to format.
     * @return a formatted percentage string.
     */
    public static String formatPercentage(long percentage) {
        return formatNumber(percentage, null) + "%";
    }

    /**
     * Formats a numeric percentage, as a decimal number with at most 2 digits.
     *
     * @param percentage the numeric percentage to format.
     * @param locale
     * @return a formatted percentage string.
     */
    public static String formatPercentage(double percentage, Locale locale) {
        return formatNumber(percentage, locale) + "%";
    }

    /**
     * Formats a numeric percentage, as a decimal number with at most 2 digits.
     *
     * @param percentage the numeric percentage to format.
     * @param locale
     * @return a formatted percentage string.
     */
    public static String formatPercentage(long percentage, Locale locale) {
        return formatNumber(percentage, locale) + "%";
    }

    /**
     * Formats a number, as a decimal number with at most 2 digits.
     *
     * @param number the number to format.
     * @param locale
     * @return a formatted number string.
     */
    public static String formatNumber(double number, Locale locale) {
        if  (locale == null)
            return DECIMAL.format(number);
        else {
            DecimalFormat decimalLocalized = new DecimalFormat(DECIMAL_PATTERN, new DecimalFormatSymbols(locale));
            return decimalLocalized.format(number);
        }

    }

    /**
     * Formats a number, as a decimal number with at most 2 digits.
     *
     * @param number the number to format.
     * @param locale
     * @return a formatted number string.
     */
    public static String formatNumber(long number, Locale locale) {
        if (locale == null)
            return DECIMAL.format(number);
        else {
            DecimalFormat decimalLocalized = new DecimalFormat(DECIMAL_PATTERN, new DecimalFormatSymbols(locale));
            return decimalLocalized.format(number);
        }
    }

    /**
     * Formats the given date (as elapsed milliseconds) using the default format 'yyyy/MM/dd HH:mm:ss'.
     *
     * @param millis the date
     * @return a formatted date string
     */
    public synchronized static String formatDate(long millis) {
        return DEFAULT_DATE.format(new Date(millis));
    }

    /**
     * Formats the given date (as elapsed milliseconds) using the provided format pattern.
     *
     * @param millis the date
     * @param format the format pattern {@link SimpleDateFormat}
     * @return a formatted date string
     */
    public static String formatDate(long millis, String format) {
        return new SimpleDateFormat(format).format(new Date(millis));
    }

    /**
     * Given an input String, this method splits the String with newlines into a multiline String with line-lengths
     * approximately lineLength. The split is made at the first blank space found at more than lineLength characters
     * after the previous split.
     *
     * @param input the input String.
     * @param lineLength the desired line length.
     * @return the split String.
     * @throws ArgumentNotValid if the input is null or the lineLength is not positive
     */
    public static String splitStringOnWhitespace(String input, int lineLength) {
        ArgumentNotValid.checkNotNull(input, "input");
        ArgumentNotValid.checkPositive(lineLength, "lineLength");
        input = input.trim();
        String[] inputLines = input.split("\n");
        StringBuffer output = new StringBuffer();
        for (int i = 0; i < inputLines.length; i++) {
            int foundIndex = 0;
            String inputLine = inputLines[i];
            while (foundIndex != -1) {
                foundIndex = inputLine.indexOf(" ", foundIndex + lineLength);
                // We split after the found blank space so check that this is
                // meaningful.
                if (foundIndex != -1 && inputLine.length() > foundIndex + 1) {
                    inputLine = inputLine.substring(0, foundIndex + 1) + "\n" + inputLine.substring(foundIndex + 1);
                }
            }
            output.append(inputLine);
            output.append("\n");
        }
        return output.toString();
    }

    /**
     * Given a multi-line input string, this method splits the string so that no line has length greater than
     * maxLineLength. Any input lines less than or equal to this length remain unaffected.
     *
     * @param input the input String.
     * @param maxLineLength the maximum permitted line length.
     * @return the split multi-line String.
     * @throws ArgumentNotValid if input is null or maxLineLength is non-positive
     */
    public static String splitStringForce(String input, int maxLineLength) {
        ArgumentNotValid.checkNotNull(input, "input");
        ArgumentNotValid.checkPositive(maxLineLength, "maxLineLength");
        input = input.trim();
        String[] inputLines = input.split("\n");
        StringBuffer output = new StringBuffer();
        for (String inputLine : inputLines) {
            int lastSplittingIndex = 0;
            int currentLineLength = inputLine.length();
            boolean stillSplitting = true;
            while (stillSplitting) {
                int nextSplittingIndex = lastSplittingIndex + maxLineLength;
                if (nextSplittingIndex < currentLineLength - 1) {
                    output.append(inputLine.substring(lastSplittingIndex, nextSplittingIndex));
                    output.append("\n");
                    lastSplittingIndex = nextSplittingIndex;
                } else {
                    output.append(inputLine.substring(lastSplittingIndex));
                    output.append("\n");
                    stillSplitting = false;
                }
            }
        }
        return output.toString().trim();
    }

}
