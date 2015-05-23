using System;
using System.Collections;
using System.Diagnostics;
using System.IO;
using System.Xml;

using Org.System.Xml.Sax;
using AttributesImpl = Org.System.Xml.Sax.Helpers.AttributesImpl;
using LocatorImpl = Org.System.Xml.Sax.Helpers.LocatorImpl;
using ParseErrorImpl = Org.System.Xml.Sax.Helpers.ParseErrorImpl;

namespace Nagasena.Sax {

  /*******************************/
  /// <summary>
  /// Emulates the SAX parsers behaviours.
  /// </summary>
  public class SaxAdapter {

    public IContentHandler ContentHandler {
      get {
        return callBackHandler;
      }
      set {
        callBackHandler = (Transmogrifier.SAXEventHandler)value;
      }
    }
    public IDeclHandler DeclHandler { get; set; }
    public IDtdHandler DtdHandler {
      get {
        return null;
      }
      set {
      }
    }
    public IEntityResolver EntityResolver {
      get {
        return entityResolver;
      }
      set {
        entityResolver = value;
      }
    }
    public IErrorHandler ErrorHandler {
      get {
        return errorHandler;
      }
      set {
        errorHandler = value;
      }
    }
    public ILexicalHandler LexicalHandler {
      get {
        return lexicalHandler;
      }
      set {
        lexicalHandler = value;
      }
    }

    public XmlReaderStatus Status {
      get {
        return XmlReaderStatus.Ready;
      } 
    }

    protected LocatorImpl locator;

    private Transmogrifier.SAXEventHandler callBackHandler;
    private IErrorHandler errorHandler;
    protected ILexicalHandler lexicalHandler;
    protected IEntityResolver entityResolver;

    /// <summary>
    /// Public constructor for the class.
    /// </summary>
    public SaxAdapter() {
      callBackHandler = null;
      errorHandler = null;
      locator = null;
      lexicalHandler = null;
      entityResolver = null;
    }

    /// <summary>
    /// Emulates the behaviour of a SAX LocatorImpl object.
    /// </summary>
    private void UpdateLocatorData(LocatorImpl locator, System.Xml.XmlReader xmlReader, String parserFileName) {
      if (locator != null && xmlReader != null) {
        XmlReaderSettings settings;
        if ((settings = xmlReader.Settings) != null) {
          locator.LineNumber = settings.LineNumberOffset;
          locator.ColumnNumber = settings.LinePositionOffset;
        }
        locator.SystemId = parserFileName;
      }
    }

