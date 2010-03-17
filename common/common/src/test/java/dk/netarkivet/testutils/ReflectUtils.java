/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2010 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import dk.netarkivet.deploy.BuildCompleteSettings;

import junit.framework.Assert;;


/**
 * Methods that help in doing common reflection tasks
 *
 */

public class ReflectUtils {
    /** Look up a private method and make it accessible for testing.
     *
     * @param c Class to look in.
     * @param name Name of the method.
     * @param args Arguments for the method.  Note that primitive types are
     *  found using XXX.TYPE.
     * @return Method object, accessible for calling.
     * @throws NoSuchMethodException
     */
    public static Method getPrivateMethod(Class<?> c, String name, Class<?>... args)
            throws NoSuchMethodException {
        Method m = c.getDeclaredMethod(name, args);
        m.setAccessible(true);
        return m;
    }

    /** Look up a private field and make it accessible for testing.
     *
     * @param c The class that declares the field.
     * @param fieldName The name of the field.
     * @return The field, which can now be set.
     * @throws NoSuchFieldException If there is no such field declared in
     *  the class.
     */
    public static <T> Field getPrivateField(Class<?> c, String fieldName)
            throws NoSuchFieldException {
        Field f = c.getDeclaredField(fieldName);
        f.setAccessible(true);
        return f;
    }

    /** Look up a private constructor and make it accessible for testing.
     *
     * @param c Class to look in.
     * @param args Arguments for the constructor. Note that primitive types are
     *  found using XXX.TYPE.
     * @return Constructor object, accessible for calling.
     * @throws NoSuchMethodException
     */
    public static <T> Constructor<T> getPrivateConstructor(Class<T> c,
                                                           Class<?>... args)
            throws NoSuchMethodException {
        Constructor<T> con = c.getDeclaredConstructor(args);
        con.setAccessible(true);
        return con;
    }
    
    /**
     * Method for testing the constructor of a utility class (the constructor 
     * should be private).
     */
    public static void testUtilityConstructor(Class c) {
        Constructor<BuildCompleteSettings>[] constructors = c.getConstructors();
        
        Assert.assertEquals("There should be no public constructors.", 
                0, constructors.length);
        
        constructors = c.getDeclaredConstructors();
        Assert.assertEquals("There should be one constructor.", 1, constructors.length);
        
        for(Constructor<BuildCompleteSettings> con : constructors) {
            Assert.assertFalse("The constructor should not be accessible.", 
                    con.isAccessible());
            
            con.setAccessible(true);
            Assert.assertTrue("The constructor should now be accessible.", 
                    con.isAccessible());
            
            try {
                Object instance = con.newInstance(null);
                Assert.assertNotNull("It should be possible to instatiate now.", instance);
            } catch (Throwable e) {
                e.printStackTrace();
                Assert.fail(e.getMessage());
            }
        }
    }
}
