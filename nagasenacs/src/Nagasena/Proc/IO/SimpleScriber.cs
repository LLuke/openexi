using System.Diagnostics;
using System.IO;

using EventCode = Nagasena.Proc.Common.EventCode;
using EventType = Nagasena.Proc.Common.EventType;
using QName = Nagasena.Proc.Common.QName;
using StringTable = Nagasena.Proc.Common.StringTable;
using EventCodeTuple = Nagasena.Proc.Grammars.EventCodeTuple;
using Characters = Nagasena.Schema.Characters;
using EXISchema = Nagasena.Schema.EXISchema;

namespace Nagasena.Proc.IO {

  internal abstract class SimpleScriber : Scriber {

    internal SimpleScriber(bool isForEXIOptions) : base(isForEXIOptions) {

      m_valueScriberTable[CODEC_STRING] = m_stringValueScriberInherent;
      m_valueScriberTable[CODEC_BOOLEAN] = m_booleanValueScriberInherent;
      m_valueScriberTable[CODEC_ENUMERATION] = m_enumerationValueScriberInherent;
      m_valueScriberTable[CODEC_LIST] = m_listValueScriberInherent;
      m_valueScriberTable[CODEC_DECIMAL] = m_decimalValueScriberInherent;
      m_valueScriberTable[CODEC_DATETIME] = m_dateTimeValueScriberInherent;
      m_valueScriberTable[CODEC_TIME] = m_timeValueScriberInherent;
      m_valueScriberTable[CODEC_DATE] = m_dateValueScriberInherent;
      m_valueScriberTable[CODEC_GDAY] = m_gDayValueScriberInherent;
      m_valueScriberTable[CODEC_GMONTH] = m_gMonthValueScriberInherent;
      m_valueScriberTable[CODEC_GMONTHDAY] = m_gMonthDayValueScriberInherent;
      m_valueScriberTable[CODEC_GYEAR] = m_gYearValueScriberInherent;
      m_valueScriberTable[CODEC_GYEARMONTH] = m_gYearMonthValueScriberInherent;
      m_valueScriberTable[CODEC_DOUBLE] = m_floatValueScriberInherent;
      m_valueScriberTable[CODEC_INTEGER] = m_integerValueScriberInherent;
      m_valueScriberTable[CODEC_BASE64BINARY] = m_base64BinaryValueScriberInherent;
      m_valueScriberTable[CODEC_HEXBINARY] = m_hexBinaryValueScriberInherent;
    }

    public override int BlockSize {
      set {
        // Do nothing.
      }
    }

    public override bool PreserveLexicalValues {
      set {
        bool prevPreserveLexicalValues = m_preserveLexicalValues;
        base.PreserveLexicalValues = value;
        if (prevPreserveLexicalValues != value) {
          if (value) {
            m_valueScriberTable[CODEC_STRING] = m_stringValueScriberLexical;
            m_valueScriberTable[CODEC_BOOLEAN] = m_booleanValueScriberLexical;
            m_valueScriberTable[CODEC_ENUMERATION] = m_enumerationValueScriberLexical;
            m_valueScriberTable[CODEC_LIST] = m_listValueScriberLexical;
            m_valueScriberTable[CODEC_DECIMAL] = m_decimalValueScriberLexical;
            m_valueScriberTable[CODEC_DATETIME] = m_dateTimeValueScriberLexical;
            m_valueScriberTable[CODEC_TIME] = m_timeValueScriberLexical;
            m_valueScriberTable[CODEC_DATE] = m_dateValueScriberLexical;
            m_valueScriberTable[CODEC_GDAY] = m_gDayValueScriberLexical;
            m_valueScriberTable[CODEC_GMONTH] = m_gMonthValueScriberLexical;
            m_valueScriberTable[CODEC_GMONTHDAY] = m_gMonthDayValueScriberLexical;
            m_valueScriberTable[CODEC_GYEAR] = m_gYearValueScriberLexical;
            m_valueScriberTable[CODEC_GYEARMONTH] = m_gYearMonthValueScriberLexical;
            m_valueScriberTable[CODEC_DOUBLE] = m_floatValueScriberLexical;
            m_valueScriberTable[CODEC_INTEGER] = m_integerValueScriberLexical;
            m_valueScriberTable[CODEC_BASE64BINARY] = m_base64BinaryValueScriberLexical;
            m_valueScriberTable[CODEC_HEXBINARY] = m_hexBinaryValueScriberLexical;
          }
          else {
            m_valueScriberTable[CODEC_STRING] = m_stringValueScriberInherent;
            m_valueScriberTable[CODEC_BOOLEAN] = m_booleanValueScriberInherent;
            m_valueScriberTable[CODEC_ENUMERATION] = m_enumerationValueScriberInherent;
            m_valueScriberTable[CODEC_LIST] = m_listValueScriberInherent;
            m_valueScriberTable[CODEC_DECIMAL] = m_decimalValueScriberInherent;
            m_valueScriberTable[CODEC_DATETIME] = m_dateTimeValueScriberInherent;
            m_valueScriberTable[CODEC_TIME] = m_timeValueScriberInherent;
            m_valueScriberTable[CODEC_DATE] = m_dateValueScriberInherent;
            m_valueScriberTable[CODEC_GDAY] = m_gDayValueScriberInherent;
            m_valueScriberTable[CODEC_GMONTH] = m_gMonthValueScriberInherent;
            m_valueScriberTable[CODEC_GMONTHDAY] = m_gMonthDayValueScriberInherent;
            m_valueScriberTable[CODEC_GYEAR] = m_gYearValueScriberInherent;
            m_valueScriberTable[CODEC_GYEARMONTH] = m_gYearMonthValueScriberInherent;
            m_valueScriberTable[CODEC_DOUBLE] = m_floatValueScriberInherent;
            m_valueScriberTable[CODEC_INTEGER] = m_integerValueScriberInherent;
            m_valueScriberTable[CODEC_BASE64BINARY] = m_base64BinaryValueScriberInherent;
            m_valueScriberTable[CODEC_HEXBINARY] = m_hexBinaryValueScriberInherent;
          }
        }
      }
    }

