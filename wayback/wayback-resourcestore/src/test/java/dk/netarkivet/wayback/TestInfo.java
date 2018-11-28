/*
 * #%L
 * Netarchivesuite - wayback - test
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
package dk.netarkivet.wayback;

import java.io.File;

/**
 * Defines test data and directories for the package dk.netarkivet.archive.arcrepository.
 */
public class TestInfo {

    public static final File DATA_DIR = new File("tests/dk/netarkivet/wayback/data/");
    public static final File ORIGINALS_DIR = new File(DATA_DIR, "originals");
    public static final File WORKING_DIR = new File(DATA_DIR, "working");
    public static final File FILE_DIR = new File(WORKING_DIR, "filedir");
    /*
     * static final File CORRECT_ORIGINALS_DIR = new File(DATA_DIR, "correct/originals/"); static final File
     * CORRECT_WORKING_DIR = new File(DATA_DIR, "correct/working/"); static final File TMP_FILE = new File(WORKING_DIR,
     * "temp");
     */
    public static final long SHORT_TIMEOUT = 1000;

}
