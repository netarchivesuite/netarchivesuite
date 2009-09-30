/* File:                $Id$
 * Revision:            $Revision$
 * Date:                $Date$
 * Author:              $Author$
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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package dk.netarkivet.archive.bitarchive;

import dk.netarkivet.archive.bitarchive.distribute.BitarchiveServer;
import dk.netarkivet.common.utils.ApplicationUtils;

/**
 * This class is used to start the BitArchive application.
 *
 */
public class BitarchiveApplication {
   /**
    * Runs the BitarchiveApplication. Settings are read from
    * config files
    *
    * @see BitarchiveServer
    * @param args an empty array
    */
   public static void main(String[] args) {
       ApplicationUtils.startApp(BitarchiveServer.class, args);
   }
}
