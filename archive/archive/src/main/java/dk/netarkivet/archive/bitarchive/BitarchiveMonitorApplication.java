/* File: $Id$
 * Revision: $Revision$
 * Date:     $Date$
 * Author:   $Author$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2009 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301 
 *  USA
 */
package dk.netarkivet.archive.bitarchive;

import dk.netarkivet.archive.bitarchive.distribute.BitarchiveMonitorServer;
import dk.netarkivet.common.utils.ApplicationUtils;

/**
 * This class is used to start the BitarchiveMonitor application.
 *
 * @see BitarchiveMonitorServer
 */
public class BitarchiveMonitorApplication {
    /**
     * Private constructor to prevent instantiation of this class.
     */
    private BitarchiveMonitorApplication() {}
    
   /**
    * Runs the BitarchiveMonitor. Settings are read from
    * config files
    *
    * @param args an empty array
    */
   public static void main(String[] args) {
       ApplicationUtils.startApp(BitarchiveMonitorServer.class, args);
   }
}
