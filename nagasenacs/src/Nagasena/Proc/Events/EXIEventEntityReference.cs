using System.Diagnostics;

using BinaryDataSource = Nagasena.Proc.Common.BinaryDataSource;
using EventType = Nagasena.Proc.Common.EventType;
using EventDescription = Nagasena.Proc.Common.EventDescription;
using EventDescription_Fields = Nagasena.Proc.Common.EventDescription_Fields;
using Characters = Nagasena.Schema.Characters;

namespace Nagasena.Proc.Events {

  /// <exclude/>
  public sealed class EXIEventEntityReference : EventDescription {

    private readonly string m_name;
    private readonly EventType m_eventType;

    public EXIEventEntityReference(string name, EventType eventType) {
      Debug.Assert(eventType.itemType == EventType.ITEM_ER);
      m_name = name;
      m_eventType = eventType;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Implementation of EXIEvent interface
    ///////////////////////////////////////////////////////////////////////////

    public sbyte EventKind {
      get {
        return EventDescription_Fields.EVENT_ER;
      }
    }

    public string URI {
      get {
        return null;
      }
    }

    public string Name {
      get {
        return m_name;
      }
    }

    public int NameId {
      get {
        return -1;
      }
    }

    public int URIId {
      get {
        return -1;
      }
    }

    public string Prefix {
      get {
        return null;
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
      return m_eventType;
    }

  }

}