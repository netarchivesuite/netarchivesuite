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

import java.io.File;
import java.util.Date;
import java.util.Map;
import java.util.Set;

public abstract class ArchiveHeaderBase {

	public boolean bIsArc;

	public boolean bIsWarc;

	public abstract Object getHeaderValue(String key);

	public abstract String getHeaderStringValue(String key);

	public abstract Set<String> getHeaderFieldKeys();

	public abstract Map<String, Object> getHeaderFields();

	public abstract Date getDate();

	public abstract String getArcDateStr();

	public abstract long getLength();

	public abstract String getUrl();

	public abstract String getIp();

	public abstract String getMimetype();

	public abstract String getVersion();

	public abstract long getOffset();

	public abstract String getReaderIdentifier();

	public abstract String getRecordIdentifier();



	public abstract File getArchiveFile();

}
