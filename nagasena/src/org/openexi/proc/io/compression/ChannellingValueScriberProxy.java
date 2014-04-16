package org.openexi.proc.io.compression;

import java.io.IOException;
import java.io.OutputStream;

import org.openexi.proc.common.QName;
import org.openexi.proc.common.StringTable;
import org.openexi.proc.io.Scribble;
import org.openexi.proc.io.Scriber;
import org.openexi.proc.io.ValueScriber;
import org.openexi.schema.EXISchema;

class ChannellingValueScriberProxy extends ValueScriber {

  protected final ChannelKeeper m_channelKeeper;
  protected final ValueScriber m_valueScriber;
  
  ChannellingValueScriberProxy(ChannelKeeper channelKeeper, ValueScriber valueScriber) {
    m_channelKeeper = channelKeeper;
    m_valueScriber = valueScriber;
  }

  @Override
  public final void scribe(String value, Scribble scribble, int localName, int uri, int tp, Scriber scriber) throws IOException {
    final StringTable stringTable = scriber.stringTable;
    final ScriberChannel channel = (ScriberChannel)m_channelKeeper.getChannel(localName, uri, stringTable);
    final boolean reached = m_channelKeeper.incrementValueCount(channel);
    channel.values.add(new ScriberValueHolder(localName, uri, tp, toValue(value, scribble, scriber), this));
    if (reached)
      ((ChannellingScriber)scriber).finishBlock();
  }

  ///////////////////////////////////////////////////////////////////////////
  // Other methods (simply calling through to corresponding methods)
  ///////////////////////////////////////////////////////////////////////////

  @Override
  public final QName getName() {
    return m_valueScriber.getName();
  }
  
  @Override
  public final short getCodecID() {
    return m_valueScriber.getCodecID();
  }

  @Override
  public final int getBuiltinRCS(int simpleType, Scriber scriber) {
    return m_valueScriber.getBuiltinRCS(simpleType, scriber);
  }

  @Override
  public final boolean process(String value, int tp, EXISchema schema, Scribble scribble, Scriber scriber) {
    return m_valueScriber.process(value, tp, schema, scribble, scriber);
  }

  @Override
  public final Object toValue(String value, Scribble scribble, Scriber scriber) {
    return m_valueScriber.toValue(value, scribble, scriber);
  }
  
  @Override
  public final void doScribe(Object value, int localName, int uri, int tp, OutputStream channelStream, Scriber scriber) throws IOException {
    m_valueScriber.doScribe(value, localName, uri, tp, channelStream, scriber);
  }
  
  @Override
  public final void scribe(String value, Scribble scribble, int localName, int uri, int tp, OutputStream ostream, Scriber scriber) throws IOException {
    m_valueScriber.scribe(value, scribble, localName, uri, tp, ostream, scriber);
  }
  
}
