package org.openexi.sax;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;

import javax.xml.parsers.SAXParserFactory;

import org.openexi.proc.EXIDecoder;
import org.openexi.proc.common.AlignmentType;
import org.openexi.proc.common.EventDescription;
import org.openexi.proc.common.EventType;
import org.openexi.proc.common.GrammarOptions;
import org.openexi.proc.grammars.GrammarCache;
import org.openexi.proc.io.Scanner;
import org.openexi.schema.EXISchema;
import org.openexi.schema.EmptySchema;
import org.xml.sax.InputSource;

import junit.framework.Assert;
import junit.framework.TestCase;

public class TransmogrifierTest extends TestCase {
  
  public TransmogrifierTest(String name) {
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
   * SAXParser factory for use with Transmogrifier needs to be aware of namespaces.
   */
  public void testSAXParserFactoryMisConfigured_01() throws Exception {

    SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();

    saxParserFactory.setNamespaceAware(true);
    new Transmogrifier(saxParserFactory);
    
    saxParserFactory.setNamespaceAware(false);
    try {
      new Transmogrifier(saxParserFactory);
    }
    catch (TransmogrifierRuntimeException te) {
      Assert.assertEquals(TransmogrifierRuntimeException.SAXPARSER_FACTORY_NOT_NAMESPACE_AWARE, te.getCode());
      return;
    }
    Assert.fail();
  }
  
  /**
   * Tests accessors to the GrammarCache.
   */
  public void testGrammarCacheAccessor_01() throws Exception {

    GrammarCache grammarCache = new GrammarCache(EmptySchema.getEXISchema(), GrammarOptions.STRICT_OPTIONS);
    
    Transmogrifier encoder = new Transmogrifier();
    
    encoder.setGrammarCache(grammarCache);
    Assert.assertSame(grammarCache, encoder.getGrammarCache());
  }
  
  /**
   * Enable XML parser's "http://xml.org/sax/features/namespace-prefixes" feature.
   */
  public void testNamespacePrefixesFeature_01() throws Exception {

    GrammarCache grammarCache = new GrammarCache((EXISchema)null, GrammarOptions.addNS(GrammarOptions.DEFAULT_OPTIONS));
    
    final String xmlString = "<abc:rpc message-id='id' xmlns:abc='a.b.c'><abc:inner/></abc:rpc>\n";
    
    for (AlignmentType alignment : Alignments) {
      
      Transmogrifier encoder = new Transmogrifier(true); // Turn on "http://xml.org/sax/features/namespace-prefixes"
      encoder.setAlignmentType(alignment);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      encoder.setGrammarCache(grammarCache);
      encoder.setOutputStream(baos);
      
      encoder.encode(new InputSource(new StringReader(xmlString)));
      
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
