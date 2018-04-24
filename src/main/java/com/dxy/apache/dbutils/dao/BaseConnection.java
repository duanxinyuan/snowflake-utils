package com.dxy.apache.dbutils.dao;

import com.alibaba.druid.pool.DruidPooledConnection;
import com.dxy.apache.dbutils.util.ExceptionUtil;
import com.google.common.collect.Lists;

import java.sql.SQLException;
import java.util.List;

/**
 * DB连接基类
 * @author dxy
 * 2018/4/24 20:15
 */
public class BaseConnection {

    private DruidPooledConnection connection;//JDBC连接，使用事物时需要传入
    private boolean isTransactionExcuting = false;//事务是否正在执行
    private List<ConnectionListener> rollbackListeners;
    private List<ConnectionListener> commitListeners;

    public BaseConnection(DruidPooledConnection connection) {
        this.connection = connection;
    }

    public DruidPooledConnection getConnection() {
        return connection;
    }

    public void setConnection(DruidPooledConnection connection) {
        this.connection = connection;
    }

    public boolean isTransactionExcuting() {
        return isTransactionExcuting;
    }

    public void setTransactionExcuting(boolean transactionExcuting) {
        isTransactionExcuting = transactionExcuting;
    }

    public void addCommitListener(ConnectionListener listener) {
        if (commitListeners == null) {
            commitListeners = Lists.newArrayList();
        }
        commitListeners.add(listener);
    }

    public void addRollbackListener(ConnectionListener listener) {
        if (rollbackListeners == null) {
            rollbackListeners = Lists.newArrayList();
        }
        rollbackListeners.add(listener);
    }

    public interface ConnectionListener {
        void callback();
    }

    /**
     * 开启事务
     */
    public void begin() {
        if (connection != null) {
            try {
                // 开启事务
                connection.setAutoCommit(false);
                isTransactionExcuting = true;
            } catch (Exception e) {
                ExceptionUtil.disposeError(e);//处理异常
            }
        }
    }

    /**
     * 回滚事务
     */
    public void rollback() {
        if (connection != null) {
            try {
                if (!connection.isClosed()) {
                    connection.rollback();
                    isTransactionExcuting = false;
                    if (rollbackListeners != null && rollbackListeners.size() > 0) {
                        rollbackListeners.forEach(ConnectionListener::callback);
                    }
                }
            } catch (Exception e) {
                ExceptionUtil.disposeError(e);//处理异常
            } finally {
                close();
            }
        }
    }

    /**
     * 提交事务
     */
    public void commit() {
        if (connection != null) {
            try {
                if (!connection.isClosed()) {
                    connection.commit();
                    isTransactionExcuting = false;
                    if (rollbackListeners != null && rollbackListeners.size() > 0) {
                        commitListeners.forEach(ConnectionListener::callback);
                    }
                }
            } catch (Exception e) {
                ExceptionUtil.disposeError(e);//处理异常
            } finally {
                close();
            }
        }
    }

    public void close() {
        if (connection != null) {
            try {
                if (!connection.isClosed()) {
                    connection.setAutoCommit(true);
                    connection.close();
                }
            } catch (SQLException e) {
                ExceptionUtil.disposeError(e);//处理异常
            }
        }
    }
}
