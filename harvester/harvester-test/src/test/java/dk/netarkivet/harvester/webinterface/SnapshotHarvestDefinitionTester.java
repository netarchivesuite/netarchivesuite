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
package dk.netarkivet.harvester.webinterface;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import javax.inject.Provider;
import javax.servlet.ServletRequest;
import javax.servlet.jsp.PageContext;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import dk.netarkivet.common.exceptions.ForwardedToErrorPage;
import dk.netarkivet.common.utils.I18n;
import dk.netarkivet.harvester.datamodel.DomainDAO;
import dk.netarkivet.harvester.datamodel.FullHarvest;
import dk.netarkivet.harvester.datamodel.HarvestDefinition;
import dk.netarkivet.harvester.datamodel.HarvestDefinitionDAO;
import dk.netarkivet.harvester.datamodel.JobDAO;
import dk.netarkivet.harvester.datamodel.eav.EAV;
import dk.netarkivet.harvester.datamodel.extendedfield.ExtendedFieldDAO;
import dk.netarkivet.testutils.StringAsserts;

/** Unit-test for the SnapshotHarvestDefinition class. */
public class SnapshotHarvestDefinitionTester {

    private HarvestDefinitionDAO harvestDefinitionDAOMock = mock(HarvestDefinitionDAO.class);
    // java 8 required
    //private Provider<HarvestDefinitionDAO> harvestDefinitionDAOProvider = () -> harvestDefinitionDAOMock;
    private Provider<HarvestDefinitionDAO> harvestDefinitionDAOProvider = new Provider<HarvestDefinitionDAO>() {
        @Override
        public HarvestDefinitionDAO get() {
            return harvestDefinitionDAOMock;
        }
    };

    private JobDAO jobDaoMock = mock(JobDAO.class);
    // java 8 required
    //private Provider<JobDAO> jobDAOProvider = () -> jobDaoMock;
    private Provider<JobDAO> jobDAOProvider = new Provider<JobDAO>() {
		@Override
		public JobDAO get() { return jobDaoMock;}
	};

    private DomainDAO domainDAOMock = mock(DomainDAO.class);
    // java 8 required
    //private Provider<DomainDAO> domainDAOProvider = () -> domainDAOMock; JAVA 8 syntax required
    private Provider<DomainDAO> domainDAOProvider = new Provider<DomainDAO>() {
        @Override
        public DomainDAO get() {
            return domainDAOMock;
        }
    };

    private ExtendedFieldDAO extendedFieldMock = mock(ExtendedFieldDAO.class);
    // java 8 required
    //private Provider<ExtendedFieldDAO> extendedFieldDAOProvider = () -> extendedFieldMock; JAVA 8 required
    private Provider<ExtendedFieldDAO> extendedFieldDAOProvider = new Provider<ExtendedFieldDAO>() {
        @Override
        public ExtendedFieldDAO get() {
            return extendedFieldMock;
        }
    };

    private EAV eavDAOMock = mock(EAV.class);
    // java 8 required
    //private Provider<EAV> eavDAOProvider = () -> eavDAOMock;
    private  Provider<EAV> eavDAOProvider = new Provider<EAV>() {
    	@Override
    	public EAV get() {
    		return eavDAOMock;
    	}
    };

    private SnapshotHarvestDefinition snapshotHarvestDefinition = new SnapshotHarvestDefinition(
            harvestDefinitionDAOProvider, jobDAOProvider, extendedFieldDAOProvider, domainDAOProvider, eavDAOProvider);

