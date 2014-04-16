using System.Diagnostics;

using BinaryDataSource = Nagasena.Proc.Common.BinaryDataSource;
using EventType = Nagasena.Proc.Common.EventType;
using EventDescription = Nagasena.Proc.Common.EventDescription;
using EventDescription_Fields = Nagasena.Proc.Common.EventDescription_Fields;
using Characters = Nagasena.Schema.Characters;

namespace Nagasena.Proc.Events {

  public sealed class EXIEventNS : EventDescription {

    private string m_uri; // "" represents disassociation
    private string m_prefix; // "" represents the default (i.e. no prefix)
    private bool m_localElementNs;
    private EventType m_eventType;

    public EXIEventNS(string prefix, string uri, bool localElementNs, EventType eventType) {
      Debug.Assert(eventType.itemType == EventType.ITEM_NS);

      m_prefix = prefix;
      m_uri = uri;
      m_localElementNs = localElementNs;
      m_eventType = eventType;
    }

    public sbyte EventKind {
      get {
        return EventDescription_Fields.EVENT_NS;
      }
    }

    public string URI {
      get {
        return m_uri;
      }
    }

    public string Name {
      get {
        return null;
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

    public bool LocalElementNs {
      get {
        return m_localElementNs;
      }
    }

    public EventType getEventType() {
      return m_eventType;
    }

    public string Prefix {
      get {
        return m_prefix;
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

  }

}