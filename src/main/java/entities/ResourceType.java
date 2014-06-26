/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package entities;

import java.util.Map;
import java.util.TreeMap;

/**
 *
 * @author besnik
 */
public class ResourceType {

    public int resource_type_id;
    public String type_uri;
    public String type_description;
    public Schema schema;
    //log the updates for each dataset stored as crawl-id -> dataset_id -> resource_id, log_type
    public Map<Integer, Map<Integer, Map<Integer, String>>> resource_type_crawl_logs;

    public ResourceType() {
        schema = new Schema();
        resource_type_crawl_logs = new TreeMap<Integer, Map<Integer, Map<Integer, String>>>();
    }

    public boolean isValidResourceType() {
        return true;
    }
}
