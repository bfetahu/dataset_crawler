package database_operations;

import crawl_utils.Properties;
import data_crawler.DataCrawler;
import entities.*;

import java.sql.Connection;
import java.util.*;

/**
 * Created by besnik on 11/08/2014.
 */
public class CrawlOperations {
    private Connection mysql_connection;

    public CrawlOperations(Connection mysql_connection) {
        this.mysql_connection = mysql_connection;
    }

    /**
     * Extracts and writes the associated namespaces with a dataset.
     *
     * @param dataset
     */
    public void crawlDatasetSchemasDump(Dataset dataset, Map<String, Namespaces> schemas, Set<String> existing_schemas, CrawlLog crawl_log, CrawlDBOperations co) {
        //load the existing datataset namespaces
        Map<Integer, Namespaces> dataset_existing_schemas = co.loadDatasetSchemaURI(dataset);

        //crawl up to date namespaces from the endpoint
        if (schemas == null || schemas.isEmpty()) {
            return;
        }

        for (String schema_uri : schemas.keySet()) {
            Namespaces schema = schemas.get(schema_uri);
            if (!existing_schemas.contains(schema.namespace_uri)) {
                co.writeSchema(schema);
            }
            else{
                schema = co.loadSchema(schema_uri);
            }

            //log the crawled namespaces for the respective datasets

            for (int namespace_id : dataset_existing_schemas.keySet()) {
                if (dataset_existing_schemas.get(namespace_id).namespace_uri.equals(schema.namespace_uri)) {
                    schema.namespace_id = namespace_id;
                    break;
                }
            }
            co.writeDatasetSchemas(dataset, schema);

            //store the dataset namespace logs. Create the new namespace objects with namespace_id, namespace_uri and the corresponding crawl_id and dataset_id\F
            Namespaces schema_log = new Namespaces();
            schema_log.namespace_id = schema.namespace_id;
            schema_log.namespace_uri = schema.namespace_uri;

            Map<Integer, String> log_types = new TreeMap<Integer, String>();
            log_types.put(dataset.id, Properties.crawl_log_status.added.name());
            schema_log.dataset_namespace_crawl_logs.put(crawl_log.crawl_id, log_types);

            //write the changes
            co.writeDatasetSchemasLogs(schema_log);

            //write the namespace instances
            co.writeSchemaInstances(schema);
            //add the logs for the namespace instances : crawl_id -> dataset_id -> namespace_value_uri, log_type
            Map<Integer, Map<String, String>> sub_schi_log = schema.namespace_instance_crawl_logs.get(crawl_log.crawl_id);
            sub_schi_log = sub_schi_log == null ? new HashMap<Integer, Map<String, String>>() : sub_schi_log;
            schema.namespace_instance_crawl_logs.put(crawl_log.crawl_id, sub_schi_log);

            Map<String, String> dataset_schi_log = sub_schi_log.get(dataset.id);
            dataset_schi_log = dataset_schi_log == null ? new HashMap<String, String>() : dataset_schi_log;
            sub_schi_log.put(dataset.id, dataset_schi_log);

            for (NamespaceInstance schi : schema.instances) {
                dataset_schi_log.put(schi.namespace_value_uri, Properties.crawl_log_status.added.name());
            }

            co.writeSchemaInstanceLogs(schema);
        }

        //remove those namespaces that are still present in the dataset
        if (dataset_existing_schemas != null) {
            for (int schema_id : dataset_existing_schemas.keySet()) {
                if (schemas.containsKey(dataset_existing_schemas.get(schema_id))) {
                    continue;
                }

                //store the dataset namespace logs. Create the new namespace objects with namespace_id, namespace_uri and the corresponding crawl_id and dataset_id\F
                Namespaces schema = dataset_existing_schemas.get(schema_id);

                Map<Integer, String> log_types = new TreeMap<Integer, String>();
                log_types.put(dataset.id, Properties.crawl_log_status.deleted.name());
                schema.dataset_namespace_crawl_logs.put(crawl_log.crawl_id, log_types);

                //write the changes
                co.writeDatasetSchemasLogs(schema);
            }
        }
    }

    /**
     * Extracts and stores into the database the extracted resource types for a
     * specific dataset.
     *
     * @param dataset
     */
    public void crawlDatasetResourceTypesDump(Map<String, ResourceType> resource_types, CrawlDBOperations co) {
        if (resource_types == null || resource_types.isEmpty()) {
            return;
        }
        Map<String, ResourceType> existing_resource_types = co.loadExistingResourceTypeURI(mysql_connection);
        //filter out existing resource types.
        Set<String> diff_res_types = new HashSet<String>(resource_types.keySet());
        diff_res_types.removeAll(existing_resource_types.keySet());

        Map<String, ResourceType> diff_res_crawled_res_types = new TreeMap<String, ResourceType>();
        for (String res_type_uri : diff_res_types) {
            diff_res_crawled_res_types.put(res_type_uri, resource_types.get(res_type_uri));
        }
        co.writeResourceTypes(diff_res_crawled_res_types);
    }

