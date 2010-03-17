/*$Id$
* $Revision$
* $Date$
* $Author$
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
package dk.netarkivet.harvester.datamodel;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

/**
 * Unit tests for the DomainHistory class.
 */
public class DomainHistoryTester extends TestCase {
    public DomainHistoryTester(String s) {
        super(s);
    }

    public void setUp() {
    }

    public void tearDown() {
    }
    
    /**
     * Tests the getmostRecentHarvestInfo() method.
     */
    public void testGetMostRecentHarvestInfo() {
        DomainHistory h = new DomainHistory();
        assertNull("Should be no most recent harvestInfo at construction time",
                h.getMostRecentHarvestInfo("bar"));
        h = setupHarvestInfos();
        assertEquals("Most recent harvest info for bar must be #2",
                new Long(2), h.getMostRecentHarvestInfo("bar").getHarvestID());
        HarvestInfo hi = h.getMostRecentHarvestInfo("baz");
        assertNull("Must not get non-existing harvest info", hi);
    }
    /**
     * Tests the getHarvestInfo() method.
     */
    public void testGetHarvestInfo() {
        DomainHistory h = setupHarvestInfos();
        List<HarvestInfo> readhislist = new ArrayList<HarvestInfo>();

        for(Iterator<HarvestInfo> i = h.getHarvestInfo(); i.hasNext(); ) {
            readhislist.add(i.next());
        }

        HarvestInfo[] his
                = (HarvestInfo[]) readhislist.toArray(new HarvestInfo[0]);
        assertEquals("Must have 4 harvest infos after adding them", 4, his.length);
        assertEquals("Must be in order by time", 4L, his[0].getDate().getTime());
        assertEquals("Must be in order by time", 3L, his[1].getDate().getTime());
        assertEquals("Must be in order by time", 2L, his[2].getDate().getTime());
        assertEquals("Must be in order by time", 1L, his[3].getDate().getTime());
    }

    /** Tests that two harvests on the same date of the same domain but
     * different configurations and/or harvest definitions can be recorded
     * in harvest info.
     */
    public void testMultipleHarvestInfoOnSameDate() throws Exception {
        DomainHistory h = new DomainHistory();
        h.addHarvestInfo(new HarvestInfo(new Long(1L), "foo", "bar", new Date(1L), 1L, 1L, StopReason.DOWNLOAD_COMPLETE));
        h.addHarvestInfo(new HarvestInfo(new Long(2L), "foo", "bar", new Date(1L), 1L, 1L, StopReason.DOWNLOAD_COMPLETE));
        h.addHarvestInfo(new HarvestInfo(new Long(2L), "foo", "foo", new Date(1L), 1L, 1L, StopReason.DOWNLOAD_COMPLETE));
        h.addHarvestInfo(new HarvestInfo(new Long(3L), "foo", "baz", new Date(1L), 1L, 1L, StopReason.DOWNLOAD_COMPLETE));
        List<HarvestInfo> readhislist = new ArrayList<HarvestInfo>();

        for(Iterator<HarvestInfo> i = h.getHarvestInfo(); i.hasNext(); ) {
            readhislist.add(i.next());
        }

        HarvestInfo[] his
                = (HarvestInfo[]) readhislist.toArray(new HarvestInfo[0]);
        assertEquals("Must have 4 harvest infos after adding them", 4, his.length);
        Map<Long,Integer> hdOids = new HashMap<Long,Integer>();
        Map<String, Integer> configNames = new HashMap<String, Integer>();
        for (int i=0; i<his.length; i++) {
            Integer oidcount = (Integer) hdOids.get(his[i].getHarvestID());
            if (oidcount == null) {
                oidcount = new Integer(1);
            } else {
                oidcount = new Integer(oidcount.intValue() + 1);
            }
            hdOids.put(his[i].getHarvestID(), oidcount);
            Integer configcount = (Integer) configNames.get(his[i].getDomainConfigurationName());
            if (configcount == null) {
                configcount = new Integer(1);
            } else {
                configcount = new Integer(configcount.intValue() + 1);
            }
            configNames.put(his[i].getDomainConfigurationName(), configcount);
        }
        assertEquals("Three harvestdefs", 3, hdOids.keySet().size());
        assertEquals("One with hd1", new Integer(1), hdOids.get(new Long(1L)));
        assertEquals("Two with hd2", new Integer(2), hdOids.get(new Long(2L)));
        assertEquals("One with hd3", new Integer(1), hdOids.get(new Long(3L)));
        assertEquals("Three configurations", 3, configNames.keySet().size());
        assertEquals("Two with bar", new Integer(2), configNames.get("bar"));
        assertEquals("One with foo", new Integer(1), configNames.get("foo"));
        assertEquals("One with baz", new Integer(1), configNames.get("baz"));
    }


    private DomainHistory setupHarvestInfos() {
        DomainHistory h = new DomainHistory();
        h.addHarvestInfo(new HarvestInfo(new Long(1), "foo", "bar", new Date(1L), 1L, 1L, StopReason.DOWNLOAD_COMPLETE));
        h.addHarvestInfo(new HarvestInfo(new Long(2), "foo", "bar", new Date(3L), 1L, 1L, StopReason.DOWNLOAD_COMPLETE));
        h.addHarvestInfo(new HarvestInfo(new Long(3), "foo", "bar", new Date(2L), 1L, 1L, StopReason.DOWNLOAD_COMPLETE));
        h.addHarvestInfo(new HarvestInfo(new Long(4), "foo", "foo", new Date(4L), 1L, 1L, StopReason.DOWNLOAD_COMPLETE));
        return h;
    }

}