/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package database_operations;

import java.sql.Connection;
import java.sql.DriverManager;

/**
 *
 * @author besnik
 */
public class DatabaseConnection {
     public static Connection getMySQLConnection(String host, String database, String username, String password) {
        Connection conn = null;
        String dbURL = "jdbc:mysql://" + host + "/" + database + "?useUnicode=true&characterEncoding=UTF-8";

        try {
            Class.forName("com.mysql.jdbc.Driver");
            conn = DriverManager.getConnection(dbURL, username, password);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return conn;
    }
}
