package dk.netarkivet.harvester.harvesting;

import java.lang.reflect.Field;
import java.util.Arrays;

import org.apache.commons.httpclient.URIException;
import org.archive.modules.CrawlURI;
import org.archive.modules.SchedulingConstants;
import org.archive.modules.deciderules.surt.SurtPrefixedDecideRule;
import org.archive.net.UURI;
import org.archive.net.UURIFactory;
import org.archive.util.SurtPrefixSet;
import org.junit.Assert;
import org.junit.Test;

public class NASSurtPrefixedDecideRuleTester {

    @Test
	public void test_nas_surtprefixeddeciderule() {
		Object[][] cases;
		NASSurtPrefixedDecideRule decideRule;
		Field field;
		SurtPrefixSet surtPrefixSet;
		UURI uuri;
		CrawlURI curi;
		String surt;

		cases = new Object[][] {
				{
					"http://sport.tv2.dk/mokeybusiness/",
					"http://(dk,tv2,sport,)/mokeybusiness/",
					new String[] {
							"http://(dk,tv2,sport,)/mokeybusiness/"
					}
				},
				{
					"http://nyheder.tv2.dk/business",
					"http://(dk,tv2,nyheder,)/",
					new String[] {
							"http://(dk,tv2,nyheder,)/",
							"http://(dk,tv2,sport,)/mokeybusiness/",
					}
				},
				{
					"http://www.tv2.dk/",
					"http://(dk,tv2,www,)/",
					new String[] {
							"http://(dk,tv2,nyheder,)/",
							"http://(dk,tv2,sport,)/mokeybusiness/",
							"http://(dk,tv2,www,)/"
					}
				},
				{
					"http://www.tv2.dk",
					"http://(dk,tv2,www,",
					new String[] {
							"http://(dk,tv2,nyheder,)/",
							"http://(dk,tv2,sport,)/mokeybusiness/",
							"http://(dk,tv2,www,"
					}
				},
				{
					"http://tv2.dk/",
					"http://(dk,tv2,)/",
					new String[] {
							"http://(dk,tv2,)/",
							"http://(dk,tv2,nyheder,)/",
							"http://(dk,tv2,sport,)/mokeybusiness/",
							"http://(dk,tv2,www,"
					}
				},
				{
					"http://tv2.dk",
					"http://(dk,tv2,",
					new String[] {
							"http://(dk,tv2,"
					}
				},
		};

		try {
			decideRule = new NASSurtPrefixedDecideRule();
			// Use reflection to read protected field.
			field = SurtPrefixedDecideRule.class.getDeclaredField("surtPrefixes");
			field.setAccessible(true);
			surtPrefixSet = (SurtPrefixSet)field.get(decideRule);

			for (int i=0; i<cases.length; ++i) {
				String uri = (String)cases[i][0];
				String expectedSurt = (String)cases[i][1];
				String[] expectedPrefixes = (String[])cases[i][2];
				Arrays.sort(expectedPrefixes);

				// Construct CrawlURI the same way TextSeedModule does.
				uuri = UURIFactory.getInstance(uri);
	            curi = new CrawlURI(uuri);
	            curi.setSeed(true);
	            curi.setSchedulingDirective(SchedulingConstants.MEDIUM);
	            curi.setSourceTag(uri);
				surt = decideRule.addedSeedImpl(curi);
				Assert.assertEquals(expectedSurt, surt);

				String[] prefixes = new String[surtPrefixSet.size()];
				surtPrefixSet.toArray(prefixes);
				Arrays.sort(prefixes);
				Assert.assertEquals(expectedPrefixes.length, prefixes.length);
				Assert.assertArrayEquals(expectedPrefixes, prefixes);
			}
		} catch (NoSuchFieldException e) {
			e.printStackTrace();
			Assert.fail("Unexpected exception!");
		} catch (SecurityException e) {
			e.printStackTrace();
			Assert.fail("Unexpected exception!");
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			Assert.fail("Unexpected exception!");
		} catch (IllegalAccessException e) {
			e.printStackTrace();
			Assert.fail("Unexpected exception!");
		} catch (URIException e) {
			e.printStackTrace();
			Assert.fail("Unexpected exception!");
		}

		cases = new Object[][] {
				{
					"http://sport.tv2.dk/mokeybusiness/",
					"http://(dk,tv2,sport,)/mokeybusiness/",
					new String[] {
							"http://(dk,tv2,sport,)/mokeybusiness/"
					}
				},
				{
					"http://nyheder.tv2.dk/business",
					"http://(dk,tv2,nyheder,)/",
					new String[] {
							"http://(dk,tv2,nyheder,)/",
							"http://(dk,tv2,sport,)/mokeybusiness/",
					}
				},
				{
					"http://www.tv2.dk/",
					"http://(dk,tv2,www,)/",
					new String[] {
							"http://(dk,tv2,nyheder,)/",
							"http://(dk,tv2,sport,)/mokeybusiness/",
							"http://(dk,tv2,www,)/"
					}
				},
				{
					"http://www.tv2.dk",
					"http://(dk,tv2,www,)/",
					new String[] {
							"http://(dk,tv2,nyheder,)/",
							"http://(dk,tv2,sport,)/mokeybusiness/",
							"http://(dk,tv2,www,)/"
					}
				},
				{
					"http://tv2.dk/",
					"http://(dk,tv2,)/",
					new String[] {
							"http://(dk,tv2,)/",
							"http://(dk,tv2,nyheder,)/",
							"http://(dk,tv2,sport,)/mokeybusiness/",
							"http://(dk,tv2,www,)/"
					}
				},
				{
					"http://tv2.dk",
					"http://(dk,tv2,)/",
					new String[] {
							"http://(dk,tv2,)/",
							"http://(dk,tv2,nyheder,)/",
							"http://(dk,tv2,sport,)/mokeybusiness/",
							"http://(dk,tv2,www,)/"
					}
				},
		};

		try {
			decideRule = new NASSurtPrefixedDecideRule();
			// Use reflection to read protected field.
			field = SurtPrefixedDecideRule.class.getDeclaredField("surtPrefixes");
			field.setAccessible(true);
			surtPrefixSet = (SurtPrefixSet)field.get(decideRule);

			for (int i=0; i<cases.length; ++i) {
				String uri = (String)cases[i][0];
				String expectedSurt = (String)cases[i][1];
				String[] expectedPrefixes = (String[])cases[i][2];
				Arrays.sort(expectedPrefixes);

				// Construct CrawlURI the same way TextSeedModule does.
				uuri = UURIFactory.getInstance(uri);
	            curi = new CrawlURI(uuri);
	            curi.setSeed(true);
	            curi.setSchedulingDirective(SchedulingConstants.MEDIUM);
	            //curi.setSourceTag(uri);
				surt = decideRule.addedSeedImpl(curi);
				Assert.assertEquals(expectedSurt, surt);

				String[] prefixes = new String[surtPrefixSet.size()];
				surtPrefixSet.toArray(prefixes);
				Arrays.sort(prefixes);
				Assert.assertEquals(expectedPrefixes.length, prefixes.length);
				Assert.assertArrayEquals(expectedPrefixes, prefixes);
			}
		} catch (NoSuchFieldException e) {
			e.printStackTrace();
			Assert.fail("Unexpected exception!");
		} catch (SecurityException e) {
			e.printStackTrace();
			Assert.fail("Unexpected exception!");
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			Assert.fail("Unexpected exception!");
		} catch (IllegalAccessException e) {
			e.printStackTrace();
			Assert.fail("Unexpected exception!");
		} catch (URIException e) {
			e.printStackTrace();
			Assert.fail("Unexpected exception!");
		}
	}

}
