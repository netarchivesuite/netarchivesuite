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
package dk.netarkivet.archive.bitarchive;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Arrays;

import org.junit.After;
import org.junit.Before;

import dk.netarkivet.archive.ArchiveSettings;
import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.testutils.TestFileUtils;
import dk.netarkivet.testutils.preconfigured.MockupJMS;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;
import dk.netarkivet.testutils.preconfigured.UseTestRemoteFile;

/** A collection of setup/teardown stuff usable by most bitarchive tests.. */
public abstract class BitarchiveTestCase {

    private final UseTestRemoteFile rf = new UseTestRemoteFile();
    protected Bitarchive archive;
    ReloadSettings rs = new ReloadSettings();

    MockupJMS mj = new MockupJMS();

    /**
     * Make a new BitarchiveTestCase using the given directory for originals.
     *
     */
    protected abstract File getOriginalsDir();

    @Before
    public void setUp() throws Exception {
        FileUtils.removeRecursively(TestInfo.WORKING_DIR);

        // Copy over the "existing" bit archive.
        TestFileUtils.copyDirectoryNonCVS(getOriginalsDir(), TestInfo.WORKING_DIR);
        setUpArchive(TestInfo.WORKING_DIR.getAbsolutePath());
    }

    public void setUpArchive(final String... filedir) {
        this.tearDownArchive();
        // super.setUp();
        rs.setUp();
        mj.setUp();
        Settings.set(ArchiveSettings.BITARCHIVE_SERVER_FILEDIR, filedir);
        Channels.reset(); // resetting channels
        archive = Bitarchive.getInstance();

        rf.setUp();
    }

    public void tearDownArchive() {
        if (archive != null) {
            archive.close();
        }
        mj.tearDown();
        rf.tearDown();
        rs.tearDown();
    }

    @After
    public void tearDownArchiveAndClean() {
        tearDownArchive();
        Arrays.stream(Settings.getAll(ArchiveSettings.BITARCHIVE_SERVER_FILEDIR)).map(string -> new File(string))
                .forEach(file ->
                        FileUtils.removeRecursively(file));
    }

    public static void resetChannels() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
    	Field field = Channels.class.getDeclaredField("instance");
    	field.set(null, null);
    }
    
}
