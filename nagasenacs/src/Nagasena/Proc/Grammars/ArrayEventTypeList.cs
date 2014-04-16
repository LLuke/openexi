using System;
using System.Diagnostics;

using EventType = Nagasena.Proc.Common.EventType;
using EventTypeList = Nagasena.Proc.Common.EventTypeList;

namespace Nagasena.Proc.Grammars {

  internal sealed class ArrayEventTypeList : EventTypeList {

    private EventType[] m_eventTypes;

    private static readonly EventTypeSchema[] SCHEMA_ATTRIBUTES_NONE;
    private static readonly EventType[] SCHEMA_ATTRIBUTES_INVALID_NONE;
    private static readonly EventType[] SCHEMA_ATTRIBUTES_WILDCARD_NS;
    static ArrayEventTypeList() {
      SCHEMA_ATTRIBUTES_NONE = new EventTypeSchema[0];
      SCHEMA_ATTRIBUTES_INVALID_NONE = new EventTypeSchema[0];
      SCHEMA_ATTRIBUTES_WILDCARD_NS = new EventType[0];
    }

    private EventTypeSchema[] m_schemaAttributes;
    private EventType[] m_schemaAttributesInvalid;
    private int m_n_schemaAttributes;
    private int m_n_schemaAttributesInvalid;
    private EventType m_schemaAttributeWildcard;
    private EventType m_attributeWildcardAnyUntyped;
    private EventType[] m_schemaAttributeWildcardNS;
    private int m_n_schemaAttributesWildcardNS;

    private EventType m_schemaCharacters;
    private EventType m_characters;

    private EventType m_namespaceDeclaration;

    internal ArrayEventTypeList() : base(false) {
      m_schemaAttributes = SCHEMA_ATTRIBUTES_NONE;
      m_schemaAttributesInvalid = SCHEMA_ATTRIBUTES_INVALID_NONE;
      m_n_schemaAttributes = 0;
      m_n_schemaAttributesInvalid = 0;
      m_schemaAttributeWildcard = null;
      m_attributeWildcardAnyUntyped = null;
      m_schemaAttributeWildcardNS = SCHEMA_ATTRIBUTES_WILDCARD_NS;
      m_n_schemaAttributesWildcardNS = 0;
      m_schemaCharacters = null;
      m_characters = null;
      m_namespaceDeclaration = null;
    }

    internal EventType[] Items {
      set {
        Debug.Assert(m_eventTypes == null);
        m_eventTypes = value;
        int i, len;
        for (i = 0, len = m_eventTypes.Length; i < len; i++) {
          EventType eventType = value[i];
          eventType.Index = i;
          short itemType = eventType.itemType;
          switch (itemType) {
            case EventType.ITEM_SCHEMA_AT:
              if (m_schemaAttributes.Length == m_n_schemaAttributes) {
                int sz = m_n_schemaAttributes == 0 ? 4 : 2 * m_n_schemaAttributes;
                EventTypeSchema[] schemaAttributes = new EventTypeSchema[sz];
                if (m_n_schemaAttributes != 0) {
                  Array.Copy(m_schemaAttributes, 0, schemaAttributes, 0, m_n_schemaAttributes);
                }
                m_schemaAttributes = schemaAttributes;
              }
              m_schemaAttributes[m_n_schemaAttributes++] = (EventTypeSchema)eventType;
              break;
            case EventType.ITEM_SCHEMA_AT_INVALID_VALUE:
              if (m_schemaAttributesInvalid.Length == m_n_schemaAttributesInvalid) {
                int sz = m_n_schemaAttributesInvalid == 0 ? 4 : 2 * m_n_schemaAttributesInvalid;
                EventType[] schemaAttributesInvalid = new EventType[sz];
                if (m_n_schemaAttributesInvalid != 0) {
                  Array.Copy(m_schemaAttributesInvalid, 0, schemaAttributesInvalid, 0, m_n_schemaAttributesInvalid);
                }
                m_schemaAttributesInvalid = schemaAttributesInvalid;
              }
              m_schemaAttributesInvalid[m_n_schemaAttributesInvalid++] = eventType;
              break;
            case EventType.ITEM_SCHEMA_CH:
              m_schemaCharacters = eventType;
              break;
            case EventType.ITEM_SCHEMA_CH_MIXED:
              Debug.Assert(m_characters == null);
              m_characters = eventType;
              break;
            case EventType.ITEM_CH:
    //          assert m_characters == null || m_characters.itemType == EventType.ITEM_SCHEMA_CH_MIXED || 
    //            eventType.getGrammar().m_grammarType == AbstractGrammar.SCHEMA_GRAMMAR_ELEMENT_FRAGMENT;
              if (m_characters == null) {
                m_characters = eventType;
              }
              break;
            case EventType.ITEM_NS:
              m_namespaceDeclaration = eventType;
              break;
            case EventType.ITEM_SCHEMA_AT_WC_ANY:
              if (m_schemaAttributeWildcard == null) {
                m_schemaAttributeWildcard = eventType;
                break;
              }
              Debug.Assert(m_schemaAttributeWildcard.itemType == EventType.ITEM_SCHEMA_AT_WC_ANY);
              break;
            case EventType.ITEM_AT_WC_ANY_UNTYPED:
              if (m_attributeWildcardAnyUntyped == null) {
                m_attributeWildcardAnyUntyped = eventType;
              }
              break;
            case EventType.ITEM_SCHEMA_AT_WC_NS:
              if (m_schemaAttributeWildcardNS.Length == m_n_schemaAttributesWildcardNS) {
                int sz = m_n_schemaAttributesWildcardNS == 0 ? 4 : 2 * m_n_schemaAttributesWildcardNS;
                EventType[] schemaAttributeWildcardNS = new EventType[sz];
                if (m_n_schemaAttributesWildcardNS != 0) {
                  Array.Copy(m_schemaAttributeWildcardNS, 0, schemaAttributeWildcardNS, 0, m_n_schemaAttributesWildcardNS);
                }
                m_schemaAttributeWildcardNS = schemaAttributeWildcardNS;
              }
              m_schemaAttributeWildcardNS[m_n_schemaAttributesWildcardNS++] = eventType;
              break;
            default:
              break;
          }
        }
      }
    }

