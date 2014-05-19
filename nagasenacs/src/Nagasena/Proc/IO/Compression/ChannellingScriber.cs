using System.Diagnostics;
using System.IO;
using System.Collections.Generic;

using ICSharpCode.SharpZipLib.Zip.Compression;

using AlignmentType = Nagasena.Proc.Common.AlignmentType;
using Channel = Nagasena.Proc.Common.Channel;
using EventCode = Nagasena.Proc.Common.EventCode;
using EventType = Nagasena.Proc.Common.EventType;
using QName = Nagasena.Proc.Common.QName;
using StringTable = Nagasena.Proc.Common.StringTable;
using EventCodeTuple = Nagasena.Proc.Grammars.EventCodeTuple;
using Characters = Nagasena.Schema.Characters;
using EXISchema = Nagasena.Schema.EXISchema;

namespace Nagasena.Proc.IO.Compression {

  /// <exclude/>
  public sealed class ChannellingScriber : Scriber {

    private readonly bool m_compressed;

    private Stream m_baseDataStream;

    private readonly ValueScriber m_stringValueScriberInherentProxy;
    private readonly ValueScriber m_booleanValueScriberInherentProxy;
    private readonly ValueScriber m_enumerationValueScriberInherentProxy;
    private readonly ValueScriber m_listValueScriberInherentProxy;
    private readonly ValueScriber m_decimalValueScriberInherentProxy;
    private readonly ValueScriber m_dateTimeValueScriberInherentProxy;
    private readonly ValueScriber m_timeValueScriberInherentProxy;
    private readonly ValueScriber m_dateValueScriberInherentProxy;
    private readonly ValueScriber m_gDayValueScriberInherentProxy;
    private readonly ValueScriber m_gMonthValueScriberInherentProxy;
    private readonly ValueScriber m_gMonthDayValueScriberInherentProxy;
    private readonly ValueScriber m_gYearValueScriberInherentProxy;
    private readonly ValueScriber m_gYearMonthValueScriberInherentProxy;
    private readonly ValueScriber m_floatValueScriberInherentProxy;
    private readonly ValueScriber m_integerValueScriberInherentProxy;
    private readonly ValueScriber m_base64BinaryValueScriberInherentProxy;
    private readonly ValueScriber m_hexBinaryValueScriberInherentProxy;

    private readonly ValueScriber m_stringValueScriberLexicalProxy;
    private readonly ValueScriber m_booleanValueScriberLexicalProxy;
    private readonly ValueScriber m_enumerationValueScriberLexicalProxy;
    private readonly ValueScriber m_listValueScriberLexicalProxy;
    private readonly ValueScriber m_decimalValueScriberLexicalProxy;
    private readonly ValueScriber m_dateTimeValueScriberLexicalProxy;
    private readonly ValueScriber m_timeValueScriberLexicalProxy;
    private readonly ValueScriber m_dateValueScriberLexicalProxy;
    private readonly ValueScriber m_gDayValueScriberLexicalProxy;
    private readonly ValueScriber m_gMonthValueScriberLexicalProxy;
    private readonly ValueScriber m_gMonthDayValueScriberLexicalProxy;
    private readonly ValueScriber m_gYearValueScriberLexicalProxy;
    private readonly ValueScriber m_gYearMonthValueScriberLexicalProxy;
    private readonly ValueScriber m_floatValueScriberLexicalProxy;
    private readonly ValueScriber m_integerValueScriberLexicalProxy;
    private readonly ValueScriber m_base64BinaryValueScriberLexicalProxy;
    private readonly ValueScriber m_hexBinaryValueScriberLexicalProxy;

    private readonly ChannelKeeper m_channelKeeper;
    private readonly Deflater m_deflator;

