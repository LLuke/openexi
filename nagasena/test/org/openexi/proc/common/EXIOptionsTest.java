package org.openexi.proc.common;

import junit.framework.Assert;
import junit.framework.TestCase;

public class EXIOptionsTest extends TestCase {
  
  public EXIOptionsTest(String name) {
    super(name);
  }

  /**
   * <common> element will be output when getOutput is called
   * with outputSchemaId of value true and an schemaId is available.
   */
  public void testSchemaId_01() throws Exception {
    EXIOptions options = new EXIOptions();
    options.setSchemaId(new SchemaId(""));
    int outline = options.getOutline(true);
    Assert.assertTrue((outline & EXIOptions.ADD_COMMON) != 0);
  }

  /**
   * <common> element will *not* be output when getOutput is called
   * with outputSchemaId of value false even though an schemaId is
   * available.
   */
  public void testSchemaId_02() throws Exception {
    EXIOptions options = new EXIOptions();
    options.setSchemaId(new SchemaId(""));
    int outline = options.getOutline(false);
    Assert.assertTrue((outline & EXIOptions.ADD_COMMON) == 0);
  }

}
