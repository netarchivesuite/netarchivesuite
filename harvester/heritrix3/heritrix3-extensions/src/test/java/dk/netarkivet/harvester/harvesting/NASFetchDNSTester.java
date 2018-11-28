/*
 * #%L
 * Netarchivesuite - Heritrix 3 extensions
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
package dk.netarkivet.harvester.harvesting;

import java.io.Reader;
import java.io.StringReader;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.archive.io.ReadSource;
import org.junit.Assert;
import org.junit.Test;

public class NASFetchDNSTester {

    @Test
    public void test_configuration() {
        NASFetchDNS fetcher = new NASFetchDNS();
        Assert.assertEquals(true, fetcher.acceptDefinedHosts); 
        Assert.assertEquals(null, fetcher.hostsFile);
        Assert.assertEquals(null, fetcher.hostsSource);
        Assert.assertEquals(fetcher.acceptDefinedHosts, fetcher.getAcceptDefinedHosts()); 
        Assert.assertEquals(fetcher.hostsFile, fetcher.getHostsFile());
        Assert.assertEquals(fetcher.hostsSource, fetcher.getHostsSource());

        fetcher.setAcceptDefinedHosts(false);
        ReadSource hostsFile = new ReadSource() {
            @Override
            public Reader obtainReader() {
                StringReader sr = new StringReader("#comment\n"
                        + "1.2.3.4   netarivket.dk   netarkivet   # testing\n");
                return sr;
            }
        };
        fetcher.setHostsFile(hostsFile); 
        ReadSource hostsSource = new ReadSource() {
            @Override
            public Reader obtainReader() {
                StringReader sr = new StringReader("#comment\n"
                        + "4.3.2.1   kb.dk   kb   # comment\n");
                return sr;
            }
        };
        fetcher.setHostsSource(hostsSource); 
        Assert.assertEquals(false, fetcher.acceptDefinedHosts); 
        Assert.assertEquals(hostsFile, fetcher.hostsFile);
        Assert.assertEquals(hostsSource, fetcher.hostsSource);
        Assert.assertEquals(fetcher.acceptDefinedHosts, fetcher.getAcceptDefinedHosts()); 
        Assert.assertEquals(fetcher.hostsFile, fetcher.getHostsFile());
        Assert.assertEquals(fetcher.hostsSource, fetcher.getHostsSource());

        fetcher.setAcceptDefinedHosts(true);
        fetcher.setHostsFile(null); 
        fetcher.setHostsSource(null); 
        Assert.assertEquals(true, fetcher.acceptDefinedHosts); 
        Assert.assertEquals(null, fetcher.hostsFile);
        Assert.assertEquals(null, fetcher.hostsSource);
        Assert.assertEquals(fetcher.acceptDefinedHosts, fetcher.getAcceptDefinedHosts()); 
        Assert.assertEquals(fetcher.hostsFile, fetcher.getHostsFile());
        Assert.assertEquals(fetcher.hostsSource, fetcher.getHostsSource());
    }

    @Test
    public void test_tokenizer() {
        String[] tokensArr = new String[3];
        int tokens;

        tokens = NASFetchDNS.tokenize("", tokensArr);
        Assert.assertEquals(0, tokens);
        tokens = NASFetchDNS.tokenize("    ", tokensArr);
        Assert.assertEquals(0, tokens);
        tokens = NASFetchDNS.tokenize("\t\t\t\t", tokensArr);
        Assert.assertEquals(0, tokens);

        tokens = NASFetchDNS.tokenize("one", tokensArr);
        Assert.assertEquals(1, tokens);
        Assert.assertEquals("one", tokensArr[0]);
        tokens = NASFetchDNS.tokenize("    one    ", tokensArr);
        Assert.assertEquals(1, tokens);
        Assert.assertEquals("one", tokensArr[0]);
        tokens = NASFetchDNS.tokenize("\t\t\t\tone\t\t\t\t", tokensArr);
        Assert.assertEquals(1, tokens);
        Assert.assertEquals("one", tokensArr[0]);

        tokens = NASFetchDNS.tokenize("one two three", tokensArr);
        Assert.assertEquals(3, tokens);
        Assert.assertEquals("one", tokensArr[0]);
        Assert.assertEquals("two", tokensArr[1]);
        Assert.assertEquals("three", tokensArr[2]);
        tokens = NASFetchDNS.tokenize("    one    two    three    ", tokensArr);
        Assert.assertEquals(3, tokens);
        Assert.assertEquals("one", tokensArr[0]);
        Assert.assertEquals("two", tokensArr[1]);
        Assert.assertEquals("three", tokensArr[2]);
        tokens = NASFetchDNS.tokenize("\t\t\t\tone\t\t\t\ttwo\t\t\t\tthree\t\t\t\t", tokensArr);
        Assert.assertEquals(3, tokens);
        Assert.assertEquals("one", tokensArr[0]);
        Assert.assertEquals("two", tokensArr[1]);
        Assert.assertEquals("three", tokensArr[2]);

        tokens = NASFetchDNS.tokenize("one two three four", tokensArr);
        Assert.assertEquals(3, tokens);
        Assert.assertEquals("one", tokensArr[0]);
        Assert.assertEquals("two", tokensArr[1]);
        Assert.assertEquals("three", tokensArr[2]);
        tokens = NASFetchDNS.tokenize("    one    two    three    four    ", tokensArr);
        Assert.assertEquals(3, tokens);
        Assert.assertEquals("one", tokensArr[0]);
        Assert.assertEquals("two", tokensArr[1]);
        Assert.assertEquals("three", tokensArr[2]);
        tokens = NASFetchDNS.tokenize("\t\t\t\tone\t\t\t\ttwo\t\t\t\tthree\t\t\t\tfour\t\t\t\t", tokensArr);
        Assert.assertEquals(3, tokens);
        Assert.assertEquals("one", tokensArr[0]);
        Assert.assertEquals("two", tokensArr[1]);
        Assert.assertEquals("three", tokensArr[2]);
    }

    @Test
    public void test_one() {
        NASFetchDNS fetcher = new NASFetchDNS();
        ReadSource hostsFile = new ReadSource() {
            @Override
            public Reader obtainReader() {
                StringReader sr = new StringReader(
                        "#\n"
                        + "#comment\n"
                        + "1.2.3.4   netarkivet.dk   netarkivet   # testing\n"
                        + "5.6.7.8   www.netarkivet.dk   nark# testing\n"
                        + "9.10.11.12   www.netarkivet.dk   # testing\n"
                        + "42.43.44.45   www.nas.dk   nas   # testing\n"
                );
                return sr;
            }
        };
        fetcher.setHostsFile(hostsFile); 
        ReadSource hostsSource = new ReadSource() {
            @Override
            public Reader obtainReader() {
                StringReader sr = new StringReader(
                        "#\n"
                        + "#comment\n"
                        + "4.3.2.1   kb.dk   kb\n"
                        + "8.7.6.5   www.kb.dk\n"
                        + "45.44.43.42   www.nas.dk\n"
                );
                return sr;
            }
        };
        fetcher.setHostsSource(hostsSource);
        fetcher.reload();

        /*
        Iterator<Entry<String, String>> iter = fetcher.hosts.entrySet().iterator();
        Entry<String, String> entry;
        while (iter.hasNext()) {
            entry = iter.next();
            System.out.println(entry.getKey() + " = " + entry.getValue());
        }
        */

        Assert.assertEquals(9, fetcher.hosts.size());
        Assert.assertEquals("9.10.11.12", fetcher.hosts.get("www.netarkivet.dk"));
        Assert.assertEquals("1.2.3.4", fetcher.hosts.get("netarkivet"));
        Assert.assertEquals("45.44.43.42", fetcher.hosts.get("www.nas.dk"));
        Assert.assertEquals("4.3.2.1", fetcher.hosts.get("kb"));
        Assert.assertEquals("5.6.7.8", fetcher.hosts.get("nark"));
        Assert.assertEquals("42.43.44.45", fetcher.hosts.get("nas"));
        Assert.assertEquals("8.7.6.5", fetcher.hosts.get("www.kb.dk"));
        Assert.assertEquals("4.3.2.1", fetcher.hosts.get("kb.dk"));
        Assert.assertEquals("1.2.3.4", fetcher.hosts.get("netarkivet.dk"));

        InetAddress inetAddress = null;
        byte[] address;
        try {
            inetAddress = InetAddress.getByName("42.43.44.45");
            Assert.assertNotNull(inetAddress);
            address = inetAddress.getAddress();
            Assert.assertEquals(4, address.length);
            Assert.assertEquals(42, address[0] & 255);
            Assert.assertEquals(43, address[1] & 255);
            Assert.assertEquals(44, address[2] & 255);
            Assert.assertEquals(45, address[3] & 255);
        } catch (UnknownHostException e1) {
            Assert.fail("Unexpected exception!");
        }
        try {
            inetAddress = InetAddress.getByName("::1");
            Assert.assertNotNull(inetAddress);
            address = inetAddress.getAddress();
            Assert.assertEquals(16, address.length);
            Assert.assertEquals(0, address[0] & 255);
            Assert.assertEquals(0, address[1] & 255);
            Assert.assertEquals(0, address[2] & 255);
            Assert.assertEquals(0, address[3] & 255);
            Assert.assertEquals(0, address[4] & 255);
            Assert.assertEquals(0, address[5] & 255);
            Assert.assertEquals(0, address[6] & 255);
            Assert.assertEquals(0, address[7] & 255);
            Assert.assertEquals(0, address[8] & 255);
            Assert.assertEquals(0, address[9] & 255);
            Assert.assertEquals(0, address[10] & 255);
            Assert.assertEquals(0, address[11] & 255);
            Assert.assertEquals(0, address[12] & 255);
            Assert.assertEquals(0, address[13] & 255);
            Assert.assertEquals(0, address[14] & 255);
            Assert.assertEquals(1, address[15] & 255);
        } catch (UnknownHostException e1) {
            Assert.fail("Unexpected exception!");
        }
    }

}
