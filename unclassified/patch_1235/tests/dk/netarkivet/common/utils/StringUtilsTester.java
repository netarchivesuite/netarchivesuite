/*$Id$
* $Revision$
* $Date$
* $Author$
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

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.testutils.StringAsserts;

import junit.framework.TestCase;

import java.util.Arrays;
import java.util.Collection;

public class StringUtilsTester extends TestCase {

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public final void testReplace() {
        String testString = "This is a test $ string \\ , it is nice!";
        assertEquals("This is a test new word string \\ , it is nice!", StringUtils.replace(testString, "$", "new word"));
        assertEquals("This is a test $ string new word , it is nice!", StringUtils.replace(testString, "\\", "new word"));
        assertEquals("This is a test and it works", StringUtils.replace(testString, "$ string \\ , it is nice!", "and it works"));
        assertEquals("This is a test $ string \\ , it is nice!", StringUtils.replace(testString, "cat", "shouldnt work"));
    }

    public void testConjoinStrings() throws Exception {
        assertEquals("Should give correct conjoined list with strings",
                "a-b-c", conjoinList("-", "a", "b", "c"));
        assertEquals("Should give simple string for single-element list",
                "c", conjoinList("-", "c"));
        assertEquals("Should turn objects into strings",
                "1-2", conjoinList("-", 1, 2));
        assertEquals("Should turn null elements into null strings",
                "1-null", conjoinList("-", 1, null));

        assertNull("Should get null on null list", conjoinList("-",
                (String [])null));
        assertEquals("Should be able to use different modifier",
                "d%e", conjoinList("%", 'd', 'e'));
    }

    /** Helper method that allows varargs. */
    private static <T> String conjoinList(String sep, T... objects) {
        if (objects == null) {
            // Can't do Arrays.asList(null)
            return StringUtils.conjoin(sep,(Collection<String>)null );
        } else {
            return StringUtils.conjoin(sep, Arrays.asList(objects) );
        }
    }

    public void testSurjoin() throws Exception {
        assertEquals("Should give correct surjoin for simple args",
                ":a/:b/:c/", surjoinList(":", "/", "a", "b", "c"));
        assertEquals("Should give simple string for single-element list",
                "#dgee", surjoinList("#", "gee", "d"));
        assertEquals("Should work with empty pre and post",
                "abc", surjoinList("", "", "a", "b", "c"));
        assertEquals("Should turn nulls into string versions",
                "%a%null", surjoinList("%", "", "a", null));
        assertNull("Should get null on null list",
                surjoinList("foo", "bar", (String[])null));
        assertEquals("Should do nothing for empty list",
                "", surjoinList("foo", "bar"));
    }

    /** Helper method that allows varargs. */
    private String surjoinList(String pre, String post, String... strings) {
        if (strings == null) {
            return StringUtils.surjoin(null, pre, post);
        } else {
            return StringUtils.surjoin(Arrays.asList(strings), pre, post);
        }
    }

    public void testRepeat() throws Exception {
        assertEquals("Should repeat single string three times with arg 3",
                "ababab", StringUtils.repeat("ab", 3));
        assertEquals("Should give nothing on zero repeats",
                "", StringUtils.repeat("foo", 0));
        try {
            StringUtils.repeat("foo", -1);
            fail("Should not be possible to repeat negative amount");
        } catch (ArgumentNotValid e) {
            StringAsserts.assertStringContains("Should mention amount",
                    " -1", e.getMessage());
        }
    }
}
