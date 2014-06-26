package entities;

import java.io.Serializable;
import java.util.*;


public class Dataset implements Serializable {

    private static final long serialVersionUID = 5077448742783249961L;
    public int id;
    public String dataset_id_datahub;
    public String maintainer_email;
    public String revision_id;
    public String state;
    public String type;
    public String maintainer;
    public String url;
    public String version;
    public String author;
    public String title;
    public String author_email;
    public String name;
    public String capacity;
    public String licence_id;
    public String notes;
    
    public Set<String> tags;
    public Set<String> groups;
    
    public Map<String, ResourceType> types;    
    public Map<String, Resource> resources;
    public Map<String, Schema> schemas;
    public Map<Integer, Boolean> dataset_crawl_availability;

    public Dataset() {
        tags = new HashSet<String>();
        groups = new HashSet<String>();

        types = new HashMap<String, ResourceType>();
        resources = new HashMap<String, Resource>();
        schemas = new HashMap<String, Schema>();
        dataset_crawl_availability = new HashMap<Integer, Boolean>();
    }

    public boolean isPublicationContent(String filter) {
        for (String tag : tags) {
            if (tag.contains(filter)) {
                return true;
            }
        }

        for (String group : groups) {
            if (group.contains(filter)) {
                return true;
            }
        }

        return false;
    }

    public boolean hasResources() {
        return this.resources != null && this.resources.size() != 0;
    }

    public boolean hasValidResourceClasses() {
        return this.types != null && this.types.size() != 0;
    }
}
