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
package dk.netarkivet.common.tools;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

public class CommonToolsTesterSuite {
    public static Test suite() {
        TestSuite suite;
        suite = new TestSuite(CommonToolsTesterSuite.class.getName());
        addToSuite(suite);
        return suite;
    }

    public static void addToSuite(TestSuite suite) {
        suite.addTestSuite(ArcMergeTester.class);
        suite.addTestSuite(ArcWrapTester.class);
        suite.addTestSuite(ExtractCDXTester.class);
        suite.addTestSuite(ToolRunnerTester.class);
        suite.addTestSuite(ChecksumCalculatorTester.class);
    }

    public static void main(String args[]) {
        String args2[] = {"-noloading", CommonToolsTesterSuite.class.getName()};

        TestRunner.main(args2);
    }

}