    /**
     * Crawls the resource instances and their corresponding values along with
     * the corresponding logs of operations.
     * <p>
     * At every crawl we first check what is already crawled from a dataset, and
     * then define the crawl operations whether that is an insert,update,delete
     * of a resource instance or its values.
     *
     * @param dataset
     */
    public void crawlDatasetResourceInstancesDump(Dataset dataset, Map<String, ResourceType> resource_types, Map<String, Resource> resource_instances, CrawlLog crawl_log, DataCrawler dc, CrawlDBOperations co) {
        dataset.types = resource_types;

        //load for each resource type first only the resource URI to check which ones are deleted.
        for (final String type_uri : resource_types.keySet()) {
            //load the existing set of resource instances for the specific dataset and the corresponding resource type.
            Map<Integer, Map.Entry<String, Map<Integer, String>>> existing_resource_uris = co.loadDatasetResourcesURI(dataset, resource_types.get(type_uri));

            //in case there is no data for the respective dataset then we perform a bulk load.
            if (existing_resource_uris == null || existing_resource_uris.isEmpty()) {
                dataset.resources = resource_instances;

                //load the crawl logs for each of the resource instances and resource values
                for (String res_uri : dataset.resources.keySet()) {
                    Resource resource = dataset.resources.get(res_uri);
                    resource.crawl_logs.put(crawl_log.crawl_id, Properties.crawl_log_status.added.name());

                    //update the corresponding values such that the logs are stored accordingly.
                    for (ResourceValue value : resource.values) {
                        value.log_entry.put(crawl_log.crawl_id, Properties.crawl_log_status.added.name());
                    }
                }

                //store the crawled dataset data
                writeDatasetCrawledData(co, dataset, resource_types, type_uri, crawl_log);

                dataset.resources.clear();
                continue;
            }
            Iterator<Resource> res_type_iterator = resource_instances.values().stream().filter(resource -> resource.types.containsKey(type_uri)).iterator();
            //for a resource type load the most up to date set of resource URIs.
            Map<String, Integer> resource_uris = new HashMap<>();
            while (res_type_iterator.hasNext()) {
                Resource resource = res_type_iterator.next();
                resource_uris.put(resource.resource_uri, resource.values.size());
            }

            //load the set of resource instances and their corresponding resource type values
            //get first the newly added resources from the live version of the dataset.
            Set<String> added_resource_uris = new HashSet<String>(resource_uris.keySet());
            //get the deleted resources based on the fact that they exist in the already crawled data but not in the live version of the dataset
            Set<String> deleted_resource_uris = new HashSet<String>();
            Set<String> existing_deleted_resource_uris = new HashSet<String>();

            //get the common set of resources to check whether there is an update in the resource values.
            Set<String> updated_resource_uris = new HashSet<String>();

            for (int resource_id : existing_resource_uris.keySet()) {
                Map.Entry<String, Map<Integer, String>> resource_entry = existing_resource_uris.get(resource_id);

                //if its contained in the existing set of resources remove it as it is not a newly added resource
                if (added_resource_uris.contains(resource_entry.getKey())) {
                    added_resource_uris.remove(resource_entry.getKey());
                }

                //checks for each resource instance whether it is deleted.
                //This is because in cases the resource instance is in both datasets, we do not consider logging again in case the dataset is already deleted
                if (isResourceDeleted(resource_entry.getValue())) {
                    existing_deleted_resource_uris.add(resource_entry.getKey());
                }

                deleted_resource_uris.add(resource_entry.getKey());
                updated_resource_uris.add(resource_entry.getKey());
            }

            //check the deleted resourcce uris. Remove first those that still exist in the live version of the dataset.
            deleted_resource_uris.removeAll(resource_uris.keySet());
            //remove from the deleted set of resource uris those that are already flagged as deleted.
            deleted_resource_uris.removeAll(existing_deleted_resource_uris);

            //write the logs of deleted and added resource instances.
            writeResourceInstanceLogs(added_resource_uris, deleted_resource_uris, existing_resource_uris, crawl_log, co);

            //check for updates on the commonly shared set of resource uris for a datasets based on the existing data and the ones from the live dataset.
            updated_resource_uris.retainAll(resource_uris.keySet());

            //for the existing resource instances log the resource types. Remove the resource instance URIs that are flagged as deleted based on the previous crawl points.
            updated_resource_uris.removeAll(existing_deleted_resource_uris);
            //analyse the resource updates by considering their added datatype property values or changed ones

            ResourceType resource_type = new ResourceType();
            resource_type.resource_type_id = co.loadResourceTypeID(type_uri);
            resource_type.type_uri = type_uri;
            analyseResourceValueUpdates(updated_resource_uris, resource_uris, dataset, resource_type, crawl_log, dc, co);

            //store the added resource instances into the corresponding resource instance logs and tables along with their values.
            Map<Integer, Set<Integer>> resource_instance_types = new HashMap<Integer, Set<Integer>>();

            for (String added_resource_uri : added_resource_uris) {
                Resource resource = dc.loadResource(added_resource_uri, dataset.url);
                resource.crawl_logs.put(crawl_log.crawl_id, Properties.crawl_log_status.added.name());

                //update the corresponding values such that the logs are stored accordingly.
                for (ResourceValue value : resource.values) {
                    value.log_entry.put(crawl_log.crawl_id, Properties.crawl_log_status.added.name());
                }

                writeResourceCrawledData(co, resource, dataset, resource_instance_types, resource_types, type_uri, crawl_log);
            }

            //write the resource instance type logs
            co.writeDatasetResourceInstanceType(resource_instance_types);
        }
    }

