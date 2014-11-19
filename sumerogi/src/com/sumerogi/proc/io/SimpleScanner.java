package com.sumerogi.proc.io;

import java.io.IOException;

import com.sumerogi.proc.common.EventDescription;
import com.sumerogi.proc.common.EventCode;
import com.sumerogi.proc.common.EventType;
import com.sumerogi.proc.events.EXIEventWildcardStartContainer;
import com.sumerogi.proc.grammars.EventCodeTuple;
import com.sumerogi.schema.Characters;

abstract class SimpleScanner extends Scanner {

//  private final EXIEventTransientCharacters m_transientCharacters;
//  private final EXIEventTransientElement m_transientElement;
//  private final EXIEventTransientAttribute m_transientAttribute;
  
  SimpleScanner() {
//    m_transientCharacters = new EXIEventTransientCharacters();
//    m_transientElement = new EXIEventTransientElement();
//    m_transientAttribute = new EXIEventTransientAttribute();
  }
  
  @Override
  public final void setBlockSize(int blockSize){
    // Do nothing.
  }

//  public final void setBinaryChunkSize(int chunkSize) {
//    m_binaryChunkSize = chunkSize;
//  }

  @Override
  public final EventDescription nextEvent() throws IOException {
    EventCodeTuple eventCodeTuple;
    if ((eventCodeTuple = getNextEventCodes()) == null)
      return null;
    
    EventType eventType;
    do {
      final EventCode eventCodeItem;
      final int width;
      if ((width = eventCodeTuple.width) != 0) {
      	final int n = readNBitUnsigned(width, m_inputStream);
      	eventCodeItem = eventCodeTuple.reversed ? eventCodeTuple.eventCodes[eventCodeTuple.itemsCount - (n + 1)] : eventCodeTuple.eventCodes[n]; 
      }
      else {
        eventCodeItem = eventCodeTuple.headItem;
      }
      if (eventCodeItem.itemType != EventType.ITEM_TUPLE) {
        eventType = (EventType)eventCodeItem;
        break;
      }
      eventCodeTuple = (EventCodeTuple)eventCodeItem;
    }
    while (true);

    switch (eventType.itemType) {
      case EventType.ITEM_START_DOCUMENT:
        startDocument();
        return eventType.asEventDescription();
      case EventType.ITEM_END_DOCUMENT:
        endDocument();
        return eventType.asEventDescription();
      case EventType.ITEM_START_OBJECT_ANONYMOUS:
        startObjectAnonymous(eventType);
        return eventType.asEventDescription();
      case EventType.ITEM_START_OBJECT_WILDCARD:
        return doStartObjectWildcard(eventType);
      case EventType.ITEM_START_OBJECT_NAMED:
        startObjectNamed(eventType);
        return eventType.asEventDescription();
      case EventType.ITEM_STRING_VALUE_ANONYMOUS:
        return doStringValueAnonymous(eventType);
      case EventType.ITEM_STRING_VALUE_WILDCARD:
        return doStringValueWildcard(eventType);
      case EventType.ITEM_STRING_VALUE_NAMED:
        return doStringValue(eventType);
      case EventType.ITEM_NUMBER_VALUE_ANONYMOUS:
        return doNumberValueAnonymous(eventType);
      case EventType.ITEM_NUMBER_VALUE_WILDCARD:
        return doNumberValueWildcard(eventType);
      case EventType.ITEM_NUMBER_VALUE_NAMED:
        return doNumberValue(eventType);
      case EventType.ITEM_BOOLEAN_VALUE_ANONYMOUS:
        return doBooleanValueAnonymous(eventType);
      case EventType.ITEM_BOOLEAN_VALUE_WILDCARD:
        return doBooleanValueWildcard(eventType);
      case EventType.ITEM_BOOLEAN_VALUE_NAMED:
        return doBooleanValue(eventType);
      case EventType.ITEM_END_OBJECT:
        endObject();
        return eventType.asEventDescription();
      case EventType.ITEM_START_ARRAY_ANONYMOUS:
        startArrayAnonymous();
        return eventType.asEventDescription();
      case EventType.ITEM_START_ARRAY_WILDCARD:        
        return doStartArrayWildcard(eventType);
      case EventType.ITEM_START_ARRAY_NAMED:
        startArrayNamed(eventType);
        return eventType.asEventDescription();
      case EventType.ITEM_END_ARRAY:
        endArray();
        return eventType.asEventDescription();
      case EventType.ITEM_NULL_ANONYMOUS:
        anonymousNullValue(eventType);
        return eventType.asEventDescription();
      case EventType.ITEM_NULL_WILDCARD:
        return doNullValueWildcard(eventType);
      case EventType.ITEM_NULL_NAMED:
        return eventType.asEventDescription();
      default:
        assert false;
        break;
    }
    return null;
  }
  
