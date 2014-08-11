package org.de.unihannover.l3s.dataset_crawler;

import crawl_utils.FileUtils;
import crawl_utils.Properties;
import database_operations.CrawlDBOperations;
import database_operations.CrawlOperations;
import database_operations.DatabaseConnection;
import dataset_snapshots.DatasetDumpCrawler;
import dataset_snapshots.IncrementalDatasetCrawler;
import entities.CrawlLog;

import java.sql.Connection;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.AbstractMap;
import java.util.Locale;
import java.util.Map;

/**
 * Hello world!
 */
public class App {
    public static void main(String[] args) {
        //String path = "C:\\Users\\besnik\\Documents\\intelliJ_workspace\\dataset_crawler\\dataset_crawler.ini";
        Properties.properties = FileUtils.readIntoStringMap(args[0], "=", false);
        //Properties.properties = FileUtils.readIntoStringMap(path, "=", false);
        Connection mysql_connection = DatabaseConnection.getMySQLConnection(Properties.properties.get("mysql_host"), Properties.properties.get("mysql_schema"), Properties.properties.get("mysql_user"), Properties.properties.get("mysql_pwd"));
        CrawlOperations co = new CrawlOperations(mysql_connection);
        CrawlDBOperations co_db = new CrawlDBOperations(mysql_connection);

        if (Properties.properties.get("run_case").equals("multiple_run")) {
            while (true) {
                System.out.println("Initialising and starting crawl operation...");
                long time = System.nanoTime();
                IncrementalDatasetCrawler inc_crawl = new IncrementalDatasetCrawler(Properties.properties, mysql_connection, co_db, co);
                Map<Integer, Map.Entry<String, String>> crawl_setups = co_db.getActiveCrawlSetups();
                for (int setup_id : crawl_setups.keySet()) {
                    Map.Entry<String, String> entry = crawl_setups.get(setup_id);

                    runCrawlEntry(entry, inc_crawl);

                    System.out.println("Crawl was completed in " + measureComputingTime(time));
                    inc_crawl.updateCrawlSetup(setup_id, co_db);

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
        } else if (Properties.properties.get("run_case").equals("single_run")){
            long time = System.nanoTime();
            System.out.println("Initialising and starting crawl operation...");
            IncrementalDatasetCrawler inc_crawl = new IncrementalDatasetCrawler(Properties.properties, mysql_connection, co_db, co);
            String datasets_file = FileUtils.readText(Properties.properties.get("datasets_path"));
            Map.Entry<String, String> crawl_setup = new AbstractMap.SimpleEntry<String, String>(datasets_file, Properties.properties.get("crawl_description"));
            runCrawlEntry(crawl_setup, inc_crawl);
            System.out.println("Crawl was completed in " + measureComputingTime(time));
        } else if(Properties.properties.get("run_case").equals("dump_run")){
            long time = System.nanoTime();
            System.out.println("Initialising and starting crawl operation...");
            DatasetDumpCrawler dump_crawl = new DatasetDumpCrawler(Properties.properties.get("dump_location"), mysql_connection, co, co_db);
            String datasets_file = FileUtils.readText(Properties.properties.get("datasets_path"));
            Map.Entry<String, String> crawl_setup = new AbstractMap.SimpleEntry<String, String>(datasets_file, Properties.properties.get("crawl_description"));
            runDumpCrawlEntry(crawl_setup, dump_crawl, crawl_setup.getKey());
            System.out.println("Crawl was completed in " + measureComputingTime(time));
        }
    }

    private static void runCrawlEntry(Map.Entry<String, String> entry, IncrementalDatasetCrawler inc_crawl) {
        String crawl_description = entry.getValue();
        String datasets = entry.getKey();

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
    }

    private static void runDumpCrawlEntry(Map.Entry<String, String> entry, DatasetDumpCrawler dump_crawl, String datasets_file) {
        String crawl_description = entry.getValue();
        //initiate crawl log
        CrawlLog crawl_log = dump_crawl.initialiseCrawl(crawl_description);

        //close the connection
        try {
            //crawl all the data from a specific dataset.
            dump_crawl.processDatasetFromDump(crawl_log, datasets_file);

            dump_crawl.mysql_connection.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * For a given start time (as long taken from the system.nanotime(), compute the time difference
     * which results in the time span taken to perform a process.
     *
     * @param time
     * @return
     */
    public static String measureComputingTime(long time) {
        time = System.nanoTime() - time;

        String timeString = NumberFormat.getInstance(Locale.US).format((double) time / 1000D / 1000D / 1000D);
        return ((new StringBuilder(String.valueOf(timeString))).append("s").toString());
    }
}
