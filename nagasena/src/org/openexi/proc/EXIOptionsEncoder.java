package org.openexi.proc;

import java.io.IOException;
import java.io.OutputStream;

import org.openexi.schema.EXISchema;
import org.openexi.schema.EXISchemaConst;
import org.openexi.proc.common.AlignmentType;
import org.openexi.proc.common.EXIOptions;
import org.openexi.proc.common.EXIOptionsException;
import org.openexi.proc.common.EventType;
import org.openexi.proc.common.EventTypeList;
import org.openexi.proc.common.GrammarOptions;
import org.openexi.proc.common.QName;
import org.openexi.proc.common.SchemaId;
import org.openexi.proc.common.StringTable;
import org.openexi.proc.grammars.GrammarCache;
import org.openexi.proc.grammars.OptionsGrammarCache;
import org.openexi.proc.io.BitOutputStream;
import org.openexi.proc.io.BitPackedScriber;
import org.openexi.proc.io.IntegerValueScriber;
import org.openexi.proc.io.Scribble;
import org.openexi.proc.io.Scriber;
import org.openexi.proc.io.ScriberFactory;
import org.openexi.proc.util.ExiUriConst;

public final class EXIOptionsEncoder {
  
  private final GrammarCache m_grammarCache;
  private final BitPackedScriber m_scriber;
  private final Scribble m_scribble;
  
  private final EXISchema m_schema;
  private final int m_unsignedIntType;
  private final IntegerValueScriber m_valueScriberUnsignedInt;

  public EXIOptionsEncoder() {
    m_grammarCache = OptionsGrammarCache.getGrammarCache();
    m_schema = m_grammarCache.getEXISchema();
    final StringTable stringTable = Scriber.createStringTable(m_grammarCache);
    m_scriber = ScriberFactory.createHeaderOptionsScriber(); 
    m_scriber.setSchema(m_grammarCache.getEXISchema(), (QName[])null, 0);
    m_scriber.setPreserveNS(GrammarOptions.hasNS(m_grammarCache.grammarOptions));
    m_scriber.setStringTable(stringTable);
    m_scriber.setValueMaxLength(EXIOptions.VALUE_MAX_LENGTH_UNBOUNDED);
    m_scribble = new Scribble();
    m_unsignedIntType = m_schema.getBuiltinTypeOfSchema(EXISchemaConst.UNSIGNED_INT_TYPE);
    m_valueScriberUnsignedInt = (IntegerValueScriber)m_scriber.getValueScriber(m_unsignedIntType);
  }
  
