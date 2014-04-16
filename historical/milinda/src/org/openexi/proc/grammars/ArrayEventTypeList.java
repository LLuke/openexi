package org.openexi.proc.grammars;

import org.openexi.proc.common.EventCode;
import org.openexi.proc.common.EventType;

final class ArrayEventTypeList extends AbstractEventTypeList {
  
  private EventType[] m_eventTypes;
  
  private static final EventTypeSchemaAttribute[] SCHEMA_ATTRIBUTES_NONE;
  private static final EventTypeSchemaAttributeInvalid[] SCHEMA_ATTRIBUTES_INVALID_NONE;
  private static final EventTypeSchemaAttributeWildcardNS[] SCHEMA_ATTRIBUTES_WILDCARD_NS;
  static {
    SCHEMA_ATTRIBUTES_NONE = new EventTypeSchemaAttribute[0];
    SCHEMA_ATTRIBUTES_INVALID_NONE = new EventTypeSchemaAttributeInvalid[0];
    SCHEMA_ATTRIBUTES_WILDCARD_NS = new EventTypeSchemaAttributeWildcardNS[0];
  }
  
  private EventTypeSchemaAttribute[] m_schemaAttributes;
  private EventTypeSchemaAttributeInvalid[] m_schemaAttributesInvalid;
  private int m_n_schemaAttributes;
  private int m_n_schemaAttributesInvalid;
  private EventTypeSchemaAttributeWildcardAny m_schemaAttributeWildcard;
  private EventTypeAttributeWildcardAnyUntyped m_attributeWildcardAnyUntyped;
  private EventTypeSchemaAttributeWildcardNS[] m_schemaAttributeWildcardNS;
  private int m_n_schemaAttributesWildcardNS;
  
  private EventTypeSchemaCharacters m_schemaCharacters;
  private EventType m_characters;
  
  private EventTypeNamespaceDeclaration m_namespaceDeclaration;
  
