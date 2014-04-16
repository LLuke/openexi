using System;
using NUnit.Framework;

namespace Nagasena.Schema {
	
  [TestFixture]
  public class EmptySchemaTest {
	

    ///////////////////////////////////////////////////////////////////////////
    // Test cases
    ///////////////////////////////////////////////////////////////////////////

    /// <summary>
    /// Vet the empty schema.
    /// </summary>
    [Test]
    public void testEmptySchema() {
      EXISchema schema = EmptySchema.EXISchema;
    
      Assert.AreEqual(4, schema.uris.Length);
      Assert.AreEqual("", schema.uris[0]); 
      Assert.AreEqual("http://www.w3.org/XML/1998/namespace", schema.uris[1]); 
      Assert.AreEqual("http://www.w3.org/2001/XMLSchema-instance", schema.uris[2]); 
      Assert.AreEqual("http://www.w3.org/2001/XMLSchema", schema.uris[3]); 

      Assert.AreEqual(EXISchemaConst.N_BUILTIN_TYPES, EXISchemaUtil.getTypeCountOfSchema("http://www.w3.org/2001/XMLSchema", schema));

      int anyType = schema.getTypeOfSchema("http://www.w3.org/2001/XMLSchema", "anyType");
      Assert.IsTrue(anyType != EXISchema.NIL_NODE);

      int anySimpleType = schema.getTypeOfSchema("http://www.w3.org/2001/XMLSchema", "anySimpleType");
      Assert.IsTrue(anySimpleType != EXISchema.NIL_NODE);

      int stringType = schema.getTypeOfSchema("http://www.w3.org/2001/XMLSchema", "string");
      Assert.AreEqual(anySimpleType, schema.getBaseTypeOfSimpleType(stringType));
    }

  }
  
}
