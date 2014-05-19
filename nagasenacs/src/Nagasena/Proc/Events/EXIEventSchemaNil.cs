using System.Diagnostics;

using BinaryDataSource = Nagasena.Proc.Common.BinaryDataSource;
using EventType = Nagasena.Proc.Common.EventType;
using EventDescription = Nagasena.Proc.Common.EventDescription;
using EventDescription_Fields = Nagasena.Proc.Common.EventDescription_Fields;
using XmlUriConst = Nagasena.Proc.Common.XmlUriConst;
using Characters = Nagasena.Schema.Characters;
using EXISchemaConst = Nagasena.Schema.EXISchemaConst;

namespace Nagasena.Proc.Events {

  /// <exclude/>
  public sealed class EXIEventSchemaNil : EventDescription {

    private readonly EventType m_eventType;
    private readonly bool m_nilled;
    private readonly string m_prefix;
    private readonly Characters m_lexicalValue;

    public EXIEventSchemaNil(bool nilled, Characters lexicalValue, string prefix, EventType eventType) {
      Debug.Assert(eventType.itemType == EventType.ITEM_SCHEMA_NIL);
      m_nilled = nilled;
      m_lexicalValue = lexicalValue;
      m_eventType = eventType;
      m_prefix = prefix;
    }

    public bool Nilled {
      get {
        return m_nilled;
      }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Implementation of EXIEvent interface
    ///////////////////////////////////////////////////////////////////////////

    public sbyte EventKind {
      get {
        return EventDescription_Fields.EVENT_NL;
      }
    }

    public string URI {
      get {
        return XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI;
      }
    }

    public string Name {
      get {
        return "nil";
      }
    }

    public int NameId {
      get {
        return EXISchemaConst.XSI_LOCALNAME_NIL_ID;
      }
    }

    public int URIId {
      get {
        return XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI_ID;
      }
    }

    public string Prefix {
      get {
        return m_prefix;
      }
    }

    /// <summary>
    /// Returns the lexical value when lexical preservation is on, 
    /// otherwise returns null. 
    /// </summary>
    public Characters Characters {
      get {
        return m_lexicalValue;
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