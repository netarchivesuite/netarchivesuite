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
package dk.netarkivet.viewerproxy;

import static org.junit.Assert.assertEquals;

import java.net.URI;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 * Tests of the MissingURIRecorder class.
 */
public class MissingURIRecorderTester {
    private MissingURIRecorder mur;

    @Before
    public void setUp() {
        mur = new MissingURIRecorder();
    }

    /**
     * Tests start. Simply tests that urls are recorded after start is called.
     */
    @Test
    public void testStartRecordingURIs() throws Exception {
        assertEquals("mur should report no collected urls", 0, mur.getRecordedURIs().size());
        // start collecting
        mur.startRecordingURIs();
        // give a not found url
        mur.notify(new URI("http://foo.bar"), URIResolver.NOT_FOUND);
        Set<URI> recordedURIs = mur.getRecordedURIs();
        assertEquals("mur should have collected URI", 1, recordedURIs.size());
        assertEquals("mur should have collected URI", new URI("http://foo.bar"), recordedURIs.iterator().next());
    }

    /** Tests stop. Tests that urls are NOT recorded after calling stop. */
    @Test
    public void testStopRecordingURIs() throws Exception {
        assertEquals("mur should report no collected urls", 0, mur.getRecordedURIs().size());
        // start collecting
        mur.startRecordingURIs();
        // give a not found url
        mur.notify(new URI("http://foo.bar"), URIResolver.NOT_FOUND);
        Set<URI> recordedURIs = mur.getRecordedURIs();
        assertEquals("mur should have collected URI", 1, recordedURIs.size());
        assertEquals("mur should have collected URI", new URI("http://foo.bar"), recordedURIs.iterator().next());
        mur.stopRecordingURIs();
        // give a not found url
        mur.notify(new URI("http://foo2.bar"), URIResolver.NOT_FOUND);
        recordedURIs = mur.getRecordedURIs();
        assertEquals("mur should NOT have collected URI", 1, recordedURIs.size());
        assertEquals("mur should have collected URI", new URI("http://foo.bar"), recordedURIs.iterator().next());
    }

    /**
     * Tests clear. Tests that the set of URLs is cleared after calling clear.
     */
    @Test
    public void testClearRecordedURIs() throws Exception {
        assertEquals("mur should report no collected urls", 0, mur.getRecordedURIs().size());
        // start collecting
        mur.startRecordingURIs();
        // give a not found url
        mur.notify(new URI("http://foo.bar"), URIResolver.NOT_FOUND);
        Set<URI> recordedURIs = mur.getRecordedURIs();
        assertEquals("mur should have collected URI", 1, recordedURIs.size());
        assertEquals("mur should have collected URI", new URI("http://foo.bar"), recordedURIs.iterator().next());
        mur.clearRecordedURIs();
        recordedURIs = mur.getRecordedURIs();
        assertEquals("mur cleared collected URIs", 0, recordedURIs.size());
    }

    /**
     * Tests getRecordedURIs. Tests the result of getRecordedURIs before adding a URL, and after adding 1 and 2 URLs.
     * After the second url, the order should be sorted.
     */
    @Test
    public void testGetRecordedURIs() throws Exception {
        assertEquals("mur should report no collected urls yet", 0, mur.getRecordedURIs().size());
        // start collecting
        mur.startRecordingURIs();
        // give a not found url
        mur.notify(new URI("http://foob.bar"), URIResolver.NOT_FOUND);
        Set<URI> recordedURIs = mur.getRecordedURIs();
        assertEquals("mur should have collected URI", 1, recordedURIs.size());
        assertEquals("mur should have collected URI", new URI("http://foob.bar"), recordedURIs.iterator().next());
        // give another not found url
        mur.notify(new URI("http://fooa.bar"), URIResolver.NOT_FOUND);
        recordedURIs = mur.getRecordedURIs();
        assertEquals("mur should have collected URI", 2, recordedURIs.size());
        assertEquals("mur should have collected URI", new URI("http://fooa.bar"), recordedURIs.iterator().next());
        assertEquals("mur should have collected URI", new URI("http://foob.bar"), recordedURIs.toArray()[1]);
    }

    /**
     * Tests notify. First tests for exception on null argument. Then tests each of the four cases: 1) Recording URIs
     * and not found url reported 2) Recording URIs and found url reported 3) Not Recording URIs and not found url
     * reported 4) Not Recording URIs and found url reported Only case 1) should add an url to the set.
     */
    @Test
    public void testNotify() throws Exception {
        try {
            mur.notify(null, 0);
            // Expected ArgumentNotValid
        } catch (ArgumentNotValid e) {
            // expected
        }

        assertEquals("mur should report no collected urls yet", 0, mur.getRecordedURIs().size());
        // start collecting
        mur.startRecordingURIs();
        // give a not found url
        mur.notify(new URI("http://foo.bar"), URIResolver.NOT_FOUND);
        Set<URI> recordedURIs = mur.getRecordedURIs();
        assertEquals("mur should have collected URI", 1, recordedURIs.size());
        assertEquals("mur should have collected URI", new URI("http://foo.bar"), recordedURIs.iterator().next());
        // give a found url
        mur.notify(new URI("http://foo2.bar"), 200);
        recordedURIs = mur.getRecordedURIs();
        assertEquals("mur should NOT have collected URI", 1, recordedURIs.size());
        assertEquals("mur should NOT have collected URI", new URI("http://foo.bar"), recordedURIs.toArray()[0]);
        // stop collecting
        mur.startRecordingURIs();
        // give a not found url
        mur.notify(new URI("http://foo.bar"), URIResolver.NOT_FOUND);
        recordedURIs = mur.getRecordedURIs();
        assertEquals("mur should NOT have collected URI", 1, recordedURIs.size());
        assertEquals("mur should have collected URI", new URI("http://foo.bar"), recordedURIs.iterator().next());
        // give a found url
        mur.notify(new URI("http://foo2.bar"), 200);
        recordedURIs = mur.getRecordedURIs();
        assertEquals("mur should NOT have collected URI", 1, recordedURIs.size());
        assertEquals("mur should NOT have collected URI", new URI("http://foo.bar"), recordedURIs.toArray()[0]);
    }
}
