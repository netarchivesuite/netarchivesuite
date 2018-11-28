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

import static org.archive.modules.fetcher.FetchStatusCodes.S_DNS_SUCCESS;
import static org.archive.modules.fetcher.FetchStatusCodes.S_UNFETCHABLE_URI;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.httpclient.URIException;
import org.apache.commons.io.IOUtils;
import org.archive.io.ReadSource;
import org.archive.modules.CrawlURI;
import org.archive.modules.fetcher.FetchDNS;
import org.archive.modules.net.CrawlHost;

/**
 * Extended FetchDNS processor which allows the override of hosts
 * to be used before they are querying through a DNS server.
 *
 * @author nicl
 */
public class NASFetchDNS extends FetchDNS {

    /** Logger instance. */
    private static Logger logger = Logger.getLogger(FetchDNS.class.getName());

    /**
     * Look for hosts in the hosts file/text value before doing a DNS lookup.
     */
    protected boolean acceptDefinedHosts = true; 
    public boolean getAcceptDefinedHosts() {
        return acceptDefinedHosts;
    }
    // @Required
    public void setAcceptDefinedHosts(boolean acceptDefinedHosts) {
        this.acceptDefinedHosts = acceptDefinedHosts;
    }

    /**
     * Text from which to load hosts
     */
    protected ReadSource hostsFile = null;
    public ReadSource getHostsFile() {
        return hostsFile;
    }
    // @Required
    public void setHostsFile(ReadSource hostsFile) {
        this.hostsFile = hostsFile;
    }

    /**
     * Text from which to look for hosts.
     */
    protected ReadSource hostsSource = null;
    public ReadSource getHostsSource() {
        return hostsSource;
    }
    // @Required
    public void setHostsSource(ReadSource hostsSource) {
        this.hostsSource = hostsSource;
    }

    ///private static final long DEFAULT_TTL_FOR_HOSTS_RESOLVES = Long.MAX_VALUE;        // A very long time...

    /** Has the hosts been loaded. */
    private boolean bInitialized = false;

    /** Map of hosts that override the normal DNS lookup. */
    protected Map<String, String> hosts;

    /*
     * Check for the host in the hosts map before calling the extended method.
     * @see org.archive.modules.fetcher.FetchDNS#innerProcess(org.archive.modules.CrawlURI)
     */
    @Override
    protected void innerProcess(CrawlURI curi) {
        InetAddress address = null;
        if (acceptDefinedHosts) {
            String dnsName = null;
            try {
                dnsName = curi.getUURI().getReferencedHost();
            } catch (URIException e) {
                logger.log(Level.SEVERE, "Failed parse of dns record " + curi, e);
            }
            if(dnsName == null) {
                curi.setFetchStatus(S_UNFETCHABLE_URI);
                return;
            }
            CrawlHost targetHost = getServerCache().getHostFor(dnsName);
            if (isQuadAddress(curi, dnsName, targetHost)) {
                // We're done processing.
                return;
            }
            if (!bInitialized) {
                reload();
                bInitialized = true;
            }
            if (hosts.size() > 0) {
                // Do actual DNS lookup.
                String ipAddress = hosts.get(dnsName);
                if (ipAddress != null) {
                    curi.setFetchBeginTime(System.currentTimeMillis());
                    try {
                        address = InetAddress.getByName(ipAddress);
                    } catch (UnknownHostException e1) {
                        address = null;
                    }
                    if (address != null) {
                        //targetHost.setIP(address, DEFAULT_TTL_FOR_HOSTS_RESOLVES);
                        //curi.setFetchStatus(S_GETBYNAME_SUCCESS);
                        targetHost.setIP(address, CrawlHost.IP_NEVER_EXPIRES);
                        curi.setFetchStatus(S_DNS_SUCCESS);
                        curi.setContentType("text/dns");
                        if (logger.isLoggable(Level.FINE)) {
                            logger.fine("Found address for " + dnsName + " using hosts file.");
                        }
                    } else {
                        if (logger.isLoggable(Level.FINE)) {
                            logger.fine("Failed find of address for " + dnsName + " using hosts file.");
                        }
                        setUnresolvable(curi, targetHost);
                    }
                    curi.setFetchCompletedTime(System.currentTimeMillis());
                }
            }
        }
        if (address == null) {
            super.innerProcess(curi);
        }
    }

    /**
     * Clear loaded hosts of reload from hosts file and value text.
     */
    protected void reload() {
        hosts = new HashMap<String, String>();
        getHosts(getHostsFile());
        getHosts(getHostsSource());
    }

    /**
     * Run through the lines in a <code>ReadSource</code> and add all valid host lines encountered.
     * @param hostsSource hosts file or value text
     */
    protected void getHosts(ReadSource hostsSource) {
        if (hostsSource != null) {
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("reading surt prefixes from " + hostsSource);
            }
            Reader reader = hostsSource.obtainReader();
            BufferedReader bufferedReader = new BufferedReader(reader);
            try {
                String str;
                while ((str = bufferedReader.readLine()) != null) {
                    str = str.trim();
                    if (!str.startsWith("#")) {
                        int idx = str.indexOf('#');
                        if (idx != -1) {
                            str = str.substring(0, idx).trim();
                        }
                        String[] tokensArr = new String[3];
                        int tokens = tokenize(str, tokensArr);
                        if (tokens >= 2) {
                            hosts.put(tokensArr[1], tokensArr[0]);
                        }
                        if (tokens >= 3) {
                            hosts.put(tokensArr[2], tokensArr[0]);
                        }
                    }
                }
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Exception parsing hosts", e);
            } finally {
                IOUtils.closeQuietly(bufferedReader);
                IOUtils.closeQuietly(reader);
            }
        }
    }

    /**
     * Split input string into tokens. Treats multiple whitespace as one.
     * Only parse the number of tokens that are able to fit into the supplied token array.
     * @param str split input string into tokens
     * @param tokensArr supply a string array to be filled with tokens
     * @return number of tokens inserted into the token array
     */
    public static int tokenize(String str, String[] tokensArr) {
        int tokens = 0;
        int idx = 0;
        int pIdx;
        while (tokens < tokensArr.length && idx < str.length()) {
            while (idx < str.length() && Character.isWhitespace(str.charAt(idx))) {
                ++idx;
            }
            pIdx = idx;
            while (idx < str.length() && !Character.isWhitespace(str.charAt(idx))) {
                ++idx;
            }
            if (idx > pIdx) {
                tokensArr[tokens++] = str.substring(pIdx, idx);
            }
        }
        return tokens;
    }

}
