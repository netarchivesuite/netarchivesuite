/*
 * #%L
 * Netarchivesuite - archive - test
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

package dk.netarkivet.archive.arcrepository.distribute;

import java.io.File;

public class TestInfo {
    public static final File BASEDIR = new File("tests/dk/netarkivet/archive/arcrepository/distribute/data");

    public static final File ORIGINALS = new File(BASEDIR, "originals");

    public static final File WORKING = new File(BASEDIR, "working");

    public static final File ARCDIR = new File(WORKING, "local_files");
    public static final File ARCFILE = new File(ARCDIR, "Upload2.ARC");

}
