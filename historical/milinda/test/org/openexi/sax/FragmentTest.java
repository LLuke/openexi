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
import org.openexi.proc.common.EXIEvent;
import org.openexi.proc.common.EXIOptions;
import org.openexi.proc.common.EventCode;
import org.openexi.proc.common.EventType;
import org.openexi.proc.common.EventTypeList;
import org.openexi.proc.common.GrammarOptions;
import org.openexi.proc.common.QName;
import org.openexi.proc.events.EXIEventSchemaNil;
import org.openexi.proc.events.EXIEventSchemaType;
import org.openexi.proc.grammars.BuiltinGrammar;
import org.openexi.proc.grammars.DocumentGrammarState;
import org.openexi.proc.grammars.EventTypeSchema;
import org.openexi.proc.grammars.EventTypeSchemaAttribute;
import org.openexi.proc.grammars.Grammar;
import org.openexi.proc.grammars.GrammarCache;
import org.openexi.proc.grammars.GrammarState;
import org.openexi.proc.grammars.SchemaInformedGrammar;
import org.openexi.proc.io.Apparatus;
import org.openexi.proc.io.Scanner;
import org.openexi.proc.io.Scribble;
import org.openexi.proc.io.Scriber;
import org.openexi.proc.io.ScriberFactory;
import org.openexi.proc.io.StringTable;
import org.openexi.proc.io.ValueScriber;
import org.openexi.sax.Transmogrifier;
import org.openexi.schema.EXISchema;
import org.openexi.schema.EXISchemaConst;
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
      
      encoder.setEXISchema(grammarCache);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);

      encoder.encode(new InputSource(new StringReader(xmlString)));
      
      bts = baos.toByteArray();
      
      decoder.setEXISchema(grammarCache);
      decoder.setInputStream(new ByteArrayInputStream(bts));
      scanner = decoder.processHeader();
      
      EXIEvent exiEvent;
      EventType eventType;
      EventTypeList eventTypeList;
      GrammarState grammarState;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EXIEvent.EVENT_SD, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_FRAGMENT, grammarState.targetGrammar.getGrammarType());
        Assert.assertEquals(Grammar.DOCUMENT_STATE_COMPLETED, grammarState.phase);
      }
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(9,  eventTypeList.getLength());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A__", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("B", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventCode.ITEM_ED, eventType.itemType);
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_ELEMENT, grammarState.targetGrammar.getGrammarType());
        Assert.assertEquals(SchemaInformedGrammar.ELEMENT_STATE_UNBOUND, grammarState.phase);
      }

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
      Assert.assertEquals("true", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_CH, eventType.itemType);
      Assert.assertEquals(corpus.getBuiltinTypeOfSchema(EXISchemaConst.BOOLEAN_TYPE), 
          ((EventTypeSchema)eventType).getSchemaSubstance());
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(1, eventTypeList.getLength());
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_ELEMENT_CONTENT, grammarState.targetGrammar.getGrammarType());
        Assert.assertEquals(SchemaInformedGrammar.ELEMENT_STATE_CONTENT_COMPLETE, grammarState.phase);
      }
      
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(1, eventTypeList.getLength());
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_FRAGMENT, grammarState.targetGrammar.getGrammarType());
        Assert.assertEquals(Grammar.DOCUMENT_STATE_COMPLETED, grammarState.phase);
      }
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(8, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(9,  eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A__", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("B", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_FRAGMENT, grammarState.targetGrammar.getGrammarType());
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

    final DocumentGrammarState documentState = new DocumentGrammarState();

    final Grammar documentGrammar = grammarCache.retrieveDocumentGrammar(true, documentState.eventTypesWorkSpace); 

    final Scribble scribble = new Scribble();

    final int booleanType = corpus.getBuiltinTypeOfSchema(EXISchemaConst.BOOLEAN_TYPE);

    for (AlignmentType alignment : Alignments) {
      final Scriber scriber = ScriberFactory.createScriber(alignment);
      scriber.setSchema(grammarCache.getEXISchema(), (QName[])null, 0);
      scriber.setPreserveNS(GrammarOptions.hasNS(grammarCache.grammarOptions));
      scriber.setStringTable(new StringTable(grammarCache.getEXISchema()));
      scriber.setValueMaxLength(EXIOptions.VALUE_MAX_LENGTH_UNBOUNDED);

      final ValueScriber booleanValueScriber = scriber.getValueScriber(booleanType);

      final ByteArrayOutputStream baos = new ByteArrayOutputStream();
      scriber.setOutputStream(baos);

      Scriber.writeHeaderPreamble(baos, false, false);
      
      documentGrammar.init(documentState);

      EventTypeList eventTypes;
      EventType eventType;

      eventTypes = documentState.getNextEventTypes();
      
      eventType = eventTypes.item(0);
      Assert.assertEquals(EventCode.ITEM_SD, eventType.itemType);
      Assert.assertEquals(1, eventTypes.getLength());
      scriber.writeEventType(eventType);
      documentState.startDocument();
      eventTypes = documentState.getNextEventTypes();
      Assert.assertEquals(9, eventTypes.getLength());

      eventType = eventTypes.item(0);
      Assert.assertEquals("A", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      scriber.writeEventType(eventType);
      documentState.startElement(0, "urn:goo", "A");
      eventTypes = documentState.getNextEventTypes();
      Assert.assertEquals(1, eventTypes.getLength());

      eventType = eventTypes.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_CH, eventType.itemType);
      scriber.writeEventType(eventType);
      booleanValueScriber.process("true", booleanType, corpus, scribble);
      booleanValueScriber.scribe("true", scribble, "A", "urn:goo", booleanType);
      documentState.characters();
      eventTypes = documentState.getNextEventTypes();
      Assert.assertEquals(1, eventTypes.getLength());
      
      eventType = eventTypes.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      scriber.writeEventType(eventType);
      documentState.endElement("urn:goo", "A");
      eventTypes = documentState.getNextEventTypes();
      Assert.assertEquals(9, eventTypes.getLength());

      eventType = eventTypes.item(0);
      Assert.assertEquals("A", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      scriber.writeEventType(eventType);
      documentState.startElement(0, "urn:goo", "A");
      eventTypes = documentState.getNextEventTypes();
      Assert.assertEquals(1, eventTypes.getLength());

      eventType = eventTypes.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_CH, eventType.itemType);
      scriber.writeEventType(eventType);
      booleanValueScriber.process("false", booleanType, corpus, scribble);
      booleanValueScriber.scribe("false", scribble, "A", "urn:goo", booleanType);
      documentState.characters();
      eventTypes = documentState.getNextEventTypes();
      Assert.assertEquals(1, eventTypes.getLength());
      
      eventType = eventTypes.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      scriber.writeEventType(eventType);
      documentState.endElement("urn:goo", "A");
      eventTypes = documentState.getNextEventTypes();
      Assert.assertEquals(9, eventTypes.getLength());
      
      eventType = eventTypes.item(8);
      Assert.assertEquals(EventCode.ITEM_ED, eventType.itemType);
      scriber.writeEventType(eventType);
      documentState.endDocument();
      eventTypes = documentState.getNextEventTypes();
      Assert.assertEquals(0, eventTypes.getLength());
      
      scriber.finish();
      
      
      EXIDecoder decoder = new EXIDecoder();
      Scanner scanner;

      decoder.setAlignmentType(alignment);
      decoder.setFragment(true);
      
      decoder.setEXISchema(grammarCache);
      decoder.setInputStream(new ByteArrayInputStream(baos.toByteArray()));
      scanner = decoder.processHeader();
      
      EXIEvent exiEvent;
      EventTypeList eventTypeList;
      GrammarState grammarState;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EXIEvent.EVENT_SD, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_FRAGMENT, grammarState.targetGrammar.getGrammarType());
        Assert.assertEquals(Grammar.DOCUMENT_STATE_COMPLETED, grammarState.phase);
      }
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(9,  eventTypeList.getLength());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A__", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("B", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventCode.ITEM_ED, eventType.itemType);
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_ELEMENT, grammarState.targetGrammar.getGrammarType());
        Assert.assertEquals(SchemaInformedGrammar.ELEMENT_STATE_UNBOUND, grammarState.phase);
      }

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
      Assert.assertEquals("true", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_CH, eventType.itemType);
      Assert.assertEquals(corpus.getBuiltinTypeOfSchema(EXISchemaConst.BOOLEAN_TYPE), 
          ((EventTypeSchema)eventType).getSchemaSubstance());
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(1, eventTypeList.getLength());
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_ELEMENT_CONTENT, grammarState.targetGrammar.getGrammarType());
        Assert.assertEquals(SchemaInformedGrammar.ELEMENT_STATE_CONTENT_COMPLETE, grammarState.phase);
      }
      
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(1, eventTypeList.getLength());
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_FRAGMENT, grammarState.targetGrammar.getGrammarType());
        Assert.assertEquals(Grammar.DOCUMENT_STATE_COMPLETED, grammarState.phase);
      }
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(9,  eventTypeList.getLength());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A__", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("B", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventCode.ITEM_ED, eventType.itemType);
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_ELEMENT, grammarState.targetGrammar.getGrammarType());
        Assert.assertEquals(SchemaInformedGrammar.ELEMENT_STATE_UNBOUND, grammarState.phase);
      }

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
      Assert.assertEquals("false", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_CH, eventType.itemType);
      Assert.assertEquals(corpus.getBuiltinTypeOfSchema(EXISchemaConst.BOOLEAN_TYPE), 
          ((EventTypeSchema)eventType).getSchemaSubstance());
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(1, eventTypeList.getLength());
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_ELEMENT_CONTENT, grammarState.targetGrammar.getGrammarType());
        Assert.assertEquals(SchemaInformedGrammar.ELEMENT_STATE_CONTENT_COMPLETE, grammarState.phase);
      }
      
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(1, eventTypeList.getLength());
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_FRAGMENT, grammarState.targetGrammar.getGrammarType());
        Assert.assertEquals(Grammar.DOCUMENT_STATE_COMPLETED, grammarState.phase);
      }
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(8, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(9,  eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A__", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("B", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_FRAGMENT, grammarState.targetGrammar.getGrammarType());
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

    final DocumentGrammarState documentState = new DocumentGrammarState();

    final Grammar documentGrammar = grammarCache.retrieveDocumentGrammar(true, documentState.eventTypesWorkSpace); 

    final Scribble scribble = new Scribble();

    final int booleanType = corpus.getBuiltinTypeOfSchema(EXISchemaConst.BOOLEAN_TYPE);

    for (AlignmentType alignment : Alignments) {
      final Scriber scriber = ScriberFactory.createScriber(alignment);
      scriber.setSchema(grammarCache.getEXISchema(), (QName[])null, 0);
      scriber.setPreserveNS(GrammarOptions.hasNS(grammarCache.grammarOptions));
      scriber.setStringTable(new StringTable(grammarCache.getEXISchema()));
      scriber.setValueMaxLength(EXIOptions.VALUE_MAX_LENGTH_UNBOUNDED);

      final ValueScriber booleanValueScriber = scriber.getValueScriber(booleanType);

      final ByteArrayOutputStream baos = new ByteArrayOutputStream();
      scriber.setOutputStream(baos);

      Scriber.writeHeaderPreamble(baos, false, false);
      
      documentGrammar.init(documentState);

      EventTypeList eventTypes;
      EventType eventType;

      eventTypes = documentState.getNextEventTypes();
      
      eventType = eventTypes.item(0);
      Assert.assertEquals(EventCode.ITEM_SD, eventType.itemType);
      Assert.assertEquals(1, eventTypes.getLength());
      scriber.writeEventType(eventType);
      documentState.startDocument();
      eventTypes = documentState.getNextEventTypes();
      Assert.assertEquals(9, eventTypes.getLength());

      eventType = eventTypes.item(0);
      Assert.assertEquals("A", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      scriber.writeEventType(eventType);
      documentState.startElement(0, "urn:goo", "A");
      eventTypes = documentState.getNextEventTypes();
      Assert.assertEquals(1, eventTypes.getLength());

      eventType = eventTypes.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_CH, eventType.itemType);
      scriber.writeEventType(eventType);
      booleanValueScriber.process("true", booleanType, corpus, scribble);
      booleanValueScriber.scribe("true", scribble, "A", "urn:goo", booleanType);
      documentState.characters();
      eventTypes = documentState.getNextEventTypes();
      Assert.assertEquals(1, eventTypes.getLength());
      
      eventType = eventTypes.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      scriber.writeEventType(eventType);
      documentState.endElement("urn:goo", "A");
      eventTypes = documentState.getNextEventTypes();
      Assert.assertEquals(9, eventTypes.getLength());

      eventType = eventTypes.item(0);
      Assert.assertEquals("A", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      scriber.writeEventType(eventType);
      documentState.startElement(0, "urn:goo", "A");
      eventTypes = documentState.getNextEventTypes();
      Assert.assertEquals(1, eventTypes.getLength());

      eventType = eventTypes.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_CH, eventType.itemType);
      scriber.writeEventType(eventType);
      booleanValueScriber.process("false", booleanType, corpus, scribble);
      booleanValueScriber.scribe("false", scribble, "A", "urn:goo", booleanType);
      documentState.characters();
      eventTypes = documentState.getNextEventTypes();
      Assert.assertEquals(1, eventTypes.getLength());
      
      eventType = eventTypes.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      scriber.writeEventType(eventType);
      documentState.endElement("urn:goo", "A");
      eventTypes = documentState.getNextEventTypes();
      Assert.assertEquals(9, eventTypes.getLength());
      
      eventType = eventTypes.item(8);
      Assert.assertEquals(EventCode.ITEM_ED, eventType.itemType);
      scriber.writeEventType(eventType);
      documentState.endDocument();
      eventTypes = documentState.getNextEventTypes();
      Assert.assertEquals(0, eventTypes.getLength());
      
      scriber.finish();
      
      
      EXIReader decoder = new EXIReader();

      decoder.setAlignmentType(alignment);
      decoder.setFragment(true);
      
      decoder.setEXISchema(grammarCache);

      ArrayList<Event> exiEventList = new ArrayList<Event>();
      SAXRecorder saxRecorder = new SAXRecorder(exiEventList, true);
      decoder.setContentHandler(saxRecorder);
      decoder.setLexicalHandler(saxRecorder);

      decoder.parse(new InputSource(new ByteArrayInputStream(baos.toByteArray())));

      Event saxEvent;
      int n = 0;
  
      saxEvent = exiEventList.get(n++);
      Assert.assertEquals(Event.NAMESPACE, saxEvent.type);
      Assert.assertEquals("urn:goo", saxEvent.namespace);
      Assert.assertEquals("p0", saxEvent.name);
      
      saxEvent = exiEventList.get(n++);
      Assert.assertEquals(Event.START_ELEMENT, saxEvent.type);
      Assert.assertEquals("urn:goo", saxEvent.namespace);
      Assert.assertEquals("A", saxEvent.localName);
      Assert.assertEquals("p0:A", saxEvent.name);

      saxEvent = exiEventList.get(n++);
      Assert.assertEquals(Event.CHARACTERS, saxEvent.type);
      Assert.assertEquals("true", new String(saxEvent.charValue));

      saxEvent = exiEventList.get(n++);
      Assert.assertEquals(Event.END_ELEMENT, saxEvent.type);
      Assert.assertEquals("urn:goo", saxEvent.namespace);
      Assert.assertEquals("A", saxEvent.localName);
      Assert.assertEquals("p0:A", saxEvent.name);

      saxEvent = exiEventList.get(n++);
      Assert.assertEquals(Event.END_NAMESPACE, saxEvent.type);
      Assert.assertEquals("p0", saxEvent.name);

      saxEvent = exiEventList.get(n++);
      Assert.assertEquals(Event.NAMESPACE, saxEvent.type);
      Assert.assertEquals("urn:goo", saxEvent.namespace);
      Assert.assertEquals("p0", saxEvent.name);
      
      saxEvent = exiEventList.get(n++);
      Assert.assertEquals(Event.START_ELEMENT, saxEvent.type);
      Assert.assertEquals("urn:goo", saxEvent.namespace);
      Assert.assertEquals("A", saxEvent.localName);
      Assert.assertEquals("p0:A", saxEvent.name);

      saxEvent = exiEventList.get(n++);
      Assert.assertEquals(Event.CHARACTERS, saxEvent.type);
      Assert.assertEquals("false", new String(saxEvent.charValue));

      saxEvent = exiEventList.get(n++);
      Assert.assertEquals(Event.END_ELEMENT, saxEvent.type);
      Assert.assertEquals("urn:goo", saxEvent.namespace);
      Assert.assertEquals("A", saxEvent.localName);
      Assert.assertEquals("p0:A", saxEvent.name);

      saxEvent = exiEventList.get(n++);
      Assert.assertEquals(Event.END_NAMESPACE, saxEvent.type);
      Assert.assertEquals("p0", saxEvent.name);
      
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
      
      encoder.setEXISchema(grammarCache);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);

      encoder.encode(new InputSource(new StringReader(xmlString)));
      
      bts = baos.toByteArray();
      
      decoder.setEXISchema(grammarCache);
      decoder.setInputStream(new ByteArrayInputStream(bts));
      scanner = decoder.processHeader();
      
      EXIEvent exiEvent;
      EventType eventType;
      EventTypeList eventTypeList;
      GrammarState grammarState;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EXIEvent.EVENT_SD, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(1, eventTypeList.getLength());
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.BUILTIN_GRAMMAR_FRAGMENT, grammarState.targetGrammar.getGrammarType());
        Assert.assertEquals(Grammar.DOCUMENT_STATE_COMPLETED, grammarState.phase);
      }
      
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      Assert.assertEquals(1, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(3,  eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_ED, eventType.itemType);
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.BUILTIN_GRAMMAR_ELEMENT, grammarState.targetGrammar.getGrammarType());
        Assert.assertEquals(BuiltinGrammar.ELEMENT_STATE_IN_TAG, grammarState.phase);
      }

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
      Assert.assertEquals("true", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      Assert.assertEquals(4, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(5, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.BUILTIN_GRAMMAR_ELEMENT, grammarState.targetGrammar.getGrammarType());
        Assert.assertEquals(BuiltinGrammar.ELEMENT_STATE_IN_CONTENT, grammarState.phase);
      }
      
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(3, eventTypeList.getLength());
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.BUILTIN_GRAMMAR_FRAGMENT, grammarState.targetGrammar.getGrammarType());
        Assert.assertEquals(Grammar.DOCUMENT_STATE_COMPLETED, grammarState.phase);
      }
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(2, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(3,  eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.BUILTIN_GRAMMAR_FRAGMENT, grammarState.targetGrammar.getGrammarType());
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
    
    final DocumentGrammarState documentState = new DocumentGrammarState();

    final Scribble scribble = new Scribble();
    
    final QName qname = new QName();

    for (AlignmentType alignment : Alignments) {
      final Scriber scriber = ScriberFactory.createScriber(alignment);
      scriber.setSchema(grammarCache.getEXISchema(), (QName[])null, 0);
      scriber.setPreserveNS(GrammarOptions.hasNS(grammarCache.grammarOptions));
      scriber.setStringTable(new StringTable(grammarCache.getEXISchema()));
      scriber.setValueMaxLength(EXIOptions.VALUE_MAX_LENGTH_UNBOUNDED);

      final ValueScriber stringValueScriber = scriber.getValueScriberByID(Apparatus.CODEC_STRING);

      final ByteArrayOutputStream baos = new ByteArrayOutputStream();
      scriber.setOutputStream(baos);

      Scriber.writeHeaderPreamble(baos, false, false);
      
      final Grammar documentGrammar;
      documentGrammar = grammarCache.retrieveDocumentGrammar(true, documentState.eventTypesWorkSpace); 
      documentGrammar.init(documentState);

      EventTypeList eventTypes;
      EventType eventType;

      eventTypes = documentState.getNextEventTypes();
      
      eventType = eventTypes.item(0);
      Assert.assertEquals(EventCode.ITEM_SD, eventType.itemType);
      Assert.assertEquals(1, eventTypes.getLength());
      scriber.writeEventType(eventType);
      documentState.startDocument();
      eventTypes = documentState.getNextEventTypes();
      Assert.assertEquals(2, eventTypes.getLength()); // SE(*), ED

      eventType = eventTypes.item(0);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      scriber.writeEventType(eventType);
      qname.setValue("urn:goo", "A", null);
      scriber.writeQName(qname, EventCode.ITEM_SE_WC);
      documentState.startUndeclaredElement("urn:goo", "A");
      eventTypes = documentState.getNextEventTypes();
      Assert.assertEquals(4, eventTypes.getLength()); // EE, AT(*), SE(*), CH

      eventType = eventTypes.item(3);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      scriber.writeEventType(eventType);
      stringValueScriber.scribe("true", scribble, "A", "urn:goo", EXISchema.NIL_NODE);
      documentState.undeclaredCharacters();
      eventTypes = documentState.getNextEventTypes();
      Assert.assertEquals(3, eventTypes.getLength()); // EE, SE(*), CH
      
      eventType = eventTypes.item(0);
      Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
      scriber.writeEventType(eventType);
      documentState.endElement("urn:goo", "A");
      eventTypes = documentState.getNextEventTypes();
      Assert.assertEquals(3, eventTypes.getLength()); // SE(goo:A), SE(*), ED

      eventType = eventTypes.item(0);
      Assert.assertEquals("A", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      scriber.writeEventType(eventType);
      documentState.startElement(0, "urn:goo", "A");
      eventTypes = documentState.getNextEventTypes();
      Assert.assertEquals(5, eventTypes.getLength());

      eventType = eventTypes.item(0);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      scriber.writeEventType(eventType);
      stringValueScriber.scribe("false", scribble, "A", "urn:goo", EXISchema.NIL_NODE);
      documentState.undeclaredCharacters();
      eventTypes = documentState.getNextEventTypes();
      Assert.assertEquals(3, eventTypes.getLength()); // EE, SE(*), CH
      
      eventType = eventTypes.item(0);
      Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
      scriber.writeEventType(eventType);
      documentState.endElement("urn:goo", "A");
      eventTypes = documentState.getNextEventTypes();
      Assert.assertEquals(3, eventTypes.getLength()); // SE(goo:A), SE(*), ED
      
      eventType = eventTypes.item(2);
      Assert.assertEquals(EventCode.ITEM_ED, eventType.itemType);
      scriber.writeEventType(eventType);
      documentState.endDocument();
      eventTypes = documentState.getNextEventTypes();
      Assert.assertEquals(0, eventTypes.getLength());
      
      scriber.finish();
      
      
      EXIDecoder decoder = new EXIDecoder();
      Scanner scanner;

      decoder.setAlignmentType(alignment);
      decoder.setFragment(true);
      
      decoder.setEXISchema(grammarCache);
      decoder.setInputStream(new ByteArrayInputStream(baos.toByteArray()));
      scanner = decoder.processHeader();
      
      EXIEvent exiEvent;
      EventTypeList eventTypeList;
      GrammarState grammarState;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EXIEvent.EVENT_SD, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.BUILTIN_GRAMMAR_FRAGMENT, grammarState.targetGrammar.getGrammarType());
        Assert.assertEquals(Grammar.DOCUMENT_STATE_COMPLETED, grammarState.phase);
      }
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      Assert.assertEquals("A", exiEvent.getName());
      Assert.assertEquals("urn:goo", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      Assert.assertEquals(1, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(3,  eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_ED, eventType.itemType);
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.BUILTIN_GRAMMAR_ELEMENT, grammarState.targetGrammar.getGrammarType());
        Assert.assertEquals(BuiltinGrammar.ELEMENT_STATE_IN_TAG, grammarState.phase); 
      }

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
      Assert.assertEquals("true", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      Assert.assertEquals(4, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(5, eventTypeList.getLength()); // CH, EE, AT(*), SE(*), CH
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.BUILTIN_GRAMMAR_ELEMENT, grammarState.targetGrammar.getGrammarType());
        Assert.assertEquals(BuiltinGrammar.ELEMENT_STATE_IN_CONTENT, grammarState.phase);
      }
      
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(3, eventTypeList.getLength()); // EE, SE(*), CH
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.BUILTIN_GRAMMAR_FRAGMENT, grammarState.targetGrammar.getGrammarType());
        Assert.assertEquals(Grammar.DOCUMENT_STATE_COMPLETED, grammarState.phase);
      }
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      Assert.assertEquals("A", exiEvent.getName());
      Assert.assertEquals("urn:goo", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(3,  eventTypeList.getLength()); // SE(goo:A), SE(*), ED
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_ED, eventType.itemType);
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.BUILTIN_GRAMMAR_ELEMENT, grammarState.targetGrammar.getGrammarType());
        Assert.assertEquals(BuiltinGrammar.ELEMENT_STATE_IN_TAG, grammarState.phase);
      }

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
      Assert.assertEquals("false", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(5, eventTypeList.getLength()); // CH, EE, AT(*), SE(*), CH
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.BUILTIN_GRAMMAR_ELEMENT, grammarState.targetGrammar.getGrammarType());
        Assert.assertEquals(BuiltinGrammar.ELEMENT_STATE_IN_CONTENT, grammarState.phase);
      }
      
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(3, eventTypeList.getLength()); // EE, SE(*), CH
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.BUILTIN_GRAMMAR_FRAGMENT, grammarState.targetGrammar.getGrammarType());
        Assert.assertEquals(Grammar.DOCUMENT_STATE_COMPLETED, grammarState.phase);
      }
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(2, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(3,  eventTypeList.getLength()); // SE(goo:A), SE(*), EE
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.BUILTIN_GRAMMAR_FRAGMENT, grammarState.targetGrammar.getGrammarType());
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

      encoder.setEXISchema(grammarCache);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);

      encoder.encode(new InputSource(new StringReader(xmlString)));
      
      bts = baos.toByteArray();
      
      decoder.setEXISchema(grammarCache);
      decoder.setInputStream(new ByteArrayInputStream(bts));
      scanner = decoder.processHeader();
      
      EXIEvent exiEvent;
  
      EventType eventType;
      EventTypeList eventTypeList;
      GrammarState grammarState;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EXIEvent.EVENT_SD, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_FRAGMENT, grammarState.targetGrammar.getGrammarType());
        Assert.assertEquals(Grammar.DOCUMENT_STATE_COMPLETED, grammarState.phase);
      }
      
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      Assert.assertEquals(5, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(9,  eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A__", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("B", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventCode.ITEM_ED, eventType.itemType);
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_ELEMENT_FRAGMENT, grammarState.targetGrammar.getGrammarType());
        Assert.assertEquals(SchemaInformedGrammar.ELEMENT_FRAGMENT_STATE_TAG, grammarState.phase);
      }

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
      Assert.assertEquals("xyz", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      Assert.assertEquals(15, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(16, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("a", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("b", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("c", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(9);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A__", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(10);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("B", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(11);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(12);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(13);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(14);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_ELEMENT_FRAGMENT, grammarState.targetGrammar.getGrammarType());
        Assert.assertEquals(SchemaInformedGrammar.ELEMENT_FRAGMENT_STATE_CONTENT, grammarState.phase);
      }

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      Assert.assertEquals(8, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(10, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A__", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("B", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(9);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_FRAGMENT, grammarState.targetGrammar.getGrammarType());
        Assert.assertEquals(Grammar.DOCUMENT_STATE_COMPLETED, grammarState.phase);
      }
      
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(8, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(9,  eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A__", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("B", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_FRAGMENT, grammarState.targetGrammar.getGrammarType());
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

      encoder.setEXISchema(grammarCache);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);

      encoder.encode(new InputSource(new StringReader(xmlString)));
      
      bts = baos.toByteArray();
      
      decoder.setEXISchema(grammarCache);
      decoder.setInputStream(new ByteArrayInputStream(bts));
      scanner = decoder.processHeader();
      
      EXIEvent exiEvent;
  
      EventType eventType;
      EventTypeList eventTypeList;
      GrammarState grammarState;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EXIEvent.EVENT_SD, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_FRAGMENT, grammarState.targetGrammar.getGrammarType());
        Assert.assertEquals(Grammar.DOCUMENT_STATE_COMPLETED, grammarState.phase);
      }
      
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      Assert.assertEquals(5, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(9,  eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A__", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("B", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventCode.ITEM_ED, eventType.itemType);
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_ELEMENT_FRAGMENT, grammarState.targetGrammar.getGrammarType());
        Assert.assertEquals(SchemaInformedGrammar.ELEMENT_FRAGMENT_STATE_TAG, grammarState.phase);
      }

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
      Assert.assertEquals("xyz", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      Assert.assertEquals(15, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(23, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("a", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("b", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("c", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(9);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A__", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(10);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("B", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(11);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(12);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(13);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(14);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      eventType = eventTypeList.item(16);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      eventType = eventTypeList.item(17);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_UNDECLARED_AT_INVALID_VALUE, eventType.itemType);
      Assert.assertEquals("a", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(18);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_UNDECLARED_AT_INVALID_VALUE, eventType.itemType);
      Assert.assertEquals("b", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(19);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_UNDECLARED_AT_INVALID_VALUE, eventType.itemType);
      Assert.assertEquals("c", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(20);
      Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
      eventType = eventTypeList.item(21);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(22);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_ELEMENT_FRAGMENT, grammarState.targetGrammar.getGrammarType());
        Assert.assertEquals(SchemaInformedGrammar.ELEMENT_FRAGMENT_STATE_CONTENT, grammarState.phase);
      }

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      Assert.assertEquals(8, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(12, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A__", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("B", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(9);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      eventType = eventTypeList.item(10);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(11);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_FRAGMENT, grammarState.targetGrammar.getGrammarType());
        Assert.assertEquals(Grammar.DOCUMENT_STATE_COMPLETED, grammarState.phase);
      }
      
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(8, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(9,  eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A__", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("B", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_FRAGMENT, grammarState.targetGrammar.getGrammarType());
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

      encoder.setEXISchema(grammarCache);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);

      encoder.encode(new InputSource(new StringReader(xmlString)));
      
      bts = baos.toByteArray();
      
      decoder.setEXISchema(grammarCache);
      decoder.setInputStream(new ByteArrayInputStream(bts));
      scanner = decoder.processHeader();
      
      EXIEvent exiEvent;
  
      EventType eventType;
      EventTypeList eventTypeList;
      GrammarState grammarState;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EXIEvent.EVENT_SD, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_FRAGMENT, grammarState.targetGrammar.getGrammarType());
        Assert.assertEquals(Grammar.DOCUMENT_STATE_COMPLETED, grammarState.phase);
      }
      
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      Assert.assertEquals(5, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(10,  eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A__", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("B", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventCode.ITEM_ED, eventType.itemType);
      eventType = eventTypeList.item(9);
      Assert.assertEquals(EventCode.ITEM_CM, eventType.itemType);
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_ELEMENT_FRAGMENT, grammarState.targetGrammar.getGrammarType());
        Assert.assertEquals(SchemaInformedGrammar.ELEMENT_FRAGMENT_STATE_TAG, grammarState.phase);
      }

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EXIEvent.EVENT_CM, exiEvent.getEventVariety());
      Assert.assertEquals(" Hello! ", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_CM, eventType.itemType);
      Assert.assertEquals(23, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(24, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("a", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("b", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("c", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(9);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A__", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(10);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("B", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(11);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(12);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(13);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(14);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      eventType = eventTypeList.item(15);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      eventType = eventTypeList.item(16);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      eventType = eventTypeList.item(17);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_UNDECLARED_AT_INVALID_VALUE, eventType.itemType);
      Assert.assertEquals("a", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(18);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_UNDECLARED_AT_INVALID_VALUE, eventType.itemType);
      Assert.assertEquals("b", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(19);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_UNDECLARED_AT_INVALID_VALUE, eventType.itemType);
      Assert.assertEquals("c", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(20);
      Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
      eventType = eventTypeList.item(21);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(22);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_ELEMENT_FRAGMENT, grammarState.targetGrammar.getGrammarType());
        Assert.assertEquals(SchemaInformedGrammar.ELEMENT_FRAGMENT_STATE_CONTENT, grammarState.phase);
      }

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
      Assert.assertEquals("xyz", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      Assert.assertEquals(9, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(13, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A__", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("B", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      eventType = eventTypeList.item(10);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(11);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
      eventType = eventTypeList.item(12);
      Assert.assertEquals(EventCode.ITEM_CM, eventType.itemType);
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_ELEMENT_FRAGMENT, grammarState.targetGrammar.getGrammarType());
        Assert.assertEquals(SchemaInformedGrammar.ELEMENT_FRAGMENT_STATE_CONTENT, grammarState.phase);
      }

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      Assert.assertEquals(8, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(13, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A__", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("B", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(9);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      eventType = eventTypeList.item(10);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(11);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
      eventType = eventTypeList.item(12);
      Assert.assertEquals(EventCode.ITEM_CM, eventType.itemType);
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_FRAGMENT, grammarState.targetGrammar.getGrammarType());
        Assert.assertEquals(Grammar.DOCUMENT_STATE_COMPLETED, grammarState.phase);
      }
      
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(8, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(10,  eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A__", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("B", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(9);
      Assert.assertEquals(EventCode.ITEM_CM, eventType.itemType);
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_FRAGMENT, grammarState.targetGrammar.getGrammarType());
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

      encoder.setEXISchema(grammarCache);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);

      encoder.encode(new InputSource(new StringReader(xmlString)));
      
      bts = baos.toByteArray();
      
      decoder.setEXISchema(grammarCache);
      decoder.setInputStream(new ByteArrayInputStream(bts));
      scanner = decoder.processHeader();
      
      EXIEvent exiEvent;
  
      EventType eventType;
      EventTypeList eventTypeList;
      GrammarState grammarState;
      int nd;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EXIEvent.EVENT_SD, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_FRAGMENT, grammarState.targetGrammar.getGrammarType());
        Assert.assertEquals(Grammar.DOCUMENT_STATE_COMPLETED, grammarState.phase);
      }
      
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      Assert.assertEquals(5, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(9,  eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A__", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("B", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventCode.ITEM_ED, eventType.itemType);
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_ELEMENT_FRAGMENT, grammarState.targetGrammar.getGrammarType());
        Assert.assertEquals(SchemaInformedGrammar.ELEMENT_FRAGMENT_STATE_TAG, grammarState.phase);
      }

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EXIEvent.EVENT_AT, exiEvent.getEventVariety());
      Assert.assertEquals("true", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("c", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      nd = ((EventTypeSchema)eventType).getSchemaSubstance();
      Assert.assertEquals(EXISchemaConst.BOOLEAN_TYPE, corpus.getSerialOfType(corpus.getTypeOfAttr(nd)));
      Assert.assertTrue(((EventTypeSchemaAttribute)eventType).useSpecificType());
      Assert.assertEquals(4, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(16, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("a", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("b", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(9);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A__", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(10);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("B", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(11);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(12);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(13);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(14);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      eventType = eventTypeList.item(15);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_ELEMENT_FRAGMENT, grammarState.targetGrammar.getGrammarType());
        Assert.assertEquals(SchemaInformedGrammar.ELEMENT_FRAGMENT_STATE_TAG, grammarState.phase);
      }

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
      Assert.assertEquals("xyz", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      Assert.assertEquals(15, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(16, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("a", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("b", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("c", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(9);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A__", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(10);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("B", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(11);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(12);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(13);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(14);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_ELEMENT_FRAGMENT, grammarState.targetGrammar.getGrammarType());
        Assert.assertEquals(SchemaInformedGrammar.ELEMENT_FRAGMENT_STATE_CONTENT, grammarState.phase);
      }

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      Assert.assertEquals(8, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(10, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A__", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("B", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(9);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_FRAGMENT, grammarState.targetGrammar.getGrammarType());
        Assert.assertEquals(Grammar.DOCUMENT_STATE_COMPLETED, grammarState.phase);
      }
      
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(8, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(9,  eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A__", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("B", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_FRAGMENT, grammarState.targetGrammar.getGrammarType());
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

      encoder.setEXISchema(grammarCache);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);

      encoder.encode(new InputSource(new StringReader(xmlString)));
      
      bts = baos.toByteArray();
      
      decoder.setEXISchema(grammarCache);
      decoder.setInputStream(new ByteArrayInputStream(bts));
      scanner = decoder.processHeader();
      
      EXIEvent exiEvent;
  
      EventType eventType;
      EventTypeList eventTypeList;
      GrammarState grammarState;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EXIEvent.EVENT_SD, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_FRAGMENT, grammarState.targetGrammar.getGrammarType());
        Assert.assertEquals(Grammar.DOCUMENT_STATE_COMPLETED, grammarState.phase);
      }
      
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      Assert.assertEquals(5, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(9,  eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A__", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("B", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventCode.ITEM_ED, eventType.itemType);
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_ELEMENT_FRAGMENT, grammarState.targetGrammar.getGrammarType());
        Assert.assertEquals(SchemaInformedGrammar.ELEMENT_FRAGMENT_STATE_TAG, grammarState.phase);
      }

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      Assert.assertEquals(11, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(16, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("a", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("b", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("c", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(9);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A__", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(10);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("B", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(12);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(13);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(14);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      eventType = eventTypeList.item(15);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_ELEMENT_FRAGMENT, grammarState.targetGrammar.getGrammarType());
        Assert.assertEquals(SchemaInformedGrammar.ELEMENT_FRAGMENT_STATE_TAG, grammarState.phase);
      }

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
      Assert.assertEquals("xyz", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      Assert.assertEquals(15, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(16, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("a", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("b", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("c", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(9);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A__", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(10);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("B", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(11);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(12);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(13);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(14);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_ELEMENT_FRAGMENT, grammarState.targetGrammar.getGrammarType());
        Assert.assertEquals(SchemaInformedGrammar.ELEMENT_FRAGMENT_STATE_CONTENT, grammarState.phase);
      }

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      Assert.assertEquals(8, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(10, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A__", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("B", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(9);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_ELEMENT_FRAGMENT, grammarState.targetGrammar.getGrammarType());
        Assert.assertEquals(SchemaInformedGrammar.ELEMENT_FRAGMENT_STATE_CONTENT, grammarState.phase);
      }

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      Assert.assertEquals(8, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(10, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A__", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("B", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(9);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_FRAGMENT, grammarState.targetGrammar.getGrammarType());
        Assert.assertEquals(Grammar.DOCUMENT_STATE_COMPLETED, grammarState.phase);
      }
      
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(8, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(9,  eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A__", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("B", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_FRAGMENT, grammarState.targetGrammar.getGrammarType());
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

      encoder.setEXISchema(grammarCache);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);

      encoder.encode(new InputSource(new StringReader(xmlString)));
      
      bts = baos.toByteArray();
      
      decoder.setEXISchema(grammarCache);
      decoder.setInputStream(new ByteArrayInputStream(bts));
      scanner = decoder.processHeader();
      
      EXIEvent exiEvent;
  
      EventType eventType;
      EventTypeList eventTypeList;
      GrammarState grammarState;
      int nd;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EXIEvent.EVENT_SD, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_FRAGMENT, grammarState.targetGrammar.getGrammarType());
        Assert.assertEquals(Grammar.DOCUMENT_STATE_COMPLETED, grammarState.phase);
      }
      
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      Assert.assertEquals(5, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(9,  eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A__", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("B", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventCode.ITEM_ED, eventType.itemType);
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_ELEMENT_FRAGMENT, grammarState.targetGrammar.getGrammarType());
        Assert.assertEquals(SchemaInformedGrammar.ELEMENT_FRAGMENT_STATE_TAG, grammarState.phase);
      }

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EXIEvent.EVENT_NL, exiEvent.getEventVariety());
      Assert.assertTrue(((EXIEventSchemaNil)exiEvent).isNilled());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
      Assert.assertEquals(1, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(16, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("a", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("b", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("c", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(9);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A__", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(10);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("B", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(11);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(12);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(13);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(14);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      eventType = eventTypeList.item(15);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_ELEMENT_FRAGMENT, grammarState.targetGrammar.getGrammarType());
        Assert.assertEquals(SchemaInformedGrammar.ELEMENT_FRAGMENT_EMPTY_STATE_TAG, grammarState.phase);
      }

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EXIEvent.EVENT_AT, exiEvent.getEventVariety());
      Assert.assertEquals("_3.1415926_", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("a", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      nd = corpus.getTypeOfAttr(((EventTypeSchema)eventType).getSchemaSubstance());
      Assert.assertEquals(EXISchemaConst.DECIMAL_TYPE, corpus.getAncestryIdOfSimpleType(nd));
      Assert.assertFalse(((EventTypeSchemaAttribute)eventType).useSpecificType());
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(5, eventTypeList.getLength());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("b", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("c", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_ELEMENT_FRAGMENT, grammarState.targetGrammar.getGrammarType());
        Assert.assertEquals(SchemaInformedGrammar.ELEMENT_FRAGMENT_EMPTY_STATE_TAG, grammarState.phase);
      }

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      Assert.assertEquals(4, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(5, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("a", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("b", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("c", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_FRAGMENT, grammarState.targetGrammar.getGrammarType());
        Assert.assertEquals(Grammar.DOCUMENT_STATE_COMPLETED, grammarState.phase);
      }
      
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(8, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(9,  eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A__", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("B", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_FRAGMENT, grammarState.targetGrammar.getGrammarType());
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

      encoder.setEXISchema(grammarCache);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);

      encoder.encode(new InputSource(new StringReader(xmlString)));
      
      bts = baos.toByteArray();
      
      decoder.setEXISchema(grammarCache);
      decoder.setInputStream(new ByteArrayInputStream(bts));
      scanner = decoder.processHeader();
      
      EXIEvent exiEvent;
  
      EventType eventType;
      EventTypeList eventTypeList;
      GrammarState grammarState;
      int nd;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EXIEvent.EVENT_SD, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_FRAGMENT, grammarState.targetGrammar.getGrammarType());
        Assert.assertEquals(Grammar.DOCUMENT_STATE_COMPLETED, grammarState.phase);
      }
      
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      Assert.assertEquals(5, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(9,  eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A__", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("B", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventCode.ITEM_ED, eventType.itemType);
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_ELEMENT_FRAGMENT, grammarState.targetGrammar.getGrammarType());
        Assert.assertEquals(SchemaInformedGrammar.ELEMENT_FRAGMENT_STATE_TAG, grammarState.phase);
      }

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EXIEvent.EVENT_NL, exiEvent.getEventVariety());
      Assert.assertTrue(((EXIEventSchemaNil)exiEvent).isNilled());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
      Assert.assertEquals(1, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(23, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("a", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("b", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("c", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(9);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A__", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(10);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("B", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(11);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(12);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(13);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(14);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      eventType = eventTypeList.item(15);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      eventType = eventTypeList.item(16);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      eventType = eventTypeList.item(17);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_UNDECLARED_AT_INVALID_VALUE, eventType.itemType);
      Assert.assertEquals("a", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(18);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_UNDECLARED_AT_INVALID_VALUE, eventType.itemType);
      Assert.assertEquals("b", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(19);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_UNDECLARED_AT_INVALID_VALUE, eventType.itemType);
      Assert.assertEquals("c", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(20);
      Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
      eventType = eventTypeList.item(21);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(22);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_ELEMENT_FRAGMENT, grammarState.targetGrammar.getGrammarType());
        Assert.assertEquals(SchemaInformedGrammar.ELEMENT_FRAGMENT_EMPTY_STATE_TAG, grammarState.phase);
      }

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EXIEvent.EVENT_AT, exiEvent.getEventVariety());
      Assert.assertEquals("_3.1415926_", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("a", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      nd = corpus.getTypeOfAttr(((EventTypeSchema)eventType).getSchemaSubstance());
      Assert.assertEquals(EXISchemaConst.DECIMAL_TYPE, corpus.getAncestryIdOfSimpleType(nd));
      Assert.assertFalse(((EventTypeSchemaAttribute)eventType).useSpecificType());
      Assert.assertEquals(2, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(14, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("b", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("c", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_UNDECLARED_AT_INVALID_VALUE, eventType.itemType);
      Assert.assertEquals("a", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(9);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_UNDECLARED_AT_INVALID_VALUE, eventType.itemType);
      Assert.assertEquals("b", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(10);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_UNDECLARED_AT_INVALID_VALUE, eventType.itemType);
      Assert.assertEquals("c", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(11);
      Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
      eventType = eventTypeList.item(12);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(13);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_ELEMENT_FRAGMENT, grammarState.targetGrammar.getGrammarType());
        Assert.assertEquals(SchemaInformedGrammar.ELEMENT_FRAGMENT_EMPTY_STATE_TAG, grammarState.phase);
      }

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
      Assert.assertEquals("xyz", exiEvent.getCharacters().makeString());
      Assert.assertEquals(13, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(14, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("a", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("b", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("c", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_UNDECLARED_AT_INVALID_VALUE, eventType.itemType);
      Assert.assertEquals("a", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(9);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_UNDECLARED_AT_INVALID_VALUE, eventType.itemType);
      Assert.assertEquals("b", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(10);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_UNDECLARED_AT_INVALID_VALUE, eventType.itemType);
      Assert.assertEquals("c", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(11);
      Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
      eventType = eventTypeList.item(12);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_ELEMENT_FRAGMENT, grammarState.targetGrammar.getGrammarType());
        Assert.assertEquals(SchemaInformedGrammar.ELEMENT_FRAGMENT_EMPTY_STATE_CONTENT, grammarState.phase);
      }

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(3, eventTypeList.getLength());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_FRAGMENT, grammarState.targetGrammar.getGrammarType());
        Assert.assertEquals(Grammar.DOCUMENT_STATE_COMPLETED, grammarState.phase);
      }
      
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(8, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(9,  eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A__", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("B", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_FRAGMENT, grammarState.targetGrammar.getGrammarType());
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

      encoder.setEXISchema(grammarCache);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);

      encoder.encode(new InputSource(new StringReader(xmlString)));
      
      bts = baos.toByteArray();
      
      decoder.setEXISchema(grammarCache);
      decoder.setInputStream(new ByteArrayInputStream(bts));
      scanner = decoder.processHeader();
      
      EXIEvent exiEvent;
  
      EventType eventType;
      EventTypeList eventTypeList;
      GrammarState grammarState;
      int nd;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EXIEvent.EVENT_SD, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_FRAGMENT, grammarState.targetGrammar.getGrammarType());
        Assert.assertEquals(Grammar.DOCUMENT_STATE_COMPLETED, grammarState.phase);
      }
      
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      Assert.assertEquals(5, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(9,  eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A__", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("B", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventCode.ITEM_ED, eventType.itemType);
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_ELEMENT_FRAGMENT, grammarState.targetGrammar.getGrammarType());
        Assert.assertEquals(SchemaInformedGrammar.ELEMENT_FRAGMENT_STATE_TAG, grammarState.phase);
      }

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EXIEvent.EVENT_TP, exiEvent.getEventVariety());
      nd = ((EXIEventSchemaType)exiEvent).getTp();
      Assert.assertEquals("ATYPE", corpus.getNameOfType(nd));
      Assert.assertEquals("urn:foo", corpus.getTargetNamespaceNameOfType(nd));
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(16, eventTypeList.getLength());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("a", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("b", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("c", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(9);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A__", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(10);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("B", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(11);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(12);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(13);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(14);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      eventType = eventTypeList.item(15);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_ELEMENT_TAG, grammarState.targetGrammar.getGrammarType());
        Assert.assertEquals(SchemaInformedGrammar.ELEMENT_STATE_TAG, grammarState.phase);
      }

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      Assert.assertEquals(2, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(4,  eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("b", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_ELEMENT, grammarState.targetGrammar.getGrammarType());
        Assert.assertEquals(SchemaInformedGrammar.ELEMENT_STATE_UNBOUND, grammarState.phase);
      }

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
      Assert.assertEquals("12345", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_CH, eventType.itemType);
      nd = ((EventTypeSchema)eventType).getSchemaSubstance();
      Assert.assertEquals(EXISchemaConst.INT_TYPE, corpus.getSerialOfType(nd));
      Assert.assertEquals(1, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(2,  eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_ELEMENT_CONTENT, grammarState.targetGrammar.getGrammarType());
        Assert.assertEquals(SchemaInformedGrammar.ELEMENT_STATE_CONTENT_COMPLETE, grammarState.phase);
      }

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(1, eventTypeList.getLength());
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_ELEMENT_CONTENT, grammarState.targetGrammar.getGrammarType());
        Assert.assertEquals(SchemaInformedGrammar.ELEMENT_STATE_CONTENT_COMPLETE, grammarState.phase);
      }

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(1, eventTypeList.getLength());
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_FRAGMENT, grammarState.targetGrammar.getGrammarType());
        Assert.assertEquals(Grammar.DOCUMENT_STATE_COMPLETED, grammarState.phase);
      }
      
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(8, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(9,  eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A__", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("B", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_FRAGMENT, grammarState.targetGrammar.getGrammarType());
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

      encoder.setEXISchema(grammarCache);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);

      encoder.encode(new InputSource(new StringReader(xmlString)));
      
      bts = baos.toByteArray();
      
      decoder.setEXISchema(grammarCache);
      decoder.setInputStream(new ByteArrayInputStream(bts));
      scanner = decoder.processHeader();
      
      EXIEvent exiEvent;
  
      EventType eventType;
      EventTypeList eventTypeList;
      GrammarState grammarState;
      int nd;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EXIEvent.EVENT_SD, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_FRAGMENT, grammarState.targetGrammar.getGrammarType());
        Assert.assertEquals(Grammar.DOCUMENT_STATE_COMPLETED, grammarState.phase);
      }
      
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      Assert.assertEquals(5, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(9,  eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A__", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("B", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventCode.ITEM_ED, eventType.itemType);
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_ELEMENT_FRAGMENT, grammarState.targetGrammar.getGrammarType());
        Assert.assertEquals(SchemaInformedGrammar.ELEMENT_FRAGMENT_STATE_TAG, grammarState.phase);
      }

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      Assert.assertEquals(6, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(16, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("a", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("b", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("c", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(9);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A__", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(10);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("B", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(11);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(12);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(13);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(14);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      eventType = eventTypeList.item(15);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_ELEMENT, grammarState.targetGrammar.getGrammarType());
        Assert.assertEquals(SchemaInformedGrammar.ELEMENT_STATE_UNBOUND, grammarState.phase);
      }
      
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
      Assert.assertEquals("true", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_CH, eventType.itemType);
      nd = ((EventTypeSchema)eventType).getSchemaSubstance();
      Assert.assertEquals(EXISchemaConst.BOOLEAN_TYPE, corpus.getSerialOfType(nd));
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(1,  eventTypeList.getLength());
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_ELEMENT_CONTENT, grammarState.targetGrammar.getGrammarType());
        Assert.assertEquals(SchemaInformedGrammar.ELEMENT_STATE_CONTENT_COMPLETE, grammarState.phase);
      }

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(1, eventTypeList.getLength());
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_ELEMENT_FRAGMENT, grammarState.targetGrammar.getGrammarType());
        Assert.assertEquals(SchemaInformedGrammar.ELEMENT_FRAGMENT_STATE_CONTENT, grammarState.phase);
      }

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      Assert.assertEquals(8, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(10, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A__", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("B", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(9);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_FRAGMENT, grammarState.targetGrammar.getGrammarType());
        Assert.assertEquals(Grammar.DOCUMENT_STATE_COMPLETED, grammarState.phase);
      }

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(8, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(9,  eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A_", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A__", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("B", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        grammarState = scanner.getGrammarState();
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_FRAGMENT, grammarState.targetGrammar.getGrammarType());
        Assert.assertEquals(SchemaInformedGrammar.DOCUMENT_STATE_END, grammarState.phase);
      }
      
      Assert.assertNull(scanner.nextEvent());
    }
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
    
    decoder.setEXISchema(grammarCache);
    decoder.setFragment(true);
    decoder.setInputStream(url.openStream());
    scanner = decoder.processHeader();
    
    EXIEvent exiEvent;
    EventType eventType;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EXIEvent.EVENT_SD, exiEvent.getEventVariety());
    eventType = exiEvent.getEventType();
    Assert.assertSame(exiEvent, eventType);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
    Assert.assertEquals("C", eventType.getName());
    Assert.assertEquals("urn:foo", eventType.getURI());
    
    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
    Assert.assertEquals("None", exiEvent.getName());
    Assert.assertEquals("", exiEvent.getURI());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
    
    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
    
    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
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
    
    decoder.setEXISchema(grammarCache);
    decoder.setFragment(true);
    decoder.setInputStream(url.openStream());
    scanner = decoder.processHeader();
    
    EXIEvent exiEvent;
    EventType eventType;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EXIEvent.EVENT_SD, exiEvent.getEventVariety());
    eventType = exiEvent.getEventType();
    Assert.assertSame(exiEvent, eventType);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
    Assert.assertEquals("C", eventType.getName());
    Assert.assertEquals("urn:foo", eventType.getURI());
    
    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EXIEvent.EVENT_NL, exiEvent.getEventVariety());
    Assert.assertTrue(((EXIEventSchemaNil)exiEvent).isNilled());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EXIEvent.EVENT_AT, exiEvent.getEventVariety());
    Assert.assertEquals("2010-04-28", exiEvent.getCharacters().makeString());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventCode.ITEM_SCHEMA_AT, eventType.itemType);
    Assert.assertEquals("b", eventType.getName());
    Assert.assertEquals("urn:aoo", eventType.getURI());

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EXIEvent.EVENT_AT, exiEvent.getEventVariety());
    Assert.assertEquals("12:34:58", exiEvent.getCharacters().makeString());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventCode.ITEM_SCHEMA_AT, eventType.itemType);
    Assert.assertEquals("d", eventType.getName());
    Assert.assertEquals("urn:foo", eventType.getURI());

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EXIEvent.EVENT_AT, exiEvent.getEventVariety());
    Assert.assertEquals("", exiEvent.getURI());
    Assert.assertEquals("none", exiEvent.getName());
    Assert.assertEquals("Welcome to the world of element fragment grammar!", exiEvent.getCharacters().makeString());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
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
    
    decoder.setEXISchema(grammarCache);
    decoder.setFragment(true);
    decoder.setInputStream(url.openStream());
    scanner = decoder.processHeader();
    
    EXIEvent exiEvent;
    EventType eventType;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EXIEvent.EVENT_SD, exiEvent.getEventVariety());
    eventType = exiEvent.getEventType();
    Assert.assertSame(exiEvent, eventType);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
    Assert.assertEquals("C", eventType.getName());
    Assert.assertEquals("urn:foo", eventType.getURI());
    
    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EXIEvent.EVENT_TP, exiEvent.getEventVariety());
    int nd = ((EXIEventSchemaType)exiEvent).getTp();
    Assert.assertEquals("tC", corpus.getNameOfType(nd));
    Assert.assertEquals("urn:foo", corpus.getTargetNamespaceNameOfType(nd));
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EXIEvent.EVENT_AT, exiEvent.getEventVariety());
    Assert.assertEquals("2010-08-18", exiEvent.getCharacters().makeString());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventCode.ITEM_SCHEMA_AT, eventType.itemType);
    Assert.assertEquals("c", eventType.getName());
    Assert.assertEquals("urn:foo", eventType.getURI());

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EXIEvent.EVENT_AT, exiEvent.getEventVariety());
    Assert.assertEquals("", exiEvent.getURI());
    Assert.assertEquals("none", exiEvent.getName());
    Assert.assertEquals("Welcome to the world of element fragment grammar!", exiEvent.getCharacters().makeString());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
    Assert.assertEquals("C", eventType.getName());
    Assert.assertEquals("urn:foo", eventType.getURI());
    
    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
    Assert.assertEquals("D", eventType.getName());
    Assert.assertEquals("urn:foo", eventType.getURI());
    
    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
    
    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
    eventType = exiEvent.getEventType();
    Assert.assertSame(exiEvent, eventType);
    
    Assert.assertNull(scanner.nextEvent());
  }
  
}