    public override int Length {
      get {
        return m_eventTypes.Length;
      }
    }

    public override EventType item(int i) {
      return m_eventTypes[i];
    }

    public override EventType SD {
      get {
        EventType eventType = m_eventTypes[0];
        return eventType.itemType == EventType.ITEM_SD ? eventType : null;
      }
    }

    public override EventType EE {
      get {
        // REVISIT: replace with optimization
        int i, len;
        for (i = 0, len = m_eventTypes.Length; i < len; i++) {
          EventType eventType = m_eventTypes[i];
          if (eventType.itemType == EventType.ITEM_EE) {
            return eventType;
          }
        }
        return null;
      }
    }

    public override EventType getSchemaAttribute(string uri, string name) {
      for (int i = 0; i < m_n_schemaAttributes; i++) {
        EventTypeSchema eventType = m_schemaAttributes[i];
        if (name.Equals(eventType.name) && uri.Equals(eventType.uri)) {
          return eventType;
        }
      }
      return null;
    }

    public override EventType getSchemaAttributeInvalid(string uri, string name) {
      for (int i = 0; i < m_n_schemaAttributesInvalid; i++) {
        EventType eventType = m_schemaAttributesInvalid[i];
        if (name.Equals(eventType.name) && uri.Equals(eventType.uri)) {
          return eventType;
        }
      }
      return null;
    }

    public override EventType getLearnedAttribute(string uri, string name) {
      // Event types ITEM_AT never belong to ArrayEventTypeList. 
      return (EventType)null;
    }

    public override EventType SchemaAttributeWildcardAny {
      get {
        return m_schemaAttributeWildcard;
      }
    }

    public override EventType AttributeWildcardAnyUntyped {
      get {
        return m_attributeWildcardAnyUntyped;
      }
    }

    public override EventType getSchemaAttributeWildcardNS(string uri) {
      for (int i = 0; i < m_n_schemaAttributesWildcardNS; i++) {
        EventType schemaAttributeWildcardNS = m_schemaAttributeWildcardNS[i];
        if (uri.Equals(schemaAttributeWildcardNS.uri)) {
          return schemaAttributeWildcardNS;
        }
      }
      return null;
    }

    public override EventType SchemaCharacters {
      get {
        return m_schemaCharacters;
      }
    }

    public override EventType Characters {
      get {
        return m_characters;
      }
    }

    public override EventType NamespaceDeclaration {
      get {
        return m_namespaceDeclaration;
      }
    }

  }

}