    /// <summary>
    /// Emulates the behavior of a SAX parser, it realizes the callback events of the parser.
    /// </summary>
    private void DoParsing(XmlReader xmlReader, String systemId) {
      bool bDocument = true;
      Stack/*<int>*/ nsDeclsCount = new Stack();
      Stack/*<String>*/ prefixes = new Stack();
      locator = new LocatorImpl();
      try {
        UpdateLocatorData(this.locator, xmlReader, systemId);
        if (this.callBackHandler != null)
          this.callBackHandler.SetDocumentLocator(locator);
        if (this.callBackHandler != null)
          this.callBackHandler.StartDocument();
        while (xmlReader.Read()) {
          UpdateLocatorData(this.locator, xmlReader, systemId);
          int n_nsdecls;
          switch (xmlReader.NodeType) {
            case System.Xml.XmlNodeType.Element:
              if (bDocument) {
                bDocument = false;
              }
              bool Empty = xmlReader.IsEmptyElement;
              System.String namespaceURI = "";
              System.String localName = "";
              namespaceURI = xmlReader.NamespaceURI;
              localName = xmlReader.LocalName;
              n_nsdecls = 0;
              System.String name = xmlReader.Name;
              AttributesImpl attributes = new AttributesImpl();
              if (xmlReader.HasAttributes) {
                for (int i = 0; i < xmlReader.AttributeCount; i++) {
                  xmlReader.MoveToAttribute(i);
                  System.String prefixName = (xmlReader.Name.IndexOf(":") > 0) ? 
                    xmlReader.Name.Substring(xmlReader.Name.IndexOf(":") + 1, xmlReader.Name.Length - xmlReader.Name.IndexOf(":") - 1) : "";
                  System.String prefix = (xmlReader.Name.IndexOf(":") > 0) ? 
                    xmlReader.Name.Substring(0, xmlReader.Name.IndexOf(":")) : xmlReader.Name;
                  bool IsXmlns = prefix.ToLower().Equals("xmlns");
                  if (!IsXmlns)
                    attributes.AddAttribute(xmlReader.NamespaceURI, xmlReader.LocalName, xmlReader.Name, "" + xmlReader.NodeType, xmlReader.Value, true);
                  else {
                    ++n_nsdecls;
                    prefixes.Push(prefixName);
                    if (this.callBackHandler != null)
                      this.callBackHandler.StartPrefixMapping(prefixName, xmlReader.Value);
                  }
                }
              }
              nsDeclsCount.Push(n_nsdecls);
              if (this.callBackHandler != null) {
                this.callBackHandler.StartElement(namespaceURI, localName, name, attributes);
              }
              if (Empty) {
                if (this.callBackHandler != null)
                  this.callBackHandler.EndElement(namespaceURI, localName, name);
                n_nsdecls = (int)nsDeclsCount.Pop();
                while (n_nsdecls > 0) {
                  String prefixName = (String)prefixes.Pop();
                  if (this.callBackHandler != null)
                    this.callBackHandler.EndPrefixMapping(prefixName);
                  --n_nsdecls;
                }
              }
              break;

            case System.Xml.XmlNodeType.EndElement:
                if (this.callBackHandler != null)
                  this.callBackHandler.EndElement(xmlReader.NamespaceURI, xmlReader.LocalName, xmlReader.Name);

              n_nsdecls = (int)nsDeclsCount.Pop();
              while (n_nsdecls > 0) {
                String prefixName = (String)prefixes.Pop();
                if (this.callBackHandler != null)
                  this.callBackHandler.EndPrefixMapping(prefixName);
                --n_nsdecls;
              }
              break;

            case System.Xml.XmlNodeType.Text:
              if (this.callBackHandler != null)
                this.callBackHandler.Characters(xmlReader.Value.ToCharArray(), 0, xmlReader.Value.Length);
              break;

            case System.Xml.XmlNodeType.Whitespace:
              if (!bDocument && this.callBackHandler != null)
                this.callBackHandler.IgnorableWhitespace(xmlReader.Value.ToCharArray(), 0, xmlReader.Value.Length);
              break;

            case System.Xml.XmlNodeType.ProcessingInstruction:
              if (this.callBackHandler != null)
                this.callBackHandler.ProcessingInstruction(xmlReader.Name, xmlReader.Value);
              break;

            case System.Xml.XmlNodeType.Comment:
              if (this.lexicalHandler != null)
                this.lexicalHandler.Comment(xmlReader.Value.ToCharArray(), 0, xmlReader.Value.Length);
              break;

            case System.Xml.XmlNodeType.CDATA:
              if (this.lexicalHandler != null) {
                lexicalHandler.StartCData();
                if (this.callBackHandler != null)
                  this.callBackHandler.Characters(xmlReader.Value.ToCharArray(), 0, xmlReader.Value.ToCharArray().Length);
                lexicalHandler.EndCData();
              }
              break;

            case System.Xml.XmlNodeType.DocumentType:
              if (this.lexicalHandler != null) {
                this.lexicalHandler.StartDtd(xmlReader.Name, null, null);
                this.lexicalHandler.EndDtd();
              }
              break;

            case System.Xml.XmlNodeType.EntityReference:
              if (this.callBackHandler != null) {
                this.callBackHandler.SkippedEntity(xmlReader.Name);
              }
              break;

            default:
              break;
          }
        }
        if (this.callBackHandler != null)
          this.callBackHandler.EndDocument();
      }
      catch (System.Xml.XmlException e) {
        throw e;
      }
      finally {
        xmlReader.Close();
      }
    }

