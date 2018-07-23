/*
 * #%L
 * Netarchivesuite - common - test
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
package dk.netarkivet.common.utils.batch;

import java.io.IOException;
import java.io.OutputStream;

import javax.annotation.Resource;
import javax.annotation.Resources;

import org.archive.io.arc.ARCRecord;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.arc.ARCBatchJob;

/**
 * The batchjob checks each record whether it has a specific URL and/or specific mimetype (both in the shape of a
 * regular expression). The URLs of the digital objects which matches these constrains are returned.
 */
@SuppressWarnings({"serial", "unused"})
@Resources(value = {
        @Resource(name = "regex", description = "The regular expression for the " + "urls.",
                type = java.lang.String.class),
        @Resource(name = "mimetype", type = java.lang.String.class),
        @Resource(description = "Batchjob for finding URLs which matches a given"
                + " regular expression and has a mimetype which matches another" + " regular expression.",
                type = dk.netarkivet.common.utils.batch.UrlSearch.class)})
public class UrlSearch extends ARCBatchJob {
    private String regex;
    private String mimetype;
    private long urlCount = 0L;
    private long mimeCount = 0L;
    private long totalCount = 0L;
    private long bothCount = 0L;

    public UrlSearch(String arg1, String regex, String mimetype) throws ArgumentNotValid {
        ArgumentNotValid.checkNotNull(regex, "String regex");
        ArgumentNotValid.checkNotNull(mimetype, "String mimetype");
        this.regex = regex;
        this.mimetype = mimetype;
    }

    @Override
    public void finish(OutputStream os) {
        // TODO Auto-generated method stub

        try {
            os.write("\nResults:\n".getBytes());
            os.write(("Urls matched = " + urlCount + "\n").getBytes());
            os.write(("Mimetypes matched = " + mimeCount + "\n").getBytes());
            os.write(("Url and Mimetype matches = " + bothCount + "\n").getBytes());
        } catch (IOException e) {
            throw new IOFailure("Unexpected problem when writing to output " + "stream.", e);
        }
    }

    @Override
    public void initialize(OutputStream os) {
        // TODO Auto-generated method stub

    }

    @Override
    public void processRecord(ARCRecord record, OutputStream os) {
        totalCount++;
        boolean valid = true;
        if (record.getMetaData().getUrl().matches(regex)) {
            urlCount++;
        } else {
            valid = false;
        }
        if (record.getMetaData().getMimetype().matches(mimetype)) {
            mimeCount++;
        } else {
            valid = false;
        }

        if (valid) {
            bothCount++;
            try {
                os.write((record.getMetaData().getUrl() + " : " + record.getMetaData().getMimetype() + "\n").getBytes());
            } catch (IOException e) {
                // unexpected!
                throw new IOFailure("Cannot print to os!", e);
            }
        }
    }

}
