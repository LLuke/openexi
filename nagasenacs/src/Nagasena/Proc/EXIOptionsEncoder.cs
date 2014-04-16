using System.Diagnostics;
using System.IO;

using EXISchema = Nagasena.Schema.EXISchema;
using EXISchemaConst = Nagasena.Schema.EXISchemaConst;
using AlignmentType = Nagasena.Proc.Common.AlignmentType;
using EXIOptions = Nagasena.Proc.Common.EXIOptions;
using EventType = Nagasena.Proc.Common.EventType;
using EventTypeList = Nagasena.Proc.Common.EventTypeList;
using GrammarOptions = Nagasena.Proc.Common.GrammarOptions;
using QName = Nagasena.Proc.Common.QName;
using SchemaId = Nagasena.Proc.Common.SchemaId;
using StringTable = Nagasena.Proc.Common.StringTable;
using GrammarCache = Nagasena.Proc.Grammars.GrammarCache;
using OptionsGrammarCache = Nagasena.Proc.Grammars.OptionsGrammarCache;
using BitOutputStream = Nagasena.Proc.IO.BitOutputStream;
using BitPackedScriber = Nagasena.Proc.IO.BitPackedScriber;
using IntegerValueScriber = Nagasena.Proc.IO.IntegerValueScriber;
using Scribble = Nagasena.Proc.IO.Scribble;
using Scriber = Nagasena.Proc.IO.Scriber;
using ScriberFactory = Nagasena.Proc.IO.ScriberFactory;
using ExiUriConst = Nagasena.Proc.Util.ExiUriConst;

namespace Nagasena.Proc {

  public sealed class EXIOptionsEncoder {

    private readonly GrammarCache m_grammarCache;
    private readonly BitPackedScriber m_scriber;
    private readonly Scribble m_scribble;

    private readonly EXISchema m_schema;
    private readonly int m_unsignedIntType;
    private readonly IntegerValueScriber m_valueScriberUnsignedInt;

    public EXIOptionsEncoder() {
      m_grammarCache = OptionsGrammarCache.GrammarCache;
      m_schema = m_grammarCache.EXISchema;
      StringTable stringTable = Scriber.createStringTable(m_grammarCache);
      m_scriber = ScriberFactory.createHeaderOptionsScriber();
      m_scriber.setSchema(m_grammarCache.EXISchema, (QName[])null, 0);
      m_scriber.PreserveNS = GrammarOptions.hasNS(m_grammarCache.grammarOptions);
      m_scriber.StringTable = stringTable;
      m_scriber.ValueMaxLength = EXIOptions.VALUE_MAX_LENGTH_UNBOUNDED;
      m_scribble = new Scribble();
      m_unsignedIntType = m_schema.getBuiltinTypeOfSchema(EXISchemaConst.UNSIGNED_INT_TYPE);
      m_valueScriberUnsignedInt = (IntegerValueScriber)m_scriber.getValueScriber(m_unsignedIntType);
    }

