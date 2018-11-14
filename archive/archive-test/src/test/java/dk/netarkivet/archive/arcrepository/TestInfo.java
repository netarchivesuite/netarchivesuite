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

package dk.netarkivet.archive.arcrepository;

import java.io.File;

/**
 * Defines test data and directories for the package dk.netarkivet.archive.arcrepository.
 */
class TestInfo {

    static final File DATA_DIR = new File("tests/dk/netarkivet/archive/arcrepository/data");
    static final File ORIGINALS_DIR = new File(DATA_DIR, "originals");
    static final File WORKING_DIR = new File(DATA_DIR, "working");
    static final File CORRECT_ORIGINALS_DIR = new File(DATA_DIR, "correct/originals/");
    static final File CORRECT_WORKING_DIR = new File(DATA_DIR, "correct/working/");
    static final File TMP_FILE = new File(WORKING_DIR, "temp");
    public static final long SHORT_TIMEOUT = 1000;

    static final File ORIGINAL_DATABASE_DIR = new File(DATA_DIR, "database");
    static final File DATABASE_DIR = new File(WORKING_DIR, "database");
    static final File DATABASE_FILE = new File("archivedatabasedir", "archivedb.sql");

    /* Constants taken from the old dk.netarkivet.archive.distribute.arcrepository.TestInfo */
    private static final File DISTRIBUTE_ARCREPOSITORY_BASE_DIR = new File("tests/dk/netarkivet/archive/distribute/arcrepository/data");
    public static final File DISTRIBUTE_ARCREPOSITORY_WORKING_DIR = new File(DISTRIBUTE_ARCREPOSITORY_BASE_DIR, "working");
    public static final File DISTRIBUTE_ARCREPOSITORY_ORIGINALS_DIR = new File(DISTRIBUTE_ARCREPOSITORY_BASE_DIR, "originals");

  /**
   * An archive directory to work on.
   */
   public static final File DISTRIBUTE_ARCREPOSITORY_ARCHIVE_DIR = new File(DISTRIBUTE_ARCREPOSITORY_WORKING_DIR, "bitarchive1");
   public static final File DISTRIBUTE_ARCREPOSITORY_INDEX_DIR_2_3 = new File(DISTRIBUTE_ARCREPOSITORY_WORKING_DIR, "2-3-cache");
//  public static final File DISTRIBUTE_ARCREPOSITORY_INDEX_DIR_2_4_3_5 = new File(DISTRIBUTE_ARCREPOSITORY_WORKING_DIR, "2-4-3-5-cache");
   public static final File DISTRIBUTE_ARCREPOSITORY_LOG_PATH = new File(DISTRIBUTE_ARCREPOSITORY_WORKING_DIR, "tmp");
   public static final File DISTRIBUTE_ARCREPOSITORY_SAMPLE_FILE = new File(DISTRIBUTE_ARCREPOSITORY_WORKING_DIR, "testFile.txt");
   public static final File DISTRIBUTE_ARCREPOSITORY_SAMPLE_FILE_COPY = new File(DISTRIBUTE_ARCREPOSITORY_WORKING_DIR, "testCopy.txt");
   public static final File DISTRIBUTE_ARCREPOSITORY_EMPTY_FILE = new File(DISTRIBUTE_ARCREPOSITORY_WORKING_DIR, "zeroByteFile");

}
