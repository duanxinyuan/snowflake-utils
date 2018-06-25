package com.dxy.apache.dbutils.dao;

import com.alibaba.druid.pool.DruidDataSource;
import lombok.extern.slf4j.Slf4j;

import java.sql.SQLException;

/**
 * 数据库连接池配置中心
 * 可以自行继承定制，但是需要跟BaseDao同步修改
 * @author dxy
 * 2018/4/24 20:15
 */
@Slf4j
public class BaseDataSource {

    //schema名，必须设置
    String schema;

    String url_prefix = "jdbc:mysql://";
    String url_suffix = "?autoReconnect=true&amp;useUnicode=true&amp;characterEncoding=UTF-8";

    //DB访问域名/IP，必须设置
    String url = "mysql.test.com";
    //DB账号，必须设置
    String username = "root";
    //DB账号的密码，必须设置
    String password = "123456";

    //初始的连接数，可自行优化
    int initialSize = 10;

    //最大连接数，可自行优化
    int maxActive = 50;

    //获取连接等待超时的时间，单位是毫秒，可自行优化
    int maxWait = 5000;

    //最小连接数，可自行优化
    int minIdle = 1;

    public BaseDataSource() {
    }

    public BaseDataSource(String schema, String url_prefix, String url_suffix, String url, String username, String password) {
        this.schema = schema;
        this.url_prefix = url_prefix;
        this.url_suffix = url_suffix;
        this.url = url;
        this.username = username;
        this.password = password;
    }

    private DruidDataSource dataSource;

    public void setSchema(String schema) {
        this.schema = schema;
        this.dataSource = getDataSource(schema);
    }

    public void setUrl_prefix(String url_prefix) {
        this.url_prefix = url_prefix;
    }

    public void setUrl_suffix(String url_suffix) {
        this.url_suffix = url_suffix;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setInitialSize(int initialSize) {
        this.initialSize = initialSize;
    }

    public void setMaxActive(int maxActive) {
        this.maxActive = maxActive;
    }

    public void setMaxWait(int maxWait) {
        this.maxWait = maxWait;
    }

    public void setMinIdle(int minIdle) {
        this.minIdle = minIdle;
    }

    public DruidDataSource getDataSource() {
        return dataSource;
    }

    /**
     * 获取连接池
     */
    public DruidDataSource getDataSource(String schema) {
        DruidDataSource druidDataSource = new DruidDataSource();
        try {
            druidDataSource.setUrl(getUrl(schema));
            druidDataSource.setUsername(username);
            druidDataSource.setPassword(password);
            druidDataSource.setInitialSize(initialSize);
            druidDataSource.setMinIdle(minIdle);
            druidDataSource.setMaxActive(maxActive);
            druidDataSource.setMaxWait(maxWait);
            druidDataSource.setTimeBetweenEvictionRunsMillis(60000);
            druidDataSource.setMinEvictableIdleTimeMillis(300000);
            druidDataSource.setValidationQuery("SELECT 'x'");
            druidDataSource.setTestWhileIdle(true);
            druidDataSource.setTestOnBorrow(false);
            druidDataSource.setTestOnReturn(false);
            druidDataSource.setPoolPreparedStatements(false);
            druidDataSource.setMaxPoolPreparedStatementPerConnectionSize(20);
            druidDataSource.init();
        } catch (SQLException e) {
            log.error("getDataSource error", e);
        }
        return druidDataSource;
    }

    public String getUrl(String schema) {
        return url_prefix + url + ":3306/" + schema + url_suffix;
    }

}
