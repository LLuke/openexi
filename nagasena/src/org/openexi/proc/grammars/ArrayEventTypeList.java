package org.openexi.proc.grammars;

import org.openexi.proc.common.EventType;
import org.openexi.proc.common.EventTypeList;

final class ArrayEventTypeList extends EventTypeList {
  
  private EventType[] m_eventTypes;
  
  private static final EventTypeSchema[] SCHEMA_ATTRIBUTES_NONE;
  private static final EventType[] SCHEMA_ATTRIBUTES_INVALID_NONE;
  private static final EventType[] SCHEMA_ATTRIBUTES_WILDCARD_NS;
  static {
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
  
  ArrayEventTypeList() {
    super(false);
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
  
  final void setItems(EventType[] eventTypes) {
    assert m_eventTypes == null;
    m_eventTypes = eventTypes;
    int i, len;
    for (i = 0, len = m_eventTypes.length; i < len; i++) {
      final EventType eventType = eventTypes[i]; 
      eventType.setIndex(i);
      final short itemType = eventType.itemType;
      switch (itemType) {
        case EventType.ITEM_SCHEMA_AT:
          if (m_schemaAttributes.length == m_n_schemaAttributes) {
            final int sz = m_n_schemaAttributes == 0 ? 4 : 2 * m_n_schemaAttributes;
            EventTypeSchema[] schemaAttributes = new EventTypeSchema[sz];
            if (m_n_schemaAttributes != 0)
              System.arraycopy(m_schemaAttributes, 0, schemaAttributes, 0, m_n_schemaAttributes);
            m_schemaAttributes = schemaAttributes;
          }
          m_schemaAttributes[m_n_schemaAttributes++] = (EventTypeSchema)eventType;
          break;
        case EventType.ITEM_SCHEMA_AT_INVALID_VALUE:
          if (m_schemaAttributesInvalid.length == m_n_schemaAttributesInvalid) {
            final int sz = m_n_schemaAttributesInvalid == 0 ? 4 : 2 * m_n_schemaAttributesInvalid;
            EventType[] schemaAttributesInvalid = new EventType[sz];
            if (m_n_schemaAttributesInvalid != 0)
              System.arraycopy(m_schemaAttributesInvalid, 0, schemaAttributesInvalid, 0, m_n_schemaAttributesInvalid);
            m_schemaAttributesInvalid = schemaAttributesInvalid;
          }
          m_schemaAttributesInvalid[m_n_schemaAttributesInvalid++] = eventType;
          break;
        case EventType.ITEM_SCHEMA_CH:
          m_schemaCharacters = eventType;
          break;
        case EventType.ITEM_SCHEMA_CH_MIXED:
          assert m_characters == null;
          m_characters = eventType;
          break;
        case EventType.ITEM_CH:
//          assert m_characters == null || m_characters.itemType == EventType.ITEM_SCHEMA_CH_MIXED || 
//            eventType.getGrammar().m_grammarType == AbstractGrammar.SCHEMA_GRAMMAR_ELEMENT_FRAGMENT;
          if (m_characters == null)
            m_characters = eventType;
          break;
        case EventType.ITEM_NS:
          m_namespaceDeclaration = eventType;
          break;
        case EventType.ITEM_SCHEMA_AT_WC_ANY:
          if (m_schemaAttributeWildcard == null) {
            m_schemaAttributeWildcard = eventType;
            break;
          }
          assert m_schemaAttributeWildcard.itemType == EventType.ITEM_SCHEMA_AT_WC_ANY;
          break;
        case EventType.ITEM_AT_WC_ANY_UNTYPED:
          if (m_attributeWildcardAnyUntyped == null)
            m_attributeWildcardAnyUntyped = eventType;
          break;
        case EventType.ITEM_SCHEMA_AT_WC_NS:
          if (m_schemaAttributeWildcardNS.length == m_n_schemaAttributesWildcardNS) {
            final int sz = m_n_schemaAttributesWildcardNS == 0 ? 4 : 2 * m_n_schemaAttributesWildcardNS;
            EventType[] schemaAttributeWildcardNS = new EventType[sz];
            if (m_n_schemaAttributesWildcardNS != 0)
              System.arraycopy(m_schemaAttributeWildcardNS, 0, schemaAttributeWildcardNS, 0, m_n_schemaAttributesWildcardNS);
            m_schemaAttributeWildcardNS = schemaAttributeWildcardNS;
          }
          m_schemaAttributeWildcardNS[m_n_schemaAttributesWildcardNS++] = eventType;
          break;
        default:
          break;
      }
    }
  }

  @Override
  public int getLength() {
    return m_eventTypes.length;
  }

  @Override
  public EventType item(int i) {
    return m_eventTypes[i];
  }

  @Override
  public final EventType getSD() {
    final EventType eventType = m_eventTypes[0];
    return eventType.itemType == EventType.ITEM_SD ? eventType : null;
  }

  @Override
  public final EventType getEE() {
    // REVISIT: replace with optimization
    int i, len;
    for (i = 0, len = m_eventTypes.length; i < len; i++) {
      final EventType eventType = m_eventTypes[i];
      if (eventType.itemType == EventType.ITEM_EE)
        return eventType;
    }
    return null;
  }
  
  @Override
  public final EventTypeSchema getSchemaAttribute(String uri, String name) {
    for (int i = 0; i < m_n_schemaAttributes; i++) {
      final EventTypeSchema eventType = m_schemaAttributes[i];
      if (name.equals(eventType.name) && uri.equals(eventType.uri))
        return eventType;
    }
    return null;
  }
  
  @Override
  public final EventType getSchemaAttributeInvalid(String uri, String name) {
    for (int i = 0; i < m_n_schemaAttributesInvalid; i++) {
      final EventType eventType = m_schemaAttributesInvalid[i];
      if (name.equals(eventType.name) && uri.equals(eventType.uri))
        return eventType;
    }
    return null;
  }
  
  @Override
  public final EventType getLearnedAttribute(String uri, String name) {
    // Event types ITEM_AT never belong to ArrayEventTypeList. 
    return (EventType)null;
  }

  @Override
  public final EventType getSchemaAttributeWildcardAny() {
    return m_schemaAttributeWildcard;
  }
  
  @Override
  public final EventType getAttributeWildcardAnyUntyped() {
    return m_attributeWildcardAnyUntyped;
  }

  @Override
  public final EventType getSchemaAttributeWildcardNS(String uri) {
    for (int i = 0; i < m_n_schemaAttributesWildcardNS; i++) {
      final EventType schemaAttributeWildcardNS = m_schemaAttributeWildcardNS[i];
      if (uri.equals(schemaAttributeWildcardNS.uri)) {
        return schemaAttributeWildcardNS;
      }
    }
    return null;
  }

  @Override
  public final EventType getSchemaCharacters() {
    return m_schemaCharacters; 
  }

  @Override
  public final EventType getCharacters() {
    return m_characters; 
  }

  @Override
  public EventType getNamespaceDeclaration() {
    return m_namespaceDeclaration;
  }

}
