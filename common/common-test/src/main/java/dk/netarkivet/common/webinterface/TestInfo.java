/*
 * #%L
 * Netarchivesuite - common - test
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
package dk.netarkivet.common.webinterface;

import java.io.File;

public class TestInfo {

    public static final File DATA_DIR = new File("tests/dk/netarkivet/common/webinterface/data");
    public static final File ORIGINALS_DIR = new File(DATA_DIR, "originals");
    public static final File WORKING_DIR = new File(DATA_DIR, "working");
    public static final File TEMPDIR = new File(DATA_DIR, "working/");

    public static final int GUI_WEB_SERVER_PORT = 4242;
    public static final String GUI_WEB_SERVER_WEBBASE = "/jsp";
    public static final String GUI_WEB_SERVER_JSP_DIRECTORY = "tests/dk/netarkivet/common/webinterface/data/jsp";
    public static final String GUI_WEB_SERVER_SITESECTION_CLASS = TestSiteSection.class.getName();
}
