package org.openexi.proc;

import java.io.InputStream;
import java.io.IOException;

import org.openexi.proc.common.AlignmentType;
import org.openexi.proc.common.EventDescription;
import org.openexi.proc.common.EXIOptions;
import org.openexi.proc.common.EXIOptionsException;
import org.openexi.proc.common.EventType;
import org.openexi.proc.common.QName;
import org.openexi.proc.common.SchemaId;
import org.openexi.proc.events.EXIEventSchemaNil;
import org.openexi.proc.grammars.GrammarCache;
import org.openexi.proc.grammars.OptionsGrammarCache;
import org.openexi.proc.io.HeaderOptionsInputStream;
import org.openexi.proc.io.BitPackedScanner;
import org.openexi.proc.io.Scanner;
import org.openexi.proc.io.ScannerFactory;
import org.openexi.proc.util.ExiUriConst;
import org.openexi.schema.EXISchema;

class HeaderOptionsDecoder {

  private static final GrammarCache m_grammarCache;
  static {
    m_grammarCache = OptionsGrammarCache.getGrammarCache();
  }

  private final BitPackedScanner m_scanner;
  
  HeaderOptionsDecoder() {
    m_scanner = ScannerFactory.createHeaderOptionsScanner();
    final EXISchema schema = m_grammarCache.getEXISchema(); 
    m_scanner.setSchema(schema, (QName[])null, 0);
    m_scanner.setStringTable(Scanner.createStringTable(m_grammarCache));
    try {
      m_scanner.prepare();
    }
    catch (IOException ioe) {
      assert false;
    }
  }
  
  public HeaderOptionsInputStream decode(EXIOptions options, InputStream istream) throws IOException, EXIOptionsException {
    m_scanner.setEXIOptionsInputStream(istream);
    m_scanner.setGrammar(m_grammarCache.retrieveRootGrammar(false, m_scanner.eventTypesWorkSpace), m_grammarCache.grammarOptions);
    m_scanner.reset();

    short eventVariety;
    do {
      EventDescription exiEvent;
      EventType eventType;
      exiEvent = m_scanner.nextEvent();
      if ((eventVariety = exiEvent.getEventKind()) == EventDescription.EVENT_SE) {
        final String name = exiEvent.getName();
        final String uri = exiEvent.getURI();
        if (ExiUriConst.W3C_2009_EXI_URI.equals(uri)) {
          if ("byte".equals(name)) {
            options.setAlignmentType(AlignmentType.byteAligned);
          }
          else if ("pre-compress".equals(name)) {
            options.setAlignmentType(AlignmentType.preCompress);
          }
          else if ("compression".equals(name)) {
            options.setAlignmentType(AlignmentType.compress);
          }
          else if ("strict".equals(name)) {
            options.setStrict(true);
          }
          else if ("comments".equals(name)) {
            options.setPreserveComments(true);
          }
          else if ("pis".equals(name)) {
            options.setPreservePIs(true);
          }
          else if ("dtd".equals(name)) {
            options.setPreserveDTD(true);
          }
          else if ("prefixes".equals(name)) {
            options.setPreserveNS(true);
          }
          else if ("lexicalValues".equals(name)) {
            options.setDatatypeRepresentationMap((QName[])null, 0);
            options.setPreserveLexicalValues(true);
          }
          else if ("schemaId".equals(name)) {
            exiEvent = m_scanner.nextEvent();
            eventType = exiEvent.getEventType();
            if (eventType.itemType == EventType.ITEM_SCHEMA_NIL) {
              if (((EXIEventSchemaNil)exiEvent).isNilled()) {
                options.setSchemaId(new SchemaId(null));
                continue;
              }
              else {
                exiEvent = m_scanner.nextEvent();
                eventType = exiEvent.getEventType();
              }
            }
            assert eventType.itemType == EventType.ITEM_SCHEMA_CH;
            options.setSchemaId(new SchemaId(exiEvent.getCharacters().makeString()));
          }
          else if ("blockSize".equals(name)) {
            exiEvent = m_scanner.nextEvent();
            assert exiEvent.getEventType().itemType == EventType.ITEM_SCHEMA_CH;
            options.setBlockSize(Integer.valueOf(exiEvent.getCharacters().makeString()));
          }
          else if ("valueMaxLength".equals(name)) {
            exiEvent = m_scanner.nextEvent();
            assert exiEvent.getEventType().itemType == EventType.ITEM_SCHEMA_CH;
            options.setValueMaxLength((Integer.valueOf(exiEvent.getCharacters().makeString())));
          }
          else if ("valuePartitionCapacity".equals(name)) {
            exiEvent = m_scanner.nextEvent();
            assert exiEvent.getEventType().itemType == EventType.ITEM_SCHEMA_CH;
            options.setValuePartitionCapacity((Integer.valueOf(exiEvent.getCharacters().makeString())));
          }
          else if ("datatypeRepresentationMap".equals(name)) {
            final EventDescription typeName = m_scanner.nextEvent();
            assert typeName.getEventType().itemType == EventType.ITEM_SCHEMA_WC_ANY;
            exiEvent = m_scanner.nextEvent();
            assert exiEvent.getEventType().itemType == EventType.ITEM_EE;
            final EventDescription codecName = m_scanner.nextEvent();
            assert codecName.getEventType().itemType == EventType.ITEM_SCHEMA_WC_ANY;
            exiEvent = m_scanner.nextEvent();
            assert exiEvent.getEventType().itemType == EventType.ITEM_EE;
            options.appendDatatypeRepresentationMap(typeName, codecName);
          }
          else if ("fragment".equals(name)) {
            options.setFragment(true);
          }
          else if ("selfContained".equals(name)) {
            options.setInfuseSC(true);
          }
        }
      }
    } 
    while (eventVariety != EventDescription.EVENT_ED);
    
    return (HeaderOptionsInputStream)m_scanner.getBitInputStream();
  }
  
}
