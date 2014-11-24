package dk.netarkivet.externalsoftware;

import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.deciderules.DecideRuleSequence;
import org.archive.crawler.deciderules.DecidingScope;
import org.archive.crawler.deciderules.MatchesListRegExpDecideRule;
import org.archive.crawler.framework.CrawlController;
import org.junit.Assert;
import org.junit.Test;

import dk.netarkivet.harvester.harvesting.ContentSizeAnnotationPostProcessor;
import dk.netarkivet.harvester.harvesting.report.Heritrix1Constants;

public class Heritrix1ConstantsTester {

	@Test
	public void test_heritrix1constants() {
	    Assert.assertEquals(ContentSizeAnnotationPostProcessor.CONTENT_SIZE_ANNOTATION_PREFIX, Heritrix1Constants.CONTENT_SIZE_ANNOTATION_PREFIX);
	    Assert.assertEquals(CrawlURI.S_BLOCKED_BY_QUOTA, Heritrix1Constants.CRAWLURI_S_BLOCKED_BY_QUOTA);
	    Assert.assertEquals(DecideRuleSequence.class.getName(), Heritrix1Constants.DECIDERULESEQUENCE_CLASSNAME);
	    Assert.assertEquals(DecidingScope.class.getName(), Heritrix1Constants.DECIDINGSCOPE_CLASSNAME);
	    Assert.assertEquals(MatchesListRegExpDecideRule.class.getName(), Heritrix1Constants.MATCHESLISTREGEXPDECIDERULE_CLASSNAME);
	    Assert.assertEquals(CrawlController.FINISHED, Heritrix1Constants.CRAWLCONTROLLER_FINISHED);
	}

}
