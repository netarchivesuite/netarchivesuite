package dk.netarkivet.common.utils;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class TemplateTester {

	@Test
	public void testMissing() {
		String replaceStr = "ljdaskdasdkasdæas kkaækfakfækfæasfæas"
				+ "\n" + "%{WARC_ITEMS_PLACEHOLDER} KKKFKKFKF";
		Map<String,String> env = new HashMap<String,String>();
        env.put("WARC_ITEMS_PLACEHOLDER", "WARC_ITEMS_LIST");
		String newString = Template.untemplate(replaceStr, env, false);
		System.out.println(newString);
	}

}
