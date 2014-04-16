namespace Nagasena.Sax {

  using ContentHandler = Org.System.Xml.Sax.IContentHandler;
  using LexicalHandler = Org.System.Xml.Sax.ILexicalHandler;

  using GrammarCache = Nagasena.Proc.Grammars.GrammarCache;

  /// <summary>
  /// Applications can directly feed SAX events into a transmogrifier through SAXTransmogrifier.
  /// </summary>
  public interface SAXTransmogrifier : ContentHandler, LexicalHandler, BinaryDataHandler {

    /// <summary>
    /// Returns the GrammarCache that is in use by this SAXTransmogrifier. </summary>
    /// <returns> a GrammarCache </returns>
    GrammarCache GrammarCache { get; }

  }

}