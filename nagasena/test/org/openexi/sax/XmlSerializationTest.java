package org.openexi.sax;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import junit.framework.Assert;

import org.openexi.proc.EXIDecoder;
import org.openexi.proc.common.AlignmentType;
import org.openexi.proc.common.EventDescription;
import org.openexi.proc.common.EventType;
import org.openexi.proc.common.GrammarOptions;
import org.openexi.proc.common.XmlUriConst;
import org.openexi.proc.grammars.GrammarCache;
import org.openexi.proc.io.Scanner;
import org.openexi.schema.EXISchema;
import org.openexi.schema.EmptySchema;
import org.openexi.schema.TestBase;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.exi.ttf.Event;
import org.w3c.exi.ttf.sax.SAXRecorder;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

public class XmlSerializationTest extends TestBase {

  public XmlSerializationTest(String name) {
    super(name);
  }
  
  private static final AlignmentType[] Alignments = new AlignmentType[] { 
    AlignmentType.bitPacked, 
    AlignmentType.byteAligned, 
    AlignmentType.preCompress, 
    AlignmentType.compress 
  };

  ///////////////////////////////////////////////////////////////////////////
  // Test cases
  ///////////////////////////////////////////////////////////////////////////

  /**
   * Use JAXP transformer to serialize SAX events into text XML.
   */
  public void testXMLSerialization_01() throws Exception {

    GrammarCache grammarCache = new GrammarCache(EmptySchema.getEXISchema(), 
        GrammarOptions.addCM(GrammarOptions.DEFAULT_OPTIONS));
    
    final String xmlString = 
      "<foo:A xmlns:foo='urn:foo' xmlns:goo='urn:goo' goo:a='Good Bye!'>Hello <!-- evil --> World!</foo:A>\n"; 

    SAXTransformerFactory saxTransformerFactory = (SAXTransformerFactory)SAXTransformerFactory.newInstance();
    SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
    saxParserFactory.setNamespaceAware(true);

    for (AlignmentType alignment : Alignments) {
      Transmogrifier encoder = new Transmogrifier();
      EXIReader decoder = new EXIReader();
      
      encoder.setAlignmentType(alignment);
      decoder.setAlignmentType(alignment);

      encoder.setGrammarCache(grammarCache);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);
      
      encoder.encode(new InputSource(new StringReader(xmlString)));
      
      byte[] bts = baos.toByteArray();
      
      decoder.setGrammarCache(grammarCache);
      
      TransformerHandler transformerHandler = saxTransformerFactory.newTransformerHandler();
      StringWriter stringWriter = new StringWriter();
      transformerHandler.setResult(new StreamResult(stringWriter));
      
      decoder.setContentHandler(transformerHandler);
      decoder.setLexicalHandler(transformerHandler);
      decoder.parse(new InputSource(new ByteArrayInputStream(bts)));
      
      final String reconstitutedString;
      reconstitutedString = stringWriter.getBuffer().toString();
      
      ArrayList<Event> exiEventList = new ArrayList<Event>();
      SAXRecorder saxRecorder = new SAXRecorder(exiEventList, true);
      
      XMLReader xmlReader = saxParserFactory.newSAXParser().getXMLReader();
      xmlReader.setContentHandler(saxRecorder);
      xmlReader.setProperty("http://xml.org/sax/properties/lexical-handler", saxRecorder);
      xmlReader.parse(new InputSource(new StringReader(reconstitutedString)));

      Assert.assertEquals(14, exiEventList.size());
  
      Event saxEvent;
  
      int n = 0;
      
      saxEvent = exiEventList.get(n++);
      Assert.assertEquals(Event.NAMESPACE, saxEvent.type);
      Assert.assertEquals("urn:foo", saxEvent.namespace);
      Assert.assertEquals("p0", saxEvent.name);

      saxEvent = exiEventList.get(n++);
      Assert.assertEquals(Event.NAMESPACE, saxEvent.type);
      Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, saxEvent.namespace);
      Assert.assertEquals("xsi", saxEvent.name);

