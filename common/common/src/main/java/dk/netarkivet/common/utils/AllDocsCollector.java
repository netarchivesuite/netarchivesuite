/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2012 The Royal Danish Library, the Danish State and
 * University Library, the National Library of France and the Austrian
 * National Library.
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
package dk.netarkivet.common.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Scorer;

/** Simple Collector to collect all results from Lucene query. */
public class AllDocsCollector extends Collector { 
    private List<ScoreDoc> docs = new ArrayList<ScoreDoc>(); 
    private Scorer scorer; 
    private int docBase;
    @Override
    public boolean acceptsDocsOutOfOrder() {
        return true; 
    } 
    @Override
    public void setScorer(Scorer scorer) { 
        this.scorer = scorer; 
    } 
//    @Override
//    public void setNextReader(IndexReader reader, int docBase) { 
//        this.docBase = docBase; 
//    }
    @Override
    public void collect(int doc) throws IOException { 
        docs.add(
                new ScoreDoc(doc + docBase, scorer.score()));
    }

    public List<ScoreDoc> getHits() { 
        return docs; 
    }
    
    public void reset() { 
        docs.clear(); 
    }
    @Override
    public void setNextReader(AtomicReaderContext arg0) throws IOException {
        this.docBase = arg0.docBase;
        
    } 
}
