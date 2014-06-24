 package dk.netarkivet.harvester.indexserver;

import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 * Encapsulates the options for the indexing process.
 *
 */
 public class DigestOptions {

     /** the useBlacklist set to true results in docs matching the
         mimefilter being ignored. 
     */
     private final boolean useBlacklist;
 
     /** An regular expression for the mimetypes to include or exclude from
      * the index. According to the useBlacklist setting.
      */
     private final String mimeFilter;
     
     /** Avoid logging to STDOUT when indexing. */
     private final boolean verbose;
     
     /**
      * Set the needed options used by the DigestIndexer.
      * @param useMimefilterAsBlacklist Are we using the mimeFilter as a black
      * or a whitelist.
      * @param verboseIndexing print logging to stdout while indexing, or not.
      * @param theMimeFilter The given black or whitelist according to mimetype.
      */
     public DigestOptions(boolean useMimefilterAsBlacklist, 
             boolean verboseIndexing, String theMimeFilter) {
         ArgumentNotValid.checkNotNullOrEmpty(
                 theMimeFilter, "String theMimeFilter");
         this.useBlacklist = useMimefilterAsBlacklist;
         this.mimeFilter = theMimeFilter;
         this.verbose = verboseIndexing;
     }
     
     /**
      * 
      * @return true if we use the mimefilter as a blacklist; false otherwise
      */
     public boolean getUseBlacklist() {
         return this.useBlacklist;
     }

     /**
      * 
      * @return true, if we are verbose; otherwise false
      */
     public boolean getVerboseMode() {
         return this.verbose;
     }

     /**
      * 
      * @return the mimefilter 
      */
     public String getMimeFilter() {
         return this.mimeFilter;
     }
     
}
