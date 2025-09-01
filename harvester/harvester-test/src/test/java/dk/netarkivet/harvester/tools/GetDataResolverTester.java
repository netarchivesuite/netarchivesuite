/*
 * #%L
 * Netarchivesuite - harvester - test
 * %%
 * Copyright (C) 2005 - 2018 The Royal Danish Library,
 * the National Library of France and the Austrian National Library.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program. If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */
package dk.netarkivet.harvester.tools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.arcrepository.TestArcRepositoryClient;
import dk.netarkivet.common.distribute.arcrepository.ArcRepositoryClient;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.testutils.StringAsserts;
import dk.netarkivet.testutils.preconfigured.MoveTestFiles;

/**
 * Unit-tests for the GetDataResolver class.
 */
public class GetDataResolverTester {

	private static ByteArrayOutputStream bos;
	private static int status = -1;

	MoveTestFiles mtf = new MoveTestFiles(TestInfo.METADATA_DIR, TestInfo.WORKING_DIR);
	File tempdir = new File(TestInfo.WORKING_DIR, "commontempdir");
	ArcRepositoryClient arcrep = null;

	@Before
	public void setUp() {
		mtf.setUp();
		tempdir.mkdir();
		Settings.set(CommonSettings.DIR_COMMONTEMPDIR, tempdir.getAbsolutePath());

		arcrep = new TestArcRepositoryClient(TestInfo.WORKING_DIR);
	}

	@After
	public void tearDown() {
		arcrep.close();
		mtf.tearDown();
	}

	@Test
	public void testExecuteCommand() throws Exception {
		String url = "/History/Harveststatus-download-report-template.jsp";
		GetDataResolver.setClient(arcrep);

		// Get File KO
		File testFile = new File(TestInfo.WORKING_DIR, "dummyNotFound");
		Map<String, String> parameters = new HashMap<>();
		parameters.put(GetDataResolver.COMMAND_PARAMETER, GetDataResolver.GET_FILE_COMMAND);
		parameters.put(GetDataResolver.ARCFILE_PARAMETER, testFile.getName());
		HttpServletRequest request = makeHttpServletRequest(url, parameters);
		HttpServletResponse responseMock = makeHttpServletResponse();
		try {
			GetDataResolver.executeCommand(request, responseMock);
			fail("Should get exception on missing file");
		} catch (IOFailure e) {
			StringAsserts.assertStringContains("Should have file name in error", testFile.getName(), e.getMessage());
		}

		// Get record KO
		testFile = new File(TestInfo.WORKING_DIR, "fyensdk.arc");
		parameters = new HashMap<>();
		parameters.put(GetDataResolver.COMMAND_PARAMETER, GetDataResolver.GET_RECORD_COMMAND);
		parameters.put(GetDataResolver.ARCFILE_PARAMETER, testFile.getName());
		parameters.put(GetDataResolver.FILE_OFFSET_PARAMETER, "104");
		parameters.put(GetDataResolver.FILENAME_PARAMETER, "xxx.xml");
		request = makeHttpServletRequest(url, parameters);
		responseMock = makeHttpServletResponse();

		try {
			GetDataResolver.executeCommand(request, responseMock);
			fail("Should get exception on missing record");
		} catch (IOFailure e) {
			StringAsserts.assertStringContains("Should have file name in error", testFile.getName(), e.getMessage());
		}

		// Get record OK
		parameters = new HashMap<>();
		parameters.put(GetDataResolver.COMMAND_PARAMETER, GetDataResolver.GET_RECORD_COMMAND);
		parameters.put(GetDataResolver.ARCFILE_PARAMETER, testFile.getName());
		parameters.put(GetDataResolver.FILE_OFFSET_PARAMETER, "136");
		parameters.put(GetDataResolver.FILENAME_PARAMETER, "xxx.jpeg");
		request = makeHttpServletRequest(url, parameters);
		responseMock = makeHttpServletResponse();

		GetDataResolver.executeCommand(request, responseMock);
		assertEquals("Should have 200 response for second record", 200, responseMock.getStatus());
		OutputStream out = responseMock.getOutputStream();
		String resultText = out.toString();
		assertTrue("Should have start of record in response, not " + resultText, resultText.startsWith("HTTP/1.1 200 OK"));
		assertEquals("Should have right length of data in response", 6669, bos.size());

	}

	private HttpServletRequest makeHttpServletRequest(final String requestUrl, final Map<String, String> parameters) {
		HttpServletRequest requestStub = mock(HttpServletRequest.class);

		// Mock getRequestURL()
		when(requestStub.getRequestURL()).thenReturn(new StringBuffer(requestUrl));

		// Create a map to hold the parameter values as String[]
		Map<String, String[]> parameterMap = new HashMap<>();
		for (Map.Entry<String, String> entry : parameters.entrySet()) {
			parameterMap.put(entry.getKey(), new String[] { entry.getValue() });
		}
		// Mock the getParameterMap() method to return the created map
		when(requestStub.getParameterMap()).thenReturn(parameterMap);

		return requestStub;
	}

	private HttpServletResponse makeHttpServletResponse() throws IOException {
		HttpServletResponse responseStub = mock(HttpServletResponse.class);

		// Mock getOutputStream()
		bos = new ByteArrayOutputStream();
		ServletOutputStream sos = new ServletOutputStream() {
			@Override
			public void write(int b) throws IOException {
				bos.write(b);
			}

			@Override
			public void write(byte[] b) throws IOException {
				bos.write(b);
			}

			@Override
			public void write(byte[] b, int off, int len) throws IOException {
				bos.write(b, off, len);
			}

			@Override
			public boolean isReady() {
				return false;
			}

			@Override
			public void setWriteListener(WriteListener listener) {
				throw new RuntimeException("Not yet implemented");
			}

			@Override
			public String toString() {
				return bos.toString();
			}
		};
		when(responseStub.getOutputStream()).thenReturn(sos);

		// Mock getStatus()
		doAnswer(inv -> {
			status = (int) inv.getArgument(0);
			return null;
		}).when(responseStub).setStatus(Mockito.anyInt());
		doAnswer(inv -> status).when(responseStub).getStatus();
		return responseStub;
	}

}
