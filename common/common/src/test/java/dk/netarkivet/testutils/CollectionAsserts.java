/* File:    $Id$
 * Version: $Revision$
 * Date:    $Date$
 * Author:  $Author$
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

package dk.netarkivet.testutils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import junit.framework.TestCase;

import dk.netarkivet.harvester.datamodel.Named;


/** 
 * Utilities for doing asserts on collections. 
 */
public class CollectionAsserts {
    
    /** Test that two iterators contain the same objects in the same order.
     * The objects are tested using equals().  The iterators will be used
     * by this.
     *
     * @param msg Failure message
     * @param i1 First iterator
     * @param i2 Second iterator
     */
    public static <T> void assertIteratorEquals(String msg, Iterator<T> i1, Iterator<T> i2) {
        while (i1.hasNext() && i2.hasNext()) {
            T o1 = i1.next();
            T o2 = i2.next();
            TestCase.assertEquals(msg, o1, o2);
        }
        if (i1.hasNext()) {
            TestCase.fail(msg + ": More elements in first iterator");
        }
        if (i2.hasNext()) {
            TestCase.fail(msg + ": More elements in second iterator");
        }
    }

    /** Assert iterators of Named or String-objects have equal names in any order.
     *
     * @param message Failure message
     * @param i1 First iterator
     * @param i2 Second iterator
     */
    public static void assertIteratorNamedEquals(String message, Iterator i1, Iterator i2) {
        String[] a1 = getSortedArray(i1, message);
        String[] a2 = getSortedArray(i2, message);
        TestCase.assertTrue(message + "\n List 1: " + Arrays.asList(a1) + "\n List 2: " 
                + Arrays.asList(a1), Arrays.equals(a1, a2));
    }

    private static String[] getSortedArray(Iterator i1, String message) {
        List<String> l1 = new ArrayList<String>();
        while(i1.hasNext()) {
            Object o = i1.next();
            if (o instanceof Named) {
                l1.add(((Named) o).getName());
            } else if (o instanceof String) {
                l1.add((String) o);
            } else {
                TestCase.fail(message + "(iterator of wrong kind of object '" 
                        + o + "' - " + o.getClass() + ")");
            }
        }
        String[] a1 = l1.toArray(new String[]{});
        Arrays.sort(a1);
        return a1;
    }

    /** Assert that a list contains the given elements in order
     *
     * @param msg A message in case of failure
     * @param actual A list of objects
     * @param expected The values that the list should contain.
     */
    public static void assertListEquals(String msg, List<? extends Object> actual,
                                        Object... expected) {
        if (actual == null) {
            TestCase.fail(msg + ": Null list not expected");
        }
        if (expected.length != actual.size()) {
            TestCase.fail(msg + ": Length mismatch: Expected " + expected.length
                    + ", but got " + actual.size() + "\nExpected list: "
                    + Arrays.asList(expected) + "\nActual list: " + actual);
        }
        for (int i = 0; i < expected.length; i++) {
            final Object expectedValue = expected[i];
            final Object actualValue = actual.get(i);
            if (expectedValue != null) {
                if (actualValue == null) {
                    TestCase.fail(msg + ": Element " + i + " should be '" +
                            expectedValue + "', but was null");
                } else if (!expectedValue.equals(actualValue)) {
                    TestCase.fail(msg + ": Element " + i + " should be '" +
                            expectedValue + "', but was '" + actualValue + "'");
                }
            } else {
                if (actualValue != null) {
                    TestCase.fail(msg + ": Element " + i
                            + " should be null, but was '" + actualValue);
                }
            }
        }
    }
}