    public ChannellingScriber(bool compressed) : base(false) {
      m_compressed = compressed;
      m_channelKeeper = new ChannelKeeper(new ScriberChannelFactory());
      m_deflator = compressed ? new Deflater(Deflater.DEFAULT_COMPRESSION, true) : null;

      m_stringValueScriberInherentProxy = new ChannellingValueScriberProxy(m_channelKeeper, m_stringValueScriberInherent);
      m_booleanValueScriberInherentProxy = new ChannellingValueScriberProxy(m_channelKeeper, m_booleanValueScriberInherent);
      m_enumerationValueScriberInherentProxy = new ChannellingValueScriberProxy(m_channelKeeper, m_enumerationValueScriberInherent);
      m_listValueScriberInherentProxy = new ChannellingValueScriberProxy(m_channelKeeper, m_listValueScriberInherent);
      m_decimalValueScriberInherentProxy = new ChannellingValueScriberProxy(m_channelKeeper, m_decimalValueScriberInherent);
      m_dateTimeValueScriberInherentProxy = new ChannellingValueScriberProxy(m_channelKeeper, m_dateTimeValueScriberInherent);
      m_timeValueScriberInherentProxy = new ChannellingValueScriberProxy(m_channelKeeper, m_timeValueScriberInherent);
      m_dateValueScriberInherentProxy = new ChannellingValueScriberProxy(m_channelKeeper, m_dateValueScriberInherent);
      m_gDayValueScriberInherentProxy = new ChannellingValueScriberProxy(m_channelKeeper, m_gDayValueScriberInherent);
      m_gMonthValueScriberInherentProxy = new ChannellingValueScriberProxy(m_channelKeeper, m_gMonthValueScriberInherent);
      m_gMonthDayValueScriberInherentProxy = new ChannellingValueScriberProxy(m_channelKeeper, m_gMonthDayValueScriberInherent);
      m_gYearValueScriberInherentProxy = new ChannellingValueScriberProxy(m_channelKeeper, m_gYearValueScriberInherent);
      m_gYearMonthValueScriberInherentProxy = new ChannellingValueScriberProxy(m_channelKeeper, m_gYearMonthValueScriberInherent);
      m_floatValueScriberInherentProxy = new ChannellingValueScriberProxy(m_channelKeeper, m_floatValueScriberInherent);
      m_integerValueScriberInherentProxy = new ChannellingValueScriberProxy(m_channelKeeper, m_integerValueScriberInherent);
      m_base64BinaryValueScriberInherentProxy = new ChannellingBinaryValueScriberProxy(m_channelKeeper, m_base64BinaryValueScriberInherent);
      m_hexBinaryValueScriberInherentProxy = new ChannellingBinaryValueScriberProxy(m_channelKeeper, m_hexBinaryValueScriberInherent);

      m_stringValueScriberLexicalProxy = new ChannellingValueScriberProxy(m_channelKeeper, m_stringValueScriberLexical);
      m_booleanValueScriberLexicalProxy = new ChannellingValueScriberProxy(m_channelKeeper, m_booleanValueScriberLexical);
      m_enumerationValueScriberLexicalProxy = new ChannellingValueScriberProxy(m_channelKeeper, m_enumerationValueScriberLexical);
      m_listValueScriberLexicalProxy = new ChannellingValueScriberProxy(m_channelKeeper, m_listValueScriberLexical);
      m_decimalValueScriberLexicalProxy = new ChannellingValueScriberProxy(m_channelKeeper, m_decimalValueScriberLexical);
      m_dateTimeValueScriberLexicalProxy = new ChannellingValueScriberProxy(m_channelKeeper, m_dateTimeValueScriberLexical);
      m_timeValueScriberLexicalProxy = new ChannellingValueScriberProxy(m_channelKeeper, m_timeValueScriberLexical);
      m_dateValueScriberLexicalProxy = new ChannellingValueScriberProxy(m_channelKeeper, m_dateValueScriberLexical);
      m_gDayValueScriberLexicalProxy = new ChannellingValueScriberProxy(m_channelKeeper, m_gDayValueScriberLexical);
      m_gMonthValueScriberLexicalProxy = new ChannellingValueScriberProxy(m_channelKeeper, m_gMonthValueScriberLexical);
      m_gMonthDayValueScriberLexicalProxy = new ChannellingValueScriberProxy(m_channelKeeper, m_gMonthDayValueScriberLexical);
      m_gYearValueScriberLexicalProxy = new ChannellingValueScriberProxy(m_channelKeeper, m_gYearValueScriberLexical);
      m_gYearMonthValueScriberLexicalProxy = new ChannellingValueScriberProxy(m_channelKeeper, m_gYearMonthValueScriberLexical);
      m_floatValueScriberLexicalProxy = new ChannellingValueScriberProxy(m_channelKeeper, m_floatValueScriberLexical);
      m_integerValueScriberLexicalProxy = new ChannellingValueScriberProxy(m_channelKeeper, m_integerValueScriberLexical);
      m_base64BinaryValueScriberLexicalProxy = new ChannellingValueScriberProxy(m_channelKeeper, m_base64BinaryValueScriberLexical);
      m_hexBinaryValueScriberLexicalProxy = new ChannellingValueScriberProxy(m_channelKeeper, m_hexBinaryValueScriberLexical);

      m_valueScriberTable[CODEC_STRING] = m_stringValueScriberInherentProxy;
      m_valueScriberTable[CODEC_BOOLEAN] = m_booleanValueScriberInherentProxy;
      m_valueScriberTable[CODEC_ENUMERATION] = m_enumerationValueScriberInherentProxy;
      m_valueScriberTable[CODEC_LIST] = m_listValueScriberInherentProxy;
      m_valueScriberTable[CODEC_DECIMAL] = m_decimalValueScriberInherentProxy;
      m_valueScriberTable[CODEC_DATETIME] = m_dateTimeValueScriberInherentProxy;
      m_valueScriberTable[CODEC_TIME] = m_timeValueScriberInherentProxy;
      m_valueScriberTable[CODEC_DATE] = m_dateValueScriberInherentProxy;
      m_valueScriberTable[CODEC_GDAY] = m_gDayValueScriberInherentProxy;
      m_valueScriberTable[CODEC_GMONTH] = m_gMonthValueScriberInherentProxy;
      m_valueScriberTable[CODEC_GMONTHDAY] = m_gMonthDayValueScriberInherentProxy;
      m_valueScriberTable[CODEC_GYEAR] = m_gYearValueScriberInherentProxy;
      m_valueScriberTable[CODEC_GYEARMONTH] = m_gYearMonthValueScriberInherentProxy;
      m_valueScriberTable[CODEC_DOUBLE] = m_floatValueScriberInherentProxy;
      m_valueScriberTable[CODEC_INTEGER] = m_integerValueScriberInherentProxy;
      m_valueScriberTable[CODEC_BASE64BINARY] = m_base64BinaryValueScriberInherentProxy;
      m_valueScriberTable[CODEC_HEXBINARY] = m_hexBinaryValueScriberInherentProxy;
    }

