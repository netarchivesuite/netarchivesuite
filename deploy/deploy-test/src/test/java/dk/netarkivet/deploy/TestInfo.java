/*
 * #%L
 * Netarchivesuite - deploy - test
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
package dk.netarkivet.deploy;

import java.io.File;

import dk.netarkivet.testutils.TestResourceUtils;

public class TestInfo {
    // directories
    public static final File DATA_DIR = new File("tests/dk/netarkivet/deploy/data");
    public static final File ORIGINALS_DIR = new File(DATA_DIR, "originals");
    public static final File WORKING_DIR = new File(DATA_DIR, "working");
    public static final File TMPDIR = new File(WORKING_DIR, "tmpdir");
    public static final File TARGETDIR = new File(WORKING_DIR, "target");
    public static final File SINGLE_TARGET_DIR = new File(WORKING_DIR, "single_target");
    public static final File DATABASE_TARGET_DIR = new File(WORKING_DIR, "database_target");
    public static final File TEST_TARGET_DIR = new File(WORKING_DIR, "test_target");

    // argument files
    public static final File IT_CONF_FILE = new File(WORKING_DIR, "deploy_config.xml");
    public static final File IT_CONF_SINGLE_FILE = new File(WORKING_DIR, "deploy_single_config.xml");
    public static final File FILE_NETATCHIVE_SUITE = new File(WORKING_DIR, "null.zip");
    public static final File FILE_SECURITY_POLICY = new File(WORKING_DIR, "security.policy");
    public static final File FILE_LOG_PROP = new File(WORKING_DIR, "log.prop");
    public static final File FILE_DATABASE = new File(WORKING_DIR, "database.jar");
    public static final File FILE_BP_DATABASE = new File(WORKING_DIR, "bpdb.jar");
    public static final File EXTERNALS_DIR = new File(WORKING_DIR, "externals");

    // arguments
    public static final String ARGUMENT_CONFIG_FILE = Constants.ARG_INIT_ARG + Constants.ARG_CONFIG_FILE;
    public static final String ARGUMENT_NETARCHIVE_SUITE_FILE = Constants.ARG_INIT_ARG
            + Constants.ARG_NETARCHIVE_SUITE_FILE;
    public static final String ARGUMENT_SECURITY_FILE = Constants.ARG_INIT_ARG + Constants.ARG_SECURITY_FILE;
    public static final String ARGUMENT_SLF4J_CONFIG_FILE = Constants.ARG_INIT_ARG + Constants.ARG_SLF4J_CONFIG_FILE;
    public static final String ARGUMENT_OUTPUT_DIRECTORY = Constants.ARG_INIT_ARG + Constants.ARG_OUTPUT_DIRECTORY;
    public static final String ARGUMENT_HARVEST_DATABASE_FILE = Constants.ARG_INIT_ARG + Constants.ARG_DATABASE_FILE;
    public static final String ARGUMENT_ARCHIVE_DATABASE_FILE = Constants.ARG_INIT_ARG + Constants.ARG_ARC_DB;
    public static final String ARGUMENT_TEST = Constants.ARG_INIT_ARG + Constants.ARG_TEST;
    public static final String ARGUMENT_EVALUATE = Constants.ARG_INIT_ARG + Constants.ARG_EVALUATE;
    public static final String ARGUMENT_JAR_FOLDER = Constants.ARG_INIT_ARG + Constants.ARG_JAR_FOLDER;
    public static final String ARGUMENT_TEST_ARG = "1000,1005,test,test@kb.dk";

    public static final String LOGBACK_CONFIG = TestResourceUtils.getFilePath("logback-test.xml");
}
