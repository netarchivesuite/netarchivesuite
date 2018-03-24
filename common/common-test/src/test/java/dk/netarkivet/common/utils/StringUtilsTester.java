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
package dk.netarkivet.common.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Test;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.testutils.StringAsserts;

/**
 * Unit tests for the class StringUtils.
 */
public class StringUtilsTester {

    @Test
    public final void testReplace() {
        String testString = "This is a test $ string \\ , it is nice!";
        assertEquals("This is a test new word string \\ , it is nice!",
                StringUtils.replace(testString, "$", "new word"));
        assertEquals("This is a test $ string new word , it is nice!",
                StringUtils.replace(testString, "\\", "new word"));
        assertEquals("This is a test and it works",
                StringUtils.replace(testString, "$ string \\ , it is nice!", "and it works"));
        assertEquals("This is a test $ string \\ , it is nice!",
                StringUtils.replace(testString, "cat", "shouldnt work"));
    }

    @Test
    public void testConjoinStrings() throws Exception {
        assertEquals("Should give correct conjoined list with strings", "a-b-c", conjoinList("-", "a", "b", "c"));
        assertEquals("Should give simple string for single-element list", "c", conjoinList("-", "c"));
        assertEquals("Should turn objects into strings", "1-2", conjoinList("-", 1, 2));
        assertEquals("Should turn null elements into null strings", "1-null", conjoinList("-", 1, null));

        assertNull("Should get null on null list", conjoinList("-", (String[]) null));
        assertEquals("Should be able to use different modifier", "d%e", conjoinList("%", 'd', 'e'));
        String[] strings = null;
        assertEquals(null, StringUtils.conjoin(" ", strings));
    }

    /** Helper method that allows varargs. */
    private static <T> String conjoinList(String sep, T... objects) {
        if (objects == null) {
            // Can't do Arrays.asList(null)
            return StringUtils.conjoin(sep, (Collection<String>) null);
        } else {
            return StringUtils.conjoin(sep, Arrays.asList(objects));
        }
    }

    @Test
    public void testSurjoin() throws Exception {
        assertEquals("Should give correct surjoin for simple args", ":a/:b/:c/", surjoinList(":", "/", "a", "b", "c"));
        assertEquals("Should give simple string for single-element list", "#dgee", surjoinList("#", "gee", "d"));
        assertEquals("Should work with empty pre and post", "abc", surjoinList("", "", "a", "b", "c"));
        assertEquals("Should turn nulls into string versions", "%a%null", surjoinList("%", "", "a", null));
        assertNull("Should get null on null list", surjoinList("foo", "bar", (String[]) null));
        assertEquals("Should do nothing for empty list", "", surjoinList("foo", "bar"));
    }

    /** Helper method that allows varargs. */
    private String surjoinList(String pre, String post, String... strings) {
        if (strings == null) {
            return StringUtils.surjoin(null, pre, post);
        } else {
            return StringUtils.surjoin(Arrays.asList(strings), pre, post);
        }
    }

    @Test
    public void testRepeat() throws Exception {
        assertEquals("Should repeat single string three times with arg 3", "ababab", StringUtils.repeat("ab", 3));
        assertEquals("Should give nothing on zero repeats", "", StringUtils.repeat("foo", 0));
        try {
            StringUtils.repeat("foo", -1);
            fail("Should not be possible to repeat negative amount");
        } catch (ArgumentNotValid e) {
            StringAsserts.assertStringContains("Should mention amount", " -1", e.getMessage());
        }
    }

    @Test
    public void testSplitString() {
        String input = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Aliquam volutpat euismod aliquet. Nullam vestibulum mollis arcu, quis laoreet nibh aliquet et. In at ligula pellentesque magna placerat luctus. Donec mauris nibh, lacinia non feugiat quis, dapibus id orci. Suspendisse sollicitudin suscipit sodales. Mauris interdum consectetur nunc sed interdum. Nulla facilisi. Quisque urna lectus, tempor ut feugiat sit amet, congue eget lectus. Duis eget interdum turpis. Morbi turpis arcu, venenatis ac venenatis nec, pretium ac tellus. Fusce condimentum iaculis sem. Cras eros dui, imperdiet vitae faucibus feugiat, pellentesque eu quam. In dignissim facilisis sollicitudin. Cras tincidunt arcu at lectus tincidunt a porta lorem accumsan. Pellentesque porta, est at viverra sagittis, est elit congue lorem, feugiat lobortis tellus nisl in augue.";
        String output = StringUtils.splitStringOnWhitespace(input, 50);
        // System.out.println(output);
        assertTrue("Should have split String into multiple lines", output.split("\n").length > 5);
    }

    @Test
    public void testSplitStringAlreadySplit() {
        String input = "Lorem ipsum dolor\n sit amet, consectetur\n adipiscing elit. Aliquam\n volutpat euismod aliquet.\n Nullam vestibulum mol";
        String output = StringUtils.splitStringOnWhitespace(input, 50);
        // System.out.println(output);
        assertEquals("Splitter should not split already split string", output.split("\n").length,
                input.split("\n").length);
    }

