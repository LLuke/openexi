package org.openexi.sax;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URL;

import org.openexi.proc.EXIDecoder;
import org.openexi.proc.common.AlignmentType;
import org.openexi.proc.common.EventDescription;
import org.openexi.proc.common.EventType;
import org.openexi.proc.common.GrammarOptions;
import org.openexi.proc.grammars.GrammarCache;
import org.openexi.proc.io.Scanner;
import org.openexi.sax.Transmogrifier;
import org.openexi.schema.EXISchema;
import org.openexi.schema.EXISchemaConst;
import org.openexi.schema.TestBase;
import org.openexi.scomp.EXISchemaFactoryErrorMonitor;
import org.openexi.scomp.EXISchemaFactoryTestUtil;
import org.xml.sax.InputSource;

import junit.framework.Assert;

public class Base64BinaryValueEncodingTest extends TestBase {
  
  public Base64BinaryValueEncodingTest(String name) {
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
   * A valid base64Binary value matching ITEM_SCHEMA_CH where the associated
   * datatype is xsd:base64Binary.
   */
  public void testValidBase64Binary() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/base64Binary.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);
    
    final String[] xmlStrings;
    final String[] originalValues = {
        " \t\r QUJDREVGR0hJSg==\n", 
        " \t\r\n ",
        " RHIuIFN1ZSBDbGFyayBpcyBjdXJyZW50bHkgYSBSZWdlbnRzIFByb2Zlc3NvciBvZiBDaGVtaXN0\n" +
        "cnkgYXQgV2FzaGluZ3RvbiBTdGF0ZSBVbml2ZXJzaXR5IGluIFB1bGxtYW4sIFdBLCB3aGVyZSBz\n" +
        "aGUgaGFzIHRhdWdodCBhbmQgY29uZHVjdGVkIHJlc2VhcmNoIGluIGFjdGluaWRlIGVudmlyb25t\n" +
        "ZW50YWwgY2hlbWlzdHJ5IGFuZCByYWRpb2FuYWx5dGljYWwgY2hlbWlzdHJ5IHNpbmNlIDE5OTYu\n" +
        "ICBGcm9tIDE5OTIgdG8gMTk5NiwgRHIuIENsYXJrIHdhcyBhIFJlc2VhcmNoIEVjb2xvZ2lzdCBh\n" +
        "dCB0aGUgVW5pdmVyc2l0eSBvZiBHZW9yZ2lh4oCZcyBTYXZhbm5haCBSaXZlciBFY29sb2d5IExh\n" +
        "Ym9yYXRvcnkuICBQcmlvciB0byBoZXIgcG9zaXRpb24gYXQgdGhlIFVuaXZlcnNpdHkgb2YgR2Vv\n" +
        "cmdpYSwgc2hlIHdhcyBhIFNlbmlvciBTY2llbnRpc3QgYXQgdGhlIFdlc3Rpbmdob3VzZSBTYXZh\n" +
        "bm5haCBSaXZlciBDb21wYW554oCZcyBTYXZhbm5haCBSaXZlciBUZWNobm9sb2d5IENlbnRlci4g\n" +
        "IERyLiBDbGFyayBoYXMgc2VydmVkIG9uIHZhcmlvdXMgYm9hcmRzIGFuZCBhZHZpc29yeSBjb21t\n" +
        "aXR0ZWVzLCBpbmNsdWRpbmcgdGhlIE5hdGlvbmFsIEFjYWRlbWllcyBOdWNsZWFyIGFuZCBSYWRp\n" +
        "YXRpb24gU3R1ZGllcyBCb2FyZCBhbmQgdGhlIERlcGFydG1lbnQgb2YgRW5lcmd54oCZcyBCYXNp\n" +
        "YyBFbmVyZ3kgU2NpZW5jZXMgQWR2aXNvcnkgQ29tbWl0dGVlLiAgRHIuIENsYXJrIGhvbGRzIGEg\n" +
        "UGguRC4gYW5kIE0uUy4gaW4gSW5vcmdhbmljL1JhZGlvY2hlbWlzdHJ5IGZyb20gRmxvcmlkYSBT\n" +
        "dGF0ZSBVbml2ZXJzaXR5IGFuZCBhIEIuUy4gaW4gQ2hlbWlzdHJ5IGZyb20gTGFuZGVyIENvbGxl\n" +
        "Z2UuDQo=\t\r\n"
    };
    final String[] parsedOriginalValues = {
        " \t\n QUJDREVGR0hJSg==\n", 
        " \t\n ",
        " RHIuIFN1ZSBDbGFyayBpcyBjdXJyZW50bHkgYSBSZWdlbnRzIFByb2Zlc3NvciBvZiBDaGVtaXN0\n" +
        "cnkgYXQgV2FzaGluZ3RvbiBTdGF0ZSBVbml2ZXJzaXR5IGluIFB1bGxtYW4sIFdBLCB3aGVyZSBz\n" +
        "aGUgaGFzIHRhdWdodCBhbmQgY29uZHVjdGVkIHJlc2VhcmNoIGluIGFjdGluaWRlIGVudmlyb25t\n" +
        "ZW50YWwgY2hlbWlzdHJ5IGFuZCByYWRpb2FuYWx5dGljYWwgY2hlbWlzdHJ5IHNpbmNlIDE5OTYu\n" +
        "ICBGcm9tIDE5OTIgdG8gMTk5NiwgRHIuIENsYXJrIHdhcyBhIFJlc2VhcmNoIEVjb2xvZ2lzdCBh\n" +
        "dCB0aGUgVW5pdmVyc2l0eSBvZiBHZW9yZ2lh4oCZcyBTYXZhbm5haCBSaXZlciBFY29sb2d5IExh\n" +
        "Ym9yYXRvcnkuICBQcmlvciB0byBoZXIgcG9zaXRpb24gYXQgdGhlIFVuaXZlcnNpdHkgb2YgR2Vv\n" +
        "cmdpYSwgc2hlIHdhcyBhIFNlbmlvciBTY2llbnRpc3QgYXQgdGhlIFdlc3Rpbmdob3VzZSBTYXZh\n" +
        "bm5haCBSaXZlciBDb21wYW554oCZcyBTYXZhbm5haCBSaXZlciBUZWNobm9sb2d5IENlbnRlci4g\n" +
        "IERyLiBDbGFyayBoYXMgc2VydmVkIG9uIHZhcmlvdXMgYm9hcmRzIGFuZCBhZHZpc29yeSBjb21t\n" +
        "aXR0ZWVzLCBpbmNsdWRpbmcgdGhlIE5hdGlvbmFsIEFjYWRlbWllcyBOdWNsZWFyIGFuZCBSYWRp\n" +
        "YXRpb24gU3R1ZGllcyBCb2FyZCBhbmQgdGhlIERlcGFydG1lbnQgb2YgRW5lcmd54oCZcyBCYXNp\n" +
        "YyBFbmVyZ3kgU2NpZW5jZXMgQWR2aXNvcnkgQ29tbWl0dGVlLiAgRHIuIENsYXJrIGhvbGRzIGEg\n" +
        "UGguRC4gYW5kIE0uUy4gaW4gSW5vcmdhbmljL1JhZGlvY2hlbWlzdHJ5IGZyb20gRmxvcmlkYSBT\n" +
        "dGF0ZSBVbml2ZXJzaXR5IGFuZCBhIEIuUy4gaW4gQ2hlbWlzdHJ5IGZyb20gTGFuZGVyIENvbGxl\n" +
        "Z2UuDQo=\t\n"
    };
    final String[] resultValues = {
        "QUJDREVGR0hJSg==",
        "",
        "RHIuIFN1ZSBDbGFyayBpcyBjdXJyZW50bHkgYSBSZWdlbnRzIFByb2Zlc3NvciBvZiBDaGVtaXN0\n" +
        "cnkgYXQgV2FzaGluZ3RvbiBTdGF0ZSBVbml2ZXJzaXR5IGluIFB1bGxtYW4sIFdBLCB3aGVyZSBz\n" +
        "aGUgaGFzIHRhdWdodCBhbmQgY29uZHVjdGVkIHJlc2VhcmNoIGluIGFjdGluaWRlIGVudmlyb25t\n" +
        "ZW50YWwgY2hlbWlzdHJ5IGFuZCByYWRpb2FuYWx5dGljYWwgY2hlbWlzdHJ5IHNpbmNlIDE5OTYu\n" +
        "ICBGcm9tIDE5OTIgdG8gMTk5NiwgRHIuIENsYXJrIHdhcyBhIFJlc2VhcmNoIEVjb2xvZ2lzdCBh\n" +
        "dCB0aGUgVW5pdmVyc2l0eSBvZiBHZW9yZ2lh4oCZcyBTYXZhbm5haCBSaXZlciBFY29sb2d5IExh\n" +
        "Ym9yYXRvcnkuICBQcmlvciB0byBoZXIgcG9zaXRpb24gYXQgdGhlIFVuaXZlcnNpdHkgb2YgR2Vv\n" +
        "cmdpYSwgc2hlIHdhcyBhIFNlbmlvciBTY2llbnRpc3QgYXQgdGhlIFdlc3Rpbmdob3VzZSBTYXZh\n" +
        "bm5haCBSaXZlciBDb21wYW554oCZcyBTYXZhbm5haCBSaXZlciBUZWNobm9sb2d5IENlbnRlci4g\n" +
        "IERyLiBDbGFyayBoYXMgc2VydmVkIG9uIHZhcmlvdXMgYm9hcmRzIGFuZCBhZHZpc29yeSBjb21t\n" +
        "aXR0ZWVzLCBpbmNsdWRpbmcgdGhlIE5hdGlvbmFsIEFjYWRlbWllcyBOdWNsZWFyIGFuZCBSYWRp\n" +
        "YXRpb24gU3R1ZGllcyBCb2FyZCBhbmQgdGhlIERlcGFydG1lbnQgb2YgRW5lcmd54oCZcyBCYXNp\n" +
        "YyBFbmVyZ3kgU2NpZW5jZXMgQWR2aXNvcnkgQ29tbWl0dGVlLiAgRHIuIENsYXJrIGhvbGRzIGEg\n" +
        "UGguRC4gYW5kIE0uUy4gaW4gSW5vcmdhbmljL1JhZGlvY2hlbWlzdHJ5IGZyb20gRmxvcmlkYSBT\n" +
        "dGF0ZSBVbml2ZXJzaXR5IGFuZCBhIEIuUy4gaW4gQ2hlbWlzdHJ5IGZyb20gTGFuZGVyIENvbGxl\n" +
        "Z2UuDQo="
    };

