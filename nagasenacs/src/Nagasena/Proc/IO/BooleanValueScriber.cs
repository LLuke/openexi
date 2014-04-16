using System.IO;

using QName = Nagasena.Proc.Common.QName;
using ExiUriConst = Nagasena.Proc.Util.ExiUriConst;
using EXISchema = Nagasena.Schema.EXISchema;

namespace Nagasena.Proc.IO {

  internal sealed class BooleanValueScriber : ValueScriberBase {

    public static readonly BooleanValueScriber instance;
    static BooleanValueScriber() {
      instance = new BooleanValueScriber();
    }

    private BooleanValueScriber() : base(new QName("exi:boolean", ExiUriConst.W3C_2009_EXI_URI)) {
    }

    public override short CodecID {
      get {
        return Scriber.CODEC_BOOLEAN;
      }
    }

    public override int getBuiltinRCS(int simpleType, Scriber scriber) {
      return BuiltinRCS.RCS_ID_BOOLEAN;
    }

    ////////////////////////////////////////////////////////////

    public override bool process(string value, int tp, EXISchema schema, Scribble scribble, Scriber scriber) {
      if (!trimWhitespaces(value)) {
        return false;
      }

      bool distinguishLexicalValues = scribble.booleanValue2 = schema.isPatternedBooleanSimpleType(tp);

      int len = limitPosition - startPosition;
      if (len == 4 && value[startPosition] == 't' && value[startPosition + 1] == 'r' && 
          value[startPosition + 2] == 'u' && value[startPosition + 3] == 'e') {
        if (distinguishLexicalValues) {
          scribble.intValue1 = 2;
        }
        else {
          scribble.booleanValue1 = true;
        }
      }
      else if (len == 5 && value[startPosition] == 'f' && value[startPosition + 1] == 'a' && 
               value[startPosition + 2] == 'l' && value[startPosition + 3] == 's' && value[startPosition + 4] == 'e') {
        if (distinguishLexicalValues) {
          scribble.intValue1 = 0;
        }
        else {
          scribble.booleanValue1 = false;
        }
      }
      else if (len == 1) {
        char c;
        if ((c = value[startPosition]) == '1') {
          if (distinguishLexicalValues) {
            scribble.intValue1 = 3;
          }
          else {
            scribble.booleanValue1 = true;
          }
        }
        else if (c == '0') {
          if (distinguishLexicalValues) {
            scribble.intValue1 = 1;
          }
          else {
            scribble.booleanValue1 = false;
          }
        }
        else {
          return false;
        }
      }
      else {
        return false;
      }

      return true;
    }

    public override void scribe(string value, Scribble scribble, int localName, int uri, int tp, Stream channelStream, Scriber scriber) {
      if (scribble.booleanValue2) {
        scriber.writeNBitUnsigned(scribble.intValue1, 2, channelStream);
      }
      else {
        scriber.writeBoolean(scribble.booleanValue1, channelStream);
      }
    }

    private class BooleanValue {
      internal bool distinguishLexicalValues;
      internal bool booleanValue;
      internal sbyte lexicalValueId;
      internal BooleanValue(bool preserveLexicalValue, bool booleanValue, sbyte lexicalValueId) {
        this.distinguishLexicalValues = preserveLexicalValue;
        this.booleanValue = booleanValue;
        this.lexicalValueId = lexicalValueId;
      }
    }

    public override object toValue(string value, Scribble scribble, Scriber scriber) {
      if (scribble.booleanValue2) {
        return new BooleanValue(true, true, (sbyte)scribble.intValue1);
      }
      else {
        return new BooleanValue(false, scribble.booleanValue1, (sbyte)-1);
      }
    }

    public override void doScribe(object value, int localName, int uri, int tp, Stream channelStream, Scriber scriber) {
      BooleanValue booleanValue = (BooleanValue)value;
      if (booleanValue.distinguishLexicalValues) {
        scriber.writeNBitUnsigned(booleanValue.lexicalValueId, 2, channelStream);
      }
      else {
        scriber.writeBoolean(booleanValue.booleanValue, channelStream);
      }
    }

  }

}