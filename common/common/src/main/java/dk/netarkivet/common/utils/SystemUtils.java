/* File:            $Id$
 * Revision:        $Revision$
 * Author:          $Author$
 * Date:            $Date$
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
package dk.netarkivet.common.utils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.net.ServerSocket;
import java.net.BindException;
import java.io.IOException;

import dk.netarkivet.common.exceptions.IOFailure;

/** Miscellanous utilities for getting system resources. */
public class SystemUtils {

    /** Hostname for this machine used when no name can be found, or when the
     * actual name doesn't matter. */
    public static final String LOCALHOST = "localhost";

    /**
     * Looks up the IP number of the local host.
     * Note that Java does not guarantee that the result is
     * IPv4 or IPv6.
     * @return the found IP; returns "UNKNOWNIP" if it could not
     * be found.
     */
    public static String getLocalIP() {
        String result;
        try {
            result = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            result = "UNKNOWNIP";
        }
        return result;
    }

    /**
     * Get the first hostname available for this machine, or "localhost" if none
     * are available.
     *
     * @return A hostname, as returned by
     * InetAddress.getLocalHost().getCanonicalHostName()()
     */
    public static String getLocalHostName() {
        String hostname = LOCALHOST;
        try {
            hostname = InetAddress.getLocalHost().getCanonicalHostName();
        } catch (UnknownHostException e) {
            // If no interfaces, use default;
        }
        return hostname;
    }

    /** Check that a given port is not in use.  If this method returns
     * normally, the port is safe to bind.
     *
     * @param port Port to check
     * @throws IOFailure if the port cannot be bound.
     */
    public static void checkPortNotUsed(int port) {
        try {
            ServerSocket s = new ServerSocket(port, 1);
            s.close();
        } catch (BindException e) {
            throw new IOFailure("Port " + port + " already in use, or "
                                + "port is out of range", e);
        } catch (IOException e) {
            throw new IOFailure("IO error testing port " + port, e);
        }
    }
}
