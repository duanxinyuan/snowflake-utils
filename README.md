# Apache-Dbutils-dxy
## 基于Apache开源的Dbutils和阿里巴巴开源的Druid进行封装的DBUtils，全自定义SQL风格

## 纯Sql请求DB的方式<br><br>
## 本框架相对于MyBatis来说，优势主要在于配置简单，速度快、可高度自定制以及轻便，其他如缓存、SQL管理封装、事务、ORM等等功能都没有实现，使用请谨慎
-----
### 一、用法：
#### 1、先自定义dataSource的Url、Schema、username、password

<html>
public class TestBaseDao extends BaseDao {

    @Override
    public BaseDataSource getBaseDataSource() {
        BaseDataSource baseDataSource = super.getBaseDataSource();
        baseDataSource.setUrl("test.mysql.com");
        baseDataSource.setSchema("test");
        baseDataSource.setUsername("root");
        baseDataSource.setPassword("123456");
        return baseDataSource;
    }
}
</html><br>

#### 2、让你的dao层类继承自己实现的BaseDao，执行Sql就可以调用父类的方法，例：
<html>
public class UserDao extends TestBaseDao  {
  
    pubic String queryNickname(Long userId, String mobile) {
      String sql = "select nickname from loginuser where user_id=? and mobile=?";
      return createQuery(sql, String.class).param(userId, mobile).single();
    }
}
</html><br>

#### 3、事务

<html>
public class OrderDao extends TestBaseDao {

    @Override
    public String saveOrder() {
        String orderNo = OrderNoUtil.getOrderNo();
        BaseConnection connection = getConnection();
        //开启事务
        connection.begin();
        try {
            //保存订单主表记录
            String sql = "insert into order(order_no) values(?)";
            createQuery(sql).param(orderNo).connection(connection).insert();
            //保存订单支子表记录
            sql = "insert into order_book(order_no,create_date) values(?,now())";
            createQuery(sql).param(orderNo).connection(connection).insert();
            //事务提交成功的回调
            connection.addCommitListener(() -> {
                SLog.info("保存成功" + orderNo);
            });
            //提交事务
            connection.commit();
            return orderNo;
        } catch (Exception e) {
            //执行失败回滚事务，并发送警报
            connection.rollback();
            ExceptionUtil.disposeError(e);
            return null;
        }
    }
}

</html><br>

如果不想使用阿里巴巴的DruidDataSource，可通过继承BaseDateSource来自定义连接池<br>

----
### 二、具体方法的使用

#### 设置参数方法：<br>
param()/paramBatch()<br>

#### crud方法：<br>
1、single()--查询单条数据，以实体形式返回<br>
2、singleMap()--查询单条数据，以map形式返回<br>
3、list()--查询多条数据，以List形式返回<br>
4、listArray()-查询多条数据，以List<Object[]>形式返回<br>
5、listMap()--查询多条数据，以List<Map<String, Object>>形式返回<br>
6、keyedMap()--查询指定列的数据，返回Map<String, Map<String, Object>>，列名作为该Map的键，Map中的值为对应行数据转换的键值对，键为列名<br>
7、columnList()--查询指定列的数据，以List形式返回<br>
8、firstRowMap()--查询满足条件的第一条记录，以Map形式返回<br>
9、firstRowArray()--查询满足条件的第一条记录，以Object[]形式返回<br>
10、pagination()--查询分页数据<br>
11、insert()--插入数据，返回插入的数据的Id<br>
12、insertBatch()--批量插入，返回插入的数据的Id，配合paramBatch方法使用<br>
13、update()--修改、删除，返回值为1表示成功<br>
14、updateBatch()--批量修改、删除，返回值为1表示成功，配合paramBatch方法使用<br><br>

<br><br>
注：<br>
1、BaseDao和BaseConnection都实现了SQL执行异常的监控，该框架目前只实现了log形式的警报，可以针对ExceptionUtil做自定制<br>
2、可在BaseDao中添加SQL执行时长统计<br>
3、事务传递机制暂未实现，可通过修改BaseConnection来实现<br>
4、一些简单的SQL未进行封装，SQL需要自行管理，如果没有这样的能力，建议直接引入市面的MyBatis


