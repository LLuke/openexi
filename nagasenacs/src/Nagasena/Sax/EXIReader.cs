using System;
using System.Diagnostics;
using System.IO;

using Org.System.Xml.Sax;

using Characters = Nagasena.Schema.Characters;
using AlignmentType = Nagasena.Proc.Common.AlignmentType;
using EventDescription = Nagasena.Proc.Common.EventDescription;
using EventDescription_Fields = Nagasena.Proc.Common.EventDescription_Fields;
using EXIOptionsException = Nagasena.Proc.Common.EXIOptionsException;
using QName = Nagasena.Proc.Common.QName;
using XmlUriConst = Nagasena.Proc.Common.XmlUriConst;
using EXIEventDTD = Nagasena.Proc.Events.EXIEventDTD;
using EXIEventNS = Nagasena.Proc.Events.EXIEventNS;
using EXIEventSchemaNil = Nagasena.Proc.Events.EXIEventSchemaNil;
using EXIEventSchemaType = Nagasena.Proc.Events.EXIEventSchemaType;
using Scanner = Nagasena.Proc.IO.Scanner;
using EXISchemaResolver = Nagasena.Proc.EXISchemaResolver;

namespace Nagasena.Sax {

  /// <summary>
  /// EXIReader implements the SAX XMLReader to provide a convenient and 
  /// familiar interface for decoding an EXI stream.
  /// </summary>
  public sealed class EXIReader : ReaderSupport {

    private bool m_hasLexicalHandler;
    private ILexicalHandler m_lexicalHandler;

    private static readonly Characters CHARACTERS_TRUE;
    private static readonly Characters CHARACTERS_FALSE;
    static EXIReader() {
      CHARACTERS_TRUE = new Characters("true".ToCharArray(), 0, "true".Length, false);
      CHARACTERS_FALSE = new Characters("false".ToCharArray(), 0, "false".Length, false);
    }

    public EXIReader() : base() {
      m_hasLexicalHandler = false;
      m_lexicalHandler = null;
    }

    ///////////////////////////////////////////////////////////////////////////
    // XMLReader APIs
    ///////////////////////////////////////////////////////////////////////////

    /// <summary>
    /// This method wraps the friendlier setLexicalHandler method to provide 
    /// syntax familiar to experienced SAX programmers. The only property 
    /// supported is: <pre>http://xml.org/sax/properties/lexical-handler</pre> </summary>
    /// <param name="name"> must equal "http://xml.org/sax/properties/lexical-handler" </param>
    /// <param name="value"> an Org.System.Xml.Sax.ext.LexicalHandler object </param>
    public void SetProperty(string name, object value) {
      if ("http://xml.org/sax/properties/lexical-handler".Equals(name)) {
        LexicalHandler = (ILexicalHandler)value;
        return;
      }
      else if ("http://xml.org/sax/properties/declaration-handler".Equals(name)) {
        // REVISIT: add support for declaration handler.
        return;
      }
      throw new ArgumentException("Property '" + name + "' is not recognized.");
    }

    /// <summary>
    /// Use to retrieve the name of the lexical handler, currently the only
    /// property recognized by this class. Pass the String
    /// "http://xml.org/sax/properties/lexical-handler" as the name. </summary>
    /// <returns> String name of the lexical handler </returns>
    /// 
    public IProperty<T> GetProperty<T>(string name) {
      if ("http://xml.org/sax/properties/lexical-handler".Equals(name)) {
        return (IProperty<T>)this.m_lexicalHandler;
      }
      else if ("http://xml.org/sax/properties/declaration-handler".Equals(name)) {
        // REVISIT: add support for declaration handler.
        return null;
      }
      throw new ArgumentException("Property '" + name + "' is not recognized.");
    }

    /// <summary>
    /// Set features for the SAX parser. The only supported arguments are <pre>
    /// EXIReader.setFeature("http://xml.org/sax/features/namespaces", true);</pre> and <pre>
    /// EXIReader.setFeature("http://xml.org/sax/features/namespace-prefixes", false);</pre>
    /// </summary>
    public void SetFeature(string name, bool value) {
      if ("http://xml.org/sax/features/namespaces".Equals(name)) {
        if (!value) {
          throw new NotSupportedException("");
        }
        return;
      }
      else if ("http://xml.org/sax/features/namespace-prefixes".Equals(name)) {
        if (value) {
          throw new NotSupportedException("");
        }
        return;
      }
      throw new ArgumentException("Feature '" + name + "' is not recognized.");
    }

