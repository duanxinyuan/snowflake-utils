import com.dxy.library.json.jackson.JacksonUtil;
import com.dxy.library.snowflake.SnowflakeIdUtils;
import org.junit.Test;

import java.time.Clock;
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
