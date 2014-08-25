package org.openexi.proc.io;

import java.io.IOException;
import java.io.OutputStream;

import org.openexi.proc.common.QName;
import org.openexi.proc.common.StringTable;
import org.openexi.proc.common.StringTable.LocalValuePartition;
import org.openexi.proc.util.ExiUriConst;
import org.openexi.schema.Characters;
import org.openexi.schema.EXISchema;

public final class StringValueScriber extends ValueScriberBase {
  
  public static final StringValueScriber instance;
  static {
    instance = new StringValueScriber();
  }
  
  private StringValueScriber() {
    super(new QName("exi:string", ExiUriConst.W3C_2009_EXI_URI));
  }
  
  @Override
  public short getCodecID() {
    return Scriber.CODEC_STRING;
  }

  @Override
  public boolean process(String value, int tp, EXISchema schema, Scribble scribble, Scriber scriber) {
    scribble.stringValue1 = value;
    return true;
  }
  
  @Override
  public void scribe(String value, Scribble scribble, int localName, int uri, int tp, OutputStream channelStream, Scriber scriber) throws IOException {
    final int length = value.length();
    final Characters characterSequence = scriber.ensureCharacters(length).addString(value, length);
    scribeStringValue(characterSequence, localName, uri, tp, channelStream, scriber);
  }

  void scribeStringValue(Characters value, int localNameId, int uriId, int tp, OutputStream ostream, Scriber scriber) throws IOException {
    final StringTable.GlobalValuePartition globalPartition = scriber.stringTable.globalValuePartition;
    final StringTable.GlobalEntry entry;
    if ((entry = globalPartition.getEntry(value)) == null) {
      final int length = value.length;
      scriber.writeLiteralCharacters(value, length, 2, tp, ostream);
      if (length != 0 && length < scriber.valueMaxExclusiveLength) {
        globalPartition.addValue(value, localNameId, uriId);
      }
      return;
    }
    final LocalValuePartition localPartition;
    if ((localPartition = entry.localPartition) == globalPartition.getLocalPartition(localNameId, uriId)) {
      scriber.writeUnsignedInteger32(0, ostream);
      scriber.writeNBitUnsigned(entry.localEntry.number, localPartition.width, ostream);
      return;
    }
    scriber.writeUnsignedInteger32(1, ostream);
    scriber.writeNBitUnsigned(entry.number, globalPartition.width, ostream);
  }

  ////////////////////////////////////////////////////////////
  
  @Override
  public Object toValue(String value, Scribble scribble, Scriber scriber) {
    final int length = value.length();
    return scriber.ensureCharacters(length).addString(value, length);
  }

  @Override
  public void doScribe(Object value, int localName, int uri, int tp, OutputStream channelStream, Scriber scriber) throws IOException  {
    scribeStringValue((Characters)value, localName, uri, tp, channelStream, scriber);
  }

}
