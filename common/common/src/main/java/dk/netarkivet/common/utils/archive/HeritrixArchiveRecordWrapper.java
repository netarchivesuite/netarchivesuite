/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
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
package dk.netarkivet.common.utils.archive;

import java.io.InputStream;

import org.archive.io.ArchiveRecord;
import org.archive.io.arc.ARCRecord;
import org.archive.io.warc.WARCRecord;

import dk.netarkivet.common.exceptions.ArgumentNotValid;

public class HeritrixArchiveRecordWrapper extends ArchiveRecordBase {

	protected ArchiveRecord record;

	protected ArchiveHeaderBase header;

	public HeritrixArchiveRecordWrapper(ArchiveRecord record) {
		this.record = record;
		this.header = HeritrixArchiveHeaderWrapper.wrapArchiveHeader(this, record);
		if (record instanceof ARCRecord) {
			this.bIsArc = true;
		} else if (record instanceof WARCRecord) {
			this.bIsWarc = true;
		} else {
	        throw new ArgumentNotValid(
	                "Unsupported ArchiveRecord type: "
	                + record.getClass().getName());
		}
	}

	public ArchiveHeaderBase getHeader() {
		return header;
	}

	public InputStream getInputStream() {
		return record;
	}

}