    public override void reset() {
      base.reset();
      m_channelKeeper.reset();
      if (m_compressed) {
        m_deflator.Reset();
      }
      m_baseDataStream = null;
      m_outputStream = null;
    }

    /// <summary>
    /// Set ZLIB compression level and strategy. </summary>
    /// <param name="level"> the new compression level (0-9) </param>
    /// <param name="strategy"> the new compression strategy </param>
    /// <seealso cref= java.util.zip.Deflator#setLevel(int level) </seealso>
    /// <seealso cref= java.util.zip.Deflator#setStrategy(int strategy) </seealso>
    public override void setDeflateParams(int level, DeflateStrategy strategy) {
      if (m_compressed) {
        m_deflator.SetLevel(level);
        m_deflator.SetStrategy(strategy);
      }
    }

    public override AlignmentType AlignmentType {
      get {
        return m_compressed ? AlignmentType.compress : AlignmentType.preCompress;
      }
    }

    public override Stream OutputStream {
      set {
        m_baseDataStream = value;
        m_outputStream = m_compressed ? new EXIDeflaterOutputStream(m_baseDataStream, m_deflator) : m_baseDataStream;
      }
    }

    public override int BlockSize {
      set {
        m_channelKeeper.BlockSize = value;
      }
    }

    public override bool PreserveLexicalValues {
      set {
        bool prevPreserveLexicalValues = m_preserveLexicalValues;
        if (prevPreserveLexicalValues != value) {
          base.PreserveLexicalValues = value;
          if (value) {
            m_valueScriberTable[CODEC_STRING] = m_stringValueScriberLexicalProxy;
            m_valueScriberTable[CODEC_BOOLEAN] = m_booleanValueScriberLexicalProxy;
            m_valueScriberTable[CODEC_ENUMERATION] = m_enumerationValueScriberLexicalProxy;
            m_valueScriberTable[CODEC_LIST] = m_listValueScriberLexicalProxy;
            m_valueScriberTable[CODEC_DECIMAL] = m_decimalValueScriberLexicalProxy;
            m_valueScriberTable[CODEC_DATETIME] = m_dateTimeValueScriberLexicalProxy;
            m_valueScriberTable[CODEC_TIME] = m_timeValueScriberLexicalProxy;
            m_valueScriberTable[CODEC_DATE] = m_dateValueScriberLexicalProxy;
            m_valueScriberTable[CODEC_GDAY] = m_gDayValueScriberLexicalProxy;
            m_valueScriberTable[CODEC_GMONTH] = m_gMonthValueScriberLexicalProxy;
            m_valueScriberTable[CODEC_GMONTHDAY] = m_gMonthDayValueScriberLexicalProxy;
            m_valueScriberTable[CODEC_GYEAR] = m_gYearValueScriberLexicalProxy;
            m_valueScriberTable[CODEC_GYEARMONTH] = m_gYearMonthValueScriberLexicalProxy;
            m_valueScriberTable[CODEC_DOUBLE] = m_floatValueScriberLexicalProxy;
            m_valueScriberTable[CODEC_INTEGER] = m_integerValueScriberLexicalProxy;
            m_valueScriberTable[CODEC_BASE64BINARY] = m_base64BinaryValueScriberLexicalProxy;
            m_valueScriberTable[CODEC_HEXBINARY] = m_hexBinaryValueScriberLexicalProxy;
          }
          else {
            m_valueScriberTable[CODEC_STRING] = m_stringValueScriberInherentProxy;
            m_valueScriberTable[CODEC_BOOLEAN] = m_booleanValueScriberInherentProxy;
            m_valueScriberTable[CODEC_ENUMERATION] = m_enumerationValueScriberInherentProxy;
            m_valueScriberTable[CODEC_LIST] = m_listValueScriberInherentProxy;
            m_valueScriberTable[CODEC_DECIMAL] = m_decimalValueScriberInherentProxy;
            m_valueScriberTable[CODEC_DATETIME] = m_dateTimeValueScriberInherentProxy;
            m_valueScriberTable[CODEC_TIME] = m_timeValueScriberInherentProxy;
            m_valueScriberTable[CODEC_DATE] = m_dateValueScriberInherentProxy;
            m_valueScriberTable[CODEC_GDAY] = m_gDayValueScriberInherentProxy;
            m_valueScriberTable[CODEC_GMONTH] = m_gMonthValueScriberInherentProxy;
            m_valueScriberTable[CODEC_GMONTHDAY] = m_gMonthDayValueScriberInherentProxy;
            m_valueScriberTable[CODEC_GYEAR] = m_gYearValueScriberInherentProxy;
            m_valueScriberTable[CODEC_GYEARMONTH] = m_gYearMonthValueScriberInherentProxy;
            m_valueScriberTable[CODEC_DOUBLE] = m_floatValueScriberInherentProxy;
            m_valueScriberTable[CODEC_INTEGER] = m_integerValueScriberInherentProxy;
            m_valueScriberTable[CODEC_BASE64BINARY] = m_base64BinaryValueScriberInherentProxy;
            m_valueScriberTable[CODEC_HEXBINARY] = m_hexBinaryValueScriberInherentProxy;
          }
        }
      }
    }

