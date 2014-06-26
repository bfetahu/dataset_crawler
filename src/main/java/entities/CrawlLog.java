/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package entities;

import java.sql.Date;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

/**
 *
 * @author besnik
 */
public class CrawlLog {

    public int crawl_id;
    public String crawl_description;
    public Date crawl_date;
    //store the start and end datetime for each individual dataset
    public Map<String, Entry<Date, Date>> dataset_crawl_logs;

    public CrawlLog() {
        dataset_crawl_logs = new TreeMap<String, Entry<Date, Date>>();
    }
}
