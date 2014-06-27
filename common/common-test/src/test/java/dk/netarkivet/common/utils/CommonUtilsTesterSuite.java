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

package dk.netarkivet.common.utils;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

/**
 * Sweet suite of utility tests.
 *
 */
public class CommonUtilsTesterSuite {
    public static Test suite() {
        TestSuite suite;
        suite = new TestSuite(CommonUtilsTesterSuite.class.getName());
        addToSuite(suite);
        return suite;
    }

    public static void addToSuite(TestSuite suite) {
        // Moved to "monitor"
        // suite.addTestSuite(ApplicationUtilsTester.class);

        suite.addTestSuite(DiscardingOutputStreamTester.class);
        suite.addTestSuite(ExceptionUtilsTester.class);
        suite.addTestSuite(FileArrayIteratorTester.class);
        suite.addTestSuite(FileUtilsTester.class);
        suite.addTestSuite(FilterIteratorTester.class);
        suite.addTestSuite(JMXUtilsTester.class);
        suite.addTestSuite(KeyValuePairTester.class);
        suite.addTestSuite(LargeFileGZIPInputStreamTester.class);
        // Disabled, as it is platform specific
        // suite.addTestSuite(ProcessUtilsTester.class);
        suite.addTestSuite(ReadOnlyByteArrayTester.class);
        suite.addTestSuite(SettingsTester.class);
        suite.addTestSuite(SettingsFactoryTester.class);
        suite.addTestSuite(SimpleXmlTester.class);
        suite.addTestSuite(SparseBitSetTester.class);
        suite.addTestSuite(StreamUtilsTester.class);
        suite.addTestSuite(StringUtilsTester.class);
        suite.addTestSuite(SystemUtilsTester.class);
        suite.addTestSuite(TablesortTester.class);
        suite.addTestSuite(TimeUtilsTester.class);
        suite.addTestSuite(XmlTreeTester.class);
        suite.addTestSuite(XmlUtilsTester.class);
        suite.addTestSuite(ZipUtilsTester.class);
    }

    public static void main(String[] args) {
        String[] args2 = {"-noloading", CommonUtilsTesterSuite.class.getName()};

        TestRunner.main(args2);
    }
}
