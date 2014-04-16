package org.openexi.proc.io;

import java.io.OutputStream;

import junit.framework.Assert;
import junit.framework.TestCase;

public class ScriberRuntimeExceptionTest extends TestCase {

  public ScriberRuntimeExceptionTest(String name) {
    super(name);
  }

  ///////////////////////////////////////////////////////////////////////////
  // Test cases
  ///////////////////////////////////////////////////////////////////////////

  /**
   * Test PREFIX_IS_NULL.
   */
  public void testPrefixIsNull() throws Exception {
    Scriber scriber = new BitPackedScriber(false);
    scriber.setPreserveNS(true);
    try {
      scriber.writePrefixOfQName((String)null, -1, (OutputStream)null);
    }
    catch (ScriberRuntimeException sre) {
      Assert.assertEquals(ScriberRuntimeException.PREFIX_IS_NULL, sre.getCode());
      return;
    }
    Assert.fail();
  }

}
