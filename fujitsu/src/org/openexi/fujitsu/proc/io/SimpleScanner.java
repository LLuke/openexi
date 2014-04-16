package org.openexi.fujitsu.proc.io;

import java.io.InputStream;
import java.io.IOException;

import org.openexi.fujitsu.proc.common.CharacterSequence;
import org.openexi.fujitsu.proc.common.EXIEvent;
import org.openexi.fujitsu.proc.common.EventCode;
import org.openexi.fujitsu.proc.common.EventType;
import org.openexi.fujitsu.proc.events.EXIEventDTD;
import org.openexi.fujitsu.proc.events.EXIEventElement;
import org.openexi.fujitsu.proc.events.EXIEventAttributeByValue;
import org.openexi.fujitsu.proc.events.EXIEventUndeclaredElement;
import org.openexi.fujitsu.proc.events.EXIEventComment;
import org.openexi.fujitsu.proc.events.EXIEventSchemaMixedCharactersByValue;
import org.openexi.fujitsu.proc.events.EXIEventProcessingInstruction;
import org.openexi.fujitsu.proc.events.EXIEventWildcardStartElement;
import org.openexi.fujitsu.proc.events.EXIEventUndeclaredCharactersByValue;
import org.openexi.fujitsu.proc.events.EXIEventSchemaNil;
import org.openexi.fujitsu.proc.events.EXIEventEntityReference;
import org.openexi.fujitsu.proc.events.EXIEventWildcardAttributeByValue;
import org.openexi.fujitsu.proc.events.EXIEventSchemaCharactersByValue;
import org.openexi.fujitsu.proc.grammars.EventTypeSchema;
import org.openexi.fujitsu.proc.grammars.EventTypeSchemaAttribute;
import org.openexi.fujitsu.proc.util.URIConst;
import org.openexi.fujitsu.schema.EXISchema;

abstract class SimpleScanner extends Scanner {

  SimpleScanner(boolean isForEXIOptions) {
    super(isForEXIOptions);
  }
  
  @Override
  public final void setBlockSize(int blockSize){
    // Do nothing.
  }

