package org.de.unihannover.l3s.dataset_crawler;

import crawl_utils.FileUtils;
import crawl_utils.Properties;
import dataset_snapshots.IncrementalDatasetCrawler;
import entities.CrawlLog;

import java.sql.SQLException;
import java.util.Map;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args ){
        Properties.properties = FileUtils.readIntoStringMap(args[0], "=", false);
        IncrementalDatasetCrawler inc_crawl = new IncrementalDatasetCrawler(Properties.properties);

        while(true){
            Map<Integer, Map.Entry<String, String>> crawl_setups = inc_crawl.co.getActiveCrawlSetups();
            for(int setup_id:crawl_setups.keySet()){
                Map.Entry<String, String> entry = crawl_setups.get(setup_id);

                String crawl_description = entry.getValue();
                String datasets = entry.getValue();

                //initiate crawl log
                CrawlLog crawl_log = inc_crawl.initialiseCrawl(crawl_description);

                //crawl all the data from a specific dataset.
                inc_crawl.crawlData(crawl_log, datasets);

                //close the connection
                try {
                    inc_crawl.mysql_connection.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }

                try {
                    Thread.sleep(30 * 60 * 1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
