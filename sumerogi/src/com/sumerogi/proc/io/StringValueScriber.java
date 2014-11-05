package com.sumerogi.proc.io;

import java.io.IOException;
import java.io.OutputStream;

import com.sumerogi.proc.common.StringTable;
import com.sumerogi.proc.common.StringTable.LocalValuePartition;
import com.sumerogi.schema.Characters;

public final class StringValueScriber extends ValueScriberBase {
  
  public static final StringValueScriber instance;
  static {
    instance = new StringValueScriber();
  }

  public StringValueScriber() {
  }
  
  @Override
  public boolean process(char[] value, int offset, int length, Scribble scribble, Scriber scriber) {
    scribble.stringValue1 = new String(value, offset, length); 
    return true;
  }
  
  @Override
  public void scribe(String value, Scribble scribble, int localName, OutputStream channelStream, Scriber scriber) throws IOException {
    final int length = value.length();
    final Characters characterSequence = scriber.ensureCharacters(length).addString(value, length);
    scribeStringValue(characterSequence, localName, channelStream, scriber);
  }

  void scribeStringValue(Characters value, int localNameId, OutputStream ostream, Scriber scriber) throws IOException {
    final StringTable.GlobalValuePartition globalPartition = scriber.stringTable.globalValuePartition;
    final StringTable.GlobalEntry entry;
    if ((entry = globalPartition.getEntry(value)) == null) {
      final int length = value.length;
      scriber.writeLiteralCharacters(value, length, 2, ostream);
      if (length != 0) {
        globalPartition.addValue(value, localNameId);
      }
      return;
    }
    final LocalValuePartition localPartition;
    if ((localPartition = entry.localPartition) == globalPartition.getLocalPartition(localNameId)) {
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
  public void doScribe(Object value, int localName, OutputStream channelStream, Scriber scriber) throws IOException  {
    scribeStringValue((Characters)value, localName, channelStream, scriber);
  }

}
