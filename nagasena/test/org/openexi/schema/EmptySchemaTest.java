package org.openexi.schema;

import junit.framework.Assert;
import junit.framework.TestCase;

public class EmptySchemaTest extends TestCase {

  ///////////////////////////////////////////////////////////////////////////
  // Test cases
  ///////////////////////////////////////////////////////////////////////////

  /**
   * Vet the empty schema.
   */
  public void testEmptySchema() throws Exception {
    EXISchema schema = EmptySchema.getEXISchema();
    
    Assert.assertEquals(4, schema.uris.length);
    Assert.assertEquals("", schema.uris[0]); 
    Assert.assertEquals("http://www.w3.org/XML/1998/namespace", schema.uris[1]); 
    Assert.assertEquals("http://www.w3.org/2001/XMLSchema-instance", schema.uris[2]); 
    Assert.assertEquals("http://www.w3.org/2001/XMLSchema", schema.uris[3]); 

    Assert.assertEquals(EXISchemaConst.N_BUILTIN_TYPES, EXISchemaUtil.getTypeCountOfSchema("http://www.w3.org/2001/XMLSchema", schema));

    int anyType = schema.getTypeOfSchema("http://www.w3.org/2001/XMLSchema", "anyType");
    Assert.assertTrue(anyType != EXISchema.NIL_NODE);

    int anySimpleType = schema.getTypeOfSchema("http://www.w3.org/2001/XMLSchema", "anySimpleType");
    Assert.assertTrue(anySimpleType != EXISchema.NIL_NODE);

    int stringType = schema.getTypeOfSchema("http://www.w3.org/2001/XMLSchema", "string");
    Assert.assertEquals(anySimpleType, schema.getBaseTypeOfSimpleType(stringType));
  }
  
}
