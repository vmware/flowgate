package com.vmware.flowgate.labsdb;

import com.vmware.flowgate.labsdb.redis.TestRedisConfiguration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestRedisConfiguration.class)
@ActiveProfiles("test")
public class LabsdbApplicationTests {

	@Test
	public void contextLoads() {
	}

}
