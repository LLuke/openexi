package org.openexi.sax;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;

import junit.framework.Assert;

import org.openexi.proc.EXIDecoder;
import org.openexi.proc.common.AlignmentType;
import org.openexi.proc.common.EventDescription;
import org.openexi.proc.common.EventType;
import org.openexi.proc.common.EventTypeList;
import org.openexi.proc.common.GrammarOptions;
import org.openexi.proc.common.XmlUriConst;
import org.openexi.proc.events.EXIEventSchemaType;
import org.openexi.proc.grammars.GrammarCache;
import org.openexi.proc.io.Scanner;
import org.openexi.schema.EmptySchema;
import org.openexi.schema.TestBase;
import org.w3c.exi.ttf.Event;
import org.w3c.exi.ttf.sax.SAXRecorder;
import org.xml.sax.InputSource;

public class ProfileDecodeTest extends TestBase {
  
  public ProfileDecodeTest(String name) {
    super(name);
  }

  ///////////////////////////////////////////////////////////////////////////
  // Test cases
  ///////////////////////////////////////////////////////////////////////////

  /**
   * <A><A/></A> 
   */
  public void testNestedA_01() throws Exception {

    GrammarCache grammarCache = new GrammarCache(EmptySchema.getEXISchema(), GrammarOptions.STRICT_OPTIONS);
    
    String[] exiFiles = { 
        "/profile/nestedA_01.bitPacked",
        "/profile/nestedA_01.byteAligned",
        "/profile/nestedA_01_profile.bitPacked",
        "/profile/nestedA_01_profile.byteAligned"
    };

    AlignmentType[] alignments =  { 
        AlignmentType.bitPacked,
        AlignmentType.byteAligned,
        AlignmentType.bitPacked,
        AlignmentType.byteAligned
    };
    
    boolean[] profiled = {
        false,
        false,
        true,
        true
    };

    for (int i = 0; i < exiFiles.length; i++) {
      
      final EXIReader decoder = new EXIReader();
      decoder.setAlignmentType(alignments[i]);
      decoder.setGrammarCache(grammarCache);

      final InputStream inputStream = resolveSystemIdAsURL(exiFiles[i]).openStream();
      try {
        InputSource inputSource = new InputSource();
        inputSource.setByteStream(inputStream);
        ArrayList<Event> exiEventList = new ArrayList<Event>();
        SAXRecorder saxRecorder = new SAXRecorder(exiEventList, true);
        decoder.setContentHandler(saxRecorder);
        decoder.setLexicalHandler(saxRecorder);

        decoder.parse(inputSource);
        
        Assert.assertEquals(profiled[i] ? 14 : 10, exiEventList.size());
        
        Event saxEvent;
    
        int n = 0;
        
        saxEvent = exiEventList.get(n++);
        Assert.assertEquals(Event.NAMESPACE, saxEvent.type);
        Assert.assertEquals(XmlUriConst.W3C_XML_1998_URI, saxEvent.namespace);
        Assert.assertEquals("xml", saxEvent.name);

        saxEvent = exiEventList.get(n++);
        Assert.assertEquals(Event.NAMESPACE, saxEvent.type);
        Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, saxEvent.namespace);
        Assert.assertEquals("xsi", saxEvent.name);

        saxEvent = exiEventList.get(n++);
        Assert.assertEquals(Event.NAMESPACE, saxEvent.type);
        Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_URI, saxEvent.namespace);
        Assert.assertEquals("xsd", saxEvent.name);

        if (profiled[i]) {
          saxEvent = exiEventList.get(n++);
          Assert.assertEquals(Event.NAMESPACE, saxEvent.type);
          Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_URI, saxEvent.namespace);
          Assert.assertEquals("p0", saxEvent.name);
        }
        
        saxEvent = exiEventList.get(n++);
        Assert.assertEquals(Event.START_ELEMENT, saxEvent.type);
        Assert.assertEquals("", saxEvent.namespace);
        Assert.assertEquals("A", saxEvent.localName);
        
