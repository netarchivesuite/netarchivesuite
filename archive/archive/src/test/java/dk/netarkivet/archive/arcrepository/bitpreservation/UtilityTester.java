/* File:    $Id: ArchiveArcrepositoryBitPreservationTesterSuite.java 1276 2010-02-18 16:36:45Z jolf $
 * Version: $Revision: 1276 $
 * Date:    $Date: 2010-02-18 17:36:45 +0100 (Thu, 18 Feb 2010) $
 * Author:  $Author: jolf $
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

package dk.netarkivet.archive.arcrepository.bitpreservation;

import dk.netarkivet.testutils.ReflectUtils;
import junit.framework.TestCase;

public class UtilityTester extends TestCase {

    public void testConstants() {
        ReflectUtils.testUtilityConstructor(Constants.class);
    }
    
}
