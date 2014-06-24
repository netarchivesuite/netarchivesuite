/*
 * #%L
 * Netarchivesuite - common - test
 * %%
 * Copyright (C) 2005 - 2014 The Royal Danish Library, the Danish State and University Library,
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

/**
 * Class testing the NullRemoteFile class.
 */

package dk.netarkivet.common.distribute;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import dk.netarkivet.common.exceptions.NotImplementedException;
import junit.framework.TestCase;

public class NullRemoteFileTester extends TestCase {

    public void testNewInstance() {
        RemoteFile nrf1 = NullRemoteFile.getInstance(null, false, false, false);
        assertTrue(nrf1 instanceof NullRemoteFile);
        assertEquals(nrf1.getSize(), 0);
        assertEquals(nrf1.getInputStream(), null);
        assertEquals(nrf1.getName(), null);
        try {
        	nrf1.getChecksum();
        	fail("Should have thrown NotImplementedException");
        } catch (NotImplementedException e){
        	// Expected
        }
        OutputStream os = new ByteArrayOutputStream();
        nrf1.appendTo(os);
        try {
            nrf1.appendTo(null);
        } catch (Exception e) {
            fail("Exception not expected with appendTo and null arg ");
        }
    }
}
