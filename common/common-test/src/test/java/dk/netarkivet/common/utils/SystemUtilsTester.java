/*
 * #%L
 * Netarchivesuite - common - test
 * %%
 * Copyright (C) 2005 - 2018 The Royal Danish Library, 
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

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;

import dk.netarkivet.testutils.CollectionAsserts;

/**
 * Unit tests for the class SystemUtils.
 */
public class SystemUtilsTester {
    @Test
    public void testGetLocalIP() {
        String ip = SystemUtils.getLocalIP();
        String[] parts = ip.split("\\.");
        assertTrue("Expected at least four parts in the IP adress " + ip, parts.length >= 4);
    }

    /**
     * Tests getting hostname. This is nearly impossible, but we _can_ check that an IP address is not returned (note:
     * IP number comparison disabled as the method invoked uses getCanonicalHostname which does a reverse DNS lookup
     * which does not return a name but the IP-number if the number is not in DNS (which frequently is the case for
     * residential networks behind a NAT-router), and that it at least does not throw an exception.
     *
     * @throws Exception
     */
    @Test
    public void testGetLocalHostName() throws Exception {
        String result = SystemUtils.getLocalHostName();
        // assertFalse("Should not be an IPv4 address: " + result,
        // Constants.IP_KEY_REGEXP.matcher(result).matches());
        // assertFalse("Should not be an IPv6 address: " + result,
        // Constants.IPv6_KEY_REGEXP.matcher(result).matches());
        assertTrue("hostname not empty string", result.length() > 0);
    }

    @Test
    public void testGetCurrentClasspath() throws Exception {
        List<String> classpath = SystemUtils.getCurrentClasspath();
        String[] systemClassPath = System.getProperty("java.class.path").split(":");
        CollectionAsserts.assertListEquals("Should return the system" + " class path as a list", classpath,
                (Object[]) systemClassPath);
        // Test that some version of the standard libraries are in there
        JARS:
        for (String jar : new String[] {"commons-fileupload.*\\.jar$", "commons-httpclient.*\\.jar$",
                "dom4j-.*\\.jar$", "jaxen-.*\\.jar$", "jetty-.*\\.jar$", "junit-.*\\.jar$",
                // Removed as not used in common.
                // "libidn-.*\\.jar$",
                // "lucene-core-.*\\.jar$"
        }) {
            Matcher m = Pattern.compile(jar).matcher("");
            for (String path : classpath) {
                if (m.reset(path).find()) {
                    continue JARS;
                }
            }
            fail("Cannot find jar " + jar + " in classpath " + classpath);
        }
    }
}
