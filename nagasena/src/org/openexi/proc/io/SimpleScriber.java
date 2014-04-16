package org.openexi.proc.io;

import java.io.IOException;
import java.io.OutputStream;

import org.openexi.proc.common.EventCode;
import org.openexi.proc.common.EventType;
import org.openexi.proc.common.QName;
import org.openexi.proc.common.StringTable;
import org.openexi.proc.grammars.EventCodeTuple;
import org.openexi.schema.Characters;
import org.openexi.schema.EXISchema;

abstract class SimpleScriber extends Scriber {
  
  SimpleScriber(boolean isForEXIOptions) {
    super(isForEXIOptions);
    
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
  
  @Override
  public void setBlockSize(int blockSize) {
    // Do nothing.
  }

  @Override
  public final void setPreserveLexicalValues(boolean preserveLexicalValues) {
    final boolean prevPreserveLexicalValues = m_preserveLexicalValues;
    super.setPreserveLexicalValues(preserveLexicalValues);
    if (prevPreserveLexicalValues != preserveLexicalValues) {
      if (preserveLexicalValues) {
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

  ///////////////////////////////////////////////////////////////////////////
  /// Structure Scriber Functions
  ///////////////////////////////////////////////////////////////////////////
  
  @Override
  public final void writeEventType(EventType eventType) throws IOException {
    EventCode[] path;
    path = eventType.getItemPath();

    int i, len;
    EventCode item = path[0].parent;
    for (i = 0, len = path.length; i < len; i++) {
      EventCodeTuple parent = (EventCodeTuple)item;
      item = path[i];
      final int width;
      if ((width = parent.width) != 0) {
        writeNBitUnsigned(parent.reversed ? parent.itemsCount - 1 - item.position : item.position, width, (OutputStream)null);
      }
    }
  }
  
  @Override
  public final void writeNS(String uri, String prefix, boolean localElementNs) 
    throws IOException {
    assert m_preserveNS;
    writeURI(uri, (OutputStream)null);
    writePrefixOfNS(prefix, uri);
    writeBoolean(localElementNs, (OutputStream)null);
  }
  
  @Override
  public void writeQName(QName qName, EventType eventType) throws IOException {
    final byte itemType = eventType.itemType;
    final StringTable.LocalNamePartition localNamePartition;
    final int uriId, localNameId;
    switch (itemType) {
      case EventType.ITEM_SCHEMA_WC_ANY:
      case EventType.ITEM_AT_WC_ANY_UNTYPED:
      case EventType.ITEM_SE_WC:
      case EventType.ITEM_SCHEMA_AT_WC_ANY:
        uriId = writeURI(qName.namespaceName, (OutputStream)null);
        localNamePartition = stringTable.getLocalNamePartition(uriId);
        localNameId = writeLocalName(qName.localName, localNamePartition, (OutputStream)null);
        break;
      case EventType.ITEM_SCHEMA_WC_NS:
      case EventType.ITEM_SCHEMA_AT_WC_NS:
        uriId = eventType.getURIId();
        localNamePartition = stringTable.getLocalNamePartition(uriId);
        localNameId = writeLocalName(qName.localName, localNamePartition, (OutputStream)null);
        break;
      case EventType.ITEM_SCHEMA_AT:
      case EventType.ITEM_AT:        
      case EventType.ITEM_SE:
      case EventType.ITEM_SCHEMA_TYPE:        
      case EventType.ITEM_SCHEMA_NIL:     
      case EventType.ITEM_SCHEMA_AT_INVALID_VALUE:
        uriId = eventType.getURIId();
        localNameId = eventType.getNameId();
        break;
      default:
        uriId = localNameId = -1;
        assert false;
    }
    qName.uriId = uriId;
    qName.localNameId = localNameId;
    if (m_preserveNS)
      writePrefixOfQName(qName.prefix, uriId, (OutputStream)null);
  }

  ///////////////////////////////////////////////////////////////////////////
  /// Other functions
  ///////////////////////////////////////////////////////////////////////////
  
  private void writePrefixOfNS(String prefix, String uri) throws IOException {
    final StringTable.PrefixPartition partition;
    final int uriId = stringTable.getCompactIdOfURI(uri);
    partition = stringTable.getPrefixPartition(uriId);
    final int n_names, width, id;
    n_names = partition.n_strings;
    width = partition.forwardedWidth;
    if ((id = partition.internPrefix(prefix)) < n_names)
      writeNBitUnsigned(id + 1, width, (OutputStream)null);
    else {
      writeNBitUnsigned(0, width, (OutputStream)null);
      final int length = prefix.length();
      final CharacterBuffer characterBuffer = ensureCharacters(length);
      final Characters characterSequence = characterBuffer.addString(prefix, length);
      writeLiteralCharacters(characterSequence, length, 0, EXISchema.NIL_NODE, (OutputStream)null);
    }
  }

}
