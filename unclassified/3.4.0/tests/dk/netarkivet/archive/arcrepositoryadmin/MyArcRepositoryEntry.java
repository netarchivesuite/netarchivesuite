/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
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
package dk.netarkivet.archive.arcrepositoryadmin;

import dk.netarkivet.archive.arcrepository.distribute.StoreMessage;

/**
 * Class needed to test the constructor for FilePreservationStatus, which takes
 * an ArcRepositoryEntry as one of its arguments.
 * The constructor of ArcRepositoryEntry is package private.
 *
 *
 */
public class MyArcRepositoryEntry extends ArcRepositoryEntry {

    public MyArcRepositoryEntry(String filename, String md5sum,
            StoreMessage replyInfo) {
        super(filename, md5sum, replyInfo);

    }

}
