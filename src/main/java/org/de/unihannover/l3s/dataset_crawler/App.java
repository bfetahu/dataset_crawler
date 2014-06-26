package org.de.unihannover.l3s.dataset_crawler;

import crawl_utils.Properties;
import dataset_snapshots.IncrementalDatasetCrawler;
import entities.CrawlLog;
import crawl_utils.FileUtils;

import java.sql.SQLException;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args )
    {
        //String path = "C:\\Users\\besnik\\Documents\\intelliJ_workspace\\dataset_crawler\\dataset_crawler.ini";
        Properties.properties = FileUtils.readIntoStringMap(args[0], "=", false);
        IncrementalDatasetCrawler inc_crawl = new IncrementalDatasetCrawler(Properties.properties);

        //initiate crawl log
        CrawlLog crawl_log = inc_crawl.initialiseCrawl(Properties.properties.get("crawl_description"));

        //crawl all the data from a specific dataset.
        inc_crawl.crawlData(crawl_log);

        //close the connection
        try {
            inc_crawl.mysql_connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
