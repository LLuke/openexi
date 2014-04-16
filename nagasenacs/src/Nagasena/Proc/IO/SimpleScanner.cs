using System.Diagnostics;

using BinaryDataSource = Nagasena.Proc.Common.BinaryDataSource;
using EventDescription = Nagasena.Proc.Common.EventDescription;
using EventDescription_Fields = Nagasena.Proc.Common.EventDescription_Fields;
using EventCode = Nagasena.Proc.Common.EventCode;
using EventType = Nagasena.Proc.Common.EventType;
using QName = Nagasena.Proc.Common.QName;
using StringTable = Nagasena.Proc.Common.StringTable;
using XmlUriConst = Nagasena.Proc.Common.XmlUriConst;
using EXIEventDTD = Nagasena.Proc.Events.EXIEventDTD;
using EXIEventComment = Nagasena.Proc.Events.EXIEventComment;
using EXIEventProcessingInstruction = Nagasena.Proc.Events.EXIEventProcessingInstruction;
using EXIEventWildcardStartElement = Nagasena.Proc.Events.EXIEventWildcardStartElement;
using EXIEventSchemaNil = Nagasena.Proc.Events.EXIEventSchemaNil;
using EXIEventEntityReference = Nagasena.Proc.Events.EXIEventEntityReference;
using Apparatus = Nagasena.Proc.Grammars.Apparatus;
using EventCodeTuple = Nagasena.Proc.Grammars.EventCodeTuple;
using EventTypeSchema = Nagasena.Proc.Grammars.EventTypeSchema;
using Characters = Nagasena.Schema.Characters;
using EXISchema = Nagasena.Schema.EXISchema;
using EXISchemaConst = Nagasena.Schema.EXISchemaConst;
using EXISchemaLayout = Nagasena.Schema.EXISchemaLayout;

namespace Nagasena.Proc.IO {

  internal abstract class SimpleScanner : Scanner {

    private readonly EXIEventTransientCharacters m_transientCharacters;
    private readonly EXIEventTransientElement m_transientElement;
    private readonly EXIEventTransientAttribute m_transientAttribute;
    private readonly EXIEventTransientBinaryData m_transientBinaryData;

    internal SimpleScanner(bool isForEXIOptions) : base(isForEXIOptions) {
      m_transientCharacters = new EXIEventTransientCharacters();
      m_transientElement = new EXIEventTransientElement();
      m_transientAttribute = new EXIEventTransientAttribute();
      m_transientBinaryData = new EXIEventTransientBinaryData();
    }

    public override sealed int BlockSize {
      set {
        // Do nothing.
      }
    }

    public sealed override int BinaryChunkSize {
      set {
        m_binaryChunkSize = value;
      }
    }

