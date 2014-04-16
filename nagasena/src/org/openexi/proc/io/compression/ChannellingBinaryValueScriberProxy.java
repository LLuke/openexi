package org.openexi.proc.io.compression;

import java.io.IOException;

import org.openexi.proc.common.StringTable;
import org.openexi.proc.io.BinaryDataSink;
import org.openexi.proc.io.BinaryValueScriber;
import org.openexi.proc.io.Scribble;
import org.openexi.proc.io.Scriber;
import org.openexi.proc.io.ScriberRuntimeException;
import org.openexi.schema.EXISchema;

final class ChannellingBinaryValueScriberProxy extends ChannellingValueScriberProxy implements BinaryDataSink {

  ChannellingBinaryValueScriberProxy(ChannelKeeper channelKeeper, BinaryValueScriber binaryValueScriber) {
    super(channelKeeper, binaryValueScriber);
  }
  
  ///////////////////////////////////////////////////////////////////////////
  /// BinaryDataSink Functions
  ///////////////////////////////////////////////////////////////////////////
  
  public final void startBinaryData(long totalSize, Scribble scribble, Scriber scriber) throws ScriberRuntimeException {
    scribble.intValue1 = 0;
    if ((totalSize & (Long.MIN_VALUE >> 31)) != 0) {
      throw new ScriberRuntimeException(ScriberRuntimeException.BINARY_DATA_SIZE_TOO_LARGE, 
          new String[] { Long.toString(totalSize) });
    }
    scribble.binaryValue = scribble.expandOctetArray(scribble.intValue2 = (int)totalSize);
  }
  
  public final void binaryData(byte[] byteArray, int offset, int length, Scribble scribble, Scriber scriber) throws IOException {
    System.arraycopy(byteArray, offset, scribble.binaryValue, scribble.intValue1, length);
    scribble.intValue1 += length;
  }

  public final void endBinaryData(Scribble scribble, int localName, int uri, Scriber scriber) throws ScriberRuntimeException, IOException {
    if (scribble.intValue1 != scribble.intValue2) {
      throw new ScriberRuntimeException(ScriberRuntimeException.BINARY_DATA_SIZE_MISMATCH);
    }
    final StringTable stringTable = scriber.stringTable;
    final ScriberChannel channel = (ScriberChannel)m_channelKeeper.getChannel(localName, uri, stringTable);
    final boolean reached = m_channelKeeper.incrementValueCount(channel);
    channel.values.add(new ScriberValueHolder(localName, uri, EXISchema.NIL_NODE, m_valueScriber.toValue((String)null, scribble, scriber), this));
    if (reached)
      ((ChannellingScriber)scriber).finishBlock();
  }

}
