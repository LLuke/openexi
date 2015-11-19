package org.openexi.proc;

import java.net.URL;
import java.util.ArrayList;

import org.openexi.proc.common.AlignmentType;
import org.openexi.proc.common.EventDescription;
import org.openexi.proc.common.EventType;
import org.openexi.proc.common.EventTypeList;
import org.openexi.proc.common.GrammarOptions;
import org.openexi.proc.grammars.GrammarCache;
import org.openexi.proc.io.Scanner;
import org.openexi.proc.io.compression.ChannellingScanner;
import org.openexi.proc.util.ExiUriConst;
import org.openexi.schema.EXISchema;
import org.openexi.schema.TestBase;
import org.openexi.scomp.EXISchemaFactoryErrorMonitor;
import org.openexi.scomp.EXISchemaFactoryTestUtil;

import junit.framework.Assert;

public class DecodeStrictTest extends TestBase {

  public DecodeStrictTest(String name) {
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
   * Decode EXI-encoded NLM data.
   */
  public void testNLM_strict_01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/NLM/nlmcatalogrecord_060101.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);
    
    String[] exiFiles = { 
        "/NLM/catplussamp2006.bitPacked", 
        "/NLM/catplussamp2006.byteAligned", 
        "/NLM/catplussamp2006.preCompress", 
        "/NLM/catplussamp2006.compress" };

    for (int i = 0; i < Alignments.length; i++) {
      AlignmentType alignment = Alignments[i];
      EXIDecoder decoder = new EXIDecoder();
      Scanner scanner;
      
      decoder.setAlignmentType(alignment);
  
      URL url = resolveSystemIdAsURL(exiFiles[i]);
  
      int n_events;
      
      decoder.setGrammarCache(grammarCache);
      decoder.setInputStream(url.openStream());
      scanner = decoder.processHeader();
      
      ArrayList<EventDescription> exiEventList = new ArrayList<EventDescription>();
      
      EventDescription exiEvent;
      n_events = 0;
      while ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        exiEventList.add(exiEvent);
      }
      Assert.assertEquals(35176, n_events);
      
