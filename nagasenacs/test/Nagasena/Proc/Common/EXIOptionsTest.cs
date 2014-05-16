using System;
using NUnit.Framework;

namespace Nagasena.Proc.Common {

  [TestFixture]
  public class EXIOptionsTest : Nagasena.LocaleLauncher {

    /// <summary>
    /// <common> element will be output when getOutput is called
    /// with outputSchemaId of value true and an schemaId is available.
    /// </summary>
    [Test]
    public virtual void testSchemaId_01() {
      EXIOptions options = new EXIOptions();
      options.SchemaId = new SchemaId("");
      int outline = options.getOutline(true);
      Assert.IsTrue((outline & EXIOptions.ADD_COMMON) != 0);
    }

    /// <summary>
    /// <common> element will *not* be output when getOutput is called
    /// with outputSchemaId of value false even though an schemaId is
    /// available.
    /// </summary>
    [Test]
    public virtual void testSchemaId_02() {
      EXIOptions options = new EXIOptions();
      options.SchemaId = new SchemaId("");
      int outline = options.getOutline(false);
      Assert.IsTrue((outline & EXIOptions.ADD_COMMON) == 0);
    }

  }

}