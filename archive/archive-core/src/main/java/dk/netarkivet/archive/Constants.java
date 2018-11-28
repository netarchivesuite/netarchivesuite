/*
 * #%L
 * Netarchivesuite - archive
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
package dk.netarkivet.archive;

/**
 * Constants for the Archive module.
 */
public class Constants {
    /**
     * Internationalisation resource bundle.
     */
    public static final String TRANSLATIONS_BUNDLE = "dk.netarkivet.archive.Translations";

    /**
     * The name of the directory in which files are stored.
     */
    public static final String FILE_DIRECTORY_NAME = "filedir";

    /**
     * Temporary directory used during upload, where partial files exist, until moved into directory
     * FILE_DIRECTORY_NAME.
     */
    public static final String TEMPORARY_DIRECTORY_NAME = "tempdir";

    /**
     * Directory where "deleted" files are placed".
     */
    public static final String ATTIC_DIRECTORY_NAME = "atticdir";
}
