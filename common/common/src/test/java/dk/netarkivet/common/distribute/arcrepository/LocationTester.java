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

package dk.netarkivet.common.distribute.arcrepository;
/**
 * Tests of the Location class.
 */

import java.util.Collection;

import junit.framework.TestCase;

import dk.netarkivet.common.Settings;
import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.UnknownID;

public class LocationTester extends TestCase {
    private String[] knownTestNames;

    public LocationTester() {
        if (knownTestNames == null) {
        	knownTestNames = Settings.getAll(Settings.ENVIRONMENT_LOCATION_NAMES);
        }
    }

    // initializeKnownList is tested in the test of get
    // getName() is tested in the test of get
    public void testGet() {
    	assertTrue("Differences in sizes of location maps",
    		knownTestNames.length == Location.getKnown().size());
    	for (int i=0; i< knownTestNames.length; i++) {
            assertEquals(
                "Location name " + knownTestNames[i] + " not known in Location map.",
                Location.get(knownTestNames[i]).getName(), knownTestNames[i]
               );
    	}
        try {
        	Location.get("XYZXYZ");
            fail("Should have thrown exception on UnknownID exception");
        } catch (UnknownID e) {
            // expected
        }
    }

    public void testIsKnownLocation() {
    	for (int i=0; i< knownTestNames.length; i++) {
            assertTrue(
                "Location name " + knownTestNames[i] + " not known in Location map.",
                Location.isKnownLocation( knownTestNames[i] )
            );
    	}
    	if (Location.isKnownLocation("XYZXYZ"))
    	{ fail("Says it knows Location XYZXYZ"); }
    	try {
    		String s = "";
    		Location.isKnownLocation(s);
    	}
        catch (ArgumentNotValid e) {
            // expected
        }
    	try { Location.isKnownLocation(null); }
        catch (ArgumentNotValid e) {
            // expected
        }
    }

    public void  testGetKnown() {
    	// Collection<Location>
    	int i;
    	boolean found;
    	Collection<Location> col = Location.getKnown();

    	found = false;
    	assertTrue(
    	    "Differences in sizes of count of known names",
     		col.size() == knownTestNames.length
        );

    	for (i=0; i< knownTestNames.length; i++) {
    		for (Location l: col) {
        		if (knownTestNames[i].equals(l.getName())) {
        			found = true;
        		};
        		if (found) break;
        	};
            assertTrue(
                "Location name " + knownTestNames[i] + " was not found in known locations.",
                found
            );
    	}

    	found = false;
    	for (Location l: col) {
    	   	for (i=0; i< knownTestNames.length; i++) {
        		if (knownTestNames[i].equals(l.getName())) {
        			found = true;
        		};
        		if (found) break;
        	};
            assertTrue(
                "Location name " + knownTestNames[i] + " was not found in test locations.",
                found
            );
    	}
    }

    public void testGetKnownNames() {
    	boolean found;
    	int i;
    	int j;
    	String[] kn = Location.getKnownNames();

    	assertTrue(
    		"Differences in sizes of count of known names",
        	kn.length == knownTestNames.length
        );

    	found = false;
    	for (i=0; i< knownTestNames.length; i++) {
        	for (j=0; j< kn.length; j++) {
        		if (knownTestNames[i].equals(kn[j])) {
        			found = true;
        		};
        		if (found) break;
        	};
            assertTrue(
                    "Location name " + knownTestNames[i] + " was not found in known names for location.",
                    found
            );
    	};

    	found = false;
    	for (i=0; i< knownTestNames.length; i++) {
        	for (j=0; j< kn.length; j++) {
        		if (knownTestNames[i].equals(kn[j])) {
        			found = true;
        		};
        		if (found) break;
        	};
            assertTrue(
                "Location name " + knownTestNames[i] + " was not found in known names for test locations..",
                found
            );
    	};
    }

    public void TestGetChannelID() {
    	for (int i=0; i< knownTestNames.length; i++) {
            assertTrue(
                "Location ChanneId for name " + knownTestNames[i] + " is not the same in Location.",
                Channels.getBaMonForLocation(knownTestNames[i]).equals(Location.get(knownTestNames[i]).getChannelID())
               );
    	}
    }

    public void TestToString() {
    	for (int i=0; i< knownTestNames.length; i++) {
            assertTrue(
                "ToString for name " + knownTestNames[i] + " is not the same in Location.",
                "Location " + knownTestNames[i] == Location.get(knownTestNames[i]).toString()
               );
    	}
    }
}