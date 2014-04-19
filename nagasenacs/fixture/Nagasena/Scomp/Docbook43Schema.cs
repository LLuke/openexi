using System;
using NUnit.Framework;

using Org.System.Xml.Sax;

using EXISchema = Nagasena.Schema.EXISchema;

namespace Nagasena.Scomp {

  public class Docbook43Schema {

    private static readonly EXISchema m_corpus;

    static Docbook43Schema() {
      m_corpus = EXISchemaFactoryTestUtil.getEXISchema("/docbook-xsd-4.3/docbook.xsc", (Object)null);
    }

    private Docbook43Schema() {
    }

    /// <summary>
    /// Returns the schema that represents the Docbook Schema.
    /// </summary>
    public static EXISchema EXISchema {
      get {
        return m_corpus;
      }
    }

  }
}