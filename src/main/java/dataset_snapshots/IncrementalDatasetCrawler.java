/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package dataset_snapshots;

import crawl_utils.Properties;
import data_crawler.DataCrawler;
import database_operations.CrawlDBOperations;
import database_operations.CrawlOperations;
import entities.CrawlLog;
import entities.Dataset;
import metadata_crawler.Metadata;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * @author besnik
 */
public class IncrementalDatasetCrawler {
    public Connection mysql_connection;
    private Map<String, String> props;
    private CrawlDBOperations co_db;
    private CrawlOperations co;

    public IncrementalDatasetCrawler(Map<String, String> props, Connection mysql_connection, CrawlDBOperations co_db, CrawlOperations co) {
        this.mysql_connection = mysql_connection;
        this.props = props;

        this.co = co;
        this.co_db = co_db;
    }

    /**
     * Iniatialise the crawl main entry point.
     *
     * @param crawl_description
     * @return
     */
    public CrawlLog initialiseCrawl(String crawl_description) {
        CrawlLog crawl_log = new CrawlLog();
        crawl_log.crawl_description = crawl_description;

        //write to the database the crawl main log entry.
        co_db.writeCrawlMainEntry(crawl_log);
        return crawl_log;
    }


    /**
     * Crawls the datasets for their metadata and their resources. At every step
     * we check whether there is already stored for a particular dataset. In
     * case there is we check for changes or deletions, otherwise we consider it
     * as an insert.
     *
     * @param crawl_log
     */
    public void crawlDataDataHub(CrawlLog crawl_log) {
        Metadata metadata_crawler = new Metadata();
        DataCrawler dc = new DataCrawler(mysql_connection, crawl_log);

        co_db.crawl_log_global = crawl_log;
        dc.crawl_log_global = crawl_log;
        metadata_crawler.crawl_log_global = crawl_log;

        String[] datahub_group = Properties.properties.get("datahub_keyword").split(",");

        boolean is_datahub_group_search = Properties.properties.get("is_datahub_group_search").equals("true");
        List<Dataset> datasets = metadata_crawler.searchDataHub(is_datahub_group_search, datahub_group, mysql_connection);

        //load first the crawled namespaces and resource types.
        Set<String> existing_schemas = co_db.loadSchemaURI(mysql_connection);

        //write the crawled metadata.
        for (Dataset dataset : datasets) {
            if (dataset == null || dataset.name == null || dataset.url == null)
                continue;

            System.out.println("Crawling data for dataset:  " + dataset.name);
            Entry<String, Boolean> dataset_availability = dc.isDatasetEndpointAvailable(dataset);
            //dataset metadata is stored.
            co_db.writeDatasetMetadata(dataset);
            //store the availability of the endpoint
            co_db.writeDatasetEndpointAvailability(dataset_availability, dataset, crawl_log);

            if (dataset_availability == null || !dataset_availability.getValue()) {
                System.out.println("Dataset " + dataset.name + " is down.");
                continue;
            }

            //crawl the dataset namespaces
            co.crawlDatasetSchemas(dataset, existing_schemas, crawl_log, co_db, dc);

            //crawl the dataset resource types first
            co.crawlDatasetResourceTypes(dataset, dc, co_db);

            //log the start of the crawling for the current dataset
            co_db.writeDatasetCrawlEntry(dataset, crawl_log);

            //crawl the dataset resource instances
            co.crawlDatasetResourceInstances(dataset, crawl_log, dc, co_db);

            //log the end time of crawling the dataset.
            co_db.updateDatasetCrawlEntry(dataset, crawl_log);
        }
    }

    /**
     * Crawls the datasets for their metadata and their resources. At every step
     * we check whether there is already stored for a particular dataset. In
     * case there is we check for changes or deletions, otherwise we consider it
     * as an insert.
     *
     * @param crawl_log
     */
    public void crawlData(CrawlLog crawl_log, String datasets_str) {
        Metadata metadata_crawler = new Metadata();
        DataCrawler dc = new DataCrawler(mysql_connection, crawl_log);

        co_db.crawl_log_global = crawl_log;
        dc.crawl_log_global = crawl_log;
        metadata_crawler.crawl_log_global = crawl_log;

        String[] datasets_groups = datasets_str.split("\n");
        List<Dataset> datasets = new ArrayList<Dataset>();

        for (String dataset_line : datasets_groups) {
            String[] tmp = dataset_line.split("\t");
            if (tmp.length < 3) {
                System.out.println(dataset_line);
                continue;
            }
            String dataset_id = tmp[0];
            String dataset_url = tmp[1];
            String dataset_name = dataset_id;
            String dataset_description = tmp[2];

            Dataset dataset = new Dataset();
            dataset.dataset_id_datahub = dataset_id;
            dataset.name = dataset_name;
            dataset.notes = dataset_description;
            dataset.url = dataset_url;
            datasets.add(dataset);
        }
        //load first the crawled namespaces and resource types.
        Set<String> existing_schemas = co_db.loadSchemaURI(mysql_connection);

        //write the crawled metadata.
        for (Dataset dataset : datasets) {
            if (dataset == null || dataset.name == null || dataset.url == null)
                continue;

            System.out.println("Crawling data for dataset:  " + dataset.name);
            Entry<String, Boolean> dataset_availability = dc.isDatasetEndpointAvailable(dataset);
            //dataset metadata is stored.
            co_db.writeDatasetMetadata(dataset);
            //store the availability of the endpoint
            co_db.writeDatasetEndpointAvailability(dataset_availability, dataset, crawl_log);

            if (dataset_availability == null || !dataset_availability.getValue()) {
                System.out.println("Dataset " + dataset.name + " is down.");
                continue;
            }

            //crawl the dataset namespaces
            co.crawlDatasetSchemas(dataset, existing_schemas, crawl_log, co_db, dc);

            //crawl the dataset resource types first
            co.crawlDatasetResourceTypes(dataset, dc, co_db);

            //log the start of the crawling for the current dataset
            co_db.writeDatasetCrawlEntry(dataset, crawl_log);

            //crawl the dataset resource instances
            co.crawlDatasetResourceInstances(dataset, crawl_log, dc, co_db);

            //log the end time of crawling the dataset.
            co_db.updateDatasetCrawlEntry(dataset, crawl_log);
        }
    }

    public void updateCrawlSetup(int crawl_setupid, CrawlDBOperations co_db) {
        co_db.updateCrawlSetup(crawl_setupid);
    }
}
