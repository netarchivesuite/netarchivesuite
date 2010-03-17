/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2010 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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

package dk.netarkivet.common.webinterface;

import javax.servlet.jsp.JspWriter;
import java.io.IOException;
import java.io.StringWriter;

/** JSP writer that simply writes to a public string writer. */
public class JspWriterMockup extends JspWriter {
    public StringWriter sw = new StringWriter();

    protected JspWriterMockup() {
        super(Integer.MAX_VALUE, false);
    }

    public void newLine() throws IOException {
        sw.append('\n');
    }

    public void print(boolean b) throws IOException {
        sw.append(Boolean.toString(b));
    }

    public void print(char c) throws IOException {
        sw.append(Character.toString(c));
    }

    public void print(int i) throws IOException {
        sw.append(Integer.toString(i));
    }

    public void print(long l) throws IOException {
        sw.append(Long.toString(l));
    }

    public void print(float v) throws IOException {
        sw.append(Float.toString(v));
    }

    public void print(double v) throws IOException {
        sw.append(Double.toString(v));
    }

    public void print(char[] chars) throws IOException {
        sw.append(new String(chars));
    }

    public void print(String string) throws IOException {
        sw.append(string);
    }

    public void print(Object object) throws IOException {
        sw.append(object.toString());
    }

    public void println() throws IOException {
        sw.append('\n');
    }

    public void println(boolean b) throws IOException {
        print(b);
        println();
    }

    public void println(char c) throws IOException {
        print(c);
        println();
    }

    public void println(int i) throws IOException {
        print(i);
        println();
    }

    public void println(long l) throws IOException {
        print(l);
        println();
    }

    public void println(float v) throws IOException {
        print(v);
        println();
    }

    public void println(double v) throws IOException {
        print(v);
        println();
    }

    public void println(char[] chars) throws IOException {
        print(chars);
        println();
    }

    public void println(String string) throws IOException {
        print(string);
        println();
    }

    public void println(Object object) throws IOException {
        print(object);
        println();
    }

    public void clear() throws IOException {
        sw = new StringWriter();
    }

    public void clearBuffer() throws IOException {
        sw = new StringWriter();
    }

    public void write(char cbuf[], int off, int len) throws IOException {
        sw.append(new String(cbuf, off, len));
    }

    public void flush() throws IOException {
        sw.flush();
    }

    public void close() throws IOException {
        sw.close();
    }

    public int getRemaining() {
        return Integer.MAX_VALUE;
    }
}
