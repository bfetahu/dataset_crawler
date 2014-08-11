/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package database_operations;

import crawl_utils.Properties;
import entities.*;

import java.sql.*;
import java.sql.Date;
import java.util.*;
import java.util.Map.Entry;

/**
 * @author besnik
 */
public class CrawlDBOperations {
    //for each crawl operation there is a CrawlLog which these operations are associated with. We keep an singelton object of the crawllog
    public CrawlLog crawl_log_global;
    private Connection conn;

    public CrawlDBOperations(Connection conn) {
        this.conn = conn;
    }
    //=============================================================================================================================================
    //=============================================== Write operations into the database ==========================================================
    //=============================================================================================================================================

    /**
     * Stores the crawled dataset metadata into the database.
     *
     * @param dataset
     * @return
     */
    public boolean writeDatasetMetadata(Dataset dataset) {
        PreparedStatement pst = null;

        try {
            boolean is_update = false;
            Entry<Integer, Boolean> dataset_entry = isDatasetMetadataUpdated(dataset);
            if (dataset_entry.getKey() != -1 && !dataset_entry.getValue()) {
                pst = conn.prepareStatement("UPDATE dataset SET dataset_name=?, dataset_description=?, dataset_url=? WHERE dataset_id_datahub=? AND dataset_id=" + dataset_entry.getKey());
                dataset.id = dataset_entry.getKey();

                is_update = true;
            } else {
                pst = conn.prepareStatement("INSERT INTO dataset(dataset_name,dataset_description,dataset_url,dataset_id_datahub) VALUES(?,?,?,?)", Statement.RETURN_GENERATED_KEYS);
            }

            pst.setString(1, dataset.name);
            pst.setString(2, dataset.notes);
            pst.setString(3, dataset.url);
            pst.setString(4, dataset.dataset_id_datahub);

            int rst = pst.executeUpdate();
            //get the generated auto increment keys.
            if (!is_update) {
                ResultSet rs_keys = pst.getGeneratedKeys();

                if (rst != 0) {
                    if (dataset.id == 0) {
                        if (rs_keys.next()) {
                            dataset.id = rs_keys.getInt(1);
                        }
                    }
                    CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.success.toString(), "writeDatasetMetadata", "sucssess writing metadata for" + dataset.id, crawl_log_global, conn);
                } else {
                    CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.error.toString(), "writeDatasetMetadata", "error writing metadata for" + dataset.id, crawl_log_global, conn);
                }
            } else {
                CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.success.toString(), "writeDatasetMetadata", "sucssess updating metadata for" + dataset.id, crawl_log_global, conn);
            }
            return rst != 0;
        } catch (Exception ex) {
            CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.exception.toString(), "writeDataset", "exception writing metadata for" + dataset.id + "\n " + ex.getMessage(), crawl_log_global, conn);
        }
        CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.error.toString(), "writeDataset", "error writing metadata for" + dataset.id, crawl_log_global, conn);
        return false;
    }

    /**
     * Writes the dataset namespaces.
     *
     * @param dataset
     * @param namespace
     */
    public void writeDatasetSchemas(Dataset dataset, Namespaces namespace) {
        PreparedStatement pst = null;

        try {
            pst = conn.prepareStatement("INSERT INTO dataset_namespaces(dataset_id,namespace_id) VALUES (?,?)");
            pst.setInt(1, dataset.id);
            pst.setInt(2, namespace.namespace_id);

            int rst = pst.executeUpdate();

            if (rst != 0) {
                CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.success.toString(), "writeDatasetSchemas", "success writing dataset namespaces for dataset: " + dataset.dataset_id_datahub + " and namespaces: " + namespace.namespace_uri, crawl_log_global, conn);
            } else {
                CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.error.toString(), "writeDatasetSchemas", "error writing dataset namespaces for dataset: " + dataset.dataset_id_datahub + " and namespaces: " + namespace.namespace_uri, crawl_log_global, conn);
            }
        } catch (Exception ex) {
            CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.exception.toString(), "writeDatasetSchemas", "exception writing dataset namespaces for" + dataset.dataset_id_datahub + " and namespaces: " + namespace.namespace_uri + "\n " + ex.getMessage(), crawl_log_global, conn);
        }
    }

    /**
     * Writes the dataset namespaces log crawls.
     *
     * @param namespace
     */
    public void writeDatasetSchemasLogs(Namespaces namespace) {
        PreparedStatement pst = null;

        try {
            pst = conn.prepareStatement("INSERT INTO dataset_namespaces_log(dataset_id,namespace_id,log_type,crawl_id) VALUES (?,?,?,?)");
            for (int crawl_id : namespace.dataset_namespace_crawl_logs.keySet()) {
                for (int dataset_id : namespace.dataset_namespace_crawl_logs.get(crawl_id).keySet()) {
                    String log_type = namespace.dataset_namespace_crawl_logs.get(crawl_id).get(dataset_id);

                    pst.setInt(1, dataset_id);
                    pst.setInt(2, namespace.namespace_id);
                    pst.setString(3, log_type);
                    pst.setInt(4, crawl_id);

                    pst.addBatch();
                }
            }

            int[] rst = pst.executeBatch();
            for (int i = 0; i < rst.length; i++) {
                if (rst[i] != 0) {
                    CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.success.toString(), "writeDatasetSchemasLogs", "success writing dataset namespaces logs ", crawl_log_global, conn);
                } else {
                    CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.error.toString(), "writeDatasetSchemasLogs", "error writing dataset namespaces logs ", crawl_log_global, conn);
                }
            }
        } catch (Exception ex) {
            CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.exception.toString(), "writeDatasetSchemasLogs", "exception writing dataset namespaces logs\n " + ex.getMessage(), crawl_log_global, conn);
        }
    }


    /**
     * Writes the availability of the datasets' endpoints.
     *
     * @param dataset_availability
     */
    public void writeDatasetEndpointAvailability(Entry<String, Boolean> dataset_availability, Dataset dataset, CrawlLog crawl_log) {
        PreparedStatement pst = null;
        try {
            pst = conn.prepareStatement("INSERT INTO dataset_availability(crawl_id,dataset_id,is_available) VALUES(?,?,?)");
            pst.setInt(1, crawl_log.crawl_id);
            pst.setInt(2, dataset.id);
            pst.setBoolean(3, dataset_availability.getValue());

            int rst = pst.executeUpdate();

            if (rst != 0) {
                CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.success.toString(), "writeDatasetEndpointAvailability", "sucssess writing endpoint availability for dataset " + dataset.name, crawl_log_global, conn);
            } else {
                CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.error.toString(), "writeDatasetEndpointAvailability", "error writing endpoint availability  for dataset " + dataset.name, crawl_log_global, conn);
            }
        } catch (Exception ex) {
            CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.exception.toString(), "writeDatasetEndpointAvailability", "exception writing endpoint availability\n " + ex.getMessage(), crawl_log_global, conn);
        }
    }

    /**
     * Writes the resource types from a particular dataset. First checks if the
     * particular resource exists, in that case it doesnt write otherwise yes.
     * It also provides the resource_type_id which is the primary key from the
     * database.
     *
     * @param resource_types
     */
    public void writeResourceTypes(Map<String, ResourceType> resource_types) {
        PreparedStatement pst = null;
        try {
            pst = conn.prepareStatement("INSERT INTO resource_types(type_uri,namespace_id) VALUES(?,?)", Statement.RETURN_GENERATED_KEYS);
            for (String resource_type_uri : resource_types.keySet()) {
                ResourceType resource_type = resource_types.get(resource_type_uri);
                pst.setString(1, resource_type.type_uri);
                pst.setInt(2, resource_type.namespace.namespace_id);

                pst.addBatch();
            }

            int[] rst = pst.executeBatch();
            //get the generated auto increment keys.
            ResultSet rs_keys = pst.getGeneratedKeys();

            Object[] res_type_arr = resource_types.keySet().toArray();
            for (int i = 0; i < rst.length; i++) {
                String resource_type = (String) res_type_arr[i];
                if (rst[i] != 0) {
                    rs_keys.next();
                    resource_types.get(resource_type).resource_type_id = rs_keys.getInt(1);

                    CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.success.toString(), "writeResourceTypes", "sucssess writing resource type " + resource_type, crawl_log_global, conn);
                } else {
                    CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.error.toString(), "writeResourceTypes", "error writing resource type " + resource_type, crawl_log_global, conn);
                }
            }
        } catch (Exception ex) {
            CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.exception.toString(), "writeResourceTypes", "exception writing resource types\n " + ex.getMessage(), crawl_log_global, conn);
        }
    }

    /**
     * Writes the crawl logs for the resource types.
     *
     * @param resource_types
     */
    public void writeResourceTypeCrawlLogs(Set<ResourceType> resource_types) {
        PreparedStatement pst = null;
        try {
            //log the updates for each dataset stored as crawl-id -> dataset_id -> resource_id, log_type
            pst = conn.prepareStatement("INSERT INTO resource_type_log(resource_id,type_id,log_type,crawl_id) VALUES(?,?,?,?)");
            for (ResourceType resource_type : resource_types) {
                for (int crawl_id : resource_type.resource_type_crawl_logs.keySet()) {
                    for (int dataset_id : resource_type.resource_type_crawl_logs.get(crawl_id).keySet()) {
                        for (int resource_id : resource_type.resource_type_crawl_logs.get(crawl_id).get(dataset_id).keySet()) {
                            String log_type = resource_type.resource_type_crawl_logs.get(crawl_id).get(dataset_id).get(resource_id);

                            pst.setInt(1, resource_id);
                            pst.setInt(2, resource_type.resource_type_id);
                            pst.setString(3, log_type);
                            pst.setInt(4, crawl_id);

                            pst.addBatch();
                        }
                    }
                }
            }

            int[] rst = pst.executeBatch();
            Object[] res_type_arr = resource_types.toArray();
            for (int i = 0; i < rst.length; i++) {
                String resource_type = ((ResourceType) res_type_arr[i]).type_uri;
                if (rst[i] != 0) {
                    CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.success.toString(), "writeResourceTypeCrawlLogs", "sucssess writing resource type log " + resource_type, crawl_log_global, conn);
                } else {
                    CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.error.toString(), "writeResourceTypeCrawlLogs", "error writing resource type log" + resource_type, crawl_log_global, conn);
                }
            }
        } catch (Exception ex) {
            CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.exception.toString(), "writeResourceTypeCrawlLogs", "exception writing resource types log \n " + ex.getMessage(), crawl_log_global, conn);
        }
    }

    /**
     * Writes the main entry for an initiated crawl.
     *
     * @param crawl_log
     */
    public void writeCrawlMainEntry(CrawlLog crawl_log) {
        PreparedStatement pst = null;
        try {
            Date date = new Date(new java.util.Date().getTime());
            java.sql.Timestamp time_stamp = new java.sql.Timestamp(date.getTime());
            crawl_log.crawl_date = date;

            pst = conn.prepareStatement("INSERT INTO crawl_log(crawl_description,crawl_date) VALUES(?,?)", Statement.RETURN_GENERATED_KEYS);
            pst.setString(1, crawl_log.crawl_description);
            pst.setTimestamp(2, time_stamp);

            int rst = pst.executeUpdate();

            //get the generated auto increment keys.
            ResultSet rs_keys = pst.getGeneratedKeys();

            if (rst != 0) {
                rs_keys.next();
                crawl_log.crawl_id = rs_keys.getInt(1);

                CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.success.toString(), "writeCrawlMainEntry", "success writing main crawl log entry: " + crawl_log.crawl_description, crawl_log, conn);
            } else {
                CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.error.toString(), "writeCrawlMainEntry", "error writing main crawl log entry: " + crawl_log.crawl_description, crawl_log, conn);
            }
        } catch (Exception ex) {
            CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.exception.toString(), "writeCrawlMainEntry", "exception writing main crawl log entry: " + crawl_log.crawl_description + "\n" + ex.getMessage(), crawl_log, conn);
        }
    }

    /**
     * Writes the crawl entry for a specific dataset.
     *
     * @param dataset
     * @param crawl_log
     */
    public void writeDatasetCrawlEntry(Dataset dataset, CrawlLog crawl_log) {

        PreparedStatement pst = null;
        try {
            Date date = new Date(new java.util.Date().getTime());
            java.sql.Timestamp time_stamp = new java.sql.Timestamp(date.getTime());

            pst = conn.prepareStatement("INSERT INTO dataset_crawl_log(crawl_id,dataset_id,timestamp_start) VALUES(?,?,?)");
            pst.setInt(1, crawl_log.crawl_id);
            pst.setInt(2, dataset.id);
            pst.setTimestamp(3, time_stamp);

            int rst = pst.executeUpdate();

            if (rst != 0) {
                CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.success.toString(), "writeDatasetCrawlEntry", "success writing dataset crawl log entry: for crawl_id " + crawl_log.crawl_id + " and dataset " + dataset.id, crawl_log_global, conn);
            } else {
                CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.error.toString(), "writeDatasetCrawlEntry", "error writing dataset crawl log entry: for crawl_id " + crawl_log.crawl_id + " and dataset " + dataset.id, crawl_log_global, conn);
            }
        } catch (Exception ex) {
            CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.error.toString(), "writeDatasetCrawlEntry", "exception writing dataset crawl log entry: for crawl_id " + crawl_log.crawl_id + " and dataset " + dataset.id + "\n" + ex.getMessage(), crawl_log_global, conn);
        }
    }

    /**
     * Updates the dataset crawl entry whenever its crawl operations have
     * finished.
     *
     * @param dataset
     * @param crawl_log
     */
    public void updateDatasetCrawlEntry(Dataset dataset, CrawlLog crawl_log) {
        PreparedStatement pst = null;
        try {
            Date date = new Date(new java.util.Date().getTime());
            java.sql.Timestamp time_stamp = new java.sql.Timestamp(date.getTime());

            pst = conn.prepareStatement("UPDATE dataset_crawl_log SET timestamp_end=? WHERE crawl_id=? AND dataset_id=?");
            pst.setTimestamp(1, time_stamp);
            pst.setInt(2, crawl_log.crawl_id);
            pst.setInt(3, dataset.id);

            int rst = pst.executeUpdate();

            if (rst != 0) {
                CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.success.toString(), "updateDatasetCrawlEntry", "success updating dataset crawl log entry: for crawl_id " + crawl_log.crawl_id + " and dataset " + dataset.id, crawl_log_global, conn);
            } else {
                CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.error.toString(), "updateDatasetCrawlEntry", "error updating dataset crawl log entry: for crawl_id " + crawl_log.crawl_id + " and dataset " + dataset.id, crawl_log_global, conn);
            }
        } catch (Exception ex) {
            CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.error.toString(), "updateDatasetCrawlEntry", "exception updating dataset crawl log entry: for crawl_id " + crawl_log.crawl_id + " and dataset " + dataset.id + "\n" + ex.getMessage(), crawl_log_global, conn);
        }
    }

    /**
     * Writes the namespace data into the database.
     *
     * @param namespace
     */
    public void writeSchema(Namespaces namespace) {
        PreparedStatement pst = null;
        try {
            pst = conn.prepareStatement("INSERT INTO namespace(namespace_uri) VALUES(?)", Statement.RETURN_GENERATED_KEYS);
            pst.setString(1, namespace.namespace_uri);

            int rst = pst.executeUpdate();
            //get the generated auto increment keys.
            ResultSet rs_keys = pst.getGeneratedKeys();

            if (rst != 0) {
                rs_keys.next();
                namespace.namespace_id = rs_keys.getInt(1);
                CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.success.toString(), "writeSchema", "success writing namespaces: " + namespace.namespace_uri, crawl_log_global, conn);
            } else {
                CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.error.toString(), "writeSchema", "error writing namespaces: " + namespace.namespace_uri, crawl_log_global, conn);
            }
        } catch (Exception ex) {
            CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.error.toString(), "writeSchema", "exception writing namespaces: " + namespace.namespace_uri + "\n" + ex.getMessage(), crawl_log_global, conn);
        }
    }

    /**
     * Writes the namespace instances for a particular namespace uri.
     *
     * @param namespace
     */
    public void writeSchemaInstances(Namespaces namespace) {
        PreparedStatement pst = null;
        try {
            pst = conn.prepareStatement("INSERT INTO namespaces_instances(namespace_id,namespace_value_uri,namespace_value_type) VALUES(?,?,?)", Statement.RETURN_GENERATED_KEYS);
            for (NamespaceInstance namespace_instance : namespace.instances) {
                pst.setInt(1, namespace.namespace_id);
                pst.setString(2, namespace_instance.namespace_value_uri);
                pst.setBoolean(3, namespace_instance.isProperty);

                pst.addBatch();
            }

            int[] rst = pst.executeBatch();

            for (int i = 0; i < rst.length; i++) {
                if (rst[i] != 0) {
                    CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.success.toString(), "writeSchemaInstances", "success writing namespaces instance: " + namespace.namespace_uri + " and instance " + namespace.instances.get(i).namespace_value_uri, crawl_log_global, conn);
                } else {
                    CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.error.toString(), "writeSchemaInstances", "error writing namespaces instances: " + namespace.namespace_uri + " and instance " + namespace.instances.get(i).namespace_value_uri, crawl_log_global, conn);
                }
            }
        } catch (Exception ex) {
            CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.error.toString(), "writeSchemaInstances", "exception writing namespaces instance: " + namespace.namespace_uri + "\n" + ex.getMessage(), crawl_log_global, conn);
        }
    }

    /**
     * Writes the namespace instances for a particular namespace uri.
     *
     * @param namespace
     */
    public void writeSchemaInstanceLogs(Namespaces namespace) {
        PreparedStatement pst = null;
        try {
            pst = conn.prepareStatement("INSERT INTO namespaces_instance_log(namespace_id,namespace_value_uri,log_type,crawl_id,dataset_id) VALUES(?,?,?,?,?)");

            for (int crawl_id : namespace.namespace_instance_crawl_logs.keySet()) {
                for (int dataset_id : namespace.namespace_instance_crawl_logs.get(crawl_id).keySet()) {
                    for (String schi_val : namespace.namespace_instance_crawl_logs.get(crawl_id).get(dataset_id).keySet()) {
                        pst.setInt(1, namespace.namespace_id);
                        pst.setString(2, schi_val);
                        pst.setString(3, namespace.namespace_instance_crawl_logs.get(crawl_id).get(dataset_id).get(schi_val));
                        pst.setInt(4, crawl_id);
                        pst.setInt(5, dataset_id);

                        pst.addBatch();
                    }
                }
            }
            int[] rst = pst.executeBatch();

            for (int i = 0; i < rst.length; i++) {
                if (rst[i] != 0) {
                    CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.success.toString(), "writeSchemaInstanceLogs", "success writing namespaces instance logs: " + namespace.namespace_uri, crawl_log_global, conn);
                } else {
                    CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.error.toString(), "writeSchemaInstanceLogs", "error writing namespaces instances logs: " + namespace.namespace_uri, crawl_log_global, conn);
                }
            }
        } catch (Exception ex) {
            CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.error.toString(), "writeSchemaInstanceLogs", "exception writing namespaces instance logs: " + namespace.namespace_uri + "\n" + ex.getMessage(), crawl_log_global, conn);
        }
    }

    /**
     * Writes the set of resource instances for a dataset.
     *
     * @param dataset
     */
    public void writeResourceInstances(Dataset dataset) {
        PreparedStatement pst = null;
        try {
            pst = conn.prepareStatement("INSERT INTO resource_instances(resource_uri,dataset_id) VALUES(?,?)", Statement.RETURN_GENERATED_KEYS);
            for (String resource_uri : dataset.resources.keySet()) {
                Resource resource = dataset.resources.get(resource_uri);

                pst.setString(1, resource.resource_uri);
                pst.setInt(2, dataset.id);

                pst.addBatch();
            }

            int[] rst = pst.executeBatch();
            //get the generated auto increment keys.
            ResultSet rs_keys = pst.getGeneratedKeys();

            Object[] res_uri_arr = dataset.resources.keySet().toArray();
            for (int i = 0; i < rst.length; i++) {
                Resource resource = dataset.resources.get(res_uri_arr[i].toString());
                if (rst[i] != 0) {
                    rs_keys.next();
                    resource.resource_id = rs_keys.getInt(1);

                    //if added successfully, add the corresponding resource types for the resource instance
                    //add the resource types for the resource instance
                    writeResourceInstanceType(resource);
                } else {
                    CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.error.toString(), "writeResourceInstances", "error writing resource instance: " + resource.resource_uri, crawl_log_global, conn);
                }
            }
            //if there is no error write that the operation was carried successfully.
            CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.success.toString(), "writeResourceInstances", "success writing resource instances", crawl_log_global, conn);
        } catch (Exception ex) {
            CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.error.toString(), "writeResourceInstances", "exception writing resource instances: " + "\n" + ex.getMessage(), crawl_log_global, conn);
        }
    }

    /**
     * Writes the set of resource instances for a dataset.
     *
     * @param dataset
     */
    public void writeResourceInstances(Resource resource, Dataset dataset) {
        PreparedStatement pst = null;
        try {
            pst = conn.prepareStatement("INSERT INTO resource_instances(resource_uri,dataset_id) VALUES(?,?,?)", Statement.RETURN_GENERATED_KEYS);

            pst.setString(1, resource.resource_uri);
            pst.setInt(2, dataset.id);

            int rst = pst.executeUpdate();
            //get the generated auto increment keys.
            ResultSet rs_keys = pst.getGeneratedKeys();

            if (rst != 0) {
                rs_keys.next();
                resource.resource_id = rs_keys.getInt(1);
                //add the resource types for the resource instance
                writeResourceInstanceType(resource);
            } else {
                CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.error.toString(), "writeResourceInstances", "error writing resource instance: " + resource.resource_uri, crawl_log_global, conn);
            }
        } catch (Exception ex) {
            CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.error.toString(), "writeResourceInstances", "exception writing resource instances: " + "\n" + ex.getMessage(), crawl_log_global, conn);
        }
    }

    /**
     * Writes for each resource instance its resource types.
     *
     * @param resource
     */
    public void writeResourceInstanceType(Resource resource) {
        PreparedStatement pst = null;
        try {
            pst = conn.prepareStatement("INSERT INTO resource_instance_type(resource_id,type_id) VALUES(?,?)");

            for (String resource_type_uri : resource.types.keySet()) {
                ResourceType resource_type = resource.types.get(resource_type_uri);

                pst.setInt(1, resource.resource_id);
                pst.setInt(2, resource_type.resource_type_id);

                pst.addBatch();
            }

            int[] rst = pst.executeBatch();
            Object[] res_type_array = resource.types.values().toArray();
            for (int i = 0; i < rst.length; i++) {
                ResourceType res_type = (ResourceType) res_type_array[i];

                if (rst[i] == 0) {
                    CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.error.toString(), "writeResourceInstanceType", "error writing resource instance types: " + resource.resource_uri + " and resource type: " + res_type.type_uri, crawl_log_global, conn);
                }
            }
            CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.success.toString(), "writeResourceInstanceType", "success writing resource instance and resource type mapping.", crawl_log_global, conn);
        } catch (Exception ex) {
            CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.error.toString(), "writeResourceInstanceType", "exception writing resource instances types: " + "\n" + ex.getMessage(), crawl_log_global, conn);
        }
    }

    /**
     * Writes for each resource instance its resource types.
     *
     * @param res_type_instances
     */
    public void writeDatasetResourceInstanceType(Map<Integer, Set<Integer>> res_type_instances) {
        PreparedStatement pst = null;
        try {
            pst = conn.prepareStatement("INSERT INTO resource_instance_type(resource_id,type_id) VALUES(?,?)");

            for (int res_id : res_type_instances.keySet()) {
                for (int type_id : res_type_instances.get(res_id)) {
                    pst.setInt(1, res_id);
                    pst.setInt(2, type_id);
                    pst.addBatch();
                }
            }

            int[] rst = pst.executeBatch();
            Object[] res_type_array = res_type_instances.keySet().toArray();
            for (int i = 0; i < rst.length; i++) {
                if (rst[i] == 0) {
                    CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.error.toString(), "writeDatasetResourceInstanceType", "error writing resource instance types: " + res_type_array[i], crawl_log_global, conn);
                }
            }

            CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.success.toString(), "writeDatasetResourceInstanceType", "success writing resource instance types", crawl_log_global, conn);
        } catch (Exception ex) {
            CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.error.toString(), "writeDatasetResourceInstanceType", "exception writing resource instances types: " + "\n" + ex.getMessage(), crawl_log_global, conn);
        }
    }

    /**
     * Writes for each resource instance its resource types.
     *
     * @param dataset
     */
    public void writeDatasetResourceInstanceType(Dataset dataset, ResourceType type) {
        PreparedStatement pst = null;
        try {
            pst = conn.prepareStatement("INSERT INTO resource_instance_type(resource_id,type_id) VALUES(?,?)");

            for (String resource_uri : dataset.resources.keySet()) {
                Resource resource = dataset.resources.get(resource_uri);
                pst.setInt(1, resource.resource_id);
                pst.setInt(2, type.resource_type_id);
                pst.addBatch();
            }

            int[] rst = pst.executeBatch();
            Object[] res_type_array = dataset.resources.values().toArray();
            for (int i = 0; i < rst.length; i++) {
                Resource resource = (Resource) res_type_array[i];

                if (rst[i] == 0) {
                    CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.error.toString(), "writeDatasetResourceInstanceType", "error writing resource instance types: " + resource.resource_uri + " and resource type: " + type.type_uri, crawl_log_global, conn);
                }
            }
            CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.success.toString(), "writeDatasetResourceInstanceType", "success writing resource instance and resource type: " + type.type_uri, crawl_log_global, conn);
        } catch (Exception ex) {
            CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.error.toString(), "writeDatasetResourceInstanceType", "exception writing resource instances types: " + "\n" + ex.getMessage(), crawl_log_global, conn);
        }
    }


    /**
     * Writes the log of changes for each resource instance and the corresponding associated resource types at the different logs.
     *
     * @param type
     */
    public void writeDatasetResourceInstanceTypeLog(ResourceType type) {
        PreparedStatement pst = null;
        try {
            pst = conn.prepareStatement("INSERT INTO resource_instance_type_log(resource_id,type_id,log_type,crawl_id) VALUES(?,?,?,?)");

            for (int crawl_log : type.resource_type_crawl_logs.keySet()) {
                for (int dataset_id : type.resource_type_crawl_logs.get(crawl_log).keySet()) {
                    for (int resource_id : type.resource_type_crawl_logs.get(crawl_log).get(dataset_id).keySet()) {
                        String log_type = type.resource_type_crawl_logs.get(crawl_log).get(dataset_id).get(resource_id);

                        pst.setInt(1, resource_id);
                        pst.setInt(2, type.resource_type_id);
                        pst.setString(3, log_type);
                        pst.setInt(4, crawl_log);

                        pst.addBatch();
                    }
                }
            }

            int[] rst = pst.executeBatch();

            for (int i = 0; i < rst.length; i++) {
                if (rst[i] == 0) {
                    CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.error.toString(), "writeDatasetResourceInstanceTypeLog", "error writing resource instance type logs " + type.type_uri, crawl_log_global, conn);
                }
            }

            CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.success.toString(), "writeDatasetResourceInstanceTypeLog", "success writing resource instance type logs" + type.type_uri, crawl_log_global, conn);
        } catch (Exception ex) {
            CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.error.toString(), "writeDatasetResourceInstanceTypeLog", "exception writing resource instances type logs: " + "\n" + ex.getMessage(), crawl_log_global, conn);
        }
    }

    /**
     * Writes the log of changes for each resource instance and the corresponding associated resource types at the different logs.
     *
     * @param resource
     */
    public void writeResourceInstanceTypeLog(Resource resource) {
        PreparedStatement pst = null;
        try {
            pst = conn.prepareStatement("INSERT INTO resource_instance_type_log(resource_id,type_id,log_type,crawl_id) VALUES(?,?,?,?)");

            for (int crawl_log : resource.resource_type_log.keySet()) {
                for (int resource_type_id : resource.resource_type_log.get(crawl_log).keySet()) {
                    String log_type = resource.resource_type_log.get(crawl_log).get(resource_type_id);

                    pst.setInt(1, resource.resource_id);
                    pst.setInt(2, resource_type_id);
                    pst.setString(3, log_type);
                    pst.setInt(4, crawl_log);

                    pst.addBatch();
                }
            }

            int[] rst = pst.executeBatch();

            for (int i = 0; i < rst.length; i++) {
                if (rst[i] == 0) {
                    CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.error.toString(), "writeResourceInstanceTypeLog", "error writing resource instance type  logs: " + resource.resource_uri, crawl_log_global, conn);
                }
            }
            CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.success.toString(), "writeResourceInstanceTypeLog", "success writing resource instance type logs: " + resource.resource_uri, crawl_log_global, conn);
        } catch (Exception ex) {
            CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.error.toString(), "writeResourceInstanceTypeLog", "exception writing resource instances type logs: " + "\n" + ex.getMessage(), crawl_log_global, conn);
        }
    }

    /**
     * Writes the log of changes for each resource instance and the corresponding associated resource types at the different logs.
     *
     * @param resource
     */
    public void writeResourceInstanceTypeLog(Map<Integer, Resource> existing_resources) {
        PreparedStatement pst = null;
        try {
            pst = conn.prepareStatement("INSERT INTO resource_instance_type_log(resource_id,type_id,log_type,crawl_id) VALUES(?,?,?,?)");

            for (int resource_id : existing_resources.keySet()) {
                Resource resource = existing_resources.get(resource_id);

                for (int crawl_log : resource.resource_type_log.keySet()) {
                    for (int resource_type_id : resource.resource_type_log.get(crawl_log).keySet()) {
                        String log_type = resource.resource_type_log.get(crawl_log).get(resource_type_id);

                        pst.setInt(1, resource.resource_id);
                        pst.setInt(2, resource_type_id);
                        pst.setString(3, log_type);
                        pst.setInt(4, crawl_log);

                        pst.addBatch();
                    }
                }
            }

            int[] rst = pst.executeBatch();
            CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.success.toString(), "writeResourceInstanceTypeLog", "success writing resource instance type logs", crawl_log_global, conn);
        } catch (Exception ex) {
            CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.error.toString(), "writeResourceInstanceTypeLog", "exception writing resource instances type logs: " + "\n" + ex.getMessage(), crawl_log_global, conn);
        }
    }

    /**
     * Write the resource instance logs for a specific dataset.
     *
     * @param dataset
     */
    public void writeResourceInstanceLogs(Dataset dataset) {
        PreparedStatement pst = null;
        try {
            pst = conn.prepareStatement("INSERT INTO resource_instance_log(resource_id,log_type,crawl_id) VALUES(?,?,?)");
            for (String resource_uri : dataset.resources.keySet()) {
                Resource resource = dataset.resources.get(resource_uri);
                for (int crawl_id : resource.crawl_logs.keySet()) {
                    pst.setInt(1, resource.resource_id);
                    pst.setString(2, resource.crawl_logs.get(crawl_id));
                    pst.setInt(3, crawl_id);

                    pst.addBatch();
                }
            }

            int[] rst = pst.executeBatch();
            Object[] res_uri_arr = dataset.resources.keySet().toArray();
            for (int i = 0; i < rst.length; i++) {
                Resource resource = dataset.resources.get(res_uri_arr[i].toString());
                if (rst[i] == 0) {
                    CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.error.toString(), "writeResourceInstanceLogs", "error writing resource instance logs: " + resource.resource_uri, crawl_log_global, conn);
                }
            }

            CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.success.toString(), "writeResourceInstanceLogs", "success writing resource instance logs", crawl_log_global, conn);
        } catch (Exception ex) {
            CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.error.toString(), "writeResourceInstanceLogs", "exception writing resource instances logs: " + "\n" + ex.getMessage(), crawl_log_global, conn);
        }
    }

    /**
     * Write the resource instance logs for a specific dataset.
     *
     * @param resource
     */
    public void writeResourceInstanceLogs(Resource resource) {
        PreparedStatement pst = null;
        try {
            pst = conn.prepareStatement("INSERT INTO resource_instance_log(resource_id,log_type,crawl_id) VALUES(?,?,?)");
            for (int crawl_id : resource.crawl_logs.keySet()) {
                pst.setInt(1, resource.resource_id);
                pst.setString(2, resource.crawl_logs.get(crawl_id));
                pst.setInt(3, crawl_id);

                pst.addBatch();
            }


            int[] rst = pst.executeBatch();
            for (int i = 0; i < rst.length; i++) {
                if (rst[i] == 0) {
                    CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.error.toString(), "writeResourceInstanceLogs", "error writing resource instance logs: " + resource.resource_uri, crawl_log_global, conn);
                }
            }
        } catch (Exception ex) {
            CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.error.toString(), "writeResourceInstanceLogs", "exception writing resource instances logs: " + "\n" + ex.getMessage(), crawl_log_global, conn);
        }
    }

    /**
     * Write the resource instance logs for a specific dataset.
     *
     * @param resource_instance_logs
     */
    public void writeResourceInstanceLogs(Map<Integer, Map<Integer, String>> resource_instance_logs) {
        PreparedStatement pst = null;
        try {
            pst = conn.prepareStatement("INSERT INTO resource_instance_log(resource_id,log_type,crawl_id) VALUES(?,?,?)");
            for (int resource_id : resource_instance_logs.keySet()) {
                for (int crawl_id : resource_instance_logs.get(resource_id).keySet()) {
                    pst.setInt(1, resource_id);
                    pst.setString(2, resource_instance_logs.get(resource_id).get(crawl_id));
                    pst.setInt(3, crawl_id);

                    pst.addBatch();
                }
            }

            int[] rst = pst.executeBatch();
            Object[] res_uri_arr = resource_instance_logs.keySet().toArray();
            for (int i = 0; i < rst.length; i++) {
                if (rst[i] == 0) {
                    CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.error.toString(), "writeResourceInstanceLogs", "error writing resource instance logs: " + res_uri_arr[i], crawl_log_global, conn);
                }
            }
            CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.success.toString(), "writeResourceInstanceLogs", "success writing resource instance logs", crawl_log_global, conn);
        } catch (Exception ex) {
            CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.error.toString(), "writeResourceInstanceLogs", "exception writing resource instances logs: " + "\n" + ex.getMessage(), crawl_log_global, conn);
        }
    }

    /**
     * Writes the resource values for all resource instances for a particular
     * dataset.
     *
     * @param dataset
     */
    public void writeResourceInstanceValues(Dataset dataset) {
        PreparedStatement pst = null;
        try {
            pst = conn.prepareStatement("INSERT INTO resource_values(resource_id,property_uri,value) VALUES(?,?,?)", Statement.RETURN_GENERATED_KEYS);
            for (String resource_uri : dataset.resources.keySet()) {
                Resource resource = dataset.resources.get(resource_uri);
                for (ResourceValue resource_value : resource.values) {
                    pst.setInt(1, resource.resource_id);
                    pst.setString(2, resource_value.datatype_property);
                    pst.setString(3, resource_value.value);

                    pst.addBatch();
                }

                int[] rst = pst.executeBatch();
                //get the generated auto increment keys.
                ResultSet rs_keys = pst.getGeneratedKeys();

                for (int i = 0; i < rst.length; i++) {
                    ResourceValue resource_value = resource.values.get(i);
                    if (rst[i] != 0) {
                        rs_keys.next();
                        resource_value.resource_value_id = rs_keys.getInt(1);
                    } else {
                        CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.error.toString(), "writeResourceInstanceValues", "error writing resource instance values: " + resource.resource_uri, crawl_log_global, conn);
                    }
                }
            }

            CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.success.toString(), "writeResourceInstanceValues", "success writing resource instance values", crawl_log_global, conn);

        } catch (Exception ex) {
            CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.error.toString(), "writeResourceInstanceValues", "exception writing resource instances values: " + "\n" + ex.getMessage(), crawl_log_global, conn);
        }
    }

    /**
     * Writes the resource values for all resource instances for a particular
     * dataset.
     *
     * @param resource
     */
    public void writeResourceInstanceValues(Set<Integer> updated_resources, Map<Integer, Resource> resources) {
        PreparedStatement pst = null;
        try {
            pst = conn.prepareStatement("INSERT INTO resource_values(resource_id,property_uri,value) VALUES(?,?,?)");

            for (int resource_id : updated_resources) {
                Resource resource = resources.get(resource_id);
                for (ResourceValue resource_value : resource.values) {
                    if (resource_value.isValid) {
                        pst.setInt(1, resource_id);
                        pst.setString(2, resource_value.datatype_property);
                        pst.setString(3, resource_value.value);

                        pst.addBatch();
                    }
                }
            }

            int[] rst = pst.executeBatch();

            CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.success.toString(), "writeResourceInstanceValues", "error writing resource instance values", crawl_log_global, conn);
        } catch (Exception ex) {
            CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.error.toString(), "writeResourceInstanceValues", "exception writing resource instances values: " + "\n" + ex.getMessage(), crawl_log_global, conn);
        }

    }

    /**
     * Writes the resource values for all resource instances for a particular
     * dataset.
     *
     * @param resource
     */
    public void writeResourceInstanceValues(Resource resource) {
        PreparedStatement pst = null;
        try {
            pst = conn.prepareStatement("INSERT INTO resource_values(resource_id,property_uri,value) VALUES(?,?,?)", Statement.RETURN_GENERATED_KEYS);

            for (ResourceValue resource_value : resource.values) {
                if (resource_value.isValid) {
                    pst.setInt(1, resource.resource_id);
                    pst.setString(2, resource_value.datatype_property);
                    pst.setString(3, resource_value.value);

                    pst.addBatch();
                }
            }

            int[] rst = pst.executeBatch();
            //get the generated auto increment keys.
            ResultSet rs_keys = pst.getGeneratedKeys();

            for (int i = 0; i < rst.length; i++) {
                ResourceValue resource_value = resource.values.get(i);
                if (rst[i] != 0) {
                    rs_keys.next();
                    resource_value.resource_value_id = rs_keys.getInt(1);
                } else {
                    CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.error.toString(), "writeResourceInstanceValues", "error writing resource instance values: " + resource.resource_uri, crawl_log_global, conn);
                }
            }

            CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.success.toString(), "writeResourceInstanceValues", "success writing resource instance values: " + resource.resource_uri, crawl_log_global, conn);
        } catch (Exception ex) {
            CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.error.toString(), "writeResourceInstanceValues", "exception writing resource instances values: " + "\n" + ex.getMessage(), crawl_log_global, conn);
        }
    }

    /**
     * Writes the resource values log for resource instances from a particular
     * dataset.
     *
     * @param dataset
     */
    public void writeResourceInstanceValuesLog(Dataset dataset) {
        PreparedStatement pst = null;
        try {
            pst = conn.prepareStatement("INSERT INTO resource_value_log(resource_value_id,log_type,crawl_id) VALUES(?,?,?)", Statement.RETURN_GENERATED_KEYS);
            for (String resource_uri : dataset.resources.keySet()) {
                Resource resource = dataset.resources.get(resource_uri);
                for (ResourceValue resource_value : resource.values) {
                    for (int crawl_id : resource_value.log_entry.keySet()) {
                        pst.setInt(1, resource_value.resource_value_id);
                        pst.setString(2, resource_value.log_entry.get(crawl_id));
                        pst.setInt(3, crawl_id);

                        pst.addBatch();
                    }
                }
            }

            int[] rst = pst.executeBatch();
            Object[] res_uri_arr = dataset.resources.keySet().toArray();
            for (int i = 0; i < rst.length; i++) {
                Resource resource = dataset.resources.get(res_uri_arr[i].toString());
                if (rst[i] == 0) {
                    CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.error.toString(), "writeResourceInstanceValuesLog", "error writing resource instance values log: " + resource.resource_uri, crawl_log_global, conn);
                }
            }

            CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.success.toString(), "writeResourceInstanceValuesLog", "success writing resource instance values log", crawl_log_global, conn);
        } catch (Exception ex) {
            CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.error.toString(), "writeResourceInstanceValuesLog", "exception writing resource instances values log: " + "\n" + ex.getMessage(), crawl_log_global, conn);
        }
    }

    /**
     * Writes the resource values log for resource instances from a particular
     * dataset.
     *
     * @param resource
     * @param crawl_log
     */
    public void writeResourceInstanceValuesLog(Resource resource, CrawlLog crawl_log) {
        PreparedStatement pst = null;
        try {
            pst = conn.prepareStatement("INSERT INTO resource_value_log(resource_value_id,log_type,crawl_id) VALUES(?,?,?)", Statement.RETURN_GENERATED_KEYS);
            for (ResourceValue resource_value : resource.values) {
                if (resource_value.isValid) {
                    pst.setInt(1, resource_value.resource_value_id);
                    pst.setString(2, Properties.crawl_log_status.added.name());
                    pst.setInt(3, crawl_log.crawl_id);

                    pst.addBatch();
                } else {
                    pst.setInt(1, resource_value.resource_value_id);
                    pst.setString(2, Properties.crawl_log_status.deleted.name());
                    pst.setInt(3, crawl_log.crawl_id);

                    pst.addBatch();
                }
            }


            int[] rst = pst.executeBatch();
            for (int i = 0; i < rst.length; i++) {
                if (rst[i] == 0) {
                    CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.error.toString(), "writeResourceInstanceValuesLog", "error writing resource instance values log: " + resource.resource_uri, crawl_log_global, conn);
                }
            }

            CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.success.toString(), "writeResourceInstanceValuesLog", "success writing resource instance values log: " + resource.resource_uri, crawl_log_global, conn);
        } catch (Exception ex) {
            CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.error.toString(), "writeResourceInstanceValuesLog", "exception writing resource instances values log: " + "\n" + ex.getMessage(), crawl_log_global, conn);
        }
    }