      // Check the last value in the last value channel
      exiEvent = exiEventList.get(33009);
      Assert.assertEquals("Interdisciplinary Studies", exiEvent.getCharacters().makeString());
    }
  }
  
  /**
   * Only a handful of values in a stream.
   */
  public void testSequence_01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/interop/schemaInformedGrammar/acceptance.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

    String[] exiFiles = { 
        "/interop/schemaInformedGrammar/declaredProductions/sequence-01.bitPacked", 
        "/interop/schemaInformedGrammar/declaredProductions/sequence-01.byteAligned", 
        "/interop/schemaInformedGrammar/declaredProductions/sequence-01.preCompress", 
        "/interop/schemaInformedGrammar/declaredProductions/sequence-01.compress" };

    for (int i = 0; i < Alignments.length; i++) {
      AlignmentType alignment = Alignments[i];
      EXIDecoder decoder = new EXIDecoder();
      Scanner scanner;
      
      decoder.setAlignmentType(alignment);
  
      URL url = resolveSystemIdAsURL(exiFiles[i]);
  
      int n_events;
      
      decoder.setGrammarCache(grammarCache);
      decoder.setInputStream(url.openStream());
      scanner = decoder.processHeader();
      
      EventDescription exiEvent;
      n_events = 0;
      
      EventType eventType;
      EventTypeList eventTypeList;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(1, eventTypeList.getLength());
      ++n_events;
      
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
      Assert.assertEquals("A", exiEvent.getName());
      Assert.assertEquals("urn:foo", exiEvent.getURI());
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
      Assert.assertEquals("AB", exiEvent.getName());
      Assert.assertEquals("urn:foo", exiEvent.getURI());
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
      Assert.assertEquals("", exiEvent.getCharacters().makeString());
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
      Assert.assertEquals("AC", exiEvent.getName());
      Assert.assertEquals("urn:foo", exiEvent.getURI());
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
      Assert.assertEquals("", exiEvent.getCharacters().makeString());
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
      Assert.assertEquals("AC", exiEvent.getName());
      Assert.assertEquals("urn:foo", exiEvent.getURI());
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
      Assert.assertEquals("", exiEvent.getCharacters().makeString());
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
      ++n_events;
      
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
      Assert.assertEquals("AD", exiEvent.getName());
      Assert.assertEquals("urn:foo", exiEvent.getURI());
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
      Assert.assertEquals("", exiEvent.getCharacters().makeString());
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
      ++n_events;
      
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
      Assert.assertEquals("AE", exiEvent.getName());
      Assert.assertEquals("urn:foo", exiEvent.getURI());
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
      Assert.assertEquals("", exiEvent.getCharacters().makeString());
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
      ++n_events;
      
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
      ++n_events;
      
      Assert.assertEquals(19, n_events);
    }
  }

  /**
   */
  public void testHeaderOptionsAlignment_01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/optionsSchema.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    /**
     * Use DEFAULT_OPTIONS to confuse the decoder. The streams all have been
     * encoded with STRICT_OPTIONS.
     */
    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);

    String[] exiFiles = { 
        "/encoding/headerOptions-01.bitPacked",
        "/encoding/headerOptions-01.byteAligned", 
        "/encoding/headerOptions-01.preCompress", 
        "/encoding/headerOptions-01.compress", 
    };

    for (int i = 0; i < Alignments.length; i++) {
      EXIDecoder decoder = new EXIDecoder();
      Scanner scanner;
      
      URL url = resolveSystemIdAsURL(exiFiles[i]);
  
      int n_events;
      
      final AlignmentType falseAlignmentType;
      falseAlignmentType = Alignments[i] == AlignmentType.compress ?  
          AlignmentType.bitPacked : AlignmentType.compress;
      decoder.setAlignmentType(falseAlignmentType); // trying to confuse decoder.
      decoder.setGrammarCache(grammarCache);
      decoder.setInputStream(url.openStream());
      scanner = decoder.processHeader();
      Assert.assertEquals(Alignments[i], scanner.getAlignmentType());
      
      ArrayList<EventDescription> exiEventList = new ArrayList<EventDescription>();
      
      EventDescription exiEvent;
      n_events = 0;
      while ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        exiEventList.add(exiEvent);
      }

      Assert.assertEquals(6, n_events);
      
      EventType eventType;
  
      exiEvent = exiEventList.get(0);
      Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
      
      exiEvent = exiEventList.get(1);
      Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
      Assert.assertEquals("header", exiEvent.getName());
      Assert.assertEquals(ExiUriConst.W3C_2009_EXI_URI, exiEvent.getURI());
      
      exiEvent = exiEventList.get(2);
      Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
      Assert.assertEquals("strict", exiEvent.getName());
      Assert.assertEquals(ExiUriConst.W3C_2009_EXI_URI, exiEvent.getURI());
      
      exiEvent = exiEventList.get(3);
      Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
  
      exiEvent = exiEventList.get(4);
      Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
  
      exiEvent = exiEventList.get(5);
      Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
    }
  }

  /**
   */
  public void testEmptyBlock_01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/compression/emptyBlock_01.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

    EXIDecoder decoder = new EXIDecoder();
    Scanner scanner;
    
    decoder.setAlignmentType(AlignmentType.compress);
    decoder.setBlockSize(1);

    URL url = resolveSystemIdAsURL("/compression/emptyBlock_01.compress");

    int n_events;
    
    decoder.setGrammarCache(grammarCache);
    decoder.setInputStream(url.openStream());
    scanner = decoder.processHeader();
    
    ArrayList<EventDescription> exiEventList = new ArrayList<EventDescription>();
    
    EventDescription exiEvent;
    n_events = 0;
    while ((exiEvent = scanner.nextEvent()) != null) {
      ++n_events;
      exiEventList.add(exiEvent);
    }
    
    Assert.assertEquals(11, n_events);
    Assert.assertEquals(1, ((ChannellingScanner)scanner).getBlockCount());

    EventType eventType;
    EventTypeList eventTypeList;

    int pos = 0;

    exiEvent = exiEventList.get(pos++);
    Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertSame(exiEvent, eventType);
    Assert.assertEquals(0, eventType.getIndex());
    eventTypeList = eventType.getEventTypeList();
    Assert.assertNull(eventTypeList.getEE());
    
    exiEvent = exiEventList.get(pos++);
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    Assert.assertEquals("root", exiEvent.getName());
    Assert.assertEquals("", exiEvent.getURI());

    exiEvent = exiEventList.get(pos++);
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    Assert.assertEquals("parent", exiEvent.getName());
    Assert.assertEquals("", exiEvent.getURI());

    exiEvent = exiEventList.get(pos++);
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    Assert.assertEquals("child", exiEvent.getName());
    Assert.assertEquals("", exiEvent.getURI());

    exiEvent = exiEventList.get(pos++);
    Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
    Assert.assertEquals("42", exiEvent.getCharacters().makeString());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);

    exiEvent = exiEventList.get(pos++);
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());

    exiEvent = exiEventList.get(pos++);
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());

    exiEvent = exiEventList.get(pos++);
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    Assert.assertEquals("adjunct", exiEvent.getName());
    Assert.assertEquals("", exiEvent.getURI());

    exiEvent = exiEventList.get(pos++);
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());

    exiEvent = exiEventList.get(pos++);
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    
    exiEvent = exiEventList.get(pos++);
    Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
  }

  /**
   * Enumeration of union
   */
  public void testEnmueration_04() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/interop/datatypes/enumeration/enumeration.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

    String exiFiles = "/interop/datatypes/enumeration/enumeration-valid-04.byteAligned"; 

    EXIDecoder decoder = new EXIDecoder();
    Scanner scanner;
    
    decoder.setAlignmentType(AlignmentType.byteAligned);

    URL url = resolveSystemIdAsURL(exiFiles);

    int n_events;
    
    decoder.setGrammarCache(grammarCache);
    decoder.setInputStream(url.openStream());
    scanner = decoder.processHeader();
    
    EventDescription exiEvent;
    n_events = 0;
    
    EventType eventType;
    EventTypeList eventTypeList;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertSame(exiEvent, eventType);
    Assert.assertEquals(0, eventType.getIndex());
    eventTypeList = eventType.getEventTypeList();
    Assert.assertEquals(1, eventTypeList.getLength());
    ++n_events;
    
    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    Assert.assertEquals("root", exiEvent.getName());
    Assert.assertEquals("", exiEvent.getURI());
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    Assert.assertEquals("union", exiEvent.getName());
    Assert.assertEquals("", exiEvent.getURI());
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
    Assert.assertEquals("+10", exiEvent.getCharacters().makeString());
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    Assert.assertEquals("union", exiEvent.getName());
    Assert.assertEquals("", exiEvent.getURI());
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
    Assert.assertEquals("12:32:00", exiEvent.getCharacters().makeString());
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
    ++n_events;
    
    Assert.assertEquals(10, n_events);
  }
  
  /**
   * Decode FixML EXI documents.
   */
  public void testFixML_01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/FixML-4.4/schema/fixml-main-4-4.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

    AlignmentType alignment = AlignmentType.bitPacked;
    EXIDecoder decoder = new EXIDecoder();
    Scanner scanner;
    
    decoder.setAlignmentType(alignment);

    URL url = resolveSystemIdAsURL("/FixML-4.4/AllocationInstructionAck.exi_vi_openexi.bitPacked");

    decoder.setGrammarCache(grammarCache);
    decoder.setInputStream(url.openStream());
    scanner = decoder.processHeader();
    
    EventDescription exiEvent;
    
    EventType eventType;
    EventTypeList eventTypeList;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertSame(exiEvent, eventType);
    Assert.assertEquals(0, eventType.getIndex());
    eventTypeList = eventType.getEventTypeList();
    Assert.assertEquals(1, eventTypeList.getLength());
    
    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    Assert.assertEquals("FIXML", exiEvent.getName());
    Assert.assertEquals("http://www.fixprotocol.org/FIXML-4-4", exiEvent.getURI());

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    Assert.assertEquals("AllocInstrctnAck", exiEvent.getName());
    Assert.assertEquals("http://www.fixprotocol.org/FIXML-4-4", exiEvent.getURI());

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_AT, exiEvent.getEventKind());
    Assert.assertEquals("ID", exiEvent.getName());
    Assert.assertEquals("", exiEvent.getURI());
    Assert.assertEquals("20012358", exiEvent.getCharacters().makeString());

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_AT, exiEvent.getEventKind());
    Assert.assertEquals("Stat", exiEvent.getName());
    Assert.assertEquals("", exiEvent.getURI());
    Assert.assertEquals("3", exiEvent.getCharacters().makeString());

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_AT, exiEvent.getEventKind());
    Assert.assertEquals("TrdDt", exiEvent.getName());
    Assert.assertEquals("", exiEvent.getURI());
    Assert.assertEquals("2003-10-30", exiEvent.getCharacters().makeString());

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_AT, exiEvent.getEventKind());
    Assert.assertEquals("TxnTm", exiEvent.getName());
    Assert.assertEquals("", exiEvent.getURI());
    Assert.assertEquals("2003-10-30T16:46:17", exiEvent.getCharacters().makeString());

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    Assert.assertEquals("Hdr", exiEvent.getName());
    Assert.assertEquals("http://www.fixprotocol.org/FIXML-4-4", exiEvent.getURI());

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_AT, exiEvent.getEventKind());
    Assert.assertEquals("SID", exiEvent.getName());
    Assert.assertEquals("", exiEvent.getURI());
    Assert.assertEquals("BONDBROKER", exiEvent.getCharacters().makeString());

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_AT, exiEvent.getEventKind());
    Assert.assertEquals("SeqNum", exiEvent.getName());
    Assert.assertEquals("", exiEvent.getURI());
    Assert.assertEquals("307", exiEvent.getCharacters().makeString());

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_AT, exiEvent.getEventKind());
    Assert.assertEquals("Snt", exiEvent.getName());
    Assert.assertEquals("", exiEvent.getURI());
    Assert.assertEquals("2003-10-30T16:46:18", exiEvent.getCharacters().makeString());

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_AT, exiEvent.getEventKind());
    Assert.assertEquals("TID", exiEvent.getName());
    Assert.assertEquals("", exiEvent.getURI());
    Assert.assertEquals("ABCINV", exiEvent.getCharacters().makeString());

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
    
    Assert.assertNull(scanner.nextEvent());
  }

}
