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
package dk.netarkivet.deploy;
/**
 * lc forgot to comment this!
 */

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;

import dk.netarkivet.archive.arcrepository.ArcRepositoryApplication;
import dk.netarkivet.archive.bitarchive.BitarchiveApplication;
import dk.netarkivet.archive.bitarchive.BitarchiveMonitorApplication;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.harvester.harvesting.HarvestControllerApplication;
import dk.netarkivet.harvester.harvesting.distribute.HarvestControllerServer;
import dk.netarkivet.harvester.sidekick.HarvestControllerServerMonitorHook;
import dk.netarkivet.harvester.sidekick.SideKick;
import dk.netarkivet.harvester.webinterface.HarvestDefinitionApplication;
import dk.netarkivet.testutils.FileAsserts;
import dk.netarkivet.testutils.ReflectUtils;
import dk.netarkivet.testutils.StringAsserts;
import dk.netarkivet.testutils.preconfigured.MoveTestFiles;
import dk.netarkivet.viewerproxy.ViewerProxyApplication;


public class HostTester extends TestCase {
    MoveTestFiles mtf = new MoveTestFiles(TestInfo.ORIGINALS_DIR,
            TestInfo.WORKING_DIR);

    public HostTester(String s) {
        super(s);
    }

    public void setUp() throws Exception {
        super.setUp();
        mtf.setUp();
    }

    public void tearDown() throws Exception {
        mtf.tearDown();
        super.tearDown();
    }

    public void testGetJarFiles() throws Exception {
        List<String> harvestJars = Arrays.asList(new String[] {
                "dk.netarkivet.harvester.jar",
                "dk.netarkivet.archive.jar",
                "dk.netarkivet.viewerproxy.jar",
                "dk.netarkivet.monitor.jar"
        });
        List<String> archiveJars = Arrays.asList(new String[] {
                "dk.netarkivet.archive.jar",
                "dk.netarkivet.viewerproxy.jar",
                "dk.netarkivet.monitor.jar"
        });
        List<String> accessJars = Arrays.asList(new String[] {
                "dk.netarkivet.viewerproxy.jar",
                "dk.netarkivet.archive.jar",
                "dk.netarkivet.monitor.jar"
        });
        List<String> monitorJars = Arrays.asList(new String[] {
                "dk.netarkivet.monitor.jar"
        });
        Host host = new Host("bar", "foo", Host.Type.bitarchive);
        assertEquals("Should get right jar for harvestdefinition",
                harvestJars,
                host.getJarFiles(HarvestDefinitionApplication.class.getName()));
        assertEquals("Should get right jar for harvestcontroller",
                harvestJars,
                host.getJarFiles(HarvestControllerApplication.class.getName()));
        assertEquals("Should get right jar for sidekick",
                harvestJars,
                host.getJarFiles(SideKick.class.getName()));
        assertEquals("Should get right jar for sidekick monitorhook",
                harvestJars,
                host.getJarFiles(HarvestControllerServerMonitorHook.class.getName()));
        assertEquals("Should get right jar for sidekick extended string",
                harvestJars,
                host.getJarFiles("dk.netarkivet.harvester.sidekick.SideKick "
                        + "dk.netarkivet.harvester.sidekick.HarvestControllerServerMonitorHook "
                        + " ./conf/someharvester "));

        assertEquals("Should get right jar for arcrepository",
                archiveJars,
                host.getJarFiles(ArcRepositoryApplication.class.getName()));
        assertEquals("Should get right jar for bamon",
                archiveJars,
                host.getJarFiles(BitarchiveMonitorApplication.class.getName()));
        assertEquals("Should get right jar for bitarchive",
                archiveJars,
                host.getJarFiles(BitarchiveApplication.class.getName()));

        assertEquals("Should get right jar for viewerproxy",
                accessJars,
                host.getJarFiles(ViewerProxyApplication.class.getName()));

        try {
            host.getJarFiles(HarvestControllerServer.class.getName());
            fail("Should fail on non-application class");
        } catch (ArgumentNotValid e) {
            StringAsserts.assertStringContains("Should mention bad class",
                    "HarvestControllerServer", e.getMessage());
        }

        try {
            host.getJarFiles("dk.netarkivet.harvester.NotAClass");
            fail("Should fail on non-existing class");
        } catch (ArgumentNotValid e) {
            StringAsserts.assertStringContains("Should mention bad class",
                    "NotAClass", e.getMessage());
        }

        try {
            host.getJarFiles(null);
            fail("Should fail on null class");
        } catch (ArgumentNotValid e) {
            // Expected
        }
    }

