package org.openexi.fujitsu.proc;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.openexi.fujitsu.proc.common.AlignmentType;
import org.openexi.fujitsu.proc.common.EXIEvent;
import org.openexi.fujitsu.proc.common.EventCode;
import org.openexi.fujitsu.proc.common.EventType;
import org.openexi.fujitsu.proc.common.EventTypeList;
import org.openexi.fujitsu.proc.common.GrammarOptions;
import org.openexi.fujitsu.proc.common.QName;
import org.openexi.fujitsu.proc.events.EXIEventNS;
import org.openexi.fujitsu.proc.events.EXIEventSchemaType;
import org.openexi.fujitsu.proc.grammars.DocumentGrammarState;
import org.openexi.fujitsu.proc.grammars.EventTypeSchema;
import org.openexi.fujitsu.proc.grammars.Grammar;
import org.openexi.fujitsu.proc.grammars.GrammarCache;
import org.openexi.fujitsu.proc.io.Scanner;
import org.openexi.fujitsu.proc.io.Scribble;
import org.openexi.fujitsu.proc.io.Scriber;
import org.openexi.fujitsu.proc.io.ScriberFactory;
import org.openexi.fujitsu.proc.io.StringTable;
import org.openexi.fujitsu.proc.io.ValueScriber;
import org.openexi.fujitsu.schema.EXISchema;
import org.openexi.fujitsu.scomp.EXISchemaFactoryErrorMonitor;
import org.openexi.fujitsu.scomp.EXISchemaFactoryTestUtil;

public class XsiTypeTest extends TestCase {

