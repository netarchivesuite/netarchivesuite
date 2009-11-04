/* File:       $Id: Bitarchive.java 752 2009-03-05 18:09:21Z svc $
 * Revision:   $Revision: 752 $
 * Author:     $Author: svc $
 * Date:       $Date: 2009-03-05 19:09:21 +0100 (to, 05 mar 2009) $
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

package dk.netarkivet.archive.arcrepository.distribute;

import java.io.File;
import java.io.OutputStream;

import dk.netarkivet.common.utils.batch.FileBatchJob;

/**
 * @author ngiraud
 *
 */
public class ArcFileRemover extends FileBatchJob {

	@Override
	public void finish(OutputStream os) {

	}

	@Override
	public void initialize(OutputStream os) {

	}

	@Override
	public boolean processFile(File file, OutputStream os) {
		return file.delete();
	}

}