    public void testGetJMXPortParameter() throws Exception {
        ItConfiguration itConfig
                = new ItConfiguration(TestInfo.IT_CONF_FILE);
        String environmentName = "prod";
        itConfig.setEnvironment(environmentName);
        itConfig.calculateDefaultSettings(TestInfo.SETTINGS_FILE
                .getParentFile());
        itConfig.loadDefaultSettings(TestInfo.SETTINGS_FILE, environmentName);
        Field hostlist = ReflectUtils.getPrivateField(ItConfiguration.class,
                "hostlist");
        List<Host> hosts = (List<Host>)hostlist.get(itConfig);
        for (Host h : hosts) {
            if (h.getLocation().equals("kb")) {
                if (h.isType(Host.Type.admin)) {
                    checkJMXPorts(h, 4);
                } else if (h.isType(Host.Type.bitarchive)) {
                    checkJMXPorts(h, 1);
                } else if (h.isType(Host.Type.harvesters)) {
                    if (h.getName().equals("kb-dev-har-001.kb.dk")) {
                        // This one has two harvesters running.
                        checkJMXPorts(h, 4);
                    } else {
                        checkJMXPorts(h, 2);
                    }
                } else if (h.isType(Host.Type.ftp)) {
                    checkJMXPorts(h, 0);
                } else if (h.isType(Host.Type.access)) {
                    checkJMXPorts(h, 2);
                }
            } else {
                if (h.isType(Host.Type.access)) {
                    checkJMXPorts(h, 1);
                } else if (h.isType(Host.Type.bitarchive)) {
                    checkJMXPorts(h, 1);
                } else if (h.isType(Host.Type.harvesters)) {
                    checkJMXPorts(h, 2);
                }
            }
        }
    }

    /** Check that the expected number of ports is available */
    private void checkJMXPorts(Host h, int numPorts) throws
            NoSuchMethodException, IllegalAccessException,
            InvocationTargetException {
        Method getJMXPortParameter = ReflectUtils.getPrivateMethod(Host.class,
                "getJMXPortParameter");
        for (int i = 8100; i < 8100 + numPorts; i++) {
            String result = (String)getJMXPortParameter.invoke(h);
            assertEquals("Must have port " + i,
                    "-Dsettings.common.jmx.port=" + i + " -Dsettings.common.jmx.rmiPort=" + (i + 100), result);
        }
        try {
            String result = (String)getJMXPortParameter.invoke(h);
            fail("No ports should be left behind on " + h
                    + " after " + numPorts + " are taken: " + result);
        } catch (InvocationTargetException e) {
            StringAsserts.assertStringContains("Should mention host",
                    h.toString(), e.getCause().getMessage());
        }
    }

    public void testWriteJMXPassword() throws Exception {
        ItConfiguration itConfig
                = new ItConfiguration(TestInfo.IT_CONF_FILE);
        String environmentName = "prod";
        itConfig.setEnvironment(environmentName);
        itConfig.calculateDefaultSettings(TestInfo.SETTINGS_FILE.getParentFile());
        Field pwdField = ReflectUtils.getPrivateField(ItConfiguration.class,
                "jmxMonitorRolePassword");
        pwdField.set(itConfig, "TestPassword");
        itConfig.loadDefaultSettings(TestInfo.SETTINGS_FILE, environmentName);
        Field hostlist = ReflectUtils.getPrivateField(ItConfiguration.class,
                "hostlist");
        List<Host> hosts = (List<Host>)hostlist.get(itConfig);
        for (Host h : hosts) {
            File confDir = new File(new File(TestInfo.TMPDIR, h.getName()),
                    "conf");
            confDir.mkdirs(); // This is normally created outside writeJMXPwd
            h.writeJMXPassword(confDir);
            FileAsserts.assertFileContains("Should have pwd from it-conf",
                    "monitorRole TestPassword",
                    new File(confDir, "jmxremote.password"));
        }
    }
}