    @Test
    public void testProcessRequest() throws Exception {
        ServletRequest requestStub = mock(ServletRequest.class);
        String newHDname = "fnord";
        I18n I18N = new I18n(dk.netarkivet.common.Constants.TRANSLATIONS_BUNDLE);
        PageContext pageContext = new HarvesterWebinterfaceTestCase.TestPageContext(requestStub);

        snapshotHarvestDefinition.processRequest(pageContext, I18N);
        verifyZeroInteractions(harvestDefinitionDAOMock, jobDaoMock);

        when(requestStub.getParameter(Constants.UPDATE_PARAM)).thenReturn("yes");
        when(requestStub.getParameter(Constants.DOMAIN_PARAM)).thenReturn(newHDname);
        when(requestStub.getParameter(Constants.CREATENEW_PARAM)).thenReturn("yes");
        when(requestStub.getParameter(Constants.DOMAIN_OBJECTLIMIT_PARAM)).thenReturn("-1");
        when(requestStub.getParameter(Constants.DOMAIN_BYTELIMIT_PARAM)).thenReturn("117");
        when(requestStub.getParameter(Constants.HARVEST_PARAM)).thenReturn(newHDname);
        when(requestStub.getParameter(Constants.COMMENTS_PARAM)).thenReturn("You did not see this");
        snapshotHarvestDefinition.processRequest(pageContext, I18N);

        ArgumentCaptor<HarvestDefinition> hdCapture = ArgumentCaptor.forClass(HarvestDefinition.class);
        verify(harvestDefinitionDAOMock).create(hdCapture.capture());
        FullHarvest newHD = (FullHarvest) hdCapture.getValue();

        assertNotNull("Should have fnord after creation", newHD);
        assertEquals("Should have right name", newHDname, newHD.getName());
        assertEquals("Should have right bytelimit", 117, newHD.getMaxBytes());
        assertEquals("Should have right comments", "You did not see this", newHD.getComments());
        assertNull("Old harvest id should be null", newHD.getPreviousHarvestDefinition());
        assertFalse("Should initially be inactive", newHD.getActive());
    }

    @Test
    public void testExistingHarvestDefinition() {
        ServletRequest requestStub = mock(ServletRequest.class);
        String newHDname = "fnord";
        I18n I18N = new I18n(dk.netarkivet.common.Constants.TRANSLATIONS_BUNDLE);
        PageContext pageContext = new HarvesterWebinterfaceTestCase.TestPageContext(requestStub);
        when(requestStub.getParameter(Constants.UPDATE_PARAM)).thenReturn("yes");
        when(requestStub.getParameter(Constants.CREATENEW_PARAM)).thenReturn("yes");
        when(requestStub.getParameter(Constants.HARVEST_PARAM)).thenReturn(newHDname);
        when(harvestDefinitionDAOMock.getHarvestDefinition(newHDname)).thenReturn(mock(HarvestDefinition.class));
        try {
            snapshotHarvestDefinition.processRequest(pageContext, I18N);
            fail("Should complain about existing harvest definition");
        } catch (ForwardedToErrorPage e) {
            assertEquals("Harvest definition '" + newHDname + "' already exists", e.getMessage());
        }
    }

    @Test
    public void testNoHarvestParamInRequest() throws Exception {
        String newHDname = "fnord";
        ServletRequest requestStub = mock(ServletRequest.class);
        I18n I18N = new I18n(dk.netarkivet.common.Constants.TRANSLATIONS_BUNDLE);
        PageContext pageContext = new HarvesterWebinterfaceTestCase.TestPageContext(requestStub);
        when(requestStub.getParameter(Constants.UPDATE_PARAM)).thenReturn("yes");
        when(requestStub.getParameter(Constants.DOMAIN_PARAM)).thenReturn(newHDname);
        when(requestStub.getParameter(Constants.CREATENEW_PARAM)).thenReturn("yes");
        when(requestStub.getParameter(Constants.DOMAIN_OBJECTLIMIT_PARAM)).thenReturn("-1");
        when(requestStub.getParameter(Constants.DOMAIN_BYTELIMIT_PARAM)).thenReturn("117");
        when(requestStub.getParameter(Constants.COMMENTS_PARAM)).thenReturn("You did not see this");
        try {
            snapshotHarvestDefinition.processRequest(pageContext, I18N);
            fail("Should complain about missing " + Constants.HARVEST_PARAM);
        } catch (ForwardedToErrorPage e) {
            StringAsserts.assertStringContains("Should mention " + Constants.HARVEST_PARAM + " in msg",
                    Constants.HARVEST_PARAM, e.getMessage());
        }
    }
}
