package dk.netarkivet.harvester.harvesting;

import java.io.File;

import dk.netarkivet.common.utils.FileUtils;

/** This testing tool runs a single harvest similar to what would happen
 * when a Job arrives.
 * It takes as arguments the name of an order.xml file and a seeds.txt file.
 * The order.xml file must not use deduplication.
 * A crawldir is created in the current directory.
 * A settings.xml file is required in a directory named 'conf' */
public class StandaloneHarvester {
    public static void main(String[] argv) {
        if (argv.length != 2 ||
                !new File(argv[0]).exists() ||
                !new File(argv[1]).exists()) {
            System.out.println("Usage: java "
                               + StandaloneHarvester.class.getName()
                               + " <order.xml file> <seeds.txt file>");
            System.exit(1);
        }
        File crawlDir = new File("CrawlDir" + System.currentTimeMillis());
        crawlDir.mkdirs();
        FileUtils.copyFile(new File(argv[0]),
                           new File(crawlDir, "order.xml"));
        FileUtils.copyFile(new File(argv[1]),
                           new File(crawlDir, "seeds.txt"));
        System.out.println("Attempting crawl in " + crawlDir);
        HeritrixFiles files = new HeritrixFiles(crawlDir, new JobInfoTestImpl(1L, 1L));
        HeritrixLauncher launcher = HeritrixLauncherFactory.getInstance(files);
        launcher.doCrawl();
        System.out.println("Crawl ended, results are in " + crawlDir);
    }
}
