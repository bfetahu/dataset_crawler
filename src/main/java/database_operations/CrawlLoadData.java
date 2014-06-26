package database_operations;

import crawl_utils.Properties;
import entities.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by besnik on 15/04/2014.
 */
public class CrawlLoadData {
    public void loadFullDatasetInformation(Dataset dataset, CrawlLog crawl_a, CrawlLog crawl_b) {
        loadDatasetMetadata(dataset, crawl_a, crawl_b);
        loadDatasetSchemas(dataset, crawl_a, crawl_b);
        loadDatasetResourceTypes(dataset, crawl_a, crawl_b);
        loadDatasetResourceInstances(dataset, crawl_a, crawl_b);
        loadDatasetResourceInstanceValues(dataset, crawl_a, crawl_b);
    }

    /**
     * Loads the basic metadata information about a dataset between any two given crawl periods.
     *
     * @param dataset
     * @param crawl_a
     * @param crawl_b
     */
    public void loadDatasetMetadata(Dataset dataset, CrawlLog crawl_a, CrawlLog crawl_b) {
        Connection conn = DatabaseConnection.getMySQLConnection(Properties.properties.get("mysql_host"), Properties.properties.get("mysql_schema"), Properties.properties.get("mysql_user"), Properties.properties.get("mysql_pwd"));
        PreparedStatement pst = null;

        try {
            pst = conn.prepareStatement("SELECT d.dataset_id, d.dataset_name, d.dataset_id_datahub, d.dataset_url, da.is_available, da.crawl_id " +
                    "FROM ld_dataset_crawler.dataset d, ld_dataset_crawler.dataset_availability da " +
                    "WHERE d.dataset_id=? AND d.dataset_id=da.dataset_id AND da.crawl_id BETWEEN ? AND ?");

            pst.setInt(1, dataset.id);
            pst.setInt(2, crawl_a.crawl_id);
            pst.setInt(3, crawl_b.crawl_id);

            ResultSet rst = pst.executeQuery();
            while (rst.next()) {
                dataset.id = rst.getInt("dataset_id");
                dataset.name = rst.getString("dataset_name");
                dataset.dataset_id_datahub = rst.getString("dataset_id_datahub");
                dataset.url = rst.getString("dataset_url");

                dataset.dataset_crawl_availability.put(rst.getInt("crawl_id"), rst.getBoolean("is_available"));
            }
        } catch (Exception ex) {
            CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.exception.toString(), "loadDatasetMetadata", "exception reading the full the dataset metadata for" + dataset.id + "\n " + ex.getMessage(), null, conn);
        }
    }

    /**
     * Loads the dataset schemas and their corresponding associations.
     *
     * @param dataset
     * @param crawl_a
     * @param crawl_b
     */
    public void loadDatasetSchemas(Dataset dataset, CrawlLog crawl_a, CrawlLog crawl_b) {
        Connection conn = DatabaseConnection.getMySQLConnection(Properties.properties.get("mysql_host"), Properties.properties.get("mysql_schema"), Properties.properties.get("mysql_user"), Properties.properties.get("mysql_pwd"));
        PreparedStatement pst = null;

        try {
            pst = conn.prepareStatement("SELECT c.crawl_id, s.schema_id, s.schema_uri, dsl.log_type AS dataset_schema_log FROM " +
                    "ld_dataset_crawler.dataset_schemas ds, ld_dataset_crawler.dataset d, ld_dataset_crawler.schemas s, " +
                    "ld_dataset_crawler.dataset_schema_log dsl, ld_dataset_crawler.crawl_log c " +
                    "WHERE ds.dataset_id = d.dataset_id AND ds.schema_id = s.schema_id AND " +
                    "dsl.dataset_id = d.dataset_id AND dsl.schema_id = s.schema_id AND dsl.crawl_id = c.crawl_id AND " +
                    "d.dataset_id = ? AND c.crawl_id BETWEEN ? AND ?");

            pst.setInt(1, dataset.id);
            pst.setInt(2, crawl_a.crawl_id);
            pst.setInt(3, crawl_b.crawl_id);

            ResultSet rst = pst.executeQuery();
            while (rst.next()) {
                Schema schema = dataset.schemas.get(rst.getString("schema_uri"));
                schema = schema == null ? new Schema() : schema;
                dataset.schemas.put(rst.getString("schema_uri"), schema);

                schema.schema_id = rst.getInt("schema_id");
                schema.schema_uri = rst.getString("schema_uri");

                Map<Integer, String> sub_schema_logs = schema.schema_crawl_logs.get(rst.getInt("crawl_id"));
                sub_schema_logs = sub_schema_logs == null ? new HashMap<Integer, String>() : sub_schema_logs;
                schema.schema_crawl_logs.put(rst.getInt("crawl_id"), sub_schema_logs);

                sub_schema_logs.put(dataset.id, rst.getString("log_type"));
            }
        } catch (Exception ex) {
            CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.exception.toString(), "loadDatasetSchemas", "exception reading the full the dataset schemas for" + dataset.id + "\n " + ex.getMessage(), null, conn);
        }
    }

    /**
     * Loads the resource types that are associated with a dataset for a given period of crawl time points.
     *
     * @param dataset
     * @param crawl_a
     * @param crawl_b
     */
    public void loadDatasetResourceTypes(Dataset dataset, CrawlLog crawl_a, CrawlLog crawl_b) {
        Connection conn = DatabaseConnection.getMySQLConnection(Properties.properties.get("mysql_host"), Properties.properties.get("mysql_schema"), Properties.properties.get("mysql_user"), Properties.properties.get("mysql_pwd"));
        PreparedStatement pst = null;

        try {
            pst = conn.prepareStatement("SELECT c.crawl_id, rt.type_id, rt.type_uri, s.schema_uri " +
                    "FROM ld_dataset_crawler.crawl_log c, ld_dataset_crawler.dataset d, ld_dataset_crawler.resource_types rt, " +
                    "ld_dataset_crawler.resource_type_log rtl, ld_dataset_crawler.schemas s, ld_dataset_crawler.dataset_schemas ds " +
                    "WHERE c.crawl_id = rtl.crawl_id AND ds.schema_id = s.schema_id AND rt.schema_id = s.schema_id AND " +
                    "rt.type_id = rtl.type_id AND ds.dataset_id = ? AND c.crawl_id BETWEEN ? AND ? ");

            pst.setInt(1, dataset.id);
            pst.setInt(2, crawl_a.crawl_id);
            pst.setInt(3, crawl_b.crawl_id);

            ResultSet rst = pst.executeQuery();
            while (rst.next()) {
                ResourceType resource_type = dataset.types.get(rst.getString("type_uri"));
                resource_type = resource_type == null ? new ResourceType() : resource_type;
                dataset.types.put(rst.getString("type_uri"), resource_type);

                resource_type.resource_type_id = rst.getInt("type_id");
                resource_type.type_uri = rst.getString("type_uri");
                resource_type.schema = dataset.schemas.get(rst.getString("schema_uri"));
            }
        } catch (Exception ex) {
            CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.exception.toString(), "loadDatasetResourceTypes", "exception reading the full the dataset schemas for" + dataset.id + "\n " + ex.getMessage(), null, conn);
        }
    }

    /**
     * Loads the resources that are associated with a dataset for a given period of crawl time points.
     *
     * @param dataset
     * @param crawl_a
     * @param crawl_b
     */
    public void loadDatasetResourceInstances(Dataset dataset, CrawlLog crawl_a, CrawlLog crawl_b) {
        Connection conn = DatabaseConnection.getMySQLConnection(Properties.properties.get("mysql_host"), Properties.properties.get("mysql_schema"), Properties.properties.get("mysql_user"), Properties.properties.get("mysql_pwd"));
        PreparedStatement pst = null;

        try {
            pst = conn.prepareStatement("SELECT c.crawl_id, ri.resource_id, ri.resource_uri, ril.log_type, rt.type_id, rt.type_uri, ritl.log_type resource_instance_type_log " +
                    "FROM ld_dataset_crawler.crawl_log c, ld_dataset_crawler.resource_instances ri, ld_dataset_crawler.resource_instance_log ril, " +
                    "ld_dataset_crawler.resource_instance_type rit, ld_dataset_crawler.resource_instance_type_log ritl, ld_dataset_crawler.resource_types rt " +
                    "WHERE c.crawl_id = ril.crawl_id AND ri.resource_id = ril.resource_id AND ri.resource_id = rit.resource_id AND rit.type_id = rt.type_id AND " +
                    "rit.resource_id = ritl.resource_id AND ritl.crawl_id = c.crawl_id AND ri.dataset_id = ? AND c.crawl_id BETWEEN ? AND ?");

            pst.setInt(1, dataset.id);
            pst.setInt(2, crawl_a.crawl_id);
            pst.setInt(3, crawl_b.crawl_id);

            ResultSet rst = pst.executeQuery();
            while (rst.next()) {
                Resource resource = dataset.resources.get(rst.getString("resource_uri"));
                resource = resource == null ? new Resource() : resource;
                dataset.resources.put(rst.getString("resource_uri"), resource);

                resource.resource_id = rst.getInt("resource_id");
                resource.types.put(rst.getString("type_uri"), dataset.types.get(rst.getString("type_uri")));
                resource.crawl_logs.put(rst.getInt("crawl_id"), rst.getString("log_type"));

                Map<Integer, String> sub_resource_type_log = resource.resource_type_log.get(rst.getInt("crawl_id"));
                sub_resource_type_log = sub_resource_type_log == null ? new HashMap<Integer, String>() : sub_resource_type_log;
                resource.resource_type_log.put(rst.getInt("crawl_id"), sub_resource_type_log);

                sub_resource_type_log.put(rst.getInt("type_id"), rst.getString("resource_instance_type_log"));
            }
        } catch (Exception ex) {
            CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.exception.toString(), "loadDatasetResourceTypes", "exception reading the full the dataset schemas for" + dataset.id + "\n " + ex.getMessage(), null, conn);
        }
    }

    /**
     * Loads the resource values that are associated with a dataset for a given period of crawl time points.
     *
     * @param dataset
     * @param crawl_a
     * @param crawl_b
     */
    public void loadDatasetResourceInstanceValues(Dataset dataset, CrawlLog crawl_a, CrawlLog crawl_b) {
        Connection conn = DatabaseConnection.getMySQLConnection(Properties.properties.get("mysql_host"), Properties.properties.get("mysql_schema"), Properties.properties.get("mysql_user"), Properties.properties.get("mysql_pwd"));
        PreparedStatement pst = null;

        try {
            pst = conn.prepareStatement("SELECT c.crawl_id, ri.resource_uri, rv.resource_value_id, rv.property_uri, rv.value, rvl.log_type " +
                    "FROM ld_dataset_crawler.crawl_log c, ld_dataset_crawler.resource_instances ri, " +
                    "ld_dataset_crawler.resource_values rv, ld_dataset_crawler.resource_value_log rvl " +
                    "WHERE ri.dataset_id = ? AND ri.resource_id = rv.resource_id AND rvl.resource_value_id = rv.resource_value_id " +
                    "AND rvl.crawl_id = c.crawl_id AND c.crawl_id BETWEEN ? AND ?");

            pst.setInt(1, dataset.id);
            pst.setInt(2, crawl_a.crawl_id);
            pst.setInt(3, crawl_b.crawl_id);

            ResultSet rst = pst.executeQuery();

            Map<String, Map<Integer, ResourceValue>> added_res_values = new HashMap<String, Map<Integer, ResourceValue>>();

            while (rst.next()) {
                Resource resource = dataset.resources.get(rst.getString("resource_uri"));

                //load the values.
                Map<Integer, ResourceValue> sub_added_res_values = added_res_values.get(resource.resource_uri);
                sub_added_res_values = sub_added_res_values == null ? new HashMap<Integer, ResourceValue>() : sub_added_res_values;
                added_res_values.put(resource.resource_uri, sub_added_res_values);

                ResourceValue resource_value = sub_added_res_values.get(rst.getInt("resource_value_id"));
                resource_value = resource_value == null ? new ResourceValue() : resource_value;
                sub_added_res_values.put(rst.getInt("resource_value_id"), resource_value);

                resource_value.resource_value_id = rst.getInt("resource_value_id");
                resource_value.datatype_property = rst.getString("property_uri");
                resource_value.value = rst.getString("value");

                resource_value.log_entry.put(rst.getInt("crawl_id"), rst.getString("log_type"));
            }

            for (String resource_uri : added_res_values.keySet()) {
                for (int res_value_id : added_res_values.get(resource_uri).keySet()) {
                    dataset.resources.get(resource_uri).values.add(added_res_values.get(resource_uri).get(res_value_id));
                }
            }
        } catch (Exception ex) {
            CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.exception.toString(), "loadDatasetResourceTypes", "exception reading the full the dataset schemas for" + dataset.id + "\n " + ex.getMessage(), null, conn);
        }
    }


}