  ArrayEventTypeList() {
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
  
  final void setItems(AbstractEventType[] eventTypes) {
    assert m_eventTypes == null;
    m_eventTypes = eventTypes;
    int i, len;
    for (i = 0, len = m_eventTypes.length; i < len; i++) {
      final AbstractEventType eventType = eventTypes[i]; 
      eventType.setIndex(i);
      final short itemType = eventType.itemType;
      switch (itemType) {
        case EventCode.ITEM_SCHEMA_AT:
          if (m_schemaAttributes.length == m_n_schemaAttributes) {
            final int sz = m_n_schemaAttributes == 0 ? 4 : 2 * m_n_schemaAttributes;
            EventTypeSchemaAttribute[] schemaAttributes = new EventTypeSchemaAttribute[sz];
            if (m_n_schemaAttributes != 0)
              System.arraycopy(m_schemaAttributes, 0, schemaAttributes, 0, m_n_schemaAttributes);
            m_schemaAttributes = schemaAttributes;
          }
          m_schemaAttributes[m_n_schemaAttributes++] = (EventTypeSchemaAttribute)eventType;
          break;
        case EventCode.ITEM_SCHEMA_UNDECLARED_AT_INVALID_VALUE:
          if (m_schemaAttributesInvalid.length == m_n_schemaAttributesInvalid) {
            final int sz = m_n_schemaAttributesInvalid == 0 ? 4 : 2 * m_n_schemaAttributesInvalid;
            EventTypeSchemaAttributeInvalid[] schemaAttributesInvalid = new EventTypeSchemaAttributeInvalid[sz];
            if (m_n_schemaAttributesInvalid != 0)
              System.arraycopy(m_schemaAttributesInvalid, 0, schemaAttributesInvalid, 0, m_n_schemaAttributesInvalid);
            m_schemaAttributesInvalid = schemaAttributesInvalid;
          }
          m_schemaAttributesInvalid[m_n_schemaAttributesInvalid++] = (EventTypeSchemaAttributeInvalid)eventType;
          break;
        case EventCode.ITEM_SCHEMA_CH:
          m_schemaCharacters = (EventTypeSchemaCharacters)eventType;
          break;
        case EventCode.ITEM_SCHEMA_CH_MIXED:
          assert m_characters == null;
          m_characters = (EventTypeSchemaMixedCharacters)eventType;
          break;
        case EventCode.ITEM_CH:
          assert m_characters == null || m_characters.itemType == EventCode.ITEM_SCHEMA_CH_MIXED || 
            eventType.getGrammar().m_grammarType == Grammar.SCHEMA_GRAMMAR_ELEMENT_FRAGMENT;
          if (m_characters == null)
            m_characters = (EventTypeCharacters)eventType;
          break;
        case EventCode.ITEM_NS:
          m_namespaceDeclaration = (EventTypeNamespaceDeclaration)eventType;
          break;
        case EventCode.ITEM_SCHEMA_AT_WC_ANY:
          if (m_schemaAttributeWildcard == null) {
            m_schemaAttributeWildcard = (EventTypeSchemaAttributeWildcardAny)eventType;
            break;
          }
          assert m_schemaAttributeWildcard.itemType == EventCode.ITEM_SCHEMA_AT_WC_ANY;
          break;
        case EventCode.ITEM_AT_WC_ANY_UNTYPED:
          if (m_attributeWildcardAnyUntyped == null)
            m_attributeWildcardAnyUntyped = (EventTypeAttributeWildcardAnyUntyped)eventType;
          break;
        case EventCode.ITEM_SCHEMA_AT_WC_NS:
          if (m_schemaAttributeWildcardNS.length == m_n_schemaAttributesWildcardNS) {
            final int sz = m_n_schemaAttributesWildcardNS == 0 ? 4 : 2 * m_n_schemaAttributesWildcardNS;
            EventTypeSchemaAttributeWildcardNS[] schemaAttributeWildcardNS = new EventTypeSchemaAttributeWildcardNS[sz];
            if (m_n_schemaAttributesWildcardNS != 0)
              System.arraycopy(m_schemaAttributeWildcardNS, 0, schemaAttributeWildcardNS, 0, m_n_schemaAttributesWildcardNS);
            m_schemaAttributeWildcardNS = schemaAttributeWildcardNS;
          }
          m_schemaAttributeWildcardNS[m_n_schemaAttributesWildcardNS++] = (EventTypeSchemaAttributeWildcardNS)eventType;
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
    return eventType.itemType == EventCode.ITEM_SD ? eventType : null;
  }

  @Override
  public final EventType getEE() {
    // REVISIT: replace with optimization
    int i, len;
    for (i = 0, len = m_eventTypes.length; i < len; i++) {
      final EventType eventType = m_eventTypes[i];
      final short itemType;
      if ((itemType = eventType.itemType) == EventCode.ITEM_SCHEMA_EE)
        return eventType;
      else if (itemType == EventCode.ITEM_EE)
        return eventType;
    }
    return null;
  }
  
  @Override
  public final EventTypeSchemaAttribute getSchemaAttribute(String uri, String name) {
    for (int i = 0; i < m_n_schemaAttributes; i++) {
      final EventTypeSchemaAttribute eventType = m_schemaAttributes[i];
      if (name.equals(eventType.getName()) && uri.equals(eventType.getURI()))
        return eventType;
    }
    return null;
  }
  
  @Override
  public final EventTypeSchemaAttributeInvalid getSchemaAttributeInvalid(String uri, String name) {
    for (int i = 0; i < m_n_schemaAttributesInvalid; i++) {
      final EventTypeSchemaAttributeInvalid eventType = m_schemaAttributesInvalid[i];
      if (name.equals(eventType.getName()) && uri.equals(eventType.getURI()))
        return eventType;
    }
    return null;
  }
  
  @Override
  public final EventTypeAttribute getAttribute(String uri, String name) {
    // Event types ITEM_AT never belong to ArrayEventTypeList. 
    return (EventTypeAttribute)null;
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
  public final EventTypeSchemaAttributeWildcardNS getSchemaAttributeWildcardNS(String uri) {
    for (int i = 0; i < m_n_schemaAttributesWildcardNS; i++) {
      final EventTypeSchemaAttributeWildcardNS schemaAttributeWildcardNS = m_schemaAttributeWildcardNS[i];
      if (uri.equals(schemaAttributeWildcardNS.m_uri)) {
        return schemaAttributeWildcardNS;
      }
    }
    return null;
  }

  @Override
  public final EventTypeSchemaCharacters getSchemaCharacters() {
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

  @Override
  public final boolean isMutable() {
    return false;
  }
  
}
