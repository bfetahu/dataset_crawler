package metadata_crawler;

import crawl_utils.Properties;
import database_operations.CrawlerLogs;
import entities.CrawlLog;
import entities.Dataset;
import org.json.JSONArray;
import org.json.JSONObject;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;


public class Metadata {
    //for each crawl operation there is a CrawlLog which these operations are associated with. We keep an singelton object of the crawllog
    public CrawlLog crawl_log_global;

    /*
     * Search string for the datasets within the group of linkededucation.
     */

    public final static String DATAHUBURL = "http://datahub.io/api/action/group_show";
    public final static String DATAHUBSINGLEURL = "http://datahub.io/api/action/package_show";

    /**
     * The proxy method, which depending whether its a group search or single package within datahub
     * delegates to the corresponding method and returns the group of datasets of the single dataset.
     *
     * @param is_datahub_group_search
     * @param datahub_keyword
     * @return
     */
    public List<Dataset> searchDataHub(boolean is_datahub_group_search, String[] datahub_keyword, Connection conn) {
        List<Dataset> lst_dataset = new ArrayList<Dataset>();

        for(String datahub_key:datahub_keyword){
            if (is_datahub_group_search) {
                lst_dataset.addAll(loadGroupDatasetInformation(datahub_key, conn));
            } else {
                lst_dataset.add(packageSearchSingle(datahub_key, conn));
            }
        }
        return lst_dataset;
    }

