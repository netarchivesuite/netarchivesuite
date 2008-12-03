/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
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

import junit.framework.TestCase;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.testutils.ClassAsserts;
import dk.netarkivet.testutils.StringAsserts;

/**
 * Unit tests for the ExceptionUtils class.
 */
public class ExceptionUtilsTester extends TestCase {
    public ExceptionUtilsTester(String s) {
        super(s);
    }

    public void setUp() {
    }

    public void tearDown() {
    }

    /** Test that this utility class cannot be instantiated. */
    public void testExceptionUtils() {
        ClassAsserts.assertPrivateConstructor(ExceptionUtils.class);
    }

    /** Test that exceptions are printed including all stacktraces,
     * and that nul exceptions are turned into 'null\n'. */
    public void testGetStackTrace() throws Exception {
        assertEquals(
                "Null exceptions should simply return 'null' and a linebreak",
                "null\n", ExceptionUtils.getStackTrace(null));
        String exceptionMessage = "Test";
        ArgumentNotValid cause = new ArgumentNotValid(exceptionMessage);
        ArgumentNotValid throwable = new ArgumentNotValid(exceptionMessage,
                                                          cause);
        String result = ExceptionUtils.getStackTrace(
                throwable);
        StringAsserts.assertStringContains("Should contain the exception",
                                           ArgumentNotValid.class.getName(),
                                           result);
        StringAsserts.assertStringContains("Should contain the exception msg",
                                           exceptionMessage,
                                           result);
        StringAsserts.assertStringContains("Should contain the stacktrace",
                                           "testGetStackTrace",
                                           result);
        StringAsserts.assertStringContains("Should contain the stacktrace",
                                           "ExceptionUtilsTester",
                                           result);
        StringAsserts.assertStringContains("Should contain the cause",
                                           "Caused by:",
                                           result);
    }
}