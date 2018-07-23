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
package dk.netarkivet.common.exceptions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit-tests for all Exceptions in this package.
 */
public class ExceptionsTester {

    private String nullString;
    private String emptyString;
    private String meaningfullString;

    @Before
    public void setUp() {
        nullString = null;
        emptyString = "";
        meaningfullString = "This is a meaningful exception";
    }

    /**
     * Test PermissionDenied exception.
     */
    @Test
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
     */
    @Test
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
     */
    @Test
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
     */
    @Test
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
     */
    @Test
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
    @Test
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

    /**
     * test {@link BatchTermination} constructors. Note that null arguments are currently allowed!
     */
    @Test
    public void testBatchTermination() {
        final String errMsg = "Batch terminated";
        try {
            new BatchTermination(nullString);
        } catch (Exception e) {
            fail("Null message should not throw an exception");
        }

        try {
            new BatchTermination(emptyString);
        } catch (Exception e) {
            fail("Empty message should not throw an exception");
        }
        BatchTermination bt = new BatchTermination(errMsg);
        assertEquals(errMsg, bt.getMessage());

        bt = new BatchTermination(nullString, null);
        assertEquals(null, bt.getMessage());
        assertEquals(null, bt.getCause());

        IOFailure iof = new IOFailure("Some IO error occurred");

        bt = new BatchTermination(errMsg, iof);
        assertEquals(errMsg, bt.getMessage());
        assertEquals(iof, bt.getCause());
    }

    /**
     * test {@link HarvestingAbort} constructors. Note that null arguments are currently allowed!
     */
    @Test
    public void testHarvestingAbort() {
        final String errMsg = "Harvest aborted";
        try {
            new HarvestingAbort(nullString);
        } catch (Exception e) {
            fail("Null message should not throw an exception");
        }

        try {
            new HarvestingAbort(emptyString);
        } catch (Exception e) {
            fail("Empty message should not throw an exception");
        }
        HarvestingAbort bt = new HarvestingAbort(errMsg);
        assertEquals(errMsg, bt.getMessage());

        bt = new HarvestingAbort(nullString, null);
        assertEquals(null, bt.getMessage());
        assertEquals(null, bt.getCause());

        IOFailure iof = new IOFailure("Some IO error occurred");

        bt = new HarvestingAbort(errMsg, iof);
        assertEquals(errMsg, bt.getMessage());
        assertEquals(iof, bt.getCause());
    }
}
