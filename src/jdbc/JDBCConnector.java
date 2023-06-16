package jdbc;

import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.logging.Logger;

import javax.sql.DataSource;

import com.alibaba.druid.pool.DruidDataSourceFactory;

public class JDBCConnector {
    private Connection connection;
    private Statement statement;
    private ResultSet resultSet;
    private DataSource dataSource;
    private static final Logger logger = Logger.getLogger("JDBCConnector.class");

    /**
     * connection with pool
     */
    public JDBCConnector() {
        // create properties object，to read configuration
        try {
            Properties properties = new Properties();
            properties.load(new FileInputStream("src\\jdbc\\druid.properties"));
            // create connection pool with properties
            dataSource = DruidDataSourceFactory.createDataSource(properties);
        } catch (Exception e) {
            logger.info("创建数据库连接出错：" + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /*
     * simple connection
     */
    public JDBCConnector(int i){
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:..\\file-manager\\src\\test.db");
            connection.setAutoCommit(false);
        } catch (ClassNotFoundException | SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * add, delete, update
     * 
     * @param sql
     */
    public void update(String sql) {
        logger.info("SQL语句: " + sql);
        try {
            connection = dataSource.getConnection();
            connection.setAutoCommit(false);
            statement = connection.createStatement();
            statement.executeUpdate(sql);
            connection.commit();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * select
     * 
     * @param sql
     * @return
     */
    public ResultSet select(String sql) {
        logger.info("SQL语句: " + sql);
        try {
            connection = dataSource.getConnection();
            connection.setAutoCommit(false);
            statement = connection.createStatement();
            resultSet = statement.executeQuery(sql);
            connection.commit();
            return resultSet;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * close the connection
     */
    public void close() {
        try {
            if (resultSet != null)
                resultSet.close();
            if (statement != null)
                statement.close();
            if (connection != null)
                connection.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
