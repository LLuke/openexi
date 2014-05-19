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
  public sealed class EXIEventSchemaType : EventDescription {

    private readonly EventType m_eventType;
    private readonly int m_tp;
    private readonly string m_typeUri;
    private readonly string m_typeLocalName;
    private readonly string m_typePrefix;
    private readonly string m_prefix;
    private readonly Characters m_typeQualifiedName;

    public EXIEventSchemaType(int tp, string typeUri, string typeLocalName, string typePrefix, Characters typeQualifiedName, string prefix, EventType eventType) {
      Debug.Assert(eventType.itemType == EventType.ITEM_SCHEMA_TYPE || eventType.itemType == EventType.ITEM_AT_WC_ANY_UNTYPED || eventType.itemType == EventType.ITEM_AT);
      m_eventType = eventType;
      m_tp = tp;
      m_typeUri = typeUri;
      m_typeLocalName = typeLocalName;
      m_typePrefix = typePrefix;
      m_typeQualifiedName = typeQualifiedName;
      m_prefix = prefix;
    }

    public int Tp {
      get {
        return m_tp;
      }
    }

    public string TypeURI {
      get {
        return m_typeUri;
      }
    }

    public string TypeName {
      get {
        return m_typeLocalName;
      }
    }

    public string TypePrefix {
      get {
        return m_typePrefix;
      }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Implementation of EXIEvent interface
    ///////////////////////////////////////////////////////////////////////////

    public sbyte EventKind {
      get {
        return EventDescription_Fields.EVENT_TP;
      }
    }

    public string URI {
      get {
        return XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI;
      }
    }

    public string Name {
      get {
        return "type";
      }
    }

    public int NameId {
      get {
        return EXISchemaConst.XSI_LOCALNAME_TYPE_ID;
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
    /// Returns the qualified name of the type when lexical preservation is on, 
    /// otherwise returns null. 
    /// </summary>
    public Characters Characters {
      get {
        return m_typeQualifiedName;
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