  @Override
  public final EXIEvent nextEvent() throws IOException {
    final EventCode eventCodeItem2;
    if ((eventCodeItem2 = m_documentGrammarState.getNextEventCodes()) == null)
      return null;
    
    final EventType eventType = readEventType(eventCodeItem2);
    
    String name;
    String prefix, publicId, systemId;
    CharacterSequence text;    
    final XMLLocusItem locusItem;
    int tp;
    byte itemType;
    switch (itemType = eventType.itemType) {
      case EventCode.ITEM_SD:
        m_documentGrammarState.startDocument();
        return eventType.asEXIEvent();
      case EventCode.ITEM_DTD:
        name = readText().makeString();
        text = readText();
        publicId = text.length() != 0 ? text.makeString() : null;
        text = readText();
        systemId = text.length() != 0 ? text.makeString() : null;
        text = readText();
        return new EXIEventDTD(name, publicId, systemId, text, eventType);
      case EventCode.ITEM_SCHEMA_SE:
      case EventCode.ITEM_SE:
        readQName(qname, eventType);
        pushLocusItem(qname.namespaceName, qname.localName);
        m_documentGrammarState.startElement(eventType.getIndex(), qname.namespaceName, qname.localName);
        if (m_preserveNS) {
          return new EXIEventElement(qname.prefix, eventType);
        }
        return (EXIEvent)eventType;
      case EventCode.ITEM_SCHEMA_AT:
      case EventCode.ITEM_AT:
        readQName(qname, eventType);
        prefix = qname.prefix;
        if (itemType == EventCode.ITEM_SCHEMA_AT)
          m_documentGrammarState.attribute(eventType.getIndex(), qname.namespaceName, qname.localName);
        if (itemType == EventCode.ITEM_AT && "type".equals(qname.localName) && 
            URIConst.W3C_2001_XMLSCHEMA_INSTANCE_URI.equals(qname.namespaceName)) {
          return readXsiTypeValue(prefix, eventType);
        }
        else {
          tp = EXISchema.NIL_NODE;
          EventTypeSchemaAttribute eventTypeSchemaAttribute;
          if (itemType == EventCode.ITEM_SCHEMA_AT && (eventTypeSchemaAttribute = (EventTypeSchemaAttribute)eventType).useSpecificType()) {
            final int attr = eventTypeSchemaAttribute.getSchemaSubstance();
            assert EXISchema.ATTRIBUTE_NODE == m_schema.getNodeType(attr);
            tp = m_schema.getTypeOfAttr(attr); 
            text = getValueScanner(tp).scan(qname.localName, qname.namespaceName, tp, (InputStream)null);
          }
          else { 
            text = m_valueScannerTable[CODEC_STRING].scan(qname.localName, qname.namespaceName, tp, (InputStream)null);
          }
          return new EXIEventAttributeByValue(prefix, text, eventType);
        }
      case EventCode.ITEM_SCHEMA_UNDECLARED_AT_INVALID_VALUE:
        readQName(qname, eventType);
        text = m_valueScannerTable[CODEC_STRING].scan(qname.localName, qname.namespaceName, EXISchema.NIL_NODE, (InputStream)null);
        m_documentGrammarState.attribute(((EventTypeSchema)eventType).serial, qname.namespaceName, qname.localName);
        return new EXIEventAttributeByValue(text, (EventType)eventType);
      case EventCode.ITEM_SCHEMA_CH:
        m_documentGrammarState.characters();
        tp = ((EventTypeSchema)eventType).getSchemaSubstance();
        locusItem = m_locusStack[m_locusLastDepth];
        text = getValueScanner(tp).scan(locusItem.elementLocalName, locusItem.elementURI, tp, (InputStream)null); 
        return new EXIEventSchemaCharactersByValue(text, eventType);
      case EventCode.ITEM_SCHEMA_CH_MIXED:
        m_documentGrammarState.undeclaredCharacters();
        locusItem = m_locusStack[m_locusLastDepth];
        text = m_valueScannerTable[CODEC_STRING].scan(locusItem.elementLocalName, locusItem.elementURI, EXISchema.NIL_NODE, (InputStream)null);
        return new EXIEventSchemaMixedCharactersByValue(text, eventType);
      case EventCode.ITEM_CH:
        m_documentGrammarState.undeclaredCharacters();
        locusItem = m_locusStack[m_locusLastDepth];
        text = m_valueScannerTable[CODEC_STRING].scan(locusItem.elementLocalName, locusItem.elementURI, EXISchema.NIL_NODE, (InputStream)null);
        return new EXIEventUndeclaredCharactersByValue(text, eventType);
      case EventCode.ITEM_SCHEMA_EE:
      case EventCode.ITEM_EE:
        m_documentGrammarState.endElement("", "");
        --m_locusLastDepth;
        return (EXIEvent)eventType;
      case EventCode.ITEM_ED:
        m_documentGrammarState.endDocument();
        return eventType.asEXIEvent();
      case EventCode.ITEM_SCHEMA_WC_ANY:
      case EventCode.ITEM_SCHEMA_WC_NS:
        readQName(qname, eventType);
        pushLocusItem(qname.namespaceName, qname.localName);
        m_documentGrammarState.startElement(eventType.getIndex(), qname.namespaceName, qname.localName);
        return new EXIEventWildcardStartElement(qname.namespaceName, qname.localName, qname.prefix, eventType); 
      case EventCode.ITEM_SE_WC:
        readQName(qname, eventType);
        pushLocusItem(qname.namespaceName, qname.localName);
        m_documentGrammarState.startUndeclaredElement(qname.namespaceName, qname.localName);
        return new EXIEventUndeclaredElement(qname.namespaceName, qname.localName, qname.prefix, eventType); 
      case EventCode.ITEM_SCHEMA_AT_WC_ANY:
      case EventCode.ITEM_SCHEMA_AT_WC_NS:
      case EventCode.ITEM_AT_WC_ANY_UNTYPED:
        readQName(qname, eventType);
        prefix = qname.prefix;
        m_documentGrammarState.undeclaredAttribute(qname.namespaceName, qname.localName);
        if ("type".equals(qname.localName) && URIConst.W3C_2001_XMLSCHEMA_INSTANCE_URI.equals(qname.namespaceName)) {
          assert itemType == EventCode.ITEM_AT_WC_ANY_UNTYPED;
          return readXsiTypeValue(prefix, eventType);
        }
        else {
          tp = EXISchema.NIL_NODE;
          if (itemType != EventCode.ITEM_AT_WC_ANY_UNTYPED) {
            int ns;
            if ((ns = m_schema.getNamespaceOfSchema(qname.namespaceName)) != EXISchema.NIL_NODE) {
              int attr;
              if ((attr = m_schema.getAttrOfNamespace(ns, qname.localName)) != EXISchema.NIL_NODE) {
                tp = m_schema.getTypeOfAttr(attr);
              }
            }
          }
          text = getValueScanner(tp).scan(qname.localName, qname.namespaceName, tp, (InputStream)null);
          return new EXIEventWildcardAttributeByValue(qname.namespaceName, qname.localName, prefix, text, eventType);
        }
      case EventCode.ITEM_SCHEMA_NIL:
        readQName(qname, eventType);
        final EXIEventSchemaNil eventSchemaNil = readXsiNilValue(qname.prefix, eventType); 
        if (eventSchemaNil.isNilled()) {
          m_documentGrammarState.nillify();
        }
        return eventSchemaNil;
      case EventCode.ITEM_SCHEMA_TYPE:
        readQName(qname, eventType);
        prefix = qname.prefix;
        return readXsiTypeValue(prefix, eventType);
      case EventCode.ITEM_NS:
        return readNS(eventType);
      case EventCode.ITEM_SC:
        throw new UnsupportedOperationException("Event type SC is not supported yet.");
      case EventCode.ITEM_PI:
        m_documentGrammarState.miscContent();
        name = readText().makeString();
        text = readText();
        return new EXIEventProcessingInstruction(name, text, eventType);
      case EventCode.ITEM_CM:
        m_documentGrammarState.miscContent();
        return new EXIEventComment(readText(), eventType);
      case EventCode.ITEM_ER:
        m_documentGrammarState.miscContent();
        name = readText().makeString();
        return new EXIEventEntityReference(name, eventType);
      default:
        assert false;
        break;
    }
    return null;
  }

}
