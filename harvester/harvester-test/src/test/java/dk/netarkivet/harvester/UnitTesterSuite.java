/*
 * #%L
 * Netarchivesuite - harvester - test
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

package dk.netarkivet.harvester;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;
import dk.netarkivet.common.webinterface.HarvesterCommonWebinterfaceTesterSuite;
import dk.netarkivet.harvester.datamodel.HarvesterDataModelTesterSuite;
import dk.netarkivet.harvester.datamodel.extendedfield.HarvesterDataModelExtendedfieldTesterSuite;
import dk.netarkivet.harvester.distribute.HarvesterDistributeTesterSuite;
import dk.netarkivet.harvester.harvesting.HarvestingTesterSuite;
import dk.netarkivet.harvester.harvesting.distribute.HarvestingDistributeTesterSuite;
import dk.netarkivet.harvester.harvesting.frontier.HarvesterHarvestingFrontierTesterSuite;
import dk.netarkivet.harvester.scheduler.HarvesterSchedulerTesterSuite;
import dk.netarkivet.harvester.webinterface.HarvesterWebinterfaceTesterSuite;

/**
 * This class runs all the harvester module unit tests.
 */
public class UnitTesterSuite {
    public static void addToSuite(TestSuite suite) {
        HarvesterTesterSuite.addToSuite(suite);
        HarvestingTesterSuite.addToSuite(suite);
        HarvesterDataModelTesterSuite.addToSuite(suite);
        HarvesterDataModelExtendedfieldTesterSuite.addToSuite(suite);
        HarvesterDistributeTesterSuite.addToSuite(suite);
        HarvesterHarvestingFrontierTesterSuite.addToSuite(suite);
        HarvestingDistributeTesterSuite.addToSuite(suite);
        HarvesterSchedulerTesterSuite.addToSuite(suite);
        //HarvesterToolsTesterSuite.addToSuite(suite);
        HarvesterWebinterfaceTesterSuite.addToSuite(suite);

        HarvesterCommonWebinterfaceTesterSuite.addToSuite(suite);
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
