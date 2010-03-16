/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
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

package dk.netarkivet.testutils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import dk.netarkivet.common.exceptions.PermissionDenied;
import dk.netarkivet.common.utils.CleanupIF;

/**
 * Utility class containing various method for making assertions on 
 * Class objects.
 * 
 */
public class ClassAsserts {

    /**
     * Tests the class has a static factory method getInstance()
     * @param c the class to test
     */
    public static void assertHasFactoryMethod(Class c) {
        Method m = null;
        try {
            m = c.getDeclaredMethod("getInstance", (Class[]) null);
        } catch (NoSuchMethodException e) {
            TestCase.fail("Expect to find getInstance() method in class " +
                    c.getName());
        }
        TestCase.assertEquals("getInstance() method should return " +
                "an object of the same class: ", c, m.getReturnType());
        TestCase.assertTrue("getInstance() method should be static:",
                Modifier.isStatic(m.getModifiers()));
    }

    /**
     * Tests that a class has a static factory method getInstance() and that
     * it acts as a singleton. NB This method will create an instance of the
     * class. It is your responsibility to clean up after yourself.
     * @param c the class to test
     * @return the singleton
     */
    public static <T> T assertSingleton(Class<T> c)  {
        assertHasFactoryMethod(c) ;
        assertPrivateConstructor(c);
        T o1 = null;
        T o2 = null;
        try {
            Method m = c.getDeclaredMethod("getInstance");
            o1 = (T) m.invoke(null);
            o2 = (T) m.invoke(null);
        } catch (Exception e) {
            throw new PermissionDenied("Unexpected error in unit test ", e);
        }
        TestCase.assertSame("Expect to find only one distinct instance of " +
                "class " + c.getName(), o1, o2);
        if (o1 instanceof CleanupIF) {
            ((CleanupIF) o1).cleanup();
        }
        return o1;
    }

    /** Tests if there are any public constructors. Will fail on any public
     * constructor, and simply return otherwise.
     *
     * @param c A class to test.
     */
    public static <T> void assertPrivateConstructor(Class<T> c) {
        TestCase.assertEquals("Expect to find no public constructors for " +
                "class " + c.getName(), 0, c.getConstructors().length);
    }

    /** Tests that a class' equals method notice changed
     * fields.  Also performs some testing of the hashCode method, but not
     * as comprehensive.
     *
     * @param o1 An object to test on.
     * @param o2 Another object with all fields to be tested set to
     * different values from o1
     * @param excludedFields A list of field names of fields that should
     * not be included in the test.
     * @throws IllegalAccessException
     */
    public static void assertEqualsValid(Object o1,
                                         Object o2,
                                         List excludedFields)
            throws IllegalAccessException {
        TestCase.fail("Unsafe to use with final fields - pending rethought");
        Class c = o1.getClass();
        TestCase.assertSame("Class must be the same for both objects",
                c, o2.getClass());
        Map<String,Object> fieldValues1 = new HashMap<String,Object>();
        Map<String,Object> fieldValues2 = new HashMap<String,Object>();
        Field[] fields = c.getDeclaredFields();
        for (final Field field : fields) {
            if (!excludedFields.contains(field.getName()) &&
                !Modifier.isStatic(field.getModifiers())) {
                field.setAccessible(true);
                Object fieldValue1 = field.get(o1);
                fieldValues1.put(field.getName(), fieldValue1);
                final Object fieldValue2 = field.get(o2);
                fieldValues2.put(field.getName(), fieldValue2);
                TestCase.assertNotSame(
                        "Values for field " + field + " should not be the same",
                        fieldValue1, fieldValue2);
                field.set(o2, fieldValue1);
            }
        }
        for (final Field field : fields) {
            if (!excludedFields.contains(field.getName()) &&
                !Modifier.isStatic(field.getModifiers())) {
                TestCase.assertEquals(
                        "Objects must be same with all fields equals",
                        o1, o2);
                field.set(o2, fieldValues2.get(field.getName()));
                TestCase.assertFalse(
                        "Changing field " + field + " should cause non-equals",
                        o1.equals(o2));
                field.set(o2, fieldValues1.get(field.getName()));
                TestCase.assertEquals(
                        "Objects must have same hashcode with all fields equals",
                        o1.hashCode(), o2.hashCode());
            }
        }
    }
}
