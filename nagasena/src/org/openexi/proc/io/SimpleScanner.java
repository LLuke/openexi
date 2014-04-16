package org.openexi.proc.io;

import java.io.IOException;

import org.openexi.proc.common.BinaryDataSource;
import org.openexi.proc.common.EventDescription;
import org.openexi.proc.common.EventCode;
import org.openexi.proc.common.EventType;
import org.openexi.proc.common.QName;
import org.openexi.proc.common.StringTable;
import org.openexi.proc.common.XmlUriConst;
import org.openexi.proc.events.EXIEventDTD;
import org.openexi.proc.events.EXIEventComment;
import org.openexi.proc.events.EXIEventProcessingInstruction;
import org.openexi.proc.events.EXIEventWildcardStartElement;
import org.openexi.proc.events.EXIEventSchemaNil;
import org.openexi.proc.events.EXIEventEntityReference;
import org.openexi.proc.grammars.Apparatus;
import org.openexi.proc.grammars.EventCodeTuple;
import org.openexi.proc.grammars.EventTypeSchema;
import org.openexi.schema.Characters;
import org.openexi.schema.EXISchema;
import org.openexi.schema.EXISchemaConst;
import org.openexi.schema.EXISchemaLayout;

abstract class SimpleScanner extends Scanner {

  private final EXIEventTransientCharacters m_transientCharacters;
  private final EXIEventTransientElement m_transientElement;
  private final EXIEventTransientAttribute m_transientAttribute;
  private final EXIEventTransientBinaryData m_transientBinaryData;
  
  SimpleScanner(boolean isForEXIOptions) {
    super(isForEXIOptions);
    m_transientCharacters = new EXIEventTransientCharacters();
    m_transientElement = new EXIEventTransientElement();
    m_transientAttribute = new EXIEventTransientAttribute();
    m_transientBinaryData = new EXIEventTransientBinaryData();
  }
  
  @Override
  public final void setBlockSize(int blockSize){
    // Do nothing.
  }

