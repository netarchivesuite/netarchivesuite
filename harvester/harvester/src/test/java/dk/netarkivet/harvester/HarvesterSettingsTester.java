/* File:    $Id$
* Revision: $Revision$
* Author:   $Author$
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
package dk.netarkivet.harvester;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import junit.framework.TestCase;

/** Unittestersuite for the HarvesterSettings class. */
public class HarvesterSettingsTester extends TestCase {

    public void testNoFinalSettingsConstants() {
        Class c = HarvesterSettings.class;
        Field[] fields = c.getDeclaredFields();
        for (Field f: fields) {
            // Check that all static public fields are not final
            int modifiers = f.getModifiers();
            if (Modifier.isPublic(modifiers) && Modifier.isStatic(modifiers)) {
                assertFalse("public static fields must not be final, " 
                        + "but this was violated by field " + f.getName(),            
                         Modifier.isFinal(modifiers));
            }
        }
    }
    
    /** 
     * If this test fails, we need to update the SingleMBeanObjectTester#Setup 
     * and ChannelIDTester.
     */
    public void testHarvestControllerPrioritySettingUnchanged() {
       assertEquals("The 'HarvesterSettings.HARVEST_CONTROLLER_PRIORITY' "
               + "setting has changed. Please update " 
               + "SingleMBeanObjectTester#Setup method",
               HarvesterSettings.HARVEST_CONTROLLER_PRIORITY, 
               "settings.harvester.harvesting.queuePriority"); 
    }
    
}
