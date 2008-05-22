/* File:    $Id$
 * Revision:$Revision$
 * Author:  $Author$
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
package dk.netarkivet.common.distribute;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import junit.framework.TestCase;

import dk.netarkivet.common.Settings;
import dk.netarkivet.common.exceptions.PermissionDenied;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.harvester.datamodel.JobPriority;
import dk.netarkivet.testutils.StringAsserts;

/**
 * Tests the ChannelID class that defines instances of message channels
 * (ie. queues and topics).
 */
public class ChannelIDTester extends TestCase {

    /**
     * Verify that the old ChannelID.getInstance(String s) method has been
     * removed.
     */
    public void testOldFactoryMethodGone() {
        try{
            Channels.getAllBa().getClass().getMethod("getInstance", new Class[] {"".getClass()} );
            fail("Have not deleted deprecated factory method getInstance()" );
        } catch (NoSuchMethodException e) {
            // expected
        }

    }

    /**
     * Verify that a queue instance is serializable.
     */
    public void testSerializability() throws IOException, ClassNotFoundException {
        //Take two queues: one for study and one for reference.
        ChannelID q1 = Channels.getAnyBa();
        ChannelID q2 = Channels.getAnyBa();
        //Now serialize and deserialize the studied queue (but NOT the reference):
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream ous = new ObjectOutputStream(baos);
        ous.writeObject(q1);
        ous.close();
        baos.close();
        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()));
        q1 = (ChannelID) ois.readObject();
        //Finally, compare their visible states:
        assertEquals("The two channels should have the same name", q1.getName(), q2.getName());
        assertEquals("The two channels should be of same type (queue or topic)", q1.isTopic(), q2.isTopic());
    }

    /**
     * Test that each channel is equal only to itself
     */
    public void testChannelIdentity(){
        String priority1 = Settings.get(Settings.HARVEST_CONTROLLER_PRIORITY);
        JobPriority p = JobPriority.valueOf(priority1);
        ChannelID result1;
        switch(p) {
            case LOWPRIORITY:
                result1 = Channels.getAnyLowpriorityHaco();
                break;
            case HIGHPRIORITY:
                result1 = Channels.getAnyHighpriorityHaco();
                break;
            default:
                throw new UnknownID(priority1 + " is not a valid priority");
        }
        ChannelID[] l1 =
         {Channels.getAllBa(), result1, Channels.getAnyBa(),
          Channels.getError(), Channels.getTheArcrepos(), Channels.getTheBamon(),
          Channels.getTheSched(), Channels.getThisHaco()};
        String priority = Settings.get(Settings.HARVEST_CONTROLLER_PRIORITY);
        ChannelID result;
        JobPriority p1 = JobPriority.valueOf(priority1);
        switch(p1) {
            case LOWPRIORITY:
                result = Channels.getAnyLowpriorityHaco();
                break;
            case HIGHPRIORITY:
                result = Channels.getAnyHighpriorityHaco();
                break;
            default:
                throw new UnknownID(priority + " is not a valid priority");
        }
        ChannelID[] l2 =
                {Channels.getAllBa(), result, Channels.getAnyBa(),
                 Channels.getError(), Channels.getTheArcrepos(), Channels.getTheBamon(),
                 Channels.getTheSched(), Channels.getThisHaco()};

        for (int i = 0; i<l1.length; i++){
            for (int j = 0; j<l2.length; j++){
                if (i == j) {
                    assertEquals("Two different instances of same queue "
                            +l1[i].getName(), l1[i],
                            l2[j]);
                    assertEquals("Two instances of same channel have different " +
                            "names: "
                            + l1[i].getName() + " and " +
                            l2[j].getName(), l1[i].getName(),
                            l2[j].getName() ) ;
                }
                else{
                    assertNotSame("Two different queues are the same object "
                            +l1[i].getName() + " "
                            + l2[j].getName(), l1[i],
                            l2[j]);
                    assertNotSame("Two different channels have same name",
                            l1[i].getName(), l2[j].getName());
                }
            }
        }
    }


    /**
     * Verify that a topic instance is Serializable.
     */
    public void testTopicSerializability() throws IOException, ClassNotFoundException {
        //Take two queues: one for study and one for reference.
        ChannelID q1 = Channels.getAllBa();
        ChannelID q2 = Channels.getAllBa();
        //Now serialize and deserialize the studied queue (but NOT the reference):
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream ous = new ObjectOutputStream(baos);
        ous.writeObject(q1);
        ous.close();
        baos.close();
        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()));
        q1 = (ChannelID) ois.readObject();
        //Finally, compare their visible states:
        assertEquals("The two channels should have the same name", q1.getName(), q2.getName());
        assertEquals("The two channels should be of same type (queue or topic)", q1.isTopic(), q2.isTopic());
    }

    /**
     * Test that AllArchives_ALL_BAs returns as array of ALL_BA channels which are all
     * distinct and all contain ALL_BA in name.
     */
    public void testALL_ALL_BAs() {
      ChannelID[] ALL_BAs = Channels.getAllArchives_ALL_BAs();
      for (int i = 0; i < ALL_BAs.length; i++) {
          StringAsserts.assertStringContains(
                  "ChannelID.getAllArchives_ALL_BAs() returned a channel"
                          + " without ALL_BA in its name",
                  "ALL_BA", ALL_BAs[i].getName());
          for (int j = 0; j < ALL_BAs.length; j++) {
              if (i != j) assertNotSame("Two ALL_BAs have the same name " +
                      ALL_BAs[i].getName(), ALL_BAs[i].getName(),
                      ALL_BAs[j].getName());
          }
      }
    }

     /**
     * Test that AllArchives_ANY_BAs returns as array of ANY_BA channels which are all
     * distinct and all contain ANY_BA in name
     */
    public void testALL_ANY_BAs() {
      ChannelID[] ANY_BAs = Channels.getAllArchives_ANY_BAs();
      for (int i = 0; i < ANY_BAs.length; i++) {
          StringAsserts.assertStringContains(
                  "ChannelID.getAllArchives_ANY_BAs() returned a channel"
                          + " without ANY_BA in its name",
                  "ANY_BA", ANY_BAs[i].getName());
          for (int j = 0; j < ANY_BAs.length; j++) {
              if (i != j) assertNotSame("Two ANY_BAs have the same name " +
                      ANY_BAs[i].getName(), ANY_BAs[i].getName(),
                      ANY_BAs[j].getName());
          }
      }
    }

     /**
     * Test that AllArchives_BAMONs returns as array of BAMON channels which are all
     * distinct and all contain BAMON in name.
     */
    public void testAllArchives_BAMONs() {
      ChannelID[] BAMONs = Channels.getAllArchives_BAMONs();
      for (int i = 0; i < BAMONs.length; i++) {
          assertTrue("ChannelID.getAllArchives_BAMONs() returned a channel without BAMON " +
                  "in its name: " + BAMONs[i].getName(),
          BAMONs[i].getName().indexOf("BAMON") != -1 );
          for (int j = 0; j < BAMONs.length; j++) {
              if (i != j) assertNotSame("Two BAMONs have the same name " +
                      BAMONs[i].getName(), BAMONs[i].getName(),
                      BAMONs[j].getName());
          }
      }
    }

    /**
     * Tests that only the ArcRepository may request channels for
     * all bit archives. At the time of writing, there is no ArcRepository
     * application, so this funtionality cannot be implemented (or tested).
     */
    public void excludedtesttestALL_ALL_BAsAccess() {
        try {
            Channels.getAllArchives_ALL_BAs();
            fail("ChannelID.getAllArchives_ALL_BAs() failed to throw exception when " +
                    "application was not arcrepository");
        } catch (PermissionDenied e) {
            // expected
        }

    }



}
