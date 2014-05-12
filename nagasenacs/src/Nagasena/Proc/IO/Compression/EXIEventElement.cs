using BinaryDataSource = Nagasena.Proc.Common.BinaryDataSource;
using EventType = Nagasena.Proc.Common.EventType;
using EventDescription = Nagasena.Proc.Common.EventDescription;
using EventDescription_Fields = Nagasena.Proc.Common.EventDescription_Fields;
using Characters = Nagasena.Schema.Characters;

namespace Nagasena.Proc.IO.Compression {

  internal sealed class EXIEventElement : EventDescription {

    internal string prefix;
    internal EventType eventType;

    public EXIEventElement() {
      prefix = null;
      eventType = null;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Implementation of EXIEvent interface
    ///////////////////////////////////////////////////////////////////////////

    public sbyte EventKind {
      get {
        return EventDescription_Fields.EVENT_SE;
      }
    }

    public string Name {
      get {
        return eventType.name;
      }
    }

    public string URI {
      get {
        return eventType.uri;
      }
    }

    public int NameId {
      get {
        return eventType.NameId;
      }
    }

    public int URIId {
      get {
        return eventType.URIId;
      }
    }

    public string Prefix {
      get {
        return prefix;
      }
    }

    public Characters Characters {
      get {
        return null;
      }
    }

    public BinaryDataSource BinaryDataSource {
      get {
        return null;
      }
    }

    public EventType getEventType() {
      return eventType;
    }

  }

}