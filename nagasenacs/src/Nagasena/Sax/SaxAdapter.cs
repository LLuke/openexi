using System;
using System.Collections;
using System.Diagnostics;
using System.IO;
using System.Xml;

using Org.System.Xml.Sax;
//using ILocator = Org.System.Xml.Sax.ILocator;
using LocatorImpl = Org.System.Xml.Sax.Helpers.LocatorImpl;

using SaxParseException = Org.System.Xml.Sax.SaxParseException;

using AttributesImpl = Org.System.Xml.Sax.Helpers.AttributesImpl;
using ParseErrorImpl = Org.System.Xml.Sax.Helpers.ParseErrorImpl;

namespace Nagasena.Sax {

  /*******************************/
  /// <summary>
  /// Emulates the SAX parsers behaviours.
  /// </summary>
  public class SaxAdapter : IXmlReader {

    public IContentHandler ContentHandler {
      get {
        return callBackHandler;
      }
      set {
        callBackHandler = value;
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

    protected XmlReader reader;
    private IContentHandler callBackHandler;
    private IErrorHandler errorHandler;
    //protected XmlSaxLocatorImpl locator;
    protected ILexicalHandler lexicalHandler;
    protected IEntityResolver entityResolver;
    protected System.String parserFileName;

    /// <summary>
    /// Public constructor for the class.
    /// </summary>
    public SaxAdapter() {
      reader = null;
      callBackHandler = null;
      errorHandler = null;
      locator = null;
      lexicalHandler = null;
      entityResolver = null;
      parserFileName = "";
    }

    public void Suspend() {
    }
    public void Abort() {
    }
    public void Resume() {
    }

    /// <summary>
    /// Emulates the behaviour of a SAX LocatorImpl object.
    /// </summary>
    private void UpdateLocatorData(LocatorImpl locator, System.Xml.XmlReader xmlReader) {
      if (locator != null && xmlReader != null) {
        locator.ColumnNumber = xmlReader.Settings.LinePositionOffset;
        locator.LineNumber = xmlReader.Settings.LinePositionOffset;
        locator.SystemId = parserFileName;
      }
    }

    /// <summary>
    /// Emulates the behavior of a SAX parsers. Set the value of a feature.
    /// </summary>
    /// <param name="name">The feature name, which is a fully-qualified URI.</param>
    /// <param name="value">The requested value for the feature.</param>
    public virtual void SetFeature(System.String name, bool value) {
      switch (name) {
        default:
          throw new ManagerNotRecognizedException("The specified feature: " + name + " are not supported");
      }
    }

    /// <summary>
    /// Emulates the behavior of a SAX parsers. Gets the value of a feature.
    /// </summary>
    /// <param name="name">The feature name, which is a fully-qualified URI.</param>
    /// <returns>The requested value for the feature.</returns>
    public virtual bool GetFeature(System.String name) {
      switch (name) {
        default:
          throw new ManagerNotRecognizedException("The specified feature: " + name +" are not supported");
      }
    }

    ///// <summary>
    ///// Emulates the behavior of a SAX parsers. Sets the value of a property.
    ///// </summary>
    ///// <param name="name">The property name, which is a fully-qualified URI.</param>
    ///// <param name="value">The requested value for the property.</param>
    //public virtual void setProperty(System.String name, System.Object value) {
    //  switch (name) {
    //    case "http://xml.org/sax/properties/lexical-handler": {
    //      try {
    //        lexicalHandler = (IlexicalHandler)value;
    //        break;
    //      }
    //      catch (System.Exception e) {
    //        throw new ManagerNotSupportedException("The property is not supported as an internal exception was thrown when trying to set it: " + e.Message);
    //      }
    //    }
    //    default:
    //      throw new ManagerNotRecognizedException("The specified feature: " + name + " is not recognized");
    //  }
    //}

    /// <summary>
    /// Emulates the behavior of a SAX parsers. Gets the value of a property.
    /// </summary>
    /// <param name="name">The property name, which is a fully-qualified URI.</param>
    /// <returns>The requested value for the property.</returns>
    public virtual IProperty<T> GetProperty<T>(System.String name) {
      switch (name) {
        case "http://xml.org/sax/properties/lexical-handler": {
          try {
            // REVISIT: 
            return null;
            //return this.lexical;
          }
          catch {
            throw new ManagerNotSupportedException("The specified operation was not performed");
          }
        }
        default:
          throw new ManagerNotRecognizedException("The specified feature: " + name + " are not supported");
      }
    }

    /// <summary>
    /// Emulates the behavior of a SAX parser, it realizes the callback events of the parser.
    /// </summary>
    private void DoParsing() {
      bool bDocument = true;
      Stack/*<int>*/ nsDeclsCount = new Stack();
      Stack/*<String>*/ prefixes = new Stack();
      locator = new LocatorImpl();
      try {
        UpdateLocatorData(this.locator, this.reader);
        if (this.callBackHandler != null)
          this.callBackHandler.SetDocumentLocator(locator);
        if (this.callBackHandler != null)
          this.callBackHandler.StartDocument();
        while (this.reader.Read()) {
          UpdateLocatorData(this.locator, this.reader);
          int n_nsdecls;
          switch (this.reader.NodeType) {
            case System.Xml.XmlNodeType.Element:
              if (bDocument) {
                bDocument = false;
              }
              bool Empty = reader.IsEmptyElement;
              System.String namespaceURI = "";
              System.String localName = "";
              namespaceURI = reader.NamespaceURI;
              localName = reader.LocalName;
              n_nsdecls = 0;
              System.String name = reader.Name;
              AttributesImpl attributes = new AttributesImpl();
              if (reader.HasAttributes) {
                for (int i = 0; i < reader.AttributeCount; i++) {
                  reader.MoveToAttribute(i);
                  System.String prefixName = (reader.Name.IndexOf(":") > 0) ? reader.Name.Substring(reader.Name.IndexOf(":") + 1, reader.Name.Length - reader.Name.IndexOf(":") - 1) : "";
                  System.String prefix = (reader.Name.IndexOf(":") > 0) ? reader.Name.Substring(0, reader.Name.IndexOf(":")) : reader.Name;
                  bool IsXmlns = prefix.ToLower().Equals("xmlns");
                  if (!IsXmlns)
                    attributes.AddAttribute(reader.NamespaceURI, reader.LocalName, reader.Name, "" + reader.NodeType, reader.Value, true);
                  else {
                    ++n_nsdecls;
                    prefixes.Push(prefixName);
                    if (this.callBackHandler != null)
                      this.callBackHandler.StartPrefixMapping(prefixName, reader.Value);
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
                  this.callBackHandler.EndElement(reader.NamespaceURI, reader.LocalName, reader.Name);

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
                this.callBackHandler.Characters(reader.Value.ToCharArray(), 0, reader.Value.Length);
              break;

            case System.Xml.XmlNodeType.Whitespace:
              if (!bDocument && this.callBackHandler != null)
                this.callBackHandler.IgnorableWhitespace(reader.Value.ToCharArray(), 0, reader.Value.Length);
              break;

            case System.Xml.XmlNodeType.ProcessingInstruction:
              if (this.callBackHandler != null)
                this.callBackHandler.ProcessingInstruction(reader.Name, reader.Value);
              break;

            case System.Xml.XmlNodeType.Comment:
              if (this.lexicalHandler != null)
                this.lexicalHandler.Comment(reader.Value.ToCharArray(), 0, reader.Value.Length);
              break;

            case System.Xml.XmlNodeType.CDATA:
              if (this.lexicalHandler != null) {
                lexicalHandler.StartCData();
                if (this.callBackHandler != null)
                  this.callBackHandler.Characters(this.reader.Value.ToCharArray(), 0, this.reader.Value.ToCharArray().Length);
                lexicalHandler.EndCData();
              }
              break;

            case System.Xml.XmlNodeType.DocumentType:
              //if (this.lexical != null) {
              //  System.String lname = this.reader.Name;
              //  System.String systemId = null;
              //  if (this.reader.AttributeCount > 0)
              //    systemId = this.reader.GetAttribute(0);
              //  this.lexical.startDTD(lname, null, systemId);
              //  this.lexical.startEntity("[dtd]");
              //  this.lexical.endEntity("[dtd]");
              //  this.lexical.endDTD();
              //}
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
        this.reader.Close();
      }
    }

    ///// <summary>
    ///// Parses the specified file path and process the events over the specified handler.
    ///// </summary>
    ///// <param name="filepath">The path of the file to be used.</param>
    //public virtual void parse(System.String filepath) {
    //  try {
    //    if (handler is XmlSaxDefaultHandler) {
    //      this.errorHandler = (XmlSaxDefaultHandler) handler;
    //    }
    //    if (!(this is XmlSaxParserAdapter))
    //      this.callBackHandler = handler;
    //    else {
    //      if(this.callBackHandler == null)
    //        this.callBackHandler = handler;
    //    }
    //    XmlReaderSettings settings = new XmlReaderSettings();
    //    settings.ProhibitDtd = false;
    //    settings.ValidationType = (this.isValidating) ? System.Xml.ValidationType.DTD : System.Xml.ValidationType.None;
    //    settings.ValidationEventHandler += new System.Xml.Schema.ValidationEventHandler(this.ValidationEventHandle);
    //    settings.XmlResolver = new XmlResolverAdapter(entityResolver);
    //    this.reader = XmlReader.Create(filepath, settings);
    //    parserFileName = filepath;
    //    this.DoParsing();
    //  }
    //  catch (System.Xml.XmlException e) {
    //    if (this.errorHandler != null)
    //      this.errorHandler.fatalError(e);
    //    throw e;
    //  }
    //}

    ///// <summary>
    ///// Parses the specified stream and process the events over the specified handler.
    ///// </summary>
    ///// <param name="stream">The stream with the XML.</param>
    ///// <param name="handler">The handler that manage the parser events.</param>
    //public virtual void parse(System.IO.Stream stream, XmlSaxContentHandler handler) {
    //  try {
    //    if (handler is XmlSaxDefaultHandler) {
    //      this.errorHandler = (XmlSaxDefaultHandler) handler;
    //    }
    //    if (!(this is XmlSaxParserAdapter))
    //      this.callBackHandler = handler;
    //    else {
    //      if(this.callBackHandler == null)
    //        this.callBackHandler = handler;
    //    }
    //    XmlReaderSettings settings = new XmlReaderSettings();
    //    settings.ProhibitDtd = false;
    //    settings.ValidationType = (this.isValidating) ? System.Xml.ValidationType.DTD : System.Xml.ValidationType.None;
    //    settings.ValidationEventHandler += new System.Xml.Schema.ValidationEventHandler(this.ValidationEventHandle);
    //    settings.XmlResolver = new XmlResolverAdapter(entityResolver);
    //    this.reader = XmlReader.Create(stream, settings);
    //    parserFileName = null;
    //    this.DoParsing();
    //  }
    //  catch (System.Xml.XmlException e) {
    //    if (this.errorHandler != null)
    //      this.errorHandler.fatalError(e);
    //    throw e;
    //  }
    //}

    /// <summary>
    /// Parses the specified stream and process the events over the specified handler, and resolves the 
    /// entities with the specified URI.
    /// </summary>
    /// <param name="stream">The stream with the XML.</param>
    /// <param name="handler">The handler that manage the parser events.</param>
    /// <param name="URI">The namespace URI for resolve external etities.</param>
    public virtual void Parse(System.IO.Stream stream, System.String URI) {
      try {
        //if (handler is XmlSaxDefaultHandler) {
        //  this.errorHandler = (XmlSaxDefaultHandler) handler;
        //}
        //if (!(this is XmlSaxParserAdapter))
        //  this.callBackHandler = handler;
        //else {
        //  if(this.callBackHandler == null)
        //    this.callBackHandler = handler;
        //}
        XmlReaderSettings settings = new XmlReaderSettings();
        settings.ProhibitDtd = false;
        settings.ValidationType = System.Xml.ValidationType.None;
        settings.ValidationEventHandler += new System.Xml.Schema.ValidationEventHandler(this.ValidationEventHandle);
        settings.XmlResolver = new XmlResolverAdapter(entityResolver);
        this.reader = XmlReader.Create(stream, settings, URI);
        parserFileName = null;
        this.DoParsing();
      }
      catch (System.Xml.XmlException e) {
        if (this.errorHandler != null)
          this.errorHandler.FatalError(new ParseErrorImpl(e.Message, (String)null, e.SourceUri.ToString(), e.LineNumber, e.LinePosition, e));
        throw e;
      }
    }

    ///// <summary>
    ///// Parses the specified stream and process the events over the specified handler.
    ///// </summary>
    ///// <param name="textReader">The stream with the XML.</param>
    ///// <param name="handler">The handler that manage the parser events.</param>
    //public virtual void parse(System.IO.TextReader textReader) {
    //  parse(textReader, (XmlSaxContentHandler)null);
    //}

    ///// <summary>
    ///// Parses the specified stream and process the events over the specified handler.
    ///// </summary>
    ///// <param name="textReader">The stream with the XML.</param>
    ///// <param name="handler">The handler that manage the parser events.</param>
    //public virtual void parse(System.IO.TextReader textReader, XmlSaxContentHandler handler) {
    //  try
    //  {
    //    if (handler != null) {
    //      if (handler is XmlSaxDefaultHandler) {
    //        this.errorHandler = (XmlSaxDefaultHandler)handler;
    //      }
    //      if (!(this is XmlSaxParserAdapter))
    //        this.callBackHandler = handler;
    //      else {
    //        if (this.callBackHandler == null)
    //          this.callBackHandler = handler;
    //      }
    //    }
    //    XmlReaderSettings settings = new XmlReaderSettings();
    //    settings.ProhibitDtd = false;
    //    settings.ValidationType = (this.isValidating) ? System.Xml.ValidationType.DTD : System.Xml.ValidationType.None;
    //    settings.ValidationEventHandler += new System.Xml.Schema.ValidationEventHandler(this.ValidationEventHandle);
    //    settings.XmlResolver = new XmlResolverAdapter(entityResolver);
    //    this.reader = XmlReader.Create(textReader, settings);
    //    parserFileName = null;
    //    this.DoParsing();
    //  }
    //  catch (System.Xml.XmlException e) {
    //    if (this.errorHandler != null)
    //      this.errorHandler.fatalError(e);
    //    throw e;
    //  }
    //}

    /// <summary>
    /// Parses the specified 'XmlSourceSupport' instance and process the events over the specified handler, 
    /// and resolves the entities with the specified URI.
    /// </summary>
    /// <param name="source">The 'XmlSourceSupport' that contains the XML.</param>
    /// <param name="handler">The handler that manages the parser events.</param>
    public virtual void Parse(InputSource source) {
        if (source is InputSource<MemoryStream>)
          Parse(((InputSource<MemoryStream>)source).Source, source.SystemId);
        else {
          if (source.SystemId != null)
            Parse(source.SystemId);
          else
            throw new System.Xml.XmlException("InputSource's SystemId can't be null");
        }
    }

    /// <summary>
    /// Parses the specified file path and processes the events over previously specified handler.
    /// </summary>
    /// <param name="filepath">The path of the file with the XML.</param>
    public virtual void Parse(String filepath) {
      try {
        XmlReaderSettings settings = new XmlReaderSettings();
        settings.ProhibitDtd = false;
        settings.ValidationType = System.Xml.ValidationType.None;
        settings.ValidationEventHandler += new System.Xml.Schema.ValidationEventHandler(this.ValidationEventHandle);
        settings.XmlResolver = new XmlResolverAdapter(entityResolver);
        this.reader = XmlReader.Create(filepath, settings);
        parserFileName = filepath;
        this.DoParsing();
      }
      catch (System.Xml.XmlException e) {
        if (this.errorHandler != null)
          this.errorHandler.FatalError(new ParseErrorImpl(e.Message, (String)null, e.SourceUri.ToString(), e.LineNumber, e.LinePosition, e));
        throw e;
      }
    }

    /// <summary>
    /// Manages all the exceptions that were thrown when the validation over XML fails.
    /// </summary>
    public void ValidationEventHandle(System.Object sender, System.Xml.Schema.ValidationEventArgs args) {
      System.Xml.Schema.XmlSchemaException tempException = args.Exception;
      if (args.Severity == System.Xml.Schema.XmlSeverityType.Warning) {
        if (this.errorHandler != null)
          this.errorHandler.Warning(
            new ParseErrorImpl(tempException.Message, (String)null, tempException.SourceUri.ToString(), tempException.LineNumber, tempException.LinePosition, tempException));
      }
      else {
        if (this.errorHandler != null)
          this.errorHandler.FatalError(
            new ParseErrorImpl(tempException.Message, (String)null, tempException.SourceUri.ToString(), tempException.LineNumber, tempException.LinePosition, tempException));
      }
    }
        
    ///// <summary>
    ///// Assigns the object that will handle all the error events. 
    ///// </summary>
    ///// <param name="handler">The object that handles the errors events.</param>
    //public virtual void setErrorHandler(XmlSaxErrorHandler handler) {
    //  this.errorHandler = handler;
    //}

    ///// <summary>
    ///// Obtains the object that will handle all the content events.
    ///// </summary>
    ///// <returns>The object that handles the content events.</returns>
    //public virtual XmlSaxContentHandler getContentHandler() {
    //  return this.callBackHandler;
    //}

    ///// <summary>
    ///// Assigns the object that will handle all the error events. 
    ///// </summary>
    ///// <returns>The object that handles the error events.</returns>
    //public virtual XmlSaxErrorHandler getErrorHandler() {
    //  return this.errorHandler;
    //}

    ///// <summary>
    ///// Returns the current entity resolver.
    ///// </summary>
    ///// <returns>The current entity resolver, or null if none has been registered.</returns>
    //public virtual XmlSaxEntityResolver getEntityResolver() {
    //  return this.entityResolver;
    //}

    ///// <summary>
    ///// Allows an application to register an entity resolver.
    ///// </summary>
    ///// <param name="resolver">The entity resolver.</param>
    //public virtual void setEntityResolver(XmlSaxEntityResolver resolver) {
    //  this.entityResolver = resolver;
    //}

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

  /*******************************/
  /// <summary>
  /// This exception is thrown by the XmlSaxDocumentManager in the SetProperty and SetFeature methods 
  /// if a property or method couldn't be supported.
  /// </summary>
  public class ManagerNotSupportedException : System.Exception
  {
    /// <summary>
    /// Creates a new ManagerNotSupportedException with the message specified.
    /// </summary>
    /// <param name="Message">Error message of the exception.</param>
    public ManagerNotSupportedException(System.String Message)
      : base(Message) {
    }
  }

  /*******************************/
  /// <summary>
  /// This exception is thrown by the XmlSaxDocumentManager in the SetProperty and SetFeature 
  /// methods if a property or method couldn't be found.
  /// </summary>
  public class ManagerNotRecognizedException : System.Exception
  {
    /// <summary>
    /// Creates a new ManagerNotRecognizedException with the message specified.
    /// </summary>
    /// <param name="Message">Error message of the exception.</param>
    public ManagerNotRecognizedException(System.String Message)
      : base(Message) {
    }
  }


}
