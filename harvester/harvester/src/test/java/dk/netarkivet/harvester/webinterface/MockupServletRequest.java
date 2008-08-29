/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2007 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package dk.netarkivet.harvester.webinterface;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.NotImplementedException;

/**
 * A ServletRequest used by unit tests.
 * It is only a partial implementation of ServletRequest.
 * Only implements the 8 methods:
 *  public void setAttribute(String string, Object object)
 *  public void removeAttribute(String string)
 *  public Object getAttribute(String string)
 *  public Enumeration getAttributeNames() 
 *  public String getParameter(String string)
 *  public Enumeration<String> getParameterNames()
 *  public String[] getParameterValues(String string)
 *  public Map<String, String[]> getParameterMap()
 *  
 */
class MockupServletRequest implements ServletRequest {
    Map<String, String[]> parameters = new HashMap<String, String[]>();
    Map<String, Object> attributes = new HashMap<String, Object>();

    // Utility methods:
    void addParameter(String key, String value) {
        String[] oldParameters = parameters.get(key);
        if (oldParameters != null) {
            String[] newParameters = new String[oldParameters.length + 1];
            System.arraycopy(oldParameters, 0, newParameters, 0,
                    oldParameters.length);
            newParameters[newParameters.length - 1] = value;
            parameters.put(key, newParameters);
        } else {
            parameters.put(key, new String[] { value } );
        }
    }

    void removeParameter(String key) {
        parameters.remove(key);
    }

    void clearParameters() {
        parameters.clear();
    }

    // Mocked-up methods:
    public Object getAttribute(String string) {
        return attributes.get(string);
    }

    public Enumeration getAttributeNames() {
        return null;
    }

    public String getCharacterEncoding() {
        //TODO: implement method
        throw new NotImplementedException("Not implemented");
    }

    public void setCharacterEncoding(String string)
            throws UnsupportedEncodingException {
        //TODO: implement method
        throw new NotImplementedException("Not implemented");
    }

    public int getContentLength() {
        //TODO: implement method
        throw new NotImplementedException("Not implemented");
    }

    public String getContentType() {
        //TODO: implement method
        throw new NotImplementedException("Not implemented");
    }

    public ServletInputStream getInputStream() throws IOException {
        //TODO: implement method
        throw new NotImplementedException("Not implemented");
    }

    public String getParameter(String string) {
        String[] values = parameters.get(string);
        if (values != null && values.length == 1) {
            return values[0];
        } else {
            if (values == null || values.length == 0) {
                return null;
            } else {
                throw new ArgumentNotValid("Not exactly one parameter value "
                        + "for parameter '" + string + "': " + values);
            }
        }
    }

    public Enumeration<String> getParameterNames() {
        return new Vector<String>(parameters.keySet()).elements();
    }

    public String[] getParameterValues(String string) {
        return parameters.get(string);
    }

    public Map<String, String[]> getParameterMap() {
        return parameters;
    }

    public String getProtocol() {
        //TODO: implement method
        throw new NotImplementedException("Not implemented");
    }

    public String getScheme() {
        //TODO: implement method
        throw new NotImplementedException("Not implemented");
    }

    public String getServerName() {
        //TODO: implement method
        throw new NotImplementedException("Not implemented");
    }

    public int getServerPort() {
        //TODO: implement method
        throw new NotImplementedException("Not implemented");
    }

    public BufferedReader getReader() throws IOException {
        //TODO: implement method
        throw new NotImplementedException("Not implemented");
    }

    public String getRemoteAddr() {
        //TODO: implement method
        throw new NotImplementedException("Not implemented");
    }

    public String getRemoteHost() {
        //TODO: implement method
        throw new NotImplementedException("Not implemented");
    }

    public void setAttribute(String string, Object object) {
        attributes.put(string, object);
    }

    public void removeAttribute(String string) {
        attributes.remove(string);
    }

    public Locale getLocale() {
        //TODO: implement method
        throw new NotImplementedException("Not implemented");
    }

    public Enumeration getLocales() {
        //TODO: implement method
        throw new NotImplementedException("Not implemented");
    }

    public boolean isSecure() {
        //TODO: implement method
        throw new NotImplementedException("Not implemented");
    }

    public RequestDispatcher getRequestDispatcher(String string) {
        //TODO: implement method
        throw new NotImplementedException("Not implemented");
    }

    public String getRealPath(String string) {
        //TODO: implement method
        throw new NotImplementedException("Not implemented");
    }

    public int getRemotePort() {
        //TODO: implement method
        throw new NotImplementedException("Not implemented");
    }

    public String getLocalName() {
        //TODO: implement method
        throw new NotImplementedException("Not implemented");
    }

    public String getLocalAddr() {
        //TODO: implement method
        throw new NotImplementedException("Not implemented");
    }

    public int getLocalPort() {
        //TODO: implement method
        throw new NotImplementedException("Not implemented");
    }
}
