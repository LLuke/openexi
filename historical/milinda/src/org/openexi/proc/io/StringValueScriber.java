package org.openexi.proc.io;

import java.io.IOException;
import java.io.OutputStream;

import org.openexi.proc.common.CharacterSequence;
import org.openexi.proc.common.QName;
import org.openexi.proc.util.ExiUriConst;
import org.openexi.schema.EXISchema;

public final class StringValueScriber extends ValueScriberBase {
  
  private int m_valueMaxExclusiveLength;
  private StringTable m_stringTable;
  
  public StringValueScriber(Scriber scriber) {
    super(scriber, new QName("exi:string", ExiUriConst.W3C_2009_EXI_URI));
    m_valueMaxExclusiveLength = Integer.MAX_VALUE;
    m_stringTable = null;
  }
  
  @Override
  public short getCodecID() {
    return Scriber.CODEC_STRING;
  }

  @Override
  public void setStringTable(StringTable stringTable) {
    m_stringTable = stringTable;
  }

  @Override
  public void setValueMaxLength(int valueMaxLength) {
    assert valueMaxLength >= 0;
    m_valueMaxExclusiveLength = valueMaxLength != Integer.MAX_VALUE ? valueMaxLength + 1 : Integer.MAX_VALUE;
  }

  @Override
  public boolean process(String value, int tp, EXISchema schema, Scribble scribble) {
    scribble.stringValue1 = value;
    return true;
  }
  
  @Override
  public void scribe(String value, Scribble scribble, String localName, String uri, int tp, OutputStream channelStream) throws IOException {
    final int length = value.length();
    final CharacterSequence characterSequence = m_scriber.ensureCharacters(length).addString(value, length);
    scribeStringValue(characterSequence, localName, uri, tp, channelStream);
  }

  void scribeStringValue(CharacterSequence value, String localName, String uri, int tp, OutputStream ostream) throws IOException {
    final StringTable.GlobalPartition globalPartition = m_stringTable.getGlobalPartition();
    final StringTable.GlobalEntry entry;
    if ((entry = globalPartition.getEntry(value)) == null) {
      final int length = value.length();
      m_scriber.writeLiteralString(value, length, 2, tp, ostream);
      if (length != 0 && length < m_valueMaxExclusiveLength) {
        globalPartition.addString(value, localName, uri);
      }
      return;
    }
    final StringTable.LocalPartition localPartition;
    localPartition = entry.localPartition;
    if (localName.equals(localPartition.name) && uri.equals(localPartition.uri)) {
      m_scriber.writeUnsignedInteger32(0, ostream);
      m_scriber.writeNBitUnsigned(entry.localEntry.number, localPartition.width, ostream);
      return;
    }
    m_scriber.writeUnsignedInteger32(1, ostream);
    m_scriber.writeNBitUnsigned(entry.number, globalPartition.width, ostream);
  }

  ////////////////////////////////////////////////////////////
  
  @Override
  public Object toValue(String value, Scribble scribble) {
    final int length = value.length();
    return m_scriber.ensureCharacters(length).addString(value, length);
  }

  @Override
  public void doScribe(Object value, String localName, String uri, int tp, OutputStream channelStream) throws IOException  {
    scribeStringValue((CharacterSequence)value, localName, uri, tp, channelStream);
  }

}