    ///////////////////////////////////////////////////////////////////////////
    /// Structure Scriber Functions
    ///////////////////////////////////////////////////////////////////////////

    public override void writeEventType(EventType eventType) {
      EventCode[] path;
      path = eventType.ItemPath;

      int i, len;
      EventCode item = path[0].parent;
      for (i = 0, len = path.Length; i < len; i++) {
        EventCodeTuple parent = (EventCodeTuple)item;
        item = path[i];
        int width;
        if ((width = parent.width) != 0) {
          writeNBitUnsigned(parent.reversed ? parent.itemsCount - 1 - item.position : item.position, width, m_outputStream);
        }
      }
    }

    public override void writeNS(string uri, string prefix, bool localElementNs) {
      Debug.Assert(m_preserveNS);
      Debug.Assert(m_outputStream != null);
      int uriId = writeURI(uri, m_outputStream);
      writePrefixOfNS(prefix, uriId);
      writeBoolean(localElementNs, m_outputStream);
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
          uriId = writeURI(qName.namespaceName, m_outputStream);
          localNamePartition = stringTable.getLocalNamePartition(uriId);
          localNameId = writeLocalName(qName.localName, localNamePartition, m_outputStream);
          break;
        case EventType.ITEM_SCHEMA_WC_NS:
        case EventType.ITEM_SCHEMA_AT_WC_NS:
          uriId = eventType.URIId;
          localNamePartition = stringTable.getLocalNamePartition(uriId);
          localNameId = writeLocalName(qName.localName, localNamePartition, m_outputStream);
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
        writePrefixOfQName(qName.prefix, uriId, m_outputStream);
      }
    }