    int i;
    xmlStrings = new String[originalValues.length];
    for (i = 0; i < originalValues.length; i++) {
      xmlStrings[i] = "<foo:Base64Binary xmlns:foo='urn:foo'>" + originalValues[i] + "</foo:Base64Binary>\n";
    };
    
    Transmogrifier encoder = new Transmogrifier();
    EXIDecoder decoder = new EXIDecoder();
    
    for (AlignmentType alignment : Alignments) {
      for (boolean preserveLexicalValues : new boolean[] { true, false }) {
        String[] values = preserveLexicalValues ? 
            parsedOriginalValues : resultValues;
        for (i = 0; i < xmlStrings.length; i++) {
          Scanner scanner;
          
          encoder.setAlignmentType(alignment);
          decoder.setAlignmentType(alignment);
          
          encoder.setPreserveLexicalValues(preserveLexicalValues);
          decoder.setPreserveLexicalValues(preserveLexicalValues);
          
          encoder.setGrammarCache(grammarCache);
          ByteArrayOutputStream baos = new ByteArrayOutputStream();
          encoder.setOutputStream(baos);
          
          byte[] bts;
          int n_events;
          
          encoder.encode(new InputSource(new StringReader(xmlStrings[i])));
          
          bts = baos.toByteArray();
          
          decoder.setGrammarCache(grammarCache);
          decoder.setInputStream(new ByteArrayInputStream(bts));
          scanner = decoder.processHeader();
          
          EventDescription exiEvent;
          EventType eventType;

          n_events = 0;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_SD, eventType.itemType);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
          Assert.assertEquals("Base64Binary", eventType.name);
          Assert.assertEquals("urn:foo", eventType.uri);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
          Assert.assertEquals(values[i], exiEvent.getCharacters().makeString());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            int tp = scanner.currentState.contentDatatype; 
            Assert.assertEquals(EXISchemaConst.BASE64BINARY_TYPE, corpus.getSerialOfType(tp));
          }
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_ED, eventType.itemType);
          ++n_events;