  private EventDescription doStringValueAnonymous(EventType eventType) throws IOException {
    anonymousStringValue(eventType);
    final int name = currentState.name;
    final Characters text = m_stringValueScannerInherent.scan(name);
    return new EXIEventAnonymousStringValue(text, eventType);
  }

  private EventDescription doStringValueWildcard(EventType eventType) throws IOException {
    final int nameId = readName(stringTable);
    wildcardStringValue(eventType.getIndex(), nameId);
    final String nameString = stringTable.localNameEntries[nameId].localName;
    final Characters text = m_stringValueScannerInherent.scan(nameId);
    return new EXIEventWildcardValue(EventDescription.EVENT_STRING_VALUE, nameString, nameId, text, eventType);
  }

  private EventDescription doStringValue(EventType eventType) throws IOException {
    final Characters text;    
    final int nameId = eventType.getNameId();
    text = m_stringValueScannerInherent.scan(nameId);
    return new EXIEventStringValue(text, eventType);
  }

  private EventDescription doNumberValueAnonymous(EventType eventType) throws IOException {
    anonymousNumberValue(eventType);
    final int name = currentState.name;
    final Characters text = m_numberValueScannerInherent.scan(name);
    return new EXIEventNumberValue(text, eventType);
  }
  
  private EventDescription doNumberValueWildcard(EventType eventType) throws IOException {
    final int nameId = readName(stringTable);
    wildcardNumberValue(eventType.getIndex(), nameId);
    final String nameString = stringTable.localNameEntries[nameId].localName;
    final Characters text = m_numberValueScannerInherent.scan(nameId);
    return new EXIEventWildcardValue(EventDescription.EVENT_NUMBER_VALUE, nameString, nameId, text, eventType);
  }

  private EventDescription doNumberValue(EventType eventType) throws IOException {
    final Characters text;    
    final int nameId = eventType.getNameId();
    text = m_numberValueScannerInherent.scan(nameId);
    return new EXIEventNumberValue(text, eventType);
  }

  private EventDescription doBooleanValueAnonymous(EventType eventType) throws IOException {
    anonymousBooleanValue(eventType);
    final int name = currentState.name;
    final Characters text = m_booleanValueScannerInherent.scan(name);
    return new EXIEventBooleanValue(text, eventType);
  }

  private EventDescription doBooleanValueWildcard(EventType eventType) throws IOException {
    final int nameId = readName(stringTable);
    wildcardBooleanValue(eventType.getIndex(), nameId);
    final String nameString = stringTable.localNameEntries[nameId].localName;
    final Characters text = m_booleanValueScannerInherent.scan(nameId);
    return new EXIEventWildcardValue(EventDescription.EVENT_BOOLEAN_VALUE, nameString, nameId, text, eventType);
  }

  private EventDescription doBooleanValue(EventType eventType) throws IOException {
    final Characters text;    
    final int nameId = eventType.getNameId();
    text = m_booleanValueScannerInherent.scan(nameId);
    return new EXIEventBooleanValue(text, eventType);
  }

  private EventDescription doNullValueWildcard(EventType eventType) throws IOException {
    final int nameId = readName(stringTable);
    wildcardNullValue(eventType.getIndex(), nameId);
    final String nameString = stringTable.localNameEntries[nameId].localName;
    return new EXIEventWildcardValue(EventDescription.EVENT_NULL, nameString, nameId, Characters.CHARACTERS_NULL, eventType);
  }

