package org.openexi.fujitsu.schema;

import junit.framework.Assert;
import junit.framework.TestCase;

public class HeaderOptionsSchemaTest extends TestCase {
  
  public HeaderOptionsSchemaTest(String name) {
    super(name);
  }

  ///////////////////////////////////////////////////////////////////////////
  // Test cases
  ///////////////////////////////////////////////////////////////////////////

  /**
   * Vet the compiled header options schema.
   */
  public void testHeaderOptionsSchema() throws Exception {

    EXISchema schema = HeaderOptionsSchema.getEXISchema();
    
    Assert.assertNotNull(schema);
    
    int exins = schema.getNamespaceOfSchema("http://www.w3.org/2009/exi");
    Assert.assertTrue(exins != EXISchema.NIL_NODE);
    
    Assert.assertEquals(17, schema.getTypeCountOfNamespace(exins));
    Assert.assertEquals(1, schema.getElemCountOfNamespace(exins));
  }
  
}
