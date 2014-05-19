using System.Diagnostics;

using BinaryDataSource = Nagasena.Proc.Common.BinaryDataSource;
using EventType = Nagasena.Proc.Common.EventType;
using EventDescription = Nagasena.Proc.Common.EventDescription;
using EventDescription_Fields = Nagasena.Proc.Common.EventDescription_Fields;
using Characters = Nagasena.Schema.Characters;

namespace Nagasena.Proc.Events {

  /// <exclude/>
  public sealed class EXIEventDTD : EventDescription {

    private readonly string m_name;
    private readonly string m_publicId;
    private readonly string m_systemId;
    private readonly Characters m_text;
    private readonly EventType m_eventType;

    public EXIEventDTD(string name, string publicId, string systemId, Characters text, EventType eventType) {
      Debug.Assert(eventType.itemType == EventType.ITEM_DTD);
      m_name = name;
      m_publicId = publicId;
      m_systemId = systemId;
      m_text = text;
      m_eventType = eventType;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Accessors
    ///////////////////////////////////////////////////////////////////////////

    public string PublicId {
      get {
        return m_publicId;
      }
    }

    public string SystemId {
      get {
        return m_systemId;
      }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Implementation of EXIEvent interface
    ///////////////////////////////////////////////////////////////////////////

    public sbyte EventKind {
      get {
        return EventDescription_Fields.EVENT_DTD;
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

    public int URIId {
      get {
        return -1;
      }
    }

    public int NameId {
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
        return m_text;
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