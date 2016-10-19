
logfilePrefix = "scripting_events"   // A logfile will be created with this prefix + ".log"
initials = "ABC"   // Curator initials to be added to log-messages

// To use, just remove the initial "//" from any one of these lines.
//
//killToeThread  1       //Kill a toe thread by number
//listFrontier '.*stats.*'    //List uris in the frontier matching a given regexp
//deleteFromFrontier '.*foobar.*'    //Remove uris matching a given regexp from the frontier
//printCrawlLog '.*'          //View already crawled lines uris matching a given regexp

import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.OperationStatus

import java.nio.file.Files
import java.util.function.Consumer
import java.util.function.Predicate
import java.util.logging.FileHandler
import java.util.logging.Logger;

void killToeThread(int thread) {
    job.crawlController.requestCrawlPause();
    job.crawlController.killThread(thread, true);
    logEvent("Killed Toe Thread number " + thread + ".")
    rawOut.println "WARNING: This job and heritrix may now need to be manually terminated when it is finished harvesting."
    rawOut.println "REMINDER: This job is now in a Paused state."
}

/**
 * Utility method to find and return the logger for a given log-prefix, or initialise it
 * if it doesn't already exist.
 * @return
 */
Logger getLogger() {
    for (Map.Entry<Logger,FileHandler> entry: job.crawlController.loggerModule.fileHandlers ) {
        if (entry.key.name.contains(logfilePrefix)) {
            return entry.key
        }
    }
    return job.crawlController.loggerModule.setupSimpleLog(logfilePrefix);
}

void logEvent(String e) {
    getLogger().info("Action from user " + initials + ": " +e)
}

void deleteFromFrontier(String regex) {
    job.crawlController.requestCrawlPause()
    count = job.crawlController.frontier.deleteURIs(".*", regex)
    rawOut.println "REMINDER: This job is now in a Paused state."
    logEvent("Deleted " + count + " uris matching regex '" + regex + "'")
    rawOut.println count + " uris deleted from frontier."
    rawOut.println("This action has been logged in " + logfilePrefix + ".log")
}

void listFrontier(String regex) {
    style = 'overflow: auto; word-wrap: normal; white-space: pre; width:1200px; height:500px'
    htmlOut.println '<pre style="' + style +'">'
    pattern = ~regex
    //type  org.archive.crawler.frontier.BdbMultipleWorkQueues
    pendingUris = job.crawlController.frontier.pendingUris
    //iterates over the raw underlying instance of com.sleepycat.je.Database
    cursor = pendingUris.pendingUrisDB.openCursor(null, null);
    key = new DatabaseEntry();
    value = new DatabaseEntry();
    matchingCount = 0
    try {
        while (cursor.getNext(key, value, null) == OperationStatus.SUCCESS) {
            if (value.getData().length == 0) {
                continue;
            }
            curi = pendingUris.crawlUriBinding.entryToObject(value);
            if (pattern.matcher(curi.toString())) {
                htmlOut.println '<span style="font-size:small;">' + curi + '</span>'
                matchingCount++
            }
        }
    } finally {
        cursor.close();
    }
    htmlOut.println '</pre>'
    htmlOut.println '<p>'+ matchingCount + " matching uris found </p>"
}

class patternMatchingPredicate implements Predicate<String>
    {
        private java.util.regex.Pattern p;
        public patternMatchingPredicate(java.util.regex.Pattern p) {this.p=p;}
        boolean test(String s) {return s.matches(p);}
    }


class PrintConsumer implements Consumer<String>
{
    private PrintWriter out;
    public PrintConsumer(PrintWriter out){this.out=out;}
    void accept(String s) {out.println("<span style=\"font-size:small\">" + s + "</span>");}
}


void printCrawlLog(String regex) {
    style = 'overflow: auto; word-wrap: normal; white-space: pre; width:1200px; height:500px'
    htmlOut.println '<pre style="' + style +'">'
    namePredicate = new patternMatchingPredicate(~regex);
    crawlLogFile = job.crawlController.frontier.loggerModule.crawlLogPath.file
    matchingCount =  Files.lines(crawlLogFile.toPath()).filter(namePredicate).peek(new PrintConsumer(htmlOut)).count()
    htmlOut.println '</pre>'
    htmlOut.println '<p>'+ matchingCount + " matching lines found </p>"
}