using System;
using System.IO;
using NUnit.Framework;

namespace Nagasena.Proc.IO {

  [TestFixture]
  public class ScriberRuntimeExceptionTest : Nagasena.LocaleLauncher {

    ///////////////////////////////////////////////////////////////////////////
    // Test cases
    ///////////////////////////////////////////////////////////////////////////

    /// <summary>
    /// Test PREFIX_IS_NULL.
    /// </summary>
    [Test]
    public virtual void testPrefixIsNull() {
      Scriber scriber = new BitPackedScriber(false);
      scriber.PreserveNS = true;
      try {
        scriber.writePrefixOfQName((String)null, -1, (Stream)null);
      }
      catch (ScriberRuntimeException sre) {
        Assert.AreEqual(ScriberRuntimeException.PREFIX_IS_NULL, sre.Code);
        return;
      }
      Assert.Fail();
    }

  }

}