using System.IO;

using QName = Nagasena.Proc.Common.QName;
using StringTable = Nagasena.Proc.Common.StringTable;
using LocalValuePartition = Nagasena.Proc.Common.StringTable.LocalValuePartition;
using ExiUriConst = Nagasena.Proc.Util.ExiUriConst;
using Characters = Nagasena.Schema.Characters;
using EXISchema = Nagasena.Schema.EXISchema;

namespace Nagasena.Proc.IO {

  internal sealed class StringValueScriber : ValueScriberBase {

    public StringValueScriber() : base(new QName("exi:string", ExiUriConst.W3C_2009_EXI_URI)) {
    }

    public override short CodecID {
      get {
        return Scriber.CODEC_STRING;
      }
    }

    public override bool process(string value, int tp, EXISchema schema, Scribble scribble, Scriber scriber) {
      scribble.stringValue1 = value;
      return true;
    }

    public override void scribe(string value, Scribble scribble, int localName, int uri, int tp, Stream channelStream, Scriber scriber) {
      int length = value.Length;
      Characters characterSequence = scriber.ensureCharacters(length).addString(value, length);
      scribeStringValue(characterSequence, localName, uri, tp, channelStream, scriber);
    }

    internal void scribeStringValue(Characters value, int localNameId, int uriId, int tp, Stream ostream, Scriber scriber) {
      StringTable.GlobalValuePartition globalPartition = scriber.stringTable.globalValuePartition;
      StringTable.GlobalEntry entry;
      if ((entry = globalPartition.getEntry(value)) == null) {
        int length = value.length;
        scriber.writeLiteralCharacters(value, length, 2, tp, ostream);
        if (length != 0 && length < scriber.valueMaxExclusiveLength) {
          globalPartition.addValue(value, localNameId, uriId);
        }
        return;
      }
      StringTable.LocalValuePartition localPartition;
      if ((localPartition = entry.localPartition) == globalPartition.getLocalPartition(localNameId, uriId)) {
        scriber.writeUnsignedInteger32(0, ostream);
        scriber.writeNBitUnsigned(entry.localEntry.number, localPartition.width, ostream);
        return;
      }
      scriber.writeUnsignedInteger32(1, ostream);
      scriber.writeNBitUnsigned(entry.number, globalPartition.width, ostream);
    }

    ////////////////////////////////////////////////////////////

    public override object toValue(string value, Scribble scribble, Scriber scriber) {
      int length = value.Length;
      return scriber.ensureCharacters(length).addString(value, length);
    }

    public override void doScribe(object value, int localName, int uri, int tp, Stream channelStream, Scriber scriber) {
      scribeStringValue((Characters)value, localName, uri, tp, channelStream, scriber);
    }

  }

}