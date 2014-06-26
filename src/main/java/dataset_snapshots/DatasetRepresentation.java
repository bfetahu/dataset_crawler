/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package dataset_snapshots;

import database_operations.CrawlLoadData;
import entities.CrawlLog;
import entities.Dataset;
import entities.Resource;
import entities.ResourceValue;

import java.util.Iterator;

/**
 *
 * @author besnik
 */
public class DatasetRepresentation {

    /**
     * For a specific dataset and a given interval of crawls (two dates of
     * crawls) we compute the dataset representation by presenting only the
     * non-deleted resource instances and values.
     *
     * @param dataset_id
     * @param crawl_log_a
     * @param crawl_log_b
     * @return
     */
    public Dataset loadDatasetRepresentation(int dataset_id, CrawlLog crawl_log_a, CrawlLog crawl_log_b) {
        Dataset dataset = new Dataset();
        dataset.id = dataset_id;
        CrawlLoadData cld =  new CrawlLoadData();

        cld.loadFullDatasetInformation(dataset, crawl_log_a, crawl_log_b);

        //keep only non deleted resources.
        Iterator<String> resources = dataset.resources.keySet().iterator();
        while (resources.hasNext()) {
            String resource_uri = resources.next();
            Resource resource = dataset.resources.get(resource_uri);
            //remove deleted resources.
            if (!resource.isValidResource()) {
                dataset.resources.remove(resource_uri);
            }

            //from the filtered set of resources, check the state of value updates.
            Iterator<ResourceValue> values = resource.values.iterator();
            while (values.hasNext()) {
                ResourceValue value = values.next();
                if (value.isDeleted()) {
                    values.remove();
                }
            }
        }
        return dataset;
    }
}