    /// <summary>
    /// Get features for the SAX parser. </summary>
    /// <returns> <i>true</i> if the feature is "http://xml.org/sax/features/namespaces"
    /// and <i>false</i> if the feature is "http://xml.org/sax/features/namespace-prefixes" </returns>
    public bool GetFeature(string name) {
      if ("http://xml.org/sax/features/namespaces".Equals(name)) {
        return true;
      }
      else if ("http://xml.org/sax/features/namespace-prefixes".Equals(name)) {
        return false;
      }
      throw new ArgumentException("Feature '" + name + "' is not recognized.");
    }

    ///////////////////////////////////////////////////////////////////////////
    // Methods to configure EXIDecoder
    ///////////////////////////////////////////////////////////////////////////

    /// <summary>
    /// Set the bit alignment style used to compile the EXI input stream. </summary>
    /// <param name="alignmentType"> <seealso cref="Nagasena.Proc.common.AlignmentType"/> </param>
    /// <exception cref="EXIOptionsException"> </exception>
    public AlignmentType AlignmentType {
      set {
        m_decoder.AlignmentType = value;
      }
    }

    /// <summary>
    /// Set to true if the EXI input stream is an XML fragment (a non-compliant
    /// XML document with multiple root elements). </summary>
    /// <param name="isFragment"> true if the EXI input stream is an XML fragment. </param>
    public bool Fragment {
      set {
        m_decoder.Fragment = value;
      }
    }

    /// <summary>
    /// Set to true if the EXI input stream was compiled with the Preserve Lexical
    /// Values set to true. The original strings, rather than logical XML
    /// equivalents, are restored in the XML output stream. </summary>
    /// <param name="preserveLexicalValues"> set to true if the EXI input stream was compiled with 
    /// Preserve Lexical Values set to true. </param>
    /// <exception cref="EXIOptionsException"> </exception>
    public bool PreserveLexicalValues {
      set {
        m_decoder.PreserveLexicalValues = value;
      }
    }

    /// <summary>
    /// Set the EXISchemaResolver to retrieve the schema needed to decode the 
    /// current EXI stream. </summary>
    /// <param name="schemaResolver"> <seealso cref="Nagasena.Proc.EXISchemaResolver"/> </param>
    public EXISchemaResolver EXISchemaResolver {
      set {
        m_decoder.EXISchemaResolver = value;
      }
    }

    /// <summary>
    /// Set a datatype representation map. </summary>
    /// <param name="dtrm"> a sequence of pairs of datatype qname and datatype representation qname </param>
    /// <param name="n_bindings"> the number of qname pairs </param>
    public void setDatatypeRepresentationMap(QName[] dtrm, int n_bindings) {
      m_decoder.setDatatypeRepresentationMap(dtrm, n_bindings);
    }

    /// <summary>
    /// Set the size, in number of values, of the information that will be 
    /// processed as a chunk of the entire EXI stream. Reducing the block size 
    /// can improve performance for devices with limited dynamic memory. 
    /// Default is 1,000,000 items (not 1MB, but 1,000,000 complete Attribute 
    /// and Element values). Block size is only used when the EXI stream is
    /// encoded with EXI-compression. </summary>
    /// <param name="blockSize"> number of values in each processing block. Default is 1,000,000. </param>
    /// <exception cref="EXIOptionsException"> </exception>
    public int BlockSize {
      set {
        m_decoder.BlockSize = value;
      }
    }

    /// <summary>
    /// Set the maximum length of a string that will be stored for reuse in the
    /// String Table. By default, there is no maximum length. However, in data
    /// sets that have long, unique strings of information, you can improve
    /// performance by limiting the size to the length of strings that are more
    /// likely to appear more than once. </summary>
    /// <param name="valueMaxLength"> maximum length of entries in the String Table.  </param>
    public int ValueMaxLength {
      set {
        m_decoder.ValueMaxLength = value;
      }
    }
    /// <summary>
    /// Set the maximum number of values in the String Table. By default, there
    /// is no limit. If the target device has limited dynamic memory, limiting 
    /// the number of entries in the String Table can improve performance and
    /// reduce the likelihood that you will exceed memory capacity. </summary>
    /// <param name="valuePartitionCapacity"> maximum number of entries in the String Table </param>
    public int ValuePartitionCapacity {
      set {
        m_decoder.ValuePartitionCapacity = value;
      }
    }