    /**
     * Stores the different elements of data crawled for a particular dataset. For instances, resources, types, and the logs.
     *
     * @param co
     * @param dataset
     * @param dataset_resource_types
     * @param type_uri
     * @param crawl_log
     */
    private void writeDatasetCrawledData(CrawlDBOperations co, Dataset dataset, Map<String, ResourceType> dataset_resource_types, String type_uri, CrawlLog crawl_log) {
        //store first the resource uri
        co.writeResourceInstances(dataset);
        System.out.println("Dataset: " + dataset.name + "\t type: " + type_uri + " resource instances have been written.");

        //log the added resource instances
        co.writeResourceInstanceLogs(dataset);
        System.out.println("Dataset: " + dataset.name + "\t type: " + type_uri + " resource instance logs have been written.");

        //store the resource values
        co.writeResourceInstanceValues(dataset);
        System.out.println("Dataset: " + dataset.name + "\t type: " + type_uri + " resource values have been written.");
        //log the stored resource values
        co.writeResourceInstanceValuesLog(dataset);
        System.out.println("Dataset: " + dataset.name + "\t type: " + type_uri + " resource value logs have been written.");

        //add the logs for resource instance and resource types
        ResourceType type = dataset_resource_types.get(type_uri);
        Map<Integer, Map<Integer, String>> type_crawl_log = type.resource_type_crawl_logs.get(crawl_log.crawl_id);
        type_crawl_log = type_crawl_log == null ? new HashMap<Integer, Map<Integer, String>>() : type_crawl_log;
        type.resource_type_crawl_logs.put(crawl_log.crawl_id, type_crawl_log);

        Map<Integer, String> dataset_type_crawl_log = type_crawl_log.get(dataset.id);
        dataset_type_crawl_log = dataset_type_crawl_log == null ? new HashMap<Integer, String>() : dataset_type_crawl_log;
        type_crawl_log.put(dataset.id, dataset_type_crawl_log);

        for (String res_uri : dataset.resources.keySet()) {
            //log the corresponding association such that we know that at this point this resource was associated with the particular type.
            Resource resource = dataset.resources.get(res_uri);
            dataset_type_crawl_log.put(resource.resource_id, Properties.crawl_log_status.added.name());
        }
        //after writing the resources clear the data.
        co.writeDatasetResourceInstanceTypeLog(type);

        //write the resource instance and type data
        co.writeDatasetResourceInstanceType(dataset, type);
    }

