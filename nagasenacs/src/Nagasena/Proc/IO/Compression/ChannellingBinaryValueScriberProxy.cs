using System;
using System.Globalization;

using StringTable = Nagasena.Proc.Common.StringTable;
using EXISchema = Nagasena.Schema.EXISchema;

namespace Nagasena.Proc.IO.Compression {

  internal sealed class ChannellingBinaryValueScriberProxy : ChannellingValueScriberProxy, BinaryDataSink {

    internal ChannellingBinaryValueScriberProxy(ChannelKeeper channelKeeper, BinaryValueScriber binaryValueScriber) : 
      base(channelKeeper, binaryValueScriber) {
    }

    ///////////////////////////////////////////////////////////////////////////
    /// BinaryDataSink Functions
    ///////////////////////////////////////////////////////////////////////////

    public void startBinaryData(long totalSize, Scribble scribble, Scriber scriber) {
      scribble.intValue1 = 0;
      if ((totalSize & (long.MinValue >> 31)) != 0) {
        throw new ScriberRuntimeException(ScriberRuntimeException.BINARY_DATA_SIZE_TOO_LARGE, 
          new string[] { Convert.ToString(totalSize, NumberFormatInfo.InvariantInfo) });
      }
      scribble.binaryValue = scribble.expandOctetArray(scribble.intValue2 = (int)totalSize);
    }

    public void binaryData(byte[] byteArray, int offset, int length, Scribble scribble, Scriber scriber) {
      Array.Copy(byteArray, offset, scribble.binaryValue, scribble.intValue1, length);
      scribble.intValue1 += length;
    }

    public void endBinaryData(Scribble scribble, int localName, int uri, Scriber scriber) {
      if (scribble.intValue1 != scribble.intValue2) {
        throw new ScriberRuntimeException(ScriberRuntimeException.BINARY_DATA_SIZE_MISMATCH);
      }
      StringTable stringTable = scriber.stringTable;
      ScriberChannel channel = (ScriberChannel)m_channelKeeper.getChannel(localName, uri, stringTable);
      bool reached = m_channelKeeper.incrementValueCount(channel);
      channel.values.Add(new ScriberValueHolder(localName, uri, EXISchema.NIL_NODE, m_valueScriber.toValue((string)null, scribble, scriber), this));
      if (reached) {
        ((ChannellingScriber)scriber).finishBlock();
      }
    }

  }

}