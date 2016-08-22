using System;
using System.IO;
using System.Text;

using Org.System.Xml.Sax;
using Org.System.Xml.Sax.Helpers;

using AlignmentType = Nagasena.Proc.Common.AlignmentType;
using GrammarCache = Nagasena.Proc.Grammars.GrammarCache;

namespace Nagasena.Sax {

  public class XMLifier {

    private EXIReader m_exiReader;

    private static Encoding m_utf8Encoding; 

    static XMLifier() {
      m_utf8Encoding = new UTF8Encoding(true, true);
    }

    public XMLifier() {
      m_exiReader = new EXIReader();
    }

    /// <summary>
    /// Convert EXI stream into UTF8-encoded XML stream with BOM.
    /// </summary>
    public void Convert(Stream inputStream, Stream outputStream) {
      Convert(inputStream, outputStream, m_utf8Encoding);
    }

    /// <summary>
    /// Convert EXI stream into XML stream in the specified text encoding.
    /// </summary>
    public void Convert(Stream inputStream, Stream outputStream, Encoding textEncoding) {
      XMLifierContentHandler handler = new XMLifierContentHandler(outputStream, textEncoding);
      m_exiReader.ContentHandler = handler;
      m_exiReader.LexicalHandler = handler;
      m_exiReader.Parse(inputStream);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Methods to configure EXIDecoder
    ///////////////////////////////////////////////////////////////////////////

    /// <summary>
    /// Set the bit alignment style used to compile the EXI input stream. 
    /// </summary>
    public AlignmentType AlignmentType {
      set {
        m_exiReader.AlignmentType = value;
      }
    }

    /// <summary>
    /// Set to true if the EXI input stream was compiled with the Preserve Lexical
    /// Values set to true. The original strings, rather than logical XML
    /// equivalents, are restored in the XML output stream. </summary>
    /// <param name="preserveLexicalValues">set to true if the EXI input stream was compiled with 
    /// Preserve Lexical Values set to true.</param>
    public bool PreserveLexicalValues {
      set {
        m_exiReader.PreserveLexicalValues = value;
      }
    }

    /// <summary>
    /// Set the GrammarCache used in parsing EXI streams. 
    /// </summary>
    public virtual GrammarCache GrammarCache {
      set {
        m_exiReader.GrammarCache = value;
      }
    }

    private sealed class XMLifierContentHandler : DefaultHandler {

      private StreamWriter m_writer;

      private int m_n_prefixBindings;
      private String[] m_prefixBindings;

      private int m_n_prefixes;

      internal XMLifierContentHandler(Stream outputStream, Encoding textEncoding) {
        m_writer = new StreamWriter(outputStream, textEncoding);
        m_n_prefixBindings = 0;
        m_prefixBindings = new String[128];
      }

      public override void StartElement(string uri, string localName, string qName, IAttributes attributes) {
        m_writer.Write("<" + qName);
        if (m_n_prefixes > 0) {
          int start = m_n_prefixBindings - m_n_prefixes;
          for (int i = 0; i < m_n_prefixes; i++) {
            String prefix = m_prefixBindings[(start + i) << 1];
            String nsuri = m_prefixBindings[(start + i) << 1 | 1];
            if (!"".Equals(prefix))
              m_writer.Write(" xmlns:" + prefix + "=\"" + nsuri + "\"");
            else
              m_writer.Write(" xmlns=\"" + nsuri + "\"");
          }
          m_n_prefixes = 0;
        }
        for (int i = 0; i < attributes.Length; i++) {
          m_writer.Write(" " + attributes.GetQName(i) + "=\"" + attributes.GetValue(i) + "\"");
        }
        m_writer.Write(">");
      }

      public override void EndElement(string uri, string localName, string qName) {
        m_writer.Write("</" + qName + ">");
      }

      public override void Characters(char[] ch, int start, int length) {
        m_writer.Write(ch, start, length);
      }

      public override void StartPrefixMapping(string prefix, string uri) {
        m_prefixBindings[m_n_prefixBindings << 1] = prefix;
        m_prefixBindings[m_n_prefixBindings << 1 | 1] = uri;
        m_n_prefixBindings++;
        m_n_prefixes++;
      }

      public override void EndPrefixMapping(string prefix) {
        m_n_prefixBindings--;
      }

      public override void EndDocument() {
        m_writer.Flush();
      }

      public override void Comment(char[] ch, int start, int length) {
        m_writer.Write("<!--");
        m_writer.Write(ch, start, length);
        m_writer.Write("-->");
      }

      public override void ProcessingInstruction(string target, string data) {
        m_writer.Write("<?" + target + " " + data + "?>");
      }

    }
  }
}