    /**
     * Stores the different crawled data for a particular resource. For instance, the resource instance, resource types, the logs etc.
     *
     * @param co
     * @param resource
     * @param dataset
     * @param resource_instance_types
     * @param dataset_resource_types
     * @param type_uri
     * @param crawl_log
     */
    private void writeResourceCrawledData(CrawlDBOperations co, Resource resource, Dataset dataset, Map<Integer, Set<Integer>> resource_instance_types,
                                          Map<String, ResourceType> dataset_resource_types, String type_uri, CrawlLog crawl_log) {
        //store the corresponding values.
        co.writeResourceInstances(resource, dataset);
        System.out.println("Dataset: " + dataset.name + "\t type: " + type_uri + " resource instances have been written.");

        co.writeResourceInstanceLogs(resource);
        System.out.println("Dataset: " + dataset.name + "\t type: " + type_uri + " resource instance logs have been written.");

        co.writeResourceInstanceValues(resource);
        System.out.println("Dataset: " + dataset.name + "\t type: " + type_uri + " resource values have been written.");

        co.writeResourceInstanceValuesLog(resource, crawl_log);
        System.out.println("Dataset: " + dataset.name + "\t type: " + type_uri + " resource value logs have been written.");

        Set<Integer> sub_resource_instance_types = new HashSet<Integer>();
        resource_instance_types.put(resource.resource_id, sub_resource_instance_types);

        //add the logs for resource instance and resource types
        for (String added_type_uri : resource.types.keySet()) {
            ResourceType type = dataset_resource_types.get(added_type_uri);
            Map<Integer, Map<Integer, String>> type_crawl_log = type.resource_type_crawl_logs.get(crawl_log.crawl_id);
            type_crawl_log = type_crawl_log == null ? new HashMap<Integer, Map<Integer, String>>() : type_crawl_log;
            type.resource_type_crawl_logs.put(crawl_log.crawl_id, type_crawl_log);

            Map<Integer, String> dataset_type_crawl_log = type_crawl_log.get(dataset.id);
            dataset_type_crawl_log = dataset_type_crawl_log == null ? new HashMap<Integer, String>() : dataset_type_crawl_log;
            type_crawl_log.put(dataset.id, dataset_type_crawl_log);

            dataset_type_crawl_log.put(resource.resource_id, Properties.crawl_log_status.added.name());

            //after writing the resources clear the data.
            co.writeDatasetResourceInstanceTypeLog(type);

            sub_resource_instance_types.add(type.resource_type_id);
        }
    }

    /**
     * Checks the status of a resource instance by analysing all the previous
     * crawls. It checks whether the resource is deleted. The last status of the
     * resource is taken into consideration. For instances, if a resource has
     * the following order: inserted, updated, deleted, inserted the resource
     * will be flaged as not deleted as the last crawl has the log_type of
     * insert.
     *
     * @param resource_entry_log
     * @return
     */
    private boolean isResourceDeleted(Map<Integer, String> resource_entry_log) {
        boolean isDeleted = false;
        for (int crawl_id : resource_entry_log.keySet()) {
            if (resource_entry_log.get(crawl_id).equals("deleted")) {
                isDeleted = true;
            } else {
                isDeleted = false;
            }
        }
        return isDeleted;
    }

    /**
     * Constructs the crawl log data for added and deleted resource instances.
     *
     * @param added_resource_uris
     * @param deleted_resource_uris
     * @param existing_resource_uris
     */
    private void writeResourceInstanceLogs(Set<String> added_resource_uris, Set<String> deleted_resource_uris,
                                           Map<Integer, Map.Entry<String, Map<Integer, String>>> existing_resource_uris,
                                           CrawlLog crawl_log, CrawlDBOperations co) {
        //construct the log data for the added and deleted resources.
        Map<Integer, Map<Integer, String>> resource_instance_log_data = new TreeMap<Integer, Map<Integer, String>>();

        //nothing to change here
        if (added_resource_uris.isEmpty() && deleted_resource_uris.isEmpty()) {
            return;
        }

        for (int resource_id : existing_resource_uris.keySet()) {
            Map.Entry<String, Map<Integer, String>> resource_entry = existing_resource_uris.get(resource_id);

            //check if its deleted or added the resource instance
            if (added_resource_uris.contains(resource_entry.getKey())) {
                Map<Integer, String> added_resource_entry = resource_instance_log_data.get(resource_id);
                added_resource_entry = added_resource_entry == null ? new TreeMap<Integer, String>() : added_resource_entry;
                resource_instance_log_data.put(resource_id, added_resource_entry);

                added_resource_entry.put(crawl_log.crawl_id, Properties.crawl_log_status.added.name());
            } else if (deleted_resource_uris.contains(resource_entry.getKey())) {
                Map<Integer, String> delete_resource_entry = resource_instance_log_data.get(resource_id);
                delete_resource_entry = delete_resource_entry == null ? new TreeMap<Integer, String>() : delete_resource_entry;
                resource_instance_log_data.put(resource_id, delete_resource_entry);

                delete_resource_entry.put(crawl_log.crawl_id, Properties.crawl_log_status.deleted.name());
            }
        }

        //write the resource logs.
        co.writeResourceInstanceLogs(resource_instance_log_data);
    }

