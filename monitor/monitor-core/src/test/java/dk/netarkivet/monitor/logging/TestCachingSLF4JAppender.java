package dk.netarkivet.monitor.logging;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import dk.netarkivet.common.exceptions.ArgumentNotValid;

public class TestCachingSLF4JAppender {

	@Test
	public void test_cachingslf4jappender() {
		LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();

        CachingSLF4JAppender appender = new CachingSLF4JAppender();

        String pattern = appender.getPattern();
        Assert.assertEquals(null, pattern);
        appender.setPattern("%date %level [%thread] %logger{10} [%file:%line] %msg%n");
        pattern = appender.getPattern();
        Assert.assertEquals("%date %level [%thread] %logger{10} [%file:%line] %msg%n", pattern);

        appender.setContext(lc);
        appender.start();

        Logger logger = (Logger) LoggerFactory.getLogger(TestCachingSLF4JAppender.class);
        logger.addAppender(appender);
        logger.setLevel(Level.DEBUG);
        logger.setAdditive(false); /* set to true if root should log too */

        try {
            appender.getNthLogRecord(-1);
            Assert.fail("Exception expected!");
        } catch (ArgumentNotValid e) {
        }

        try {
            appender.getNthLogRecord(appender.loggingHistorySize);
            Assert.fail("Exception expected!");
        } catch (ArgumentNotValid e) {
        }

        logger.trace("'1'");
        logger.debug("'2'");
        logger.info("'3'");
        logger.warn("'4'");
        logger.error("'5'");

        String str;

        str = appender.getNthLogRecord(0);
        Assert.assertNotEquals(-1, str.indexOf("'5'"));
        str = appender.getNthLogRecord(1);
        Assert.assertNotEquals(-1, str.indexOf("'4'"));
        str = appender.getNthLogRecord(2);
        Assert.assertNotEquals(-1, str.indexOf("'3'"));
        str = appender.getNthLogRecord(3);
        Assert.assertEquals(null, str);

        // Seems fairly illogical that this is the reverse of getNthLogRecord.
        // But getRecordString does a second getNthLogIndex...
        str = appender.loggingMBeans.get(nthLogIndex(appender, 0)).getRecordString();
        Assert.assertNotEquals(-1, str.indexOf("'3'"));
        str = appender.loggingMBeans.get(nthLogIndex(appender, 1)).getRecordString();
        Assert.assertNotEquals(-1, str.indexOf("'4'"));
        str = appender.loggingMBeans.get(nthLogIndex(appender, 2)).getRecordString();
        Assert.assertNotEquals(-1, str.indexOf("'5'"));
        str = appender.loggingMBeans.get(nthLogIndex(appender, 3)).getRecordString();
        Assert.assertEquals("", str);

        int number = 6;
        int mod = 0;
        for (int i=0; i<appender.loggingHistorySize*2; ++i) {
        	switch (mod) {
        	case 0:
                logger.info("'" + number + "'");
        		break;
        	case 1:
                logger.warn("'" + number + "'");
        		break;
        	case 2:
                logger.error("'" + number + "'");
        		break;
        	}
        	mod = (mod + 1) % 3;
        	++number;
        }

        CachingSLF4JLogRecord logRecord;

        for (int i=0; i<appender.loggingHistorySize; ++i) {
        	--number;
        	str = appender.getNthLogRecord(i);
        	Assert.assertNotEquals(-1, str.indexOf("'" + number + "'"));

        	logRecord = appender.loggingMBeans.get(nthLogIndex(appender, 0));
            str = logRecord.getRecordString();
            Assert.assertNotEquals(-1, str.indexOf('5'));
            logRecord.close();
        }

        appender.stop();
        appender.close();
        appender.close();
	}

	private int nthLogIndex(CachingSLF4JAppender appender, int n) {
		return (appender.currentIndex - n - 1 + appender.loggingHistorySize) % appender.loggingHistorySize;
	}

}
