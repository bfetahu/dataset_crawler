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
public class Namespaces {
    public int namespace_id;
    public String namespace_uri;
    public List<NamespaceInstance> instances;
    
    //the datastructure stores the namespace logs.
    public Map<Integer, Map<Integer, String>> namespace_crawl_logs;

    //the datastructure here follows the structure: crawl_id -> dataset_id -> namespace_value_uri, log_type
    public Map<Integer, Map<Integer, Map<String, String>>> namespace_instance_crawl_logs;

    //stored as : crawl_id -> dataset_id, log_type
    public Map<Integer, Map<Integer, String>> dataset_namespace_crawl_logs;
    
    public Namespaces(){
        instances = new ArrayList<NamespaceInstance>();
        namespace_crawl_logs = new HashMap<Integer, Map<Integer, String>>();
        dataset_namespace_crawl_logs = new HashMap<Integer, Map<Integer, String>>();
        namespace_instance_crawl_logs = new HashMap<Integer, Map<Integer, Map<String, String>>>();
    }
}


