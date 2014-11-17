/*
 * #%L
 * Netarchivesuite - harvester - test
 * %%
 * Copyright (C) 2005 - 2014 The Royal Danish Library, the Danish State and University Library,
 *             the National Library of France and the Austrian National Library.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */
package dk.netarkivet.harvester.harvesting.frontier;

import java.io.File;
import java.io.FileFilter;
import java.net.URL;

public class TestInfo {

	protected static ClassLoader clsLdr = TestInfo.class.getClassLoader();

	public static final File getTestResourceFile(String fname) {
	    URL url = clsLdr.getResource(fname);
        String path = url.getFile();
        path = path.replaceAll("%5b", "[");
        path = path.replaceAll("%5d", "]");
	    File file = new File(path);
	    return file;
	}

	// General dirs:
    protected static final File BASEDIR = getTestResourceFile("dk/netarkivet/harvester/harvesting/frontier/data");

    protected static final File WORKDIR = new File(BASEDIR, "working");

    static File[] getFrontierReportSamples() {
        return BASEDIR.listFiles(new FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.isFile() && f.getName().startsWith("frontierReport_all_sample_")
                        && f.getName().endsWith(".txt");
            }
        });
    }

}
