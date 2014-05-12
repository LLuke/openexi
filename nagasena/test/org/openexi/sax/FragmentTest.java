package org.openexi.sax;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;

import junit.framework.Assert;

import org.openexi.proc.EXIDecoder;
import org.openexi.proc.HeaderOptionsOutputType;
import org.openexi.proc.common.AlignmentType;
import org.openexi.proc.common.EventDescription;
import org.openexi.proc.common.EXIOptions;
import org.openexi.proc.common.EventCode;
import org.openexi.proc.common.EventType;
import org.openexi.proc.common.EventTypeList;
import org.openexi.proc.common.GrammarOptions;
import org.openexi.proc.common.QName;
import org.openexi.proc.common.StringTable;
import org.openexi.proc.common.XmlUriConst;
import org.openexi.proc.events.EXIEventSchemaNil;
import org.openexi.proc.events.EXIEventSchemaType;
import org.openexi.proc.grammars.Apparatus;
import org.openexi.proc.grammars.BuiltinGrammar;
import org.openexi.proc.grammars.EventTypeSchema;
import org.openexi.proc.grammars.Grammar;
import org.openexi.proc.grammars.GrammarCache;
import org.openexi.proc.grammars.GrammarState;
import org.openexi.proc.grammars.SchemaInformedGrammar;
import org.openexi.proc.io.Scanner;
import org.openexi.proc.io.Scribble;
import org.openexi.proc.io.Scriber;
import org.openexi.proc.io.ScriberFactory;
import org.openexi.proc.io.ValueScriber;
import org.openexi.schema.EXISchema;
import org.openexi.schema.EXISchemaConst;
import org.openexi.schema.EXISchemaUtil;
import org.openexi.schema.TestBase;
import org.openexi.scomp.EXISchemaFactoryErrorMonitor;
import org.openexi.scomp.EXISchemaFactoryTestUtil;
import org.w3c.exi.ttf.Event;
import org.w3c.exi.ttf.sax.SAXRecorder;
import org.xml.sax.InputSource;

public class FragmentTest extends TestBase {

  public FragmentTest(String name) {
    super(name);
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    m_compilerErrors = new EXISchemaFactoryErrorMonitor();
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
    m_compilerErrors.clear();
  }

