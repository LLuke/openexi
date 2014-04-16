package org.openexi.proc;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.openexi.proc.common.AlignmentType;
import org.openexi.proc.common.EventDescription;
import org.openexi.proc.common.EventType;
import org.openexi.proc.common.EventTypeList;
import org.openexi.proc.common.GrammarOptions;
import org.openexi.proc.common.QName;
import org.openexi.proc.common.StringTable;
import org.openexi.proc.events.EXIEventNS;
import org.openexi.proc.events.EXIEventSchemaType;
import org.openexi.proc.grammars.Grammar;
import org.openexi.proc.grammars.GrammarCache;
import org.openexi.proc.io.Scanner;
import org.openexi.proc.io.Scribble;
import org.openexi.proc.io.Scriber;
import org.openexi.proc.io.ScriberFactory;
import org.openexi.proc.io.ValueScriber;
import org.openexi.schema.EXISchema;
import org.openexi.scomp.EXISchemaFactoryErrorMonitor;
import org.openexi.scomp.EXISchemaFactoryTestUtil;

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

    final Scribble scribble = new Scribble();

    QName trueTypeQName = new QName("  foo:trueType   ", "urn:foo");
    Assert.assertEquals("trueType", trueTypeQName.localName);
    Assert.assertEquals("foo", trueTypeQName.prefix);
    Assert.assertEquals("  foo:trueType   ", trueTypeQName.qName);
    
    int trueType = corpus.getTypeOfSchema("urn:foo", trueTypeQName.localName);
    Assert.assertTrue(EXISchema.NIL_NODE != trueType);

    for (AlignmentType alignment : Alignments) {
      final Scriber scriber = ScriberFactory.createScriber(alignment);
      scriber.setSchema(grammarCache.getEXISchema(), (QName[])null, 0);
      scriber.setPreserveNS(GrammarOptions.hasNS(grammarCache.grammarOptions));
      StringTable stringTable = Scriber.createStringTable(new GrammarCache(grammarCache.getEXISchema())); 
      scriber.setStringTable(stringTable);

      final ValueScriber booleanValueScriber = scriber.getValueScriber(trueType);

      final ByteArrayOutputStream baos = new ByteArrayOutputStream();
      scriber.setOutputStream(baos);

      Scriber.writeHeaderPreamble(baos, false, false);
      
      final Grammar documentGrammar = grammarCache.retrieveRootGrammar(false, scriber.eventTypesWorkSpace); 
      documentGrammar.init(scriber.currentState);

      EventTypeList eventTypes;
      EventType eventType;
      QName qname = new QName();
      
      final int fooId = stringTable.getCompactIdOfURI("urn:foo");

      eventTypes = scriber.getNextEventTypes();
      
      eventType = eventTypes.item(0);
      Assert.assertEquals(EventType.ITEM_SD, eventType.itemType);
      Assert.assertEquals(1, eventTypes.getLength());
      scriber.writeEventType(eventType);
      scriber.startDocument();
      eventTypes = scriber.getNextEventTypes();
      Assert.assertEquals(5, eventTypes.getLength());

      eventType = eventTypes.item(0);
      Assert.assertEquals("A", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      scriber.writeEventType(eventType);
      scriber.writeQName(qname.setValue("urn:foo", "A", null), eventType);
      final int localNameId_A = qname.localNameId;
      scriber.startElement(eventType);
      eventTypes = scriber.getNextEventTypes();
      Assert.assertEquals(2, eventTypes.getLength());

      eventType = eventTypes.item(0);
      Assert.assertEquals(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
      scriber.writeEventType(eventType);
      scriber.writeXsiTypeValue(trueTypeQName);
      scriber.xsitp(trueType);
      eventTypes = scriber.getNextEventTypes();
      Assert.assertEquals(1, eventTypes.getLength());

      eventType = eventTypes.item(0);
      Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
      scriber.writeEventType(eventType);
      Assert.assertTrue(booleanValueScriber.process("true", trueType, corpus, scribble, scriber));
      booleanValueScriber.scribe("true", scribble, localNameId_A, fooId, trueType, scriber);
      scriber.characters(eventType);
      eventTypes = scriber.getNextEventTypes();
      Assert.assertEquals(1, eventTypes.getLength());
      
      eventType = eventTypes.item(0);
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      scriber.writeEventType(eventType);
      scriber.endElement();
      eventTypes = scriber.getNextEventTypes();
      Assert.assertEquals(1, eventTypes.getLength());
      
      eventType = eventTypes.item(0);
      Assert.assertEquals(EventType.ITEM_ED, eventType.itemType);
      scriber.writeEventType(eventType);
      scriber.endDocument();
      eventTypes = scriber.getNextEventTypes();
      Assert.assertEquals(0, eventTypes.getLength());
      
      scriber.finish();
      
      
      EXIDecoder decoder = new EXIDecoder();
      Scanner scanner;

      decoder.setAlignmentType(alignment);
      decoder.setFragment(false);
      
      decoder.setGrammarCache(grammarCache);
      decoder.setInputStream(new ByteArrayInputStream(baos.toByteArray()));
      scanner = decoder.processHeader();
      
      EventDescription exiEvent;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_TP, exiEvent.getEventKind());
      Assert.assertEquals("trueType", ((EXIEventSchemaType)exiEvent).getTypeName());
      Assert.assertEquals("urn:foo", ((EXIEventSchemaType)exiEvent).getTypeURI());

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
      Assert.assertEquals("true", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        int tp = scanner.currentState.contentDatatype; 
        Assert.assertEquals(trueType, tp);
      }
      Assert.assertEquals(0, eventType.getIndex());
      
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      Assert.assertEquals(0, eventType.getIndex());
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
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
    
    final Scribble scribble = new Scribble();

    QName trueTypeQName = new QName("  foo:trueType   ", "urn:foo");
    Assert.assertEquals("trueType", trueTypeQName.localName);
    Assert.assertEquals("foo", trueTypeQName.prefix);
    Assert.assertEquals("  foo:trueType   ", trueTypeQName.qName);
    
    int trueType = corpus.getTypeOfSchema("urn:foo", trueTypeQName.localName);
    Assert.assertTrue(EXISchema.NIL_NODE != trueType);

    for (AlignmentType alignment : Alignments) {
      final Scriber scriber = ScriberFactory.createScriber(alignment);
      scriber.setSchema(grammarCache.getEXISchema(), (QName[])null, 0);
      scriber.setPreserveNS(GrammarOptions.hasNS(grammarCache.grammarOptions));
      StringTable stringTable = Scriber.createStringTable(new GrammarCache(grammarCache.getEXISchema())); 
      scriber.setStringTable(stringTable);
      scriber.setPreserveLexicalValues(true);

      final ValueScriber booleanValueScriber = scriber.getValueScriber(trueType);

      final ByteArrayOutputStream baos = new ByteArrayOutputStream();
      scriber.setOutputStream(baos);

      Scriber.writeHeaderPreamble(baos, false, false);
      
      final Grammar documentGrammar = grammarCache.retrieveRootGrammar(false, scriber.eventTypesWorkSpace); 
      documentGrammar.init(scriber.currentState);

      EventTypeList eventTypes;
      EventType eventType;
      QName qname = new QName();
      
      final int fooId = stringTable.getCompactIdOfURI("urn:foo");

      eventTypes = scriber.getNextEventTypes();
      
      eventType = eventTypes.item(0);
      Assert.assertEquals(EventType.ITEM_SD, eventType.itemType);
      Assert.assertEquals(1, eventTypes.getLength());
      scriber.writeEventType(eventType);
      scriber.startDocument();
      eventTypes = scriber.getNextEventTypes();
      Assert.assertEquals(5, eventTypes.getLength());

      eventType = eventTypes.item(0);
      Assert.assertEquals("A", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      scriber.writeEventType(eventType);
      scriber.writeQName(qname.setValue("urn:foo", "A", ""), eventType);
      final int localNameId_A = qname.localNameId;
      scriber.startElement(eventType);
      eventTypes = scriber.getNextEventTypes();

      eventType = eventTypes.getNamespaceDeclaration();
      scriber.writeEventType(eventType);
      scriber.writeNS("urn:foo", "foo", false);
      
      eventType = eventTypes.item(0);
      Assert.assertEquals(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
      scriber.writeEventType(eventType);
      scriber.writeXsiTypeValue(trueTypeQName);
      scriber.xsitp(trueType);
      eventTypes = scriber.getNextEventTypes();
      Assert.assertEquals(EventType.ITEM_SCHEMA_TYPE, eventTypes.item(0).itemType);
      Assert.assertEquals(EventType.ITEM_SCHEMA_NIL, eventTypes.item(1).itemType);

      eventType = eventTypes.item(2);
      Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
      scriber.writeEventType(eventType);
      Assert.assertTrue(booleanValueScriber.process("true", trueType, corpus, scribble, scriber));
      booleanValueScriber.scribe("true", scribble, localNameId_A, fooId, trueType, scriber);
      scriber.characters(eventType);
      eventTypes = scriber.getNextEventTypes();
      
      eventType = eventTypes.item(0);
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      scriber.writeEventType(eventType);
      scriber.endElement();
      eventTypes = scriber.getNextEventTypes();
      Assert.assertEquals(1, eventTypes.getLength());
      
      eventType = eventTypes.item(0);
      Assert.assertEquals(EventType.ITEM_ED, eventType.itemType);
      scriber.writeEventType(eventType);
      scriber.endDocument();
      eventTypes = scriber.getNextEventTypes();
      Assert.assertEquals(0, eventTypes.getLength());
      
      scriber.finish();
      
      
      EXIDecoder decoder = new EXIDecoder();
      Scanner scanner;

      decoder.setAlignmentType(alignment);
      decoder.setFragment(false);
      decoder.setPreserveLexicalValues(true);
      
      decoder.setGrammarCache(grammarCache);
      decoder.setInputStream(new ByteArrayInputStream(baos.toByteArray()));
      scanner = decoder.processHeader();
      
      EventDescription exiEvent;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_NS, exiEvent.getEventKind());
      Assert.assertEquals("foo", ((EXIEventNS)exiEvent).getPrefix());
      Assert.assertEquals("urn:foo", ((EXIEventNS)exiEvent).getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_NS, eventType.itemType);
      
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_TP, exiEvent.getEventKind());
      Assert.assertEquals("trueType", ((EXIEventSchemaType)exiEvent).getTypeName());
      Assert.assertEquals("foo", ((EXIEventSchemaType)exiEvent).getTypePrefix());
      Assert.assertEquals("urn:foo", ((EXIEventSchemaType)exiEvent).getTypeURI());
      Assert.assertEquals("  foo:trueType   ", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
      Assert.assertEquals("true", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        int tp = scanner.currentState.contentDatatype; 
        Assert.assertEquals(trueType, tp);
      }
      Assert.assertEquals(2, eventType.getIndex());
      Assert.assertEquals(EventType.ITEM_SCHEMA_TYPE, eventType.getEventTypeList().item(0).itemType); 
      Assert.assertEquals(EventType.ITEM_SCHEMA_NIL, eventType.getEventTypeList().item(1).itemType);
      
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      Assert.assertEquals(0, eventType.getIndex());
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      
      Assert.assertNull(scanner.nextEvent());
    }
  }

}