    ///////////////////////////////////////////////////////////////////////////
    // 
    ///////////////////////////////////////////////////////////////////////////

    /// <summary>
    /// Set a SAX lexical handler to receive SAX lexical events. </summary>
    /// <param name="lexicalHandler"> SAX lexical handler </param>
    public ILexicalHandler LexicalHandler {
      set {
        m_hasLexicalHandler = (m_lexicalHandler = value) != null;
      }
      get {
        return m_lexicalHandler;
      }
    }

    /// <summary>
    /// Parses an EXI input stream and reconstitute an XML.</summary>
    /// <param name="inputStream">an EXI stream to be decoded</param>
    public void Parse(Stream inputStream) {
      reset();
      Scanner scanner = processHeader(inputStream);

      EventDescription exiEvent;
      while ((exiEvent = scanner.nextEvent()) != null) {
        Characters characterSequence;
        switch (exiEvent.EventKind) {
          case EventDescription_Fields.EVENT_SD:
            m_contentHandler.StartDocument();
            break;
          case EventDescription_Fields.EVENT_ED:
            m_contentHandler.EndDocument();
            break;
          case EventDescription_Fields.EVENT_SE:
            doElement(exiEvent, scanner, 0);
            break;
          case EventDescription_Fields.EVENT_CM:
            if (m_hasLexicalHandler) {
              characterSequence = exiEvent.Characters;
              m_lexicalHandler.Comment(characterSequence.characters, characterSequence.startIndex, characterSequence.length);
            }
            break;
          case EventDescription_Fields.EVENT_PI:
            m_contentHandler.ProcessingInstruction(exiEvent.Name, exiEvent.Characters.makeString());
            break;
          case EventDescription_Fields.EVENT_DTD:
            if (m_hasLexicalHandler) {
              EXIEventDTD eventDTD = (EXIEventDTD)exiEvent;
              m_lexicalHandler.StartDtd(exiEvent.Name, eventDTD.PublicId, eventDTD.SystemId);
              m_lexicalHandler.EndDtd();
            }
            break;
          default:
            break;
        }
      }
    }

    private void doXsiNil(EventDescription exiEvent) {
      string attrQualifiedName, attrPrefix;
      if (m_preserveNS) {
        attrPrefix = exiEvent.Prefix;
        Debug.Assert(attrPrefix.Length != 0);
        stringBuilder.Length = 0;
        attrQualifiedName = stringBuilder.Append(attrPrefix).Append(":nil").ToString(/**/);
      }
      else {
        int uriId = exiEvent.URIId;
        Debug.Assert(uriId < m_n_prefixes);
        stringBuilder.Length = 0;
        attrQualifiedName = stringBuilder.Append(m_prefixesColon[uriId]).Append("nil").ToString(/**/);
      }
      addAttribute(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, "nil", attrQualifiedName, "", m_preserveLexicalValues ? exiEvent.Characters : ((EXIEventSchemaNil)exiEvent).Nilled ? CHARACTERS_TRUE : CHARACTERS_FALSE);
    }