  public BitOutputStream encode(EXIOptions options, boolean outputSchemaId, boolean observeC14N, OutputStream ostream) throws IOException, EXIOptionsException {
    m_scriber.reset();
    m_scriber.setOutputStream(ostream);

    m_grammarCache.retrieveRootGrammar(false, m_scriber.eventTypesWorkSpace).init(m_scriber.currentState);

    EventTypeList eventTypes;
    EventType eventType;

    eventTypes = m_scriber.getNextEventTypes();
    
    eventType = eventTypes.item(0);
    assert eventType.itemType == EventType.ITEM_SD && eventTypes.getLength() == 1;
    m_scriber.writeEventType(eventType);
    m_scriber.startDocument();
    eventTypes = m_scriber.getNextEventTypes();

    eventType = eventTypes.item(0);
    assert "header".equals(eventType.name);
    m_scriber.writeEventType(eventType);
    m_scriber.startElement(eventType);
    eventTypes = m_scriber.getNextEventTypes();

    int pos;
    int pos_level1 = 0;
    
    // schemaId is required by C14N EXI
    if (!outputSchemaId && observeC14N)
      outputSchemaId = true;
    
    final AlignmentType alignmentType = options.getAlignmentType();
    int delineation = options.getOutline(outputSchemaId);
    if ((delineation & EXIOptions.ADD_LESSCOMMON) != 0) {
      eventType = eventTypes.item(pos_level1++);
      assert "lesscommon".equals(eventType.name);
      m_scriber.writeEventType(eventType);
      m_scriber.startElement(eventType);
      eventTypes = m_scriber.getNextEventTypes();
      int pos_level2 = 0;
      if ((delineation & EXIOptions.ADD_UNCOMMON) != 0) {
        eventType = eventTypes.item(pos_level2++);
        assert "uncommon".equals(eventType.name);
        m_scriber.writeEventType(eventType);
        m_scriber.startElement(eventType);
        eventTypes = m_scriber.getNextEventTypes();
        int pos_level3 = 0;
        if ((delineation & EXIOptions.ADD_ALIGNMENT) != 0) {
          eventType = eventTypes.item(pos_level3++);
          assert "alignment".equals(eventType.name);
          m_scriber.writeEventType(eventType);
          m_scriber.startElement(eventType);
          eventTypes = m_scriber.getNextEventTypes();
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
          assert name.equals(eventType.name);
          m_scriber.writeEventType(eventType);
          m_scriber.startElement(eventType);
          eventTypes = m_scriber.getNextEventTypes();
          eventType = eventTypes.item(0);
          assert eventType.itemType == EventType.ITEM_EE;
          m_scriber.writeEventType(eventType);
          m_scriber.endElement();
          eventTypes = m_scriber.getNextEventTypes();
          eventType = eventTypes.getEE();
          m_scriber.writeEventType(eventType);
          m_scriber.endElement();
          eventTypes = m_scriber.getNextEventTypes();
        }
        if ((delineation & EXIOptions.ADD_VALUE_MAX_LENGTH) != 0) {
          pos = 2 - pos_level3;
          eventType = eventTypes.item(pos);
          pos_level3 = 3;
          assert "valueMaxLength".equals(eventType.name);
          m_scriber.writeEventType(eventType);
          final int valueMaxLengthId = eventType.getNameId();
          m_scriber.startElement(eventType);
          eventTypes = m_scriber.getNextEventTypes();
          eventType = eventTypes.item(0);
          assert eventType.itemType == EventType.ITEM_SCHEMA_CH;
          m_valueScriberUnsignedInt.processUnsignedInt(options.getValueMaxLength(), m_scribble);
          m_valueScriberUnsignedInt.scribe((String)null, m_scribble, valueMaxLengthId,  
              ExiUriConst.W3C_2009_EXI_URI_ID, m_unsignedIntType, m_scriber);
          m_scriber.characters(eventType);
          eventTypes = m_scriber.getNextEventTypes();
          eventType = eventTypes.getEE();
          m_scriber.writeEventType(eventType);
          m_scriber.endElement();
          eventTypes = m_scriber.getNextEventTypes();
        }
        if ((delineation & EXIOptions.ADD_VALUE_PARTITION_CAPACITY) != 0) {
          pos = 3 - pos_level3;
          eventType = eventTypes.item(pos);
          pos_level3 = 4;
          assert "valuePartitionCapacity".equals(eventType.name);
          m_scriber.writeEventType(eventType);
          final int valuePartitionCapacityId = eventType.getNameId();
          m_scriber.startElement(eventType);
          eventTypes = m_scriber.getNextEventTypes();
          eventType = eventTypes.item(0);
          assert eventType.itemType == EventType.ITEM_SCHEMA_CH;
          m_valueScriberUnsignedInt.processUnsignedInt(options.getValuePartitionCapacity(), m_scribble);
          m_valueScriberUnsignedInt.scribe((String)null, m_scribble, valuePartitionCapacityId,  
              ExiUriConst.W3C_2009_EXI_URI_ID, m_unsignedIntType, m_scriber);
          m_scriber.characters(eventType);
          eventTypes = m_scriber.getNextEventTypes();
          eventType = eventTypes.getEE();
          m_scriber.writeEventType(eventType);
          m_scriber.endElement();
          eventTypes = m_scriber.getNextEventTypes();
        }
        if ((delineation & EXIOptions.ADD_DTRM) != 0) {
          pos = 4 - pos_level3;
          pos_level3 = 5;
          final QName[] dtrm = options.getDatatypeRepresentationMap();
          final int n_dtrmItems = options.getDatatypeRepresentationMapBindingsCount();
          for (int i = 0; i < n_dtrmItems; i++, pos = 0) {
            QName qname;
            eventType = eventTypes.item(pos);
            assert "datatypeRepresentationMap".equals(eventType.name);
            m_scriber.writeEventType(eventType);
            m_scriber.startElement(eventType);
            eventTypes = m_scriber.getNextEventTypes();
            qname = dtrm[i << 1];
            eventType = eventTypes.item(0);
            assert eventTypes.getLength() == 1 && eventType.itemType == EventType.ITEM_SCHEMA_WC_ANY;
            m_scriber.writeEventType(eventType);
            m_scriber.writeQName(qname, eventType);
            m_scriber.startWildcardElement(0, qname.uriId, qname.localNameId);
            eventTypes = m_scriber.getNextEventTypes();
            eventType = eventTypes.getEE();
            m_scriber.writeEventType(eventType);
            m_scriber.endElement();
            eventTypes = m_scriber.getNextEventTypes();
            qname = dtrm[(i << 1) + 1];
            eventType = eventTypes.item(0);
            assert eventTypes.getLength() == 1 && eventType.itemType == EventType.ITEM_SCHEMA_WC_ANY;
            m_scriber.writeEventType(eventType);
            m_scriber.writeQName(qname, eventType);
            m_scriber.startWildcardElement(0, qname.uriId, qname.localNameId);
            eventTypes = m_scriber.getNextEventTypes();
            eventType = eventTypes.getEE();
            m_scriber.writeEventType(eventType);
            m_scriber.endElement();
            eventTypes = m_scriber.getNextEventTypes();
            eventType = eventTypes.getEE();
            m_scriber.writeEventType(eventType);
            m_scriber.endElement();
            eventTypes = m_scriber.getNextEventTypes();
          }
        }
        eventType = eventTypes.getEE();
        m_scriber.writeEventType(eventType);
        m_scriber.endElement();
        eventTypes = m_scriber.getNextEventTypes();
      }
      if ((delineation & EXIOptions.ADD_PRESERVE) != 0) {
        pos = 1 - pos_level2;
        eventType = eventTypes.item(pos);
        pos_level2 = 2;
        assert "preserve".equals(eventType.name);
        m_scriber.writeEventType(eventType);
        m_scriber.startElement(eventType);
        eventTypes = m_scriber.getNextEventTypes();
        int pos_level3 = 0;
        if (options.getPreserveDTD()) {
          eventType = eventTypes.item(pos_level3++);
          assert "dtd".equals(eventType.name);
          m_scriber.writeEventType(eventType);
          m_scriber.startElement(eventType);
          eventTypes = m_scriber.getNextEventTypes();
          eventType = eventTypes.getEE();
          m_scriber.writeEventType(eventType);
          m_scriber.endElement();
          eventTypes = m_scriber.getNextEventTypes();
        }
        if (options.getPreserveNS()) {
          pos = 1 - pos_level3;
          eventType = eventTypes.item(pos);
          pos_level3 = 2;
          assert "prefixes".equals(eventType.name);
          m_scriber.writeEventType(eventType);
          m_scriber.startElement(eventType);
          eventTypes = m_scriber.getNextEventTypes();
          eventType = eventTypes.getEE();
          m_scriber.writeEventType(eventType);
          m_scriber.endElement();
          eventTypes = m_scriber.getNextEventTypes();
        }
        if (options.getPreserveLexicalValues()) {
          pos = 2 - pos_level3;
          eventType = eventTypes.item(pos);
          pos_level3 = 3;
          assert "lexicalValues".equals(eventType.name);
          m_scriber.writeEventType(eventType);
          m_scriber.startElement(eventType);
          eventTypes = m_scriber.getNextEventTypes();
          eventType = eventTypes.getEE();
          m_scriber.writeEventType(eventType);
          m_scriber.endElement();
          eventTypes = m_scriber.getNextEventTypes();
        }
        /**
         * REVISIT: write out <lexicalValues/> if necessary
         */
        if (options.getPreserveComments()) {
          pos = 3 - pos_level3;
          eventType = eventTypes.item(pos);
          pos_level3 = 4;
          assert "comments".equals(eventType.name);
          m_scriber.writeEventType(eventType);
          m_scriber.startElement(eventType);
          eventTypes = m_scriber.getNextEventTypes();
          eventType = eventTypes.getEE();
          m_scriber.writeEventType(eventType);
          m_scriber.endElement();
          eventTypes = m_scriber.getNextEventTypes();
        }
        if (options.getPreservePIs()) {
          pos = 4 - pos_level3;
          eventType = eventTypes.item(pos);
          pos_level3 = 5;
          assert "pis".equals(eventType.name);
          m_scriber.writeEventType(eventType);
          m_scriber.startElement(eventType);
          eventTypes = m_scriber.getNextEventTypes();
          eventType = eventTypes.getEE();
          m_scriber.writeEventType(eventType);
          m_scriber.endElement();
          eventTypes = m_scriber.getNextEventTypes();
        }
        eventType = eventTypes.getEE();
        m_scriber.writeEventType(eventType);
        m_scriber.endElement();
        eventTypes = m_scriber.getNextEventTypes();
      }
      int blockSize;
      if ((blockSize = options.getBlockSize()) != EXIOptions.BLOCKSIZE_DEFAULT && 
          (alignmentType == AlignmentType.compress || alignmentType == AlignmentType.preCompress)) {
        pos = 2 - pos_level2;
        eventType = eventTypes.item(pos);
        pos_level2 = 3;
        assert "blockSize".equals(eventType.name);
        m_scriber.writeEventType(eventType);
        final int blockSizeId = eventType.getNameId();
        m_scriber.startElement(eventType);
        eventTypes = m_scriber.getNextEventTypes();
        eventType = eventTypes.item(0);
        assert eventType.itemType == EventType.ITEM_SCHEMA_CH;
        m_valueScriberUnsignedInt.processUnsignedInt(blockSize, m_scribble);
        m_valueScriberUnsignedInt.scribe((String)null, m_scribble, blockSizeId,  
            ExiUriConst.W3C_2009_EXI_URI_ID, m_unsignedIntType, m_scriber);
        m_scriber.characters(eventType);
        eventTypes = m_scriber.getNextEventTypes();
        eventType = eventTypes.getEE();
        m_scriber.writeEventType(eventType);
        m_scriber.endElement();
        eventTypes = m_scriber.getNextEventTypes();
      }
      eventType = eventTypes.getEE();
      m_scriber.writeEventType(eventType);
      m_scriber.endElement();
      eventTypes = m_scriber.getNextEventTypes();
    }
    if ((delineation & EXIOptions.ADD_COMMON) != 0) {
      pos = 1 - pos_level1;
      eventType = eventTypes.item(pos);
      pos_level1 = 2;
      assert "common".equals(eventType.name);
      m_scriber.writeEventType(eventType);
      m_scriber.startElement(eventType);
      eventTypes = m_scriber.getNextEventTypes();
      int pos_level2 = 0;
      if (alignmentType == AlignmentType.compress) {
        eventType = eventTypes.item(pos_level2++);
        assert "compression".equals(eventType.name);
        m_scriber.writeEventType(eventType);
        m_scriber.startElement(eventType);
        eventTypes = m_scriber.getNextEventTypes();
        eventType = eventTypes.getEE();
        m_scriber.writeEventType(eventType);
        m_scriber.endElement();
        eventTypes = m_scriber.getNextEventTypes();
      }
      if (options.isFragment()) {
        pos = 1 - pos_level2;
        eventType = eventTypes.item(pos);
        pos_level2 = 2;
        assert "fragment".equals(eventType.name);
        m_scriber.writeEventType(eventType);
        m_scriber.startElement(eventType);
        eventTypes = m_scriber.getNextEventTypes();
        eventType = eventTypes.getEE();
        m_scriber.writeEventType(eventType);
        m_scriber.endElement();
        eventTypes = m_scriber.getNextEventTypes();
      }
      if (outputSchemaId) {
        final SchemaId schemaId;
        if ((schemaId = options.getSchemaId()) != null) {
          pos = 2 - pos_level2;
          eventType = eventTypes.item(pos);
          assert "schemaId".equals(eventType.name);
          m_scriber.writeEventType(eventType);
          final int schemaIdId = eventType.getNameId();
          m_scriber.startElement(eventType);
          eventTypes = m_scriber.getNextEventTypes();
          String val;
          if ((val = schemaId.getValue()) == null) {
            eventType = eventTypes.item(0);
            assert eventType.itemType == EventType.ITEM_SCHEMA_NIL;
            m_scriber.writeEventType(eventType);
            m_scriber.writeBoolean(true);
            m_scriber.nillify(eventType.getIndex());
            eventTypes = m_scriber.getNextEventTypes();
          }
          else {
            eventType = eventTypes.item(1);
            assert eventType.itemType == EventType.ITEM_SCHEMA_CH;
            m_scriber.writeEventType(eventType);
            m_scriber.getValueScriberByID(Scriber.CODEC_STRING).scribe(
                val, m_scribble, schemaIdId, ExiUriConst.W3C_2009_EXI_URI_ID, m_scriber.currentState.contentDatatype, m_scriber);
            m_scriber.characters(eventType);
            eventTypes = m_scriber.getNextEventTypes();
          }
          eventType = eventTypes.getEE();
          m_scriber.writeEventType(eventType);
          m_scriber.endElement();
          eventTypes = m_scriber.getNextEventTypes();
          
        }
        else {
          throw new EXIOptionsException("schemaId needs to be specified.");
        }
      }
      eventType = eventTypes.getEE();
      m_scriber.writeEventType(eventType);
      m_scriber.endElement();
      eventTypes = m_scriber.getNextEventTypes();
    }
    if (options.isStrict()) {
      pos = 2 - pos_level1;
      eventType = eventTypes.item(pos);
      pos_level1 = 3;
      assert "strict".equals(eventType.name);
      m_scriber.writeEventType(eventType);
      m_scriber.startElement(eventType);
      eventTypes = m_scriber.getNextEventTypes();
      eventType = eventTypes.getEE();
      m_scriber.writeEventType(eventType);
      m_scriber.endElement();
      eventTypes = m_scriber.getNextEventTypes();
    }
    eventType = eventTypes.getEE();
    m_scriber.writeEventType(eventType);
    m_scriber.endElement();
    eventTypes = m_scriber.getNextEventTypes();

    eventType = eventTypes.item(0);
    assert eventType.itemType == EventType.ITEM_ED;
    m_scriber.writeEventType(eventType);
    m_scriber.endDocument();
    
    if (options.getAlignmentType() != AlignmentType.bitPacked)
      m_scriber.finish();
  
    return m_scriber.getBitOutputStream();
  }

}
