package entities;

import java.io.Serializable;
import java.util.*;

public class Resource implements Serializable {

    private static final long serialVersionUID = 7732343100448135213L;
    public int resource_id;
    public String resource_uri;
    public Map<String, ResourceType> types;
    public List<ResourceValue> values;

    public Map<Integer, String> crawl_logs;

    //store the resource instance + resource type logs.
    public Map<Integer, Map<Integer, String>> resource_type_log;

    public Resource() {
        types = new HashMap<String, ResourceType>();
        values = new ArrayList<ResourceValue>();
        crawl_logs = new HashMap<Integer, String>();

        resource_type_log = new HashMap<Integer, Map<Integer, String>>();
    }

    public int getNonDeletedResourceValues() {
        int count = 0;
        for (ResourceValue res_value : values) {
            if (!res_value.isDeleted()) {
                count++;
            }
        }

        return count;
    }

    /**
     * Checks whether a resource instance is deleted or not.
     *
     * @return
     */
    public boolean isValidResource() {
        boolean isValid = false;
        for (int crawl_id : crawl_logs.keySet()) {
            if (!crawl_logs.get(crawl_id).equals("deleted")) {
                isValid = false;
            } else {
                isValid = true;
            }
        }
        return isValid;
    }

    public int getHashCode() {
        int hash_code = 0;

        //get the sum of hashcodes from the existing resource values
        for (ResourceValue resource_value : values) {
            hash_code += resource_value.value.hashCode();
        }
        return hash_code;
    }
}
