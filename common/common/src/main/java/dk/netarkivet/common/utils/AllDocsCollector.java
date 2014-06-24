package dk.netarkivet.common.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.index.AtomicReaderContext;

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
