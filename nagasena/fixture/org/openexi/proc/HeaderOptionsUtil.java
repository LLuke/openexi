package org.openexi.proc;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;

import org.openexi.proc.common.EXIOptionsException;
import org.openexi.proc.common.EventDescription;
import org.openexi.proc.common.EventType;
import org.openexi.proc.common.QName;
import org.openexi.proc.events.EXIEventSchemaNil;
import org.openexi.proc.grammars.GrammarCache;
import org.openexi.proc.grammars.OptionsGrammarCache;
import org.openexi.proc.io.BitPackedScanner;
import org.openexi.proc.io.Scanner;
import org.openexi.proc.io.ScannerFactory;
import org.openexi.proc.util.ExiUriConst;

public class HeaderOptionsUtil {
  
  @SuppressWarnings("unchecked")
  public static HashMap<String,Object> decode(InputStream inputStream) 
    throws IOException, EXIOptionsException {
    
    final GrammarCache grammarCache = OptionsGrammarCache.getGrammarCache();

    final BitPackedScanner scanner;
    
    scanner = ScannerFactory.createHeaderOptionsScanner();
    scanner.setSchema(grammarCache.getEXISchema(), (QName[])null, 0);
    scanner.setStringTable(Scanner.createStringTable(grammarCache));
    scanner.prepare();
    
    scanner.setEXIOptionsInputStream(inputStream);
    scanner.setGrammar(grammarCache.retrieveRootGrammar(false, scanner.eventTypesWorkSpace), grammarCache.grammarOptions);
    scanner.reset();

    HashMap<String,Object> options = new HashMap<String,Object>(); 
    
    short eventVariety;
    do {
      EventDescription exiEvent;
      EventType eventType;
      exiEvent = scanner.nextEvent();
      if ((eventVariety = exiEvent.getEventKind()) == EventDescription.EVENT_SE) {
        final String name = exiEvent.getName();
        final String uri = exiEvent.getURI();
        if (ExiUriConst.W3C_2009_EXI_URI.equals(uri)) {
          if ("byte".equals(name)) {
            options.put("byte", (String)null);
          }
          else if ("pre-compress".equals(name)) {
            options.put("pre-compress", (String)null);
          }
          else if ("compression".equals(name)) {
            options.put("compression", (String)null);
          }
          else if ("strict".equals(name)) {
            options.put("strict", (String)null);
          }
          else if ("comments".equals(name)) {
            options.put("comments", (String)null);
          }
          else if ("pis".equals(name)) {
            options.put("pis", (String)null);
          }
          else if ("dtd".equals(name)) {
            options.put("dtd", (String)null);
          }
          else if ("prefixes".equals(name)) {
            options.put("prefixes", (String)null);
          }
          else if ("lexicalValues".equals(name)) {
            options.put("lexicalValues", (String)null);
          }
          else if ("schemaId".equals(name)) {
            exiEvent = scanner.nextEvent();
            eventType = exiEvent.getEventType();
            if (eventType.itemType == EventType.ITEM_SCHEMA_NIL) {
              if (((EXIEventSchemaNil)exiEvent).isNilled()) {
                options.put("schemaId", (String)null);
                continue;
              }
              else {
                exiEvent = scanner.nextEvent();
                eventType = exiEvent.getEventType();
              }
            }
            assert eventType.itemType == EventType.ITEM_SCHEMA_CH;
            options.put("schemaId", exiEvent.getCharacters().makeString());
          }
          else if ("blockSize".equals(name)) {
            exiEvent = scanner.nextEvent();
            assert exiEvent.getEventType().itemType == EventType.ITEM_SCHEMA_CH;
            options.put("blockSize", exiEvent.getCharacters().makeString());
          }
          else if ("valueMaxLength".equals(name)) {
            exiEvent = scanner.nextEvent();
            assert exiEvent.getEventType().itemType == EventType.ITEM_SCHEMA_CH;
            options.put("valueMaxLength", exiEvent.getCharacters().makeString());
          }
          else if ("valuePartitionCapacity".equals(name)) {
            exiEvent = scanner.nextEvent();
            assert exiEvent.getEventType().itemType == EventType.ITEM_SCHEMA_CH;
            options.put("valuePartitionCapacity", exiEvent.getCharacters().makeString());
          }
          else if ("datatypeRepresentationMap".equals(name)) {
            final EventDescription typeName = scanner.nextEvent();
            assert typeName.getEventType().itemType == EventType.ITEM_SCHEMA_WC_ANY;
            exiEvent = scanner.nextEvent();
            assert exiEvent.getEventType().itemType == EventType.ITEM_EE;
            final EventDescription codecName = scanner.nextEvent();
            assert codecName.getEventType().itemType == EventType.ITEM_SCHEMA_WC_ANY;
            exiEvent = scanner.nextEvent();
            assert exiEvent.getEventType().itemType == EventType.ITEM_EE;
            ArrayList<String> datatypeRepresentationMap;
            if ((datatypeRepresentationMap = (ArrayList<String>)options.get("datatypeRepresentationMap")) == null)
              options.put("datatypeRepresentationMap", datatypeRepresentationMap = new ArrayList<String>()); 
            datatypeRepresentationMap.add(typeName.getURI());
            datatypeRepresentationMap.add(typeName.getName());
            datatypeRepresentationMap.add(codecName.getURI());
            datatypeRepresentationMap.add(codecName.getName());
          }
          else if ("fragment".equals(name)) {
            options.put("fragment", (String)null);
          }
          else if ("selfContained".equals(name)) {
            options.put("selfContained", (String)null);
          }
          else if ("common".equals(name)) {
            options.put("common", (String)null);
          }
          else if ("lesscommon".equals(name)) {
            options.put("lesscommon", (String)null);
          }
          else if ("uncommon".equals(name)) {
            options.put("uncommon", (String)null);
          }
        }
      }
    } 
    while (eventVariety != EventDescription.EVENT_ED);
    
    return options;
  }

}