    private bool doXsiType(EventDescription exiEvent) {
      bool namespaceDeclAdded = false;
      string attrQualifiedName, attrPrefix;
      if (m_preserveNS) {
        attrPrefix = exiEvent.Prefix;
        Debug.Assert(attrPrefix.Length != 0);
        stringBuilder.Length = 0;
        attrQualifiedName = stringBuilder.Append(attrPrefix).Append(":type").ToString(/**/);
      }
      else {
        int uriId = exiEvent.URIId;
        Debug.Assert(uriId < m_n_prefixes);
        stringBuilder.Length = 0;
        attrQualifiedName = stringBuilder.Append(m_prefixesColon[uriId]).Append("type").ToString(/**/);
      }
      EXIEventSchemaType eventSchemaType = (EXIEventSchemaType)exiEvent;
      string typeQualifiedName;
      if (m_preserveLexicalValues) {
        typeQualifiedName = eventSchemaType.Characters.makeString();
      }
      else {
        string typeName = eventSchemaType.TypeName;
        string typePrefix;
        if (m_preserveNS) {
          typePrefix = eventSchemaType.TypePrefix;
          if (typePrefix.Length != 0) {
            stringBuilder.Length = 0;
            typeQualifiedName = stringBuilder.Append(typePrefix).Append(':').Append(typeName).ToString(/**/);
          }
          else {
            typeQualifiedName = typeName;
          }
        }
        else {
          // REVISIT: use eventSchemaType.getTypeURIId()
          string typeUri = eventSchemaType.TypeURI;
          if (typeUri.Length != 0) {
            int i;
            for (i = m_n_namespaceDeclarations - 1; i > -1; i--) {
              if (typeUri.Equals(m_namespaceDeclarationsLocus[i << 1 | 1])) {
                break;
              }
            }
            if (i != -1) {
              typePrefix = m_namespaceDeclarationsLocus[i << 1];
            }
            else {
              if (m_n_namespaceDeclarations < PREFIXES.Length) {
                typePrefix = PREFIXES[m_n_namespaceDeclarations];
              }
              else {
                stringBuilder.Length = 0;
                typePrefix = stringBuilder.Append('p').Append(m_n_namespaceDeclarations).ToString(/**/);
              }
              m_contentHandler.StartPrefixMapping(typePrefix, typeUri);
              pushNamespaceDeclaration(typePrefix, typeUri);
              namespaceDeclAdded = true;
            }
            stringBuilder.Length = 0;
            typeQualifiedName = stringBuilder.Append(typePrefix).Append(':').Append(typeName).ToString(/**/);
          }
          else {
            typeQualifiedName = typeName;
          }
        }
      }
      char[] typeQualifiedNameChars = typeQualifiedName.ToCharArray();
      addAttribute(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, "type", attrQualifiedName, "", new Characters(typeQualifiedNameChars, 0, typeQualifiedNameChars.Length, false));
      return namespaceDeclAdded;
    }