  private EventDescription doStartObjectWildcard(EventType eventType) throws IOException {
    final int nameId = readName(stringTable);
    startObjectWildcard(nameId);
    final String nameString = stringTable.localNameEntries[nameId].localName;
    return new EXIEventWildcardStartContainer(nameString, nameId, eventType, EventDescription.EVENT_START_OBJECT);
  }

  private EventDescription doStartArrayWildcard(EventType eventType) throws IOException {
    final int nameId = readName(stringTable);
    startArrayWildcard(nameId);
    final String nameString = stringTable.localNameEntries[nameId].localName;
    return new EXIEventWildcardStartContainer(nameString, nameId, eventType, EventDescription.EVENT_START_ARRAY);
  }

//  private static final class EXIEventWildcardStringValue implements EventDescription {
//
//    private final EventType m_eventType;
//
//    private final String m_name;
//    private final int m_nameId;
//
//    private final Characters m_text;
//    
//    public EXIEventWildcardStringValue(String name, int nameId, Characters text, EventType eventType) {
//      m_eventType = eventType;
//      m_name = name;
//      m_nameId = nameId;
//      m_text = text;
//    }
//    
//    public byte getEventKind() {
//      return EventDescription.EVENT_STRING_VALUE;
//    }
//
//    public int getNameId() {
//      return m_nameId;
//    }
//
//    public Characters getCharacters() {
//      return m_text;
//    }
//    
//    public EventType getEventType() {
//      return m_eventType;
//    }
//    
//    public String getName() {
//      return m_name;
//    }
//  }
  
  private static final class EXIEventAnonymousStringValue implements EventDescription {

    private final EventType m_eventType;
    private final Characters m_text;
    
    public EXIEventAnonymousStringValue(Characters text, EventType eventType) {
      m_eventType = eventType;
      m_text = text;
    }
    
    public byte getEventKind() {
      return EventDescription.EVENT_STRING_VALUE;
    }

    public int getNameId() {
      return -1;
    }

    public Characters getCharacters() {
      return m_text;
    }
    
    public EventType getEventType() {
      return m_eventType;
    }
    
    public String getName() {
      return null;
    }
  }

  private static final class EXIEventWildcardValue implements EventDescription {

    private final EventType m_eventType;

    private final String m_name;
    private final int m_nameId;

    private final Characters m_text;
    
    private final byte m_eventKind;
    
    public EXIEventWildcardValue(byte eventKind, String name, int nameId, Characters text, EventType eventType) {
      m_eventKind = eventKind;
      m_eventType = eventType;
      m_name = name;
      m_nameId = nameId;
      m_text = text;
    }
    
    public byte getEventKind() {
      return m_eventKind;
    }

    public int getNameId() {
      return m_nameId;
    }

    public Characters getCharacters() {
      return m_text;
    }
    
    public EventType getEventType() {
      return m_eventType;
    }
    
    public String getName() {
      return m_name;
    }
  }
  
  
//  private static final class EXIEventStringValue implements EventDescription {

//    private final EventType m_eventType;
//    private final Characters m_text;
//    
//    public EXIEventStringValue(Characters text, EventType eventType) {
//      m_eventType = eventType;
//      m_text = text;
//    }
//    
//    public byte getEventKind() {
//      return EventDescription.EVENT_STRING_VALUE;
//    }
//
//    public int getNameId() {
//      return m_eventType.getNameId();
//    }
//
//    public Characters getCharacters() {
//      return m_text;
//    }
//    
//    public EventType getEventType() {
//      return m_eventType;
//    }
//    
//    public String getName() {
//      return m_eventType.getName();
//    }
//  }

//  private static final class EXIEventWildcardStartContainer implements EventDescription {
//
//    private final EventType m_eventType;
//
//    private final String m_name;
//    private final int m_nameId;
//    
//    private final byte m_eventKind;
//
//    public EXIEventWildcardStartContainer(String name, int nameId, EventType eventType, byte eventKind) {
//      m_eventType = eventType;
//      m_name = name;
//      m_nameId = nameId;
//      m_eventKind = eventKind;
//    }
//    
//    public byte getEventKind() {
//      return m_eventKind; 
//    }
//
//    public int getNameId() {
//      return m_nameId;
//    }
//
//    public Characters getCharacters() {
//      return null;
//    }
//    
//    public EventType getEventType() {
//      return m_eventType;
//    }
//    
//    public String getName() {
//      return m_name;
//    }
//  }