          Assert.assertEquals(5, n_events);
        }
      }
    }
  }

  /**
   * Preserve lexical base64Binary values by turning on Preserve.lexicalValues.
   */
  public void testValidBase64BinaryRCS() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/base64Binary.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);
    
    final String[] xmlStrings;
    final String[] originalValues = {
        " \t\r QUJDREVGR0hJSg@==\n", // '@' will be encoded as an escaped character 
    };
    final String[] parsedOriginalValues = {
        " \t\n QUJDREVGR0hJSg@==\n", 
    };

    int i;
    xmlStrings = new String[originalValues.length];
    for (i = 0; i < originalValues.length; i++) {
      xmlStrings[i] = "<foo:Base64Binary xmlns:foo='urn:foo'>" + originalValues[i] + "</foo:Base64Binary>\n";
    };
    
    Transmogrifier encoder = new Transmogrifier();
    EXIDecoder decoder = new EXIDecoder();

    encoder.setGrammarCache(grammarCache);
    decoder.setGrammarCache(grammarCache);

    encoder.setPreserveLexicalValues(true);
    decoder.setPreserveLexicalValues(true);
    
    for (AlignmentType alignment : Alignments) {
      encoder.setAlignmentType(alignment);
      decoder.setAlignmentType(alignment);
      for (i = 0; i < xmlStrings.length; i++) {
        Scanner scanner;
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        encoder.setOutputStream(baos);
        
        byte[] bts;
        int n_events;
        
        encoder.encode(new InputSource(new StringReader(xmlStrings[i])));
        
        bts = baos.toByteArray();
        
        decoder.setInputStream(new ByteArrayInputStream(bts));
        scanner = decoder.processHeader();
        
        EventDescription exiEvent;
        EventType eventType;

        n_events = 0;

        exiEvent = scanner.nextEvent();
        Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_SD, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
        Assert.assertEquals("Base64Binary", eventType.name);
        Assert.assertEquals("urn:foo", eventType.uri);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
        Assert.assertEquals(parsedOriginalValues[i], exiEvent.getCharacters().makeString());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          int tp = scanner.currentState.contentDatatype; 
          Assert.assertEquals(EXISchemaConst.BASE64BINARY_TYPE, corpus.getSerialOfType(tp));
        }
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_ED, eventType.itemType);
        ++n_events;

        Assert.assertEquals(5, n_events);
      }
    }
  }

  /**
   * Process 1000BinaryStore
   */
  public void testDecode1000BinaryStore() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/DataStore/DataStore.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

    String[] base64Values100 = {
        "R0lGODdhWALCov////T09M7Ozqampn19fVZWVi0tLQUFBSxYAsJAA/8Iutz+MMpJq7046827/2Ao\n",
        "/9j/4BBKRklGAQEBASwBLP/hGlZFeGlmTU0qF5ZOSUtPTiBDT1JQT1JBVElPTk5J",
        "R0lGODlhHh73KSkpOTk5QkJCSkpKUlJSWlpaY2Nja2trc3Nze3t7hISEjIyMlJSUnJycpaWlra2t\n" + "tbW1vb29xsbGzs7O1tbW3t7e5+fn7+/v//////////8=",
        "/9j/4BBKRklGAQEBAf/bQwYEBQYFBAYGBQYHBwYIChAKCgkJChQODwwQFxQYGBc=",
        "R0lGODlhHh73M2aZzP8zMzNmM5kzzDP/M2YzZmZmmWbM",
        "/9j/4BBKRklGAQEBAf/bQwYEBQYFBAYGBQYHBwYIChAKCgkJChQODwwQFxQYGBcUFhYaHSUfGhsj\n" + "HBYWICwgIyYnKSopGR8tMC0oMCUoKSj/20M=",
        "R0lGODdhWAK+ov////j4+NTU1JOTk0tLSx8fHwkJCSxYAr5AA/8IMkzjrEEmahy23SpC",
        "R0lGODdh4QIpAncJIf4aU29mdHdhcmU6IE1pY3Jvc29mdCBPZmZpY2Us4QIpAof//////8z//5n/\n",
        "R0lGODdhWAK+ov////v7++fn58DAwI6Ojl5eXjExMQMDAyxYAr5AA/8Iutz+MMpJq7046827/2Ao\n" + "jmRpnmiqPsKxvg==",
        "R0lGODdh4QIpAncJIf4aU29mdHdhcmU6IE1pY3Jvc29mdCBPZmZpY2Us4QIpAob//////8z//5n/\nzP//zMw=",
    };
    
    final AlignmentType alignment = AlignmentType.bitPacked;
    
    Transmogrifier encoder = new Transmogrifier();
    encoder.setGrammarCache(grammarCache);
    encoder.setAlignmentType(alignment);
    
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    encoder.setOutputStream(baos);
    
    URL url = resolveSystemIdAsURL("/DataStore/instance/1000BinaryStore.xml");
    encoder.encode(new InputSource(url.openStream()));
    
    byte[] bts = baos.toByteArray();

    Scanner scanner;
      
    int n_texts;
    
    EXIDecoder decoder = new EXIDecoder();
    decoder.setGrammarCache(grammarCache);
    decoder.setAlignmentType(alignment);
    decoder.setInputStream(new ByteArrayInputStream(bts));
    scanner = decoder.processHeader();
    
    EventDescription exiEvent;
    n_texts = 0;
    while ((exiEvent = scanner.nextEvent()) != null) {
      if (exiEvent.getEventKind() == EventDescription.EVENT_CH) {
        if (++n_texts % 100 == 0) {
          String expected = base64Values100[(n_texts / 100) - 1];
          String val = exiEvent.getCharacters().makeString();
          Assert.assertEquals(expected, val);
        }
      }
    }
    Assert.assertEquals(1000, n_texts);
  }

  /**
   * Testing character buffer expansion code. Base64 text size of 95K. 
   * This is much larger than the initial character buffer size of 4K. 
   */
  public void testBase64BinaryText_95K() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/base64Binary.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);
    
    Transmogrifier encoder = new Transmogrifier();
    EXIDecoder decoder = new EXIDecoder();
    
    for (AlignmentType alignment : Alignments) {
      final URL url = resolveSystemIdAsURL("/base64Binary_95K.xml");
      InputSource inputSource = new InputSource(url.toString());
      InputStream inputStream = url.openStream();
      inputSource.setByteStream(inputStream);
      
      Scanner scanner;
      
      encoder.setAlignmentType(alignment);
      decoder.setAlignmentType(alignment);
          
      encoder.setGrammarCache(grammarCache);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);
      
      byte[] bts;
      int n_events;
      
      encoder.encode(inputSource);
      
      bts = baos.toByteArray();
      
      decoder.setGrammarCache(grammarCache);
      decoder.setInputStream(new ByteArrayInputStream(bts));
      scanner = decoder.processHeader();
      
      EventDescription exiEvent;
      EventType eventType;

      n_events = 0;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SD, eventType.itemType);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("Base64Binary", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
      final String base64Text = exiEvent.getCharacters().makeString();
      Assert.assertTrue(base64Text.startsWith("/9j/4AAQSkZJRgABAQEASABIAAD/2wBDAAYEBQYFBAYGBQYHBwYIChAKCgkJChQODwwQFxQYGBcU"));
      Assert.assertTrue(base64Text.endsWith("cdtrkLfpl+ojHqNoMgxQ97NUOAv4x/0tQVnw1EJqWd5tcy8/4GoLLP1Eo5GkdmCUVjivsf/Z"));
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        int tp = scanner.currentState.contentDatatype; 
        Assert.assertEquals(EXISchemaConst.BASE64BINARY_TYPE, corpus.getSerialOfType(tp));
      }
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_ED, eventType.itemType);
      ++n_events;

      Assert.assertEquals(5, n_events);
      inputStream.close();
    }
  }
  
}
