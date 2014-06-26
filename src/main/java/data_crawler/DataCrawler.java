package data_crawler;

import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.sparql.engine.http.QueryEngineHTTP;
import crawl_utils.Properties;
import database_operations.CrawlOperations;
import database_operations.CrawlerLogs;
import entities.*;

import java.sql.Connection;
import java.util.AbstractMap.SimpleEntry;
import java.util.*;
import java.util.Map.Entry;

public class DataCrawler {
    //for each crawl operation there is a CrawlLog which these operations are associated with. We keep an singelton object of the crawllog
    public CrawlLog crawl_log_global;
    public Connection mysql_connection;

    public DataCrawler(Connection mysql_connection, CrawlLog crawl_log_global) {
        this.mysql_connection = mysql_connection;
        this.crawl_log_global = crawl_log_global;
    }

    /**
     * Checks whether a dataset endpoint is available or not.
     *
     * @param dataset
     * @return
     */
    public Entry<String, Boolean> isDatasetEndpointAvailable(Dataset dataset) {
        try {
            String querystr = "SELECT * WHERE {?s ?p ?o} LIMIT 1";

            QueryEngineHTTP qt = new QueryEngineHTTP(dataset.url, querystr);
            ResultSet results = qt.execSelect();

            Entry<String, Boolean> entry = null;

            if (results.hasNext()) {
                entry = new SimpleEntry<String, Boolean>("Available", true);
            }

            CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.success.toString(), "isDatasetEndpointAvailable", "success checking endpoint availability for " + dataset.dataset_id_datahub, crawl_log_global, mysql_connection);
            qt.close();

            return entry;
        } catch (Exception e) {
            CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.exception.toString(), "isDatasetEndpointAvailable", "exception checking endpoint availability \n" + e.getMessage(), crawl_log_global, mysql_connection);
        }
        return null;
    }

    /**
     * Extracts the namespaces from a specific dataset. As there is no automatic
     * way on doing that we first extract the types, and later on from the types
     * we parse and extract the base_uri of the namespace. For example,
     * http://xmlns.com/foaf/0.1/OnlineAccount is parsed to
     * http://xmlns.com/foaf/0.1.
     *
     * @param dataset
     * @param timeout
     * @return
     */
    public Map<String, Namespaces> extractDatasetNamespaces(Dataset dataset, long timeout) {
        Map<String, Namespaces> schemas = new HashMap<String, Namespaces>();
        String querystr = " PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> SELECT DISTINCT ?type WHERE {[] rdf:type ?type}";
        try {
            QueryEngineHTTP qt = new QueryEngineHTTP(dataset.url, querystr);
            qt.setTimeout(timeout);

            ResultSet results = qt.execSelect();

            while (results.hasNext()) {
                QuerySolution qs = results.next();

                String type_uri = qs.get("?type").toString();
                String namespace = Properties.getBaseURI(type_uri);

                Namespaces schema = schemas.get(namespace);
                schema = schema == null ? new Namespaces() : schema;
                schemas.put(namespace, schema);

                schema.namespace_uri = namespace;
                NamespaceInstance schema_inst = new NamespaceInstance();
                schema_inst.isProperty = false;
                schema_inst.namespace_value_uri = type_uri;
                schema.instances.add(schema_inst);
            }
            qt.close();

            CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.success.toString(), "extractDatasetNamespaces", "success extracting namespaces from dataset " + dataset.dataset_id_datahub, crawl_log_global, mysql_connection);
        } catch (Exception e) {
            CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.exception.toString(), "extractDatasetNamespaces", "exception extracting namespaces from dataset " + dataset.dataset_id_datahub + "\n" + e.getMessage(), crawl_log_global, mysql_connection);
        }
        return schemas;
    }

    /**
     * Loads the resource types for a specific dataset.
     *
     * @param dataset
     * @param timeout
     * @return
     */
    public Map<String, ResourceType> loadDatasetResourceTypes(Dataset dataset, long timeout, CrawlOperations co) {
        String querystr = " PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> SELECT DISTINCT ?type WHERE {[] rdf:type ?type}";
        try {
            Map<String, ResourceType> types = new TreeMap<String, ResourceType>();

            QueryEngineHTTP qt = new QueryEngineHTTP(dataset.url, querystr);
            qt.setTimeout(timeout);

            ResultSet results = qt.execSelect();

            while (results.hasNext()) {
                QuerySolution qs = results.next();
                ResourceType resource_type = new ResourceType();
                resource_type.type_uri = qs.get("?type").toString();

                String schema_base_uri = Properties.getBaseURI(resource_type.type_uri);
                resource_type.namespace = co.loadSchema(schema_base_uri);

                types.put(resource_type.type_uri, resource_type);
            }
            qt.close();
            CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.success.toString(), "loadDatasetTypes", "success extracting resource types from dataset " + dataset.dataset_id_datahub, crawl_log_global, mysql_connection);
            return types;
        } catch (Exception e) {
            CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.exception.toString(), "loadDatasetTypes", "exception extracting resource types from dataset " + dataset.dataset_id_datahub + "\n" + e.getMessage(), crawl_log_global, mysql_connection);
        }
        return null;
    }

    /**
     * Loads the resource types for a given set of datasets.
     *
     * @param lst
     * @param dsread
     * @param timeout
     * @return
     */
    public Map<String, Map<String, ResourceType>> loadDatasetResourceTypes(List<Dataset> lst, Set<String> dsread, long timeout, CrawlOperations co) {
        Map<String, Map<String, ResourceType>> rst = new HashMap<String, Map<String, ResourceType>>();

        for (Dataset dataset : lst) {
            if (dataset.types != null && !dataset.types.isEmpty()) {
                continue;
            }
            rst.put(dataset.dataset_id_datahub, loadDatasetResourceTypes(dataset, timeout, co));
        }
        return rst;
    }

    /**
     * Loads the number of resource instances for each resource type for a set
     * of datasets.
     *
     * @param lst
     * @param dsread
     * @param timeout
     * @return
     */
    public Map<String, Map<String, Integer>> getNumberofResourceInstances(List<Dataset> lst, Set<String> dsread, long timeout) {
        Map<String, Map<String, Integer>> rst = new TreeMap<String, Map<String, Integer>>();
        for (Dataset dataset : lst) {
            //get the resource classes of a dataset, leave the option to select from the dataset object or feed in the dataset resource classes
            Map<String, Integer> res_counts = getNumberofResourceInstances(dataset, timeout);
            rst.put(dataset.name, res_counts);
        }

        return rst;
    }

    /**
     * Loads the number of instances for the individual resource types for a
     * dataset.
     *
     * @param dataset
     * @param timeout
     * @return
     */
    public Map<String, Integer> getNumberofResourceInstances(Dataset dataset, long timeout) {
        Map<String, Integer> rst = new TreeMap<String, Integer>();
        String querystr = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> SELECT (COUNT (DISTINCT ?x) as ?count) WHERE {?x rdf:type <RESOURCE_CLASS>}";

        if (dataset.types.isEmpty()) {
            return null;
        }

        try {
            //for all resource types get the number of resource instances belonging to
            for (String resource_type_id : dataset.types.keySet()) {
                ResourceType resource_type = dataset.types.get(resource_type_id);

                String query = querystr.replaceAll("RESOURCE_CLASS", resource_type.type_uri);
                QueryEngineHTTP qt = new QueryEngineHTTP(dataset.url, query);
                qt.setTimeout(timeout);

                ResultSet results = qt.execSelect();

                int resourceinstances = 0;
                if (results.hasNext()) {
                    QuerySolution qs = results.next();
                    resourceinstances = qs.get("?count").asLiteral().getInt();
                }
                qt.close();
                rst.put(resource_type.type_uri, resourceinstances);
            }
            CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.success.toString(), "getNumberofResourceInstances", "success extracting the number of resource instances for dataset " + dataset.dataset_id_datahub, crawl_log_global, mysql_connection);

        } catch (Exception e) {
            CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.exception.toString(), "getNumberofResourceInstances", "exception extracting the number of resource instances for dataset " + dataset.dataset_id_datahub + "\n" + e.getMessage(), crawl_log_global, mysql_connection);
        }


        return rst;
    }

    /**
     * Loads the set of resource URIs for a specific dataset and resource type.
     * For easier comparability it loads as well the number of datatype
     * properties for a specific resource.
     *
     * @param dataset
     * @param resource_type
     * @param timeout
     * @return
     */
    public Map<String, Integer> loadResourceURI(Dataset dataset, ResourceType resource_type, long timeout, int max_res_instances) {
        Map<String, Integer> resource_uris = new TreeMap<String, Integer>();
        try {
            String queryfilterstr = "SELECT DISTINCT ?resource_uri (COUNT (?p) as ?property_count) "
                    + "WHERE {?resource_uri <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <" + resource_type.type_uri + ">. ?resource_uri ?p ?o} GROUP BY ?resource_uri ";
            int numberofloadedresources = loadResourcesURI(resource_uris, dataset, queryfilterstr, timeout);

            while (max_res_instances > (numberofloadedresources + 1)) {
                String tmpquerystr = queryfilterstr + " OFFSET " + numberofloadedresources;
                int tmp = loadResourcesURI(resource_uris, dataset, tmpquerystr, timeout);
                if (tmp == 0) {
                    break;
                }
                numberofloadedresources += tmp;
            }

            CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.success.toString(), "loadResourceURI", "success extracting resource instances URIs for dataset " + dataset.dataset_id_datahub + " and type " + resource_type.type_uri, crawl_log_global, mysql_connection);

        } catch (Exception e) {
            CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.exception.toString(), "loadResourceURI", "exception extracting resource instances URIs for dataset " + dataset.dataset_id_datahub + " and type " + resource_type.type_uri + "\n" + e.getMessage(), crawl_log_global, mysql_connection);
        }

        return resource_uris;
    }

    /**
     * Loads the set of resource instances for a particular dataset and all its
     * resource types.
     *
     * @param dataset
     * @param blacklisted
     * @param timeout
     */
    public void loadDatasetResourcesDescribe(Dataset dataset, ResourceType resource_type, Set<String> blacklisted, long timeout, Map<String, Integer> resource_instances, Set<String> existing_resources) {
        //do not check resource instances belonging to an OWL class, since those are usually to defined the ontology rather than capturing the actual information
        //for all resource types get the number of resource instances belonging to
        if (!resource_type.isValidResourceType()) {
            return;
        }
        Integer resourceclassinstanceno = resource_instances.get(resource_type.type_uri);
        resourceclassinstanceno = resourceclassinstanceno == null ? 0 : resourceclassinstanceno;


        loadBulkResourcesDescribe(dataset, blacklisted, resource_type, timeout, resourceclassinstanceno, existing_resources);
    }

    /**
     * Loads the set of resource instances for a particular dataset and all its
     * resource types.
     *
     * @param dataset
     * @param timeout
     */
    public void loadDatasetResourcesDescribe(Dataset dataset, ResourceType resource_type, long timeout, int resource_instances, Set<String> analysed_resources) {
        //do not check resource instances belonging to an OWL class, since those are usually to defined the ontology rather than capturing the actual information
        //for all resource types get the number of resource instances belonging to
        if (!resource_type.isValidResourceType()) {
            return;
        }
        loadBulkResourcesDescribe(dataset, null, resource_type, timeout, resource_instances, analysed_resources);
    }

    /**
     * Loads the resource instances from a dataset and a specific type. It
     * checks that the correct number of resources is loaded due to limitations
     * from SPARQL endpoint on the number of returned rows.
     *
     * @param dataset
     * @param blacklisted
     * @param resource_type
     * @param timeout
     */
    public void loadBulkResourcesDescribe(Dataset dataset, Set<String> blacklisted, ResourceType resource_type, long timeout, int resourceclassinstanceno, Set<String> analysed_resources) {
        //for each type load a set of resources.
        if (blacklisted != null && blacklisted.contains(resource_type.type_uri.toLowerCase().trim())) {
            return;
        }
        String queryfilterstr = "DESCRIBE ?resource WHERE {?resource <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <" + resource_type.type_uri + ">} ";
        String tmpqueryfilterstr = queryfilterstr;

        try {
            int numberofloadedresources = loadResourcesDescribe(dataset, tmpqueryfilterstr, resource_type, timeout, analysed_resources);

            if (resourceclassinstanceno == 0) {
                resourceclassinstanceno = Integer.MAX_VALUE;
            }

            while (resourceclassinstanceno > (numberofloadedresources + 1)) {
                String tmpquerystr = queryfilterstr + " OFFSET " + numberofloadedresources;
                int tmp = loadResourcesDescribe(dataset, tmpquerystr, resource_type, timeout, analysed_resources);
                if (tmp == 0) {
                    break;
                }
                numberofloadedresources += tmp;
            }

            CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.success.toString(), "loadBulkResourcesDescribe", "success extracting resource instances for dataset " + dataset.dataset_id_datahub + " and type " + resource_type.type_uri, crawl_log_global, mysql_connection);

        } catch (Exception e) {
            CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.exception.toString(), "loadBulkResourcesDescribe", "exception extracting resource instances for dataset " + dataset.dataset_id_datahub + " and type " + resource_type.type_uri + "\n" + e.getMessage(), crawl_log_global, mysql_connection);
        }
    }

    /**
     * Loads resource instances of a type using the DESCRIBE command. Called by
     * loadBulkResourcesDescribe
     *
     * @param dataset
     * @param queryfilterstr
     * @param resource_type
     * @return
     */
    private int loadResourcesDescribe(Dataset dataset, String queryfilterstr, ResourceType resource_type, long timeout, Set<String> analysed_resources) {
        int numberofloadedresources = 0;
        QueryEngineHTTP qt = new QueryEngineHTTP(dataset.url, queryfilterstr);
        qt.setTimeout(timeout);

        Model model = qt.execDescribe();
        ResIterator iter = model.listSubjects();

        while (iter.hasNext()) {
            com.hp.hpl.jena.rdf.model.Resource res = iter.next();
            if (res.hasProperty(com.hp.hpl.jena.vocabulary.RDF.type)) {
                numberofloadedresources++;
                if (analysed_resources != null && analysed_resources.contains(res.getURI())) {
                    continue;
                }
                Resource resource = dataset.resources.get(res.getURI());
                resource = resource == null ? new Resource() : resource;
                dataset.resources.put(res.getURI(), resource);


                resource.resource_id = -1;
                resource.resource_uri = res.getURI();
                resource.types.put(resource_type.type_uri, resource_type);

                StmtIterator propertyiterator = res.listProperties();
                while (propertyiterator.hasNext()) {
                    Statement stm = propertyiterator.next();
                    ResourceValue res_value = new ResourceValue();
                    res_value.datatype_property = stm.getPredicate().toString();
                    res_value.value = stm.getObject().toString();

                    resource.values.add(res_value);
                }
            }
        }
        qt.close();

        return numberofloadedresources;
    }

    /**
     * Loads the resource URIs for a particular dataset and a specific resource
     * instance. It loads as well the number of datatype properties assigned to
     * a resource instance.
     *
     * @param resource_uris
     * @param dataset
     * @param querystr
     * @param timeout
     * @return
     */
    private int loadResourcesURI(Map<String, Integer> resource_uris, Dataset dataset, String querystr, long timeout) {
        int numberofloadedresources = 0;
        QueryEngineHTTP qt = new QueryEngineHTTP(dataset.url, querystr);
        qt.setTimeout(timeout);

        ResultSet results = qt.execSelect();
        while (results.hasNext()) {
            numberofloadedresources++;
            QuerySolution qs = results.next();
            resource_uris.put(qs.get("?resource_uri").toString(), qs.get("?property_count").asLiteral().getInt());
        }
        qt.close();

        return numberofloadedresources;
    }

    /**
     * Assesses whether a resource class is equivalent to some of the classes,
     * if yes then we do not consider for indexing to avoid duplicates.
     *
     * @param dataset
     * @param types
     * @param resource_type
     */
    public boolean assessEquivalentClasses(Dataset dataset, ResourceType resource_type, Set<ResourceType> types) {
        String querystr = "ASK {<" + resource_type.type_uri + ">  <http://www.w3.org/2002/07/owl#equivalentClass> <RESOURCE_CLASS_CMP>}";
        for (ResourceType typecmp : types) {
            if (typecmp.type_uri.equals(resource_type.type_uri)) {
                continue;
            }

            try {
                String querystrtmp = querystr.replaceAll("RESOURCE_CLASS_CMP", typecmp.type_uri);
                QueryEngineHTTP query = new QueryEngineHTTP(dataset.url, querystrtmp);

                boolean tmp = query.execAsk();
                if (tmp) {
                    return true;
                }
            } catch (Exception e) {
                CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.exception.toString(), "assessEquivalentClasses", "exception assessing class owl:equivalentClass for dataset and type "
                        + dataset.dataset_id_datahub + " and type " + resource_type.type_uri + "\n" + e.getMessage(), crawl_log_global, mysql_connection);
            }
        }
        return false;
    }

    /**
     * Loads a set of specific resources for a dataset.
     *
     * @param resource_uris
     * @param dataset
     * @return
     */
    public Map<String, Resource> loadDatasetSpecificResources(Set<String> resource_uris, Dataset dataset) {
        for (String resource_uri : resource_uris) {
            String queryfilterstr = "DESCRIBE <" + resource_uri + ">";

            QueryEngineHTTP qt = new QueryEngineHTTP(dataset.url, queryfilterstr);
            qt.setTimeout(Long.valueOf(Properties.properties.get("timeout")));

            Model model = qt.execDescribe();
            ResIterator iter = model.listSubjects();

            while (iter.hasNext()) {
                com.hp.hpl.jena.rdf.model.Resource res = iter.next();
                if (res.hasProperty(com.hp.hpl.jena.vocabulary.RDF.type)) {
                    Resource resource = dataset.resources.get(res.getURI());
                    resource = resource == null ? new Resource() : resource;
                    dataset.resources.put(res.getURI(), resource);

                    resource.resource_id = -1;
                    resource.resource_uri = res.getURI();

                    StmtIterator propertyiterator = res.listProperties();
                    while (propertyiterator.hasNext()) {
                        Statement stm = propertyiterator.next();
                        ResourceValue res_value = new ResourceValue();
                        res_value.datatype_property = stm.getPredicate().toString();
                        res_value.value = stm.getObject().toString();

                        resource.values.add(res_value);
                    }
                }
            }
            qt.close();
        }
        return null;
    }

    /**
     * Loads a specific resource given a particular URI and dataset endpoint.
     *
     * @param resource_uri
     * @param url
     * @return
     */
    public Resource loadResource(String resource_uri, String url) {
        String queryfilterstr = "SELECT ?property ?value WHERE { <" + resource_uri + "> ?property ?value }";

        QueryEngineHTTP qt = new QueryEngineHTTP(url, queryfilterstr);
        qt.setTimeout(Long.valueOf(Properties.properties.get("timeout")));

        ResultSet rst = qt.execSelect();
        Resource resource = new Resource();
        resource.resource_uri = resource_uri;

        while (rst.hasNext()) {
            QuerySolution qs = rst.next();

            String property = qs.get("?property").toString();
            String value = qs.get("?value").toString();

            ResourceValue res_value = new ResourceValue();
            res_value.datatype_property = property;
            res_value.value = value;

            resource.values.add(res_value);

            if (property.equals("http://www.w3.org/1999/02/22-rdf-syntax-ns#type")) {
                ResourceType res_type = new ResourceType();
                res_type.type_uri = value;
                resource.types.put(value, res_type);
            }
        }

        qt.close();
        return resource;
    }
}