        if (profiled[i]) {
          saxEvent = exiEventList.get(n++);
          Assert.assertEquals(Event.ATTRIBUTE, saxEvent.type);
          Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, saxEvent.namespace);
          Assert.assertEquals("type", saxEvent.localName);
          Assert.assertEquals("p0:anyType", saxEvent.stringValue);
        }
        
        saxEvent = exiEventList.get(n++);
        Assert.assertEquals(Event.START_ELEMENT, saxEvent.type);
        Assert.assertEquals("", saxEvent.namespace);
        Assert.assertEquals("A", saxEvent.name);
        
        if (profiled[i]) {
          saxEvent = exiEventList.get(n++);
          Assert.assertEquals(Event.ATTRIBUTE, saxEvent.type);
          Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, saxEvent.namespace);
          Assert.assertEquals("type", saxEvent.localName);
          Assert.assertEquals("p0:anyType", saxEvent.stringValue);
        }
        
        saxEvent = exiEventList.get(n++);
        Assert.assertEquals(Event.END_ELEMENT, saxEvent.type);
        Assert.assertEquals("", saxEvent.namespace);
        Assert.assertEquals("A", saxEvent.name);

        saxEvent = exiEventList.get(n++);
        Assert.assertEquals(Event.END_ELEMENT, saxEvent.type);
        Assert.assertEquals("", saxEvent.namespace);
        Assert.assertEquals("A", saxEvent.name);

        saxEvent = exiEventList.get(n++);
        Assert.assertEquals(Event.END_NAMESPACE, saxEvent.type);
        Assert.assertEquals("xml", saxEvent.name);

        saxEvent = exiEventList.get(n++);
        Assert.assertEquals(Event.END_NAMESPACE, saxEvent.type);
        Assert.assertEquals("xsi", saxEvent.name);

        saxEvent = exiEventList.get(n++);
        Assert.assertEquals(Event.END_NAMESPACE, saxEvent.type);
        Assert.assertEquals("xsd", saxEvent.name);

        if (profiled[i]) {
          saxEvent = exiEventList.get(n++);
          Assert.assertEquals(Event.END_NAMESPACE, saxEvent.type);
          Assert.assertEquals("p0", saxEvent.name);
        }
        
        Assert.assertEquals(exiEventList.size(), n);
      }
      finally {
        inputStream.close();
      }
    }
  }

  /**
   * <A><B/><B/><B/></A>
   * See testcase xsitype-02 in config/testCases-profile/xsiType.xml
   */
  public void testXsiType_02() throws Exception {

    GrammarCache grammarCache = new GrammarCache(EmptySchema.getEXISchema(), GrammarOptions.STRICT_OPTIONS);
    
    String[] exiFiles = { 
        "/profile/xsiType-02_profile.bitPacked",
        "/profile/xsiType-02_profile.byteAligned"
    };

    AlignmentType[] alignments =  { 
        AlignmentType.bitPacked,
        AlignmentType.byteAligned
    };
    
    for (int i = 0; i < exiFiles.length; i++) {
      AlignmentType alignment = alignments[i];
      EXIDecoder decoder = new EXIDecoder();
      Scanner scanner;
      
      decoder.setAlignmentType(alignment);
  
      URL url = resolveSystemIdAsURL(exiFiles[i]);
  
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
      Assert.assertEquals("A", exiEvent.getName());
      Assert.assertEquals("", exiEvent.getURI());

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
      Assert.assertEquals("B", exiEvent.getName());
      Assert.assertEquals("", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
      Assert.assertEquals(3, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(5, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("B", eventType.name);
      Assert.assertEquals("", eventType.uri);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
      
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_TP, exiEvent.getEventKind());
      Assert.assertEquals("anyType", ((EXIEventSchemaType)exiEvent).getTypeName());
      Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_URI, ((EXIEventSchemaType)exiEvent).getTypeURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
      Assert.assertEquals(2, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(5, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventType.ITEM_AT, eventType.itemType);
      Assert.assertEquals("type", eventType.name);
      Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, eventType.uri);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      Assert.assertEquals(2, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(4, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventType.ITEM_SCHEMA_WC_ANY, eventType.itemType);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventType.ITEM_SCHEMA_CH_MIXED, eventType.itemType);
      
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
      Assert.assertEquals("B", exiEvent.getName());
      Assert.assertEquals("", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
      Assert.assertEquals(2, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(4, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("B", eventType.name);
      Assert.assertEquals("", eventType.uri);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_TP, exiEvent.getEventKind());
      Assert.assertEquals("anyType", ((EXIEventSchemaType)exiEvent).getTypeName());
      Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_URI, ((EXIEventSchemaType)exiEvent).getTypeURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
      Assert.assertEquals(2, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(5, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventType.ITEM_AT, eventType.itemType);
      Assert.assertEquals("type", eventType.name);
      Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, eventType.uri);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
      
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      Assert.assertEquals(2, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(4, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventType.ITEM_SCHEMA_WC_ANY, eventType.itemType);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventType.ITEM_SCHEMA_CH_MIXED, eventType.itemType);

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
      Assert.assertEquals("B", exiEvent.getName());
      Assert.assertEquals("", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(0, eventType.getIndex());
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("B", eventType.name);
      Assert.assertEquals("", eventType.uri);
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(4, eventTypeList.getLength());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_TP, exiEvent.getEventKind());
      Assert.assertEquals("anyType", ((EXIEventSchemaType)exiEvent).getTypeName());
      Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_URI, ((EXIEventSchemaType)exiEvent).getTypeURI());

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
      
      Assert.assertNull(scanner.nextEvent());
    }
  }
  
}
