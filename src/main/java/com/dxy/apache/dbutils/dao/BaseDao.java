package com.dxy.apache.dbutils.dao;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.dbutils.QueryRunner;

import java.sql.SQLException;

/**
 * DAO层父类
 * 可以自行继承定制，但是需要跟BaseDataSource同步修改
 * @author dxy
 * 2018/4/24 20:15
 */
@Slf4j
public class BaseDao {

    private BaseDataSource baseDataSource;

    public void setBaseDataSource(BaseDataSource baseDataSource) {
        this.baseDataSource = baseDataSource;
    }

    /**
     * 可继承后自己实现连接池
     */
    public BaseDataSource getBaseDataSource() {
        return baseDataSource;
    }

    public QueryRunner getQueryRunner() {
        return new QueryRunner(getBaseDataSource().getDataSource());
    }

    public <V> BaseQuery<V> createQuery(String sql, Class<V> cls) {
        return new BaseQuery<>(sql, cls, getBaseDataSource().getDataSource());
    }

    public <V> BaseQuery<V> createQuery(String sql) {
        return new BaseQuery<>(sql, getBaseDataSource().getDataSource());
    }

    public <V> BaseQuery<V> createQuery(String sql, String countSql, int pageNo, int pageSize, Class<V> cls) {
        return new BaseQuery<>(sql, countSql, pageNo, pageSize, cls, getBaseDataSource().getDataSource());
    }

    public <V> BaseQuery<V> createQuery(String sql, int pageNo, int pageSize, Class<V> cls) {
        return new BaseQuery<>(sql, pageNo, pageSize, cls, getBaseDataSource().getDataSource());
    }

    public BaseConnection getConnection() {
        try {
            return new BaseConnection(getBaseDataSource().getDataSource().getConnection());
        } catch (SQLException e) {
            log.error("getConnection error", e);
        }
        return null;
    }
}
