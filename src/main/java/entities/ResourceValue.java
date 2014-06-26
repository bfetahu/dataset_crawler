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
public class ResourceValue {
    public int resource_value_id;
    public String datatype_property;
    public String value;
    //when computing the changes in the values of a resource instance this is necessary to indicate whether this one should be considered.
    public boolean isValid = true;
    
    public Map<Integer, String> log_entry;
    
    public ResourceValue(){
        log_entry = new TreeMap<Integer, String>();
    }
    
    public boolean isDeleted(){
        boolean isDeleted = false;
        for(int crawl_id:log_entry.keySet()){
            if(log_entry.get(crawl_id).equals("deleted")){
                isDeleted = true;
            }
            else{
                isDeleted = false;
            }
        }
        
        return isDeleted;
    }
}