    /**
     * test that code doesn't fail on special cases
     */
    @Test
    public void testSplitStringSpecialCases() {
        String input = "                                                                                   ";
        String output = StringUtils.splitStringOnWhitespace(input, 50);
        assertNotNull(output);
        // System.out.printl(output);
        input = "";
        output = StringUtils.splitStringOnWhitespace(input, 50);
        assertNotNull(output);
        // System.out.printl(output);
        output = StringUtils.splitStringOnWhitespace(input, 50);
        // System.out.printl(output);
        assertNotNull(output);
        input = "\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n";
        output = StringUtils.splitStringOnWhitespace(input, 50);
        // System.out.printl(output);
        assertNotNull(output);
        input = " \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n";
        output = StringUtils.splitStringOnWhitespace(input, 50);
        // System.out.printl(output);
        assertNotNull(output);
        input = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Aliquam volutpat euismod aliquet. Nullam vestibulum mollis                           ";
        output = StringUtils.splitStringOnWhitespace(input, 50);
        // System.out.printl(output);
        assertNotNull(output);

    }

    @Test
    public void testSplitStringForce() {
        String input = "abcdefghijkl";
        assertEquals("abc\ndef\nghi\njkl", StringUtils.splitStringForce(input, 3));
    }

    /** test of method {@link StringUtils.parseIntList} */
    @Test
    public void testParseIntlist() {
        try {
            StringUtils.parseIntList(null);
            fail("parseIntList should not work with null arg");
        } catch (Exception e) {
            // Exception
        }

        String[] stringInts = new String[] {"2", "53", "55"};

        List<Integer> targetList = new ArrayList<Integer>();
        targetList.add(Integer.valueOf(2));
        targetList.add(Integer.valueOf(53));
        targetList.add(Integer.valueOf(55));

        targetList.removeAll(StringUtils.parseIntList(stringInts));
        assertTrue(targetList.isEmpty());

        String[] stringIntsWithUnparseableInt = new String[] {"2", "53", "55", "4#"};

        try {
            StringUtils.parseIntList(stringIntsWithUnparseableInt);
            fail("Should throw ArgumentNotValid with argument containing string unparseable as int");
        } catch (ArgumentNotValid e) {
            // Expected
        }
    }

    @Test
    public void testMakeEllipsis() {
        // if length of input is less than maxSize, then output equals input
        // if not, output equals the first maxSize characters plus " .."
        String input = "Lorem ipsum dolor sit amet, consectetur adipiscing elit.";
        String output = StringUtils.makeEllipsis(input, input.length());
        assertEquals(input, output);

        String expectedOutput = "Lorem ipsum dolor sit amet, consectetur adipiscing elit.".substring(0,
                input.length() - 6) + " ..";
        output = StringUtils.makeEllipsis(input, input.length() - 5);
        assertEquals(expectedOutput, output);
    }

    @Test
    public void testFormatDate() {
        String expectedOutput = "1970/01/01 01:00:00";
        assertEquals(expectedOutput, StringUtils.formatDate(0));
        long aDateInMillis = 1324577151594L;
        expectedOutput = "2011/12/22 19:05:51";
        assertEquals(expectedOutput, StringUtils.formatDate(aDateInMillis));

        String defaultFormat = "yyyy/MM/dd HH:mm:ss";
        expectedOutput = "1970/01/01 01:00:00";
        assertEquals(expectedOutput, StringUtils.formatDate(0, defaultFormat));
        expectedOutput = "2011/12/22 19:05:51";
        assertEquals(expectedOutput, StringUtils.formatDate(aDateInMillis, defaultFormat));
    }

    /** test the methods to format a double or a long percentage. */
    @Test
    public void testFormatPercentage() {
        long aLong = 10;
        double aDouble = 2.14;
        // double asecondDouble = 2.145;

        assertEquals("10%", StringUtils.formatPercentage(aLong));
        assertEquals("2.14%", StringUtils.formatPercentage(aDouble));
        // FIXME: Rounding changed between Java 6 and 7.
        // Rounding occurs (/tra: upwards - RoundingMode.HALF_EVEN)
        // assertEquals("2.15%", StringUtils.formatPercentage(asecondDouble));
    }

    /** Test boundary conditions for StringUtils.formatDuration method. */
    @Test
    public void testFormatDuration() {
        assertEquals("0d 00:00:00", StringUtils.formatDuration(0L));
        assertEquals("-1", StringUtils.formatDuration(-12L));
    }

    /** test the methods to format a double or a long. */
    @Test
    public void testFormatNumber() {
        long aLong = 10;
        double aDouble = 2.14;
        // double asecondDouble = 2.145;

        assertEquals("10", StringUtils.formatNumber(aLong));
        assertEquals("2.14", StringUtils.formatNumber(aDouble));
        // FIXME: Rounding changed between Java 6 and 7.
        // Rounding occurs (/tra: upwards - RoundingMode.HALF_EVEN)
        // assertEquals("2.15", StringUtils.formatNumber(asecondDouble));
    }
}
