﻿using System;
using System.IO;

using Org.System.Xml.Sax;
using Org.System.Xml.Sax.Helpers;

using AlignmentType = Nagasena.Proc.Common.AlignmentType;
using GrammarCache = Nagasena.Proc.Grammars.GrammarCache;

namespace Nagasena.Sax {

  public class XMLifier {

    private EXIReader m_exiReader;

    public XMLifier() {
      m_exiReader = new EXIReader();
    }

    public void decode(Stream inputStream, Stream outputStream) {
      m_exiReader.ContentHandler = new XMLifierContentHandler(outputStream);
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

      internal XMLifierContentHandler(Stream outputStream) {
        m_writer = new StreamWriter(outputStream, System.Text.Encoding.UTF8);
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


        //const string xmlString =
        //  "<F xsi:type='F' xmlns='urn:foo' xmlns:foo='urn:foo' " +
        //  "   xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'" +
        //  "   foo:aA='abc'>" +
        //  "</F>\n";

        //<F xmlns:="" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:foo="urn:foo" xsi:type="F" foo:aA="abc"></F>

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
    }

  }
}