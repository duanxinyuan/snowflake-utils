package com.dxy.apache.dbutils.dao;

import com.alibaba.druid.pool.DruidDataSource;
import com.dxy.apache.dbutils.result.Pagination;
import com.dxy.common.util.ReflectUtil;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.*;
import org.apache.commons.lang.StringUtils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.SQLException;
import java.util.*;

/**
 * Sql执行基类
 * @author dxy
 * 2018/4/24 20:15
 */
@Slf4j
public class BaseQuery<T> {
    /**
     * 数据Sql
     */
    private String sql;
    /**
     * 总数目Sql
     */
    private String countSql;
    /**
     * 页码
     */
    private int pageNo;
    /**
     * 单页数据条数
     */
    private int pageSize;
    /**
     * 返回的数据类型
     */
    private Class<T> cls;

    /**
     * JDBC连接，使用事物时需要传入
     */
    private BaseConnection connection;
    /**
     * Sql执行的参数
     */
    private List<Object> params = new ArrayList<>();
    /**
     * Sql批量执行的参数
     */
    private List<Object[]> paramsBatch = new ArrayList<>();
    /**
     * 连接池
     */
    private DruidDataSource dataSource;


    BaseQuery(String sql, DruidDataSource dataSource) {
        this.sql = sql;
        this.dataSource = dataSource;
    }

    BaseQuery(String sql, Class<T> cls, DruidDataSource dataSource) {
        this.sql = sql;
        this.cls = cls;
        this.dataSource = dataSource;
    }

    BaseQuery(String sql, int pageNo, int pageSize, Class<T> cls, DruidDataSource dataSource) {
        this(sql, null, pageNo, pageSize, cls, dataSource);
    }

    BaseQuery(String sql, String countSql, int pageNo, int pageSize, Class<T> cls, DruidDataSource dataSource) {
        this.sql = sql;
        this.countSql = countSql;
        this.pageNo = pageNo;
        this.pageSize = pageSize;
        this.cls = cls;
        this.dataSource = dataSource;
    }

    /**
     * 设置参数
     * @param value 参数
     */
    public BaseQuery<T> param(Object value) {
        params.add(value);
        return this;
    }

    /**
     * 设置参数
     * @param value 参数集合
     */
    public BaseQuery<T> param(List<Object> value) {
        if (value != null && value.size() > 0) {
            params.addAll(value);
        }
        return this;
    }

    /**
     * 设置参数
     * @param value 参数集合
     */
    public BaseQuery<T> param(Object... value) {
        if (value != null && value.length > 0) {
            params.addAll(Arrays.asList(value));
        }
        return this;
    }

    /**
     * 批量传入组装好的参数和
     * @param value 参数集合
     */
    public BaseQuery<T> paramBatch(List<Object[]> value) {
        if (value != null && value.size() > 0) {
            paramsBatch.addAll(value);
        }
        return this;
    }

    /**
     * 批量设置参数
     * @param value 存储参数的对象集合
     * @param key 需要传入的参数Key集合
     */
    public BaseQuery<T> paramBatch(List<T> value, String... key) {
        if (value != null && value.size() > 0 && key != null && key.length > 0) {
            for (T o : value) {
                Object[] objects = new Object[key.length];
                for (int i = 0; i < key.length; i++) {
                    objects[i] = ReflectUtil.getFieldValue(o, key[i]);
                }
                paramsBatch.add(objects);
            }
        }
        return this;
    }

    public BaseQuery<T> connection(BaseConnection connection) {
        this.connection = connection;
        return this;
    }

    private Object[] convertParam() {
        return params != null && params.size() > 0 ? params.toArray(new Object[params.size()]) : null;
    }

    /**
     * 查询单条数据，以实体形式返回
     * @return V
     */
    public T single() {
        T t = null;
        try {
            ResultSetHandler<T> resultSetHandler;
            if (isPrimitiveType()) {
                resultSetHandler = new ScalarHandler<>();
            } else {
                resultSetHandler = new BeanHandler<>(cls);
            }
            if (connection != null) {
                t = new QueryRunner().query(connection.getConnection(), sql, resultSetHandler, (Object[]) convertParam());
            } else {
                t = new QueryRunner(dataSource).query(sql, resultSetHandler, (Object[]) convertParam());
            }
        } catch (SQLException e) {
            log.error("single error", e);
        }
        return t;
    }

