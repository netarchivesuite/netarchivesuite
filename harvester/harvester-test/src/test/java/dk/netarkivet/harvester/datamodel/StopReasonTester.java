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
package dk.netarkivet.harvester.datamodel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Locale;

import org.junit.Test;

import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.common.utils.I18n;

/**
 * Tests the StopReason class.
 */
public class StopReasonTester {

    private static final I18n I18N = new I18n(dk.netarkivet.harvester.Constants.TRANSLATIONS_BUNDLE);

    /**
     * Tests the translation from numbers to stop reasons.
     * <p>
     * DO NOT CHANGE THESE NUMBERS! StopReason is serialised to database using these numbers, and they are never
     * expected to change. That would kill an upgrade with an existing database!
     */
    @Test
    public void testGetStopReason() {
        // Test that stopreasonNum=0 returns StopReason.DOWNLOAD_COMPLETE
        assertEquals("getStopReason(0) should return StopReason.DOWNLOAD_COMPLETE", StopReason.DOWNLOAD_COMPLETE,
                StopReason.getStopReason(0));

        // Test that stopreasonNum=1 returns StopReason.OBJECT_LIMIT
        assertEquals("getStopReason(1) should return StopReason.OBJECT_LIMIT", StopReason.OBJECT_LIMIT,
                StopReason.getStopReason(1));

        // Test that stopreasonNum=2 returns StopReason.SIZE_LIMIT
        assertEquals("getStopReason(2) should return StopReason.SIZE_LIMIT", StopReason.SIZE_LIMIT,
                StopReason.getStopReason(2));

        // Test that stopreasonNum=3 returns StopReason.CONFIG_SIZE_LIMIT
        assertEquals("getStopReason(3) should return StopReason.CONFIG_SIZE_LIMIT", StopReason.CONFIG_SIZE_LIMIT,
                StopReason.getStopReason(3));

        // Test that stopreasonNum=4 returns StopReason.DOWNLOAD_UNFINISHED
        assertEquals("getStopReason(4) should return StopReason.DOWNLOAD_UNFINISHED", StopReason.DOWNLOAD_UNFINISHED,
                StopReason.getStopReason(4));

        // Test that stopreasonNum=5 returns StopReason.CONFIG_OBJECT_LIMIT
        assertEquals("getStopReason(5) should return StopReason.CONFIG_OBJECT_LIMIT", StopReason.CONFIG_OBJECT_LIMIT,
                StopReason.getStopReason(5));

        // Test that stopreasonNum=6 returns StopReason.TIME_LIMIT
        assertEquals("getStopReason(6) should return StopReason.TIME_LIMIT", StopReason.TIME_LIMIT,
                StopReason.getStopReason(6));

        // Test that stopreasonNum less than 0 and greater than 6 results in
        // IOFailure
        try {
            StopReason.getStopReason(-1);
            fail("UnknownID expected");
        } catch (UnknownID e) {
            // UnknownID expected
        }
        try {
            StopReason.getStopReason(7);
            fail("UnknownID expected");
        } catch (UnknownID e) {
            // UnknownID expected
        }
    }

    /**
     * Test, that the localized String for a given StopReason is correct. We only test with english Locale.
     */
    @Test
    public void testGetLocalizedString() {
        Locale l = new Locale("en");
        assertEquals("StopReason.DOWNLOAD_UNFINISHED.getLocalizedString(l) "
                + "should return correct String for english Locale",
                StopReason.DOWNLOAD_UNFINISHED.getLocalizedString(l),
                I18N.getString(l, "stopreason.download.unfinished"));

        assertEquals("StopReason.DOWNLOAD_COMPLETE.getLocalizedString(l) "
                + "should return correct String for english Locale",
                StopReason.DOWNLOAD_COMPLETE.getLocalizedString(l), I18N.getString(l, "stopreason.complete"));

        assertEquals("StopReason.CONFIG_OBJECT_LIMIT.getLocalizedString(l) "
                + "should return correct String for english Locale",
                StopReason.CONFIG_OBJECT_LIMIT.getLocalizedString(l),
                I18N.getString(l, "stopreason.max.domainobjects.limit.reached"));

        assertEquals("StopReason.CONFIG_SIZE_LIMIT.getLocalizedString(l) "
                + "should return correct String for english Locale",
                StopReason.CONFIG_SIZE_LIMIT.getLocalizedString(l),
                I18N.getString(l, "stopreason.max.domainconfig.limit.reached"));

        assertEquals("StopReason.OBJECT_LIMIT.getLocalizedString(l) "
                + "should return correct String for english Locale", StopReason.OBJECT_LIMIT.getLocalizedString(l),
                I18N.getString(l, "stopreason.max.objects.limit.reached"));

        assertEquals(
                "StopReason.SIZE_LIMIT.getLocalizedString(l) " + "should return correct String for english Locale",
                StopReason.SIZE_LIMIT.getLocalizedString(l), I18N.getString(l, "stopreason.max.bytes.limit.reached"));

        // Verify that the unknown case does not break I18n
        assertTrue("getLocalizedString should not break in the unknown case", I18N.getString(l, "stopreason.unknown.0")
                .contains("Unknown reason:"));
    }
}