  ///////////////////////////////////////////////////////////////////////////
  /// Event handlers
  ///////////////////////////////////////////////////////////////////////////

//  private EventDescription doElement(EventType eventType) throws IOException {
//  	m_characterBuffer.nextIndex = 0;
//    final int localNameId;
//    localNameId = eventType.getNameId();
//    currentState.targetGrammar.element(eventType, currentState);
//    m_nameLocusLastDepth += 2;
//    m_nameLocusStack[m_nameLocusLastDepth] = localNameId;
//    //m_nameLocusStack[m_nameLocusLastDepth + 1] = uriId;
//    return eventType.asEventDescription();
//  }

//  private EventDescription doAttribute(EventType eventType) throws IOException {
//    final int uriId, localNameId;
//    final String prefix;
//    uriId = eventType.getURIId();
//    localNameId = eventType.getNameId();
//    prefix = m_preserveNS ? readPrefixOfQName(uriId) : null;
//    attribute(eventType);
//    if (eventType.itemType == EventType.ITEM_AT && uriId == XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI_ID && 
//        localNameId == EXISchemaConst.XSI_LOCALNAME_TYPE_ID) {
//      return readXsiTypeValue(prefix, eventType);
//    }
//    else {
//      m_transientAttribute.prefix = prefix;
//      m_transientAttribute.eventType = eventType;
//      if (eventType.itemType == EventType.ITEM_SCHEMA_AT) {
//        final EventTypeSchema eventTypeSchemaAttribute = (EventTypeSchema)eventType;
//        final int tp;
//        if ((tp = eventTypeSchemaAttribute.nd) != EXISchema.NIL_NODE) {
//          final int simpleTypeSerial = m_types[tp + EXISchemaLayout.TYPE_NUMBER];
//          final ValueScanner valueScanner = m_valueScannerTable[m_codecTable[simpleTypeSerial]];
//          m_transientAttribute.text = valueScanner.scan(localNameId, uriId, tp);
//          return m_transientAttribute;
//        }
//      }
//      m_transientAttribute.text = m_valueScannerTable[CODEC_STRING].scan(localNameId, uriId, EXISchema.NIL_NODE);
//      return m_transientAttribute;
//    }
//  }
//
//  private EventDescription doAttributeInvalid(EventType eventType) throws IOException {
//    final int uriId, localNameId;
//    final String prefix;
//    final Characters text;    
//    uriId = eventType.getURIId();
//    localNameId = eventType.getNameId();
//    prefix = m_preserveNS ? readPrefixOfQName(uriId) : null;
//    text = m_valueScannerTable[CODEC_STRING].scan(localNameId, uriId, EXISchema.NIL_NODE);
//    attribute(eventType);
//    m_transientAttribute.prefix = prefix;
//    m_transientAttribute.text = text;
//    m_transientAttribute.eventType = eventType;
//    return m_transientAttribute;
//  }
//
//  private EventDescription doAttributeWildcardAny(EventType eventType) throws IOException {
//    final String prefix;
//    final Characters text;    
//    readQName(qname);
//    prefix = qname.prefix;
//    if (eventType.itemType == EventType.ITEM_AT_WC_ANY_UNTYPED)
//      wildcardAttribute(eventType.getIndex(), qname.uriId, qname.localNameId);
//    if (qname.uriId == XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI_ID && qname.localNameId == EXISchemaConst.XSI_LOCALNAME_TYPE_ID) {
//      assert eventType.itemType == EventType.ITEM_AT_WC_ANY_UNTYPED;
//      return readXsiTypeValue(prefix, eventType);
//    }
//    else {
//      final String uriString = stringTable.getURI(qname.uriId);
//      final String nameString = stringTable.getLocalNamePartition(qname.uriId).localNameEntries[qname.localNameId].localName;
//      int tp = EXISchema.NIL_NODE;
//      if (eventType.itemType != EventType.ITEM_AT_WC_ANY_UNTYPED) {
//        int attr;
//        if ((attr = schema.getGlobalAttrOfSchema(uriString, nameString)) != EXISchema.NIL_NODE) {
//          tp = schema.getTypeOfAttr(attr);
//        }
//      }
//      text = getValueScanner(tp).scan(qname.localNameId, qname.uriId, tp);
//      return new EXIEventWildcardAttribute(uriString, nameString, qname.uriId, qname.localNameId, prefix, text, eventType);
//    }
//  }

//  private EventDescription doElementWildcardAny(EventType eventType) throws IOException {
//    m_characterBuffer.nextIndex = 0;
//    readQName(qname);
//    final String uriString = stringTable.getURI(qname.uriId);
//    final String nameString = stringTable.getLocalNamePartition(qname.uriId).localNameEntries[qname.localNameId].localName;
//    m_nameLocusLastDepth += 2;
//    m_nameLocusStack[m_nameLocusLastDepth] = qname.localNameId;
//    m_nameLocusStack[m_nameLocusLastDepth + 1] = qname.uriId;
//    if (m_preserveNS) {
//      m_prefixUriBindingsLocusStack[++m_prefixUriBindingsLocusLastDepth] = m_prefixUriBindings;
//    }
//    startWildcardElement(eventType.getIndex(), qname.uriId, qname.localNameId);
//    return new EXIEventWildcardStartElement(uriString, nameString, qname.uriId, qname.localNameId, qname.prefix, eventType);
//  }
  
