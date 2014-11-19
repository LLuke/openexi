package com.sumerogi.proc.io.compression;

import java.io.IOException;
import java.io.OutputStream;

import com.sumerogi.proc.common.StringTable;
import com.sumerogi.proc.io.Scribble;
import com.sumerogi.proc.io.Scriber;
import com.sumerogi.proc.io.ValueScriber;

class ChannellingValueScriberProxy extends ValueScriber {

  protected final ChannelKeeper m_channelKeeper;
  protected final ValueScriber m_valueScriber;
  
  ChannellingValueScriberProxy(ChannelKeeper channelKeeper, ValueScriber valueScriber) {
    m_channelKeeper = channelKeeper;
    m_valueScriber = valueScriber;
  }

  @Override
  public final void scribe(String value, Scribble scribble, int localName, Scriber scriber) throws IOException {
    final StringTable stringTable = scriber.stringTable;
    final ScriberChannel channel = (ScriberChannel)m_channelKeeper.getChannel(localName, stringTable);
    final boolean reached = m_channelKeeper.incrementValueCount(channel);
    channel.values.add(new ScriberValueHolder(localName, toValue(value, scribble, scriber), this));
    if (reached)
      ((ChannellingScriber)scriber).finishBlock();
  }

  ///////////////////////////////////////////////////////////////////////////
  // Other methods (simply calling through to corresponding methods)
  ///////////////////////////////////////////////////////////////////////////

  @Override
  public final boolean process(char[] value, int offset, int length, Scribble scribble, Scriber scriber) {
    return m_valueScriber.process(value, offset, length, scribble, scriber);
  }

  @Override
  public final Object toValue(String value, Scribble scribble, Scriber scriber) {
    return m_valueScriber.toValue(value, scribble, scriber);
  }
  
  @Override
  public final void doScribe(Object value, int localName, OutputStream channelStream, Scriber scriber) throws IOException {
    m_valueScriber.doScribe(value, localName, channelStream, scriber);
  }

  @Override
  public final void scribe(String value, Scribble scribble, int localName, OutputStream ostream, Scriber scriber) throws IOException {
    m_valueScriber.scribe(value, scribble, localName, ostream, scriber);
  }
  
}