    /**
     * 查询单条数据，以map形式返回
     * @return Map<String               ,                               V>
     */
    public Map<String, T> singleMap() {
        Map<String, T> map = Maps.newHashMap();
        try {
            BeanMapHandler<String, T> resultSetHandler = new BeanMapHandler<>(cls);
            if (connection != null) {
                map = new QueryRunner().query(this.connection.getConnection(), sql, resultSetHandler, (Object[]) convertParam());
            } else {
                map = new QueryRunner(dataSource).query(sql, resultSetHandler, (Object[]) convertParam());
            }
        } catch (SQLException e) {
            log.error("singleMap error", e);
        }
        return map;
    }

    /**
     * 查询单条数据，以List形式返回
     * @return List
     */
    public List<T> list() {
        List<T> vs = new ArrayList<>();
        try {
            ResultSetHandler<List<T>> resultSetHandler;
            if (isPrimitiveType()) {
                resultSetHandler = new ColumnListHandler<>();
            } else {
                resultSetHandler = new BeanListHandler<>(cls);
            }
            if (connection != null) {
                vs = new QueryRunner().query(connection.getConnection(), sql, resultSetHandler, (Object[]) convertParam());
            } else {
                vs = new QueryRunner(dataSource).query(sql, resultSetHandler, (Object[]) convertParam());
            }
        } catch (SQLException e) {
            log.error("list error", e);
        }
        return vs;
    }

    /**
     * 查询单条数据，以List<Object[]>形式返回
     * @return List<Object               [               ]>
     */
    public List<Object[]> listArray() {
        List<Object[]> objects = new ArrayList<>();
        try {
            ArrayListHandler resultSetHandler = new ArrayListHandler();
            if (connection != null) {
                objects = new QueryRunner().query(connection.getConnection(), sql, resultSetHandler, (Object[]) convertParam());
            } else {
                objects = new QueryRunner(dataSource).query(sql, resultSetHandler, (Object[]) convertParam());
            }
        } catch (Exception e) {
            log.error("listArray error", e);
        }
        return objects;
    }

    /**
     * 查询单条数据，以List<Map<String, Object>>形式返回
     * @return List<Map               <               String               ,                               Object>>
     */
    public List<Map<String, Object>> listMap() {
        List<Map<String, Object>> vs = new ArrayList<>();
        try {
            MapListHandler resultSetHandler = new MapListHandler();
            if (connection != null) {
                vs = new QueryRunner().query(connection.getConnection(), sql, resultSetHandler, (Object[]) convertParam());
            } else {
                vs = new QueryRunner(dataSource).query(sql, resultSetHandler, (Object[]) convertParam());
            }
        } catch (Exception e) {
            log.error("listMap error", e);
        }
        return vs;
    }

    /**
     * 查询指定列的数据，列名作为该Map的键，Map中的值为对应行数据转换的键值对，键为列名
     * @return Map<String               ,                               Map               <               String               ,                               Object>>
     */
    public Map<String, Map<String, Object>> keyedMap() {
        Map<String, Map<String, Object>> mapMap = Maps.newHashMap();
        try {
            KeyedHandler<String> resultSetHandler = new KeyedHandler<>();
            if (connection != null) {
                mapMap = new QueryRunner().query(connection.getConnection(), sql, resultSetHandler, (Object[]) convertParam());
            } else {
                mapMap = new QueryRunner(dataSource).query(sql, resultSetHandler, (Object[]) convertParam());
            }
        } catch (Exception e) {
            log.error("keyedMap error", e);
        }
        return mapMap;
    }

    /**
     * 查询指定列的数据，以List形式返回
     * @return List
     */
    public List<T> columnList() {
        List<T> vs = new ArrayList<>();
        try {
            ColumnListHandler<T> resultSetHandler = new ColumnListHandler<>();
            if (connection != null) {
                vs = new QueryRunner().query(connection.getConnection(), sql, resultSetHandler, (Object[]) convertParam());
            } else {
                vs = new QueryRunner(dataSource).query(sql, resultSetHandler, (Object[]) convertParam());
            }
        } catch (Exception e) {
            log.error("columnList error", e);
        }
        return vs;
    }

    /**
     * 查询满足条件的第一条记录，以Map形式返回
     * @return Map<String               ,                               Object>
     */
    public Map<String, Object> firstRowMap() {
        Map<String, Object> objectMap = Maps.newHashMap();
        try {
            MapHandler resultSetHandler = new MapHandler();
            if (connection != null) {
                objectMap = new QueryRunner().query(connection.getConnection(), sql, resultSetHandler, (Object[]) convertParam());
            } else {
                objectMap = new QueryRunner(dataSource).query(sql, resultSetHandler, (Object[]) convertParam());
            }
        } catch (Exception e) {
            log.error("firstRowMap error", e);
        }
        return objectMap;
    }

