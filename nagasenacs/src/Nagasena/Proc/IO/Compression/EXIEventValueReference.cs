using System.IO;

using BinaryDataSource = Nagasena.Proc.Common.BinaryDataSource;
using EventDescription = Nagasena.Proc.Common.EventDescription;
using EventDescription_Fields = Nagasena.Proc.Common.EventDescription_Fields;
using EventType = Nagasena.Proc.Common.EventType;
using Apparatus = Nagasena.Proc.Grammars.Apparatus;
using Characters = Nagasena.Schema.Characters;
using EXISchema = Nagasena.Schema.EXISchema;

namespace Nagasena.Proc.IO.Compression {

  /// <exclude/>
  public sealed class EXIEventValueReference : EventDescription {

    internal sbyte eventKind;

    internal EventType eventType;

    internal string prefix;
    internal string uri;
    internal string name;

    internal int uriId;
    internal int nameId;
    internal int tp;

    internal Characters text;
    internal BinaryDataSource binaryData;

    internal EXIEventValueReference() {
      eventKind = -1;
      eventType = null;
      prefix = null;
      uri = null;
      name = null;
      uriId = -1;
      nameId = -1;
      tp = EXISchema.NIL_NODE;
      text = null;
      binaryData = null;
    }

    internal void scanText(Scanner scanner, bool binaryDataEnabled, Stream istream) {
      ValueScanner valueScanner = scanner.getValueScanner(tp);
      if (binaryDataEnabled) {
        short codecId = valueScanner.CodecID;
        if (codecId == Apparatus.CODEC_BASE64BINARY || codecId == Apparatus.CODEC_HEXBINARY) {
          binaryData = ((BinaryValueScanner)valueScanner).scan(-1, (BinaryDataSource)null);
          eventKind = EventDescription_Fields.EVENT_BLOB;
          return;
        }
      }
      text = valueScanner.scan(nameId, uriId, tp);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Implementation of EventDescription interface
    ///////////////////////////////////////////////////////////////////////////

    public sbyte EventKind {
      get {
        return eventKind;
      }
    }

    public int URIId {
      get {
        return uriId;
      }
    }

    public int NameId {
      get {
        return nameId;
      }
    }

    public string Prefix {
      get {
        return prefix;
      }
    }

    public Characters Characters {
      get {
        return text;
      }
    }

    public BinaryDataSource BinaryDataSource {
      get {
        return binaryData;
      }
    }

    public EventType getEventType() {
      return eventType;
    }

    public string Name {
      get {
        return name;
      }
    }

    public string URI {
      get {
        return uri;
      }
    }

  }

}