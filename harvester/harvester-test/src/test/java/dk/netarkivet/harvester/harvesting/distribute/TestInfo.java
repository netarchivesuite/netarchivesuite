/*
 * #%L
 * Netarchivesuite - harvester - test
 * %%
 * Copyright (C) 2005 - 2018 The Royal Danish Library, 
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
package dk.netarkivet.harvester.harvesting.distribute;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import dk.netarkivet.harvester.harvesting.metadata.MetadataEntry;

/**
 * Contains test information about all harvestdefinition test data.
 */
public class TestInfo {
    public static final File DATA_DIR = new File("tests/dk/netarkivet/harvester/harvesting/distribute/data/");
    public static final File SERVER_DIR = new File(TestInfo.DATA_DIR, "server");
    public static final MetadataEntry sampleEntry = new MetadataEntry("metadata://netarkivet.dk", "text/plain",
            "THIS IS SOME METADATA");
    public static final List<MetadataEntry> emptyMetadata = new ArrayList<MetadataEntry>();
    public static final List<MetadataEntry> oneMetadata = new ArrayList<MetadataEntry>();
    public static final String prefix = "ID";
    public static final String suffix = "X";

    static File WORKING_DIR = new File(TestInfo.DATA_DIR, "working");
    static File ORIGINALS_DIR = new File(TestInfo.DATA_DIR, "originals");
}