    /**
     * 查询满足条件的第一条记录，以Object[]形式返回
     * @return Object[]
     */
    public Object[] firstRowArray() {
        Object[] objects = new Object[0];
        try {
            ArrayHandler resultSetHandler = new ArrayHandler();
            if (connection != null) {
                objects = new QueryRunner().query(connection.getConnection(), sql, resultSetHandler, (Object[]) convertParam());
            } else {
                objects = new QueryRunner(dataSource).query(sql, resultSetHandler, (Object[]) convertParam());
            }
        } catch (Exception e) {
            log.error("firstRowArray error", e);
        }
        return objects;
    }

    /**
     * 查询分页数据
     * @return Pagination
     */
    public Pagination<T> pagination() {
        Long totalCount;
        if (!StringUtils.isEmpty(countSql)) {
            totalCount = new BaseQuery<>(countSql, Long.class, dataSource).param(params).single();
        } else {
            countSql = "select 9999";
            totalCount = new BaseQuery<>(countSql, Long.class, dataSource).single();
        }
        if (totalCount == null) {
            totalCount = 0L;
        }
        Pagination<T> pagination = new Pagination<>(pageNo, pageSize, totalCount);
        if (totalCount < 1) {
            pagination.setList(new ArrayList<>());
            return pagination;
        }
        pagination.setList(new BaseQuery<>(sql + " limit " + (pageNo - 1) * pageSize + "," + pageSize, cls, dataSource).param(params).list());
        return pagination;
    }

    /**
     * 插入，返回插入的数据的Id
     * @return Long
     */
    public Long insert() {
        try {
            ScalarHandler<Long> resultSetHandler = new ScalarHandler<>();
            if (connection != null) {
                return new QueryRunner().insert(connection.getConnection(), sql, resultSetHandler, (Object[]) convertParam());
            } else {
                return new QueryRunner(dataSource).insert(sql, resultSetHandler, (Object[]) convertParam());
            }
        } catch (Exception e) {
            log.error("insert error", e);
            return null;
        }
    }

    /**
     * 批量插入，返回插入的数据的Id
     * @return List<Long>
     */
    public List<Long> insertBatch() {
        Object[][] objects = new Object[paramsBatch.size()][];
        for (int i = 0; i < paramsBatch.size(); i++) {
            objects[i] = paramsBatch.get(i);
        }
        try {
            ColumnListHandler<Long> resultSetHandler = new ColumnListHandler<>();
            if (connection != null) {
                return new QueryRunner().insertBatch(connection.getConnection(), sql, resultSetHandler, objects);
            } else {
                return new QueryRunner(dataSource).insertBatch(sql, resultSetHandler, objects);
            }
        } catch (Exception e) {
            log.error("insertBatch error", e);
        }
        return null;
    }

    /**
     * 插入、修改、删除，返回值为1表示成功
     * @return int
     */
    public int update() {
        try {
            if (connection != null) {
                return new QueryRunner().update(connection.getConnection(), sql, (Object[]) convertParam());
            } else {
                return new QueryRunner(dataSource).update(sql, (Object[]) convertParam());
            }
        } catch (Exception e) {
            log.error("update error", e);
            return 0;
        }
    }

    /**
     * 批量插入、修改、删除，返回值为1表示成功
     * @return int
     */
    public int[] updateBatch() {
        Object[][] objects = new Object[paramsBatch.size()][];
        for (int i = 0; i < paramsBatch.size(); i++) {
            objects[i] = paramsBatch.get(i);
        }
        try {
            if (connection != null) {
                return new QueryRunner().batch(connection.getConnection(), sql, objects);
            } else {
                return new QueryRunner(dataSource).batch(sql, objects);
            }
        } catch (Exception e) {
            log.error("updateBatch error", e);
            return null;
        }
    }

    /**
     * 是否是基础数据类型或者封装类型
     */
    private boolean isPrimitiveType() {
        return cls == String.class || cls == Integer.class || cls == Short.class || cls == Float.class || cls == Double.class
                || cls == Boolean.class || cls == Long.class || cls == CharSequence.class || cls == Byte.class || cls == BigDecimal.class
                || cls == BigInteger.class || cls == Date.class || cls == Enum.class;
    }

}
