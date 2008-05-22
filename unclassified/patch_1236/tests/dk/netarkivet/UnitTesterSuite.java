/* File:    $Id$
 * Version: $Revision$
 * Date:    $Date$
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

package dk.netarkivet;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

import dk.netarkivet.archive.arcrepository.ArcRepositoryTesterSuite;
import dk.netarkivet.archive.arcrepository.bitpreservation.BitPreservationTesterSuite;
import dk.netarkivet.archive.arcrepository.distribute.ArcrepositoryDistributeTesterSuite;
import dk.netarkivet.archive.arcrepositoryadmin.ArcRepositoryAdminTesterSuite;
import dk.netarkivet.archive.bitarchive.BitarchiveTesterSuite;
import dk.netarkivet.archive.bitarchive.distribute.BitarchiveDistributeTesterSuite;
import dk.netarkivet.archive.distribute.ArchiveDistributeTesterSuite;
import dk.netarkivet.archive.indexserver.IndexServerTesterSuite;
import dk.netarkivet.archive.indexserver.distribute.IndexserverDistributeTesterSuite;
import dk.netarkivet.archive.tools.ArchiveToolsTesterSuite;
import dk.netarkivet.common.CommonsTesterSuite;
import dk.netarkivet.common.distribute.DistributeTesterSuite;
import dk.netarkivet.common.distribute.arcrepository.DistributeArcrepositoryTesterSuite;
import dk.netarkivet.common.distribute.indexserver.DistributeIndexserverTesterSuite;
import dk.netarkivet.common.exceptions.ExceptionsTesterSuite;
import dk.netarkivet.common.jmx.JmxTesterSuite;
import dk.netarkivet.common.management.ManagementTesterSuite;
import dk.netarkivet.common.tools.ToolsTesterSuite;
import dk.netarkivet.common.utils.UtilsTesterSuite;
import dk.netarkivet.common.utils.arc.ArcUtilsTesterSuite;
import dk.netarkivet.common.utils.cdx.CdxUtilsTesterSuite;
import dk.netarkivet.deploy.DeployTesterSuite;
import dk.netarkivet.harvester.datamodel.DataModelTesterSuite;
import dk.netarkivet.harvester.distribute.HarvesterDistributeTesterSuite;
import dk.netarkivet.harvester.harvesting.HarvestingTesterSuite;
import dk.netarkivet.harvester.harvesting.distribute.HarvestingDistributeTesterSuite;
import dk.netarkivet.harvester.scheduler.SchedulerTesterSuite;
import dk.netarkivet.harvester.sidekick.SideKickTesterSuite;
import dk.netarkivet.harvester.tools.HarvesterToolsTesterSuite;
import dk.netarkivet.harvester.webinterface.WebinterfaceTesterSuite;
import dk.netarkivet.monitor.MonitorTesterSuite;
import dk.netarkivet.monitor.jmx.MonitorJMXTesterSuite;
import dk.netarkivet.monitor.logging.LoggingTesterSuite;
import dk.netarkivet.viewerproxy.ViewerProxyTesterSuite;
import dk.netarkivet.viewerproxy.distribute.ViewerproxyDistributeTesterSuite;

/**
 * This class runs all the unit tests.
 */
public class UnitTesterSuite {
    public static void addToSuite(TestSuite suite) {
         // Please keep sorted.
        ArchiveDistributeTesterSuite.addToSuite(suite);
        ArchiveToolsTesterSuite.addToSuite(suite);
        ArcRepositoryAdminTesterSuite.addToSuite(suite);
        ArcrepositoryDistributeTesterSuite.addToSuite(suite);
        ArcRepositoryTesterSuite.addToSuite(suite);
        ArcUtilsTesterSuite.addToSuite(suite);
        BitarchiveDistributeTesterSuite.addToSuite(suite);
        BitarchiveTesterSuite.addToSuite(suite);
        BitPreservationTesterSuite.addToSuite(suite);
        CdxUtilsTesterSuite.addToSuite(suite);
        CommonsTesterSuite.addToSuite(suite);
        DataModelTesterSuite.addToSuite(suite);
        DeployTesterSuite.addToSuite(suite);
        DistributeArcrepositoryTesterSuite.addToSuite(suite);
        DistributeIndexserverTesterSuite.addToSuite(suite);
        DistributeTesterSuite.addToSuite(suite);
        ExceptionsTesterSuite.addToSuite(suite);
        HarvesterDistributeTesterSuite.addToSuite(suite);
        HarvestingDistributeTesterSuite.addToSuite(suite);
        HarvestingTesterSuite.addToSuite(suite);
        IndexserverDistributeTesterSuite.addToSuite(suite);
        IndexServerTesterSuite.addToSuite(suite);
        JmxTesterSuite.addToSuite(suite);
        LoggingTesterSuite.addToSuite(suite);
        ManagementTesterSuite.addToSuite(suite);
        MonitorTesterSuite.addToSuite(suite);
        MonitorJMXTesterSuite.addToSuite(suite);
        SchedulerTesterSuite.addToSuite(suite);
        SideKickTesterSuite.addToSuite(suite);
        ToolsTesterSuite.addToSuite(suite);
        HarvesterToolsTesterSuite.addToSuite(suite);
        UtilsTesterSuite.addToSuite(suite);
        ViewerproxyDistributeTesterSuite.addToSuite(suite);
        ViewerProxyTesterSuite.addToSuite(suite);
        WebinterfaceTesterSuite.addToSuite(suite);
    }

    public static Test suite() {
        TestSuite suite;
        suite = new TestSuite(UnitTesterSuite.class.getName());

        addToSuite(suite);

        return suite;
    }

    public static void main(String[] args) {
        String[] args2 = {"-noloading", UnitTesterSuite.class.getName()};
        TestRunner.main(args2);
    }
}
