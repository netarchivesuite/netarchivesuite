logfilePrefix = "scripting_events"   // A logfile will be created with this prefix + ".log"
//initials = "ABC"   // Curator initials to be added to log-messages

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
 * Utility method to find and return the logger for a given log-prefix, or initialise it if it doesn't already exist.
 * @return logger
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

/* write some lines in a file, in a directory with an extension */
void writeToFile(def directory, def fileName, def extension, def infoList) {
    String dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'" 
    new File("$directory/$fileName$extension").withWriterAppend { out ->
        infoList.each {
            out.println new Date().format(dateFormat) + " " + it
        }
    }
}

/* to log some lines in a changelog.txt file (will be in the metadata.warc */
void logToChangeLogFile(def logLines) {
	writeToFile(job.jobDir.absolutePath, "changelog", ".txt", logLines)
}

void deleteFromFrontier(String regex) {
    job.crawlController.requestCrawlPause()
    count = job.crawlController.frontier.deleteURIs(".*", regex)
    rawOut.println "REMINDER: This job is now in a Paused state."
    logEvent("Deleted " + count + " uris matching regex '" + regex + "'")
    rawOut.println count + " uris deleted from frontier."
    rawOut.println("This action has been logged in " + logfilePrefix + ".log")
}

void listFrontier(String regex, long limit) {
    //style = 'overflow: auto; word-wrap: normal; white-space: pre; width:1200px; height:500px'
    //htmlOut.println '<pre style="' + style +'">'
    htmlOut.println '<pre>'
    pattern = ~regex
    //type  org.archive.crawler.frontier.BdbMultipleWorkQueues
    pendingUris = job.crawlController.frontier.pendingUris
    htmlOut.println 'queue items: ' + pendingUris.pendingUrisDB.count()
    //iterates over the raw underlying instance of com.sleepycat.je.Database
    cursor = pendingUris.pendingUrisDB.openCursor(null, null);
    key = new DatabaseEntry();
    value = new DatabaseEntry();
    matchingCount = 0
    try {
        while (cursor.getNext(key, value, null) == OperationStatus.SUCCESS && limit > 0) {
            if (value.getData().length == 0) {
                continue;
            }
            curi = pendingUris.crawlUriBinding.entryToObject(value);
            if (pattern.matcher(curi.toString())) {
                //htmlOut.println '<span style="font-size:small;">' + curi + '</span>'
                htmlOut.println curi
                matchingCount++
                --limit;
            }
        }
    } finally {
        cursor.close();
    }
    htmlOut.println '</pre>'
    if (limit > 0) {
        htmlOut.println '<p>'+ matchingCount + " matching uris found </p>"
    } else {
        htmlOut.println '<p>The first ' + matchingCount + " matching uris found. (Return limit reached)</p>"
    }
}

void pageFrontier(long skip, int items) {
    htmlOut.println '<pre>'
    //pattern = ~regex
    //type  org.archive.crawler.frontier.BdbMultipleWorkQueues
    pendingUris = job.crawlController.frontier.pendingUris
    //iterates over the raw underlying instance of com.sleepycat.je.Database
    cursor = pendingUris.pendingUrisDB.openCursor(null, null);
    key = new DatabaseEntry();
    value = new DatabaseEntry();
    cursor.skipNext(skip, key, value, null)
    matchingCount = 0
    try {
        while (cursor.getNext(key, value, null) == OperationStatus.SUCCESS) {
            if (value.getData().length == 0) {
                continue;
            }
            curi = pendingUris.crawlUriBinding.entryToObject(value);
            htmlOut.println '<span style="font-size:small;">' + curi + '</span>'
            /*
            if (pattern.matcher(curi.toString())) {
                htmlOut.println '<span style="font-size:small;">' + curi + '</span>'
                matchingCount++
            }
            */
        }
    } finally {
        cursor.close();
    }
    htmlOut.println '</pre>'
    htmlOut.println '<p>'+ matchingCount + " matching uris found </p>"
}

class patternMatchingPredicate implements Predicate<String> {
    private java.util.regex.Pattern p;
    public patternMatchingPredicate(java.util.regex.Pattern p) {this.p=p;}
    boolean test(String s) {return s.matches(p);}
}

class PrintConsumer implements Consumer<String> {
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

void showBudget() {
	style = 'overflow: auto; word-wrap: normal; white-space: pre; width:1200px; height:500px'
    htmlOut.println '<pre style="' + style +'">'
    frontier = appCtx.getBean("frontier")
	frontier.reconsiderRetiredQueues()
	htmlOut.println 'queueTotalBudget = '+frontier.queueTotalBudget
    htmlOut.println '</pre>'
}

void changeBudget(int newBudget) {
    frontier = appCtx.getBean("frontier")
	frontier.reconsiderRetiredQueues()
	frontier.queueTotalBudget = newBudget
	
	def txtFileInfo = []
	String a = "change budget : "+ newBudget
	txtFileInfo << a
	logToChangeLogFile(txtFileInfo)
	
	showBudget()
}

void showFilters() {
	regexRuleObj = appCtx.getBean("scope").rules.find{ it.class == org.archive.modules.deciderules.MatchesListRegexDecideRule }
	style = 'overflow: auto; word-wrap: normal; white-space: pre; width:1200px; height:500px'
	htmlOut.println '<pre style="' + style +'">'
	htmlOut.println("all DecideResult.REJECT filters: "+ regexRuleObj.regexList)
	htmlOut.println("manually added DecideResult.REJECT filters : "+job.jobContext.data.get("manual-add-reject-filters"))
	htmlOut.println '</pre>'
}

void addFilter(String pat) {
	regexRuleObj = appCtx.getBean("scope").rules.find{ it.class == org.archive.modules.deciderules.MatchesListRegexDecideRule }
	regexRuleObj.regexList.add(pat)
	def filters = job.jobContext.data.get("manual-add-reject-filters");
	if(filters == null) {
		filters = [pat]
		job.jobContext.data.put("manual-add-reject-filters", filters)
	} else {
		filters = filters + pat
		job.jobContext.data.put("manual-add-reject-filters", filters)
	}
	
	def txtFileInfo = []
	String a = "manual add of a DecideResult.REJECT filter : "+ pat
	txtFileInfo << a
	logToChangeLogFile(txtFileInfo)
	
	showFilters()
}
