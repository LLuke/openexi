package org.openexi.fujitsu.proc;

import java.io.IOException;
import java.io.OutputStream;

import org.openexi.fujitsu.schema.EXISchema;
import org.openexi.fujitsu.schema.EXISchemaConst;
import org.openexi.fujitsu.proc.common.AlignmentType;
import org.openexi.fujitsu.proc.common.EXIOptions;
import org.openexi.fujitsu.proc.common.EventCode;
import org.openexi.fujitsu.proc.common.EventType;
import org.openexi.fujitsu.proc.common.EventTypeList;
import org.openexi.fujitsu.proc.common.GrammarOptions;
import org.openexi.fujitsu.proc.common.QName;
import org.openexi.fujitsu.proc.common.SchemaId;
import org.openexi.fujitsu.proc.grammars.DocumentGrammarState;
import org.openexi.fujitsu.proc.grammars.EventTypeSchema;
import org.openexi.fujitsu.proc.grammars.GrammarCache;
import org.openexi.fujitsu.proc.grammars.OptionsGrammarCache;
import org.openexi.fujitsu.proc.io.BitOutputStream;
import org.openexi.fujitsu.proc.io.BitPackedScriber;
import org.openexi.fujitsu.proc.io.IntegerValueScriber;
import org.openexi.fujitsu.proc.io.Scribble;
import org.openexi.fujitsu.proc.io.Scriber;
import org.openexi.fujitsu.proc.io.ScriberFactory;
import org.openexi.fujitsu.proc.io.StringTable;
import org.openexi.fujitsu.proc.util.URIConst;

public final class EXIOptionsEncoder {
  
  private final GrammarCache m_grammarCache;
  private final DocumentGrammarState m_documentState;
  private final BitPackedScriber m_scriber;
  private final Scribble m_scribble;
  
  private final EXISchema m_schema;
  private final int m_unsignedIntType;
  private final IntegerValueScriber m_valueScriberUnsignedInt;

  public EXIOptionsEncoder() {
    m_grammarCache = OptionsGrammarCache.getGrammarCache();
    m_schema = m_grammarCache.getEXISchema();
    m_documentState = new DocumentGrammarState();
    m_scriber = ScriberFactory.createHeaderOptionsScriber(); 
    m_scriber.setSchema(m_grammarCache.getEXISchema(), (QName[])null, 0);
    m_scriber.setPreserveNS(GrammarOptions.hasNS(m_grammarCache.grammarOptions));
    m_scriber.setStringTable(new StringTable(m_grammarCache.getEXISchema()));
    m_scriber.setValueMaxLength(EXIOptions.VALUE_MAX_LENGTH_UNBOUNDED);
    m_scribble = new Scribble();
    
    m_unsignedIntType = m_schema.getBuiltinTypeOfSchema(EXISchemaConst.UNSIGNED_INT_TYPE);
    m_valueScriberUnsignedInt = (IntegerValueScriber)m_scriber.getValueScriber(m_unsignedIntType);
  }
  
