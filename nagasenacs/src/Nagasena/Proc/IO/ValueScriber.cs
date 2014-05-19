using System.IO;

using ValueApparatus = Nagasena.Proc.Grammars.ValueApparatus;
using EXISchema = Nagasena.Schema.EXISchema;

namespace Nagasena.Proc.IO {

  /// <exclude/>
  public abstract class ValueScriber : ValueApparatus {

    public abstract int getBuiltinRCS(int simpleType, Scriber scriber);

    ////////////////////////////////////////////////////////////

    public abstract bool process(string value, int tp, EXISchema schema, Scribble scribble, Scriber scriber);

    public abstract void scribe(string value, Scribble scribble, int localName, int uri, int tp, Scriber scriber);

    /// <summary>
    /// ScriberValueHolder calls this method to write out an Object to a channelStream.
    /// </summary>
    public abstract void scribe(string value, Scribble scribble, int localName, int uri, int tp, Stream channelStream, Scriber scriber);

    ////////////////////////////////////////////////////////////

    public abstract object toValue(string value, Scribble scribble, Scriber scriber);

    public abstract void doScribe(object value, int localName, int uri, int tp, Stream channelStream, Scriber scriber);

  }

}