/*$Id$
* $Revision$
* $Author$
* $Date$
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
package dk.netarkivet.common.exceptions;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

/**
 * Unit-tests for all Exceptions in this package.
 */
public class ExceptionsTester extends TestCase {

    private String nullString;
    private String emptyString;
    private String meaningfullString;

    public ExceptionsTester(String s) {
        super(s);
    }

    public void setUp() {
        nullString = null;
        emptyString = "";
        meaningfullString = "This is a meaningfull exception";
    }

    public void tearDown() {
    }

    /**
     * Test PermissionDenied exception.
     */
    public void testPermissionDeniedException() {
        // Test PermissionDenied(String) constructor
        try {
            throw new PermissionDenied(nullString);
        } catch (PermissionDenied e) {
            assertEquals(null, e.getMessage());
        }

        try {
            throw new PermissionDenied(emptyString);
        } catch (PermissionDenied e) {
            assertEquals("", e.getMessage());
        }

        try {
            throw new PermissionDenied(meaningfullString);
        } catch (PermissionDenied e) {
            assertEquals(meaningfullString, e.getMessage());
        }

        // Test PermissionDenied(String, Throwable) constructor
        try {
            try {
                throw new Exception(meaningfullString);
            } catch (Exception e) {
                throw new PermissionDenied("rethrown exception", e);
            }
        } catch (PermissionDenied exception) {
            assertEquals("rethrown exception", exception.getMessage());
        }
    }

    /**
     * Test UnknownID exception.
     **/
    public void testUnknownIDException() {
        // Test UnknownID(String) constructor
        try {
            throw new UnknownID(nullString);
        } catch (UnknownID e) {
            assertEquals(null, e.getMessage());
        }

        try {
            throw new UnknownID(emptyString);
        } catch (UnknownID e) {
            assertEquals("", e.getMessage());
        }

        try {
            throw new UnknownID(meaningfullString);
        } catch (UnknownID e) {
            assertEquals(meaningfullString, e.getMessage());
        }

        // Test UnknownID(String, Throwable) constructor
        try {
            try {
                throw new Exception(meaningfullString);
            } catch (Exception e) {
                throw new UnknownID("rethrown exception", e);
            }
        } catch (UnknownID exception) {
            assertEquals("rethrown exception", exception.getMessage());
        }
    }

    /**
     * Test NotImplementedException exception.
     **/
    public void testNotImplementedException() {
        // Test NotImplementedException(String) constructor
        try {
            throw new NotImplementedException(nullString);
        } catch (NotImplementedException e) {
            assertEquals(null, e.getMessage());
        }

        try {
            throw new NotImplementedException(emptyString);
        } catch (NotImplementedException e) {
            assertEquals("", e.getMessage());
        }

        try {
            throw new NotImplementedException(meaningfullString);
        } catch (NotImplementedException e) {
            assertEquals(meaningfullString, e.getMessage());
        }

        // Test NotImplementedException(String, Throwable) constructor
        try {
            try {
                throw new Exception(meaningfullString);
            } catch (Exception e) {
                throw new NotImplementedException("rethrown exception", e);
            }
        } catch (NotImplementedException exception) {
            assertEquals("rethrown exception", exception.getMessage());
        }
    }

    /**
     * Test IOFailure exception.
     **/
    public void testIOFailureException() {
        // Test IOFailure(String) constructor
        try {
            throw new IOFailure(nullString);
        } catch (IOFailure e) {
            assertEquals(null, e.getMessage());
        }

        try {
            throw new IOFailure(emptyString);
        } catch (IOFailure e) {
            assertEquals("", e.getMessage());
        }

        try {
            throw new IOFailure(meaningfullString);
        } catch (IOFailure e) {
            assertEquals(meaningfullString, e.getMessage());
        }

        // Test IOFailure(String, Throwable) constructor
        try {
            try {
                throw new Exception(meaningfullString);
            } catch (Exception e) {
                throw new IOFailure("rethrown exception", e);
            }
        } catch (IOFailure exception) {
            assertEquals("rethrown exception", exception.getMessage());
        }
    }

    /**
     * Test ArgumentNotValid exception.
     **/
    public void testArgumentNotValidException() {
        // Test ArgumentNotValid(String) constructor
        try {
            throw new ArgumentNotValid(nullString);
        } catch (ArgumentNotValid e) {
            assertEquals(null, e.getMessage());
        }

        try {
            throw new ArgumentNotValid(emptyString);
        } catch (ArgumentNotValid e) {
            assertEquals("", e.getMessage());
        }

        try {
            throw new ArgumentNotValid(meaningfullString);
        } catch (ArgumentNotValid e) {
            assertEquals(meaningfullString, e.getMessage());
        }

        // Test ArgumentNotValid(String, Throwable) constructor
        try {
            try {
                throw new Exception(meaningfullString);
            } catch (Exception e) {
                throw new ArgumentNotValid("rethrown exception", e);
            }
        } catch (ArgumentNotValid exception) {
            assertEquals("rethrown exception", exception.getMessage());
        }

        // Check ArgumentNotValid.checkNotNullOrEmpty()
        try {
            List<?> lst = new ArrayList<String>();
            ArgumentNotValid.checkNotNullOrEmpty(lst, "lst");
        } catch (ArgumentNotValid e) {
            // Expected for the empty list
        }

        List<String> lst = new ArrayList<String>();
        lst.add(meaningfullString);
        ArgumentNotValid.checkNotNullOrEmpty(lst, "lst");

        try {
            List<String> aList = null;
            ArgumentNotValid.checkNotNullOrEmpty(aList, "Alist");
        } catch (ArgumentNotValid e) {
            // Expected for the null list
        }

        // check method checkTrue(boolean b, String s)
        try {
            ArgumentNotValid.checkTrue(true, emptyString);
            ArgumentNotValid.checkTrue(false, emptyString);
            fail("This should not be executed");
        } catch (ArgumentNotValid e) {
            // Expected for the false argument
        }

        // check method checkPositive(long num, String name)
        try {
            ArgumentNotValid.checkPositive(1, emptyString);
            ArgumentNotValid.checkPositive(0, emptyString);
            fail("This should not be executed");
        } catch (ArgumentNotValid e) {
            // Expected for the non-positive argument
        }

    }

    /**
     * Test IllegalState(String, Throwable) constructor.
     */
    public void testIllegalState() {

        try {
            try {
                throw new Exception(meaningfullString);
            } catch (Exception e) {
                throw new IllegalState("rethrown exception", e);
            }
        } catch (IllegalState exception) {
            assertEquals("rethrown exception", exception.getMessage());
        }
    }


}
