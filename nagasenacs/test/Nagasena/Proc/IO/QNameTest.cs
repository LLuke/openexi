using System;
using NUnit.Framework;

using QName = Nagasena.Proc.Common.QName;

namespace Nagasena.Proc.IO {

  [TestFixture]
  public class QNameTest : Nagasena.LocaleLauncher {

    ///////////////////////////////////////////////////////////////////////////
    // Test cases
    ///////////////////////////////////////////////////////////////////////////

    /// <summary>
    /// QName test
    /// </summary>
    [Test]
    public virtual void testQName() {

      QName fooA = new QName("foo:A", "urn:foo");
      Assert.AreEqual("urn:foo", fooA.namespaceName);
      Assert.AreEqual("A", fooA.localName);
      Assert.AreEqual("foo", fooA.prefix);

      QName fooB = new QName("B", "urn:foo");
      Assert.AreEqual("urn:foo", fooB.namespaceName);
      Assert.AreEqual("B", fooB.localName);
      Assert.AreEqual("", fooB.prefix);

      QName bareC = new QName("C", (string)null);
      Assert.AreEqual("", bareC.namespaceName);
      Assert.AreEqual("C", bareC.localName);
      Assert.AreEqual("", bareC.prefix);

      // Prefix "foo" not associated with any namespace
      QName fooD = new QName("foo:D", (string)null);
      Assert.IsNull(fooD.namespaceName);
      Assert.AreEqual("D", fooD.localName);
      Assert.AreEqual("foo", fooD.prefix);

      // ":E" is not a valid QName (prefix is missing)
      QName fooE = new QName(":E", "urn:foo");
      Assert.AreEqual("urn:foo", fooE.namespaceName);
      Assert.AreEqual("E", fooE.localName);
      Assert.AreEqual("", fooE.prefix);

      // "foo:" is not a valid QName (local name is missing)
      QName foo = new QName("foo:", "urn:foo");
      Assert.AreEqual("urn:foo", foo.namespaceName);
      Assert.AreEqual("", foo.localName);
      Assert.AreEqual("foo", foo.prefix);

      // "" is not a valid QName (local name is missing)
      QName empty = new QName("", "urn:foo");
      Assert.AreEqual("urn:foo", empty.namespaceName);
      Assert.AreEqual("", empty.localName);
      Assert.AreEqual("", empty.prefix);
    }

    /// <summary>
    /// QName equality test
    /// </summary>
    [Test]
    public virtual void testQNameEquality() {

      QName fooA = new QName("foo:A", "urn:foo");
      QName _fooA = new QName("_foo:A", "urn:foo");
      Assert.IsTrue(fooA.Equals(_fooA));

      QName fooB = new QName("foo:B", "urn:foo");
      Assert.IsFalse(fooB.Equals(fooA));

      QName gooA = new QName("goo:A", "urn:goo");
      Assert.IsFalse(gooA.Equals(fooA));

      QName disguisedGooA = new QName("foo:A", "urn:goo");
      Assert.IsFalse(disguisedGooA.Equals(fooA));

      QName A = new QName("A", "");
      QName _A = new QName("A", "");
      Assert.IsTrue(A.Equals(_A));
      Assert.IsFalse(A.Equals(fooA));

      QName B = new QName("B", "");
      Assert.IsFalse(B.Equals(A));

      QName unboundFooA = new QName("foo:A", (string)null);
      QName _unboundFooA = new QName("foo:A", (string)null);
      Assert.IsFalse(unboundFooA.Equals(fooA));
      Assert.IsFalse(unboundFooA.Equals(_unboundFooA));
    }

  }

}