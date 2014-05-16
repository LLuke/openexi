using System;
using System.Diagnostics;
using System.IO;
using System.Text;

using Attributes = Org.System.Xml.Sax.IAttributes;
using ContentHandler = Org.System.Xml.Sax.IContentHandler;
using SAXException = Org.System.Xml.Sax.SaxException;

using EXIDecoder = Nagasena.Proc.EXIDecoder;
using EXIOptionsException = Nagasena.Proc.Common.EXIOptionsException;
using EventDescription = Nagasena.Proc.Common.EventDescription;
using XmlUriConst = Nagasena.Proc.Common.XmlUriConst;
using GrammarCache = Nagasena.Proc.Grammars.GrammarCache;
using Scanner = Nagasena.Proc.IO.Scanner;
using Characters = Nagasena.Schema.Characters;
using EXISchema = Nagasena.Schema.EXISchema;

namespace Nagasena.Sax {

  public abstract class ReaderSupport : Attributes {

    protected internal readonly EXIDecoder m_decoder;

    protected internal ContentHandler m_contentHandler;

    protected internal string[] m_namespaceDeclarationsLocus;
    protected internal int m_n_namespaceDeclarations;

    protected internal int m_attrLength;
    private string[] m_attrData;
    private Characters[] m_attrValue;

    protected internal static readonly string[] PREFIXES = { "xml", 
    "p0", "p1", "p2", "p3", "p4", "p5", "p6", "p7", "p8", "p9",
    "p10", "p11", "p12", "p13", "p14", "p15", "p16", "p17", "p18", "p19",
    "p20", "p21", "p22", "p23", "p24", "p25", "p26", "p27", "p28", "p29",
    "p30", "p31", "p32", "p33", "p34", "p35", "p36", "p37", "p38", "p39",
    "p40", "p41", "p42", "p43", "p44", "p45", "p46", "p47", "p48", "p49",
    "p50", "p51", "p52", "p53", "p54", "p55", "p56", "p57", "p58", "p59",
    "p60", "p61", "p62" };

    protected internal bool m_preserveNS;
    protected internal bool m_preserveLexicalValues;

    protected internal int m_n_prefixes;
    protected internal string[] m_prefixes;
    protected internal string[] m_prefixesColon;
    protected internal string[] m_uris;
    protected internal int[] m_n_qualifiedNames;
    protected internal string[][] m_qualifiedNames;

    private const int ATTRIBUTE_URI_OFFSET = 0;
    private const int ATTRIBUTE_LOCALNAME_OFFSET = 1;
    private const int ATTRIBUTE_QNAME_OFFSET = 2;
    private const int ATTRIBUTE_TYPE_OFFSET = 3;
    private const int ATTRIBUTE_SZ = 4;

    protected internal readonly StringBuilder stringBuilder;

    protected internal ReaderSupport() {
      m_decoder = new EXIDecoder();
      m_namespaceDeclarationsLocus = new string[PREFIXES.Length * 2];
      m_attrData = new string[32 * ATTRIBUTE_SZ];
      m_attrValue = new Characters[32];
      stringBuilder = new StringBuilder();
      populatePrefixes((EXISchema)null);
    }

    protected internal virtual void reset() {
      m_n_namespaceDeclarations = 0;
      pushNamespaceDeclaration(PREFIXES[0], XmlUriConst.W3C_XML_1998_URI);
    }

    ///////////////////////////////////////////////////////////////////////////
    // XMLReader APIs
    ///////////////////////////////////////////////////////////////////////////

    /// <summary>
    /// Set a SAX content handler to receive SAX events. </summary>
    /// <param name="contentHandler"> SAX content handler </param>
    public ContentHandler ContentHandler {
      set {
        m_contentHandler = value;
      }
      get {
        return m_contentHandler;
      }
    }


    ///////////////////////////////////////////////////////////////////////////
    // Methods to configure ReaderSupport
    ///////////////////////////////////////////////////////////////////////////

    /// <summary>
    /// Set the GrammarCache used in parsing EXI streams. </summary>
    /// <param name="grammarCache"> <seealso cref="Nagasena.Proc.grammars.GrammarCache"/> </param>
    /// <exception cref="EXIOptionsException"> </exception>
    public virtual GrammarCache GrammarCache {
      set {
        populatePrefixes(value.EXISchema);
        m_decoder.GrammarCache = value;
      }
    }

