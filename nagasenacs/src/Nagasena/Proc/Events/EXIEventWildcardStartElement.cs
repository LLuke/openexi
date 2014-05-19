using System.Diagnostics;

using BinaryDataSource = Nagasena.Proc.Common.BinaryDataSource;
using EventType = Nagasena.Proc.Common.EventType;
using EventDescription = Nagasena.Proc.Common.EventDescription;
using EventDescription_Fields = Nagasena.Proc.Common.EventDescription_Fields;
using Characters = Nagasena.Schema.Characters;

namespace Nagasena.Proc.Events {

  /// <exclude/>
  public sealed class EXIEventWildcardStartElement : EventDescription {

    private readonly string m_uri;
    private readonly string m_name;
    private readonly int m_uriId;
    private readonly int m_nameId;
    private readonly string m_prefix;

    private readonly EventType m_eventType;

    public EXIEventWildcardStartElement(string uri, string name, int uriId, int nameId, string prefix, EventType eventType) {
      Debug.Assert(eventType.itemType == EventType.ITEM_SCHEMA_WC_ANY || eventType.itemType == EventType.ITEM_SCHEMA_WC_NS || eventType.itemType == EventType.ITEM_SE_WC);
      m_uri = uri;
      m_name = name;
      m_uriId = uriId;
      m_nameId = nameId;
      m_prefix = prefix;
      m_eventType = eventType;
    }

    public sbyte EventKind {
      get {
        return EventDescription_Fields.EVENT_SE;
      }
    }

    public string Name {
      get {
        return m_name;
      }
    }

    public string URI {
      get {
        return m_uri;
      }
    }

    public int NameId {
      get {
        return m_nameId;
      }
    }

    public int URIId {
      get {
        return m_uriId;
      }
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

    public EventType getEventType() {
      return m_eventType;
    }

  }

}