    /*
     * Returns the list of datasets within a specific search query from
     * datahub.io.
     */
    public List<Dataset> loadGroupDatasetInformation(String groupid, Connection conn) {
        try {
            List<Dataset> lst = new ArrayList<Dataset>();

            HttpPost post = new HttpPost(DATAHUBURL);
            post.setHeader("X-CKAN-API-Key", "bf317334-3107-4a25-9773-b5961ef3500b");
            StringEntity input = new StringEntity("{\"id\":\"" + groupid + "\"}");
            input.setContentType("application/json");
            post.setEntity(input);

            HttpClient httpclient = new DefaultHttpClient();
            HttpResponse response = httpclient.execute(post);
            String responsestr = getResponseText(response);

            JSONObject jsobj = new JSONObject(responsestr).getJSONObject("result");
            JSONArray jsarr = jsobj.getJSONArray("packages");

            System.out.println(jsarr.toString());

            for (int i = 0; i < jsarr.length(); i++) {
                JSONObject jsondataset = jsarr.getJSONObject(i);
                System.out.println(jsondataset.toString());

                Dataset ds = new Dataset();
                ds.dataset_id_datahub = jsondataset.getString("id");
                ds.name = jsondataset.getString("name");
                ds.notes = jsondataset.getString("notes");
                ds.maintainer = jsondataset.getString("maintainer");
                ds.maintainer_email = jsondataset.getString("maintainer_email");
                ds.author = jsondataset.getString("author");
                ds.author_email = jsondataset.getString("author_email");
                ds.capacity = jsondataset.getString("capacity");
                ds.licence_id = jsondataset.getString("license_id");
                ds.revision_id = jsondataset.getString("revision_id");
                ds.state = jsondataset.getString("state");
                ds.title = jsondataset.getString("title");
                ds.type = jsondataset.getString("type");

                loadDatasetInformation(ds, ds.name);
                lst.add(ds);
            }
            //log the success of the operation
            CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.success.toString(), "loadGroupDatasetInformation", "successfully loaded metadata for " + groupid + "\n " + post.toString(), crawl_log_global, conn);
            return lst;
        } catch (Exception e) {
            CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.exception.toString(), "loadGroupDatasetInformation", "exception loading metadata for " + groupid + "\n " + e.getMessage(), crawl_log_global, conn);
        }
        return null;
    }

    /*
     * Returns the list of datasets within a specific search query from
     * datahub.io.
     */
    public Dataset packageSearchSingle(String datasetid, Connection conn) {
        try {
            Dataset ds = new Dataset();

            HttpPost post = new HttpPost(DATAHUBSINGLEURL);
            post.setHeader("X-CKAN-API-Key", "bf317334-3107-4a25-9773-b5961ef3500b");
            StringEntity input = new StringEntity("{\"id\":\"" + datasetid + "\"}");
            input.setContentType("application/json");
            post.setEntity(input);

            HttpClient httpclient = new DefaultHttpClient();
            HttpResponse response = httpclient.execute(post);
            String responsestr = getResponseText(response);

            System.out.println(responsestr);

            JSONObject jsondataset = new JSONObject(responsestr).getJSONObject("result");

            ds.dataset_id_datahub = jsondataset.getString("id");
            ds.name = jsondataset.getString("name");
            ds.notes = jsondataset.getString("notes");
            ds.url = jsondataset.getString("url");
            ds.maintainer = jsondataset.getString("maintainer");
            ds.maintainer_email = jsondataset.getString("maintainer_email");
            ds.author = jsondataset.getString("author");
            ds.author_email = jsondataset.getString("author_email");
            ds.licence_id = jsondataset.getString("license_id");
            ds.revision_id = jsondataset.getString("revision_id");
            ds.state = jsondataset.getString("state");
            ds.title = jsondataset.getString("title");
            ds.type = jsondataset.getString("type");
            ds.version = jsondataset.getString("version");

            loadDatasetInformation(ds, ds.name);

            //log the success of the operation
            CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.success.toString(), "packageSearchSingle", "successfully loaded metadata for " + datasetid + "\n " + post.toString(), crawl_log_global, conn);
            return ds;
        } catch (Exception e) {
            CrawlerLogs.writeCrawlLog(Properties.crawl_log_operations.exception.toString(), "packageSearchSingle", "exception loading metadata for " + datasetid + "\n " + e.getMessage(), crawl_log_global, conn);
        }
        return null;
    }

    /*
     * Returns the list of datasets within a specific search query from
     * datahub.io.
     */
    public void loadDatasetInformation(Dataset ds, String datasetid) throws Exception {
        HttpPost post = new HttpPost(DATAHUBSINGLEURL);
        post.setHeader("X-CKAN-API-Key", "bf317334-3107-4a25-9773-b5961ef3500b");
        StringEntity input = new StringEntity("{\"id\":\"" + datasetid + "\"}");
        input.setContentType("application/json");
        post.setEntity(input);

        HttpClient httpclient = new DefaultHttpClient();
        HttpResponse response = httpclient.execute(post);
        String responsestr = getResponseText(response);

        JSONObject jsondataset = new JSONObject(responsestr).getJSONObject("result");
        JSONArray jsondatasetres = jsondataset.getJSONArray("resources");
        JSONArray jsondatasettags = jsondataset.getJSONArray("tags");
        JSONArray jsondatasetgroups = jsondataset.getJSONArray("groups");

        //load the dataset tags
        for (int i = 0; i < jsondatasettags.length(); i++) {
            JSONObject jsontmp = jsondatasettags.getJSONObject(i);
            if (!jsontmp.isNull("name")) {
                String strname = jsontmp.getString("name");
                ds.tags.add(strname);
            }
        }

        //load the dataset groups
        for (int i = 0; i < jsondatasetgroups.length(); i++) {
            JSONObject jsontmp = jsondatasetgroups.getJSONObject(i);
            if (!jsontmp.isNull("name")) {
                String strname = jsontmp.getString("name");
                ds.groups.add(strname);
            }
        }

        //load the SPARQL endpoint
        for (int i = 0; i < jsondatasetres.length(); i++) {
            JSONObject jsontmp = jsondatasetres.getJSONObject(i);
            String strname = "", strdesc = "", format = "";
            if (!jsontmp.isNull("name")) {
                strname = jsontmp.getString("name");
            }
            if (!jsontmp.isNull("description")) {
                strdesc = jsontmp.getString("description");
            }
            if (!jsontmp.isNull("format")) {
                format = jsontmp.getString("format");
            }

            if (strname.contains("SPARQL") || strdesc.contains("SPARQL") || format.contains("sparql")) {
                ds.url = jsontmp.getString("url");
            }
        }
    }

    private static String getResponseText(HttpResponse response) {
        String body = "";
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader((response.getEntity().getContent())));
            String line = "";
            while ((line = br.readLine()) != null) {
                body += line;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return body;
    }
}
