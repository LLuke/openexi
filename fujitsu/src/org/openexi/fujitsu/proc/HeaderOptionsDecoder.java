package org.openexi.fujitsu.proc;

import java.io.InputStream;
import java.io.IOException;

import org.openexi.fujitsu.proc.common.AlignmentType;
import org.openexi.fujitsu.proc.common.EXIEvent;
import org.openexi.fujitsu.proc.common.EXIOptions;
import org.openexi.fujitsu.proc.common.EXIOptionsException;
import org.openexi.fujitsu.proc.common.EventCode;
import org.openexi.fujitsu.proc.common.EventType;
import org.openexi.fujitsu.proc.common.QName;
import org.openexi.fujitsu.proc.common.SchemaId;
import org.openexi.fujitsu.proc.events.EXIEventSchemaNil;
import org.openexi.fujitsu.proc.grammars.DocumentGrammarState;
import org.openexi.fujitsu.proc.grammars.GrammarCache;
import org.openexi.fujitsu.proc.grammars.OptionsGrammarCache;
import org.openexi.fujitsu.proc.io.HeaderOptionsInputStream;
import org.openexi.fujitsu.proc.io.BitPackedScanner;
import org.openexi.fujitsu.proc.io.ScannerFactory;
import org.openexi.fujitsu.proc.io.StringTable;
import org.openexi.fujitsu.proc.util.URIConst;
import org.openexi.fujitsu.schema.EXISchema;

class HeaderOptionsDecoder {

  private static final GrammarCache m_grammarCache;
  static {
    m_grammarCache = OptionsGrammarCache.getGrammarCache();
  }

  private final BitPackedScanner m_scanner;
  private final DocumentGrammarState m_documentGrammarState;
  
  HeaderOptionsDecoder() {
    m_documentGrammarState = new DocumentGrammarState();
    m_scanner = ScannerFactory.createHeaderOptionsScanner(m_documentGrammarState); 
    final EXISchema schema = m_grammarCache.getEXISchema(); 
    m_scanner.setSchema(schema, (QName[])null, 0);
    m_scanner.setStringTable(new StringTable(schema));
    try {
      m_scanner.prepare();
    }
    catch (IOException ioe) {
      assert false;
    }
  }
  
  public HeaderOptionsInputStream decode(EXIOptions options, InputStream istream) throws IOException, EXIOptionsException {
    m_scanner.setEXIOptionsInputStream(istream);
    m_scanner.setGrammar(m_grammarCache.retrieveDocumentGrammar(false, m_documentGrammarState.eventTypesWorkSpace), m_grammarCache.grammarOptions);
    m_scanner.reset();

    short eventVariety;
    do {
      EXIEvent exiEvent;
      EventType eventType;
      exiEvent = m_scanner.nextEvent();
      if ((eventVariety = exiEvent.getEventVariety()) == EXIEvent.EVENT_SE) {
        final String name = exiEvent.getName();
        final String uri = exiEvent.getURI();
        if (URIConst.W3C_2009_EXI_URI.equals(uri)) {
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
            if (eventType.itemType == EventCode.ITEM_SCHEMA_NIL) {
              if (((EXIEventSchemaNil)exiEvent).isNilled()) {
                options.setSchemaId(new SchemaId(null));
                continue;
              }
              else {
                exiEvent = m_scanner.nextEvent();
                eventType = exiEvent.getEventType();
              }
            }
            assert eventType.itemType == EventCode.ITEM_SCHEMA_CH;
            options.setSchemaId(new SchemaId(exiEvent.getCharacters().makeString()));
          }
          else if ("blockSize".equals(name)) {
            exiEvent = m_scanner.nextEvent();
            assert exiEvent.getEventType().itemType == EventCode.ITEM_SCHEMA_CH;
            options.setBlockSize(Integer.valueOf(exiEvent.getCharacters().makeString()));
          }
          else if ("valueMaxLength".equals(name)) {
            exiEvent = m_scanner.nextEvent();
            assert exiEvent.getEventType().itemType == EventCode.ITEM_SCHEMA_CH;
            options.setValueMaxLength((Integer.valueOf(exiEvent.getCharacters().makeString())));
          }
          else if ("valuePartitionCapacity".equals(name)) {
            exiEvent = m_scanner.nextEvent();
            assert exiEvent.getEventType().itemType == EventCode.ITEM_SCHEMA_CH;
            options.setValuePartitionCapacity((Integer.valueOf(exiEvent.getCharacters().makeString())));
          }
          else if ("datatypeRepresentationMap".equals(name)) {
            final EXIEvent typeName = m_scanner.nextEvent();
            assert typeName.getEventType().itemType == EventCode.ITEM_SCHEMA_WC_ANY;
            exiEvent = m_scanner.nextEvent();
            assert exiEvent.getEventType().itemType == EventCode.ITEM_EE;
            final EXIEvent codecName = m_scanner.nextEvent();
            assert codecName.getEventType().itemType == EventCode.ITEM_SCHEMA_WC_ANY;
            exiEvent = m_scanner.nextEvent();
            assert exiEvent.getEventType().itemType == EventCode.ITEM_EE;
            options.appendDatatypeRepresentationMap(typeName, codecName);
          }
          else if ("fragment".equals(name)) {
            options.setFragment(true);
          }
        }
      }
    } 
    while (eventVariety != EXIEvent.EVENT_ED);
    
    return (HeaderOptionsInputStream)m_scanner.getBitInputStream();
  }
  
}