    /**
     * Resources that are present in the crawled dataset and in the live dataset
     * are analysed for changes on their resource values. In case so the
     * corresponding resource instance logs are updated and as well the newly
     * added values and updates on logs are performed.
     *
     * @param update_resource_uris
     * @param resource_uris
     * @param dataset
     * @param resource_type
     * @param crawl_log
     */
    private void analyseResourceValueUpdates(Set<String> update_resource_uris, Map<String, Integer> resource_uris, Dataset dataset, ResourceType resource_type,
                                             CrawlLog crawl_log, DataCrawler dc, CrawlDBOperations co) {
        Map<Integer, Resource> existing_resources = co.loadDatasetResourceValues(dataset, resource_type, update_resource_uris);
        Map<Integer, Resource> existing_resource_types = co.loadDatasetResourceTypes(dataset, update_resource_uris);

        if (existing_resources == null || existing_resources.isEmpty()) {
            return;
        }

        //identify resources that have a mismatch of the number of values.
        Set<Integer> updated_resource_value_uri = new HashSet<Integer>();

        //store for each resource the resource instance and type data in case there is any new association
        Map<Integer, Set<Integer>> res_type_instances = new HashMap<Integer, Set<Integer>>();

        for (int resource_id : existing_resources.keySet()) {
            Set<Integer> sub_res_type_instance = new HashSet<Integer>();
            res_type_instances.put(resource_id, sub_res_type_instance);

            Resource resource = existing_resources.get(resource_id);
            Resource resource_type_data = existing_resource_types.get(resource_id);

            Resource live_resource = dc.loadResource(resource.resource_uri, dataset.url);

            int current_resource_values = resource_uris.get(resource.resource_uri);
            //the resource instance is updated, by adding more resource values.
            if (current_resource_values != resource.getNonDeletedResourceValues()) {
                updated_resource_value_uri.add(resource.resource_id);

                //check whether there is any value that has changed. compare the two sets of values
                boolean hasResourceChanged = compareResourceValues(resource, live_resource);
            } //otherwise compare the values of the resource instance
            else {
                //compare the existing resource values with the ones from the live dataset.
                //we first analyse the sum of the hashcodes from all values to check whether there is a change before we analyse the individual values.
                int live_sum = live_resource.getHashCode();
                int existing_sum = resource.getHashCode();

                //check if the hash code sums are different.
                if (live_sum != existing_sum) {
                    updated_resource_value_uri.add(resource.resource_id);
                    //compare the two sets of values
                    boolean hasResourceChanged = compareResourceValues(resource, live_resource);
                }
            }

            //add to the log the resource types in case there is any change.
            for (String resource_type_uri : resource_type_data.types.keySet()) {
                if (!live_resource.types.containsKey(resource_type_uri)) {
                    Map<Integer, String> sub_res_type_log = resource.resource_type_log.get(crawl_log.crawl_id);
                    sub_res_type_log = sub_res_type_log == null ? new HashMap<Integer, String>() : sub_res_type_log;
                    resource.resource_type_log.put(crawl_log.crawl_id, sub_res_type_log);

                    sub_res_type_log.put(resource.types.get(resource_type_uri).resource_type_id, Properties.crawl_log_status.deleted.name());
                }
            }
            //check for added resource types
            for (String resource_type_uri : live_resource.types.keySet()) {
                if (!resource_type_data.types.containsKey(resource_type_uri)) {
                    Map<Integer, String> sub_res_type_log = resource.resource_type_log.get(crawl_log.crawl_id);
                    sub_res_type_log = sub_res_type_log == null ? new HashMap<Integer, String>() : sub_res_type_log;
                    resource.resource_type_log.put(crawl_log.crawl_id, sub_res_type_log);

                    sub_res_type_log.put(live_resource.types.get(resource_type_uri).resource_type_id, Properties.crawl_log_status.added.name());

                    sub_res_type_instance.add(live_resource.types.get(resource_type_uri).resource_type_id);
                }
            }
        }

        //write the updated values and resources
        co.writeResourceInstanceValues(updated_resource_value_uri, existing_resources);

        //write the resource instance type logs
        co.writeResourceInstanceTypeLog(existing_resources);

        //write the resource instance and type associations
        co.writeDatasetResourceInstanceType(res_type_instances);

        //write the logs for the resource instance logs
        for (int updated_resource_id : updated_resource_value_uri) {
            Resource resource = existing_resources.get(updated_resource_id);
            resource.crawl_logs.put(crawl_log.crawl_id, Properties.crawl_log_status.updated.name());
            co.writeResourceInstanceLogs(resource);
        }
    }