    public override sealed EventDescription nextEvent() {
      EventCodeTuple eventCodeTuple;
      if ((eventCodeTuple = NextEventCodes) == null) {
        return null;
      }

      EventType eventType;
      do {
        EventCode eventCodeItem;
        int width;
        if ((width = eventCodeTuple.width) != 0) {
          int n = readNBitUnsigned(width, m_inputStream);
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
          if (eventType.depth != EventCode.EVENT_CODE_DEPTH_ONE) {
            currentState.targetGrammar.end(currentState);
          }
          currentState = m_statesStack[--m_n_stackedStates - 1];
          m_nameLocusLastDepth -= 2;
          if (m_preserveNS) {
            --m_prefixUriBindingsLocusLastDepth;
          }
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
          throw new System.NotSupportedException("Event type SC is not supported yet.");
        case EventType.ITEM_PI:
          return doProcessingInstruction(eventType);
        case EventType.ITEM_CM:
          return doComment(eventType);
        case EventType.ITEM_ER:
          return doEntityReferemce(eventType);
        default:
          Debug.Assert(false);
          break;
      }
      return null;
    }

    ///////////////////////////////////////////////////////////////////////////
    /// Event handlers
    ///////////////////////////////////////////////////////////////////////////

    private EventDescription doDocumentTypeDefinition(EventType eventType) {
      string publicId, systemId;
      Characters text1, text2;
      string name = readText().makeString();
      text1 = readText();
      publicId = text1.length != 0 ? text1.makeString() : null;
      text2 = readText();
      systemId = text2.length != 0 ? text2.makeString() : null;
      return new EXIEventDTD(name, publicId, systemId, readText(), eventType);
    }

    private EventDescription doElement(EventType eventType) {
      m_characterBuffer.nextIndex = 0;
      int uriId, localNameId;
      string prefix;
      uriId = eventType.URIId;
      localNameId = eventType.NameId;
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

    private EventDescription doAttribute(EventType eventType) {
      int uriId, localNameId;
      string prefix;
      uriId = eventType.URIId;
      localNameId = eventType.NameId;
      prefix = m_preserveNS ? readPrefixOfQName(uriId) : null;
      attribute(eventType);
      if (eventType.itemType == EventType.ITEM_AT && uriId == XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI_ID && localNameId == EXISchemaConst.XSI_LOCALNAME_TYPE_ID) {
        return readXsiTypeValue(prefix, eventType);
      }
      else {
        m_transientAttribute.prefix = prefix;
        m_transientAttribute.eventType = eventType;
        if (eventType.itemType == EventType.ITEM_SCHEMA_AT) {
          EventTypeSchema eventTypeSchemaAttribute = (EventTypeSchema)eventType;
          int tp;
          if ((tp = eventTypeSchemaAttribute.nd) != EXISchema.NIL_NODE) {
            int simpleTypeSerial = m_types[tp + EXISchemaLayout.TYPE_NUMBER];
            ValueScanner valueScanner = m_valueScannerTable[m_codecTable[simpleTypeSerial]];
            m_transientAttribute.text = valueScanner.scan(localNameId, uriId, tp);
            return m_transientAttribute;
          }
        }
        m_transientAttribute.text = m_valueScannerTable[CODEC_STRING].scan(localNameId, uriId, EXISchema.NIL_NODE);
        return m_transientAttribute;
      }
    }

//JAVA TO C# CONVERTER WARNING: Method 'throws' clauses are not available in .NET:
//ORIGINAL LINE: private org.openexi.Proc.Common.EventDescription doAttributeInvalid(org.openexi.Proc.Common.EventType eventType) throws java.io.IOException
    private EventDescription doAttributeInvalid(EventType eventType) {
      int uriId, localNameId;
      string prefix;
      Characters text;
      uriId = eventType.URIId;
      localNameId = eventType.NameId;
      prefix = m_preserveNS ? readPrefixOfQName(uriId) : null;
      text = m_valueScannerTable[CODEC_STRING].scan(localNameId, uriId, EXISchema.NIL_NODE);
      attribute(eventType);
      m_transientAttribute.prefix = prefix;
      m_transientAttribute.text = text;
      m_transientAttribute.eventType = eventType;
      return m_transientAttribute;
    }

    private EventDescription doCharactersTyped(EventType eventType) {
      Characters text;
      currentState.targetGrammar.chars(eventType, currentState);
      int contentDatatype = currentState.contentDatatype;
      Debug.Assert(contentDatatype != EXISchema.NIL_NODE);
      int contentDatatypeSerial = schema.getSerialOfType(contentDatatype);
      ValueScanner valueScanner = m_valueScannerTable[m_codecTable[contentDatatypeSerial]];
      if (m_binaryDataEnabled) {
        short codecId = valueScanner.CodecID;
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

    private EventDescription doCharactersMixed(EventType eventType) {
      Characters text;
      undeclaredCharacters(eventType.Index);
      text = m_valueScannerTable[CODEC_STRING].scan(m_nameLocusStack[m_nameLocusLastDepth], m_nameLocusStack[m_nameLocusLastDepth + 1], EXISchema.NIL_NODE);
      return new EXIEventSchemaMixedCharacters(text, eventType);
    }

    private EventDescription doCharactersUntyped(EventType eventType) {
      Characters text;
      undeclaredCharacters(eventType.Index);
      text = m_valueScannerTable[CODEC_STRING].scan(m_nameLocusStack[m_nameLocusLastDepth], m_nameLocusStack[m_nameLocusLastDepth + 1], EXISchema.NIL_NODE);
      return new EXIEventUndeclaredCharacter(text, eventType);
    }

    private EventDescription doAttributeWildcardAny(EventType eventType) {
      string prefix;
      Characters text;
      readQName(qname);
      prefix = qname.prefix;
      if (eventType.itemType == EventType.ITEM_AT_WC_ANY_UNTYPED) {
        wildcardAttribute(eventType.Index, qname.uriId, qname.localNameId);
      }
      if (qname.uriId == XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI_ID && qname.localNameId == EXISchemaConst.XSI_LOCALNAME_TYPE_ID) {
        Debug.Assert(eventType.itemType == EventType.ITEM_AT_WC_ANY_UNTYPED);
        return readXsiTypeValue(prefix, eventType);
      }
      else {
        string uriString = stringTable.getURI(qname.uriId);
        string nameString = stringTable.getLocalNamePartition(qname.uriId).localNameEntries[qname.localNameId].localName;
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

    private EventDescription doAttributeWildcardNS(EventType eventType) {
      string prefix;
      Characters text;
      readQName(qname, eventType.URIId);
      prefix = qname.prefix;
      string uriString = stringTable.getURI(qname.uriId);
      string nameString = stringTable.getLocalNamePartition(qname.uriId).localNameEntries[qname.localNameId].localName;
      int tp = EXISchema.NIL_NODE;
      int attr;
      if ((attr = schema.getGlobalAttrOfSchema(uriString, nameString)) != EXISchema.NIL_NODE) {
        tp = schema.getTypeOfAttr(attr);
      }
      text = getValueScanner(tp).scan(qname.localNameId, qname.uriId, tp);
      return new EXIEventWildcardAttribute(uriString, nameString, qname.uriId, qname.localNameId, prefix, text, eventType);
    }

    private EventDescription doElementWildcardNS(EventType eventType) {
      m_characterBuffer.nextIndex = 0;
      readQName(qname, eventType.URIId);
      string uriString = stringTable.getURI(qname.uriId);
      string nameString = stringTable.getLocalNamePartition(qname.uriId).localNameEntries[qname.localNameId].localName;
      m_nameLocusLastDepth += 2;
      m_nameLocusStack[m_nameLocusLastDepth] = qname.localNameId;
      m_nameLocusStack[m_nameLocusLastDepth + 1] = qname.uriId;
      if (m_preserveNS) {
        m_prefixUriBindingsLocusStack[++m_prefixUriBindingsLocusLastDepth] = m_prefixUriBindings;
      }
      startWildcardElement(eventType.Index, qname.uriId, qname.localNameId);
      return new EXIEventWildcardStartElement(uriString, nameString, qname.uriId, qname.localNameId, qname.prefix, eventType);
    }

    private EventDescription doElementWildcardAny(EventType eventType) {
      m_characterBuffer.nextIndex = 0;
      readQName(qname);
      string uriString = stringTable.getURI(qname.uriId);
      string nameString = stringTable.getLocalNamePartition(qname.uriId).localNameEntries[qname.localNameId].localName;
      m_nameLocusLastDepth += 2;
      m_nameLocusStack[m_nameLocusLastDepth] = qname.localNameId;
      m_nameLocusStack[m_nameLocusLastDepth + 1] = qname.uriId;
      if (m_preserveNS) {
        m_prefixUriBindingsLocusStack[++m_prefixUriBindingsLocusLastDepth] = m_prefixUriBindings;
      }
      startWildcardElement(eventType.Index, qname.uriId, qname.localNameId);
      return new EXIEventWildcardStartElement(uriString, nameString, qname.uriId, qname.localNameId, qname.prefix, eventType);
    }

    private EventDescription doXsiNil(EventType eventType) {
      int uriId;
      string prefix;
      uriId = eventType.URIId;
      prefix = m_preserveNS ? readPrefixOfQName(uriId) : null;
      EXIEventSchemaNil eventSchemaNil = readXsiNilValue(prefix, eventType);
      if (eventSchemaNil.Nilled) {
        nillify(eventType.Index);
      }
      return eventSchemaNil;
    }

    private EventDescription doXsiType(EventType eventType) {
      int uriId;
      string prefix;
      uriId = eventType.URIId;
      prefix = m_preserveNS ? readPrefixOfQName(uriId) : null;
      return readXsiTypeValue(prefix, eventType);
    }

    private EventDescription doProcessingInstruction(EventType eventType) {
      Characters text;
      miscContent(eventType.Index);
      string name = readText().makeString();
      text = readText();
      return new EXIEventProcessingInstruction(name, text, eventType);
    }

    private EventDescription doComment(EventType eventType) {
      miscContent(eventType.Index);
      return new EXIEventComment(readText(), eventType);
    }

    private EventDescription doEntityReferemce(EventType eventType) {
      miscContent(eventType.Index);
      string name = readText().makeString();
      return new EXIEventEntityReference(name, eventType);
    }

    ///////////////////////////////////////////////////////////////////////////
    /// Routines for reading QName
    ///////////////////////////////////////////////////////////////////////////

    private void readQName(QName qName) {
      int uri = readURI();
      StringTable.LocalNamePartition partition;
      partition = stringTable.getLocalNamePartition(uri);
      qName.uriId = uri;
      qName.localNameId = readLocalName(partition);
      qName.prefix = m_preserveNS ? readPrefixOfQName(uri) : null;
    }

    private void readQName(QName qName, int uri) {
      int localNameId;
      StringTable.LocalNamePartition partition;
      partition = stringTable.getLocalNamePartition(uri);
      localNameId = readLocalName(partition);
      qName.uriId = uri;
      qName.localNameId = localNameId;
      qName.prefix = m_preserveNS ? readPrefixOfQName(uri) : null;
    }

    ///////////////////////////////////////////////////////////////////////////
    /// Transient events
    ///////////////////////////////////////////////////////////////////////////

    private sealed class EXIEventTransientCharacters : EventDescription {

      internal Characters characters;
      internal EventType eventType;

      internal EXIEventTransientCharacters() {
        characters = null;
        eventType = null;
      }

      public sbyte EventKind {
        get {
          return EventDescription_Fields.EVENT_CH;
        }
      }

      public string URI {
        get {
          return null;
        }
      }

      public string Name {
        get {
          return "#text";
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
          return characters;
        }
      }

      public BinaryDataSource BinaryDataSource {
        get {
          return null;
        }
      }

      public EventType getEventType() {
        return eventType;
      }
    }

    private sealed class EXIEventTransientElement : EventDescription {

      internal string prefix;
      internal EventType eventType;

      public sbyte EventKind {
        get {
          return EventDescription_Fields.EVENT_SE;
        }
      }

      public string Name {
        get {
          return eventType.name;
        }
      }

      public string URI {
        get {
          return eventType.uri;
        }
      }

      public int NameId {
        get {
          return eventType.NameId;
        }
      }

      public int URIId {
        get {
          return eventType.URIId;
        }
      }

      public string Prefix {
        get {
          return prefix;
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
        return eventType;
      }
    }

    private sealed class EXIEventTransientAttribute : EventDescription {

      internal string prefix;
      internal EventType eventType;
      internal Characters text;

      public sbyte EventKind {
        get {
          return EventDescription_Fields.EVENT_AT;
        }
      }

      public string URI {
        get {
          return eventType.uri;
        }
      }

      public string Name {
        get {
          return eventType.name;
        }
      }

      public int URIId {
        get {
          return eventType.URIId;
        }
      }

      public int NameId {
        get {
          return eventType.NameId;
        }
      }

      public string Prefix {
        get {
          return prefix;
        }
      }

      public Characters Characters {
        get {
          return text;
        }
      }

      public BinaryDataSource BinaryDataSource {
        get {
          return null;
        }
      }

      public EventType getEventType() {
        return eventType;
      }
    }

    private sealed class EXIEventUndeclaredCharacter : EventDescription {

      internal readonly EventType m_eventType;

      internal Characters m_text;

      public EXIEventUndeclaredCharacter(Characters text, EventType eventType) {
        Debug.Assert(eventType.itemType == EventType.ITEM_CH);
        m_eventType = eventType;
        m_text = text;
      }

      ///////////////////////////////////////////////////////////////////////////
      // Implementation of EXIEvent interface
      ///////////////////////////////////////////////////////////////////////////

      public sbyte EventKind {
        get {
          return EventDescription_Fields.EVENT_CH;
        }
      }

      public string URI {
        get {
          return null;
        }
      }

      public string Name {
        get {
          return "#text";
        }
      }

      public int NameId {
        get {
          return -1;
        }
      }

      public int URIId {
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

    private sealed class EXIEventSchemaMixedCharacters : EventDescription {

      internal readonly EventType m_eventType;

      internal readonly Characters m_text;

      public EXIEventSchemaMixedCharacters(Characters text, EventType eventType) {
        Debug.Assert(eventType.itemType == EventType.ITEM_SCHEMA_CH_MIXED);
        m_eventType = eventType;
        m_text = text;
      }

      ///////////////////////////////////////////////////////////////////////////
      // Implementation of EXIEvent interface
      ///////////////////////////////////////////////////////////////////////////

      public sbyte EventKind {
        get {
          return EventDescription_Fields.EVENT_CH;
        }
      }

      public string URI {
        get {
          return null;
        }
      }

      public string Name {
        get {
          return "#text";
        }
      }

      public string Prefix {
        get {
          return null;
        }
      }

      public int NameId {
        get {
          return -1;
        }
      }

      public int URIId {
        get {
          return -1;
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

    private sealed class EXIEventWildcardAttribute : EventDescription {

      internal readonly EventType m_eventType;

      internal readonly string m_uri;
      internal readonly string m_name;
      internal readonly int m_uriId;
      internal readonly int m_nameId;
      internal readonly string m_prefix;

      internal readonly Characters m_text;

      public EXIEventWildcardAttribute(string uri, string name, int uriId, int nameId, string prefix, Characters text, EventType eventType) {
        m_prefix = prefix;
        m_eventType = eventType;
        m_uri = uri;
        m_name = name;
        m_uriId = uriId;
        m_nameId = nameId;
        m_text = text;
      }

      public sbyte EventKind {
        get {
          return EventDescription_Fields.EVENT_AT;
        }
      }

      public int URIId {
        get {
          return m_uriId;
        }
      }

      public int NameId {
        get {
          return m_nameId;
        }
      }

      public string Prefix {
        get {
          return m_prefix;
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

    }

    private sealed class EXIEventTransientBinaryData : EventDescription {

      internal readonly BinaryDataSource binaryData;
      internal EventType eventType;

      internal EXIEventTransientBinaryData() {
        binaryData = new BinaryDataSource();
        eventType = null;
      }

      public sbyte EventKind {
        get {
          return EventDescription_Fields.EVENT_BLOB;
        }
      }

      public string URI {
        get {
          return null;
        }
      }

      public string Name {
        get {
          return "#text";
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
          return null;
        }
      }

      public BinaryDataSource BinaryDataSource {
        get {
          return binaryData;
        }
      }

      public EventType getEventType() {
        return eventType;
      }
    }

  }


}