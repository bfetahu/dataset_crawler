/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package database_operations;

import entities.CrawlLog;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;


/**
 *
 * @author besnik
 */
public class CrawlerLogs {

    /**
     * Stores into the database the crawl logs of all executed operations.
     *
     * @param log_type
     * @param log_method
     * @param log_message
     * @param operation_date
     * @return
     */
    public static boolean writeCrawlLog(String log_type, String log_method, String log_message, CrawlLog crawl_log, Connection conn) {
        Date date = new Date(new java.util.Date().getTime());
        PreparedStatement pst = null;

        try {
            pst = conn.prepareStatement("INSERT INTO crawl_operations_log(log_type,log_description,log_date,log_method,crawl_id) VALUES(?,?,?,?,?)");

            pst.setString(1, log_type);
            pst.setString(2, log_message);
            pst.setDate(3, date);
            pst.setString(4, log_method);
            pst.setInt(5, crawl_log.crawl_id);

            return pst.executeUpdate() != -1;

        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }
        return false;
    }
}
