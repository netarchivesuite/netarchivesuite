/* $Id$
 * $Revision$
 * $Date$
 * $Author$
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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 *   USA
 */
package dk.netarkivet.deploy;

import dk.netarkivet.common.utils.StringUtils;

public final class ScriptConstants {
    // Strings
    /** The header of some scripts.*/
    static final String BIN_BASH_COMMENT = "#!/bin/bash";
    
    // integers
    /** Number of '-' repeat for the writeDashLine function.*/
    static final int SCRIPT_DASH_NUM_REPEAT = 44;

    // functions
    /** Function for creating dash lines in scripts. */
    public static String writeDashLine() {
	return "echo " + StringUtils.repeat("-", SCRIPT_DASH_NUM_REPEAT);
    }

    
}
