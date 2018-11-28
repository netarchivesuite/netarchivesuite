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
package dk.netarkivet.viewerproxy.distribute;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspWriter;

import org.junit.Test;

import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 * Unit-tests of the class HTTPControllerClient. Uses two dummy classes: An anonymous JspWriter, and a
 * MockHttpServletResponse.
 */
public class HTTPControllerClientTester {
    String returnUrl;
    HttpServletResponse responseMock = mock(HttpServletResponse.class);
    JspWriter jspWriterMock = mock(JspWriter.class);

    @Test
    public void testRedirectForSimpleCommand() throws IOException {
        returnUrl = null;
        HTTPControllerClient client = new HTTPControllerClient(responseMock, jspWriterMock, returnUrl);
        String simpleCommand = "fooBar";
        boolean useReturnURL = false;
        client.redirectForSimpleCommand(simpleCommand, useReturnURL);

        verify(responseMock).sendRedirect(addPrefix("fooBar"));
    }

    @Test
    public void testRedirectForRedirectWithoutReturnUrl() throws IOException {
        returnUrl = null;
        HTTPControllerClient client = new HTTPControllerClient(responseMock, jspWriterMock, returnUrl);
        String simpleCommand = "fooBar";
        boolean useReturnURL = true;
        try {
            client.redirectForSimpleCommand(simpleCommand, useReturnURL);
            fail("Should test that returnUrl is non-null");
        } catch (ArgumentNotValid e) {
        }
    }

    @Test
    public void testRedirectWithReturnUrl() throws IOException {
        returnUrl = "anUrl";
        HTTPControllerClient client = new HTTPControllerClient(responseMock, jspWriterMock, returnUrl);
        String command = "barfu";

        client.redirectForSimpleCommand(command, true);
        verify(responseMock).sendRedirect(addPrefix("barfu?returnURL=anUrl"));

        client.redirectForSimpleCommand(command, false);
        verify(responseMock).sendRedirect("http://netarchivesuite.viewerproxy.invalidbarfu?returnURL=anUrl");
    }

    private String addPrefix(String commandPostfix) {
        return "http://netarchivesuite.viewerproxy.invalid" + commandPostfix;
    }
}
