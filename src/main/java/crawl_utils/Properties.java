/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package crawl_utils;

import java.util.Map;

/**
 *
 * @author besnik
 */
public class Properties {

    public static Map<String, String> properties;

    public enum crawl_log_operations {

        success, exception, error, time_out;

//        @Override
//        public String toString() {
//            switch (this) {
//                case success:
//                    System.out.println("success");
//                    break;
//                case exception:
//                    System.out.println("exception");
//                    break;
//                case time_out:
//                    System.out.println("time_out");
//                    break;
//                case error:
//                    System.out.println("error");
//                    break;
//            }
//            return super.toString();
//        }
    };

    public enum crawl_log_status {

        added, updated, deleted;

//        @Override
//        public String toString() {
//            switch (this) {
//                case added:
//                    "added";
//                    break;
//                case updated:
//                    System.out.println("updated");
//                    break;
//                case deleted:
//                    System.out.println("deleted");
//                    break;
//            }
//            return super.toString();
//        }
    };

    public static String getBaseURI(String uri) {
        String base_uri = "";
        if (uri.contains("#")) {
            base_uri = uri.substring(0, uri.lastIndexOf("#"));
        } else if (uri.contains("/")) {
            base_uri = uri.substring(0, uri.lastIndexOf("/"));
        } else {
            base_uri = uri;
        }
        return base_uri;
    }
}
