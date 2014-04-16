using System.Diagnostics;
using System.IO;

using QName = Nagasena.Proc.Common.QName;
using Apparatus = Nagasena.Proc.Grammars.Apparatus;
using Characters = Nagasena.Schema.Characters;
using EXISchema = Nagasena.Schema.EXISchema;

namespace Nagasena.Proc.IO {

  public sealed class ValueScriberLexical : ValueScriber {

    private readonly ValueScriber m_baseValueScriber;
    private readonly StringValueScriber m_stringValueScriber;

    internal ValueScriberLexical(ValueScriber valueScriber, StringValueScriber stringValueScriber) {
      m_baseValueScriber = valueScriber;
      m_stringValueScriber = stringValueScriber;
    }

    public override bool process(string value, int tp, EXISchema schema, Scribble scribble, Scriber scriber) {
      return true;
    }

    public override void scribe(string value, Scribble scribble, int localName, int uri, int tp, Scriber scriber) {
      scribe(value, scribble, localName, uri, tp, (Stream)null, scriber);
    }

    public override void scribe(string value, Scribble scribble, int localName, int uri, int tp, Stream channelStream, Scriber scriber) {
      int length = value.Length;
      CharacterBuffer characterBuffer = scriber.ensureCharacters(length);
      Characters characterSequence = characterBuffer.addString(value, length);
      m_stringValueScriber.scribeStringValue(characterSequence, localName, uri, m_baseValueScriber.getBuiltinRCS(tp, scriber), channelStream, scriber);
    }

    public override object toValue(string value, Scribble scribble, Scriber scriber) {
      return value;
    }

    public override void doScribe(object value, int localName, int uri, int tp, Stream channelStream, Scriber scriber) {
      scribe((string)value, (Scribble)null, localName, uri, tp, channelStream, scriber);
    }

    public override QName Name {
      get {
        // DTRM must not take effect when the value of the Preserve.lexicalValues 
        // fidelity option is true. 
        Debug.Assert(false);
        return (QName)null;
      }
    }

    public override short CodecID {
      get {
        return Apparatus.CODEC_LEXICAL;
      }
    }

    public override int getBuiltinRCS(int simpleType, Scriber scriber) {
      return m_baseValueScriber.getBuiltinRCS(simpleType, scriber);
    }

  }

}