    ///////////////////////////////////////////////////////////////////////////
    // 
    ///////////////////////////////////////////////////////////////////////////

    /// <summary>
    /// Parses the header and returns an scanner. </summary>
    /// <param name="inputStream"> </param>
    /// <returns> Scanner </returns>
    protected internal Scanner processHeader(Stream inputStream) {
      m_decoder.InputStream = inputStream;

      Scanner scanner;
      try {
        scanner = m_decoder.processHeader();
      }
      catch (EXIOptionsException eoe) {
        throw new SAXException(eoe.Message, eoe);
      }
      m_preserveNS = scanner.PreserveNS;
      m_preserveLexicalValues = scanner.PreserveLexicalValues;
      return scanner;
    }

    protected internal bool doAttribute(EventDescription exiEvent, Scanner scanner) {
      bool namespaceDeclAdded = false;
      string attrQualifiedName, attrPrefix;
      string attrUri = exiEvent.URI;
      string attrName = exiEvent.Name;
      if (m_preserveNS) {
        attrPrefix = exiEvent.Prefix;
        if (attrPrefix.Length != 0) {
          stringBuilder.Length = 0;
          attrQualifiedName = stringBuilder.Append(attrPrefix).Append(':').Append(attrName).ToString(/**/);
        }
        else {
          attrQualifiedName = attrName;
        }
      }
      else {
        int uriId = exiEvent.URIId;
        if (uriId < m_n_prefixes) {
          int nameId = exiEvent.NameId;
          if (nameId < m_n_qualifiedNames[uriId]) {
            attrQualifiedName = m_qualifiedNames[uriId][nameId];
          }
          else {
            stringBuilder.Length = 0;
            attrQualifiedName = stringBuilder.Append(m_prefixesColon[uriId]).Append(attrName).ToString(/**/);
          }
        }
        else {
          if (attrUri.Length != 0) {
            attrPrefix = "";
            int i;
            for (i = m_n_namespaceDeclarations - 1; i > -1; i--) {
              if (attrUri.Equals(m_namespaceDeclarationsLocus[i << 1 | 1])) {
                attrPrefix = m_namespaceDeclarationsLocus[i << 1];
                break;
              }
            }
            if (i == -1) {
              if (m_n_namespaceDeclarations < PREFIXES.Length) {
                attrPrefix = PREFIXES[m_n_namespaceDeclarations];
              }
              else {
                stringBuilder.Length = 0;
                attrPrefix = stringBuilder.Append('p').Append(m_n_namespaceDeclarations).ToString(/**/);
              }
              if (m_contentHandler != null) {
                m_contentHandler.StartPrefixMapping(attrPrefix, attrUri);
              }
              pushNamespaceDeclaration(attrPrefix, attrUri);
              namespaceDeclAdded = true;
            }
            if (attrPrefix.Length != 0) {
              stringBuilder.Length = 0;
              attrQualifiedName = stringBuilder.Append(attrPrefix).Append(':').Append(attrName).ToString(/**/);
            }
            else {
              attrQualifiedName = attrName;
            }
          }
          else {
            attrQualifiedName = attrName;
          }
        }
      }
      addAttribute(attrUri, attrName, attrQualifiedName, "", exiEvent.Characters);
      return namespaceDeclAdded;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Attribute Implementation
    ///////////////////////////////////////////////////////////////////////////

    /// <summary>
    /// @y.exclude </summary>
    public int Length {
      get {
        return m_attrLength;
      }
    }

    /// <summary>
    /// @y.exclude </summary>
    public string GetUri(int index) {
      if (-1 < index && index < m_attrLength) {
        return m_attrData[ATTRIBUTE_SZ * index + ATTRIBUTE_URI_OFFSET];
      }
      else {
        return null;
      }
    }

    /// <summary>
    /// @y.exclude </summary>
    public string GetLocalName(int index) {
      if (-1 < index && index < m_attrLength) {
        return m_attrData[ATTRIBUTE_SZ * index + ATTRIBUTE_LOCALNAME_OFFSET];
      }
      else {
        return null;
      }
    }

    /// <summary>
    /// @y.exclude </summary>
    public string GetQName(int index) {
      if (-1 < index && index < m_attrLength) {
        return m_attrData[ATTRIBUTE_SZ * index + ATTRIBUTE_QNAME_OFFSET];
      }
      else {
        return null;
      }
    }

    /// <summary>
    /// @y.exclude </summary>
    public string GetType(int index) {
      if (-1 < index && index < m_attrLength) {
        return m_attrData[ATTRIBUTE_SZ * index + ATTRIBUTE_TYPE_OFFSET];
      }
      else {
        return null;
      }
    }

    /// <summary>
    /// @y.exclude </summary>
    public string GetValue(int index) {
      if (-1 < index && index < m_attrLength) {
        return m_attrValue[index].makeString();
      }
      else {
        return null;
      }
    }

    /// <summary>
    /// @y.exclude </summary>
    public int GetIndex(string uri, string localName) {
      int max = m_attrLength * ATTRIBUTE_SZ;
      for (int i = 0; i < max; i += ATTRIBUTE_SZ) {
        if (m_attrData[i + ATTRIBUTE_URI_OFFSET].Equals(uri) && m_attrData[i + ATTRIBUTE_LOCALNAME_OFFSET].Equals(localName)) {
          return i / ATTRIBUTE_SZ;
        }
      }
      return -1;
    }

    /// <summary>
    /// @y.exclude </summary>
    public int GetIndex(string qName) {
      int max = m_attrLength * ATTRIBUTE_SZ;
      for (int i = 0; i < max; i += ATTRIBUTE_SZ) {
        if (m_attrData[i + ATTRIBUTE_QNAME_OFFSET].Equals(qName)) {
          return i / ATTRIBUTE_SZ;
        }
      }
      return -1;
    }

    /// <summary>
    /// @y.exclude </summary>
    public string GetType(string uri, string localName) {
      int max = m_attrLength * ATTRIBUTE_SZ;
      for (int i = 0; i < max; i += ATTRIBUTE_SZ) {
        if (m_attrData[i + ATTRIBUTE_URI_OFFSET].Equals(uri) && m_attrData[i + ATTRIBUTE_LOCALNAME_OFFSET].Equals(localName)) {
          return m_attrData[i + ATTRIBUTE_TYPE_OFFSET];
        }
      }
      return null;
    }

    /// <summary>
    /// @y.exclude </summary>
    public string GetType(string qName) {
      int max = m_attrLength * ATTRIBUTE_SZ;
      for (int i = 0; i < max; i += ATTRIBUTE_SZ) {
        if (m_attrData[i + ATTRIBUTE_QNAME_OFFSET].Equals(qName)) {
          return m_attrData[i + ATTRIBUTE_TYPE_OFFSET];
        }
      }
      return null;
    }

    /// <summary>
    /// @y.exclude </summary>
    public string GetValue(string uri, string localName) {
      int max = ATTRIBUTE_SZ * m_attrLength;
      for (int i = 0; i < max; i += ATTRIBUTE_SZ) {
        if (m_attrData[i + ATTRIBUTE_URI_OFFSET].Equals(uri) && m_attrData[i + ATTRIBUTE_LOCALNAME_OFFSET].Equals(localName)) {
          return m_attrValue[i / ATTRIBUTE_SZ].makeString();
        }
      }
      return null;
    }

    /// <summary>
    /// @y.exclude </summary>
    public string GetValue(string qName) {
      int max = ATTRIBUTE_SZ * m_attrLength;
      for (int i = 0; i < max; i += ATTRIBUTE_SZ) {
        if (m_attrData[i + ATTRIBUTE_QNAME_OFFSET].Equals(qName)) {
          return m_attrValue[i / ATTRIBUTE_SZ].makeString();
        }
      }
      return null;
    }

    public bool IsSpecified(int index) {
      return true;
    }

    public bool IsSpecified(string qName) {
      return true;
    }

    public bool IsSpecified(string uri, string localName) {
      return true;
    }

    protected internal void addAttribute(string uri, string localName, string qName, string type, Characters value) {
      Debug.Assert(value != null);
      int attrLength = m_attrLength + 1;
      if (m_attrData.Length < ATTRIBUTE_SZ * attrLength) {
        string[] attrData = new string[m_attrData.Length + 32 * ATTRIBUTE_SZ];
        Array.Copy(m_attrData, 0, attrData, 0, ATTRIBUTE_SZ * m_attrLength);
        m_attrData = attrData;
        Characters[] attrValue = new Characters[m_attrValue.Length + 32];
        Array.Copy(m_attrValue, 0, attrValue, 0, m_attrLength);
        m_attrValue = attrValue;
      }
      m_attrData[m_attrLength * ATTRIBUTE_SZ + ATTRIBUTE_URI_OFFSET] = uri;
      m_attrData[m_attrLength * ATTRIBUTE_SZ + ATTRIBUTE_LOCALNAME_OFFSET] = localName;
      m_attrData[m_attrLength * ATTRIBUTE_SZ + ATTRIBUTE_QNAME_OFFSET] = qName;
      m_attrData[m_attrLength * ATTRIBUTE_SZ + ATTRIBUTE_TYPE_OFFSET] = type;
      m_attrValue[m_attrLength] = value;
      m_attrLength = attrLength;
    }

    ///////////////////////////////////////////////////////////////////////////
    /// Convenience Functions
    ///////////////////////////////////////////////////////////////////////////

    protected internal void pushNamespaceDeclaration(string prefix, string uri) {
      if (2 * m_n_namespaceDeclarations == m_namespaceDeclarationsLocus.Length) {
        string[] namespaceDeclarationsLocus = new string[m_namespaceDeclarationsLocus.Length + 16];
        Array.Copy(m_namespaceDeclarationsLocus, 0, namespaceDeclarationsLocus, 0, m_namespaceDeclarationsLocus.Length);
        m_namespaceDeclarationsLocus = namespaceDeclarationsLocus;
      }
      int pos = m_n_namespaceDeclarations++ << 1;
      m_namespaceDeclarationsLocus[pos] = prefix;
      m_namespaceDeclarationsLocus[pos | 1] = uri;
    }

    private void populatePrefixes(EXISchema schema) {
      string[][] localNames;
      if (schema != null) {
          m_uris = schema.uris;
          localNames = schema.localNames;
      }
      else {
          m_uris = new string[] { "", XmlUriConst.W3C_XML_1998_URI, XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI };
          localNames = new string[3][];
          localNames[0] = null;
          localNames[1] = new string[] { "base", "id", "lang", "space" };
          localNames[2] = new string[] { "nil", "type" };
      }
      m_n_prefixes = m_uris.Length;
      m_prefixes = new string[m_n_prefixes];
      m_prefixesColon = new string[m_n_prefixes];
      m_prefixes[0] = "";
      m_prefixesColon[0] = "";
      m_prefixes[1] = "xml";
      m_prefixesColon[1] = "xml:";
      m_prefixes[2] = "xsi";
      m_prefixesColon[2] = "xsi:";
      if (m_n_prefixes > 3) {
        m_prefixes[3] = "xsd";
        m_prefixesColon[3] = "xsd:";
        for (int i = 4; i < m_n_prefixes; i++) {
          stringBuilder.Length = 0;
          m_prefixes[i] = stringBuilder.Append('s').Append(i - 4).ToString(/**/);
          stringBuilder.Length = 0;
          m_prefixesColon[i] = stringBuilder.Append(m_prefixes[i]).Append(':').ToString(/**/);
        }
      }

      m_n_qualifiedNames = new int[m_uris.Length];
      m_n_qualifiedNames[0] = 0;
      m_qualifiedNames = new string[m_uris.Length][];
      for (int i = 1; i < m_uris.Length; i++) {
        string[] names = localNames[i];
        m_n_qualifiedNames[i] = names.Length;
        m_qualifiedNames[i] = new string[names.Length];
          for (int j = 0; j < names.Length; j++) {
            stringBuilder.Length = 0;
            m_qualifiedNames[i][j] = stringBuilder.Append(m_prefixesColon[i]).Append(names[j]).ToString(/**/);
          }
      }
    }

  }

}