using System;
using System.Collections.Generic;

using EventType = Nagasena.Proc.Common.EventType;
using EventTypeList = Nagasena.Proc.Common.EventTypeList;

namespace Nagasena.Proc.Grammars {

  internal sealed class ReversedEventTypeList : EventTypeList {

    private readonly IList<EventType> m_eventTypes; // items in reverse order.
    private int m_n_eventTypes;

    internal bool hasDepthOneEE;
    private EventType m_eventTypeEndElement;

    internal bool hasDepthOneCH;
    private EventType m_eventTypeCharacters;

    private EventType m_namespaceDeclaration;
    private EventType m_attributeWildcardAnyUntyped;

    private int m_initial_n_eventTypes;
    private int m_initial_n_attributes;
    private EventType m_initialEventTypeEndElement;
    private EventType m_initialEventTypeCharacters;

    private static readonly EventType[] ATTRIBUTES_NONE;
    static ReversedEventTypeList() {
      ATTRIBUTES_NONE = new EventType[0];
    }

    private EventType[] m_attributes;
    private int m_n_attributes;

    internal ReversedEventTypeList() : base(true) {
      m_eventTypes = new List<EventType>(16);
      m_n_eventTypes = 0;
      hasDepthOneEE = false;
      m_eventTypeEndElement = null;
      hasDepthOneCH = false;
      m_eventTypeCharacters = null;
      m_attributes = ATTRIBUTES_NONE;
      m_n_attributes = 0;
      m_namespaceDeclaration = null;
      m_attributeWildcardAnyUntyped = null;
    }

    internal void checkPoint() {
      m_initial_n_eventTypes = m_n_eventTypes;
      m_initial_n_attributes = m_n_attributes;
      m_initialEventTypeEndElement = m_eventTypeEndElement;
      m_initialEventTypeCharacters = m_eventTypeCharacters;

    }

    internal void reset() {
      hasDepthOneEE = hasDepthOneCH = false;
      m_n_eventTypes = m_initial_n_eventTypes;
      m_n_attributes = m_initial_n_attributes;
      m_eventTypeEndElement = m_initialEventTypeEndElement;
      m_eventTypeCharacters = m_initialEventTypeCharacters;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Implementation of EventTypeList APIs
    ///////////////////////////////////////////////////////////////////////////

    public override int Length {
      get {
        return m_n_eventTypes;
      }
    }

    public override EventType SD {
      get {
        return null;
      }
    }

    public override EventType EE {
      get {
        return m_eventTypeEndElement;
      }
    }

    public override EventType getSchemaAttribute(string uri, string name) {
      return null;
    }

    public override EventType getSchemaAttributeInvalid(string uri, string name) {
      return null;
    }

    public override EventType getLearnedAttribute(string uri, string name) {
      for (int i = 0; i < m_n_attributes; i++) {
        EventType eventType = m_attributes[i];
        if (name.Equals(eventType.name) && uri.Equals(eventType.uri)) {
          return eventType;
        }
      }
      return null;
    }

    public override EventType SchemaAttributeWildcardAny {
      get {
        return null;
      }
    }

    public override EventType AttributeWildcardAnyUntyped {
      get {
        return m_attributeWildcardAnyUntyped;
      }
    }

    public override EventType getSchemaAttributeWildcardNS(string uri) {
      return null;
    }

    public override EventType SchemaCharacters {
      get {
        return null;
      }
    }

    public override EventType Characters {
      get {
        return m_eventTypeCharacters;
      }
    }

    public override EventType NamespaceDeclaration {
      get {
        return m_namespaceDeclaration;
      }
    }

    public override EventType item(int i) {
      return m_eventTypes[m_n_eventTypes - (i + 1)];
    }

    ///////////////////////////////////////////////////////////////////////////
    // Methods peculiar to ReversedEventTypeList
    ///////////////////////////////////////////////////////////////////////////

    /// <summary>
    /// Add an eventType as the head (i.e. at the end of ArrayList).
    /// </summary>
    internal void add(EventType eventType) {
      switch (eventType.itemType) {
        case EventType.ITEM_EE:
          m_eventTypeEndElement = eventType;
          if (eventType.Depth == 1) {
            hasDepthOneEE = true;
          }
          break;
        case EventType.ITEM_CH:
          m_eventTypeCharacters = eventType;
          if (eventType.Depth == 1) {
            hasDepthOneCH = true;
          }
          break;
        case EventType.ITEM_AT:
          if (m_attributes.Length == m_n_attributes) {
            int sz = m_n_attributes == 0 ? 4 : 2 * m_n_attributes;
            EventType[] attributes = new EventType[sz];
            if (m_n_attributes != 0) {
              Array.Copy(m_attributes, 0, attributes, 0, m_n_attributes);
            }
            m_attributes = attributes;
          }
          m_attributes[m_n_attributes++] = eventType;
          break;
        case EventType.ITEM_NS:
          m_namespaceDeclaration = eventType;
          break;
        case EventType.ITEM_AT_WC_ANY_UNTYPED:
          m_attributeWildcardAnyUntyped = eventType;
          break;
      }
      m_eventTypes.Add(eventType);
      eventType.Index = m_n_eventTypes++;
    }

  }

}