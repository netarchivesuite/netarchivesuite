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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;

/**
 * Unit tests for the SettingsFactory class.
 */
// FIXME: Need documentation.
public class SettingsFactoryTester {
    ReloadSettings rs = new ReloadSettings();

    @Before
    public void setUp() {
        rs.setUp();
    }

    @After
    public void tearDown() {
        rs.tearDown();
    }

    @Test
    public void testGetInstance() throws Exception {
        Settings.set(CommonSettings.ARC_REPOSITORY_CLIENT, Test1.class.getName());
        TestClass newClass = SettingsFactory.getInstance(CommonSettings.ARC_REPOSITORY_CLIENT);
        assertNotNull("Should be able to create trivial class", newClass);
        assertTrue("Class should have been made with getInstance", newClass.fromGetInstance);

        Settings.set(CommonSettings.ARC_REPOSITORY_CLIENT, Test2.class.getName());
        Test2 newClass2 = SettingsFactory.getInstance(CommonSettings.ARC_REPOSITORY_CLIENT, "FooBar", 23L);
        assertNotNull("Should be able to create trivial class", newClass2);
        assertTrue("Object should have been made with getInstance", newClass2.fromGetInstance);
        assertEquals("Object should get string arg", "FooBar", newClass2.arg1);
        assertEquals("Object should get long arg", 23, newClass2.arg2);

        Settings.set(CommonSettings.ARC_REPOSITORY_CLIENT, Test2.class.getName());
        Test2 newClass3 = SettingsFactory.getInstance(CommonSettings.ARC_REPOSITORY_CLIENT, "Barfu");
        assertNotNull("Should be able to create trivial class", newClass3);
        assertFalse("Class should not have been made with getInstance", newClass3.fromGetInstance);
        assertEquals("Object should get string arg", "Barfu", newClass3.arg1);
        assertEquals("Object should get have default long value", 42, newClass3.arg2);

        Settings.set(CommonSettings.ARC_REPOSITORY_CLIENT, Test2.class.getName());
        Test2 newClass4 = SettingsFactory.getInstance(CommonSettings.ARC_REPOSITORY_CLIENT, 43L);
        assertNotNull("Should be able to create trivial class", newClass4);
        assertFalse("Class should not have been made with getInstance", newClass4.fromGetInstance);
        assertEquals("Object should get default string arg", "foof", newClass4.arg1);
        assertEquals("Object should get have set long value", 43, newClass4.arg2);
    }

    static class TestClass {
        public boolean fromGetInstance;
    }

    static class Test1 extends TestClass {
        public static TestClass getInstance() {
            TestClass newClass = new Test1();
            newClass.fromGetInstance = true;
            return newClass;
        }
    }

    static class Test2 extends TestClass {
        String arg1;
        long arg2;

        public Test2(String s, long l) {
            arg1 = s;
            arg2 = l;
        }

        public Test2(String s) {
            arg1 = s;
            arg2 = 42;
        }

        public Test2(Long l) {
            arg1 = "foof";
            arg2 = l;
        }

        public static TestClass getInstance(String arg1, Long arg2) {
            TestClass newClass = new Test2(arg1, arg2);
            newClass.fromGetInstance = true;
            return newClass;
        }
    }
}