    ///////////////////////////////////////////////////////////////////////////
    /// Other functions
    ///////////////////////////////////////////////////////////////////////////

    private void writePrefixOfNS(string prefix, int uriId) {
      StringTable.PrefixPartition partition;
      partition = stringTable.getPrefixPartition(uriId);
      int n_names, width, id;
      n_names = partition.n_strings;
      width = partition.forwardedWidth;
      if ((id = partition.internPrefix(prefix)) < n_names) {
        writeNBitUnsigned(id + 1, width, m_outputStream);
      }
      else {
        writeNBitUnsigned(0, width, m_outputStream);
        int length = prefix.Length;
        CharacterBuffer characterBuffer = ensureCharacters(length);
        Characters characterSequence = characterBuffer.addString(prefix, length);
        writeLiteralCharacters(characterSequence, length, 0, EXISchema.NIL_NODE, m_outputStream);
      }
    }

    ///////////////////////////////////////////////////////////////////////////
    /// Value Scriber Functions
    ///////////////////////////////////////////////////////////////////////////

    protected internal override void writeBoolean(bool val, Stream ostream) {
      ostream.WriteByte(val ? (byte)1 : (byte)0);
    }

    protected internal override void writeNBitUnsigned(int val, int width, Stream ostream) {
      ByteAlignedCommons.writeNBitUnsigned(val, width, ostream);
    }

    protected internal override void writeUnsignedInteger32(int @uint, Stream ostream) {
      ByteAlignedCommons.writeUnsignedInteger32(@uint, ostream);
    }

    protected internal override void writeUnsignedInteger64(long @uint, Stream ostream) {
      ByteAlignedCommons.writeUnsignedInteger64(@uint, ostream);
    }

    protected internal override void writeUnsignedInteger(System.Numerics.BigInteger @uint, Stream ostream) {
      ByteAlignedCommons.writeUnsignedInteger(@uint, ostream);
    }

    ///////////////////////////////////////////////////////////////////////////
    /// Other IO Functions
    ///////////////////////////////////////////////////////////////////////////

    public void finishBlock() {
      EXIDeflaterOutputStream deflaterStream = m_compressed ? (EXIDeflaterOutputStream)m_outputStream : null;
      bool moreValues = false;
      if (m_compressed) {
        int n_values;
        if ((n_values = m_channelKeeper.TotalValueCount) == 0) {
          deflaterStream.resetDeflater();
          m_channelKeeper.punctuate();
          return;
        }
        if (moreValues = n_values > 100) {
          deflaterStream.resetDeflater();
        }
      }
      m_channelKeeper.finish();
      IList<Channel> smallChannels, largeChannels;
      int n_smallChannels, n_largeChannels;
      ScriberChannel channel;
      smallChannels = m_channelKeeper.SmallChannels;
      if ((n_smallChannels = smallChannels.Count) != 0) {
        int i = 0;
        do {
          channel = (ScriberChannel)smallChannels[i];
          List<ScriberValueHolder> textProviderList = channel.values;
          int len = textProviderList.Count;
          for (int j = 0; j < len; j++) {
            textProviderList[j].scribeValue(m_outputStream, this);
          }
        }
        while (++i < n_smallChannels);
        if (m_compressed && moreValues) {
          deflaterStream.resetDeflater();
        }
      }
      largeChannels = m_channelKeeper.LargeChannels;
      n_largeChannels = largeChannels.Count;
      for (int i = 0; i < n_largeChannels; i++) {
        channel = (ScriberChannel)largeChannels[i];
        List<ScriberValueHolder> textProviderList = channel.values;
        int len = textProviderList.Count;
        for (int j = 0; j < len; j++) {
          textProviderList[j].scribeValue(m_outputStream, this);
        }
        if (m_compressed) {
          deflaterStream.resetDeflater();
        }
      }
      if (m_compressed && !moreValues) {
        deflaterStream.resetDeflater();
      }
      m_channelKeeper.punctuate();
    }

    public override void finish() {
      finishBlock();
      m_baseDataStream.Flush();
    }

  }

}