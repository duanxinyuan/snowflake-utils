# Twitter雪花算法-完美版

* **增加了使用Redis分配workerId和datacenterId的逻辑，确保不会重复**
* **最多可以分配32*32=1024个workerId+datacenterId的组合**

## Maven依赖
```xml
<dependency>
    <groupId>com.github.duanxinyuan</groupId>
    <artifactId>util-snowflake-prefect</artifactId>
</dependency>
```

* Twitter雪花算法工具类：SnowflakeIdUtils

## 配置示例

### properties 配置示例

```properties
#雪花算法模块名
snowflake.module=test
#Redis缓存类型，single/sentinel/sharded/cluster，必须配置
cache.redis.type.snowflake=single
#Redis节点信息列表，多个使用逗号隔开，必须配置
cache.redis.nodes.snowflake=127.0.0.1:6380
#Redis密码，没有密码不需要配置
cache.redis.password.snowflake=123456
```

### yaml 配置示例

```yaml
snowflake:
    #雪花算法模块名
    module: test
cache:
    redis:
        #Redis缓存类型，single/sentinel/sharded/cluster，必须配置
        type:
            snowflake: single
        #Redis节点信息列表，多个使用逗号隔开，必须配置
        nodes:
            snowflake: 127.0.0.1:6380
        #Redis密码，没有密码不需要配置
        password:
            snowflake: 123456
```

## 使用示例

```java
import com.dxy.library.json.jackson.JacksonUtil;
import com.dxy.library.snowflake.SnowflakeIdUtils;
import org.junit.Test;

import java.time.Clock;
import java.util.List;
import java.util.Map;

/**
 * @author duanxinyuan
 * 2019/1/14 21:54
 */
public class SnowflakeIdUtilsTest {

    @Test
    public void getAllDataCenterWorkerId() {
        Map<String, Map<String, Long>> allDataCenterWorkerId = SnowflakeIdUtils.getAllDataCenterWorkerId();
        System.out.println(JacksonUtil.to(allDataCenterWorkerId));
    }

    @Test
    public void getWorkerId() {
        long workerId = SnowflakeIdUtils.getWorkerId();
        long datacenterId = SnowflakeIdUtils.getDatacenterId();
        System.out.println(workerId);
        System.out.println(datacenterId);
    }

    @Test
    public void testSnowflakeId() {
        System.out.println(SnowflakeIdUtils.getAsLong());
        System.out.println(SnowflakeIdUtils.getAsString());
    }

    @Test
    public void testSnowflakeIdBatch() {
        long start = Clock.systemUTC().millis();
        for (int i = 0; i < 100000; i++) {
            System.out.println(SnowflakeIdUtils.getAsLong());
        }
        System.out.println(Clock.systemUTC().millis() - start);
    }

}
```