  private EXISchemaFactoryErrorMonitor m_compilerErrors;
  
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
   * The element "A" leads to use a specific element grammar.
   */
  public void testSchemaInformedFragment_01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/fragment_01.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);
    
    String xmlString;
    byte[] bts;
    
    xmlString = "<A xmlns='urn:goo'>true</A>";
    
    for (AlignmentType alignment : Alignments) {
      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();
      Scanner scanner;

      encoder.setAlignmentType(alignment);
      encoder.setFragment(true);
      encoder.setOutputOptions(HeaderOptionsOutputType.lessSchemaId);
      
      encoder.setGrammarCache(grammarCache);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);

      encoder.encode(new InputSource(new StringReader(xmlString)));
      
      bts = baos.toByteArray();
      
      decoder.setGrammarCache(grammarCache);
      decoder.setInputStream(new ByteArrayInputStream(bts));
      scanner = decoder.processHeader();
      
      EventDescription exiEvent;
      EventType eventType;
      EventTypeList eventTypeList;
      GrammarState grammarState;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_FRAGMENT, grammarState.targetGrammar.grammarType);
        Assert.assertEquals(Grammar.DOCUMENT_STATE_COMPLETED, grammarState.phase);
      }
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(9,  eventTypeList.getLength());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A__", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("B", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventType.ITEM_ED, eventType.itemType);
     /**
      * This assertion is not relevant in Nagasena
      * if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
      *   grammarState = scanner.getGrammarState();
      *   Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_ELEMENT, grammarState.targetGrammar.grammarType);
      *   Assert.assertEquals(SchemaInformedGrammar.ELEMENT_STATE_UNBOUND, grammarState.phase);
      * }
      */

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
      Assert.assertEquals("true", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        int tp = scanner.currentState.contentDatatype;
        Assert.assertEquals(EXISchemaConst.BOOLEAN_TYPE, corpus.getSerialOfType(tp));
      }
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(1, eventTypeList.getLength());
     /**
      * This assertion is not relevant in Nagasena
      * if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
      *   grammarState = scanner.getGrammarState();
      *   Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_ELEMENT_CONTENT, grammarState.targetGrammar.grammarType);
      *   Assert.assertEquals(SchemaInformedGrammar.ELEMENT_STATE_CONTENT_COMPLETE, grammarState.phase);
      * }
      */
      
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(1, eventTypeList.getLength());
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_FRAGMENT, grammarState.targetGrammar.grammarType);
        Assert.assertEquals(Grammar.DOCUMENT_STATE_COMPLETED, grammarState.phase);
      }
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(8, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(9,  eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A__", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("B", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_FRAGMENT, grammarState.targetGrammar.grammarType);
        Assert.assertEquals(SchemaInformedGrammar.DOCUMENT_STATE_END, grammarState.phase);
      }
      
      Assert.assertNull(scanner.nextEvent());
    }
  }

  /**
   * Multiple root elements.
   */
  public void testSchemaInformedFragment_02() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/fragment_01.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

    final Scribble scribble = new Scribble();

    final int booleanType = corpus.getBuiltinTypeOfSchema(EXISchemaConst.BOOLEAN_TYPE);

    for (AlignmentType alignment : Alignments) {
      final Scriber scriber = ScriberFactory.createScriber(alignment);
      scriber.setSchema(grammarCache.getEXISchema(), (QName[])null, 0);
      scriber.setPreserveNS(GrammarOptions.hasNS(grammarCache.grammarOptions));
      StringTable stringTable = Scriber.createStringTable(grammarCache);
      scriber.setStringTable(stringTable);
      scriber.setValueMaxLength(EXIOptions.VALUE_MAX_LENGTH_UNBOUNDED);

      final ValueScriber booleanValueScriber = scriber.getValueScriber(booleanType);

      final ByteArrayOutputStream baos = new ByteArrayOutputStream();
      scriber.setOutputStream(baos);

      Scriber.writeHeaderPreamble(baos, false, false);
      
      final Grammar documentGrammar = grammarCache.retrieveRootGrammar(true, scriber.eventTypesWorkSpace); 
      documentGrammar.init(scriber.currentState);

      EventTypeList eventTypes;
      EventType eventType;
      QName qname = new QName();

      eventTypes = scriber.getNextEventTypes();
      
      eventType = eventTypes.item(0);
      Assert.assertEquals(EventType.ITEM_SD, eventType.itemType);
      Assert.assertEquals(1, eventTypes.getLength());
      scriber.writeEventType(eventType);
      scriber.startDocument();
      eventTypes = scriber.getNextEventTypes();
      Assert.assertEquals(9, eventTypes.getLength());

      eventType = eventTypes.item(0);
      Assert.assertEquals("A", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      scriber.writeEventType(eventType);
      scriber.writeQName(qname.setValue("urn:goo", "A", null), eventType);
      scriber.startElement(eventType);
      eventTypes = scriber.getNextEventTypes();
      Assert.assertEquals(1, eventTypes.getLength());

      eventType = eventTypes.item(0);
      Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
      scriber.writeEventType(eventType);
      booleanValueScriber.process("true", booleanType, corpus, scribble, scriber);
      booleanValueScriber.scribe("true", scribble, qname.localNameId, qname.uriId, booleanType, scriber);
      scriber.characters(eventType);
      eventTypes = scriber.getNextEventTypes();
      Assert.assertEquals(1, eventTypes.getLength());
      
      eventType = eventTypes.item(0);
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      scriber.writeEventType(eventType);
      scriber.endElement();
      eventTypes = scriber.getNextEventTypes();
      Assert.assertEquals(9, eventTypes.getLength());

      eventType = eventTypes.item(0);
      Assert.assertEquals("A", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      scriber.writeEventType(eventType);
      scriber.writeQName(qname.setValue("urn:goo", "A", null), eventType);
      scriber.startElement(eventType);
      eventTypes = scriber.getNextEventTypes();
      Assert.assertEquals(1, eventTypes.getLength());

      eventType = eventTypes.item(0);
      Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
      scriber.writeEventType(eventType);
      booleanValueScriber.process("false", booleanType, corpus, scribble, scriber);
      booleanValueScriber.scribe("false", scribble, qname.localNameId, qname.uriId, booleanType, scriber);
      scriber.characters(eventType);
      eventTypes = scriber.getNextEventTypes();
      Assert.assertEquals(1, eventTypes.getLength());
      
      eventType = eventTypes.item(0);
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      scriber.writeEventType(eventType);
      scriber.endElement();
      eventTypes = scriber.getNextEventTypes();
      Assert.assertEquals(9, eventTypes.getLength());
      
      eventType = eventTypes.item(8);
      Assert.assertEquals(EventType.ITEM_ED, eventType.itemType);
      scriber.writeEventType(eventType);
      scriber.endDocument();
      eventTypes = scriber.getNextEventTypes();
      Assert.assertEquals(0, eventTypes.getLength());
      
      scriber.finish();
      
      
      EXIDecoder decoder = new EXIDecoder();
      Scanner scanner;

      decoder.setAlignmentType(alignment);
      decoder.setFragment(true);
      
      decoder.setGrammarCache(grammarCache);
      decoder.setInputStream(new ByteArrayInputStream(baos.toByteArray()));
      scanner = decoder.processHeader();
      
      EventDescription exiEvent;
      EventTypeList eventTypeList;
      GrammarState grammarState;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_FRAGMENT, grammarState.targetGrammar.grammarType);
        Assert.assertEquals(Grammar.DOCUMENT_STATE_COMPLETED, grammarState.phase);
      }
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(9,  eventTypeList.getLength());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A__", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("B", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventType.ITEM_ED, eventType.itemType);
     /**
      * This assertion is not relevant in Nagasena
      * if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
      *   grammarState = scanner.getGrammarState();
      *   Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_ELEMENT, grammarState.targetGrammar.grammarType);
      *   Assert.assertEquals(SchemaInformedGrammar.ELEMENT_STATE_UNBOUND, grammarState.phase);
      * }
      */

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
      Assert.assertEquals("true", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        int tp = scanner.currentState.contentDatatype; 
        Assert.assertEquals(EXISchemaConst.BOOLEAN_TYPE, corpus.getSerialOfType(tp));
      }
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(1, eventTypeList.getLength());
     /**
      * This assertion is not relevant in Nagasena
      * if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
      *   grammarState = scanner.getGrammarState();
      *   Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_ELEMENT_CONTENT, grammarState.targetGrammar.grammarType);
      *   Assert.assertEquals(SchemaInformedGrammar.ELEMENT_STATE_CONTENT_COMPLETE, grammarState.phase);
      * }
      */
      
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(1, eventTypeList.getLength());
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_FRAGMENT, grammarState.targetGrammar.grammarType);
        Assert.assertEquals(Grammar.DOCUMENT_STATE_COMPLETED, grammarState.phase);
      }
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(9,  eventTypeList.getLength());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A__", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("B", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventType.ITEM_ED, eventType.itemType);
     /**
      * This assertion is not relevant in Nagasena
      * if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
      *   grammarState = scanner.getGrammarState();
      *   Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_ELEMENT, grammarState.targetGrammar.grammarType);
      *   Assert.assertEquals(SchemaInformedGrammar.ELEMENT_STATE_UNBOUND, grammarState.phase);
      * }
      */

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
      Assert.assertEquals("false", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        int tp = scanner.currentState.contentDatatype; 
        Assert.assertEquals(EXISchemaConst.BOOLEAN_TYPE, corpus.getSerialOfType(tp));
      }
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(1, eventTypeList.getLength());
     /**
      * This assertion is not relevant in Nagasena
      * if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
      *   grammarState = scanner.getGrammarState();
      *   Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_ELEMENT_CONTENT, grammarState.targetGrammar.grammarType);
      *   Assert.assertEquals(SchemaInformedGrammar.ELEMENT_STATE_CONTENT_COMPLETE, grammarState.phase);
      * }
      */
      
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(1, eventTypeList.getLength());
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_FRAGMENT, grammarState.targetGrammar.grammarType);
        Assert.assertEquals(Grammar.DOCUMENT_STATE_COMPLETED, grammarState.phase);
      }
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(8, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(9,  eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A__", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("B", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_FRAGMENT, grammarState.targetGrammar.grammarType);
        Assert.assertEquals(SchemaInformedGrammar.DOCUMENT_STATE_END, grammarState.phase);
      }
      
      Assert.assertNull(scanner.nextEvent());
    }
  }

  /**
   * Encode multiple root elements, then parse with EXIReader.
   */
  public void testSchemaInformedFragment_03() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/fragment_01.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

    final Scribble scribble = new Scribble();

    final int booleanType = corpus.getBuiltinTypeOfSchema(EXISchemaConst.BOOLEAN_TYPE);

    for (AlignmentType alignment : Alignments) {
      final Scriber scriber = ScriberFactory.createScriber(alignment);
      scriber.setSchema(grammarCache.getEXISchema(), (QName[])null, 0);
      scriber.setPreserveNS(GrammarOptions.hasNS(grammarCache.grammarOptions));
      StringTable stringTable = Scriber.createStringTable(grammarCache); 
      scriber.setStringTable(stringTable);
      scriber.setValueMaxLength(EXIOptions.VALUE_MAX_LENGTH_UNBOUNDED);

      final ValueScriber booleanValueScriber = scriber.getValueScriber(booleanType);

      final ByteArrayOutputStream baos = new ByteArrayOutputStream();
      scriber.setOutputStream(baos);

      Scriber.writeHeaderPreamble(baos, false, false);
      
      final Grammar documentGrammar = grammarCache.retrieveRootGrammar(true, scriber.eventTypesWorkSpace); 
      documentGrammar.init(scriber.currentState);

      EventTypeList eventTypes;
      EventType eventType;
      QName qname = new QName();

      eventTypes = scriber.getNextEventTypes();
      
      eventType = eventTypes.item(0);
      Assert.assertEquals(EventType.ITEM_SD, eventType.itemType);
      Assert.assertEquals(1, eventTypes.getLength());
      scriber.writeEventType(eventType);
      scriber.startDocument();
      eventTypes = scriber.getNextEventTypes();
      Assert.assertEquals(9, eventTypes.getLength());

      eventType = eventTypes.item(0);
      Assert.assertEquals("A", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      scriber.writeEventType(eventType);
      scriber.writeQName(qname.setValue("urn:goo", "A", null), eventType);
      scriber.startElement(eventType);
      eventTypes = scriber.getNextEventTypes();
      Assert.assertEquals(1, eventTypes.getLength());

      eventType = eventTypes.item(0);
      Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
      scriber.writeEventType(eventType);
      booleanValueScriber.process("true", booleanType, corpus, scribble, scriber);
      booleanValueScriber.scribe("true", scribble, qname.localNameId, qname.uriId, booleanType, scriber);
      scriber.characters(eventType);
      eventTypes = scriber.getNextEventTypes();
      Assert.assertEquals(1, eventTypes.getLength());
      
      eventType = eventTypes.item(0);
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      scriber.writeEventType(eventType);
      scriber.endElement();
      eventTypes = scriber.getNextEventTypes();
      Assert.assertEquals(9, eventTypes.getLength());

      eventType = eventTypes.item(0);
      Assert.assertEquals("A", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      scriber.writeEventType(eventType);
      scriber.writeQName(qname.setValue("urn:goo", "A", null), eventType);
      scriber.startElement(eventType);
      eventTypes = scriber.getNextEventTypes();
      Assert.assertEquals(1, eventTypes.getLength());

      eventType = eventTypes.item(0);
      Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
      scriber.writeEventType(eventType);
      booleanValueScriber.process("false", booleanType, corpus, scribble, scriber);
      booleanValueScriber.scribe("false", scribble, qname.localNameId, qname.uriId, booleanType, scriber);
      scriber.characters(eventType);
      eventTypes = scriber.getNextEventTypes();
      Assert.assertEquals(1, eventTypes.getLength());
      
      eventType = eventTypes.item(0);
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      scriber.writeEventType(eventType);
      scriber.endElement();
      eventTypes = scriber.getNextEventTypes();
      Assert.assertEquals(9, eventTypes.getLength());
      
      eventType = eventTypes.item(8);
      Assert.assertEquals(EventType.ITEM_ED, eventType.itemType);
      scriber.writeEventType(eventType);
      scriber.endDocument();
      eventTypes = scriber.getNextEventTypes();
      Assert.assertEquals(0, eventTypes.getLength());
      
      scriber.finish();
      
      
      EXIReader decoder = new EXIReader();

      decoder.setAlignmentType(alignment);
      decoder.setFragment(true);
      
      decoder.setGrammarCache(grammarCache);

      ArrayList<Event> exiEventList = new ArrayList<Event>();
      SAXRecorder saxRecorder = new SAXRecorder(exiEventList, true);
      decoder.setContentHandler(saxRecorder);
      decoder.setLexicalHandler(saxRecorder);

      decoder.parse(new InputSource(new ByteArrayInputStream(baos.toByteArray())));

      Event saxEvent;
      int n = 0;
  
      saxEvent = exiEventList.get(n++);
      Assert.assertEquals(Event.NAMESPACE, saxEvent.type);
      Assert.assertEquals(XmlUriConst.W3C_XML_1998_URI, saxEvent.namespace);
      Assert.assertEquals("xml", saxEvent.name);

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
      Assert.assertEquals("urn:foo", saxEvent.namespace);
      Assert.assertEquals("s0", saxEvent.name);

      saxEvent = exiEventList.get(n++);
      Assert.assertEquals(Event.NAMESPACE, saxEvent.type);
      Assert.assertEquals("urn:goo", saxEvent.namespace);
      Assert.assertEquals("s1", saxEvent.name);
      
      saxEvent = exiEventList.get(n++);
      Assert.assertEquals(Event.START_ELEMENT, saxEvent.type);
      Assert.assertEquals("urn:goo", saxEvent.namespace);
      Assert.assertEquals("A", saxEvent.localName);
      Assert.assertEquals("s1:A", saxEvent.name);

      saxEvent = exiEventList.get(n++);
      Assert.assertEquals(Event.CHARACTERS, saxEvent.type);
      Assert.assertEquals("true", new String(saxEvent.charValue));

      saxEvent = exiEventList.get(n++);
      Assert.assertEquals(Event.END_ELEMENT, saxEvent.type);
      Assert.assertEquals("urn:goo", saxEvent.namespace);
      Assert.assertEquals("A", saxEvent.localName);
      Assert.assertEquals("s1:A", saxEvent.name);

      saxEvent = exiEventList.get(n++);
      Assert.assertEquals(Event.END_NAMESPACE, saxEvent.type);
      Assert.assertEquals("xml", saxEvent.name);

      saxEvent = exiEventList.get(n++);
      Assert.assertEquals(Event.END_NAMESPACE, saxEvent.type);
      Assert.assertEquals("xsi", saxEvent.name);

      saxEvent = exiEventList.get(n++);
      Assert.assertEquals(Event.END_NAMESPACE, saxEvent.type);
      Assert.assertEquals("xsd", saxEvent.name);

      saxEvent = exiEventList.get(n++);
      Assert.assertEquals(Event.END_NAMESPACE, saxEvent.type);
      Assert.assertEquals("s0", saxEvent.name);

      saxEvent = exiEventList.get(n++);
      Assert.assertEquals(Event.END_NAMESPACE, saxEvent.type);
      Assert.assertEquals("s1", saxEvent.name);
      
      saxEvent = exiEventList.get(n++);
      Assert.assertEquals(Event.NAMESPACE, saxEvent.type);
      Assert.assertEquals(XmlUriConst.W3C_XML_1998_URI, saxEvent.namespace);
      Assert.assertEquals("xml", saxEvent.name);

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
      Assert.assertEquals("urn:foo", saxEvent.namespace);
      Assert.assertEquals("s0", saxEvent.name);

      saxEvent = exiEventList.get(n++);
      Assert.assertEquals(Event.NAMESPACE, saxEvent.type);
      Assert.assertEquals("urn:goo", saxEvent.namespace);
      Assert.assertEquals("s1", saxEvent.name);
      
      saxEvent = exiEventList.get(n++);
      Assert.assertEquals(Event.START_ELEMENT, saxEvent.type);
      Assert.assertEquals("urn:goo", saxEvent.namespace);
      Assert.assertEquals("A", saxEvent.localName);
      Assert.assertEquals("s1:A", saxEvent.name);

      saxEvent = exiEventList.get(n++);
      Assert.assertEquals(Event.CHARACTERS, saxEvent.type);
      Assert.assertEquals("false", new String(saxEvent.charValue));

      saxEvent = exiEventList.get(n++);
      Assert.assertEquals(Event.END_ELEMENT, saxEvent.type);
      Assert.assertEquals("urn:goo", saxEvent.namespace);
      Assert.assertEquals("A", saxEvent.localName);
      Assert.assertEquals("s1:A", saxEvent.name);

      saxEvent = exiEventList.get(n++);
      Assert.assertEquals(Event.END_NAMESPACE, saxEvent.type);
      Assert.assertEquals("xml", saxEvent.name);

      saxEvent = exiEventList.get(n++);
      Assert.assertEquals(Event.END_NAMESPACE, saxEvent.type);
      Assert.assertEquals("xsi", saxEvent.name);

      saxEvent = exiEventList.get(n++);
      Assert.assertEquals(Event.END_NAMESPACE, saxEvent.type);
      Assert.assertEquals("xsd", saxEvent.name);

      saxEvent = exiEventList.get(n++);
      Assert.assertEquals(Event.END_NAMESPACE, saxEvent.type);
      Assert.assertEquals("s0", saxEvent.name);

      saxEvent = exiEventList.get(n++);
      Assert.assertEquals(Event.END_NAMESPACE, saxEvent.type);
      Assert.assertEquals("s1", saxEvent.name);
      
      Assert.assertEquals(exiEventList.size(), n);
    }
  }

  /**
   * Test BuiltinFragmentGrammar with a single root element.
   */
  public void testBuiltinFragment_01() throws Exception {

    GrammarCache grammarCache = new GrammarCache((EXISchema)null, GrammarOptions.STRICT_OPTIONS);
    
    String xmlString;
    byte[] bts;
    
    xmlString = "<A xmlns='urn:goo'>true</A>";
    
    for (AlignmentType alignment : Alignments) {
      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();
      Scanner scanner;

      encoder.setAlignmentType(alignment);
      encoder.setFragment(true);
      
      decoder.setAlignmentType(alignment);
      decoder.setFragment(true);
      
      encoder.setGrammarCache(grammarCache);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);

      encoder.encode(new InputSource(new StringReader(xmlString)));
      
      bts = baos.toByteArray();
      
      decoder.setGrammarCache(grammarCache);
      decoder.setInputStream(new ByteArrayInputStream(bts));
      scanner = decoder.processHeader();
      
      EventDescription exiEvent;
      EventType eventType;
      EventTypeList eventTypeList;
      GrammarState grammarState;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(1, eventTypeList.getLength());
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.BUILTIN_GRAMMAR_FRAGMENT, grammarState.targetGrammar.grammarType);
        Assert.assertEquals(Grammar.DOCUMENT_STATE_COMPLETED, grammarState.phase);
      }
      
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
      Assert.assertEquals(1, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(3,  eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventType.ITEM_ED, eventType.itemType);
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.BUILTIN_GRAMMAR_ELEMENT, grammarState.targetGrammar.grammarType);
        Assert.assertEquals(BuiltinGrammar.ELEMENT_STATE_IN_TAG, grammarState.phase);
      }

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
      Assert.assertEquals("true", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      Assert.assertEquals(4, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(5, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.BUILTIN_GRAMMAR_ELEMENT, grammarState.targetGrammar.grammarType);
        Assert.assertEquals(BuiltinGrammar.ELEMENT_STATE_IN_CONTENT, grammarState.phase);
      }
      
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(3, eventTypeList.getLength());
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.BUILTIN_GRAMMAR_FRAGMENT, grammarState.targetGrammar.grammarType);
        Assert.assertEquals(Grammar.DOCUMENT_STATE_COMPLETED, grammarState.phase);
      }
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(2, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(3,  eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.BUILTIN_GRAMMAR_FRAGMENT, grammarState.targetGrammar.grammarType);
        Assert.assertEquals(SchemaInformedGrammar.DOCUMENT_STATE_END, grammarState.phase);
      }
      
      Assert.assertNull(scanner.nextEvent());
    }
  }
  
  /**
   * Multiple root elements.
   */
  public void testBuiltinFragment_02() throws Exception {
    GrammarCache grammarCache = new GrammarCache((EXISchema)null, GrammarOptions.STRICT_OPTIONS);
    
    final Scribble scribble = new Scribble();
    
    final QName qname = new QName();

    for (AlignmentType alignment : Alignments) {
      final Scriber scriber = ScriberFactory.createScriber(alignment);
      scriber.setSchema(grammarCache.getEXISchema(), (QName[])null, 0);
      scriber.setPreserveNS(GrammarOptions.hasNS(grammarCache.grammarOptions));
      StringTable stringTable = Scriber.createStringTable(grammarCache);
      scriber.setStringTable(stringTable);
      scriber.setValueMaxLength(EXIOptions.VALUE_MAX_LENGTH_UNBOUNDED);

      final ValueScriber stringValueScriber = scriber.getValueScriberByID(Apparatus.CODEC_STRING);

      final ByteArrayOutputStream baos = new ByteArrayOutputStream();
      scriber.setOutputStream(baos);

      Scriber.writeHeaderPreamble(baos, false, false);
      
      final Grammar documentGrammar;
      documentGrammar = grammarCache.retrieveRootGrammar(true, scriber.eventTypesWorkSpace); 
      documentGrammar.init(scriber.currentState);

      EventTypeList eventTypes;
      EventType eventType;

      eventTypes = scriber.getNextEventTypes();
      
      eventType = eventTypes.item(0);
      Assert.assertEquals(EventType.ITEM_SD, eventType.itemType);
      Assert.assertEquals(1, eventTypes.getLength());
      scriber.writeEventType(eventType);
      scriber.startDocument();
      eventTypes = scriber.getNextEventTypes();
      Assert.assertEquals(2, eventTypes.getLength()); // SE(*), ED

      eventType = eventTypes.item(0);
      Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
      scriber.writeEventType(eventType);
      qname.setValue("urn:goo", "A", null);
      scriber.writeQName(qname, eventType);
      final int gooId = stringTable.getCompactIdOfURI("urn:goo");
      scriber.startWildcardElement(eventType.getIndex(), gooId, qname.localNameId);
      eventTypes = scriber.getNextEventTypes();
      Assert.assertEquals(4, eventTypes.getLength()); // EE, AT(*), SE(*), CH

      eventType = eventTypes.item(3);
      Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      scriber.writeEventType(eventType);
      stringValueScriber.scribe("true", scribble, qname.localNameId, qname.uriId, EXISchema.NIL_NODE, scriber);
      scriber.undeclaredCharacters(eventType.getIndex());
      eventTypes = scriber.getNextEventTypes();
      Assert.assertEquals(3, eventTypes.getLength()); // EE, SE(*), CH
      
      eventType = eventTypes.item(0);
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      scriber.writeEventType(eventType);
      scriber.endElement();
      eventTypes = scriber.getNextEventTypes();
      Assert.assertEquals(3, eventTypes.getLength()); // SE(goo:A), SE(*), ED

      eventType = eventTypes.item(0);
      Assert.assertEquals("A", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      scriber.writeEventType(eventType);
      scriber.writeQName(qname.setValue("urn:goo", "A", null), eventType);
      scriber.startElement(eventType);
      eventTypes = scriber.getNextEventTypes();
      Assert.assertEquals(5, eventTypes.getLength());

      eventType = eventTypes.item(0);
      Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      scriber.writeEventType(eventType);
      stringValueScriber.scribe("false", scribble, qname.localNameId, qname.uriId, EXISchema.NIL_NODE, scriber);
      scriber.undeclaredCharacters(eventType.getIndex());
      eventTypes = scriber.getNextEventTypes();
      Assert.assertEquals(3, eventTypes.getLength()); // EE, SE(*), CH
      
      eventType = eventTypes.item(0);
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      scriber.writeEventType(eventType);
      scriber.endElement();
      eventTypes = scriber.getNextEventTypes();
      Assert.assertEquals(3, eventTypes.getLength()); // SE(goo:A), SE(*), ED
      
      eventType = eventTypes.item(2);
      Assert.assertEquals(EventType.ITEM_ED, eventType.itemType);
      scriber.writeEventType(eventType);
      scriber.endDocument();
      eventTypes = scriber.getNextEventTypes();
      Assert.assertEquals(0, eventTypes.getLength());
      
      scriber.finish();
      
      
      EXIDecoder decoder = new EXIDecoder();
      Scanner scanner;

      decoder.setAlignmentType(alignment);
      decoder.setFragment(true);
      
      decoder.setGrammarCache(grammarCache);
      decoder.setInputStream(new ByteArrayInputStream(baos.toByteArray()));
      scanner = decoder.processHeader();
      
      EventDescription exiEvent;
      EventTypeList eventTypeList;
      GrammarState grammarState;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.BUILTIN_GRAMMAR_FRAGMENT, grammarState.targetGrammar.grammarType);
        Assert.assertEquals(Grammar.DOCUMENT_STATE_COMPLETED, grammarState.phase);
      }
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
      Assert.assertEquals("A", exiEvent.getName());
      Assert.assertEquals("urn:goo", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
      Assert.assertEquals(1, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(3,  eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventType.ITEM_ED, eventType.itemType);
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.BUILTIN_GRAMMAR_ELEMENT, grammarState.targetGrammar.grammarType);
        Assert.assertEquals(BuiltinGrammar.ELEMENT_STATE_IN_TAG, grammarState.phase); 
      }

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
      Assert.assertEquals("true", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      Assert.assertEquals(4, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(5, eventTypeList.getLength()); // CH, EE, AT(*), SE(*), CH
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.BUILTIN_GRAMMAR_ELEMENT, grammarState.targetGrammar.grammarType);
        Assert.assertEquals(BuiltinGrammar.ELEMENT_STATE_IN_CONTENT, grammarState.phase);
      }
      
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(3, eventTypeList.getLength()); // EE, SE(*), CH
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.BUILTIN_GRAMMAR_FRAGMENT, grammarState.targetGrammar.grammarType);
        Assert.assertEquals(Grammar.DOCUMENT_STATE_COMPLETED, grammarState.phase);
      }
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
      Assert.assertEquals("A", exiEvent.getName());
      Assert.assertEquals("urn:goo", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(3,  eventTypeList.getLength()); // SE(goo:A), SE(*), ED
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventType.ITEM_ED, eventType.itemType);
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.BUILTIN_GRAMMAR_ELEMENT, grammarState.targetGrammar.grammarType);
        Assert.assertEquals(BuiltinGrammar.ELEMENT_STATE_IN_TAG, grammarState.phase);
      }

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
      Assert.assertEquals("false", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(5, eventTypeList.getLength()); // CH, EE, AT(*), SE(*), CH
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.BUILTIN_GRAMMAR_ELEMENT, grammarState.targetGrammar.grammarType);
        Assert.assertEquals(BuiltinGrammar.ELEMENT_STATE_IN_CONTENT, grammarState.phase);
      }
      
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(3, eventTypeList.getLength()); // EE, SE(*), CH
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.BUILTIN_GRAMMAR_FRAGMENT, grammarState.targetGrammar.grammarType);
        Assert.assertEquals(Grammar.DOCUMENT_STATE_COMPLETED, grammarState.phase);
      }
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(2, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(3,  eventTypeList.getLength()); // SE(goo:A), SE(*), EE
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.BUILTIN_GRAMMAR_FRAGMENT, grammarState.targetGrammar.grammarType);
        Assert.assertEquals(Grammar.DOCUMENT_STATE_END, grammarState.phase);
      }
      
      Assert.assertNull(scanner.nextEvent());
    }
  }
  
  /**
   * Test Element Fragment Grammar in strict schema mode.
   * Invoke chars() at ELEMENT_FRAGMENT_STATE_TAG to transition to ELEMENT_FRAGMENT_STATE_CONTENT.
   */
  public void testElementFragmentStrict_01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/fragment_01.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);
    
    String xmlString;
    byte[] bts;
    
    xmlString = "<Z xmlns='urn:foo'>xyz</Z>";
    
    for (AlignmentType alignment : Alignments) {
      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();
      Scanner scanner;

      encoder.setAlignmentType(alignment);
      encoder.setFragment(true);
      decoder.setAlignmentType(alignment);
      decoder.setFragment(true);

      encoder.setGrammarCache(grammarCache);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);

      encoder.encode(new InputSource(new StringReader(xmlString)));
      
      bts = baos.toByteArray();
      
      decoder.setGrammarCache(grammarCache);
      decoder.setInputStream(new ByteArrayInputStream(bts));
      scanner = decoder.processHeader();
      
      EventDescription exiEvent;
  
      EventType eventType;
      EventTypeList eventTypeList;
      GrammarState grammarState;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_FRAGMENT, grammarState.targetGrammar.grammarType);
        Assert.assertEquals(Grammar.DOCUMENT_STATE_COMPLETED, grammarState.phase);
      }
      
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      Assert.assertEquals(5, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(9,  eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A__", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("B", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventType.ITEM_ED, eventType.itemType);
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_ELEMENT_FRAGMENT, grammarState.targetGrammar.grammarType);
        Assert.assertEquals(SchemaInformedGrammar.ELEMENT_FRAGMENT_STATE_TAG, grammarState.phase);
      }

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
      Assert.assertEquals("xyz", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      Assert.assertEquals(15, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(16, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("a", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("b", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("c", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(9);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A__", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(10);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("B", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(11);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(12);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(13);
      Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(14);
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_ELEMENT_FRAGMENT, grammarState.targetGrammar.grammarType);
        Assert.assertEquals(SchemaInformedGrammar.ELEMENT_FRAGMENT_STATE_CONTENT, grammarState.phase);
      }

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      Assert.assertEquals(8, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(10, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A__", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("B", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(9);
      Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_FRAGMENT, grammarState.targetGrammar.grammarType);
        Assert.assertEquals(Grammar.DOCUMENT_STATE_COMPLETED, grammarState.phase);
      }
      
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(8, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(9,  eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A__", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("B", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_FRAGMENT, grammarState.targetGrammar.grammarType);
        Assert.assertEquals(SchemaInformedGrammar.DOCUMENT_STATE_END, grammarState.phase);
      }
      
      Assert.assertNull(scanner.nextEvent());
    }
  }

  /**
   * Test Element Fragment Grammar in default schema mode.
   * Invoke chars() at ELEMENT_FRAGMENT_STATE_TAG to transition to ELEMENT_FRAGMENT_STATE_CONTENT.
   */
  public void testElementFragmentDefault_01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/fragment_01.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);
    
    String xmlString;
    byte[] bts;
    
    xmlString = "<Z xmlns='urn:foo'>xyz</Z>";
    
    for (AlignmentType alignment : Alignments) {
      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();
      Scanner scanner;

      encoder.setAlignmentType(alignment);
      encoder.setFragment(true);
      decoder.setAlignmentType(alignment);
      decoder.setFragment(true);

      encoder.setGrammarCache(grammarCache);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);

      encoder.encode(new InputSource(new StringReader(xmlString)));
      
      bts = baos.toByteArray();
      
      decoder.setGrammarCache(grammarCache);
      decoder.setInputStream(new ByteArrayInputStream(bts));
      scanner = decoder.processHeader();
      
      EventDescription exiEvent;
  
      EventType eventType;
      EventTypeList eventTypeList;
      GrammarState grammarState;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_FRAGMENT, grammarState.targetGrammar.grammarType);
        Assert.assertEquals(Grammar.DOCUMENT_STATE_COMPLETED, grammarState.phase);
      }
      
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      Assert.assertEquals(5, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(9,  eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A__", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("B", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventType.ITEM_ED, eventType.itemType);
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_ELEMENT_FRAGMENT, grammarState.targetGrammar.grammarType);
        Assert.assertEquals(SchemaInformedGrammar.ELEMENT_FRAGMENT_STATE_TAG, grammarState.phase);
      }

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
      Assert.assertEquals("xyz", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      Assert.assertEquals(15, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(23, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("a", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("b", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("c", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(9);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A__", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(10);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("B", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(11);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(12);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(13);
      Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(14);
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      eventType = eventTypeList.item(16);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      eventType = eventTypeList.item(17);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_INVALID_VALUE, eventType.itemType);
      Assert.assertEquals("a", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(18);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_INVALID_VALUE, eventType.itemType);
      Assert.assertEquals("b", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(19);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_INVALID_VALUE, eventType.itemType);
      Assert.assertEquals("c", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(20);
      Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
      eventType = eventTypeList.item(21);
      Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(22);
      Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_ELEMENT_FRAGMENT, grammarState.targetGrammar.grammarType);
        Assert.assertEquals(SchemaInformedGrammar.ELEMENT_FRAGMENT_STATE_CONTENT, grammarState.phase);
      }

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      Assert.assertEquals(8, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(12, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A__", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("B", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(9);
      Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      eventType = eventTypeList.item(10);
      Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(11);
      Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_FRAGMENT, grammarState.targetGrammar.grammarType);
        Assert.assertEquals(Grammar.DOCUMENT_STATE_COMPLETED, grammarState.phase);
      }
      
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(8, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(9,  eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A__", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("B", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_FRAGMENT, grammarState.targetGrammar.grammarType);
        Assert.assertEquals(SchemaInformedGrammar.DOCUMENT_STATE_END, grammarState.phase);
      }
      
      Assert.assertNull(scanner.nextEvent());
    }
  }

  /**
   * Test Element Fragment Grammar in default schema mode.
   * Invoke chars() at ELEMENT_FRAGMENT_STATE_CONTENT which should incur no state transition.
   */
  public void testElementFragmentDefault_02() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/fragment_01.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.addCM(GrammarOptions.DEFAULT_OPTIONS));
    
    String xmlString;
    byte[] bts;
    
    xmlString = "<Z xmlns='urn:foo'><!-- Hello! -->xyz</Z>";
    
    for (AlignmentType alignment : Alignments) {
      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();
      Scanner scanner;

      encoder.setAlignmentType(alignment);
      encoder.setFragment(true);
      decoder.setAlignmentType(alignment);
      decoder.setFragment(true);

      encoder.setGrammarCache(grammarCache);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);

      encoder.encode(new InputSource(new StringReader(xmlString)));
      
      bts = baos.toByteArray();
      
      decoder.setGrammarCache(grammarCache);
      decoder.setInputStream(new ByteArrayInputStream(bts));
      scanner = decoder.processHeader();
      
      EventDescription exiEvent;
  
      EventType eventType;
      EventTypeList eventTypeList;
      GrammarState grammarState;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_FRAGMENT, grammarState.targetGrammar.grammarType);
        Assert.assertEquals(Grammar.DOCUMENT_STATE_COMPLETED, grammarState.phase);
      }
      
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      Assert.assertEquals(5, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(10,  eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A__", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("B", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventType.ITEM_ED, eventType.itemType);
      eventType = eventTypeList.item(9);
      Assert.assertEquals(EventType.ITEM_CM, eventType.itemType);
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_ELEMENT_FRAGMENT, grammarState.targetGrammar.grammarType);
        Assert.assertEquals(SchemaInformedGrammar.ELEMENT_FRAGMENT_STATE_TAG, grammarState.phase);
      }

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_CM, exiEvent.getEventKind());
      Assert.assertEquals(" Hello! ", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_CM, eventType.itemType);
      Assert.assertEquals(23, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(24, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("a", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("b", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("c", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(9);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A__", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(10);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("B", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(11);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(12);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(13);
      Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(14);
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      eventType = eventTypeList.item(15);
      Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      eventType = eventTypeList.item(16);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      eventType = eventTypeList.item(17);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_INVALID_VALUE, eventType.itemType);
      Assert.assertEquals("a", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(18);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_INVALID_VALUE, eventType.itemType);
      Assert.assertEquals("b", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(19);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_INVALID_VALUE, eventType.itemType);
      Assert.assertEquals("c", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(20);
      Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
      eventType = eventTypeList.item(21);
      Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(22);
      Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_ELEMENT_FRAGMENT, grammarState.targetGrammar.grammarType);
        Assert.assertEquals(SchemaInformedGrammar.ELEMENT_FRAGMENT_STATE_CONTENT, grammarState.phase);
      }

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
      Assert.assertEquals("xyz", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      Assert.assertEquals(9, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(13, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A__", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("B", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      eventType = eventTypeList.item(10);
      Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(11);
      Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
      eventType = eventTypeList.item(12);
      Assert.assertEquals(EventType.ITEM_CM, eventType.itemType);
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_ELEMENT_FRAGMENT, grammarState.targetGrammar.grammarType);
        Assert.assertEquals(SchemaInformedGrammar.ELEMENT_FRAGMENT_STATE_CONTENT, grammarState.phase);
      }

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      Assert.assertEquals(8, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(13, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A__", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("B", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(9);
      Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      eventType = eventTypeList.item(10);
      Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(11);
      Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
      eventType = eventTypeList.item(12);
      Assert.assertEquals(EventType.ITEM_CM, eventType.itemType);
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_FRAGMENT, grammarState.targetGrammar.grammarType);
        Assert.assertEquals(Grammar.DOCUMENT_STATE_COMPLETED, grammarState.phase);
      }
      
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(8, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(10,  eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A__", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("B", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(9);
      Assert.assertEquals(EventType.ITEM_CM, eventType.itemType);
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_FRAGMENT, grammarState.targetGrammar.grammarType);
        Assert.assertEquals(SchemaInformedGrammar.DOCUMENT_STATE_END, grammarState.phase);
      }
      
      Assert.assertNull(scanner.nextEvent());
    }
  }

  /**
   * Test Element Fragment Grammar in strict schema mode.
   * Invoking schemaAttribute() at ELEMENT_FRAGMENT_STATE_TAG involves no state transition.
   */
  public void testElementFragmentStrict_02() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/fragment_01.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);
    
    String xmlString;
    byte[] bts;
    
    xmlString = "<Z xmlns='urn:foo' xmlns:goo='urn:goo' goo:c='true'>xyz</Z>";
    
    for (AlignmentType alignment : Alignments) {
      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();
      Scanner scanner;

      encoder.setAlignmentType(alignment);
      encoder.setFragment(true);
      decoder.setAlignmentType(alignment);
      decoder.setFragment(true);

      encoder.setGrammarCache(grammarCache);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);

      encoder.encode(new InputSource(new StringReader(xmlString)));
      
      bts = baos.toByteArray();
      
      decoder.setGrammarCache(grammarCache);
      decoder.setInputStream(new ByteArrayInputStream(bts));
      scanner = decoder.processHeader();
      
      EventDescription exiEvent;
  
      EventType eventType;
      EventTypeList eventTypeList;
      GrammarState grammarState;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_FRAGMENT, grammarState.targetGrammar.grammarType);
        Assert.assertEquals(Grammar.DOCUMENT_STATE_COMPLETED, grammarState.phase);
      }
      
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      Assert.assertEquals(5, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(9,  eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A__", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("B", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventType.ITEM_ED, eventType.itemType);
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_ELEMENT_FRAGMENT, grammarState.targetGrammar.grammarType);
        Assert.assertEquals(SchemaInformedGrammar.ELEMENT_FRAGMENT_STATE_TAG, grammarState.phase);
      }

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_AT, exiEvent.getEventKind());
      Assert.assertEquals("true", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("c", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      int tp = ((EventTypeSchema)eventType).nd;
      Assert.assertEquals(EXISchemaConst.BOOLEAN_TYPE, corpus.getSerialOfType(tp));
      Assert.assertEquals(4, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(16, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("a", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("b", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(9);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A__", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(10);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("B", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(11);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(12);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(13);
      Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(14);
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      eventType = eventTypeList.item(15);
      Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_ELEMENT_FRAGMENT, grammarState.targetGrammar.grammarType);
        Assert.assertEquals(SchemaInformedGrammar.ELEMENT_FRAGMENT_STATE_TAG, grammarState.phase);
      }

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
      Assert.assertEquals("xyz", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      Assert.assertEquals(15, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(16, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("a", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("b", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("c", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(9);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A__", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(10);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("B", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(11);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(12);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(13);
      Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(14);
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_ELEMENT_FRAGMENT, grammarState.targetGrammar.grammarType);
        Assert.assertEquals(SchemaInformedGrammar.ELEMENT_FRAGMENT_STATE_CONTENT, grammarState.phase);
      }

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      Assert.assertEquals(8, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(10, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A__", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("B", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(9);
      Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_FRAGMENT, grammarState.targetGrammar.grammarType);
        Assert.assertEquals(Grammar.DOCUMENT_STATE_COMPLETED, grammarState.phase);
      }
      
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(8, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(9,  eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A__", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("B", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_FRAGMENT, grammarState.targetGrammar.grammarType);
        Assert.assertEquals(SchemaInformedGrammar.DOCUMENT_STATE_END, grammarState.phase);
      }
      
      Assert.assertNull(scanner.nextEvent());
    }
  }

  /**
   * Test Element Fragment Grammar in strict schema mode.
   * Invoke element() at ELEMENT_FRAGMENT_STATE_TAG to transition to ELEMENT_FRAGMENT_STATE_CONTENT
   * where the nested element itself is an element fragment.
   */
  public void testElementFragmentStrict_03() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/fragment_01.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);
    
    String xmlString;
    byte[] bts;
    
    xmlString = "<Z xmlns='urn:foo'><Z>xyz</Z></Z>";
    
    for (AlignmentType alignment : Alignments) {
      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();
      Scanner scanner;

      encoder.setAlignmentType(alignment);
      encoder.setFragment(true);
      decoder.setAlignmentType(alignment);
      decoder.setFragment(true);

      encoder.setGrammarCache(grammarCache);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);

      encoder.encode(new InputSource(new StringReader(xmlString)));
      
      bts = baos.toByteArray();
      
      decoder.setGrammarCache(grammarCache);
      decoder.setInputStream(new ByteArrayInputStream(bts));
      scanner = decoder.processHeader();
      
      EventDescription exiEvent;
  
      EventType eventType;
      EventTypeList eventTypeList;
      GrammarState grammarState;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_FRAGMENT, grammarState.targetGrammar.grammarType);
        Assert.assertEquals(Grammar.DOCUMENT_STATE_COMPLETED, grammarState.phase);
      }
      
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      Assert.assertEquals(5, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(9,  eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A__", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("B", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventType.ITEM_ED, eventType.itemType);
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_ELEMENT_FRAGMENT, grammarState.targetGrammar.grammarType);
        Assert.assertEquals(SchemaInformedGrammar.ELEMENT_FRAGMENT_STATE_TAG, grammarState.phase);
      }

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      Assert.assertEquals(11, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(16, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("a", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("b", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("c", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(9);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A__", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(10);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("B", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(12);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(13);
      Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(14);
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      eventType = eventTypeList.item(15);
      Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_ELEMENT_FRAGMENT, grammarState.targetGrammar.grammarType);
        Assert.assertEquals(SchemaInformedGrammar.ELEMENT_FRAGMENT_STATE_TAG, grammarState.phase);
      }

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
      Assert.assertEquals("xyz", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      Assert.assertEquals(15, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(16, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("a", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("b", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("c", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(9);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A__", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(10);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("B", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(11);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(12);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(13);
      Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(14);
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_ELEMENT_FRAGMENT, grammarState.targetGrammar.grammarType);
        Assert.assertEquals(SchemaInformedGrammar.ELEMENT_FRAGMENT_STATE_CONTENT, grammarState.phase);
      }

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      Assert.assertEquals(8, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(10, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A__", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("B", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(9);
      Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_ELEMENT_FRAGMENT, grammarState.targetGrammar.grammarType);
        Assert.assertEquals(SchemaInformedGrammar.ELEMENT_FRAGMENT_STATE_CONTENT, grammarState.phase);
      }

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      Assert.assertEquals(8, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(10, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A__", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("B", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(9);
      Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_FRAGMENT, grammarState.targetGrammar.grammarType);
        Assert.assertEquals(Grammar.DOCUMENT_STATE_COMPLETED, grammarState.phase);
      }
      
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(8, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(9,  eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A__", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("B", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_FRAGMENT, grammarState.targetGrammar.grammarType);
        Assert.assertEquals(SchemaInformedGrammar.DOCUMENT_STATE_END, grammarState.phase);
      }
      
      Assert.assertNull(scanner.nextEvent());
    }
  }

  /**
   * Test Element Fragment Grammar in strict schema mode.
   * Invoke nillify() at ELEMENT_FRAGMENT_STATE_TAG to transition to ELEMENT_FRAGMENT_EMPTY_STATE_TAG.
   */
  public void testElementFragmentStrict_04() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/fragment_01.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);
    
    String xmlString;
    byte[] bts;
    
    xmlString = "<Z xmlns='urn:foo' xmlns:goo='urn:goo' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' xsi:nil='true' goo:a='_3.1415926_'/>";
    
    for (AlignmentType alignment : Alignments) {
      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();
      Scanner scanner;

      encoder.setAlignmentType(alignment);
      encoder.setFragment(true);
      decoder.setAlignmentType(alignment);
      decoder.setFragment(true);

      encoder.setGrammarCache(grammarCache);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);

      encoder.encode(new InputSource(new StringReader(xmlString)));
      
      bts = baos.toByteArray();
      
      decoder.setGrammarCache(grammarCache);
      decoder.setInputStream(new ByteArrayInputStream(bts));
      scanner = decoder.processHeader();
      
      EventDescription exiEvent;
  
      EventType eventType;
      EventTypeList eventTypeList;
      GrammarState grammarState;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_FRAGMENT, grammarState.targetGrammar.grammarType);
        Assert.assertEquals(Grammar.DOCUMENT_STATE_COMPLETED, grammarState.phase);
      }
      
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      Assert.assertEquals(5, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(9,  eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A__", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("B", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventType.ITEM_ED, eventType.itemType);
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_ELEMENT_FRAGMENT, grammarState.targetGrammar.grammarType);
        Assert.assertEquals(SchemaInformedGrammar.ELEMENT_FRAGMENT_STATE_TAG, grammarState.phase);
      }

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_NL, exiEvent.getEventKind());
      Assert.assertTrue(((EXIEventSchemaNil)exiEvent).isNilled());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
      Assert.assertEquals(1, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(16, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("a", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("b", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("c", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(9);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A__", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(10);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("B", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(11);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(12);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(13);
      Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(14);
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      eventType = eventTypeList.item(15);
      Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_ELEMENT_FRAGMENT, grammarState.targetGrammar.grammarType);
        Assert.assertEquals(SchemaInformedGrammar.ELEMENT_FRAGMENT_EMPTY_STATE_TAG, grammarState.phase);
      }

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_AT, exiEvent.getEventKind());
      Assert.assertEquals("_3.1415926_", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("a", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      Assert.assertEquals(EXISchema.NIL_NODE, ((EventTypeSchema)eventType).nd);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(5, eventTypeList.getLength());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("b", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("c", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_ELEMENT_FRAGMENT, grammarState.targetGrammar.grammarType);
        Assert.assertEquals(SchemaInformedGrammar.ELEMENT_FRAGMENT_EMPTY_STATE_TAG, grammarState.phase);
      }

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      Assert.assertEquals(4, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(5, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("a", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("b", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("c", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_FRAGMENT, grammarState.targetGrammar.grammarType);
        Assert.assertEquals(Grammar.DOCUMENT_STATE_COMPLETED, grammarState.phase);
      }
      
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(8, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(9,  eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A__", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("B", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_FRAGMENT, grammarState.targetGrammar.grammarType);
        Assert.assertEquals(SchemaInformedGrammar.DOCUMENT_STATE_END, grammarState.phase);
      }
      
      Assert.assertNull(scanner.nextEvent());
    }
  }

  /**
   * Test Element Fragment Grammar in default schema mode.
   * Invoke nillify() at ELEMENT_FRAGMENT_STATE_TAG to transition to ELEMENT_FRAGMENT_EMPTY_STATE_TAG.
   */
  public void testElementFragmentDefault_04() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/fragment_01.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);
    
    String xmlString;
    byte[] bts;
    
    xmlString = "<Z xmlns='urn:foo' xmlns:goo='urn:goo' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' xsi:nil='true' goo:a='_3.1415926_'>xyz</Z>";
    
    for (AlignmentType alignment : Alignments) {
      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();
      Scanner scanner;

      encoder.setAlignmentType(alignment);
      encoder.setFragment(true);
      decoder.setAlignmentType(alignment);
      decoder.setFragment(true);

      encoder.setGrammarCache(grammarCache);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);

      encoder.encode(new InputSource(new StringReader(xmlString)));
      
      bts = baos.toByteArray();
      
      decoder.setGrammarCache(grammarCache);
      decoder.setInputStream(new ByteArrayInputStream(bts));
      scanner = decoder.processHeader();
      
      EventDescription exiEvent;
  
      EventType eventType;
      EventTypeList eventTypeList;
      GrammarState grammarState;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_FRAGMENT, grammarState.targetGrammar.grammarType);
        Assert.assertEquals(Grammar.DOCUMENT_STATE_COMPLETED, grammarState.phase);
      }
      
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      Assert.assertEquals(5, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(9,  eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A__", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("B", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventType.ITEM_ED, eventType.itemType);
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_ELEMENT_FRAGMENT, grammarState.targetGrammar.grammarType);
        Assert.assertEquals(SchemaInformedGrammar.ELEMENT_FRAGMENT_STATE_TAG, grammarState.phase);
      }

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_NL, exiEvent.getEventKind());
      Assert.assertTrue(((EXIEventSchemaNil)exiEvent).isNilled());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
      Assert.assertEquals(1, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(23, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("a", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("b", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("c", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(9);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A__", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(10);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("B", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(11);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(12);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(13);
      Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(14);
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      eventType = eventTypeList.item(15);
      Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      eventType = eventTypeList.item(16);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      eventType = eventTypeList.item(17);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_INVALID_VALUE, eventType.itemType);
      Assert.assertEquals("a", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(18);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_INVALID_VALUE, eventType.itemType);
      Assert.assertEquals("b", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(19);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_INVALID_VALUE, eventType.itemType);
      Assert.assertEquals("c", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(20);
      Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
      eventType = eventTypeList.item(21);
      Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(22);
      Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_ELEMENT_FRAGMENT, grammarState.targetGrammar.grammarType);
        Assert.assertEquals(SchemaInformedGrammar.ELEMENT_FRAGMENT_EMPTY_STATE_TAG, grammarState.phase);
      }

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_AT, exiEvent.getEventKind());
      Assert.assertEquals("_3.1415926_", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("a", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      Assert.assertEquals(EXISchema.NIL_NODE, ((EventTypeSchema)eventType).nd);
      Assert.assertEquals(2, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(14, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("b", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("c", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_INVALID_VALUE, eventType.itemType);
      Assert.assertEquals("a", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(9);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_INVALID_VALUE, eventType.itemType);
      Assert.assertEquals("b", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(10);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_INVALID_VALUE, eventType.itemType);
      Assert.assertEquals("c", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(11);
      Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
      eventType = eventTypeList.item(12);
      Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(13);
      Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_ELEMENT_FRAGMENT, grammarState.targetGrammar.grammarType);
        Assert.assertEquals(SchemaInformedGrammar.ELEMENT_FRAGMENT_EMPTY_STATE_TAG, grammarState.phase);
      }

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
      Assert.assertEquals("xyz", exiEvent.getCharacters().makeString());
      Assert.assertEquals(13, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(14, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("a", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("b", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("c", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_INVALID_VALUE, eventType.itemType);
      Assert.assertEquals("a", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(9);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_INVALID_VALUE, eventType.itemType);
      Assert.assertEquals("b", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(10);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_INVALID_VALUE, eventType.itemType);
      Assert.assertEquals("c", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(11);
      Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
      eventType = eventTypeList.item(12);
      Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_ELEMENT_FRAGMENT, grammarState.targetGrammar.grammarType);
        Assert.assertEquals(SchemaInformedGrammar.ELEMENT_FRAGMENT_EMPTY_STATE_CONTENT, grammarState.phase);
      }

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(3, eventTypeList.getLength());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_FRAGMENT, grammarState.targetGrammar.grammarType);
        Assert.assertEquals(Grammar.DOCUMENT_STATE_COMPLETED, grammarState.phase);
      }
      
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(8, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(9,  eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A__", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("B", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_FRAGMENT, grammarState.targetGrammar.grammarType);
        Assert.assertEquals(SchemaInformedGrammar.DOCUMENT_STATE_END, grammarState.phase);
      }
      
      Assert.assertNull(scanner.nextEvent());
    }
  }

  /**
   * Test Element Fragment Grammar in strict schema mode.
   * Invoke xsitp() at ELEMENT_FRAGMENT_STATE_TAG to transition to ELEMENT_STATE_TAG.
   */
  public void testElementFragmentStrict_05() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/fragment_01.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);
    
    String xmlString;
    byte[] bts;
    
    xmlString = "<foo:Z xmlns:foo='urn:foo' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' xsi:type='foo:ATYPE'><foo:Z>12345</foo:Z></foo:Z>";
    
    for (AlignmentType alignment : Alignments) {
      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();
      Scanner scanner;

      encoder.setAlignmentType(alignment);
      encoder.setFragment(true);
      decoder.setAlignmentType(alignment);
      decoder.setFragment(true);

      encoder.setGrammarCache(grammarCache);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);

      encoder.encode(new InputSource(new StringReader(xmlString)));
      
      bts = baos.toByteArray();
      
      decoder.setGrammarCache(grammarCache);
      decoder.setInputStream(new ByteArrayInputStream(bts));
      scanner = decoder.processHeader();
      
      EventDescription exiEvent;
  
      EventType eventType;
      EventTypeList eventTypeList;
      GrammarState grammarState;
      int nd;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_FRAGMENT, grammarState.targetGrammar.grammarType);
        Assert.assertEquals(Grammar.DOCUMENT_STATE_COMPLETED, grammarState.phase);
      }
      
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      Assert.assertEquals(5, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(9,  eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A__", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("B", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventType.ITEM_ED, eventType.itemType);
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_ELEMENT_FRAGMENT, grammarState.targetGrammar.grammarType);
        Assert.assertEquals(SchemaInformedGrammar.ELEMENT_FRAGMENT_STATE_TAG, grammarState.phase);
      }

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_TP, exiEvent.getEventKind());
      nd = ((EXIEventSchemaType)exiEvent).getTp();
      Assert.assertEquals("ATYPE", corpus.getNameOfType(nd));
      Assert.assertEquals("urn:foo", EXISchemaUtil.getTargetNamespaceNameOfType(nd, corpus));
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(16, eventTypeList.getLength());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("a", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("b", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("c", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(9);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A__", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(10);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("B", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(11);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(12);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(13);
      Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(14);
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      eventType = eventTypeList.item(15);
      Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
     /**
      * This assertion is not relevant in Nagasena
      * if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
      *   grammarState = scanner.getGrammarState();
      *   Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_ELEMENT_TAG, grammarState.targetGrammar.grammarType);
      *   Assert.assertEquals(SchemaInformedGrammar.ELEMENT_STATE_TAG, grammarState.phase);
      * }
      */

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      Assert.assertEquals(2, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(4,  eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("b", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
     /**
      * This assertion is not relevant in Nagasena
      * if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
      *   grammarState = scanner.getGrammarState();
      *   Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_ELEMENT, grammarState.targetGrammar.grammarType);
      *   Assert.assertEquals(SchemaInformedGrammar.ELEMENT_STATE_UNBOUND, grammarState.phase);
      * }
      */

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
      Assert.assertEquals("12345", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        nd = scanner.currentState.contentDatatype; 
        Assert.assertEquals(EXISchemaConst.INT_TYPE, corpus.getSerialOfType(nd));
      }
      Assert.assertEquals(1, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(2,  eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
     /**
      * This assertion is not relevant in Nagasena
      * if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
      *   grammarState = scanner.getGrammarState();
      *   Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_ELEMENT_CONTENT, grammarState.targetGrammar.grammarType);
      *   Assert.assertEquals(SchemaInformedGrammar.ELEMENT_STATE_CONTENT_COMPLETE, grammarState.phase);
      * }
      */

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(1, eventTypeList.getLength());
     /**
      * This assertion is not relevant in Nagasena
      * if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
      *   grammarState = scanner.getGrammarState();
      *   Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_ELEMENT_CONTENT, grammarState.targetGrammar.grammarType);
      *   Assert.assertEquals(SchemaInformedGrammar.ELEMENT_STATE_CONTENT_COMPLETE, grammarState.phase);
      * }
      */

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(1, eventTypeList.getLength());
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_FRAGMENT, grammarState.targetGrammar.grammarType);
        Assert.assertEquals(Grammar.DOCUMENT_STATE_COMPLETED, grammarState.phase);
      }
      
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(8, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(9,  eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A__", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("B", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_FRAGMENT, grammarState.targetGrammar.grammarType);
        Assert.assertEquals(SchemaInformedGrammar.DOCUMENT_STATE_END, grammarState.phase);
      }
      
      Assert.assertNull(scanner.nextEvent());
    }
  }

  /**
   * Test Element Fragment Grammar in strict schema mode.
   * Invoke element() at ELEMENT_FRAGMENT_STATE_TAG to transition to ELEMENT_FRAGMENT_STATE_CONTENT
   * where the nested element is *not* an element fragment.
   */
  public void testElementFragmentStrict_06() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/fragment_01.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);
    
    String xmlString;
    byte[] bts;
    
    xmlString = "<foo:Z xmlns:foo='urn:foo' xmlns:goo='urn:goo'><goo:A>true</goo:A></foo:Z>";
    
    for (AlignmentType alignment : Alignments) {
      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();
      Scanner scanner;

      encoder.setAlignmentType(alignment);
      encoder.setFragment(true);
      decoder.setAlignmentType(alignment);
      decoder.setFragment(true);

      encoder.setGrammarCache(grammarCache);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);

      encoder.encode(new InputSource(new StringReader(xmlString)));
      
      bts = baos.toByteArray();
      
      decoder.setGrammarCache(grammarCache);
      decoder.setInputStream(new ByteArrayInputStream(bts));
      scanner = decoder.processHeader();
      
      EventDescription exiEvent;
  
      EventType eventType;
      EventTypeList eventTypeList;
      GrammarState grammarState;
      int nd;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_FRAGMENT, grammarState.targetGrammar.grammarType);
        Assert.assertEquals(Grammar.DOCUMENT_STATE_COMPLETED, grammarState.phase);
      }
      
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      Assert.assertEquals(5, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(9,  eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A__", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("B", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventType.ITEM_ED, eventType.itemType);
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_ELEMENT_FRAGMENT, grammarState.targetGrammar.grammarType);
        Assert.assertEquals(SchemaInformedGrammar.ELEMENT_FRAGMENT_STATE_TAG, grammarState.phase);
      }

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      Assert.assertEquals(6, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(16, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("a", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("b", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("c", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(9);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A__", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(10);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("B", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(11);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(12);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(13);
      Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(14);
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      eventType = eventTypeList.item(15);
      Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
     /**
      * This assertion is not relevant in Nagasena
      * if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
      *   grammarState = scanner.getGrammarState();
      *   Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_ELEMENT, grammarState.targetGrammar.grammarType);
      *   Assert.assertEquals(SchemaInformedGrammar.ELEMENT_STATE_UNBOUND, grammarState.phase);
      * }
      */
      
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
      Assert.assertEquals("true", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        nd = scanner.currentState.contentDatatype; 
        Assert.assertEquals(EXISchemaConst.BOOLEAN_TYPE, corpus.getSerialOfType(nd));
      }
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(1,  eventTypeList.getLength());
     /**
      * This assertion is not relevant in Nagasena
      * if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
      *   grammarState = scanner.getGrammarState();
      *   Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_ELEMENT_CONTENT, grammarState.targetGrammar.grammarType);
      *   Assert.assertEquals(SchemaInformedGrammar.ELEMENT_STATE_CONTENT_COMPLETE, grammarState.phase);
      * }
      */

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(1, eventTypeList.getLength());
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_ELEMENT_FRAGMENT, grammarState.targetGrammar.grammarType);
        Assert.assertEquals(SchemaInformedGrammar.ELEMENT_FRAGMENT_STATE_CONTENT, grammarState.phase);
      }

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      Assert.assertEquals(8, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(10, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A__", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("B", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(9);
      Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_FRAGMENT, grammarState.targetGrammar.grammarType);
        Assert.assertEquals(Grammar.DOCUMENT_STATE_COMPLETED, grammarState.phase);
      }

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(8, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(9,  eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A__", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("B", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.name);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_FRAGMENT, grammarState.targetGrammar.grammarType);
        Assert.assertEquals(SchemaInformedGrammar.DOCUMENT_STATE_END, grammarState.phase);
      }
      
      Assert.assertNull(scanner.nextEvent());
    }
  }

  /**
   * There are 3 declarations of the element name "foo:A" all of which
   * use the same type. The same is true for the element name "foo:B".
   * 
   * <foo:A xmlns:foo="urn:foo">
   *   <foo:A>
   *     <foo:A />
   *     <foo:B />
   *   </foo:A>
   *   <foo:B>
   *     <foo:B />
   *     <foo:A />
   *   </foo:B>
   * </foo:A>
   * <foo:B xmlns:foo="urn:foo">
   *   <foo:B>
   *     <foo:B />
   *     <foo:A />
   *   </foo:B>
   *   <foo:A>
   *     <foo:A />
   *     <foo:B />
   *   </foo:A>
   * </foo:B>
   */
  public void testDecodeFragment_03() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/interop/schemaInformedGrammar/declaredProductions/fragment-b.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.addPI(GrammarOptions.DEFAULT_OPTIONS));
    
    EXIDecoder decoder = new EXIDecoder();
    Scanner scanner;
    
    decoder.setAlignmentType(AlignmentType.byteAligned);

    URL url = resolveSystemIdAsURL("/interop/schemaInformedGrammar/declaredProductions/fragment-03.byteAligned");
    
    decoder.setGrammarCache(grammarCache);
    decoder.setFragment(true);
    decoder.setInputStream(url.openStream());
    scanner = decoder.processHeader();
    
    EventDescription exiEvent;
    EventType eventType;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertSame(exiEvent, eventType);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
    Assert.assertEquals("A", eventType.name);
    Assert.assertEquals("urn:foo", eventType.uri);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
    Assert.assertEquals("A", eventType.name);
    Assert.assertEquals("urn:foo", eventType.uri);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
    Assert.assertEquals("A", eventType.name);
    Assert.assertEquals("urn:foo", eventType.uri);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
    Assert.assertEquals("B", eventType.name);
    Assert.assertEquals("urn:foo", eventType.uri);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
    Assert.assertEquals("B", eventType.name);
    Assert.assertEquals("urn:foo", eventType.uri);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
    Assert.assertEquals("B", eventType.name);
    Assert.assertEquals("urn:foo", eventType.uri);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
    Assert.assertEquals("A", eventType.name);
    Assert.assertEquals("urn:foo", eventType.uri);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
    Assert.assertEquals("B", eventType.name);
    Assert.assertEquals("urn:foo", eventType.uri);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
    Assert.assertEquals("B", eventType.name);
    Assert.assertEquals("urn:foo", eventType.uri);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
    Assert.assertEquals("B", eventType.name);
    Assert.assertEquals("urn:foo", eventType.uri);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
    Assert.assertEquals("A", eventType.name);
    Assert.assertEquals("urn:foo", eventType.uri);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
    Assert.assertEquals("A", eventType.name);
    Assert.assertEquals("urn:foo", eventType.uri);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
    Assert.assertEquals("A", eventType.name);
    Assert.assertEquals("urn:foo", eventType.uri);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
    Assert.assertEquals("B", eventType.name);
    Assert.assertEquals("urn:foo", eventType.uri);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);

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
    eventType = exiEvent.getEventType();
    Assert.assertSame(exiEvent, eventType);
    
    Assert.assertNull(scanner.nextEvent());
  }
  
  /**
   * There are 5 declarations of the element name "foo:A" the types of
   * which vary. The same is true for the element name "foo:B".
   * 
   * <foo:A xmlns:foo="urn:foo">
   *   <foo:A>
   *     <foo:A />
   *     <foo:B />
   *   </foo:A>
   *   <foo:B>
   *     <foo:B />
   *     <foo:A />
   *   </foo:B>
   * </foo:A>
   * <foo:B xmlns:foo="urn:foo">
   *   <foo:B>
   *     <foo:B />
   *     <foo:A />
   *   </foo:B>
   *   <foo:A>
   *     <foo:A />
   *     <foo:B />
   *   </foo:A>
   * </foo:B>
   */
  public void testDecodeFragment_04() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/interop/schemaInformedGrammar/declaredProductions/fragment-c.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.addPI(GrammarOptions.DEFAULT_OPTIONS));
    
    EXIDecoder decoder = new EXIDecoder();
    Scanner scanner;
    
    decoder.setAlignmentType(AlignmentType.byteAligned);

    URL url = resolveSystemIdAsURL("/interop/schemaInformedGrammar/declaredProductions/fragment-04.byteAligned");
    
    decoder.setGrammarCache(grammarCache);
    decoder.setFragment(true);
    decoder.setInputStream(url.openStream());
    scanner = decoder.processHeader();
    
    EventDescription exiEvent;
    EventType eventType;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertSame(exiEvent, eventType);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
    Assert.assertEquals("A", eventType.name);
    Assert.assertEquals("urn:foo", eventType.uri);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
    Assert.assertEquals("A", eventType.name);
    Assert.assertEquals("urn:foo", eventType.uri);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
    Assert.assertEquals("A", eventType.name);
    Assert.assertEquals("urn:foo", eventType.uri);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
    Assert.assertEquals("B", eventType.name);
    Assert.assertEquals("urn:foo", eventType.uri);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
    Assert.assertEquals("B", eventType.name);
    Assert.assertEquals("urn:foo", eventType.uri);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
    Assert.assertEquals("B", eventType.name);
    Assert.assertEquals("urn:foo", eventType.uri);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
    Assert.assertEquals("A", eventType.name);
    Assert.assertEquals("urn:foo", eventType.uri);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
    Assert.assertEquals("B", eventType.name);
    Assert.assertEquals("urn:foo", eventType.uri);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
    Assert.assertEquals("B", eventType.name);
    Assert.assertEquals("urn:foo", eventType.uri);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
    Assert.assertEquals("B", eventType.name);
    Assert.assertEquals("urn:foo", eventType.uri);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
    Assert.assertEquals("A", eventType.name);
    Assert.assertEquals("urn:foo", eventType.uri);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
    Assert.assertEquals("A", eventType.name);
    Assert.assertEquals("urn:foo", eventType.uri);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
    Assert.assertEquals("A", eventType.name);
    Assert.assertEquals("urn:foo", eventType.uri);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
    Assert.assertEquals("B", eventType.name);
    Assert.assertEquals("urn:foo", eventType.uri);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);

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
    eventType = exiEvent.getEventType();
    Assert.assertSame(exiEvent, eventType);
    
    Assert.assertNull(scanner.nextEvent());
  }
  
  /**
   * <foo:C xmlns:foo="urn:foo" ><None/></foo:C>
   * 
   * Start-tag of the element "None" should cause the grammar of C to move to
   * the a state where no attributes are allowed.
   */
  public void testDecodeElementFragment_02() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/interop/schemaInformedGrammar/declaredProductions/elementFragment.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);
    
    EXIDecoder decoder = new EXIDecoder();
    Scanner scanner;
    
    decoder.setAlignmentType(AlignmentType.byteAligned);

    URL url = resolveSystemIdAsURL("/interop/schemaInformedGrammar/declaredProductions/elementFragment-02b.byteAligned");
    
    decoder.setGrammarCache(grammarCache);
    decoder.setFragment(true);
    decoder.setInputStream(url.openStream());
    scanner = decoder.processHeader();
    
    EventDescription exiEvent;
    EventType eventType;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertSame(exiEvent, eventType);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
    Assert.assertEquals("C", eventType.name);
    Assert.assertEquals("urn:foo", eventType.uri);
    
    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    Assert.assertEquals("None", exiEvent.getName());
    Assert.assertEquals("", exiEvent.getURI());
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
    eventType = exiEvent.getEventType();
    Assert.assertSame(exiEvent, eventType);
    
    Assert.assertNull(scanner.nextEvent());
  }
  
  /**
   * <foo:C xmlns:foo="urn:foo" xmlns:goo="urn:goo" xmlns:aoo="urn:aoo" 
   *   xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' 
   *   none="Welcome to the world of element fragment grammar!"
   *   xsi:nil="true"
   *   aoo:b="2010-04-28"
   *   foo:d="12:34:58" />
   * 
   * The content of the root element (i.e. <foo:C>) is evaluated using element
   * fragment grammar.  
   */
  public void testDecodeElementFragment_03() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/interop/schemaInformedGrammar/declaredProductions/elementFragment.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);
    
    EXIDecoder decoder = new EXIDecoder();
    Scanner scanner;
    
    decoder.setAlignmentType(AlignmentType.bitPacked);

    URL url = resolveSystemIdAsURL("/interop/schemaInformedGrammar/declaredProductions/elementFragment-03.bitPacked");
    
    decoder.setGrammarCache(grammarCache);
    decoder.setFragment(true);
    decoder.setInputStream(url.openStream());
    scanner = decoder.processHeader();
    
    EventDescription exiEvent;
    EventType eventType;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertSame(exiEvent, eventType);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
    Assert.assertEquals("C", eventType.name);
    Assert.assertEquals("urn:foo", eventType.uri);
    
    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_NL, exiEvent.getEventKind());
    Assert.assertTrue(((EXIEventSchemaNil)exiEvent).isNilled());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_NIL, eventType.itemType);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_AT, exiEvent.getEventKind());
    Assert.assertEquals("2010-04-28", exiEvent.getCharacters().makeString());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
    Assert.assertEquals("b", eventType.name);
    Assert.assertEquals("urn:aoo", eventType.uri);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_AT, exiEvent.getEventKind());
    Assert.assertEquals("12:34:58", exiEvent.getCharacters().makeString());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
    Assert.assertEquals("d", eventType.name);
    Assert.assertEquals("urn:foo", eventType.uri);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_AT, exiEvent.getEventKind());
    Assert.assertEquals("", exiEvent.getURI());
    Assert.assertEquals("none", exiEvent.getName());
    Assert.assertEquals("Welcome to the world of element fragment grammar!", exiEvent.getCharacters().makeString());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertSame(exiEvent, eventType);
    
    Assert.assertNull(scanner.nextEvent());
  }

  /**
   * <foo:C xmlns:foo="urn:foo" xmlns:goo="urn:goo" xmlns:aoo="urn:aoo" 
   *   xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' 
   *   none="Welcome to the world of element fragment grammar!"
   *   xsi:type="foo:tC" foo:c="2010-08-18">
   *   <foo:C />
   *   <foo:D />
   * </foo:C>
   * 
   * The content of the root element (i.e. <foo:C>) initially is being evaluated 
   * using element fragment grammar, however, the bulk of it is processed by
   * a specific type grammar corresponding to "foo:tC" upon xsi:type="foo:tC". 
   */
  public void testDecodeElementFragment_04() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/interop/schemaInformedGrammar/declaredProductions/elementFragment.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);
    
    EXIDecoder decoder = new EXIDecoder();
    Scanner scanner;
    
    decoder.setAlignmentType(AlignmentType.byteAligned);

    URL url = resolveSystemIdAsURL("/interop/schemaInformedGrammar/declaredProductions/elementFragment-04.byteAligned");
    
    decoder.setGrammarCache(grammarCache);
    decoder.setFragment(true);
    decoder.setInputStream(url.openStream());
    scanner = decoder.processHeader();
    
    EventDescription exiEvent;
    EventType eventType;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertSame(exiEvent, eventType);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
    Assert.assertEquals("C", eventType.name);
    Assert.assertEquals("urn:foo", eventType.uri);
    
    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_TP, exiEvent.getEventKind());
    int nd = ((EXIEventSchemaType)exiEvent).getTp();
    Assert.assertEquals("tC", corpus.getNameOfType(nd));
    Assert.assertEquals("urn:foo", EXISchemaUtil.getTargetNamespaceNameOfType(nd, corpus));
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_AT, exiEvent.getEventKind());
    Assert.assertEquals("2010-08-18", exiEvent.getCharacters().makeString());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
    Assert.assertEquals("c", eventType.name);
    Assert.assertEquals("urn:foo", eventType.uri);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_AT, exiEvent.getEventKind());
    Assert.assertEquals("", exiEvent.getURI());
    Assert.assertEquals("none", exiEvent.getName());
    Assert.assertEquals("Welcome to the world of element fragment grammar!", exiEvent.getCharacters().makeString());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
    Assert.assertEquals("C", eventType.name);
    Assert.assertEquals("urn:foo", eventType.uri);
    
    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
    Assert.assertEquals("D", eventType.name);
    Assert.assertEquals("urn:foo", eventType.uri);
    
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
    eventType = exiEvent.getEventType();
    Assert.assertSame(exiEvent, eventType);
    
    Assert.assertNull(scanner.nextEvent());
  }
  
}
