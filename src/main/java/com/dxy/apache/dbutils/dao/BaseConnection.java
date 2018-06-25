package com.dxy.apache.dbutils.dao;

import com.alibaba.druid.pool.DruidPooledConnection;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;

import java.sql.SQLException;
import java.util.List;

/**
 * DB连接基类
 * @author dxy
 * 2018/4/24 20:15
 */
@Slf4j
public class BaseConnection {

    /**
     * 事务是否正在执行
     */
    private boolean isTransactionExcuting = false;

    private DruidPooledConnection connection;
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
                log.error("connection begin transaction error", e);
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
                log.error("connection rollback error", e);
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
                log.error("connection commit error", e);
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
                log.error("connection close error", e);
            }
        }
    }
}