    /**
     * Checks the set of values from an existing resource and a live resource
     * extracted from a dataset whether they all their values are matching.
     *
     * @param existing_resource
     * @param live_resource
     * @return
     */
    private boolean compareResourceValues(Resource existing_resource, Resource live_resource) {
        //now we need to find the particular resource values that have changed.
        //store the values into map datastructure with keys being the property_uris
        Map<String, List<ResourceValue>> existing_property_uri_res_values = new HashMap<String, List<ResourceValue>>();
        Map<String, List<ResourceValue>> live_property_uri_res_values = new HashMap<String, List<ResourceValue>>();
        for (ResourceValue existing_value : existing_resource.values) {
            List<ResourceValue> prop_values = existing_property_uri_res_values.get(existing_value.datatype_property);
            prop_values = prop_values == null ? new ArrayList<ResourceValue>() : prop_values;
            existing_property_uri_res_values.put(existing_value.datatype_property, prop_values);

            //add the two values from the existing and live ones.
            prop_values.add(existing_value);
        }

        for (ResourceValue live_value : live_resource.values) {
            List<ResourceValue> live_prop_values = live_property_uri_res_values.get(live_value.datatype_property);
            live_prop_values = live_prop_values == null ? new ArrayList<ResourceValue>() : live_prop_values;
            live_property_uri_res_values.put(live_value.datatype_property, live_prop_values);

            live_prop_values.add(live_value);
        }

        boolean hasResourceChanged = false;
        for (String property_uri : existing_property_uri_res_values.keySet()) {
            List<ResourceValue> sub_existing_prop_values = existing_property_uri_res_values.get(property_uri);
            List<ResourceValue> sub_live_prop_values = live_property_uri_res_values.get(property_uri);

            //check if the number of datatype property objects is higher or equal to two, then we need to find the first match and skip the rest.
            if (sub_existing_prop_values.size() >= 2) {
                if (sub_live_prop_values == null) {
                    for (ResourceValue existing_value : sub_existing_prop_values) {
                        existing_value.isValid = false;
                    }
                    continue;
                }

                for (ResourceValue existing_value : sub_existing_prop_values) {
                    boolean has_matching_value = false;
                    for (ResourceValue live_value : sub_live_prop_values) {
                        if (existing_value.value.hashCode() == live_value.value.hashCode()) {
                            live_value.isValid = true;
                            has_matching_value = true;
                            break;
                        }
                    }

                    if (!has_matching_value) {
                        existing_value.isValid = false;
                    }
                }

                //add all the live values for which we did not find any matching value
                for (ResourceValue live_value : sub_live_prop_values) {
                    if (live_value.isValid) {
                        existing_resource.values.add(live_value);
                    }
                }
            } else {
                //check if both of them contain the same property
                if (sub_live_prop_values == null) {
                    //if the live resource does not contain these then mark them as deleted.
                    for (ResourceValue existing_value : sub_existing_prop_values) {
                        //we do not consider this for adding to the database and the corresponding logs.
                        existing_value.isValid = false;
                        hasResourceChanged = true;
                    }
                    continue;
                }
                //compare the two lists of values.
                for (ResourceValue existing_value : sub_existing_prop_values) {
                    for (ResourceValue live_value : sub_live_prop_values) {
                        if (existing_value.value.hashCode() != live_value.value.hashCode()) {
                            //we do not consider this for adding to the database and the corresponding logs.
                            live_value.isValid = true;
                            existing_value.isValid = false;

                            existing_resource.values.add(live_value);
                            hasResourceChanged = true;
                            break;
                        }
                    }
                }
            }

        }
        return hasResourceChanged;
    }


