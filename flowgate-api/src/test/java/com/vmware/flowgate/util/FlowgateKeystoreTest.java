/**
 *
 */
package com.vmware.flowgate.util;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
public class FlowgateKeystoreTest {

   @Test
   public void testGuardStore() {
      Assert.assertTrue(FlowgateKeystore.getEncryptKey().length() == 16);
   }
}
