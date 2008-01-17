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
package dk.netarkivet.testutils;

import junit.framework.TestCase;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;

/**
 * Created by IntelliJ IDEA.
 * User: csr
 * Date: Mar 4, 2005
 * Time: 2:26:06 PM
 * To change this template use File | Settings | File Templates.
 */
public class Serial {

    /**
     * Serializes an Object, deserializes it, and then returns it.
     * @param input_object
     * @return Object
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public static<T  extends Serializable> T serial (T input_object) throws IOException, ClassNotFoundException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        ObjectOutputStream ous = new ObjectOutputStream(baos);
        ous.writeObject(input_object);
        ous.close();
        baos.close();
        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()));
        return (T) ois.readObject();
    }

    /** Check that transient fields are properly initialized after the
     * object has been serialized and deserialized.
     *
     * @param obj A Serializable object to test.
     * @param excludedFields Names of fields that we do not care are not
     *  initialized after deserialization.  Could be integers that are properly
     *  initialized to 0 or the like.  Only include fields here if you are sure
     *  that initialization is not required!
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws IllegalAccessException
     */
    public static<T extends Serializable>
    void assertTransientFieldsInitialized(T obj, String... excludedFields)
            throws IOException, ClassNotFoundException, IllegalAccessException {
        T obj2 = serial(obj);
        Field[] fields = obj2.getClass().getDeclaredFields();
        for (Field f : fields) {
            if (Modifier.isTransient(f.getModifiers())) {
                if (!Arrays.asList(excludedFields).contains(f.getName())) {
                    f.setAccessible(true);
                    final Class<?> type = f.getType();
                    final Object value = f.get(obj2);
                    if (type.isPrimitive()) {
                        if (value == null) {
                            TestCase.fail("Primitive field " + f
                                    + " is strangely null after deserialization");
                        }
                        if (type == Boolean.TYPE && value.equals(Boolean.valueOf(false))
                                || type == Byte.TYPE && value.equals(Byte.valueOf((byte)0))
                                || type == Character.TYPE && value.equals(Character.valueOf('\0'))
                                || type == Short.TYPE && value.equals(Short.valueOf((short)0))
                                || type == Integer.TYPE && value.equals(Integer.valueOf(0))
                                || type == Long.TYPE && value.equals(Long.valueOf(0L))
                                || type == Float.TYPE && value.equals(Float.valueOf(0.0f))
                                || type == Double.TYPE && value.equals(Double.valueOf(0.0))) {
                            TestCase.fail("Primitive field " + f
                                    + " has default value " + value
                                    + " after deserialization");
                        }
                    } else {
                        if (value == null) {
                            TestCase.fail("Field " + f
                                    + " is null after deserialization");
                        }
                    }
                }
            }
        }
    }
}