  public XsiTypeTest(String name) {
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
   * Typed representation of xsi:type. 
   */
  public void testXsiType_01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/boolean.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

    final DocumentGrammarState documentState = new DocumentGrammarState();

    final Grammar documentGrammar = grammarCache.retrieveDocumentGrammar(false, documentState.eventTypesWorkSpace); 

    final Scribble scribble = new Scribble();

    int foons = corpus.getNamespaceOfSchema("urn:foo");
    
    QName trueTypeQName = new QName("  foo:trueType   ", "urn:foo");
    Assert.assertEquals("trueType", trueTypeQName.localName);
    Assert.assertEquals("foo", trueTypeQName.prefix);
    Assert.assertEquals("  foo:trueType   ", trueTypeQName.qName);
    
    int trueType = corpus.getTypeOfNamespace(foons, trueTypeQName.localName);
    Assert.assertTrue(EXISchema.NIL_NODE != trueType);

    for (AlignmentType alignment : Alignments) {
      final Scriber scriber = ScriberFactory.createScriber(alignment);
      scriber.setSchema(grammarCache.getEXISchema(), (QName[])null, 0);
      scriber.setPreserveNS(GrammarOptions.hasNS(grammarCache.grammarOptions));
      scriber.setStringTable(new StringTable(grammarCache.getEXISchema()));

      final ValueScriber booleanValueScriber = scriber.getValueScriber(trueType);

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
      Assert.assertEquals(5, eventTypes.getLength());

      eventType = eventTypes.item(0);
      Assert.assertEquals("A", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      scriber.writeEventType(eventType);
      documentState.startElement(0, "urn:foo", "A");
      eventTypes = documentState.getNextEventTypes();
      Assert.assertEquals(2, eventTypes.getLength());

      eventType = eventTypes.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
      scriber.writeEventType(eventType);
      scriber.writeXsiTypeValue(trueTypeQName);
      documentState.xsitp(trueType);
      eventTypes = documentState.getNextEventTypes();
      Assert.assertEquals(1, eventTypes.getLength());

      eventType = eventTypes.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_CH, eventType.itemType);
      scriber.writeEventType(eventType);
      Assert.assertTrue(booleanValueScriber.process("true", trueType, corpus, scribble));
      booleanValueScriber.scribe("true", scribble, "A", "urn:foo", trueType);
      documentState.characters();
      eventTypes = documentState.getNextEventTypes();
      Assert.assertEquals(1, eventTypes.getLength());
      
      eventType = eventTypes.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      scriber.writeEventType(eventType);
      documentState.endElement("urn:foo", "A");
      eventTypes = documentState.getNextEventTypes();
      Assert.assertEquals(1, eventTypes.getLength());
      
      eventType = eventTypes.item(0);
      Assert.assertEquals(EventCode.ITEM_ED, eventType.itemType);
      scriber.writeEventType(eventType);
      documentState.endDocument();
      eventTypes = documentState.getNextEventTypes();
      Assert.assertEquals(0, eventTypes.getLength());
      
      scriber.finish();
      
      
      EXIDecoder decoder = new EXIDecoder();
      Scanner scanner;

      decoder.setAlignmentType(alignment);
      decoder.setFragment(false);
      
      decoder.setEXISchema(grammarCache);
      decoder.setInputStream(new ByteArrayInputStream(baos.toByteArray()));
      scanner = decoder.processHeader();
      
      EXIEvent exiEvent;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EXIEvent.EVENT_SD, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EXIEvent.EVENT_TP, exiEvent.getEventVariety());
      Assert.assertEquals("trueType", ((EXIEventSchemaType)exiEvent).getTypeName());
      Assert.assertEquals("urn:foo", ((EXIEventSchemaType)exiEvent).getTypeURI());

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
      Assert.assertEquals("true", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_CH, eventType.itemType);
      Assert.assertEquals(trueType, ((EventTypeSchema)eventType).getSchemaSubstance());
      Assert.assertEquals(0, eventType.getIndex());
      
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      Assert.assertEquals(0, eventType.getIndex());
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      
      Assert.assertNull(scanner.nextEvent());
    }
  }

  /**
   * Literal representation of xsi:type with lexical preservation option on. 
   */
  public void testXsiTypePreserveLexicalValue_01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/boolean.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.addNS(GrammarOptions.DEFAULT_OPTIONS));
    
    final DocumentGrammarState documentState = new DocumentGrammarState();
    
    final Grammar documentGrammar = grammarCache.retrieveDocumentGrammar(false, documentState.eventTypesWorkSpace); 

    final Scribble scribble = new Scribble();

    int foons = corpus.getNamespaceOfSchema("urn:foo");
    
    QName trueTypeQName = new QName("  foo:trueType   ", "urn:foo");
    Assert.assertEquals("trueType", trueTypeQName.localName);
    Assert.assertEquals("foo", trueTypeQName.prefix);
    Assert.assertEquals("  foo:trueType   ", trueTypeQName.qName);
    
    int trueType = corpus.getTypeOfNamespace(foons, trueTypeQName.localName);
    Assert.assertTrue(EXISchema.NIL_NODE != trueType);

    for (AlignmentType alignment : Alignments) {
      final Scriber scriber = ScriberFactory.createScriber(alignment);
      scriber.setSchema(grammarCache.getEXISchema(), (QName[])null, 0);
      scriber.setPreserveNS(GrammarOptions.hasNS(grammarCache.grammarOptions));
      scriber.setStringTable(new StringTable(grammarCache.getEXISchema()));
      scriber.setPreserveLexicalValues(true);

      final ValueScriber booleanValueScriber = scriber.getValueScriber(trueType);

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
      Assert.assertEquals(5, eventTypes.getLength());

      eventType = eventTypes.item(0);
      Assert.assertEquals("A", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      scriber.writeEventType(eventType);
      documentState.startElement(0, "urn:foo", "A");
      eventTypes = documentState.getNextEventTypes();

      eventType = eventTypes.getNamespaceDeclaration();
      scriber.writeEventType(eventType);
      scriber.writeNS("urn:foo", "foo", false);
      
      eventType = eventTypes.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
      scriber.writeEventType(eventType);
      scriber.writeXsiTypeValue(trueTypeQName);
      documentState.xsitp(trueType);
      eventTypes = documentState.getNextEventTypes();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventTypes.item(0).itemType);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventTypes.item(1).itemType);

      eventType = eventTypes.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_CH, eventType.itemType);
      scriber.writeEventType(eventType);
      Assert.assertTrue(booleanValueScriber.process("true", trueType, corpus, scribble));
      booleanValueScriber.scribe("true", scribble, "A", "urn:foo", trueType);
      documentState.characters();
      eventTypes = documentState.getNextEventTypes();
      
      eventType = eventTypes.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      scriber.writeEventType(eventType);
      documentState.endElement("urn:foo", "A");
      eventTypes = documentState.getNextEventTypes();
      Assert.assertEquals(1, eventTypes.getLength());
      
      eventType = eventTypes.item(0);
      Assert.assertEquals(EventCode.ITEM_ED, eventType.itemType);
      scriber.writeEventType(eventType);
      documentState.endDocument();
      eventTypes = documentState.getNextEventTypes();
      Assert.assertEquals(0, eventTypes.getLength());
      
      scriber.finish();
      
      
      EXIDecoder decoder = new EXIDecoder();
      Scanner scanner;

      decoder.setAlignmentType(alignment);
      decoder.setFragment(false);
      decoder.setPreserveLexicalValues(true);
      
      decoder.setEXISchema(grammarCache);
      decoder.setInputStream(new ByteArrayInputStream(baos.toByteArray()));
      scanner = decoder.processHeader();
      
      EXIEvent exiEvent;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EXIEvent.EVENT_SD, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EXIEvent.EVENT_NS, exiEvent.getEventVariety());
      Assert.assertEquals("foo", ((EXIEventNS)exiEvent).getPrefix());
      Assert.assertEquals("urn:foo", ((EXIEventNS)exiEvent).getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_NS, eventType.itemType);
      
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EXIEvent.EVENT_TP, exiEvent.getEventVariety());
      Assert.assertEquals("trueType", ((EXIEventSchemaType)exiEvent).getTypeName());
      Assert.assertEquals("foo", ((EXIEventSchemaType)exiEvent).getTypePrefix());
      Assert.assertEquals("urn:foo", ((EXIEventSchemaType)exiEvent).getTypeURI());
      Assert.assertEquals("  foo:trueType   ", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
      Assert.assertEquals("true", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_CH, eventType.itemType);
      Assert.assertEquals(trueType, ((EventTypeSchema)eventType).getSchemaSubstance());
      Assert.assertEquals(2, eventType.getIndex());
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.getEventTypeList().item(0).itemType);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.getEventTypeList().item(1).itemType);
      
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      Assert.assertEquals(0, eventType.getIndex());
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      
      Assert.assertNull(scanner.nextEvent());
    }
  }

}
