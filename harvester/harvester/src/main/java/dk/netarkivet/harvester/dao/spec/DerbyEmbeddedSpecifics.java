/* File:        $Id: DerbyEmbeddedSpecifics.java 2539 2012-11-15 13:42:09Z csr $
 * Revision:    $Revision: 2539 $
 * Author:      $Author: csr $
 * Date:        $Date: 2012-11-15 14:42:09 +0100 (Thu, 15 Nov 2012) $
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2012 The Royal Danish Library, the Danish State and
 * University Library, the National Library of France and the Austrian
 * National Library.
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

package dk.netarkivet.harvester.dao.spec;


/**
 * A class that implement functionality specific to the embedded Derby system.
 */
public class DerbyEmbeddedSpecifics extends DerbySpecifics {
	
	/**
     * Get an instance of the Embedded Derby specifics.
     * @return Instance of the Derby specifics implementation
     */
    public static DBSpecifics getInstance() {
        return new DerbyEmbeddedSpecifics();
    }

    /** Get the name of the JDBC driver class that handles interfacing
     * to this server.
     *
     * @return The name of a JDBC driver class
     */
    public String getDriverClassName() {
        return "org.apache.derby.jdbc.EmbeddedDriver";
    }


}
