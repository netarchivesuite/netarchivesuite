/* $Id$
 * $Revision$
 * $Date$
 * $Author$
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
/*
 * Copyright 2004 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dk.netarkivet.common.utils;

import java.io.IOException;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.search.RangeFilter;

/**
 * Identical subclass of {@link RangeFilter} from Lucene 2.0.0, except the
 * bitset returned by {@link #bits(IndexReader)} is sparse.
 */
public class SparseRangeFilter extends RangeFilter {
    private String fieldName;
    private String lowerTerm;
    private String upperTerm;
    private boolean includeLower;
    private boolean includeUpper;

    /**
     * @see RangeFilter#RangeFilter(String, String, String, boolean, boolean)
     */
    public SparseRangeFilter(String fieldName, String lowerTerm,
                             String upperTerm, boolean includeLower,
                             boolean includeUpper) {
        super(fieldName, lowerTerm, upperTerm, includeLower, includeUpper);
        this.fieldName = fieldName;
        this.lowerTerm = lowerTerm;
        this.upperTerm = upperTerm;
        this.includeLower = includeLower;
        this.includeUpper = includeUpper;
    }

    /**
     * Identical to {@link RangeFilter#bits(IndexReader)}, except a SparseBitSet
     * is returned.
     * @see RangeFilter#bits(IndexReader)
     */
    public SparseBitSet bits(IndexReader reader) throws IOException {
        SparseBitSet bits = new SparseBitSet();
        //No changes to original Lucene code beyond this point.
        TermEnum enumerator =
            (null != lowerTerm
             ? reader.terms(new Term(fieldName, lowerTerm))
             : reader.terms(new Term(fieldName,"")));

        try {

            if (enumerator.term() == null) {
                return bits;
            }

            boolean checkLower = false;
            if (!includeLower) // make adjustments to set to exclusive
                checkLower = true;

            TermDocs termDocs = reader.termDocs();
            try {

                do {
                    Term term = enumerator.term();
                    if (term != null && term.field().equals(fieldName)) {
                        if (!checkLower || null==lowerTerm || term.text().compareTo(lowerTerm) > 0) {
                            checkLower = false;
                            if (upperTerm != null) {
                                int compare = upperTerm.compareTo(term.text());
                                /* if beyond the upper term, or is exclusive and
                                 * this is equal to the upper term, break out */
                                if ((compare < 0) ||
                                    (!includeUpper && compare==0)) {
                                    break;
                                }
                            }
                            /* we have a good term, find the docs */

                            termDocs.seek(enumerator.term());
                            while (termDocs.next()) {
                                bits.set(termDocs.doc());
                            }
                        }
                    } else {
                        break;
                    }
                }
                while (enumerator.next());

            } finally {
                termDocs.close();
            }
        } finally {
            enumerator.close();
        }

        return bits;
    }
}
