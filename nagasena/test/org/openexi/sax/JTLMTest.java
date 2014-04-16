package org.openexi.sax;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URL;

import junit.framework.Assert;

import org.openexi.proc.EXIDecoder;
import org.openexi.proc.common.AlignmentType;
import org.openexi.proc.common.EventDescription;
import org.openexi.proc.common.EventType;
import org.openexi.proc.common.GrammarOptions;
import org.openexi.proc.grammars.GrammarCache;
import org.openexi.proc.io.Scanner;
import org.openexi.sax.Transmogrifier;
import org.openexi.schema.EXISchema;
import org.openexi.schema.TestBase;
import org.openexi.scomp.EXISchemaFactoryErrorMonitor;
import org.openexi.scomp.EXISchemaFactoryTestUtil;
import org.xml.sax.InputSource;

public class JTLMTest extends TestBase {

  public JTLMTest(String name) {
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
  
  final static String[] publish100_centennials_0 = {
      "http://10.32.40.2:9093/AFATDS/?TargetNumber=AA0036", 
      "500", 
      "Unknown", 
      "0", 
      "PLANNED", 
      "0", 
      "NOTSET", 
      "0", 
      "2006-08-29T16:59:59.073Z", 
      "2006-07-19T16:34:53.0Z", 
      "0", 
      "ADA_MEDIUM", 
      "36.25587463", 
      "http://10.32.40.2:9093/AFATDS/?TargetNumber=AD0040", 
      "500", 
      "Unknown", 
      "0", 
      "PLANNED", 
      "0", 
      "NOTSET", 
      "0", 
      "2006-08-29T16:59:59.183Z", 
      "2006-07-19T16:35:36.0Z", 
      "0", 
      "ADA_MEDIUM", 
      "36.02297973" };
  
  final static String[] publish911_centennials_0 = {
    "http://10.32.40.2:9093/AFATDS/?TargetNumber=LS0012",
    "500",
    "Unknown",
    "0",
    "PLANNED",
    "0",
    "NOTSET",
    "0",
    "2006-08-29T16:54:48.259Z",
    "2006-07-20T00:20:52.0Z",
    "0",
    "MISSILE_MEDIUM",
    "36.15631484",
    "http://10.32.40.2:9093/AFATDS/?TargetNumber=MS0025",
    "500",
    "Unknown",
    "0",
    "PLANNED",
    "0",
    "NOTSET",
    "0",
    "2006-08-29T16:54:48.39Z",
    "2006-07-20T00:22:24.0Z",
    "0",
    "MISSILE_MEDIUM",
    "36.46515274",
    "http://10.32.40.2:9093/AFATDS/?TargetNumber=MS0003",
    "500",
    "Unknown",
    "0",
    "PLANNED",
    "0",
    "NOTSET",
    "0",
    "2006-08-29T16:54:48.52Z",
    "2006-07-20T00:21:57.0Z",
    "0",
    "CHEMICAL_PRODUCTS",
    "36.09661865",
    "http://10.32.40.2:9093/AFATDS/?TargetNumber=RR0019",
    "500",
    "Unknown",
    "0",
    "PLANNED",
    "0",
    "NOTSET",
    "0",
    "2006-08-29T16:54:48.64Z",
    "2006-07-20T00:14:35.0Z",
    "0",
    "BUILDING_CONCRETE",
    "36.13935089",
    "http://10.32.40.2:9093/AFATDS/?TargetNumber=BD0274",
    "500",
    "Unknown",
    "0",
    "PLANNED",
    "0",
    "NOTSET",
    "0",
    "2006-08-29T16:54:48.75Z",
    "2006-07-20T00:18:32.0Z",
    "0",
    "BUILDING_METAL",
    "36.12839126",
    "http://10.32.40.2:9093/AFATDS/?TargetNumber=BD0242",
    "500",
    "Unknown",
    "0",
    "PLANNED",
    "0",
    "NOTSET",
    "0",
    "2006-08-29T16:54:48.88Z",
    "2006-07-20T00:17:27.0Z",
    "0",
    "BOAT",
    "36.15649795",
    "http://10.32.40.2:9093/AFATDS/?TargetNumber=BD0221",
    "500",
    "Unknown",
    "0",
    "PLANNED",
    "0",
    "NOTSET",
    "0",
    "2006-08-29T16:54:49.0Z",
    "2006-07-20T00:13:03.0Z",
    "0",
    "BUILDING_CONCRETE",
    "36.10599517",
    "http://10.32.40.2:9093/AFATDS/?TargetNumber=BD0203",
    "500",
    "Unknown",
    "0",
    "PLANNED",
    "0",
    "NOTSET",
    "0",
    "2006-08-29T16:54:49.131Z",
    "2006-07-20T00:07:39.0Z",
    "0",
    "ASSEMBLY_AREA_TROOPS_AND_VEHICLES",
    "36.19093322",
    "http://10.32.40.2:9093/AFATDS/?TargetNumber=AD0058",
    "500",
    "Unknown",
    "0",
    "PLANNED",
    "0",
    "NOTSET",
    "0",
    "2006-08-29T16:54:49.251Z",
    "2006-07-19T16:36:11.0Z",
    "0",
    "PATROL",
    "36.09344863",
    "http://10.32.40.2:9093/AFATDS/?TargetNumber=AA0005",
    "500",
    "Unknown",
    "0",
    "PLANNED",
    "0",
    "NOTSET",
    "0",
    "2006-08-29T16:54:49.371Z",
    "2006-07-19T16:35:47.0Z",
    "0",
    "BUILDING_CONCRETE",
    "36.06555175",
    "http://10.32.40.2:9093/AFATDS/?TargetNumber=RS0018",
    "500",
    "Unknown",
    "0",
    "PLANNED",
    "0",
    "NOTSET",
    "0",
    "2006-08-29T16:54:49.501Z",
    "2006-07-19T16:35:30.0Z",
    "0",
    "ADA_MEDIUM",
    "36.02374267",
    "http://10.32.40.2:9093/AFATDS/?TargetNumber=BD0048",
    "500",
    "Unknown",
    "0",
    "PLANNED",
    "0",
    "NOTSET",
    "0",
    "2006-08-29T16:54:49.621Z",
    "2006-07-20T00:09:39.0Z",
    "0",
    "BUILDING_CONCRETE",
    "36.21646118",
    "http://10.32.40.2:9093/AFATDS/?TargetNumber=BD0020",
    "500",
    "Unknown",
    "0",
    "PLANNED",
    "0",
    "NOTSET",
    "0",
    "2006-08-29T16:54:49.741Z",
    "2006-07-20T00:09:10.0Z",
    "0",
    "BUILDING_CONCRETE",
    "36.27273941",
    "http://10.32.40.2:9093/AFATDS/?TargetNumber=BR0002",
    "500",
    "Unknown",
    "0",
    "PLANNED",
    "0",
    "NOTSET",
    "0",
    "2006-08-29T16:54:49.852Z",
    "2006-07-20T00:15:16.0Z",
    "0",
    "BUILDING_CONCRETE",
    "36.18189239",
    "http://10.32.40.2:9093/AFATDS/?TargetNumber=BD0319",
    "500",
    "Unknown",
    "0",
    "PLANNED",
    "0",
    "NOTSET",
    "0",
    "2006-08-29T16:54:49.972Z",
    "2006-07-20T00:17:03.0Z",
    "0",
    "BRIDGE_VEHICLE_STEEL",
    "36.1946907",
    "http://10.32.40.2:9093/AFATDS/?TargetNumber=BD0186",
    "500",
    "Unknown",
    "0",
    "PLANNED",
    "0",
    "NOTSET",
    "0",
    "2006-08-29T16:54:50.102Z",
    "2006-07-20T00:12:12.0Z",
    "0",
    "ADA_MEDIUM",
    "36.18514633",
    "http://10.32.40.2:9093/AFATDS/?TargetNumber=AD0115",
    "500",
    "Unknown",
    "0",
    "PLANNED",
    "0",
    "NOTSET",
    "0",
    "2006-08-29T16:54:50.212Z",
    "2006-07-20T00:08:30.0Z",
    "0",
    "BUILDING_CONCRETE",
    "36.10503387",
    "http://10.32.40.2:9093/AFATDS/?TargetNumber=BD0141",
    "500",
    "Unknown",
    "0",
    "PLANNED",
    "0",
    "NOTSET",
    "0",
    "2006-08-29T16:54:50.332Z",
    "2006-07-20T00:19:45.0Z",
    "0",
    "BUILDING_CONCRETE",
    "36.0941658",
    "http://10.32.40.2:9093/AFATDS/?TargetNumber=BD0111",
    "500",
    "Unknown"
  };

  final static String[] publish100_centennials_1;
  final static String[] publish911_centennials_1;
  static {
    int len;
    len = publish100_centennials_0.length;
    publish100_centennials_1 = new String[len];
    for (int i = 0; i < len; i++) {
      String str = publish100_centennials_0[i];
      if (str.endsWith(".0Z"))
        str = str.substring(0, str.length() - 3) + "Z"; // i.e. omit fractional digits of value 0
      publish100_centennials_1[i] = str;
    }
    len = publish911_centennials_0.length;
    publish911_centennials_1 = new String[len];
    for (int i = 0; i < len; i++) {
      String str = publish911_centennials_0[i];
      if (str.endsWith(".0Z"))
        str = str.substring(0, str.length() - 3) + "Z"; // i.e. omit fractional digits of value 0
      publish911_centennials_1[i] = str;
    }
  }

  ///////////////////////////////////////////////////////////////////////////
  // Test cases
  ///////////////////////////////////////////////////////////////////////////

  /**
   * EXI test cases of Joint Theater Logistics Management format.
   */
  public void testJTLM_publish100() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/JTLM/schemas/TLMComposite.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);