  public BitOutputStream encode(EXIOptions options, boolean outputSchemaId, OutputStream ostream) throws IOException {
    m_scriber.setOutputStream(ostream);
    m_scriber.reset();

    m_grammarCache.retrieveDocumentGrammar(false, m_documentState.eventTypesWorkSpace).init(m_documentState);

    EventTypeList eventTypes;
    EventType eventType;

    eventTypes = m_documentState.getNextEventTypes();
    
    eventType = eventTypes.item(0);
    assert eventType.itemType == EventCode.ITEM_SD && eventTypes.getLength() == 1;
    m_scriber.writeEventType(eventType);
    m_documentState.startDocument();
    eventTypes = m_documentState.getNextEventTypes();

    eventType = eventTypes.item(0);
    assert "header".equals(eventType.getName());
    m_scriber.writeEventType(eventType);
    m_documentState.startElement(0, URIConst.W3C_2009_EXI_URI, "header");
    eventTypes = m_documentState.getNextEventTypes();

    int pos;
    int pos_level1 = 0;
    
    final AlignmentType alignmentType = options.getAlignmentType();
    int delineation = options.getOutline();
    if ((delineation & EXIOptions.ADD_LESSCOMMON) != 0) {
      eventType = eventTypes.item(pos_level1++);
      assert "lesscommon".equals(eventType.getName());
      m_scriber.writeEventType(eventType);
      m_documentState.startElement(0, URIConst.W3C_2009_EXI_URI, "lesscommon");
      eventTypes = m_documentState.getNextEventTypes();
      int pos_level2 = 0;
      if ((delineation & EXIOptions.ADD_UNCOMMON) != 0) {
        eventType = eventTypes.item(pos_level2++);
        assert "uncommon".equals(eventType.getName());
        m_scriber.writeEventType(eventType);
        m_documentState.startElement(0, URIConst.W3C_2009_EXI_URI, "uncommon");
        eventTypes = m_documentState.getNextEventTypes();
        int pos_level3 = 0;
        if ((delineation & EXIOptions.ADD_ALIGNMENT) != 0) {
          eventType = eventTypes.item(pos_level3++);
          assert "alignment".equals(eventType.getName());
          m_scriber.writeEventType(eventType);
          m_documentState.startElement(0, URIConst.W3C_2009_EXI_URI, "alignment");
          eventTypes = m_documentState.getNextEventTypes();
          final String name;
          if (alignmentType == AlignmentType.byteAligned) {
            pos = 0;
            name = "byte";
          }
          else {
            assert alignmentType == AlignmentType.preCompress;
            pos = 1;
            name = "pre-compress";
          }
          eventType = eventTypes.item(pos);
          assert name.equals(eventType.getName());
          m_scriber.writeEventType(eventType);
          m_documentState.startElement(pos, URIConst.W3C_2009_EXI_URI, name);
          eventTypes = m_documentState.getNextEventTypes();
          eventType = eventTypes.item(0);
          assert eventType.itemType == EventCode.ITEM_SCHEMA_EE;
          m_scriber.writeEventType(eventType);
          m_documentState.endElement(URIConst.W3C_2009_EXI_URI, name);
          eventTypes = m_documentState.getNextEventTypes();
          eventType = eventTypes.getEE();
          m_scriber.writeEventType(eventType);
          m_documentState.endElement(URIConst.W3C_2009_EXI_URI, "alignment");
          eventTypes = m_documentState.getNextEventTypes();
        }
        if ((delineation & EXIOptions.ADD_VALUE_MAX_LENGTH) != 0) {
          pos = 2 - pos_level3;
          eventType = eventTypes.item(pos);
          pos_level3 = 3;
          assert "valueMaxLength".equals(eventType.getName());
          m_scriber.writeEventType(eventType);
          m_documentState.startElement(pos, URIConst.W3C_2009_EXI_URI, "valueMaxLength");
          eventTypes = m_documentState.getNextEventTypes();
          m_valueScriberUnsignedInt.processUnsignedInt(options.getValueMaxLength(), m_scribble);
          m_valueScriberUnsignedInt.scribe((String)null, m_scribble, "valueMaxLength", URIConst.W3C_2009_EXI_URI, m_unsignedIntType);
          m_documentState.characters();
          eventTypes = m_documentState.getNextEventTypes();
          eventType = eventTypes.getEE();
          m_scriber.writeEventType(eventType);
          m_documentState.endElement(URIConst.W3C_2009_EXI_URI, "valueMaxLength");
          eventTypes = m_documentState.getNextEventTypes();
        }
        if ((delineation & EXIOptions.ADD_VALUE_PARTITION_CAPACITY) != 0) {
          pos = 3 - pos_level3;
          eventType = eventTypes.item(pos);
          pos_level3 = 4;
          assert "valuePartitionCapacity".equals(eventType.getName());
          m_scriber.writeEventType(eventType);
          m_documentState.startElement(pos, URIConst.W3C_2009_EXI_URI, "valuePartitionCapacity");
          eventTypes = m_documentState.getNextEventTypes();
          m_valueScriberUnsignedInt.processUnsignedInt(options.getValuePartitionCapacity(), m_scribble);
          m_valueScriberUnsignedInt.scribe((String)null, m_scribble, "valuePartitionCapacity", URIConst.W3C_2009_EXI_URI, m_unsignedIntType);
          m_documentState.characters();
          eventTypes = m_documentState.getNextEventTypes();
          eventType = eventTypes.getEE();
          m_scriber.writeEventType(eventType);
          m_documentState.endElement(URIConst.W3C_2009_EXI_URI, "valuePartitionCapacity");
          eventTypes = m_documentState.getNextEventTypes();
        }
        if ((delineation & EXIOptions.ADD_DTRM) != 0) {
          pos = 4 - pos_level3;
          pos_level3 = 5;
          final QName[] dtrm = options.getDatatypeRepresentationMap();
          final int n_dtrmItems = options.getDatatypeRepresentationMapBindingsCount();
          for (int i = 0; i < n_dtrmItems; i++, pos = 0) {
            QName qname;
            eventType = eventTypes.item(pos);
            assert "datatypeRepresentationMap".equals(eventType.getName());
            m_scriber.writeEventType(eventType);
            m_documentState.startElement(pos, URIConst.W3C_2009_EXI_URI, "datatypeRepresentationMap");
            eventTypes = m_documentState.getNextEventTypes();
            qname = dtrm[i << 1];
            eventType = eventTypes.item(0);
            assert eventTypes.getLength() == 1 && eventType.itemType == EventCode.ITEM_SCHEMA_WC_ANY;
            m_scriber.writeEventType(eventType);
            m_scriber.writeQName(qname, EventCode.ITEM_SCHEMA_WC_ANY);
            m_documentState.startElement(0, qname.namespaceName, qname.localName);
            eventTypes = m_documentState.getNextEventTypes();
            eventType = eventTypes.getEE();
            m_scriber.writeEventType(eventType);
            m_documentState.endElement(qname.namespaceName, qname.localName);
            eventTypes = m_documentState.getNextEventTypes();
            qname = dtrm[(i << 1) + 1];
            eventType = eventTypes.item(0);
            assert eventTypes.getLength() == 1 && eventType.itemType == EventCode.ITEM_SCHEMA_WC_ANY;
            m_scriber.writeEventType(eventType);
            m_scriber.writeQName(qname, EventCode.ITEM_SCHEMA_WC_ANY);
            m_documentState.startElement(0, qname.namespaceName, qname.localName);
            eventTypes = m_documentState.getNextEventTypes();
            eventType = eventTypes.getEE();
            m_scriber.writeEventType(eventType);
            m_documentState.endElement(qname.namespaceName, qname.localName);
            eventTypes = m_documentState.getNextEventTypes();
            eventType = eventTypes.getEE();
            m_scriber.writeEventType(eventType);
            m_documentState.endElement(URIConst.W3C_2009_EXI_URI, "datatypeRepresentationMap");
            eventTypes = m_documentState.getNextEventTypes();
          }
        }
        eventType = eventTypes.getEE();
        m_scriber.writeEventType(eventType);
        m_documentState.endElement(URIConst.W3C_2009_EXI_URI, "uncommon");
        eventTypes = m_documentState.getNextEventTypes();
      }
      if ((delineation & EXIOptions.ADD_PRESERVE) != 0) {
        pos = 1 - pos_level2;
        eventType = eventTypes.item(pos);
        pos_level2 = 2;
        assert "preserve".equals(eventType.getName());
        m_scriber.writeEventType(eventType);
        m_documentState.startElement(pos, URIConst.W3C_2009_EXI_URI, "preserve");
        eventTypes = m_documentState.getNextEventTypes();
        int pos_level3 = 0;
        if (options.getPreserveDTD()) {
          eventType = eventTypes.item(pos_level3++);
          assert "dtd".equals(eventType.getName());
          m_scriber.writeEventType(eventType);
          m_documentState.startElement(0, URIConst.W3C_2009_EXI_URI, "dtd");
          eventTypes = m_documentState.getNextEventTypes();
          eventType = eventTypes.getEE();
          m_scriber.writeEventType(eventType);
          m_documentState.endElement(URIConst.W3C_2009_EXI_URI, "dtd");
          eventTypes = m_documentState.getNextEventTypes();
        }
        if (options.getPreserveNS()) {
          pos = 1 - pos_level3;
          eventType = eventTypes.item(pos);
          pos_level3 = 2;
          assert "prefixes".equals(eventType.getName());
          m_scriber.writeEventType(eventType);
          m_documentState.startElement(pos, URIConst.W3C_2009_EXI_URI, "prefixes");
          eventTypes = m_documentState.getNextEventTypes();
          eventType = eventTypes.getEE();
          m_scriber.writeEventType(eventType);
          m_documentState.endElement(URIConst.W3C_2009_EXI_URI, "prefixes");
          eventTypes = m_documentState.getNextEventTypes();
        }
        if (options.getPreserveLexicalValues()) {
          pos = 2 - pos_level3;
          eventType = eventTypes.item(pos);
          pos_level3 = 3;
          assert "lexicalValues".equals(eventType.getName());
          m_scriber.writeEventType(eventType);
          m_documentState.startElement(pos, URIConst.W3C_2009_EXI_URI, "lexicalValues");
          eventTypes = m_documentState.getNextEventTypes();
          eventType = eventTypes.getEE();
          m_scriber.writeEventType(eventType);
          m_documentState.endElement(URIConst.W3C_2009_EXI_URI, "lexicalValues");
          eventTypes = m_documentState.getNextEventTypes();
        }
        /**
         * REVISIT: write out <lexicalValues/> if necessary
         */
        if (options.getPreserveComments()) {
          pos = 3 - pos_level3;
          eventType = eventTypes.item(pos);
          pos_level3 = 4;
          assert "comments".equals(eventType.getName());
          m_scriber.writeEventType(eventType);
          m_documentState.startElement(pos, URIConst.W3C_2009_EXI_URI, "comments");
          eventTypes = m_documentState.getNextEventTypes();
          eventType = eventTypes.getEE();
          m_scriber.writeEventType(eventType);
          m_documentState.endElement(URIConst.W3C_2009_EXI_URI, "comments");
          eventTypes = m_documentState.getNextEventTypes();
        }
        if (options.getPreservePIs()) {
          pos = 4 - pos_level3;
          eventType = eventTypes.item(pos);
          pos_level3 = 5;
          assert "pis".equals(eventType.getName());
          m_scriber.writeEventType(eventType);
          m_documentState.startElement(pos, URIConst.W3C_2009_EXI_URI, "pis");
          eventTypes = m_documentState.getNextEventTypes();
          eventType = eventTypes.getEE();
          m_scriber.writeEventType(eventType);
          m_documentState.endElement(URIConst.W3C_2009_EXI_URI, "pis");
          eventTypes = m_documentState.getNextEventTypes();
        }
        eventType = eventTypes.getEE();
        m_scriber.writeEventType(eventType);
        m_documentState.endElement(URIConst.W3C_2009_EXI_URI, "preserve");
        eventTypes = m_documentState.getNextEventTypes();
      }
      int blockSize;
      if ((blockSize = options.getBlockSize()) != EXIOptions.BLOCKSIZE_DEFAULT) {
        pos = 2 - pos_level2;
        eventType = eventTypes.item(pos);
        pos_level2 = 3;
        assert "blockSize".equals(eventType.getName());
        m_scriber.writeEventType(eventType);
        m_documentState.startElement(pos, URIConst.W3C_2009_EXI_URI, "blockSize");
        eventTypes = m_documentState.getNextEventTypes();
        m_valueScriberUnsignedInt.processUnsignedInt(blockSize, m_scribble);
        m_valueScriberUnsignedInt.scribe((String)null, m_scribble, "blockSize", URIConst.W3C_2009_EXI_URI, m_unsignedIntType);
        m_documentState.characters();
        eventTypes = m_documentState.getNextEventTypes();
        eventType = eventTypes.getEE();
        m_scriber.writeEventType(eventType);
        m_documentState.endElement(URIConst.W3C_2009_EXI_URI, "blockSize");
        eventTypes = m_documentState.getNextEventTypes();
      }
      eventType = eventTypes.getEE();
      m_scriber.writeEventType(eventType);
      m_documentState.endElement(URIConst.W3C_2009_EXI_URI, "lesscommon");
      eventTypes = m_documentState.getNextEventTypes();
    }
    if ((delineation & EXIOptions.ADD_COMMON) != 0) {
      pos = 1 - pos_level1;
      eventType = eventTypes.item(pos);
      pos_level1 = 2;
      assert "common".equals(eventType.getName());
      m_scriber.writeEventType(eventType);
      m_documentState.startElement(pos, URIConst.W3C_2009_EXI_URI, "common");
      eventTypes = m_documentState.getNextEventTypes();
      int pos_level2 = 0;
      if (alignmentType == AlignmentType.compress) {
        eventType = eventTypes.item(pos_level2++);
        assert "compression".equals(eventType.getName());
        m_scriber.writeEventType(eventType);
        m_documentState.startElement(0, URIConst.W3C_2009_EXI_URI, "compression");
        eventTypes = m_documentState.getNextEventTypes();
        eventType = eventTypes.getEE();
        m_scriber.writeEventType(eventType);
        m_documentState.endElement(URIConst.W3C_2009_EXI_URI, "compression");
        eventTypes = m_documentState.getNextEventTypes();
      }
      if (options.isFragment()) {
        pos = 1 - pos_level2;
        eventType = eventTypes.item(pos);
        pos_level2 = 2;
        assert "fragment".equals(eventType.getName());
        m_scriber.writeEventType(eventType);
        m_documentState.startElement(pos, URIConst.W3C_2009_EXI_URI, "fragment");
        eventTypes = m_documentState.getNextEventTypes();
        eventType = eventTypes.getEE();
        m_scriber.writeEventType(eventType);
        m_documentState.endElement(URIConst.W3C_2009_EXI_URI, "fragment");
        eventTypes = m_documentState.getNextEventTypes();
      }
      final SchemaId schemaId;
      if (outputSchemaId && (schemaId = options.getSchemaId()) != null) {
        pos = 2 - pos_level2;
        eventType = eventTypes.item(pos);
        assert "schemaId".equals(eventType.getName());
        m_scriber.writeEventType(eventType);
        m_documentState.startElement(pos, URIConst.W3C_2009_EXI_URI, "schemaId");
        eventTypes = m_documentState.getNextEventTypes();
        String val;
        if ((val = schemaId.getValue()) == null) {
          eventType = eventTypes.item(0);
          assert eventType.itemType == EventCode.ITEM_SCHEMA_NIL;
          m_scriber.writeEventType(eventType);
          m_scriber.writeBoolean(true);
          m_documentState.nillify();
          eventTypes = m_documentState.getNextEventTypes();
        }
        else {
          eventType = eventTypes.item(1);
          assert eventType.itemType == EventCode.ITEM_SCHEMA_CH;
          m_scriber.writeEventType(eventType);
          m_scriber.getValueScriberByID(Scriber.CODEC_STRING).scribe(
              val, m_scribble, "schemaId", URIConst.W3C_2009_EXI_URI, ((EventTypeSchema)eventType).getSchemaSubstance());
          m_documentState.characters();
          eventTypes = m_documentState.getNextEventTypes();
        }
        eventType = eventTypes.getEE();
        m_scriber.writeEventType(eventType);
        m_documentState.endElement(URIConst.W3C_2009_EXI_URI, "schemaId");
        eventTypes = m_documentState.getNextEventTypes();
      }
      eventType = eventTypes.getEE();
      m_scriber.writeEventType(eventType);
      m_documentState.endElement(URIConst.W3C_2009_EXI_URI, "common");
      eventTypes = m_documentState.getNextEventTypes();
    }
    if (options.isStrict()) {
      pos = 2 - pos_level1;
      eventType = eventTypes.item(pos);
      pos_level1 = 3;
      assert "strict".equals(eventType.getName());
      m_scriber.writeEventType(eventType);
      m_documentState.startElement(pos, URIConst.W3C_2009_EXI_URI, "strict");
      eventTypes = m_documentState.getNextEventTypes();
      eventType = eventTypes.getEE();
      m_scriber.writeEventType(eventType);
      m_documentState.endElement(URIConst.W3C_2009_EXI_URI, "strict");
      eventTypes = m_documentState.getNextEventTypes();
    }
    eventType = eventTypes.getEE();
    m_scriber.writeEventType(eventType);
    m_documentState.endElement(URIConst.W3C_2009_EXI_URI, "header");
    eventTypes = m_documentState.getNextEventTypes();

    eventType = eventTypes.item(0);
    assert eventType.itemType == EventCode.ITEM_ED;
    m_scriber.writeEventType(eventType);
    m_documentState.endDocument();
    
    if (options.getAlignmentType() != AlignmentType.bitPacked)
      m_scriber.finish();
  
    return (BitOutputStream)m_scriber.getOutputStream();
  }

}