    /// <summary>
    /// Parses the specified XML document into SAX events.
    /// </summary>
    /// <param name="source">XML document</param>
    public virtual void Parse(InputSource source) {
      if (source is InputSource<Stream>) {
        Parse(((InputSource<Stream>)source).Source, source.SystemId);
      }
      else if (source.SystemId != null) {
        Parse(source.SystemId);
      }
      else {
        throw new SaxException("InputSource's SystemId can't be null.");
      }
    }

    /// <summary>
    /// Parses the specified XML document into SAX events.
    /// </summary>
    /// <param name="source">XML document</param>
    public virtual void Parse(XmlReader xmlReader, System.String systemId) {
      try {
        this.DoParsing(xmlReader, systemId);
      }
      catch (System.Xml.XmlException e) {
        if (this.errorHandler != null)
          this.errorHandler.FatalError(new ParseErrorImpl(e.Message, (String)null, e.SourceUri.ToString(), e.LineNumber, e.LinePosition, e));
        else
          throw new SaxParseException(e.Message, e);
      }
    }

    /// <summary>
    /// Parses the specified stream and process the events over the specified handler, and resolves the 
    /// entities with the specified URI.
    /// </summary>
    /// <param name="stream">The stream with the XML.</param>
    /// <param name="handler">The handler that manage the parser events.</param>
    /// <param name="URI">The namespace URI for resolve external etities.</param>
    private void Parse(System.IO.Stream stream, System.String systemId) {
      try {
        XmlReader xmlReader;
        XmlReaderSettings settings = new XmlReaderSettings();
        settings.ValidationType = System.Xml.ValidationType.None;
        settings.XmlResolver = new XmlResolverAdapter(entityResolver);
        xmlReader = XmlReader.Create(stream, settings, systemId);
        this.DoParsing(xmlReader, systemId);
      }
      catch (System.Xml.XmlException e) {
        if (this.errorHandler != null)
          this.errorHandler.FatalError(new ParseErrorImpl(e.Message, (String)null, e.SourceUri.ToString(), e.LineNumber, e.LinePosition, e));
        else
          throw new SaxParseException(e.Message, e);
      }
    }

    /// <summary>
    /// Parses the specified file path and processes the events over previously specified handler.
    /// </summary>
    /// <param name="systemId">The path of the file with the XML.</param>
    private void Parse(String systemId) {
      try {
        XmlReaderSettings settings = new XmlReaderSettings();
        settings.ValidationType = System.Xml.ValidationType.None;
        settings.XmlResolver = new XmlResolverAdapter(entityResolver);
        XmlReader xmlReader = XmlReader.Create(systemId, settings);
        this.DoParsing(xmlReader, systemId);
      }
      catch (System.Xml.XmlException e) {
        if (this.errorHandler != null)
          this.errorHandler.FatalError(new ParseErrorImpl(e.Message, (String)null, e.SourceUri.ToString(), e.LineNumber, e.LinePosition, e));
        else
          throw new SaxParseException(e.Message, e);
      }
    }

    /*******************************/
    /// <summary>
    /// Relays XmlUrlResolver#GetEntity call to XmlSaxEntityResolver
    /// </summary>
    private class XmlResolverAdapter : XmlUrlResolver {
      private IEntityResolver m_entityResolver;
      internal XmlResolverAdapter(IEntityResolver entityResolver) {
        m_entityResolver = entityResolver;
      }
      public override Object GetEntity(Uri absoluteUri, string role, Type ofObjectToReturn) {
        if (m_entityResolver != null) {
          InputSource<MemoryStream> inputSource = (InputSource<MemoryStream>)m_entityResolver.ResolveEntity("", null, null, absoluteUri.ToString());
          if (inputSource != null) {
            return inputSource.Source;
          }
        }
        return base.GetEntity(absoluteUri, role, ofObjectToReturn);
      }
    }
  }

}
