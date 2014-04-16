package org.openexi.sax;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.util.ArrayList;

import org.openexi.proc.EXIDecoder;
import org.openexi.proc.common.AlignmentType;
import org.openexi.proc.common.EXIEvent;
import org.openexi.proc.common.GrammarOptions;
import org.openexi.proc.grammars.GrammarCache;
import org.openexi.proc.io.Scanner;
import org.openexi.sax.Transmogrifier;
import org.openexi.schema.EXISchema;
import org.openexi.scomp.EXISchemaFactoryErrorMonitor;
import org.openexi.scomp.EXISchemaFactoryTestUtil;
import org.xml.sax.InputSource;

import junit.framework.Assert;
import junit.framework.TestCase;

public class CookieTest extends TestCase {

  public CookieTest(String name) {
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
    AlignmentType.compress };
  
  ///////////////////////////////////////////////////////////////////////////
  // Test cases
  ///////////////////////////////////////////////////////////////////////////
  
  public void testCookie_01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        (String)null, getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);
    
    final String xmlString = "<A/>";
    
    for (AlignmentType alignment : Alignments) {
      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();
      
      encoder.setAlignmentType(alignment);
      decoder.setAlignmentType(alignment);
      
      encoder.setEXISchema(grammarCache);
      ByteArrayOutputStream baos;

      byte[] noCookie, withCookie;

      baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);

      encoder.encode(new InputSource(new StringReader(xmlString)));
      noCookie = baos.toByteArray();

      baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);

      encoder.setOutputCookie(true);
      encoder.encode(new InputSource(new StringReader(xmlString)));
      withCookie = baos.toByteArray();
      
      Assert.assertEquals(noCookie.length, withCookie.length - 4);

      int n_events;
      ArrayList<EXIEvent> exiEventList;

      decoder.setEXISchema(grammarCache);

      for (byte[] bts : new byte[][] { noCookie, withCookie } ) {
        decoder.setInputStream(new ByteArrayInputStream(bts));
        Scanner scanner = decoder.processHeader();
      
        exiEventList = new ArrayList<EXIEvent>();
        
        EXIEvent exiEvent;
        n_events = 0;
        while ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          exiEventList.add(exiEvent);
        }
        
        Assert.assertEquals(4, n_events);
        
        int pos = 0;

        exiEvent = exiEventList.get(pos++);
        Assert.assertEquals(EXIEvent.EVENT_SD, exiEvent.getEventVariety());

        exiEvent = exiEventList.get(pos++);
        Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
        Assert.assertEquals("A", exiEvent.getName());
        Assert.assertEquals("", exiEvent.getURI());

        exiEvent = exiEventList.get(pos++);
        Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
        
        exiEvent = exiEventList.get(pos++);
        Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
      }
    }
  }
  
}
