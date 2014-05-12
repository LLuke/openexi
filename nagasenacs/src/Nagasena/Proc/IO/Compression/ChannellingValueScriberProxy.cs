using System.IO;

using QName = Nagasena.Proc.Common.QName;
using StringTable = Nagasena.Proc.Common.StringTable;
using EXISchema = Nagasena.Schema.EXISchema;

namespace Nagasena.Proc.IO.Compression {

  internal class ChannellingValueScriberProxy : ValueScriber {

    protected internal readonly ChannelKeeper m_channelKeeper;
    protected internal readonly ValueScriber m_valueScriber;

    internal ChannellingValueScriberProxy(ChannelKeeper channelKeeper, ValueScriber valueScriber) {
      m_channelKeeper = channelKeeper;
      m_valueScriber = valueScriber;
    }

    public override void scribe(string value, Scribble scribble, int localName, int uri, int tp, Scriber scriber) {
      StringTable stringTable = scriber.stringTable;
      ScriberChannel channel = (ScriberChannel)m_channelKeeper.getChannel(localName, uri, stringTable);
      bool reached = m_channelKeeper.incrementValueCount(channel);
      channel.values.Add(new ScriberValueHolder(localName, uri, tp, toValue(value, scribble, scriber), this));
      if (reached) {
        ((ChannellingScriber)scriber).finishBlock();
      }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Other methods (simply calling through to corresponding methods)
    ///////////////////////////////////////////////////////////////////////////

    public override QName Name {
      get {
        return m_valueScriber.Name;
      }
    }

    public override short CodecID {
      get {
        return m_valueScriber.CodecID;
      }
    }

    public override int getBuiltinRCS(int simpleType, Scriber scriber) {
      return m_valueScriber.getBuiltinRCS(simpleType, scriber);
    }

    public override bool process(string value, int tp, EXISchema schema, Scribble scribble, Scriber scriber) {
      return m_valueScriber.process(value, tp, schema, scribble, scriber);
    }

    public override object toValue(string value, Scribble scribble, Scriber scriber) {
      return m_valueScriber.toValue(value, scribble, scriber);
    }

    public override void doScribe(object value, int localName, int uri, int tp, Stream channelStream, Scriber scriber) {
      m_valueScriber.doScribe(value, localName, uri, tp, channelStream, scriber);
    }

    public override void scribe(string value, Scribble scribble, int localName, int uri, int tp, Stream ostream, Scriber scriber) {
      m_valueScriber.scribe(value, scribble, localName, uri, tp, ostream, scriber);
    }

  }

}