      saxEvent = exiEventList.get(n++);
      Assert.assertEquals(Event.NAMESPACE, saxEvent.type);
      Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_URI, saxEvent.namespace);
      Assert.assertEquals("xsd", saxEvent.name);

      saxEvent = exiEventList.get(n++);
      Assert.assertEquals(Event.NAMESPACE, saxEvent.type);
      Assert.assertEquals("urn:goo", saxEvent.namespace);
      Assert.assertEquals("p1", saxEvent.name);

      saxEvent = exiEventList.get(n++);
      Assert.assertEquals(Event.START_ELEMENT, saxEvent.type);
      Assert.assertEquals("urn:foo", saxEvent.namespace);
      Assert.assertEquals("A", saxEvent.localName);
      Assert.assertEquals("p0:A", saxEvent.name);

      saxEvent = exiEventList.get(n++);
      Assert.assertEquals(Event.ATTRIBUTE, saxEvent.type);
      Assert.assertEquals("urn:goo", saxEvent.namespace);
      Assert.assertEquals("a", saxEvent.localName);
      Assert.assertEquals("p1:a", saxEvent.name);
      Assert.assertEquals("Good Bye!", saxEvent.stringValue);

      saxEvent = exiEventList.get(n++);
      Assert.assertEquals(Event.CHARACTERS, saxEvent.type);
      Assert.assertEquals("Hello ", new String(saxEvent.charValue));
      
      saxEvent = exiEventList.get(n++);
      Assert.assertEquals(Event.COMMENT, saxEvent.type);
      Assert.assertEquals(" evil ", new String(saxEvent.charValue));

      saxEvent = exiEventList.get(n++);
      Assert.assertEquals(Event.CHARACTERS, saxEvent.type);
      Assert.assertEquals(" World!", new String(saxEvent.charValue));
      
      saxEvent = exiEventList.get(n++);
      Assert.assertEquals(Event.END_ELEMENT, saxEvent.type);
      Assert.assertEquals("urn:foo", saxEvent.namespace);
      Assert.assertEquals("A", saxEvent.localName);
      Assert.assertEquals("p0:A", saxEvent.name);

      saxEvent = exiEventList.get(n++);
      Assert.assertEquals(Event.END_NAMESPACE, saxEvent.type);
      Assert.assertEquals("p0", saxEvent.name);

      saxEvent = exiEventList.get(n++);
      Assert.assertEquals(Event.END_NAMESPACE, saxEvent.type);
      Assert.assertEquals("xsi", saxEvent.name);

      saxEvent = exiEventList.get(n++);
      Assert.assertEquals(Event.END_NAMESPACE, saxEvent.type);
      Assert.assertEquals("xsd", saxEvent.name);

      saxEvent = exiEventList.get(n++);
      Assert.assertEquals(Event.END_NAMESPACE, saxEvent.type);
      Assert.assertEquals("p1", saxEvent.name);
      
      Assert.assertEquals(14, n);
    }
  }

  /**
   * Use JAXP transformer to serialize SAX events into text XML.
   */
  public void testXMLSerialization_02() throws Exception {

    short options = GrammarOptions.addNS(GrammarOptions.DEFAULT_OPTIONS); 
    GrammarCache grammarCache = new GrammarCache((EXISchema)null, options);
    
    URL url = resolveSystemIdAsURL("/interop/preserve/document/doc-13.xml");
    InputSource inputSource = new InputSource(url.toString());

    SAXTransformerFactory saxTransformerFactory = (SAXTransformerFactory)SAXTransformerFactory.newInstance();
    SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
    saxParserFactory.setNamespaceAware(true);

    for (AlignmentType alignment : Alignments) {
      Transmogrifier encoder = new Transmogrifier();
      EXIReader decoder = new EXIReader();
      
      encoder.setAlignmentType(alignment);
      decoder.setAlignmentType(alignment);

      encoder.setGrammarCache(grammarCache);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);
      
      inputSource.setByteStream(url.openStream());
      encoder.encode(inputSource);
      
      byte[] bts = baos.toByteArray();
      
      decoder.setGrammarCache(grammarCache);
      
      TransformerHandler transformerHandler = saxTransformerFactory.newTransformerHandler();
      StringWriter stringWriter = new StringWriter();
      transformerHandler.setResult(new StreamResult(stringWriter));
      
      decoder.setContentHandler(transformerHandler);
      decoder.parse(new InputSource(new ByteArrayInputStream(bts)));
      
      final String reconstitutedString;
      reconstitutedString = stringWriter.getBuffer().toString();
      
      ArrayList<Event> exiEventList = new ArrayList<Event>();
      SAXRecorder saxRecorder = new SAXRecorder(exiEventList, true);
      
      XMLReader xmlReader = saxParserFactory.newSAXParser().getXMLReader();
      xmlReader.setContentHandler(saxRecorder);
      xmlReader.parse(new InputSource(new StringReader(reconstitutedString)));

      Assert.assertEquals(13, exiEventList.size());
  
      Event saxEvent;
  
      int n = 0;
      
      saxEvent = exiEventList.get(n++);
      Assert.assertEquals(Event.NAMESPACE, saxEvent.type);
      Assert.assertEquals("http://www.w3.org/1999/xhtml", saxEvent.namespace);
      Assert.assertEquals("", saxEvent.name);

      saxEvent = exiEventList.get(n++);
      Assert.assertEquals(Event.START_ELEMENT, saxEvent.type);
      Assert.assertEquals("http://www.w3.org/1999/xhtml", saxEvent.namespace);
      Assert.assertEquals("html", saxEvent.localName);
      Assert.assertEquals("html", saxEvent.name);

      saxEvent = exiEventList.get(n++);
      Assert.assertEquals(Event.START_ELEMENT, saxEvent.type);
      Assert.assertEquals("http://www.w3.org/1999/xhtml", saxEvent.namespace);
      Assert.assertEquals("head", saxEvent.localName);
      Assert.assertEquals("head", saxEvent.name);

      saxEvent = exiEventList.get(n++);
      Assert.assertEquals(Event.START_ELEMENT, saxEvent.type);
      Assert.assertEquals("http://www.w3.org/1999/xhtml", saxEvent.namespace);
      Assert.assertEquals("title", saxEvent.localName);
      Assert.assertEquals("title", saxEvent.name);

      saxEvent = exiEventList.get(n++);
      Assert.assertEquals(Event.CHARACTERS, saxEvent.type);
      Assert.assertEquals("Test", new String(saxEvent.charValue));

//      <?xml version='1.0'?>
//      <!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "xhtml1-transitional.dtd">
//      <html xmlns="http://www.w3.org/1999/xhtml">
//          <head>
//              <title>Test</title>
//          </head>
//          <body>
//              Test
//          </body>
//      </html>

      saxEvent = exiEventList.get(n++);
      Assert.assertEquals(Event.END_ELEMENT, saxEvent.type);
      Assert.assertEquals("http://www.w3.org/1999/xhtml", saxEvent.namespace);
      Assert.assertEquals("title", saxEvent.localName);
      Assert.assertEquals("title", saxEvent.name);

      saxEvent = exiEventList.get(n++);
      Assert.assertEquals(Event.END_ELEMENT, saxEvent.type);
      Assert.assertEquals("http://www.w3.org/1999/xhtml", saxEvent.namespace);
      Assert.assertEquals("head", saxEvent.localName);
      Assert.assertEquals("head", saxEvent.name);

      saxEvent = exiEventList.get(n++);
      Assert.assertEquals(Event.START_ELEMENT, saxEvent.type);
      Assert.assertEquals("http://www.w3.org/1999/xhtml", saxEvent.namespace);
      Assert.assertEquals("body", saxEvent.localName);
      Assert.assertEquals("body", saxEvent.name);

      saxEvent = exiEventList.get(n++);
      Assert.assertEquals(Event.CHARACTERS, saxEvent.type);
      Assert.assertEquals("\n        Test", new String(saxEvent.charValue));

      saxEvent = exiEventList.get(n++);
      Assert.assertEquals(Event.CHARACTERS, saxEvent.type);
      Assert.assertEquals("\n    ", new String(saxEvent.charValue));

      saxEvent = exiEventList.get(n++);
      Assert.assertEquals(Event.END_ELEMENT, saxEvent.type);
      Assert.assertEquals("http://www.w3.org/1999/xhtml", saxEvent.namespace);
      Assert.assertEquals("body", saxEvent.localName);
      Assert.assertEquals("body", saxEvent.name);

      saxEvent = exiEventList.get(n++);
      Assert.assertEquals(Event.END_ELEMENT, saxEvent.type);
      Assert.assertEquals("http://www.w3.org/1999/xhtml", saxEvent.namespace);
      Assert.assertEquals("html", saxEvent.localName);
      Assert.assertEquals("html", saxEvent.name);

      saxEvent = exiEventList.get(n++);
      Assert.assertEquals(Event.END_NAMESPACE, saxEvent.type);
      Assert.assertEquals("", saxEvent.name);

      Assert.assertEquals(13, n);
    }
  }

  /**
   * Use JAXP transformer to convert SAX events into DOM.
   */
  public void testToDOM_01() throws Exception {

    GrammarCache grammarCache = new GrammarCache((EXISchema)null, GrammarOptions.addNS(GrammarOptions.DEFAULT_OPTIONS));
    
    final String xmlString = "<abc:rpc message-id='id' xmlns:abc='a.b.c'><abc:inner/></abc:rpc>\n"; 

    SAXTransformerFactory saxTransformerFactory = (SAXTransformerFactory)SAXTransformerFactory.newInstance();

    for (AlignmentType alignment : Alignments) {
      Transmogrifier encoder = new Transmogrifier();
      encoder.setAlignmentType(alignment);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      encoder.setGrammarCache(grammarCache);
      encoder.setOutputStream(baos);
      encoder.encode(new InputSource(new StringReader(xmlString)));

      byte[] bts = baos.toByteArray();
      
      EXIReader reader = new EXIReader();
      reader.setAlignmentType(alignment);
      reader.setGrammarCache(grammarCache);
      
      TransformerHandler transformerHandler = saxTransformerFactory.newTransformerHandler();
      DOMResult domResult = new DOMResult();
      transformerHandler.setResult(domResult);
      
      reader.setContentHandler(transformerHandler);
      reader.parse(new InputSource(new ByteArrayInputStream(bts)));
      
      Node documentNode = domResult.getNode();
      
      Assert.assertEquals(Node.DOCUMENT_NODE, documentNode.getNodeType());
      
      NodeList roots = documentNode.getChildNodes();
      Assert.assertEquals(1, roots.getLength());
      
      Node rootElem = roots.item(0);
      Assert.assertEquals(Node.ELEMENT_NODE, rootElem.getNodeType());
      Assert.assertEquals("a.b.c", rootElem.getNamespaceURI()); 
      Assert.assertEquals("rpc", rootElem.getLocalName());
      Assert.assertEquals("abc:rpc", rootElem.getNodeName());

      NodeList kids = rootElem.getChildNodes();
      Assert.assertEquals(1, kids.getLength());

      Node childElem = kids.item(0);
      
      Assert.assertEquals(Node.ELEMENT_NODE, childElem.getNodeType());
      Assert.assertEquals("a.b.c", childElem.getNamespaceURI()); 
      Assert.assertEquals("inner", childElem.getLocalName());
      Assert.assertEquals("abc:inner", childElem.getNodeName());

      Assert.assertNull(childElem.getFirstChild());
    }
  }

  /**
   * Use JAXP transformer to convert DOM events into SAX.
   */
  public void testFromDOM_01() throws Exception {

    GrammarCache grammarCache = new GrammarCache((EXISchema)null, GrammarOptions.addNS(GrammarOptions.DEFAULT_OPTIONS));
    
    final String xmlString = "<abc:rpc message-id='id' xmlns:abc='a.b.c'><abc:inner/></abc:rpc>\n";
    
    DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
    documentBuilderFactory.setNamespaceAware(true);
    DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
    Document doc = documentBuilder.parse(new InputSource(new StringReader(xmlString)));

    TransformerFactory transformerFactory = TransformerFactory.newInstance();

    for (AlignmentType alignment : Alignments) {
      
      Transmogrifier encoder = new Transmogrifier();
      encoder.setAlignmentType(alignment);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      encoder.setGrammarCache(grammarCache);
      encoder.setOutputStream(baos);
      
      Transformer transformer = transformerFactory.newTransformer();
      transformer.transform(new DOMSource(doc), new SAXResult(encoder.getSAXTransmogrifier()));
      
      byte[] bts = baos.toByteArray();
      
      EXIDecoder decoder = new EXIDecoder();

      decoder.setAlignmentType(alignment);
      decoder.setGrammarCache(grammarCache);
      decoder.setInputStream(new ByteArrayInputStream(bts));
      
      Scanner scanner = decoder.processHeader();
      
      EventDescription exiEvent;
      EventType eventType;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
      Assert.assertEquals("rpc", exiEvent.getName());
      Assert.assertEquals("a.b.c", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
      
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_NS, exiEvent.getEventKind());
      Assert.assertEquals("abc", exiEvent.getPrefix());
      Assert.assertEquals("a.b.c", exiEvent.getURI());

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_AT, exiEvent.getEventKind());
      Assert.assertEquals("message-id", exiEvent.getName());
      Assert.assertEquals("", exiEvent.getURI());
      Assert.assertEquals("id", exiEvent.getCharacters().makeString());

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
      Assert.assertEquals("inner", exiEvent.getName());
      Assert.assertEquals("a.b.c", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
      
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());

      Assert.assertNull(scanner.nextEvent());
    }
  }

}
