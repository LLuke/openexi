using System;
using System.Collections.Generic;
using NUnit.Framework;

using Org.System.Xml.Sax;
using Org.System.Xml.Sax.Helpers;

/*
 * EXI Testing Task Force Measurement Suite: http://www.w3.org/XML/EXI/
 *
 * Copyright [2006] World Wide Web Consortium, (Massachusetts Institute of
 * Technology, European Research Consortium for Informatics and Mathematics,
 * Keio University). All Rights Reserved. This work is distributed under the
 * W3C Software License [1] in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.
 *
 * [1] http://www.w3.org/Consortium/Legal/2002/copyright-software-20021231
 */

namespace Org.W3C.Exi.Ttf.Sax {

  /// <summary>
  /// Record an aproximation of the Infoset derived from 
  /// SAX calls to ContentHandler, DTDHandler, LexicalHandler, and DeclHandler.
  /// </summary>
  public class SAXRecorder : DefaultHandler, ILexicalHandler, IDeclHandler {

    /// <summary>
    /// Collects Event objects recorded.
    /// </summary>
    protected internal List<Event> _events;

    /// <summary>
    /// indicate whether interning is enabled
    /// </summary>
    protected internal bool _intern;

    // intern-ing strings.
    private Dictionary<string, char[]> _charArrayTable = new Dictionary<string, char[]>();

    ////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////

    /// <summary>
    /// Construct a new recorder, ready to record to the provided ArrayList
    /// and interning values if internStrings is true
    /// </summary>
    public SAXRecorder(List<Event> events, bool internStrings) {
      _events = events;
      _intern = internStrings;
    }

    ////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////
    // SAX Handler methods

    ////////////////////////////////////////////////////////////////////
    // ContentHandler

    public override void Characters(char[] ch, int start, int length) {
      _events.Add(Event.newCharacters(ToCharArray(ch, start, length)));
    }

    public override void EndElement(string uri, string localName, string qName) {
      _events.Add(Event.newEndElement(intern(uri), intern(localName), intern(qName)));
    }

    public override void ProcessingInstruction(string target, string data) {
      _events.Add(Event.newProcessingInstruction(intern(target), intern(data)));
    }

    public override void StartElement(string uri, string localName, string qName, IAttributes attributes) {
        _events.Add(Event.newStartElement(intern(uri), intern(localName), intern(qName)));
        // Write the attributes
        for (int i = 0; i < attributes.Length; i++) {
          string _uri = intern(attributes.GetUri(i));
          string _localName = intern(attributes.GetLocalName(i));
          string _qName = intern(attributes.GetQName(i));
          string _value = intern(attributes.GetValue(i));
          _events.Add(Event.newAttribute(_uri, _localName, _qName, _value));
          Assert.AreEqual(_value, attributes.GetValue(_uri, _localName));
          Assert.AreEqual(_value, attributes.GetValue(_qName));
        }
    }

    public override void StartPrefixMapping(string prefix, string uri) {
      _events.Add(Event.newNamespace(intern(prefix), intern(uri)));
    }

    public override void EndPrefixMapping(string prefix) {
      _events.Add(Event.newEndNamespace(intern(prefix)));
    }

    public override void SkippedEntity(string name) {
      _events.Add(Event.newUnexpandedEntity(intern(name)));
    }

    ////////////////////////////////////////////////////////////////////
    // DTDHandler

    public override void NotationDecl(string name, string publicId, string systemId) {
      _events.Add(Event.newNotation(intern(name), intern(publicId), intern(systemId)));
    }

    public override void UnparsedEntityDecl(string name, string publicId, string systemId, string notationName) {
      _events.Add(Event.newUnparsedEntity(intern(name), intern(publicId), intern(systemId), intern(notationName)));
    }

    ////////////////////////////////////////////////////////////////////
    // LexicalHandler

    public override void StartDtd(string name, string publicId, string systemId) {
      _events.Add(Event.newDoctype(intern(name), intern(publicId), intern(systemId)));
    }

    public override void EndDtd() {
      _events.Add(Event.newEndDTD());
    }

    public override void StartEntity(string name) {
    }

    public override void EndEntity(string name) {
    }

    public override void StartCData() {
    }

    public override void EndCData() {
    }

    public override void Comment(char[] ch, int start, int length) {
      _events.Add(Event.newComment(ToCharArray(ch, start, length)));
    }

    ////////////////////////////////////////////////////////////////////
    // DeclHandler

    public override void ElementDecl(string name, string model) {
    }

    public override void AttributeDecl(string eName, string aName, string type, string mode, string value) {
    }

    public override void InternalEntityDecl(string name, string value) {
    }

    // these are needed in case there was a skipped entity
    public override void ExternalEntityDecl(string name, string publicId, string systemId) {
      _events.Add(Event.newExternalEntity(intern(name), intern(publicId), intern(systemId)));
    }

    ////////////////////////////////////////////////////////////////////
    // internal implementation

    /// <summary>
    /// intern string if _intern==true
    /// </summary>
    protected internal virtual string intern(string s) {
      if (_intern && s != null) {
        return String.Intern(s);
      }
      return s;
    }

    /// <summary>
    /// If interning is enabled, intern char data, 
    /// otherwise just allocate a new char[]
    /// </summary>
    /// <param name="data">
    ///      source char data </param>
    /// <param name="start">
    ///      start offset within data </param>
    /// <param name="length">
    ///      count of characters
    /// @return
    ///      char[] of the subsequence of data specified.  
    ///      Note that returned array should be considered 
    ///      read-only as it may be shared. </param>
    protected internal virtual char[] ToCharArray(char[] data, int start, int length) {
      char[] result;
      if (!_intern) {
        result = new char[length];
        Array.Copy(data, start, result, 0, length);
      }
      else {
        string value = new string(data, start, length);
        if (!_charArrayTable.TryGetValue(value, out result)) {
          result = value.ToCharArray();
          _charArrayTable[value] = result;
        }
      }
      return result;
    }

  }

}