  ///////////////////////////////////////////////////////////////////////////
  /// Routines for reading QName
  ///////////////////////////////////////////////////////////////////////////

//  private void readQName(QName qName) throws IOException {
//    final int uri = readURI();
//    final StringTable.LocalNamePartition partition;
//    partition = stringTable.getLocalNamePartition(uri);
//    qName.uriId = uri;
//    qName.localNameId = readLocalName(partition);
//    qName.prefix = m_preserveNS ? readPrefixOfQName(uri) : null;
//  }
//
//  private void readQName(QName qName, int uri) throws IOException {
//    final int localNameId;
//    final StringTable.LocalNamePartition partition;
//    partition = stringTable.getLocalNamePartition(uri);
//    localNameId = readLocalName(partition);
//    qName.uriId = uri;
//    qName.localNameId = localNameId;
//    qName.prefix = m_preserveNS ? readPrefixOfQName(uri) : null;
//  }
  
  ///////////////////////////////////////////////////////////////////////////
  /// Transient events
  ///////////////////////////////////////////////////////////////////////////

//  private static final class EXIEventTransientCharacters implements EventDescription {
//
//  	Characters characters;
//  	EventType eventType;
//    
//    EXIEventTransientCharacters() {
//      characters = null;
//      eventType = null;
//    }
//
//    public byte getEventKind() {
//      return EventDescription.EVENT_CH;
//    }
//    
//    public String getURI() {
//      return null;
//    }
//    
//    public String getName() {
//      return "#text";
//    }
//
//    public int getURIId() {
//      return -1;
//    }
//    
//    public int getNameId() {
//      return -1;
//    }
//
//    public String getPrefix() {
//      return null;
//    }
//
//    public Characters getCharacters() {
//      return characters;
//    }
//    
//    public BinaryDataSource getBinaryDataSource() {
//      return null;
//    }
//    
//    public EventType getEventType() {
//      return eventType;
//    }
//  }
  
//  private static final class EXIEventTransientElement implements EventDescription {
//    
//    String prefix;
//    EventType eventType;
//    
//    public final byte getEventKind() {
//      return EventDescription.EVENT_SE;
//    }
//
//    public String getName() {
//      return eventType.name;
//    }
//
//    public String getURI() {
//      return eventType.uri;
//    }
//
//    public int getNameId() {
//      return eventType.getNameId();
//    }
//
//    public int getURIId() {
//      return eventType.getURIId();
//    }
//
//    public String getPrefix() {
//      return prefix;
//    }
//    
//    public final Characters getCharacters() {
//      return null;
//    }
//    
//    public BinaryDataSource getBinaryDataSource() {
//      return null;
//    }
//    
//    public final EventType getEventType() {
//      return eventType;
//    }
//  }
//  
//  private static final class EXIEventTransientAttribute implements EventDescription {
//    
//    String prefix;
//    EventType eventType;
//    Characters text;
//    
//    public final byte getEventKind() {
//      return EventDescription.EVENT_AT;
//    }
//
//    public final String getURI() {
//      return eventType.uri;
//    }
//    
//    public final String getName() {
//      return eventType.name;
//    }
//
//    public final int getURIId() {
//      return eventType.getURIId();
//    }
//    
//    public final int getNameId() {
//      return eventType.getNameId();
//    }
//
//    public final String getPrefix() {
//      return prefix;
//    }
//    
//    public Characters getCharacters() {
//      return text;
//    }
//
//    public BinaryDataSource getBinaryDataSource() {
//      return null;
//    }
//
//    public final EventType getEventType() {
//      return eventType;
//    }
//  }
  
//  private static final class EXIEventUndeclaredCharacter implements EventDescription {
//    
//    private final EventType m_eventType;
//
//    private Characters m_text; 
//
//    public EXIEventUndeclaredCharacter(Characters text, EventType eventType) {
//      assert eventType.itemType == EventType.ITEM_CH;
//      m_eventType = eventType;
//      m_text = text;
//    }
//
//    ///////////////////////////////////////////////////////////////////////////
//    // Implementation of EXIEvent interface
//    ///////////////////////////////////////////////////////////////////////////
//
//    public byte getEventKind() {
//      return EventDescription.EVENT_CH;
//    }
//    
//    public String getURI() {
//      return null;
//    }
//    
//    public String getName() {
//      return "#text";
//    }
//
//    public int getNameId() {
//      return -1;
//    }
//
//    public int getURIId() {
//      return -1;
//    }
//
//    public String getPrefix() {
//      return null;
//    }
//
//    public Characters getCharacters() {
//      return m_text;
//    }
//
//    public BinaryDataSource getBinaryDataSource() {
//      return null;
//    }
//    
//    public EventType getEventType() {
//      return m_eventType;
//    }
//  }

//  private static final class EXIEventWildcardAttribute implements EventDescription {
//
//    private final EventType m_eventType;
//
//    private final String m_uri;
//    private final String m_name;
//    private final int m_uriId;
//    private final int m_nameId;
//    private final String m_prefix;
//
//    private final Characters m_text;
//    
//    public EXIEventWildcardAttribute(String uri, String name, int uriId, int nameId, String prefix, Characters text, EventType eventType) {
//      m_prefix = prefix;
//      m_eventType = eventType;
//      m_uri = uri; 
//      m_name = name;
//      m_uriId= uriId;
//      m_nameId = nameId;
//      m_text = text;
//    }
//    
//    public byte getEventKind() {
//      return EventDescription.EVENT_AT;
//    }
//
//    public int getURIId() {
//      return m_uriId;
//    }
//    
//    public int getNameId() {
//      return m_nameId;
//    }
//
//    public String getPrefix() {
//      return m_prefix;
//    }
//    
//    public Characters getCharacters() {
//      return m_text;
//    }
//    
//    public BinaryDataSource getBinaryDataSource() {
//      return null;
//    }
//    
//    public EventType getEventType() {
//      return m_eventType;
//    }
//    
//    public String getName() {
//      return m_name;
//    }
//
//    public String getURI() {
//      return m_uri;
//    }
//    
//  }
  
  
}