    public BitOutputStream encode(EXIOptions options, bool outputSchemaId, Stream ostream) {
      m_scriber.reset();
      m_scriber.OutputStream = ostream;

      m_grammarCache.retrieveRootGrammar(false, m_scriber.eventTypesWorkSpace).init(m_scriber.currentState);

      EventTypeList eventTypes;
      EventType eventType;

      eventTypes = m_scriber.NextEventTypes;

      eventType = eventTypes.item(0);
      Debug.Assert(eventType.itemType == EventType.ITEM_SD && eventTypes.Length == 1);
      m_scriber.writeEventType(eventType);
      m_scriber.startDocument();
      eventTypes = m_scriber.NextEventTypes;

      eventType = eventTypes.item(0);
      Debug.Assert("header".Equals(eventType.name));
      m_scriber.writeEventType(eventType);
      m_scriber.startElement(eventType);
      eventTypes = m_scriber.NextEventTypes;

      int pos;
      int pos_level1 = 0;

      AlignmentType alignmentType = options.AlignmentType;
      int delineation = options.getOutline(outputSchemaId);
      if ((delineation & EXIOptions.ADD_LESSCOMMON) != 0) {
        eventType = eventTypes.item(pos_level1++);
        Debug.Assert("lesscommon".Equals(eventType.name));
        m_scriber.writeEventType(eventType);
        m_scriber.startElement(eventType);
        eventTypes = m_scriber.NextEventTypes;
        int pos_level2 = 0;
        if ((delineation & EXIOptions.ADD_UNCOMMON) != 0) {
          eventType = eventTypes.item(pos_level2++);
          Debug.Assert("uncommon".Equals(eventType.name));
          m_scriber.writeEventType(eventType);
          m_scriber.startElement(eventType);
          eventTypes = m_scriber.NextEventTypes;
          int pos_level3 = 0;
          if ((delineation & EXIOptions.ADD_ALIGNMENT) != 0) {
            eventType = eventTypes.item(pos_level3++);
            Debug.Assert("alignment".Equals(eventType.name));
            m_scriber.writeEventType(eventType);
            m_scriber.startElement(eventType);
            eventTypes = m_scriber.NextEventTypes;
            string name;
            if (alignmentType == AlignmentType.byteAligned) {
              pos = 0;
              name = "byte";
            }
            else {
              Debug.Assert(alignmentType == AlignmentType.preCompress);
              pos = 1;
              name = "pre-compress";
            }
            eventType = eventTypes.item(pos);
            Debug.Assert(name.Equals(eventType.name));
            m_scriber.writeEventType(eventType);
            m_scriber.startElement(eventType);
            eventTypes = m_scriber.NextEventTypes;
            eventType = eventTypes.item(0);
            Debug.Assert(eventType.itemType == EventType.ITEM_EE);
            m_scriber.writeEventType(eventType);
            m_scriber.endElement();
            eventTypes = m_scriber.NextEventTypes;
            eventType = eventTypes.EE;
            m_scriber.writeEventType(eventType);
            m_scriber.endElement();
            eventTypes = m_scriber.NextEventTypes;
          }
          if ((delineation & EXIOptions.ADD_VALUE_MAX_LENGTH) != 0) {
            pos = 2 - pos_level3;
            eventType = eventTypes.item(pos);
            pos_level3 = 3;
            Debug.Assert("valueMaxLength".Equals(eventType.name));
            m_scriber.writeEventType(eventType);
            int valueMaxLengthId = eventType.NameId;
            m_scriber.startElement(eventType);
            eventTypes = m_scriber.NextEventTypes;
            eventType = eventTypes.item(0);
            Debug.Assert(eventType.itemType == EventType.ITEM_SCHEMA_CH);
            m_valueScriberUnsignedInt.processUnsignedInt(options.ValueMaxLength, m_scribble);
            m_valueScriberUnsignedInt.scribe((string)null, m_scribble, valueMaxLengthId, ExiUriConst.W3C_2009_EXI_URI_ID, m_unsignedIntType, m_scriber);
            m_scriber.characters(eventType);
            eventTypes = m_scriber.NextEventTypes;
            eventType = eventTypes.EE;
            m_scriber.writeEventType(eventType);
            m_scriber.endElement();
            eventTypes = m_scriber.NextEventTypes;
          }
          if ((delineation & EXIOptions.ADD_VALUE_PARTITION_CAPACITY) != 0) {
            pos = 3 - pos_level3;
            eventType = eventTypes.item(pos);
            pos_level3 = 4;
            Debug.Assert("valuePartitionCapacity".Equals(eventType.name));
            m_scriber.writeEventType(eventType);
            int valuePartitionCapacityId = eventType.NameId;
            m_scriber.startElement(eventType);
            eventTypes = m_scriber.NextEventTypes;
            eventType = eventTypes.item(0);
            Debug.Assert(eventType.itemType == EventType.ITEM_SCHEMA_CH);
            m_valueScriberUnsignedInt.processUnsignedInt(options.ValuePartitionCapacity, m_scribble);
            m_valueScriberUnsignedInt.scribe((string)null, m_scribble, valuePartitionCapacityId, ExiUriConst.W3C_2009_EXI_URI_ID, m_unsignedIntType, m_scriber);
            m_scriber.characters(eventType);
            eventTypes = m_scriber.NextEventTypes;
            eventType = eventTypes.EE;
            m_scriber.writeEventType(eventType);
            m_scriber.endElement();
            eventTypes = m_scriber.NextEventTypes;
          }
          if ((delineation & EXIOptions.ADD_DTRM) != 0) {
            pos = 4 - pos_level3;
            pos_level3 = 5;
            QName[] dtrm = options.DatatypeRepresentationMap;
            int n_dtrmItems = options.DatatypeRepresentationMapBindingsCount;
            for (int i = 0; i < n_dtrmItems; i++, pos = 0) {
              QName qname;
              eventType = eventTypes.item(pos);
              Debug.Assert("datatypeRepresentationMap".Equals(eventType.name));
              m_scriber.writeEventType(eventType);
              m_scriber.startElement(eventType);
              eventTypes = m_scriber.NextEventTypes;
              qname = dtrm[i << 1];
              eventType = eventTypes.item(0);
              Debug.Assert(eventTypes.Length == 1 && eventType.itemType == EventType.ITEM_SCHEMA_WC_ANY);
              m_scriber.writeEventType(eventType);
              m_scriber.writeQName(qname, eventType);
              m_scriber.startWildcardElement(0, qname.uriId, qname.localNameId);
              eventTypes = m_scriber.NextEventTypes;
              eventType = eventTypes.EE;
              m_scriber.writeEventType(eventType);
              m_scriber.endElement();
              eventTypes = m_scriber.NextEventTypes;
              qname = dtrm[(i << 1) + 1];
              eventType = eventTypes.item(0);
              Debug.Assert(eventTypes.Length == 1 && eventType.itemType == EventType.ITEM_SCHEMA_WC_ANY);
              m_scriber.writeEventType(eventType);
              m_scriber.writeQName(qname, eventType);
              m_scriber.startWildcardElement(0, qname.uriId, qname.localNameId);
              eventTypes = m_scriber.NextEventTypes;
              eventType = eventTypes.EE;
              m_scriber.writeEventType(eventType);
              m_scriber.endElement();
              eventTypes = m_scriber.NextEventTypes;
              eventType = eventTypes.EE;
              m_scriber.writeEventType(eventType);
              m_scriber.endElement();
              eventTypes = m_scriber.NextEventTypes;
            }
          }
          eventType = eventTypes.EE;
          m_scriber.writeEventType(eventType);
          m_scriber.endElement();
          eventTypes = m_scriber.NextEventTypes;
        }
        if ((delineation & EXIOptions.ADD_PRESERVE) != 0) {
          pos = 1 - pos_level2;
          eventType = eventTypes.item(pos);
          pos_level2 = 2;
          Debug.Assert("preserve".Equals(eventType.name));
          m_scriber.writeEventType(eventType);
          m_scriber.startElement(eventType);
          eventTypes = m_scriber.NextEventTypes;
          int pos_level3 = 0;
          if (options.PreserveDTD) {
            eventType = eventTypes.item(pos_level3++);
            Debug.Assert("dtd".Equals(eventType.name));
            m_scriber.writeEventType(eventType);
            m_scriber.startElement(eventType);
            eventTypes = m_scriber.NextEventTypes;
            eventType = eventTypes.EE;
            m_scriber.writeEventType(eventType);
            m_scriber.endElement();
            eventTypes = m_scriber.NextEventTypes;
          }
          if (options.PreserveNS) {
            pos = 1 - pos_level3;
            eventType = eventTypes.item(pos);
            pos_level3 = 2;
            Debug.Assert("prefixes".Equals(eventType.name));
            m_scriber.writeEventType(eventType);
            m_scriber.startElement(eventType);
            eventTypes = m_scriber.NextEventTypes;
            eventType = eventTypes.EE;
            m_scriber.writeEventType(eventType);
            m_scriber.endElement();
            eventTypes = m_scriber.NextEventTypes;
          }
          if (options.PreserveLexicalValues) {
            pos = 2 - pos_level3;
            eventType = eventTypes.item(pos);
            pos_level3 = 3;
            Debug.Assert("lexicalValues".Equals(eventType.name));
            m_scriber.writeEventType(eventType);
            m_scriber.startElement(eventType);
            eventTypes = m_scriber.NextEventTypes;
            eventType = eventTypes.EE;
            m_scriber.writeEventType(eventType);
            m_scriber.endElement();
            eventTypes = m_scriber.NextEventTypes;
          }
          /// REVISIT: write out <lexicalValues/> if necessary
          if (options.PreserveComments) {
            pos = 3 - pos_level3;
            eventType = eventTypes.item(pos);
            pos_level3 = 4;
            Debug.Assert("comments".Equals(eventType.name));
            m_scriber.writeEventType(eventType);
            m_scriber.startElement(eventType);
            eventTypes = m_scriber.NextEventTypes;
            eventType = eventTypes.EE;
            m_scriber.writeEventType(eventType);
            m_scriber.endElement();
            eventTypes = m_scriber.NextEventTypes;
          }
          if (options.PreservePIs) {
            pos = 4 - pos_level3;
            eventType = eventTypes.item(pos);
            pos_level3 = 5;
            Debug.Assert("pis".Equals(eventType.name));
            m_scriber.writeEventType(eventType);
            m_scriber.startElement(eventType);
            eventTypes = m_scriber.NextEventTypes;
            eventType = eventTypes.EE;
            m_scriber.writeEventType(eventType);
            m_scriber.endElement();
            eventTypes = m_scriber.NextEventTypes;
          }
          eventType = eventTypes.EE;
          m_scriber.writeEventType(eventType);
          m_scriber.endElement();
          eventTypes = m_scriber.NextEventTypes;
        }
        int blockSize;
        if ((blockSize = options.BlockSize) != EXIOptions.BLOCKSIZE_DEFAULT) {
          pos = 2 - pos_level2;
          eventType = eventTypes.item(pos);
          pos_level2 = 3;
          Debug.Assert("blockSize".Equals(eventType.name));
          m_scriber.writeEventType(eventType);
          int blockSizeId = eventType.NameId;
          m_scriber.startElement(eventType);
          eventTypes = m_scriber.NextEventTypes;
          eventType = eventTypes.item(0);
          Debug.Assert(eventType.itemType == EventType.ITEM_SCHEMA_CH);
          m_valueScriberUnsignedInt.processUnsignedInt(blockSize, m_scribble);
          m_valueScriberUnsignedInt.scribe((string)null, m_scribble, blockSizeId, ExiUriConst.W3C_2009_EXI_URI_ID, m_unsignedIntType, m_scriber);
          m_scriber.characters(eventType);
          eventTypes = m_scriber.NextEventTypes;
          eventType = eventTypes.EE;
          m_scriber.writeEventType(eventType);
          m_scriber.endElement();
          eventTypes = m_scriber.NextEventTypes;
        }
        eventType = eventTypes.EE;
        m_scriber.writeEventType(eventType);
        m_scriber.endElement();
        eventTypes = m_scriber.NextEventTypes;
      }
      if ((delineation & EXIOptions.ADD_COMMON) != 0) {
        pos = 1 - pos_level1;
        eventType = eventTypes.item(pos);
        pos_level1 = 2;
        Debug.Assert("common".Equals(eventType.name));
        m_scriber.writeEventType(eventType);
        m_scriber.startElement(eventType);
        eventTypes = m_scriber.NextEventTypes;
        int pos_level2 = 0;
        if (alignmentType == AlignmentType.compress) {
          eventType = eventTypes.item(pos_level2++);
          Debug.Assert("compression".Equals(eventType.name));
          m_scriber.writeEventType(eventType);
          m_scriber.startElement(eventType);
          eventTypes = m_scriber.NextEventTypes;
          eventType = eventTypes.EE;
          m_scriber.writeEventType(eventType);
          m_scriber.endElement();
          eventTypes = m_scriber.NextEventTypes;
        }
        if (options.Fragment) {
          pos = 1 - pos_level2;
          eventType = eventTypes.item(pos);
          pos_level2 = 2;
          Debug.Assert("fragment".Equals(eventType.name));
          m_scriber.writeEventType(eventType);
          m_scriber.startElement(eventType);
          eventTypes = m_scriber.NextEventTypes;
          eventType = eventTypes.EE;
          m_scriber.writeEventType(eventType);
          m_scriber.endElement();
          eventTypes = m_scriber.NextEventTypes;
        }
        SchemaId schemaId;
        if (outputSchemaId && (schemaId = options.SchemaId) != null) {
          pos = 2 - pos_level2;
          eventType = eventTypes.item(pos);
          Debug.Assert("schemaId".Equals(eventType.name));
          m_scriber.writeEventType(eventType);
          int schemaIdId = eventType.NameId;
          m_scriber.startElement(eventType);
          eventTypes = m_scriber.NextEventTypes;
          string val;
          if ((val = schemaId.Value) == null) {
            eventType = eventTypes.item(0);
            Debug.Assert(eventType.itemType == EventType.ITEM_SCHEMA_NIL);
            m_scriber.writeEventType(eventType);
            m_scriber.writeBoolean(true);
            m_scriber.nillify(eventType.Index);
            eventTypes = m_scriber.NextEventTypes;
          }
          else {
            eventType = eventTypes.item(1);
            Debug.Assert(eventType.itemType == EventType.ITEM_SCHEMA_CH);
            m_scriber.writeEventType(eventType);
            m_scriber.getValueScriberByID(Scriber.CODEC_STRING).scribe(val, m_scribble, schemaIdId, ExiUriConst.W3C_2009_EXI_URI_ID, m_scriber.currentState.contentDatatype, m_scriber);
            m_scriber.characters(eventType);
            eventTypes = m_scriber.NextEventTypes;
          }
          eventType = eventTypes.EE;
          m_scriber.writeEventType(eventType);
          m_scriber.endElement();
          eventTypes = m_scriber.NextEventTypes;
        }
        eventType = eventTypes.EE;
        m_scriber.writeEventType(eventType);
        m_scriber.endElement();
        eventTypes = m_scriber.NextEventTypes;
      }
      if (options.Strict) {
        pos = 2 - pos_level1;
        eventType = eventTypes.item(pos);
        pos_level1 = 3;
        Debug.Assert("strict".Equals(eventType.name));
        m_scriber.writeEventType(eventType);
        m_scriber.startElement(eventType);
        eventTypes = m_scriber.NextEventTypes;
        eventType = eventTypes.EE;
        m_scriber.writeEventType(eventType);
        m_scriber.endElement();
        eventTypes = m_scriber.NextEventTypes;
      }
      eventType = eventTypes.EE;
      m_scriber.writeEventType(eventType);
      m_scriber.endElement();
      eventTypes = m_scriber.NextEventTypes;

      eventType = eventTypes.item(0);
      Debug.Assert(eventType.itemType == EventType.ITEM_ED);
      m_scriber.writeEventType(eventType);
      m_scriber.endDocument();

      if (options.AlignmentType != AlignmentType.bitPacked) {
        m_scriber.finish();
      }

      return m_scriber.BitOutputStream;
    }

  }

}