    private void doElement(EventDescription exiEvent, Scanner scanner, int depth) {
      string elementURI;
      string elementLocalName;
      string elementQualifiedName;
      int n_namespaceDeclarations = 0;

      string prefix;

      m_attrLength = 0;
      elementURI = exiEvent.URI;
      elementLocalName = exiEvent.Name;
      if (m_preserveNS) {
        prefix = exiEvent.Prefix;
        while ((exiEvent = scanner.nextEvent()) != null && exiEvent.EventKind == EventDescription_Fields.EVENT_NS) {
          string _prefix;
          string nsUri = exiEvent.URI;
          string nsPrefix = exiEvent.Prefix;
          m_contentHandler.StartPrefixMapping(nsPrefix, nsUri);
          pushNamespaceDeclaration(nsPrefix, nsUri);
          ++n_namespaceDeclarations;
          _prefix = ((EXIEventNS)exiEvent).LocalElementNs ? nsPrefix : null;
          if (_prefix != null) {
            prefix = _prefix;
          }
        }
        if (prefix.Length != 0) {
          stringBuilder.Length = 0;
          elementQualifiedName = stringBuilder.Append(prefix).Append(':').Append(elementLocalName).ToString(/**/);
        }
        else {
          elementQualifiedName = elementLocalName;
        }
      }
      else {
        if (depth == 0) {
          for (int i = 1; i < m_n_prefixes; i++) {
            m_contentHandler.StartPrefixMapping(m_prefixes[i], m_uris[i]);
          }
        }
        int uriId = exiEvent.URIId;
        if (uriId < m_n_prefixes) {
          int nameId = exiEvent.NameId;
          if (nameId < m_n_qualifiedNames[uriId]) {
            elementQualifiedName = m_qualifiedNames[uriId][nameId];
          }
          else {
            stringBuilder.Length = 0;
            elementQualifiedName = stringBuilder.Append(m_prefixesColon[uriId]).Append(elementLocalName).ToString(/**/);
          }
        }
        else {
          int i;
          if (elementURI != "") {
            for (i = m_n_namespaceDeclarations - 1; i > -1; i--) {
              if (elementURI == m_namespaceDeclarationsLocus[i << 1 | 1]) {
                break;
              }
            }
            if (i > -1) {
              prefix = m_namespaceDeclarationsLocus[i << 1];
            }
            else {
              if (m_n_namespaceDeclarations < PREFIXES.Length) {
                prefix = PREFIXES[m_n_namespaceDeclarations];
              }
              else {
                stringBuilder.Length = 0;
                prefix = stringBuilder.Append('p').Append(m_n_namespaceDeclarations).ToString(/**/);
              }
              m_contentHandler.StartPrefixMapping(prefix, elementURI);
              pushNamespaceDeclaration(prefix, elementURI);
              ++n_namespaceDeclarations;
            }
            if (prefix.Length != 0) {
              stringBuilder.Length = 0;
              elementQualifiedName = stringBuilder.Append(prefix).Append(':').Append(elementLocalName).ToString(/**/);
            }
            else {
              elementQualifiedName = elementLocalName;
            }
          }
          else {
            for (i = m_n_namespaceDeclarations - 1; i > -1; i--) {
              // look for a namespace declaration for prefix ""
              if (m_namespaceDeclarationsLocus[i << 1].Length == 0) {
                if (m_namespaceDeclarationsLocus[i << 1 | 1].Length != 0) { // i.e. it was xmlns="..."
                  m_contentHandler.StartPrefixMapping("", ""); // reclaim the prefix "" for the uri ""
                  pushNamespaceDeclaration("", "");
                  ++n_namespaceDeclarations;
                }
                break;
              }
            }
            elementQualifiedName = elementLocalName;
          }
        }
        exiEvent = scanner.nextEvent();
      }
      if (exiEvent.EventKind == EventDescription_Fields.EVENT_TP) {
        if (doXsiType(exiEvent)) {
          ++n_namespaceDeclarations;
        }
        exiEvent = scanner.nextEvent();
      }
      if (exiEvent.EventKind == EventDescription_Fields.EVENT_NL) {
        doXsiNil(exiEvent);
        exiEvent = scanner.nextEvent();
      }
      while (exiEvent.EventKind == EventDescription_Fields.EVENT_AT) {
        if (doAttribute(exiEvent, scanner)) {
          ++n_namespaceDeclarations;
        }
        exiEvent = scanner.nextEvent();
      }
      m_contentHandler.StartElement(elementURI, elementLocalName, elementQualifiedName, this);

      do {
        switch (exiEvent.EventKind) {
          case EventDescription_Fields.EVENT_SE:
            doElement(exiEvent, scanner, depth + 1);
            break;
          case EventDescription_Fields.EVENT_EE:
            m_contentHandler.EndElement(elementURI, elementLocalName, elementQualifiedName);
            if (!m_preserveNS && depth == 0) {
              for (int i = 1; i < m_n_prefixes; i++) {
                m_contentHandler.EndPrefixMapping(m_prefixes[i]);
              }
            }
            int n_prefixes = n_namespaceDeclarations;
            for (int i = 0; i < n_prefixes; i++) {
              m_contentHandler.EndPrefixMapping(m_namespaceDeclarationsLocus[--m_n_namespaceDeclarations << 1]);
            }
            return;
          case EventDescription_Fields.EVENT_CH:
            Characters characterSequence;
            characterSequence = exiEvent.Characters;
            m_contentHandler.Characters(characterSequence.characters, characterSequence.startIndex, characterSequence.length);
            break;
          case EventDescription_Fields.EVENT_CM:
            if (m_hasLexicalHandler) {
              characterSequence = exiEvent.Characters;
              m_lexicalHandler.Comment(characterSequence.characters, characterSequence.startIndex, characterSequence.length);
            }
            break;
          case EventDescription_Fields.EVENT_PI:
            m_contentHandler.ProcessingInstruction(exiEvent.Name, exiEvent.Characters.makeString());
            break;
          case EventDescription_Fields.EVENT_ER:
            m_contentHandler.SkippedEntity(exiEvent.Name);
            break;
          case EventDescription_Fields.EVENT_NL:
          case EventDescription_Fields.EVENT_TP:
          case EventDescription_Fields.EVENT_AT:
            Debug.Assert(false);
            break;
        }
        exiEvent = scanner.nextEvent();
      }
      while (true);
    }

  }

}