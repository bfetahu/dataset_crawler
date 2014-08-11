package dataset_snapshots;

import crawl_utils.Properties;
import data_crawler.DataCrawler;
import database_operations.CrawlDBOperations;
import database_operations.CrawlOperations;
import entities.*;
import metadata_crawler.Metadata;
import org.semanticweb.yars.nx.Node;
import org.semanticweb.yars.nx.parser.NxParser;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by besnik on 11/08/2014.
 */
public class DatasetDumpCrawler {
    private CrawlDBOperations co_db;
    private String dump_location;
    public Connection mysql_connection;
    public CrawlOperations co;

    public DatasetDumpCrawler(String dump_location, Connection mysql_connection, CrawlOperations co, CrawlDBOperations co_db) {
        this.mysql_connection = mysql_connection;
        this.dump_location = dump_location;

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

    public void processDatasetFromDump(CrawlLog crawl_log, String datasets_str) throws FileNotFoundException {
        //store the namespaces from the dump.
        Map<String, Namespaces> schemas = new HashMap<String, Namespaces>();
        //store the resource types from the dump
        Map<String, ResourceType> resource_types = new HashMap<String, ResourceType>();
        //store the resource instances
        Map<String, Resource> resource_instances = new HashMap<String, Resource>();

        Metadata metadata_crawler = new Metadata();
        DataCrawler dc = new DataCrawler(mysql_connection, crawl_log);

        co_db.crawl_log_global = crawl_log;
        dc.crawl_log_global = crawl_log;
        metadata_crawler.crawl_log_global = crawl_log;

        FileInputStream is = new FileInputStream(dump_location);
        NxParser nxp = new NxParser(is);

        String[] datasets_groups = datasets_str.split("\n");
        Dataset dataset = new Dataset();

        for (String dataset_line : datasets_groups) {
            String[] tmp = dataset_line.split(";");
            if (tmp.length < 3) {
                System.out.println(dataset_line);
                continue;
            }
            String dataset_id = tmp[0];
            String dataset_url = tmp[1];
            String dataset_name = dataset_id;
            String dataset_description = tmp[2];

            dataset.dataset_id_datahub = dataset_id;
            dataset.name = dataset_name;
            dataset.notes = dataset_description;
            dataset.url = dataset_url;
        }
        //load first the crawled namespaces and resource types.
        Set<String> existing_schemas = co_db.loadSchemaURI(mysql_connection);


        Node[] triple;
        while (nxp.hasNext()) {
            triple = nxp.next();

            String subject = triple[0].toString();
            String predicate = triple[1].toString();
            String value = triple[1].toString();

            Resource resource = resource_instances.get(subject);
            resource = resource == null ? new Resource() : resource;
            resource_instances.put(subject, resource);

            if (predicate.equals("http://www.w3.org/1999/02/22-rdf-syntax-ns#type")) {
                if (!resource_types.containsKey(value)) {
                    ResourceType type = new ResourceType();
                    type.type_uri = value;
                    //check if it has the namespace
                    String namespace_uri = Properties.getBaseURI(value);
                    if (!schemas.containsKey(namespace_uri)) {
                        Namespaces namespace = new Namespaces();
                        namespace.namespace_uri = namespace_uri;
                        schemas.put(namespace_uri, namespace);
                    }

                    type.namespace = schemas.get(namespace_uri);
                    resource_types.put(value, type);
                }
                resource.types.put(resource_types.get(value).type_uri, resource_types.get(value));
            } else {
                ResourceValue resource_value = new ResourceValue();
                resource_value.datatype_property = predicate;
                resource_value.value = value;
                resource.values.add(resource_value);
            }
        }

        System.out.println("Crawling data for dataset:  " + dataset.name);
        Map.Entry<String, Boolean> dataset_availability = dc.isDatasetEndpointAvailable(dataset);
        //dataset metadata is stored.
        co_db.writeDatasetMetadata(dataset);
        //store the availability of the endpoint
        co_db.writeDatasetEndpointAvailability(dataset_availability, dataset, crawl_log);

        //crawl the dataset namespaces
        co.crawlDatasetSchemasDump(dataset, schemas, existing_schemas, crawl_log, co_db);

        //crawl the dataset resource types first
        co.crawlDatasetResourceTypesDump(resource_types, co_db);

        //log the start of the crawling for the current dataset
        co_db.writeDatasetCrawlEntry(dataset, crawl_log);

        //crawl the dataset resource instances
        co.crawlDatasetResourceInstancesDump(dataset, resource_types, resource_instances, crawl_log, dc, co_db);

        //log the end time of crawling the dataset.
        co_db.updateDatasetCrawlEntry(dataset, crawl_log);
    }
}