    for (AlignmentType alignment : Alignments) {
      for (boolean useThreadedInflater : new boolean[] { true, false }) {
        if (useThreadedInflater && alignment != AlignmentType.compress)
          continue;
        Transmogrifier encoder = new Transmogrifier();
        EXIDecoder decoder = new EXIDecoder(999, useThreadedInflater);
        Scanner scanner;
        InputSource inputSource;
        
        encoder.setAlignmentType(alignment);
        decoder.setAlignmentType(alignment);
  
        encoder.setGrammarCache(grammarCache);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        encoder.setOutputStream(baos);
        
        URL url = resolveSystemIdAsURL("/JTLM/publish100.xml");
        inputSource = new InputSource(url.toString());
        inputSource.setByteStream(url.openStream());
  
        byte[] bts;
        int n_events, n_texts;
        
        encoder.encode(inputSource);
        
        bts = baos.toByteArray();
  
        decoder.setGrammarCache(grammarCache);
        decoder.setInputStream(new ByteArrayInputStream(bts));
        scanner = decoder.processHeader();
        
        EventDescription exiEvent;
        n_events = 0;
        n_texts = 0;
        while ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          if (exiEvent.getEventKind() == EventDescription.EVENT_CH) {
            if (n_texts % 100 == 0) {
              final int n = n_texts / 100;
              Assert.assertEquals(publish100_centennials_1[n], exiEvent.getCharacters().makeString());
            }
            ++n_texts;
          }
        }
        Assert.assertEquals(10610, n_events);
      }
    }
  }

  /**
   * Decode EXI-encoded JTLM data.
   */
  public void testDecodeJTLM_publish100() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/JTLM/schemas/TLMComposite.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);

    String[] exiFiles = { 
        "/JTLM/publish100/publish100.bitPacked", 
        "/JTLM/publish100/publish100.byteAligned",
        "/JTLM/publish100/publish100.preCompress", 
        "/JTLM/publish100/publish100.compress" };

    for (int i = 0; i < Alignments.length; i++) {
      AlignmentType alignment = Alignments[i];
      EXIDecoder decoder = new EXIDecoder();
      Scanner scanner;
      
      decoder.setAlignmentType(alignment);
  
      URL url = resolveSystemIdAsURL(exiFiles[i]);
  
      int n_events, n_texts;
      
      decoder.setGrammarCache(grammarCache);
      decoder.setInputStream(url.openStream());
      scanner = decoder.processHeader();
      
      EventDescription exiEvent;
      n_events = 0;
      n_texts = 0;
      while ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        if (exiEvent.getEventKind() == EventDescription.EVENT_CH) {
          String stringValue = exiEvent.getCharacters().makeString();
          if (stringValue.length() == 0 && exiEvent.getEventType().itemType == EventType.ITEM_SCHEMA_CH) {
            --n_events;
            continue;
          }
          if (n_texts % 100 == 0) {
            final int n = n_texts / 100;
            Assert.assertEquals(publish100_centennials_0[n], stringValue);
          }
          ++n_texts;
        }
      }
      
      Assert.assertEquals(10610, n_events);
    }
  }

  /**
   * Decode EXI-encoded JTLM data.
   */
  public void testDecodeJTLM_publish911() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/JTLM/schemas/TLMComposite.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);

    String[] exiFiles = { 
        "/JTLM/publish911/publish911.bitPacked",  
        "/JTLM/publish911/publish911.byteAligned",
        "/JTLM/publish911/publish911.preCompress",
        "/JTLM/publish911/publish911.compress" };

    for (int i = 0; i < Alignments.length; i++) {
      AlignmentType alignment = Alignments[i];
      EXIDecoder decoder = new EXIDecoder();
      Scanner scanner;
      
      decoder.setAlignmentType(alignment);
  
      URL url = resolveSystemIdAsURL(exiFiles[i]);
  
      int n_events, n_texts;
      
      decoder.setGrammarCache(grammarCache);
      decoder.setInputStream(url.openStream());
      scanner = decoder.processHeader();
      
      EventDescription exiEvent;
      n_events = 0;
      n_texts = 0;
      while ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        if (exiEvent.getEventKind() == EventDescription.EVENT_CH) {
          String stringValue = exiEvent.getCharacters().makeString();
          if (stringValue.length() == 0 && exiEvent.getEventType().itemType == EventType.ITEM_SCHEMA_CH) {
            --n_events;
            continue;
          }
          if (n_texts % 100 == 0) {
            final int n = n_texts / 100;
            Assert.assertEquals(publish911_centennials_0[n], stringValue);
          }
          ++n_texts;
        }
      }
      
      Assert.assertEquals(96576, n_events);
    }
  }
  
}