//=============================================================================================================================================
//=============================================================================================================================================
//=============================================================================================================================================
//*********************************************************************************************************************************************
//=============================================================================================================================================
//=============================================================================================================================================
//=============================================== Load operations from the database ==========================================================
//=============================================================================================================================================

    /**
     * Returns the dataset metadata.
     *
     * @return
     */
    public Map<String, Dataset> loadDatasetMetadata(Connection conn) {
        Map<String, Dataset> dataset_metadata = new TreeMap<String, Dataset>();
        PreparedStatement pst = null;
        try {
            pst = conn.prepareStatement("SELECT d.dataset_id, d.dataset_name, d.dataset_description, d.dataset_url, d.dataset_id_datahub, ds.namespace_id, sc.namespace_uri "
                    + "FROM dataset_namespaces ds, dataset d, namespace sc WHERE ds.dataset_id = d.dataset_id AND sc.namespace_id = ds.namespace_id;");

            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                Dataset dataset = dataset_metadata.get(rs.getString("datset_id_datahub"));
                dataset = dataset == null ? new Dataset() : dataset;
                dataset_metadata.put(rs.getString("dataset_id_datahub"), dataset);

                dataset.id = rs.getInt("dataset_id");
                dataset.name = rs.getString("dataset_name");
                dataset.notes = rs.getString("dataset_description");
                dataset.url = rs.getString("dataset_url");
                dataset.dataset_id_datahub = rs.getString("dataset_id_datahub");

                Namespaces namespace = dataset.namespaces.get(rs.getString("namespace_uri"));
                namespace = namespace == null ? new Namespaces() : namespace;
                dataset.namespaces.put(rs.getString("namespace_uri"), namespace);

                namespace.namespace_id = rs.getInt("namespace_id");
                namespace.namespace_uri = rs.getString("namespace_uri");
            }
            CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.success.toString(), "loadDatasetMetadata", "success while loading dataset metadata", crawl_log_global, conn);
            return dataset_metadata;
        } catch (Exception ex) {
            CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.exception.toString(), "loadDatasetMetadata", "exception while loading dataset metadata \n" + ex.getMessage(), crawl_log_global, conn);
        }
        return null;
    }

    /**
     * Loads the used namespaces from the crawled datasets, along with their
     * particular types and associations with datasets.
     *
     * @return
     */
    public Map<Integer, Namespaces> loadSchemas(Connection conn) {
        Map<Integer, Namespaces> namespaces = new TreeMap<Integer, Namespaces>();

        PreparedStatement pst = null;
        try {
            pst = conn.prepareStatement("SELECT sc.namespace_id, sc.namespace_uri, sci.namespace_value_uri, sci.namespace_value_type, "
                    + "scl.log_type, c.crawl_id, scl.dataset_id, dscl.log_type as dataset_namespace_log_type "
                    + "FROM namespace sc, namespaces_instances sci, namespaces_log scl, dataset_namespaces_log dscl, dataset d, crawl_log c "
                    + "WHERE sc.namespace_id = sci.namespace_id AND sc.namespace_id = scl.namespace_id "
                    + "AND sci.namespace_value_uri = scl.namespace_value_uri AND sc.namespace_id = dscl.namespace_id "
                    + "AND d.dataset_id = dscl.dataset_id AND c.crawl_id = dscl.crawl_id AND scl.crawl_id = c.crawl_id");


            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                Namespaces namespace = namespaces.get(rs.getInt("namespace_id"));
                namespace = namespace == null ? new Namespaces() : namespace;
                namespaces.put(rs.getInt("namespace_id"), namespace);

                namespace.namespace_uri = rs.getString("namespace_uri");

                //add the instances
                NamespaceInstance namespace_instance = new NamespaceInstance();
                namespace_instance.namespace_value_uri = rs.getString("namespace_value_uri");
                namespace_instance.isProperty = rs.getBoolean("namespace_value_type");
                namespace.instances.add(namespace_instance);

                //add the crawl logs.
                Map<Integer, Map<String, String>> sub_namespace_crawl_logs = namespace.namespace_instance_crawl_logs.get(rs.getInt("crawl_id"));
                sub_namespace_crawl_logs = sub_namespace_crawl_logs == null ? new HashMap<Integer, Map<String, String>>() : sub_namespace_crawl_logs;
                namespace.namespace_instance_crawl_logs.put(rs.getInt("crawl_id"), sub_namespace_crawl_logs);

                Map<String, String> dataset_sub_namespace_crawl_logs = sub_namespace_crawl_logs.get(rs.getInt("dataset_id"));
                dataset_sub_namespace_crawl_logs = dataset_sub_namespace_crawl_logs == null ? new TreeMap<String, String>() : dataset_sub_namespace_crawl_logs;
                sub_namespace_crawl_logs.put(rs.getInt("dataset_id"), dataset_sub_namespace_crawl_logs);

                dataset_sub_namespace_crawl_logs.put(rs.getString("namespace_value_uri"), rs.getString("log_type"));

                //add the dataset_namespaces crawl logs
                Map<Integer, String> dataset_namespace_log_crawl_log = namespace.dataset_namespace_crawl_logs.get(rs.getInt("crawl_id"));
                dataset_namespace_log_crawl_log = dataset_namespace_log_crawl_log == null ? new TreeMap<Integer, String>() : dataset_namespace_log_crawl_log;
                namespace.dataset_namespace_crawl_logs.put(rs.getInt("crawl_id"), dataset_namespace_log_crawl_log);

                dataset_namespace_log_crawl_log.put(rs.getInt("dataset_id"), rs.getString("dataset_namespace_log_type"));
            }
            CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.success.toString(), "loadSchemas", "success while loading dataset namespaces.", crawl_log_global, conn);
            return namespaces;

        } catch (Exception ex) {
            CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.exception.toString(), "loadSchemas", "exception while loading dataset namespaces \n" + ex.getMessage(), crawl_log_global, conn);
        }
        return null;
    }

    /**
     * Loads only the set of resource URI and their IDs for a particular dataset
     * and a resource type.
     *
     * @param dataset
     * @param resource_type
     * @return
     */
    public Map<Integer, Entry<String, Map<Integer, String>>> loadDatasetResourcesURI(Dataset dataset, ResourceType resource_type) {
        Map<Integer, Entry<String, Map<Integer, String>>> dataset_resource_uri = new TreeMap<Integer, Entry<String, Map<Integer, String>>>();

        PreparedStatement pst = null;
        try {
            pst = conn.prepareStatement("SELECT ri.resource_id, ri.resource_uri, rl.crawl_id, rl.log_type " +
                    "FROM resource_instances ri, resource_instance_log rl, resource_instance_type rit " +
                    "WHERE ri.dataset_id=? AND ri.resource_id=rit.resource_id AND rit.type_id=? AND ri.resource_id = rl.resource_id");
            pst.setInt(1, dataset.id);
            pst.setInt(2, resource_type.resource_type_id);

            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                Entry<String, Map<Integer, String>> resource_entry = new AbstractMap.SimpleEntry<String, Map<Integer, String>>(rs.getString("resource_uri"), new TreeMap<Integer, String>());
                dataset_resource_uri.put(rs.getInt("resource_id"), resource_entry);

                Map<Integer, String> sub_resource_entry = resource_entry.getValue();
                sub_resource_entry.put(rs.getInt("crawl_id"), rs.getString("log_type"));
            }
            CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.success.toString(), "loadDatasetResourcesURI", "success while loading resources for dataset " + dataset.dataset_id_datahub
                    + " and type " + resource_type.type_uri, crawl_log_global, conn);
            return dataset_resource_uri;

        } catch (Exception ex) {
            CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.exception.toString(), "loadDatasetResourcesURI", "exception while loading resources for dataset " + dataset.dataset_id_datahub
                    + " and type " + resource_type.type_uri + "\n" + ex.getMessage(), crawl_log_global, conn);
        }
        return null;
    }

    /**
     * Loads the full set of resource values for all resources in a dataset for
     * a respective resource type. The resource values represent as well the
     * changes that have occurred for a particular resource instance.
     *
     * @param dataset
     * @param resource_type
     * @return
     */
    public Map<Integer, Resource> loadDatasetResourcesComplete(Dataset dataset, ResourceType resource_type) {
        Map<Integer, Resource> resources = new TreeMap<Integer, Resource>();
        PreparedStatement pst = null;
        try {
            pst = conn.prepareStatement("SELECT ri.resource_id, ri.resource_uri, rv.resource_value_id, rv.property_uri, rv.value, dl.log_type, dl.crawl_id " +
                    "FROM resource_instances ri, resource_instance_type rti, resource_values rv, resource_value_log dl WHERE ri.resource_id = rti.resource_id AND ri.dataset_id=? AND rti.type_id=?" +
                    " AND ri.resource_id = rv.resource_id AND dl.resource_value_id=rv.resource_value_id");
            pst.setInt(1, dataset.id);
            pst.setInt(2, resource_type.resource_type_id);

            ResultSet rs = pst.executeQuery();
            Map<Integer, Map<Integer, ResourceValue>> added_res_values = new HashMap<Integer, Map<Integer, ResourceValue>>();

            while (rs.next()) {
                Resource resource = resources.get(rs.getInt("resource_id"));
                resource = resource == null ? new Resource() : resource;
                resources.put(rs.getInt("resource_id"), resource);

                resource.resource_id = rs.getInt("resource_id");
                resource.resource_uri = rs.getString("resource_uri");
                resource.types.put(resource_type.type_uri, resource_type);

                //load the values.
                Map<Integer, ResourceValue> sub_added_res_values = added_res_values.get(resource.resource_id);
                sub_added_res_values = sub_added_res_values == null ? new HashMap<Integer, ResourceValue>() : sub_added_res_values;
                added_res_values.put(resource.resource_id, sub_added_res_values);

                ResourceValue resource_value = sub_added_res_values.get(rs.getInt("resource_value_id"));
                resource_value = resource_value == null ? new ResourceValue() : resource_value;
                sub_added_res_values.put(rs.getInt("resource_value_id"), resource_value);

                resource_value.resource_value_id = rs.getInt("resource_value_id");
                resource_value.datatype_property = rs.getString("property_uri");
                resource_value.value = rs.getString("value");

                resource_value.log_entry.put(rs.getInt("crawl_id"), rs.getString("log_type"));
            }

            for (int resource_id : added_res_values.keySet()) {
                for (int res_value_id : added_res_values.get(resource_id).keySet()) {
                    resources.get(resource_id).values.add(added_res_values.get(resource_id).get(res_value_id));
                }
            }

            CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.success.toString(), "loadDatasetResourcesComplete", "success while loading resources for dataset " + dataset.dataset_id_datahub
                    + " and type " + resource_type.type_uri, crawl_log_global, conn);
            return resources;

        } catch (Exception ex) {
            CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.exception.toString(), "loadDatasetResourcesComplete", "exception while loading resources for dataset " + dataset.dataset_id_datahub
                    + " and type " + resource_type.type_uri + "\n" + ex.getMessage(), crawl_log_global, conn);
        }
        return null;
    }

    /**
     * Load a particular namespace based on its URI.
     *
     * @param namespace_uri
     * @return
     */
    public Namespaces loadSchema(String namespace_uri) {
        PreparedStatement pst = null;
        try {
            pst = conn.prepareStatement("SELECT namespace_id FROM namespace WHERE namespace_uri = ?");
            pst.setString(1, namespace_uri);

            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                Namespaces namespace = new Namespaces();
                namespace.namespace_id = rs.getInt("namespace_id");
                namespace.namespace_uri = namespace_uri;

                CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.success.toString(), "loadSchema", "success while loading namespaces " + namespace_uri + "\n", crawl_log_global, conn);
                return namespace;
            }
        } catch (Exception ex) {
            CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.exception.toString(), "loadSchema", "exception while loading namespaces " + namespace_uri + "\n" + ex.getMessage(), crawl_log_global, conn);
        }
        return null;
    }

    /**
     * Loads the resource types for all datasets along with their corresponding
     * logs for the specific crawls.
     *
     * @return
     */
    public Map<Integer, ResourceType> loadResourceTypesFull(Connection conn) {
        Map<Integer, ResourceType> resource_types = new TreeMap<Integer, ResourceType>();
        PreparedStatement pst = null;
        try {
            pst = conn.prepareStatement("SELECT rt.type_id, rt.type_uri, rt.namespace_id, sc.namespace_uri, rtl.resource_id, rtl.log_type, rtl.crawl_id, ri.dataset_id "
                    + "FROM resource_types rt, resource_type_log rtl, resource_instances ri, namespace sc "
                    + "WHERE rt.type_id = rtl.type_id AND rtl.resource_id = ri.resource_id AND sc.namespace_id = rt.namespace_id;");

            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                ResourceType resource_type = resource_types.get(rs.getInt("type_id"));
                resource_type = resource_type == null ? new ResourceType() : resource_type;
                resource_types.put(rs.getInt("type_id"), resource_type);

                resource_type.resource_type_id = rs.getInt("type_id");
                resource_type.type_uri = rs.getString("type_uri");
                //set the namespace information
                resource_type.namespace.namespace_id = rs.getInt("namespace_id");
                resource_type.namespace.namespace_uri = rs.getString("namespace_uri");

                Map<Integer, Map<Integer, String>> crawl_sub_res_type_crawl_logs = resource_type.resource_type_crawl_logs.get(rs.getInt("crawl_id"));
                crawl_sub_res_type_crawl_logs = crawl_sub_res_type_crawl_logs == null ? new TreeMap<Integer, Map<Integer, String>>() : crawl_sub_res_type_crawl_logs;
                resource_type.resource_type_crawl_logs.put(rs.getInt("crawl_id"), crawl_sub_res_type_crawl_logs);

                Map<Integer, String> dataset_crawl_sub_res_type_crawl_logs = crawl_sub_res_type_crawl_logs.get(rs.getInt("dataset_id"));
                dataset_crawl_sub_res_type_crawl_logs = dataset_crawl_sub_res_type_crawl_logs == null ? new TreeMap<Integer, String>() : dataset_crawl_sub_res_type_crawl_logs;
                crawl_sub_res_type_crawl_logs.put(rs.getInt("dataset_id"), dataset_crawl_sub_res_type_crawl_logs);

                dataset_crawl_sub_res_type_crawl_logs.put(rs.getInt("resource_id"), rs.getString("log_type"));
            }
            CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.success.toString(), "loadResourceTypesFull", "success while loading resource types ", crawl_log_global, conn);
            return resource_types;
        } catch (Exception ex) {
            CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.exception.toString(), "loadResourceTypesFull", "exception while loading resource types \n" + ex.getMessage(), crawl_log_global, conn);
        }
        return null;
    }

    /**
     * Loads the resource types for all datasets along with their corresponding
     * logs for the specific crawls.
     *
     * @return
     */
    public Map<Integer, ResourceType> loadResourceTypes(Connection conn) {
        Map<Integer, ResourceType> resource_types = new TreeMap<Integer, ResourceType>();
        PreparedStatement pst = null;
        try {
            pst = conn.prepareStatement("SELECT rt.type_id, rt.type_uri, rt.namespace_id, sc.namespace_uri "
                    + "FROM resource_types rt, namespaces sc WHERE sc.namespace_id = rt.namespace_id;");

            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                ResourceType resource_type = resource_types.get(rs.getInt("type_id"));
                resource_type = resource_type == null ? new ResourceType() : resource_type;
                resource_types.put(rs.getInt("type_id"), resource_type);

                resource_type.resource_type_id = rs.getInt("type_id");
                resource_type.type_uri = rs.getString("type_uri");
                //set the namespace information
                resource_type.namespace.namespace_id = rs.getInt("namespace_id");
                resource_type.namespace.namespace_uri = rs.getString("namespace_uri");
            }
            CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.success.toString(), "loadResourceTypes", "success while loading resource types ", crawl_log_global, conn);
            return resource_types;
        } catch (Exception ex) {
            CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.exception.toString(), "loadResourceTypes", "exception while loading resource types \n" + ex.getMessage(), crawl_log_global, conn);
        }
        return null;
    }

    /**
     * Checks if the dataset metadata is already written, if so, check if the
     * metadata is up to date.
     *
     * @param dataset
     * @return
     */
    public Entry<Integer, Boolean> isDatasetMetadataUpdated(Dataset dataset) {
        PreparedStatement pst = null;

        try {
            pst = conn.prepareStatement("SELECT dataset_id, dataset_name, dataset_description, dataset_url FROM dataset WHERE dataset_id_datahub=?");
            pst.setString(1, dataset.dataset_id_datahub);

            ResultSet rst = pst.executeQuery();
            int dataset_id = -1;
            boolean isUptoDate = true;
            if (rst.next()) {
                dataset_id = rst.getInt("dataset_id");
                isUptoDate &= dataset.name.equals(rst.getString("dataset_name")) & dataset.name.equals(rst.getString("dataset_description")) & dataset.name.equals(rst.getString("dataset_url"));
            }

            Entry<Integer, Boolean> entry = new AbstractMap.SimpleEntry<Integer, Boolean>(dataset_id, isUptoDate);
            return entry;
        } catch (Exception ex) {
            CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.exception.toString(), "writeDataset", "exception reading metadata for" + dataset.id + "\n " + ex.getMessage(), crawl_log_global, conn);
        }

        CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.error.toString(), "writeDataset", "error reading metadata for" + dataset.id, crawl_log_global, conn);
        return null;
    }

    //*********************************************************************************************************************************************
    //=============================================================================================================================================
    //=============================================================================================================================================
    //=============================================== Load of data for crawling ===================================================================
    //=============================================================================================================================================

    /**
     * Loads the set of already existing namespaces from the crawled data.
     *
     * @return
     */
    public Set<String> loadSchemaURI(Connection conn) {
        Set<String> namespaces = new HashSet<String>();
        PreparedStatement pst = null;
        try {
            pst = conn.prepareStatement("SELECT namespace_uri FROM namespace");

            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                namespaces.add(rs.getString("namespace_uri"));
            }
            CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.success.toString(), "loadSchemaURI", "success while loading namespaces URIs", crawl_log_global, conn);
            return namespaces;
        } catch (Exception ex) {
            CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.exception.toString(), "loadSchemaURI", "exception while loading namespaces URIs \n" + ex.getMessage(), crawl_log_global, conn);
        }

        return null;
    }

    /**
     * Loads the set of already existing namespaces from the crawled data for a
     * specific dataset.
     *
     * @return
     */
    public Map<Integer, Namespaces> loadDatasetSchemaURI(Dataset dataset) {
        Map<Integer, Namespaces> namespaces = new HashMap<Integer, Namespaces>();
        Map<String, NamespaceInstance> sci_list = new HashMap<String, NamespaceInstance>();

        PreparedStatement pst = null;
        try {
            pst = conn.prepareStatement("SELECT ds.namespace_id, sc.namespace_uri, sci.namespace_value_uri, sci.namespace_value_type, scil.log_type, scil.crawl_id, scil.dataset_id " +
                    "FROM dataset_namespaces ds, namespace sc, namespaces_instances sci, namespaces_instance_log scil " +
                    "WHERE ds.namespace_id = sc.namespace_id AND sc.namespace_id=sci.namespace_id AND sci.namespace_id = scil.namespace_id AND ds.dataset_id=?;");
            pst.setInt(1, dataset.id);

            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                Namespaces namespace = namespaces.get(rs.getInt("namespace_id"));
                namespace = namespace == null ? new Namespaces() : namespace;
                namespaces.put(rs.getInt("namespace_id"), namespace);

                namespace.namespace_uri = rs.getString("namespace_uri");

                if (!sci_list.containsKey(rs.getString("namespace_value_uri"))) {
                    NamespaceInstance sci = sci_list.get(rs.getString("namespace_value_uri"));
                    sci = sci == null ? new NamespaceInstance() : sci;
                    sci_list.put(rs.getString("namespace_value_uri"), sci);

                    sci.namespace_value_uri = rs.getString("namespace_value_uri");
                    sci.isProperty = rs.getBoolean("namespace_value_type");
                    namespace.instances.add(sci);
                }

                Map<Integer, Map<String, String>> sub_schi_log = namespace.namespace_instance_crawl_logs.get(rs.getInt("crawl_id"));
                sub_schi_log = sub_schi_log == null ? new HashMap<Integer, Map<String, String>>() : sub_schi_log;
                namespace.namespace_instance_crawl_logs.put(rs.getInt("crawl_id"), sub_schi_log);

                Map<String, String> dataset_schi_log = sub_schi_log.get(dataset.id);
                dataset_schi_log = dataset_schi_log == null ? new HashMap<String, String>() : dataset_schi_log;
                sub_schi_log.put(dataset.id, dataset_schi_log);

                dataset_schi_log.put(rs.getString("namespace_value_uri"), rs.getString("log_type"));
            }
            CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.success.toString(), "loadDatasetSchemaURI", "success while loading namespaces URIs for dataset: " + dataset.dataset_id_datahub, crawl_log_global, conn);
        } catch (Exception ex) {
            CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.exception.toString(), "loadDatasetSchemaURI", "exception while loading namespaces URIs  for dataset: " + dataset.dataset_id_datahub + "\n" + ex.getMessage(), crawl_log_global, conn);
        }

        return namespaces;
    }

    /**
     * Loads existing resource types with their specific namespaces.
     *
     * @return
     */
    public Map<String, ResourceType> loadExistingResourceTypeURI(Connection conn) {
        Map<String, ResourceType> existing_resource_types = new TreeMap<String, ResourceType>();
        PreparedStatement pst = null;
        try {
            pst = conn.prepareStatement("SELECT rt.type_id, rt.type_uri, sc.namespace_id, sc.namespace_uri FROM resource_types rt, namespace sc WHERE rt.namespace_id = sc.namespace_id ");

            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                ResourceType resource_type = existing_resource_types.get(rs.getString("type_uri"));
                resource_type = resource_type == null ? new ResourceType() : resource_type;
                existing_resource_types.put(rs.getString("type_uri"), resource_type);

                resource_type.resource_type_id = rs.getInt("type_id");
                resource_type.type_uri = rs.getString("type_uri");
                resource_type.namespace.namespace_id = rs.getInt("namespace_id");
                resource_type.namespace.namespace_uri = rs.getString("namespace_uri");
            }
            CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.success.toString(), "loadExistingResourceTypeURI", "success while loading resource types", crawl_log_global, conn);
            return existing_resource_types;
        } catch (Exception ex) {
            CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.exception.toString(), "loadExistingResourceTypeURI", "exception while loading resource types \n" + ex.getMessage(), crawl_log_global, conn);
        }

        return null;
    }

    /**
     * Loads the set of resource types that are part of a dataset through their
     * corresponding namespaces registered at the table dataset_namespaces.
     *
     * @param dataset
     * @return
     */
    public Map<String, ResourceType> loadDatasetResourceTypes(Dataset dataset) {
        Map<String, ResourceType> dataset_resource_types = new TreeMap<String, ResourceType>();
        PreparedStatement pst = null;
        try {
            pst = conn.prepareStatement("SELECT rt.type_id, rt.type_uri, sc.namespace_id, sc.namespace_uri "
                    + "FROM resource_types rt, namespace sc, dataset_namespaces ds WHERE rt.namespace_id = sc.namespace_id AND ds.namespace_id = sc.namespace_id AND ds.dataset_id=?");
            pst.setInt(1, dataset.id);

            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                ResourceType resource_type = dataset_resource_types.get(rs.getString("type_uri"));
                resource_type = resource_type == null ? new ResourceType() : resource_type;
                dataset_resource_types.put(rs.getString("type_uri"), resource_type);

                resource_type.resource_type_id = rs.getInt("type_id");
                resource_type.type_uri = rs.getString("type_uri");
                resource_type.namespace.namespace_id = rs.getInt("namespace_id");
                resource_type.namespace.namespace_uri = rs.getString("namespace_uri");
            }
            CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.success.toString(), "loadDatasetResourceTypes", "success while loading resource types for dataset: " + dataset.dataset_id_datahub, crawl_log_global, conn);
            return dataset_resource_types;
        } catch (Exception ex) {
            CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.exception.toString(), "loadDatasetResourceTypes", "exception while loading resource types for dataset: " + dataset.dataset_id_datahub + "\n" + ex.getMessage(), crawl_log_global, conn);
        }

        return null;
    }

    /**
     * Loads only the set of resource URI and their IDs for a particular dataset
     * and a resource type.
     *
     * @param dataset
     * @param resource_type
     * @return
     */
    public Map<Integer, Resource> loadDatasetResourceValues(Dataset dataset, ResourceType resource_type, Set<String> update_resource_uris) {
        Map<Integer, Resource> resources = new TreeMap<Integer, Resource>();

        PreparedStatement pst = null;
        try {
            pst = conn.prepareStatement("SELECT ri.resource_id, ri.resource_uri, ril.crawl_id as resource_instance_crawl_id, ril.log_type as resource_instance_log_type, " +
                    "rv.resource_value_id, rvl.crawl_id as resource_value_crawl_id, rvl.log_type as resource_value_log_type, " +
                    "rv.property_uri as resource_property, rv.value as resource_property_value " +
                    "FROM resource_instances ri, resource_instance_log ril, resource_values rv, resource_value_log rvl, resource_instance_type rit " +
                    "WHERE ri.dataset_id=? AND ri.resource_id = rit.resource_id AND rit.type_id=? AND ri.resource_id = ril.resource_id AND " +
                    "rv.resource_id = ri.resource_id AND rv.resource_value_id = rvl.resource_value_id AND rv.resource_id = ri.resource_id");
            pst.setInt(1, dataset.id);
            pst.setInt(2, resource_type.resource_type_id);

            ResultSet rs = pst.executeQuery();

            Map<Integer, Map<Integer, ResourceValue>> added_res_values = new HashMap<Integer, Map<Integer, ResourceValue>>();

            while (rs.next()) {
                if (!update_resource_uris.contains(rs.getString("resource_uri"))) {
                    continue;
                }

                Resource resource = resources.get(rs.getInt("resource_id"));
                resource = resource == null ? new Resource() : resource;
                resources.put(rs.getInt("resource_id"), resource);

                resource.resource_id = rs.getInt("resource_id");
                resource.resource_uri = rs.getString("resource_uri");
                resource.types.put(resource_type.type_uri, resource_type);

                //load the resource instance crawl logs.
                resource.crawl_logs.put(rs.getInt("resource_instance_crawl_id"), rs.getString("resource_instance_log_type"));

                //load the values.
                Map<Integer, ResourceValue> sub_added_res_values = added_res_values.get(resource.resource_id);
                sub_added_res_values = sub_added_res_values == null ? new HashMap<Integer, ResourceValue>() : sub_added_res_values;
                added_res_values.put(resource.resource_id, sub_added_res_values);

                ResourceValue resource_value = sub_added_res_values.get(rs.getInt("resource_value_id"));
                resource_value = resource_value == null ? new ResourceValue() : resource_value;
                sub_added_res_values.put(rs.getInt("resource_value_id"), resource_value);

                resource_value.resource_value_id = rs.getInt("resource_value_id");
                resource_value.datatype_property = rs.getString("resource_property");
                resource_value.value = rs.getString("resource_property_value");

                resource_value.log_entry.put(rs.getInt("resource_value_crawl_id"), rs.getString("resource_value_log_type"));
            }

            //to avoid duplicates add the values using map datastructures.
            for (int resource_id : added_res_values.keySet()) {
                Resource resource = resources.get(resource_id);
                for (int resource_value_id : added_res_values.get(resource_id).keySet()) {
                    resource.values.add(added_res_values.get(resource_id).get(resource_value_id));
                }
            }

            CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.success.toString(), "loadDatasetResourceValues", "success while loading resources for dataset " + dataset.dataset_id_datahub
                    + " and type " + resource_type.type_uri, crawl_log_global, conn);
            return resources;

        } catch (Exception ex) {
            CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.exception.toString(), "loadDatasetResourceValues", "exception while loading resources for dataset " + dataset.dataset_id_datahub
                    + " and type " + resource_type.type_uri + "\n" + ex.getMessage(), crawl_log_global, conn);
        }
        return null;
    }

    /**
     * Loads only the set of resource URI and their IDs for a particular dataset and the corresponding resource types and the logs.
     *
     * @param dataset
     * @return
     */
    public Map<Integer, Resource> loadDatasetResourceTypes(Dataset dataset, Set<String> update_resource_uris) {
        Map<Integer, Resource> resources = new TreeMap<Integer, Resource>();

        PreparedStatement pst = null;
        try {
            pst = conn.prepareStatement("SELECT ri.resource_id, ri.resource_uri, ril.crawl_id as resource_instance_crawl_id, ril.log_type as resource_instance_log_type, " +
                    "  rt.type_uri AS resource_type_uri, ritl.log_type resource_type_log, ritl.crawl_id as resource_type_crawl_id " +
                    "FROM  resource_instances ri, resource_instance_log ril, resource_instance_type rit, resource_instance_type_log ritl, resource_types rt " +
                    "WHERE ri.dataset_id = ? AND ri.resource_id = rit.resource_id AND ri.resource_id = ril.resource_id AND rit.resource_id = ritl.resource_id AND rit.type_id = rt.type_id");
            pst.setInt(1, dataset.id);

            ResultSet rs = pst.executeQuery();

            Map<Integer, Map<Integer, ResourceValue>> added_res_values = new HashMap<Integer, Map<Integer, ResourceValue>>();

            while (rs.next()) {
                Resource resource = resources.get(rs.getInt("resource_id"));
                resource = resource == null ? new Resource() : resource;
                resources.put(rs.getInt("resource_id"), resource);

                resource.resource_id = rs.getInt("resource_id");
                resource.resource_uri = rs.getString("resource_uri");
                ResourceType type = resource.types.get(rs.getString("resource_type_uri"));
                type = type == null ? new ResourceType() : type;
                resource.types.put(rs.getString("resource_type_uri"), type);
                type.type_uri = rs.getString("resource_type_uri");

                //Map<Integer, Map<Integer, Map<Integer, String>>>
                Map<Integer, Map<Integer, String>> type_log = type.resource_type_crawl_logs.get(rs.getInt("resource_type_crawl_id"));
                type_log = type_log == null ? new HashMap<Integer, Map<Integer, String>>() : type_log;
                type.resource_type_crawl_logs.put(rs.getInt("resource_type_crawl_id"), type_log);

                Map<Integer, String> sub_type_log = type_log.get(dataset.id);
                sub_type_log = sub_type_log == null ? new HashMap<Integer, String>() : sub_type_log;
                type_log.put(dataset.id, sub_type_log);

                sub_type_log.put(resource.resource_id, rs.getString("resource_type_log"));

                //load the resource instance crawl logs.
                resource.crawl_logs.put(rs.getInt("resource_instance_crawl_id"), rs.getString("resource_instance_log_type"));
            }

            CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.success.toString(), "loadDatasetResourceTypes", "success while loading resource types for dataset " + dataset.dataset_id_datahub, crawl_log_global, conn);
            return resources;

        } catch (Exception ex) {
            CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.exception.toString(), "loadDatasetResourceTypes", "exception while loading resources types for dataset " + dataset.dataset_id_datahub
                    + "\n" + ex.getMessage(), crawl_log_global, conn);
        }
        return null;
    }

    /**
     * Load resource type id based on its uri.
     *
     * @param type_uri
     * @return
     */
    public int loadResourceTypeID(String type_uri) {
        PreparedStatement pst = null;
        try {
            pst = conn.prepareStatement("SELECT type_id, type_uri FROM resource_types WHERE type_uri=?");
            pst.setString(1, type_uri);

            ResultSet rst = pst.executeQuery();
            if (rst.next()) {
                return rst.getInt("type_id");
            }
            CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.success.toString(), "loadResourceTypeID", "success while loading resource type id for type_uri " + type_uri, crawl_log_global, conn);
        } catch (SQLException e) {
            CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.exception.toString(), "loadResourceTypeID", "exception while loading resource type id for type_uri: " + type_uri + "\n" + e.getMessage(), crawl_log_global, conn);
        }
        return -1;
    }

    /**
     * Load active crawl setups.
     *
     * @return
     */
    public Map<Integer, Entry<String, String>> getActiveCrawlSetups() {
        PreparedStatement pst = null;
        try {
            pst = conn.prepareStatement("SELECT setup_id, datahub_keywords, crawl_description FROM crawl_setups WHERE is_completed=0;");

            Map<Integer, Entry<String, String>> lst = new HashMap<Integer, Entry<String, String>>();
            ResultSet rst = pst.executeQuery();
            while (rst.next()) {
                Entry<String, String> entry = new AbstractMap.SimpleEntry<String, String>(rst.getString("datahub_keywords"), rst.getString("crawl_description"));
                lst.put(rst.getInt("setup_id"), entry);
            }
            return lst;
        } catch (SQLException e) {
            CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.exception.toString(), "getActiveCrawlSetups", "exception while loading active crawl setups" + "\n" + e.getMessage(), crawl_log_global, conn);
        }
        return null;
    }

    public void updateCrawlSetup(int crawl_setup_id) {
        PreparedStatement pst = null;
        try {
            pst = conn.prepareStatement("UPDATE crawl_setups SET is_completed=1 WHERE setup_id=?");
            pst.setInt(1, crawl_setup_id);
            pst.executeUpdate();

        } catch (SQLException e) {
            CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.exception.toString(), "getActiveCrawlSetups", "exception while loading active crawl setups" + "\n" + e.getMessage(), crawl_log_global, conn);
        }
    }
}
