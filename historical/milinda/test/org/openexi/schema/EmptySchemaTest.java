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
    
    Assert.assertNotNull(schema);
    
    int xsdns = schema.getNamespaceOfSchema("http://www.w3.org/2001/XMLSchema");

    int anyType = schema.getTypeOfNamespace(xsdns, "anyType");
    Assert.assertTrue(anyType != EXISchema.NIL_NODE);

    int anySimpleType = schema.getTypeOfNamespace(xsdns, "anySimpleType");
    Assert.assertTrue(anySimpleType != EXISchema.NIL_NODE);

    int stringType = schema.getTypeOfNamespace(xsdns, "string");
    Assert.assertEquals(anySimpleType, schema.getBaseTypeOfAtomicSimpleType(stringType));
  }
  
}
