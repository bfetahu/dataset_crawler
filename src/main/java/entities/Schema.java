/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package entities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author besnik
 */
public class Schema {
    public int schema_id;
    public String schema_uri;
    public List<SchemaInstance> instances;
    
    //the datastructure stores the schema logs.
    public Map<Integer, Map<Integer, String>> schema_crawl_logs;

    //the datastructure here follows the structure: crawl_id -> dataset_id -> schema_value_uri, log_type
    public Map<Integer, Map<Integer, Map<String, String>>> schema_instance_crawl_logs;

    //stored as : crawl_id -> dataset_id, log_type
    public Map<Integer, Map<Integer, String>> dataset_schema_crawl_logs;
    
    public Schema(){
        instances = new ArrayList<SchemaInstance>();
        schema_crawl_logs = new HashMap<Integer, Map<Integer, String>>();
        dataset_schema_crawl_logs = new HashMap<Integer, Map<Integer, String>>();
        schema_instance_crawl_logs = new HashMap<Integer, Map<Integer, Map<String, String>>>();
    }
}