    /**
     * Extracts and writes the associated namespaces with a dataset.
     *
     * @param dataset
     */
    public void crawlDatasetSchemas(Dataset dataset, Set<String> existing_schemas, CrawlLog crawl_log, CrawlDBOperations co, DataCrawler dc) {
        //load the existing datataset namespaces
        Map<Integer, Namespaces> dataset_existing_schemas = co.loadDatasetSchemaURI(dataset);

        //crawl up to date namespaces from the endpoint
        Map<String, Namespaces> schemas = dc.extractDatasetNamespaces(dataset, Long.valueOf(Properties.properties.get("timeout")));
        if (schemas == null || schemas.isEmpty()) {
            return;
        }

        for (String schema_uri : schemas.keySet()) {
            Namespaces schema = schemas.get(schema_uri);
            if (!existing_schemas.contains(schema.namespace_uri)) {
                co.writeSchema(schema);
            }
            else{
                schema = co.loadSchema(schema_uri);
            }

            //log the crawled namespaces for the respective datasets

            for (int namespace_id : dataset_existing_schemas.keySet()) {
                if (dataset_existing_schemas.get(namespace_id).namespace_uri.equals(schema.namespace_uri)) {
                    schema.namespace_id = namespace_id;
                    break;
                }
            }
            co.writeDatasetSchemas(dataset, schema);

            //store the dataset namespace logs. Create the new namespace objects with namespace_id, namespace_uri and the corresponding crawl_id and dataset_id\F
            Namespaces schema_log = new Namespaces();
            schema_log.namespace_id = schema.namespace_id;
            schema_log.namespace_uri = schema.namespace_uri;

            Map<Integer, String> log_types = new TreeMap<Integer, String>();
            log_types.put(dataset.id, Properties.crawl_log_status.added.name());
            schema_log.dataset_namespace_crawl_logs.put(crawl_log.crawl_id, log_types);

            //write the changes
            co.writeDatasetSchemasLogs(schema_log);

            //write the namespace instances
            co.writeSchemaInstances(schema);
            //add the logs for the namespace instances : crawl_id -> dataset_id -> namespace_value_uri, log_type
            Map<Integer, Map<String, String>> sub_schi_log = schema.namespace_instance_crawl_logs.get(crawl_log.crawl_id);
            sub_schi_log = sub_schi_log == null ? new HashMap<Integer, Map<String, String>>() : sub_schi_log;
            schema.namespace_instance_crawl_logs.put(crawl_log.crawl_id, sub_schi_log);

            Map<String, String> dataset_schi_log = sub_schi_log.get(dataset.id);
            dataset_schi_log = dataset_schi_log == null ? new HashMap<String, String>() : dataset_schi_log;
            sub_schi_log.put(dataset.id, dataset_schi_log);

            for (NamespaceInstance schi : schema.instances) {
                dataset_schi_log.put(schi.namespace_value_uri, Properties.crawl_log_status.added.name());
            }

            co.writeSchemaInstanceLogs(schema);
        }

        //remove those namespaces that are still present in the dataset
        if (dataset_existing_schemas != null) {
            for (int schema_id : dataset_existing_schemas.keySet()) {
                if (schemas.containsKey(dataset_existing_schemas.get(schema_id))) {
                    continue;
                }

                //store the dataset namespace logs. Create the new namespace objects with namespace_id, namespace_uri and the corresponding crawl_id and dataset_id\F
                Namespaces schema = dataset_existing_schemas.get(schema_id);

                Map<Integer, String> log_types = new TreeMap<Integer, String>();
                log_types.put(dataset.id, Properties.crawl_log_status.deleted.name());
                schema.dataset_namespace_crawl_logs.put(crawl_log.crawl_id, log_types);

                //write the changes
                co.writeDatasetSchemasLogs(schema);
            }
        }
    }

    /**
     * Extracts and stores into the database the extracted resource types for a
     * specific dataset.
     *
     * @param dataset
     */
    public void crawlDatasetResourceTypes(Dataset dataset, DataCrawler dc, CrawlDBOperations co) {
        Map<String, ResourceType> resource_types = dc.loadDatasetResourceTypes(dataset, Long.valueOf(Properties.properties.get("timeout")), co);
        if (resource_types == null || resource_types.isEmpty()) {
            return;
        }

        Map<String, ResourceType> existing_resource_types = co.loadExistingResourceTypeURI(mysql_connection);
        //filter out existing resource types.
        Set<String> diff_res_types = new HashSet<String>(resource_types.keySet());
        diff_res_types.removeAll(existing_resource_types.keySet());

        Map<String, ResourceType> diff_res_crawled_res_types = new TreeMap<String, ResourceType>();
        for (String res_type_uri : diff_res_types) {
            diff_res_crawled_res_types.put(res_type_uri, resource_types.get(res_type_uri));
        }
        co.writeResourceTypes(diff_res_crawled_res_types);
    }


