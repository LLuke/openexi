using System;
using System.IO;
using System.Net;

namespace Nagasena.Schema {

  /// <summary>
  /// Base class for test cases.
  /// </summary>
  public abstract class TestBase {

    ///////////////////////////////////////////////////////////////////////////
    // Utility classes, methods
    ///////////////////////////////////////////////////////////////////////////

    /// <summary>
    /// Converts a relative systemId into its absolute form. Resolution is
    /// to be made relative to the class. </summary>
    /// <param name="relId"> relative systemId </param>
    /// <returns> absolute systemId </returns>
    protected internal virtual string resolveSystemId(string relId) {
      return getAbsoluteUri(relId).ToString();
    }

    /// <summary>
    /// Converts a relative systemId into an URL. Resolution is
    /// to be made relative to the class. </summary>
    /// <param name="relId"> relative systemId </param>
    /// <returns> URL </returns>
    protected internal virtual Uri resolveSystemIdAsURL(string relId) {
      return getAbsoluteUri(relId);
    }

    private Uri getAbsoluteUri(String relId) {
      String codeBase = this.GetType().Assembly.CodeBase;
      Uri baseUri = new Uri(codeBase);

      String basePath;
      if (relId.StartsWith("/")) {
        basePath = "testdata/whatever";
        relId = relId.Substring(1);
      }
      else {
        basePath = "testdata/" + this.GetType().Namespace.Replace(".", "/") + "/whatever";
      }
      baseUri = new Uri(baseUri, basePath);
      return new Uri(baseUri, relId);
    }

    protected internal Stream string2Stream(String str) {
      return new MemoryStream(System.Text.Encoding.UTF8.GetBytes(str.ToCharArray()));
    }

  }
}