  public final void setBinaryChunkSize(int chunkSize) {
    m_binaryChunkSize = chunkSize;
  }

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
      case EventType.ITEM_SD:
        currentState.targetGrammar.startDocument(currentState);
        return eventType.asEventDescription();
      case EventType.ITEM_DTD:
        return doDocumentTypeDefinition(eventType);
      case EventType.ITEM_SE:
        return doElement(eventType);
      case EventType.ITEM_SCHEMA_AT:
      case EventType.ITEM_AT:
        return doAttribute(eventType);
      case EventType.ITEM_SCHEMA_AT_INVALID_VALUE:
        return doAttributeInvalid(eventType);
      case EventType.ITEM_SCHEMA_CH:
        return doCharactersTyped(eventType);
      case EventType.ITEM_SCHEMA_CH_MIXED:
        return doCharactersMixed(eventType);
      case EventType.ITEM_CH:
        return doCharactersUntyped(eventType);
      case EventType.ITEM_EE:
        if (eventType.depth != EventCode.EVENT_CODE_DEPTH_ONE)
          currentState.targetGrammar.end(currentState);
        currentState = m_statesStack[--m_n_stackedStates - 1];
        m_nameLocusLastDepth -= 2;
        if (m_preserveNS)
          --m_prefixUriBindingsLocusLastDepth;
        return eventType.asEventDescription();
      case EventType.ITEM_ED:
        currentState.targetGrammar.endDocument(currentState);
        return eventType.asEventDescription();
      case EventType.ITEM_SCHEMA_WC_NS: 
        return doElementWildcardNS(eventType);
      case EventType.ITEM_SCHEMA_WC_ANY:
      case EventType.ITEM_SE_WC:
        return doElementWildcardAny(eventType);        
      case EventType.ITEM_SCHEMA_AT_WC_NS: 
        return doAttributeWildcardNS(eventType);
      case EventType.ITEM_SCHEMA_AT_WC_ANY:
      case EventType.ITEM_AT_WC_ANY_UNTYPED:
        return doAttributeWildcardAny(eventType);
      case EventType.ITEM_SCHEMA_NIL:
        return doXsiNil(eventType);
      case EventType.ITEM_SCHEMA_TYPE:
        return doXsiType(eventType);
      case EventType.ITEM_NS:
        return readNS(eventType);
      case EventType.ITEM_SC:
        throw new UnsupportedOperationException("Event type SC is not supported yet.");
      case EventType.ITEM_PI:
        return doProcessingInstruction(eventType);
      case EventType.ITEM_CM:
        return doComment(eventType);
      case EventType.ITEM_ER:
        return doEntityReferemce(eventType);
      default:
        assert false;
        break;
    }
    return null;
  }

  ///////////////////////////////////////////////////////////////////////////
  /// Event handlers
  ///////////////////////////////////////////////////////////////////////////

  private EventDescription doDocumentTypeDefinition(EventType eventType) throws IOException {
    final String publicId, systemId;
    final Characters text1, text2;    
    final String name = readText().makeString();
    text1 = readText();
    publicId = text1.length != 0 ? text1.makeString() : null;
    text2 = readText();
    systemId = text2.length != 0 ? text2.makeString() : null;
    return new EXIEventDTD(name, publicId, systemId, readText(), eventType);
  }
  
  private EventDescription doElement(EventType eventType) throws IOException {
  	m_characterBuffer.nextIndex = 0;
    final int uriId, localNameId;
    final String prefix;
    uriId = eventType.getURIId();
    localNameId = eventType.getNameId();
    prefix = m_preserveNS ? readPrefixOfQName(uriId) : null;
    currentState.targetGrammar.element(eventType, currentState);
    m_nameLocusLastDepth += 2;
    m_nameLocusStack[m_nameLocusLastDepth] = localNameId;
    m_nameLocusStack[m_nameLocusLastDepth + 1] = uriId;
    if (m_preserveNS) {
      m_prefixUriBindingsLocusStack[++m_prefixUriBindingsLocusLastDepth] = m_prefixUriBindings;
      m_transientElement.prefix = prefix;
      m_transientElement.eventType = eventType;
      return m_transientElement;
    }
    return eventType.asEventDescription();
  }

  private EventDescription doAttribute(EventType eventType) throws IOException {
    final int uriId, localNameId;
    final String prefix;
    uriId = eventType.getURIId();
    localNameId = eventType.getNameId();
    prefix = m_preserveNS ? readPrefixOfQName(uriId) : null;
    attribute(eventType);
    if (eventType.itemType == EventType.ITEM_AT && uriId == XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI_ID && 
        localNameId == EXISchemaConst.XSI_LOCALNAME_TYPE_ID) {
      return readXsiTypeValue(prefix, eventType);
    }
    else {
      m_transientAttribute.prefix = prefix;
      m_transientAttribute.eventType = eventType;
      if (eventType.itemType == EventType.ITEM_SCHEMA_AT) {
        final EventTypeSchema eventTypeSchemaAttribute = (EventTypeSchema)eventType;
        final int tp;
        if ((tp = eventTypeSchemaAttribute.nd) != EXISchema.NIL_NODE) {
          final int simpleTypeSerial = m_types[tp + EXISchemaLayout.TYPE_NUMBER];
          final ValueScanner valueScanner = m_valueScannerTable[m_codecTable[simpleTypeSerial]];
          m_transientAttribute.text = valueScanner.scan(localNameId, uriId, tp);
          return m_transientAttribute;
        }
      }
      m_transientAttribute.text = m_valueScannerTable[CODEC_STRING].scan(localNameId, uriId, EXISchema.NIL_NODE);
      return m_transientAttribute;
    }
  }

  private EventDescription doAttributeInvalid(EventType eventType) throws IOException {
    final int uriId, localNameId;
    final String prefix;
    final Characters text;    
    uriId = eventType.getURIId();
    localNameId = eventType.getNameId();
    prefix = m_preserveNS ? readPrefixOfQName(uriId) : null;
    text = m_valueScannerTable[CODEC_STRING].scan(localNameId, uriId, EXISchema.NIL_NODE);
    attribute(eventType);
    m_transientAttribute.prefix = prefix;
    m_transientAttribute.text = text;
    m_transientAttribute.eventType = eventType;
    return m_transientAttribute;
  }

  private EventDescription doCharactersTyped(EventType eventType) throws IOException {
    final Characters text;    
    currentState.targetGrammar.chars(eventType, currentState);
    final int contentDatatype = currentState.contentDatatype; 
    assert contentDatatype != EXISchema.NIL_NODE;
    final int contentDatatypeSerial = schema.getSerialOfType(contentDatatype);
    final ValueScanner valueScanner = m_valueScannerTable[m_codecTable[contentDatatypeSerial]];
    if (m_binaryDataEnabled) {
      final short codecId = valueScanner.getCodecID();
      if (codecId == Apparatus.CODEC_BASE64BINARY || codecId == Apparatus.CODEC_HEXBINARY) {
        ((BinaryValueScanner)valueScanner).scan(-1, m_transientBinaryData.binaryData);
        m_transientBinaryData.eventType = eventType;
        return m_transientBinaryData;
      }
    }
    text = valueScanner.scan(m_nameLocusStack[m_nameLocusLastDepth], m_nameLocusStack[m_nameLocusLastDepth + 1], contentDatatype);
    m_transientCharacters.characters = text;
    m_transientCharacters.eventType = eventType;
    return m_transientCharacters;
  }

  private EventDescription doCharactersMixed(EventType eventType) throws IOException {
    final Characters text;    
    undeclaredCharacters(eventType.getIndex());
    text = m_valueScannerTable[CODEC_STRING].scan(m_nameLocusStack[m_nameLocusLastDepth], m_nameLocusStack[m_nameLocusLastDepth + 1], EXISchema.NIL_NODE);
    return new EXIEventSchemaMixedCharacters(text, eventType);
  }

  private EventDescription doCharactersUntyped(EventType eventType) throws IOException {
    final Characters text;    
    undeclaredCharacters(eventType.getIndex());
    text = m_valueScannerTable[CODEC_STRING].scan(m_nameLocusStack[m_nameLocusLastDepth], m_nameLocusStack[m_nameLocusLastDepth + 1], EXISchema.NIL_NODE);
    return new EXIEventUndeclaredCharacter(text, eventType);
  }

  private EventDescription doAttributeWildcardAny(EventType eventType) throws IOException {
    final String prefix;
    final Characters text;    
    readQName(qname);
    prefix = qname.prefix;
    if (eventType.itemType == EventType.ITEM_AT_WC_ANY_UNTYPED)
      wildcardAttribute(eventType.getIndex(), qname.uriId, qname.localNameId);
    if (qname.uriId == XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI_ID && qname.localNameId == EXISchemaConst.XSI_LOCALNAME_TYPE_ID) {
      assert eventType.itemType == EventType.ITEM_AT_WC_ANY_UNTYPED;
      return readXsiTypeValue(prefix, eventType);
    }
    else {
      final String uriString = stringTable.getURI(qname.uriId);
      final String nameString = stringTable.getLocalNamePartition(qname.uriId).localNameEntries[qname.localNameId].localName;
      int tp = EXISchema.NIL_NODE;
      if (eventType.itemType != EventType.ITEM_AT_WC_ANY_UNTYPED) {
        int attr;
        if ((attr = schema.getGlobalAttrOfSchema(uriString, nameString)) != EXISchema.NIL_NODE) {
          tp = schema.getTypeOfAttr(attr);
        }
      }
      text = getValueScanner(tp).scan(qname.localNameId, qname.uriId, tp);
      return new EXIEventWildcardAttribute(uriString, nameString, qname.uriId, qname.localNameId, prefix, text, eventType);
    }
  }

  private EventDescription doAttributeWildcardNS(EventType eventType) throws IOException {
    final String prefix;
    final Characters text;    
    readQName(qname, eventType.getURIId());
    prefix = qname.prefix;
    final String uriString = stringTable.getURI(qname.uriId);
    final String nameString = stringTable.getLocalNamePartition(qname.uriId).localNameEntries[qname.localNameId].localName;
    int tp = EXISchema.NIL_NODE;
    int attr;
    if ((attr = schema.getGlobalAttrOfSchema(uriString, nameString)) != EXISchema.NIL_NODE) {
      tp = schema.getTypeOfAttr(attr);
    }
    text = getValueScanner(tp).scan(qname.localNameId, qname.uriId, tp);
    return new EXIEventWildcardAttribute(uriString, nameString, qname.uriId, qname.localNameId, prefix, text, eventType);
  }
  
  private EventDescription doElementWildcardNS(EventType eventType) throws IOException {
    m_characterBuffer.nextIndex = 0;
    readQName(qname, eventType.getURIId());
    final String uriString = stringTable.getURI(qname.uriId);
    final String nameString = stringTable.getLocalNamePartition(qname.uriId).localNameEntries[qname.localNameId].localName;
    m_nameLocusLastDepth += 2;
    m_nameLocusStack[m_nameLocusLastDepth] = qname.localNameId;
    m_nameLocusStack[m_nameLocusLastDepth + 1] = qname.uriId;
    if (m_preserveNS) {
      m_prefixUriBindingsLocusStack[++m_prefixUriBindingsLocusLastDepth] = m_prefixUriBindings;
    }
    startWildcardElement(eventType.getIndex(), qname.uriId, qname.localNameId);
    return new EXIEventWildcardStartElement(uriString, nameString, qname.uriId, qname.localNameId, qname.prefix, eventType);
  }
  
  private EventDescription doElementWildcardAny(EventType eventType) throws IOException {
    m_characterBuffer.nextIndex = 0;
    readQName(qname);
    final String uriString = stringTable.getURI(qname.uriId);
    final String nameString = stringTable.getLocalNamePartition(qname.uriId).localNameEntries[qname.localNameId].localName;
    m_nameLocusLastDepth += 2;
    m_nameLocusStack[m_nameLocusLastDepth] = qname.localNameId;
    m_nameLocusStack[m_nameLocusLastDepth + 1] = qname.uriId;
    if (m_preserveNS) {
      m_prefixUriBindingsLocusStack[++m_prefixUriBindingsLocusLastDepth] = m_prefixUriBindings;
    }
    startWildcardElement(eventType.getIndex(), qname.uriId, qname.localNameId);
    return new EXIEventWildcardStartElement(uriString, nameString, qname.uriId, qname.localNameId, qname.prefix, eventType);
  }
  
  private EventDescription doXsiNil(EventType eventType) throws IOException {
    final int uriId;
    final String prefix;
    uriId = eventType.getURIId();
    prefix = m_preserveNS ? readPrefixOfQName(uriId) : null;
    final EXIEventSchemaNil eventSchemaNil = readXsiNilValue(prefix, eventType); 
    if (eventSchemaNil.isNilled()) {
      nillify(eventType.getIndex());
    }
    return eventSchemaNil;
  }
  
  private EventDescription doXsiType(EventType eventType) throws IOException {
    final int uriId;
    final String prefix;
    uriId = eventType.getURIId();
    prefix = m_preserveNS ? readPrefixOfQName(uriId) : null;
    return readXsiTypeValue(prefix, eventType);
  }

  private EventDescription doProcessingInstruction(EventType eventType) throws IOException {
    final Characters text;    
    miscContent(eventType.getIndex());
    final String name = readText().makeString();
    text = readText();
    return new EXIEventProcessingInstruction(name, text, eventType);
  }

  private EventDescription doComment(EventType eventType) throws IOException {
    miscContent(eventType.getIndex());
    return new EXIEventComment(readText(), eventType);
  }

  private EventDescription doEntityReferemce(EventType eventType) throws IOException {
    miscContent(eventType.getIndex());
    final String name = readText().makeString();
    return new EXIEventEntityReference(name, eventType);
  }

  ///////////////////////////////////////////////////////////////////////////
  /// Routines for reading QName
  ///////////////////////////////////////////////////////////////////////////

  private void readQName(QName qName) throws IOException {
    final int uri = readURI();
    final StringTable.LocalNamePartition partition;
    partition = stringTable.getLocalNamePartition(uri);
    qName.uriId = uri;
    qName.localNameId = readLocalName(partition);
    qName.prefix = m_preserveNS ? readPrefixOfQName(uri) : null;
  }

  private void readQName(QName qName, int uri) throws IOException {
    final int localNameId;
    final StringTable.LocalNamePartition partition;
    partition = stringTable.getLocalNamePartition(uri);
    localNameId = readLocalName(partition);
    qName.uriId = uri;
    qName.localNameId = localNameId;
    qName.prefix = m_preserveNS ? readPrefixOfQName(uri) : null;
  }
  
  ///////////////////////////////////////////////////////////////////////////
  /// Transient events
  ///////////////////////////////////////////////////////////////////////////

  private static final class EXIEventTransientCharacters implements EventDescription {

  	Characters characters;
  	EventType eventType;
    
    EXIEventTransientCharacters() {
      characters = null;
      eventType = null;
    }

    public byte getEventKind() {
      return EventDescription.EVENT_CH;
    }
    
    public String getURI() {
      return null;
    }
    
    public String getName() {
      return "#text";
    }

    public int getURIId() {
      return -1;
    }
    
    public int getNameId() {
      return -1;
    }

    public String getPrefix() {
      return null;
    }

    public Characters getCharacters() {
      return characters;
    }
    
    public BinaryDataSource getBinaryDataSource() {
      return null;
    }
    
    public EventType getEventType() {
      return eventType;
    }
  }
  
  private static final class EXIEventTransientElement implements EventDescription {
    
    String prefix;
    EventType eventType;
    
    public final byte getEventKind() {
      return EventDescription.EVENT_SE;
    }

    public String getName() {
      return eventType.name;
    }

    public String getURI() {
      return eventType.uri;
    }

    public int getNameId() {
      return eventType.getNameId();
    }

    public int getURIId() {
      return eventType.getURIId();
    }

    public String getPrefix() {
      return prefix;
    }
    
    public final Characters getCharacters() {
      return null;
    }
    
    public BinaryDataSource getBinaryDataSource() {
      return null;
    }
    
    public final EventType getEventType() {
      return eventType;
    }
  }
  
  private static final class EXIEventTransientAttribute implements EventDescription {
    
    String prefix;
    EventType eventType;
    Characters text;
    
    public final byte getEventKind() {
      return EventDescription.EVENT_AT;
    }

    public final String getURI() {
      return eventType.uri;
    }
    
    public final String getName() {
      return eventType.name;
    }

    public final int getURIId() {
      return eventType.getURIId();
    }
    
    public final int getNameId() {
      return eventType.getNameId();
    }

    public final String getPrefix() {
      return prefix;
    }
    
    public Characters getCharacters() {
      return text;
    }

    public BinaryDataSource getBinaryDataSource() {
      return null;
    }

    public final EventType getEventType() {
      return eventType;
    }
  }
  
  private static final class EXIEventUndeclaredCharacter implements EventDescription {
    
    private final EventType m_eventType;

    private Characters m_text; 

    public EXIEventUndeclaredCharacter(Characters text, EventType eventType) {
      assert eventType.itemType == EventType.ITEM_CH;
      m_eventType = eventType;
      m_text = text;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Implementation of EXIEvent interface
    ///////////////////////////////////////////////////////////////////////////

    public byte getEventKind() {
      return EventDescription.EVENT_CH;
    }
    
    public String getURI() {
      return null;
    }
    
    public String getName() {
      return "#text";
    }

    public int getNameId() {
      return -1;
    }

    public int getURIId() {
      return -1;
    }

    public String getPrefix() {
      return null;
    }

    public Characters getCharacters() {
      return m_text;
    }

    public BinaryDataSource getBinaryDataSource() {
      return null;
    }
    
    public EventType getEventType() {
      return m_eventType;
    }
  }
  
  private static final class EXIEventSchemaMixedCharacters implements EventDescription {

    private final EventType m_eventType;
    
    private final Characters m_text;
    
    public EXIEventSchemaMixedCharacters(Characters text, EventType eventType) {
      assert eventType.itemType == EventType.ITEM_SCHEMA_CH_MIXED;
      m_eventType = eventType;
      m_text = text;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Implementation of EXIEvent interface
    ///////////////////////////////////////////////////////////////////////////

    public byte getEventKind() {
      return EventDescription.EVENT_CH;
    }
    
    public String getURI() {
      return null;
    }
    
    public String getName() {
      return "#text";
    }

    public String getPrefix() {
      return null;
    }

    public int getNameId() {
      return -1;
    }

    public int getURIId() {
      return -1;
    }

    public Characters getCharacters() {
      return m_text;
    }

    public BinaryDataSource getBinaryDataSource() {
      return null;
    }

    public EventType getEventType() {
      return m_eventType;
    }
  }

  private static final class EXIEventWildcardAttribute implements EventDescription {

    private final EventType m_eventType;

    private final String m_uri;
    private final String m_name;
    private final int m_uriId;
    private final int m_nameId;
    private final String m_prefix;

    private final Characters m_text;
    
    public EXIEventWildcardAttribute(String uri, String name, int uriId, int nameId, String prefix, Characters text, EventType eventType) {
      m_prefix = prefix;
      m_eventType = eventType;
      m_uri = uri; 
      m_name = name;
      m_uriId= uriId;
      m_nameId = nameId;
      m_text = text;
    }
    
    public byte getEventKind() {
      return EventDescription.EVENT_AT;
    }

    public int getURIId() {
      return m_uriId;
    }
    
    public int getNameId() {
      return m_nameId;
    }

    public String getPrefix() {
      return m_prefix;
    }
    
    public Characters getCharacters() {
      return m_text;
    }
    
    public BinaryDataSource getBinaryDataSource() {
      return null;
    }
    
    public EventType getEventType() {
      return m_eventType;
    }
    
    public String getName() {
      return m_name;
    }

    public String getURI() {
      return m_uri;
    }
    
  }
  
  private static final class EXIEventTransientBinaryData implements EventDescription {

    final BinaryDataSource binaryData;
    EventType eventType;
    
    EXIEventTransientBinaryData() {
      binaryData = new BinaryDataSource();
      eventType = null;
    }

    public byte getEventKind() {
      return EventDescription.EVENT_BLOB;
    }
    
    public String getURI() {
      return null;
    }
    
    public String getName() {
      return "#text";
    }

    public int getURIId() {
      return -1;
    }
    
    public int getNameId() {
      return -1;
    }

    public String getPrefix() {
      return null;
    }

    public Characters getCharacters() {
      return null;
    }
    
    public BinaryDataSource getBinaryDataSource() {
      return binaryData;
    }
    
    public EventType getEventType() {
      return eventType;
    }
  }
  
}