    ///////////////////////////////////////////////////////////////////////////
    /// Structure Scriber Functions
    ///////////////////////////////////////////////////////////////////////////

    public override sealed void writeEventType(EventType eventType) {
      EventCode[] path;
      path = eventType.ItemPath;

      int i, len;
      EventCode item = path[0].parent;
      for (i = 0, len = path.Length; i < len; i++) {
        EventCodeTuple parent = (EventCodeTuple)item;
        item = path[i];
        int width;
        if ((width = parent.width) != 0) {
          writeNBitUnsigned(parent.reversed ? parent.itemsCount - 1 - item.position : item.position, width, (Stream)null);
        }
      }
    }

    public override sealed void writeNS(string uri, string prefix, bool localElementNs) {
      Debug.Assert(m_preserveNS);
      writeURI(uri, (Stream)null);
      writePrefixOfNS(prefix, uri);
      writeBoolean(localElementNs, (Stream)null);
    }

    public override void writeQName(QName qName, EventType eventType) {
      sbyte itemType = eventType.itemType;
      StringTable.LocalNamePartition localNamePartition;
      int uriId, localNameId;
      switch (itemType) {
        case EventType.ITEM_SCHEMA_WC_ANY:
        case EventType.ITEM_AT_WC_ANY_UNTYPED:
        case EventType.ITEM_SE_WC:
        case EventType.ITEM_SCHEMA_AT_WC_ANY:
          uriId = writeURI(qName.namespaceName, (Stream)null);
          localNamePartition = stringTable.getLocalNamePartition(uriId);
          localNameId = writeLocalName(qName.localName, localNamePartition, (Stream)null);
          break;
        case EventType.ITEM_SCHEMA_WC_NS:
        case EventType.ITEM_SCHEMA_AT_WC_NS:
          uriId = eventType.URIId;
          localNamePartition = stringTable.getLocalNamePartition(uriId);
          localNameId = writeLocalName(qName.localName, localNamePartition, (Stream)null);
          break;
        case EventType.ITEM_SCHEMA_AT:
        case EventType.ITEM_AT:
        case EventType.ITEM_SE:
        case EventType.ITEM_SCHEMA_TYPE:
        case EventType.ITEM_SCHEMA_NIL:
        case EventType.ITEM_SCHEMA_AT_INVALID_VALUE:
          uriId = eventType.URIId;
          localNameId = eventType.NameId;
          break;
        default:
          uriId = localNameId = -1;
          Debug.Assert(false);
        break;
      }
      qName.uriId = uriId;
      qName.localNameId = localNameId;
      if (m_preserveNS) {
        writePrefixOfQName(qName.prefix, uriId, (Stream)null);
      }
    }

    ///////////////////////////////////////////////////////////////////////////
    /// Other functions
    ///////////////////////////////////////////////////////////////////////////

    private void writePrefixOfNS(string prefix, string uri) {
      StringTable.PrefixPartition partition;
      int uriId = stringTable.getCompactIdOfURI(uri);
      partition = stringTable.getPrefixPartition(uriId);
      int n_names, width, id;
      n_names = partition.n_strings;
      width = partition.forwardedWidth;
      if ((id = partition.internPrefix(prefix)) < n_names) {
        writeNBitUnsigned(id + 1, width, (Stream)null);
      }
      else {
        writeNBitUnsigned(0, width, (Stream)null);
        int length = prefix.Length;
        CharacterBuffer characterBuffer = ensureCharacters(length);
        Characters characterSequence = characterBuffer.addString(prefix, length);
        writeLiteralCharacters(characterSequence, length, 0, EXISchema.NIL_NODE, (Stream)null);
      }
    }

  }

}