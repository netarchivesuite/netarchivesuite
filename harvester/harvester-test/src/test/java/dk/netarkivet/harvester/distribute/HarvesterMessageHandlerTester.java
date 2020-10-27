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

package dk.netarkivet.harvester.distribute;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.jms.JMSException;
import javax.jms.ObjectMessage;

import org.junit.Test;

import dk.netarkivet.common.exceptions.PermissionDenied;
import dk.netarkivet.harvester.harvesting.distribute.CrawlStatusMessage;
import dk.netarkivet.harvester.harvesting.distribute.DoOneCrawlMessage;
import dk.netarkivet.harvester.indexserver.distribute.IndexRequestMessage;

@SuppressWarnings("unused")
public class HarvesterMessageHandlerTester {
    private HarvesterMessageHandler harvesterMessageHandlerUT = new TestMessageHandler();

    @Test
    public final void testOnMessage() throws JMSException {
        ObjectMessage jmsMessage = mock(ObjectMessage.class);
        HarvesterMessage harvesterMessage = mock(HarvesterMessage.class);
        when(jmsMessage.getObject()).thenReturn(harvesterMessage);
        harvesterMessageHandlerUT.onMessage(jmsMessage);
        verify(harvesterMessage).accept(any(HarvesterMessageVisitor.class));
    }

    /*
     * Class under test for void visit(CrawlStatusMessage)
     */
    @Test
    public final void testVisitCrawlStatusMessage() {
        try {
            harvesterMessageHandlerUT.visit(mock(CrawlStatusMessage.class));
            fail("Should have thrown a permission denied.");
        } catch (PermissionDenied e) {
        }
    }

    /*
     * Class under test for void visit(DoOneCrawlMessage)
     */
    @Test
    public final void testVisitDoOneCrawlMessage() {
        try {
            harvesterMessageHandlerUT.visit(mock(DoOneCrawlMessage.class));
            fail("Should have thrown a permission denied.");
        } catch (PermissionDenied e) {
        }
    }

    /*
     * Class under test for void visit(IndexRequestMessage)
     */
    @Test
    public final void testVisitIndexRequestMessage() {
        try {
            harvesterMessageHandlerUT.visit(mock(IndexRequestMessage.class));
            fail("Should have thrown a permission denied.");
        } catch (PermissionDenied e) {
        }
    }

    private static class TestMessageHandler extends HarvesterMessageHandler {
        public TestMessageHandler() {
        }
    }
}
