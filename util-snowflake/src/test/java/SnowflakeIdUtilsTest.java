import com.dxy.library.snowflake.SnowflakeIdUtils;
import org.junit.Test;

import java.time.Clock;

/**
 * @author duanxinyuan
 * 2019/1/14 21:54
 */
public class SnowflakeIdUtilsTest {

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
