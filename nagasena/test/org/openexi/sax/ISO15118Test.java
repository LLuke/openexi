package org.openexi.sax;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URL;

import junit.framework.Assert;

import org.openexi.proc.EXIDecoder;
import org.openexi.proc.common.AlignmentType;
import org.openexi.proc.common.EventDescription;
import org.openexi.proc.common.EventType;
import org.openexi.proc.common.EventTypeList;
import org.openexi.proc.common.GrammarOptions;
import org.openexi.proc.grammars.GrammarCache;
import org.openexi.proc.io.Scanner;
import org.openexi.schema.EXISchema;
import org.openexi.schema.TestBase;
import org.openexi.scomp.EXISchemaFactoryErrorMonitor;
import org.openexi.scomp.EXISchemaFactoryTestUtil;
import org.xml.sax.InputSource;

public class ISO15118Test extends TestBase {

  public ISO15118Test(String name) {
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
   * EXI test cases of ISO15118.
   */
  public void testISO15118() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/iso15118/V2G_CI_MsgDef.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

    for (AlignmentType alignment : Alignments) {
      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();
      Scanner scanner;
      InputSource inputSource;
      
      encoder.setAlignmentType(alignment);
      decoder.setAlignmentType(alignment);

      encoder.setGrammarCache(grammarCache);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);
      
      URL url = resolveSystemIdAsURL("/iso15118/SessionSetupRes.xml");
      inputSource = new InputSource(url.toString());
      inputSource.setByteStream(url.openStream());

      byte[] bts;
      int n_events;
      
      encoder.encode(inputSource);
      
      bts = baos.toByteArray();

      decoder.setGrammarCache(grammarCache);
      decoder.setInputStream(new ByteArrayInputStream(bts));
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
      Assert.assertEquals("V2G_Message", exiEvent.getName());
      Assert.assertEquals("urn:iso:15118:2:2013:MsgDef", exiEvent.getURI());
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
      Assert.assertEquals("Header", exiEvent.getName());
      Assert.assertEquals("urn:iso:15118:2:2013:MsgDef", exiEvent.getURI());
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
      Assert.assertEquals("SessionID", exiEvent.getName());
      Assert.assertEquals("urn:iso:15118:2:2013:MsgHeader", exiEvent.getURI());
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
      Assert.assertEquals("3031323334353637", exiEvent.getCharacters().makeString());
      ++n_events;
      
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
      Assert.assertEquals("Body", exiEvent.getName());
      Assert.assertEquals("urn:iso:15118:2:2013:MsgDef", exiEvent.getURI());
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
      Assert.assertEquals("SessionSetupRes", exiEvent.getName());
      Assert.assertEquals("urn:iso:15118:2:2013:MsgBody", exiEvent.getURI());
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
      Assert.assertEquals("ResponseCode", exiEvent.getName());
      Assert.assertEquals("urn:iso:15118:2:2013:MsgBody", exiEvent.getURI());
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
      Assert.assertEquals("OK", exiEvent.getCharacters().makeString());
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
      Assert.assertEquals("EVSEID", exiEvent.getName());
      Assert.assertEquals("urn:iso:15118:2:2013:MsgBody", exiEvent.getURI());
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
      Assert.assertEquals("FRA23E45B78C", exiEvent.getCharacters().makeString());
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
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

      Assert.assertNull(scanner.nextEvent());

      Assert.assertEquals(19, n_events);
    }
  }
  
}
