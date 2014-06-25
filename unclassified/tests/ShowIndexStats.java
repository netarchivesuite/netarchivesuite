
import java.io.File;
import java.io.IOException;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.FSDirectory;

public class ShowIndexStats {

    /**
     * @param args The full path of the index
     * @throws IOException 
     */
    public static void main(String[] args) throws IOException {
        String path = null;
        if (args.length != 1) {
            System.err.println("Missing path to the index");
            System.exit(1);
        } else {
            path = args[0];
        }
        FSDirectory luceneDir = FSDirectory.open(new File(path));
        IndexReader r = IndexReader.open(luceneDir);
        System.out.println("Number of docs in index: " +  r.numDocs());
        //System.out.println("Version: " + r.getVersion());
        luceneDir.close();
        r.close();
    }

}