    /**
     * Crawls the resource instances and their corresponding values along with
     * the corresponding logs of operations.
     * <p>
     * At every crawl we first check what is already crawled from a dataset, and
     * then define the crawl operations whether that is an insert,update,delete
     * of a resource instance or its values.
     *
     * @param dataset
     */
    public void crawlDatasetResourceInstances(Dataset dataset, CrawlLog crawl_log, DataCrawler dc, CrawlDBOperations co) {
        //load the resource types for the dataset first.
        Map<String, ResourceType> dataset_resource_types = co.loadDatasetResourceTypes(dataset);
        if (dataset_resource_types == null || dataset_resource_types.isEmpty()) {
            return;
        }
        dataset.types = dataset_resource_types;

        //load first for each resource type the number of resource instances
        Map<String, Integer> resource_instances = dc.getNumberofResourceInstances(dataset, Long.valueOf(Properties.properties.get("timeout")));

        //skip the already existing resource instances
        Set<String> existing_resources = new HashSet<String>();

        //load for each resource type first only the resource URI to check which ones are deleted.
        for (String type_uri : resource_instances.keySet()) {
            int res_type_res_instances = resource_instances.get(type_uri);

            //load the existing set of resource instances for the specific dataset and the corresponding resource type.
            Map<Integer, Map.Entry<String, Map<Integer, String>>> existing_resource_uris = co.loadDatasetResourcesURI(dataset, dataset_resource_types.get(type_uri));

            //in case there is no data for the respective dataset then we perform a bulk load.
            if (existing_resource_uris == null || existing_resource_uris.isEmpty()) {
                dc.loadDatasetResourcesDescribe(dataset, dataset_resource_types.get(type_uri), null, Long.valueOf(Properties.properties.get("timeout")), resource_instances, existing_resources);

                //load the crawl logs for each of the resource instances and resource values
                for (String res_uri : dataset.resources.keySet()) {
                    Resource resource = dataset.resources.get(res_uri);
                    resource.crawl_logs.put(crawl_log.crawl_id, Properties.crawl_log_status.added.name());

                    //update the corresponding values such that the logs are stored accordingly.
                    for (ResourceValue value : resource.values) {
                        value.log_entry.put(crawl_log.crawl_id, Properties.crawl_log_status.added.name());
                    }
                }

                //store the crawled dataset data
                writeDatasetCrawledData(co, dataset, dataset_resource_types, type_uri, crawl_log);

                dataset.resources.clear();
                continue;
            }

            //for a resource type load the most up to date set of resource URIs.
            Map<String, Integer> resource_uris = dc.loadResourceURI(dataset, dataset_resource_types.get(type_uri), Long.valueOf(Properties.properties.get("timeout")), res_type_res_instances);

            //load the set of resource instances and their corresponding resource type values
            //get first the newly added resources from the live version of the dataset.
            Set<String> added_resource_uris = new HashSet<String>(resource_uris.keySet());
            //get the deleted resources based on the fact that they exist in the already crawled data but not in the live version of the dataset
            Set<String> deleted_resource_uris = new HashSet<String>();
            Set<String> existing_deleted_resource_uris = new HashSet<String>();

            //get the common set of resources to check whether there is an update in the resource values.
            Set<String> updated_resource_uris = new HashSet<String>();

            for (int resource_id : existing_resource_uris.keySet()) {
                Map.Entry<String, Map<Integer, String>> resource_entry = existing_resource_uris.get(resource_id);

                //if its contained in the existing set of resources remove it as it is not a newly added resource
                if (added_resource_uris.contains(resource_entry.getKey())) {
                    added_resource_uris.remove(resource_entry.getKey());
                }

                //checks for each resource instance whether it is deleted.
                //This is because in cases the resource instance is in both datasets, we do not consider logging again in case the dataset is already deleted
                if (isResourceDeleted(resource_entry.getValue())) {
                    existing_deleted_resource_uris.add(resource_entry.getKey());
                }

                deleted_resource_uris.add(resource_entry.getKey());
                updated_resource_uris.add(resource_entry.getKey());
            }

            //check the deleted resourcce uris. Remove first those that still exist in the live version of the dataset.
            deleted_resource_uris.removeAll(resource_uris.keySet());
            //remove from the deleted set of resource uris those that are already flagged as deleted.
            deleted_resource_uris.removeAll(existing_deleted_resource_uris);

            //write the logs of deleted and added resource instances.
            writeResourceInstanceLogs(added_resource_uris, deleted_resource_uris, existing_resource_uris, crawl_log, co);

            //check for updates on the commonly shared set of resource uris for a datasets based on the existing data and the ones from the live dataset.
            updated_resource_uris.retainAll(resource_uris.keySet());

            //for the existing resource instances log the resource types. Remove the resource instance URIs that are flagged as deleted based on the previous crawl points.
            updated_resource_uris.removeAll(existing_deleted_resource_uris);
            //analyse the resource updates by considering their added datatype property values or changed ones

            ResourceType resource_type = new ResourceType();
            resource_type.resource_type_id = co.loadResourceTypeID(type_uri);
            resource_type.type_uri = type_uri;
            analyseResourceValueUpdates(updated_resource_uris, resource_uris, dataset, resource_type, crawl_log, dc, co);

            //store the added resource instances into the corresponding resource instance logs and tables along with their values.
            Map<Integer, Set<Integer>> resource_instance_types = new HashMap<Integer, Set<Integer>>();

            for (String added_resource_uri : added_resource_uris) {
                Resource resource = dc.loadResource(added_resource_uri, dataset.url);
                resource.crawl_logs.put(crawl_log.crawl_id, Properties.crawl_log_status.added.name());

                //update the corresponding values such that the logs are stored accordingly.
                for (ResourceValue value : resource.values) {
                    value.log_entry.put(crawl_log.crawl_id, Properties.crawl_log_status.added.name());
                }

                writeResourceCrawledData(co, resource, dataset, resource_instance_types, dataset_resource_types, type_uri, crawl_log);
            }

            //write the resource instance type logs
            co.writeDatasetResourceInstanceType(resource_instance_types);
        }
    }
}
