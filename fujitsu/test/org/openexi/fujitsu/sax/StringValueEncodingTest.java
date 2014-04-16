package org.openexi.fujitsu.sax;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.util.ArrayList;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.openexi.fujitsu.proc.EXIDecoder;
import org.openexi.fujitsu.proc.common.AlignmentType;
import org.openexi.fujitsu.proc.common.EXIEvent;
import org.openexi.fujitsu.proc.common.EventCode;
import org.openexi.fujitsu.proc.common.EventType;
import org.openexi.fujitsu.proc.common.EventTypeList;
import org.openexi.fujitsu.proc.common.GrammarOptions;
import org.openexi.fujitsu.proc.grammars.GrammarCache;
import org.openexi.fujitsu.proc.io.Scanner;
import org.xml.sax.InputSource;

public class StringValueEncodingTest extends TestCase {
  
  public StringValueEncodingTest(String name) {
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
   * Encode then decoder non-BMP characters.
   */
  public void testNonBMPCharacters_01() throws Exception {

    GrammarCache grammarCache = new GrammarCache(null, GrammarOptions.DEFAULT_OPTIONS);
    
    final String[] xmlStrings;
    final String[] values = {
        "\uD840\uDC0B\uD844\uDE3D", // characters in SIP (U+2000B) and (U+2123D)
        "a \uD840\uDC0B\uD844\uDE3D b",
    };
    final String[] resultValues = {
        "\uD840\uDC0B\uD844\uDE3D",
        "a \uD840\uDC0B\uD844\uDE3D b",
    };

    int i;
    xmlStrings = new String[values.length];
    for (i = 0; i < values.length; i++) {
      xmlStrings[i] = "<A>" + values[i] + "</A>\n";
    };

    for (AlignmentType alignment : Alignments) {
      for (i = 0; i < xmlStrings.length; i++) {
      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();
      Scanner scanner;
      
      encoder.setAlignmentType(alignment);
      decoder.setAlignmentType(alignment);

      encoder.setEXISchema(grammarCache);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);
      
      byte[] bts;
      int n_events;
      
      encoder.encode(new InputSource(new StringReader(xmlStrings[i])));
      
      bts = baos.toByteArray();
      
      decoder.setEXISchema(grammarCache);
      decoder.setInputStream(new ByteArrayInputStream(bts));
      scanner = decoder.processHeader();
      
      ArrayList<EXIEvent> exiEventList = new ArrayList<EXIEvent>();
      
      EXIEvent exiEvent;
      n_events = 0;
      while ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        exiEventList.add(exiEvent);
      }
      
      Assert.assertEquals(5, n_events);
  
      EventType eventType;
      EventTypeList eventTypeList;
  
      exiEvent = exiEventList.get(0);
      Assert.assertEquals(EXIEvent.EVENT_SD, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
      
      exiEvent = exiEventList.get(1);
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      
      exiEvent = exiEventList.get(2);
      Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
      Assert.assertEquals(resultValues[i], exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
      exiEvent = exiEventList.get(3);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
  
      exiEvent = exiEventList.get(4);
      Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(1, eventTypeList.getLength());
      }
    }
  }
  
}
