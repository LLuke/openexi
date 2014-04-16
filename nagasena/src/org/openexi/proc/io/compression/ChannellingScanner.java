package org.openexi.proc.io.compression;

import java.io.IOException;
import java.io.InputStream;

import java.util.List;
import java.util.Iterator;
import java.util.zip.Inflater;

import org.openexi.proc.common.AlignmentType;
import org.openexi.proc.common.Channel;
import org.openexi.proc.common.EventDescription;
import org.openexi.proc.common.EventCode;
import org.openexi.proc.common.EventType;
import org.openexi.proc.common.EventTypeList;
import org.openexi.proc.common.XmlUriConst;
import org.openexi.proc.events.*;
import org.openexi.proc.grammars.EventCodeTuple;
import org.openexi.proc.grammars.EventTypeSchema;
import org.openexi.proc.io.ByteAlignedCommons;
import org.openexi.proc.io.Scanner;
import org.openexi.schema.Characters;
import org.openexi.schema.EXISchema;
import org.openexi.schema.EXISchemaConst;

public final class ChannellingScanner extends Scanner {

  private final ChannelKeeper m_channelKeeper;

  private EventDescription[] m_eventList; 
  private int m_n_events;

  private final boolean m_compressed;
  private final Inflater m_inflater;
  private int m_bufSize;
  
  private final boolean m_useThreadedInflater;
  
  private boolean m_foundED;
  private int m_n_blocks;

  private int m_eventIndex;
  
  private int n_elements;
  private EXIEventElement[] m_elementEvents;
  private EXIEventValueReference[] m_valueEvents;

  public ChannellingScanner(boolean compressed, boolean useThreadedInflater) {
    super(false);
    if (m_compressed = compressed)
      m_inflater = new Inflater(true);
    else 
      m_inflater = null;
    
    m_useThreadedInflater = useThreadedInflater;
    
    m_eventList = new EventDescription[8192];
    m_n_events = 0;
    
    m_channelKeeper = new ChannelKeeper(new ScannerChannelFactory());
    
    m_valueEvents = new EXIEventValueReference[8192];
    
    m_elementEvents = new EXIEventElement[4096];
  }
  
  @Override
  protected void init(int inflatorBufSize) {
    m_bufSize = m_compressed ? inflatorBufSize : -1;
  }

  @Override
  public void reset() {
    super.reset();
    m_foundED = false;
    m_n_blocks = 0;
    m_n_events = 0;
    m_eventIndex = 0;
    m_channelKeeper.reset();
  }

  @Override
  public void setInputStream(InputStream istream) {
    if (m_compressed) {
      m_inflater.reset();
      // REVISIT: Consider reusing EXIInflaterInputStream instance.
      if (m_useThreadedInflater)
        super.setInputStream(m_compressed ? new EXIInflaterInputStreamThreaded(istream, m_inflater, m_bufSize) : istream);
      else
        super.setInputStream(new EXIInflaterInputStream(istream, m_inflater, m_bufSize));
    }
    else
      super.setInputStream(istream);
  }
  
  @Override
  public final void prepare() throws IOException {
    super.prepare();
    processBlock(); // load buffer with initial block
  }

  @Override
  public final void setBlockSize(int blockSize) {
    m_channelKeeper.setBlockSize(blockSize);
    if (m_valueEvents.length < blockSize) {
      EXIEventValueReference[] valueEvents = new EXIEventValueReference[blockSize];
      System.arraycopy(m_valueEvents, 0, valueEvents, 0, m_valueEvents.length);
      m_valueEvents = valueEvents;
    }
    if (m_elementEvents.length < blockSize) {
      EXIEventElement[] elementEvents = new EXIEventElement[blockSize];
      System.arraycopy(m_elementEvents, 0, elementEvents, 0, m_elementEvents.length);
      m_elementEvents = elementEvents;
    }
  }
  
  public final int getBlockCount() {
    return m_n_blocks;
  }

  @Override
  public final void setBinaryChunkSize(int chunkSize) {
    throw new UnsupportedOperationException("Setting binary chunk size is not supported.");
  }

  @Override
  public EventDescription nextEvent() throws IOException {
    if (m_eventIndex < m_n_events) {
      return m_eventList[m_eventIndex++];
    }
    else if (!m_foundED) {
      processBlock();
      return nextEvent();
    }
    return null;
  }
  
  private void readValueChannels() throws IOException {
    m_characterBuffer.nextIndex = 0;
    if (m_binaryDataEnabled)
      octetBuffer.nextIndex = 0;
    m_channelKeeper.finish();
    ScannerChannel channel;
    final Iterator<Channel> smallChannels = m_channelKeeper.getSmallChannels().iterator();
    if (smallChannels.hasNext()) {
      if (m_compressed && m_channelKeeper.getTotalValueCount() > 100) {
        if (!m_useThreadedInflater)
          ((EXIInflaterInputStream)m_inputStream).resetInflator();
      }
      do {
        channel = (ScannerChannel)smallChannels.next();
        List<EXIEventValueReference> textProviderList = channel.values;
        final int len = textProviderList.size();
        for (int j = 0; j < len; j++) {
          textProviderList.get(j).scanText(this, m_binaryDataEnabled, m_inputStream);
        }
      } while (smallChannels.hasNext());
    }
    final Iterator<Channel> largeChannels = m_channelKeeper.getLargeChannels().iterator();
    while (largeChannels.hasNext()) {
      if (m_compressed && !m_useThreadedInflater)
        ((EXIInflaterInputStream)m_inputStream).resetInflator();
      channel = (ScannerChannel)largeChannels.next(); 
      List<EXIEventValueReference> textProviderList = channel.values;
      final int len = textProviderList.size();
      for (int j = 0; j < len; j++) {
        textProviderList.get(j).scanText(this, m_binaryDataEnabled, m_inputStream);
      }
    }
    if (m_compressed && !m_useThreadedInflater)
      ((EXIInflaterInputStream)m_inputStream).resetInflator();
  }

  private void readStructureChannel() throws IOException {
    int n_values = 0;
    n_elements = 0;
    boolean reached = false; 
    
    EventCodeTuple eventCodeTuple;
    while (!reached && (eventCodeTuple = getNextEventCodes()) != null) {
      final int localName, uri;
      final EventType eventType;
      do {
        final EventCode eventCodeItem; 
        final int width;
        if ((width = eventCodeTuple.width) != 0) {
          eventCodeItem = eventCodeTuple.getItem(readNBitUnsigned(width, m_inputStream));
        }
        else {
          eventCodeItem = eventCodeTuple.getItem(0);
        }
        if (eventCodeItem.itemType != EventType.ITEM_TUPLE) {
          eventType = (EventType)eventCodeItem;
          break;
        }
        eventCodeTuple = (EventCodeTuple)eventCodeItem;
      }
      while (true);
      
      String name;
      String prefix, publicId, systemId;
      Characters text;
      ScannerChannel channel;
      EXIEventValueReference event;
      final int uriId;
      final int localNameId;
      byte itemType;
      switch (itemType = eventType.itemType) {
        case EventType.ITEM_SD:
          currentState.targetGrammar.startDocument(currentState);
          addEvent(eventType.asEventDescription());
          break;
        case EventType.ITEM_DTD:
          name = readText().makeString();
          text = readText();
          publicId = text.length != 0 ? text.makeString() : null;
          text = readText();
          systemId = text.length != 0 ? text.makeString() : null;
          text = readText();
          text.turnPermanent();
          addEvent(new EXIEventDTD(name, publicId, systemId, text, eventType));
          break;
        case EventType.ITEM_SE:
          doElement(eventType);
          break;
        case EventType.ITEM_SCHEMA_AT:
        case EventType.ITEM_AT:
          uriId = eventType.getURIId();
          localNameId = eventType.getNameId();
          prefix = m_preserveNS ? readPrefixOfQName(uriId) : null;
          attribute(eventType);
          if (itemType == EventType.ITEM_AT && uriId == XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI_ID && 
              localNameId == EXISchemaConst.XSI_LOCALNAME_TYPE_ID) {
            addEvent(readXsiTypeValue(prefix, eventType));
          }
          else {
            final int tp = itemType == EventType.ITEM_SCHEMA_AT ?
                ((EventTypeSchema)eventType).nd : EXISchema.NIL_NODE; 
            if ((event = m_valueEvents[n_values]) == null) {
              m_valueEvents[n_values] = event = new EXIEventValueReference();
            }
            channel = (ScannerChannel)m_channelKeeper.getChannel(localNameId, uriId, stringTable);
            reached = m_channelKeeper.incrementValueCount(channel);
            channel.values.add(event);
            event.nameId = localNameId;
            event.uriId = uriId;
            event.tp = tp;
            event.eventKind = EventDescription.EVENT_AT;
            event.eventType = eventType;
            event.prefix = prefix;
            event.uri = eventType.uri;
            event.name = eventType.name;
            event.text = null;
            event.binaryData = null;
            addEvent(event);            
            ++n_values;
          }
          break;
        case EventType.ITEM_SCHEMA_AT_INVALID_VALUE:
          uriId = eventType.getURIId();
          localNameId = eventType.getNameId();
          prefix = m_preserveNS ? readPrefixOfQName(uriId) : null;
          if ((event = m_valueEvents[n_values]) == null) {
            m_valueEvents[n_values] = event = new EXIEventValueReference();
          }
          channel = (ScannerChannel)m_channelKeeper.getChannel(localNameId, uriId, stringTable);
          reached = m_channelKeeper.incrementValueCount(channel);
          channel.values.add(event);
          attribute(eventType);
          event.nameId = localNameId;
          event.uriId = uriId;
          event.tp = EXISchema.NIL_NODE;
          event.eventKind = EventDescription.EVENT_AT;
          event.eventType = eventType;
          event.prefix = prefix;
          event.uri = eventType.uri;
          event.name = eventType.name;
          event.text = null;
          event.binaryData = null;
          addEvent(event);          
          ++n_values;
          break;
        case EventType.ITEM_SCHEMA_CH:
          if ((event = m_valueEvents[n_values]) == null) {
            m_valueEvents[n_values] = event = new EXIEventValueReference();
          }
          reached = doCharactersTyped(event, eventType);
          addEvent(event);
          ++n_values;
          break;
        case EventType.ITEM_SCHEMA_CH_MIXED:
          undeclaredCharacters(eventType.getIndex());
          if ((event = m_valueEvents[n_values]) == null) {
            m_valueEvents[n_values] = event = new EXIEventValueReference();
          }
          localName = m_nameLocusStack[m_nameLocusLastDepth];
          uri = m_nameLocusStack[m_nameLocusLastDepth + 1];
          channel = (ScannerChannel)m_channelKeeper.getChannel(localName, uri, stringTable);
          reached = m_channelKeeper.incrementValueCount(channel);
          channel.values.add(event);
          event.nameId = localName;
          event.uriId = uri;
          event.tp = EXISchema.NIL_NODE;
          event.eventKind = EventDescription.EVENT_CH;
          event.eventType = eventType;
          event.prefix = null;
          event.uri = null;
          event.name = "#text";
          event.text = null;
          event.binaryData = null;
          addEvent(event);
          ++n_values;
          break;
        case EventType.ITEM_CH:
          undeclaredCharacters(eventType.getIndex());
          if ((event = m_valueEvents[n_values]) == null) {
            m_valueEvents[n_values] = event = new EXIEventValueReference();
          }
          localName = m_nameLocusStack[m_nameLocusLastDepth];
          uri = m_nameLocusStack[m_nameLocusLastDepth + 1];
          channel = (ScannerChannel)m_channelKeeper.getChannel(localName, uri, stringTable);
          reached = m_channelKeeper.incrementValueCount(channel);
          channel.values.add(event);
          event.nameId = localName;
          event.uriId = uri;
          event.tp = EXISchema.NIL_NODE;
          event.eventKind = EventDescription.EVENT_CH;
          event.eventType = eventType;
          event.prefix = null;
          event.uri = null;
          event.name = "#text";
          event.text = null;
          event.binaryData = null;
          addEvent(event);
          ++n_values;
          break;
        case EventType.ITEM_EE:
          if (eventType.depth != EventCode.EVENT_CODE_DEPTH_ONE)
            currentState.targetGrammar.end(currentState);
          currentState = m_statesStack[--m_n_stackedStates - 1];
          m_nameLocusLastDepth -= 2;
          if (m_preserveNS)
            --m_prefixUriBindingsLocusLastDepth;
          addEvent(eventType.asEventDescription());
          break;
        case EventType.ITEM_ED:
          currentState.targetGrammar.endDocument(currentState);
          addEvent(eventType.asEventDescription());
          m_foundED = true;
          break;
        case EventType.ITEM_SCHEMA_WC_NS:
          m_characterBuffer.nextIndex = 0;
          doElementWildcard(eventType.getURIId(), eventType);
          break;
        case EventType.ITEM_SCHEMA_WC_ANY:
        case EventType.ITEM_SE_WC:
          m_characterBuffer.nextIndex = 0;
          doElementWildcard(readURI(), eventType);
          break;
        case EventType.ITEM_SCHEMA_AT_WC_ANY:
        case EventType.ITEM_SCHEMA_AT_WC_NS:
        case EventType.ITEM_AT_WC_ANY_UNTYPED:
          uriId = itemType == EventType.ITEM_SCHEMA_AT_WC_NS ? eventType.getURIId() : readURI();
          localNameId = readLocalName(stringTable.getLocalNamePartition(uriId));
          prefix = m_preserveNS ? readPrefixOfQName(uriId) : null;
          if (itemType == EventType.ITEM_AT_WC_ANY_UNTYPED)
            wildcardAttribute(eventType.getIndex(), uriId, localNameId);
          if (uriId == XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI_ID && localNameId == EXISchemaConst.XSI_LOCALNAME_TYPE_ID) {
            assert itemType == EventType.ITEM_AT_WC_ANY_UNTYPED;
            addEvent(readXsiTypeValue(prefix, eventType));
          }
          else {
            final String uriString = stringTable.getURI(uriId);
            final String localNameString = stringTable.getLocalNamePartition(uriId).localNameEntries[localNameId].localName;
            int tp = EXISchema.NIL_NODE;
            if (itemType != EventType.ITEM_AT_WC_ANY_UNTYPED) {
              int attr;
              if ((attr = schema.getGlobalAttrOfSchema(uriString, localNameString)) != EXISchema.NIL_NODE) {
                tp = schema.getTypeOfAttr(attr);
              }
            }
            if ((event = m_valueEvents[n_values]) == null) {
              m_valueEvents[n_values] = event = new EXIEventValueReference();
            }
            channel = (ScannerChannel)m_channelKeeper.getChannel(localNameId, uriId, stringTable);
            reached = m_channelKeeper.incrementValueCount(channel);
            channel.values.add(event);
            event.nameId = localNameId;
            event.uriId = uriId;
            event.tp = tp;
            event.eventKind = EventDescription.EVENT_AT;
            event.eventType = eventType;
            event.prefix = prefix;
            event.uri = uriString;
            event.name = localNameString;
            event.text = null;
            event.binaryData = null;
            addEvent(event);
            ++n_values;
          }
          break;
        case EventType.ITEM_SCHEMA_NIL:
          uriId = eventType.getURIId();
          localNameId = eventType.getNameId();
          prefix = m_preserveNS ? readPrefixOfQName(uriId) : null;
          final EXIEventSchemaNil eventSchemaNil = readXsiNilValue(prefix, eventType); 
          if (eventSchemaNil.isNilled()) {
            nillify(eventType.getIndex());
          }
          addEvent(eventSchemaNil);
          break;
        case EventType.ITEM_SCHEMA_TYPE:
          uriId = eventType.getURIId();
          localNameId = eventType.getNameId();
          prefix = m_preserveNS ? readPrefixOfQName(uriId) : null;
          addEvent(readXsiTypeValue(prefix, eventType));
          break;
        case EventType.ITEM_NS:
          addEvent(readNS(eventType));  
          break;
        case EventType.ITEM_SC:
          throw new UnsupportedOperationException("Event type SC is not supported yet.");
        case EventType.ITEM_PI:
          miscContent(eventType.getIndex());
          name = readText().makeString();
          text = readText();
          text.turnPermanent();
          addEvent(new EXIEventProcessingInstruction(name, text, eventType));
          break;
        case EventType.ITEM_CM:
          miscContent(eventType.getIndex());
          text = readText();
          text.turnPermanent();
          addEvent(new EXIEventComment(text, eventType));
          break;
        case EventType.ITEM_ER:
          miscContent(eventType.getIndex());
          name = readText().makeString();
          addEvent(new EXIEventEntityReference(name, eventType));
          break;
        default:
          assert false;
          break;
      }
    }
    if (reached) {
      readMore:
      do {
        EventTypeList eventTypeList = getNextEventTypes();
        final int n_eventTypes;
        if ((n_eventTypes = eventTypeList.getLength()) != 1) {
          assert n_eventTypes > 1;
          break;
        }
        EventType eventType;
        switch ((eventType = eventTypeList.item(0)).itemType) {
          case EventType.ITEM_SE:
            final int uriId = eventType.getURIId();
            final int localNameId = eventType.getNameId();
            final String prefix = m_preserveNS ? readPrefixOfQName(uriId) : null;
            pushLocusItem(uriId, localNameId);
            startElement(eventType);
            if (m_preserveNS) {
              EXIEventElement event;
              if ((event = m_elementEvents[n_elements]) == null) {
                m_elementEvents[n_elements] = event = new EXIEventElement();
              }
              ++n_elements;
              event.prefix = prefix;
              event.eventType = eventType;
              addEvent(event);
            }
            else
              addEvent(eventType.asEventDescription());
            break;
          case EventType.ITEM_EE:
            endElement();
            m_nameLocusLastDepth -= 2;
            if (m_preserveNS)
              --m_prefixUriBindingsLocusLastDepth;
            addEvent(eventType.asEventDescription());
            break;
          case EventType.ITEM_ED:
            endDocument();
            addEvent(eventType.asEventDescription());
            m_foundED = true;
            // falling through...
          default:
            break readMore;
        }
      } while (true);
    }
  }

  private void processBlock() throws IOException {
    ++m_n_blocks;
    m_n_events = 0;
    readStructureChannel();
    readValueChannels();
    m_channelKeeper.punctuate();
    m_eventIndex = 0;
  }
  
  @Override
  public AlignmentType getAlignmentType() {
    return m_compressed ? AlignmentType.compress : AlignmentType.preCompress;
  }

  ///////////////////////////////////////////////////////////////////////////
  /// Event handlers
  ///////////////////////////////////////////////////////////////////////////

  private boolean doCharactersTyped(EXIEventValueReference event, EventType eventType) throws IOException {
    final boolean reached;
    currentState.targetGrammar.chars(eventType, currentState);
    final int localName = m_nameLocusStack[m_nameLocusLastDepth];
    final int uri = m_nameLocusStack[m_nameLocusLastDepth + 1];
    final ScannerChannel channel = (ScannerChannel)m_channelKeeper.getChannel(localName, uri, stringTable);
    reached = m_channelKeeper.incrementValueCount(channel);
    channel.values.add(event);
    event.nameId = localName;
    event.uriId = uri;
    event.tp = currentState.contentDatatype;
    event.eventKind = EventDescription.EVENT_CH;
    event.eventType = eventType;
    event.prefix = null;
    event.uri = null;
    event.name = "#text";
    event.text = null;
    event.binaryData = null;
    return reached;
  }
  
  private void doElement(EventType eventType) throws IOException {
    m_characterBuffer.nextIndex = 0;
    final int uri = eventType.getURIId();
    final int localName = eventType.getNameId();
    pushLocusItem(uri, localName);
    currentState.targetGrammar.element(eventType, currentState);
    if (m_preserveNS) {
      EXIEventElement event;
      if ((event = m_elementEvents[n_elements]) == null) {
        m_elementEvents[n_elements] = event = new EXIEventElement();
      }
      ++n_elements;
      event.prefix = readPrefixOfQName(uri); 
      event.eventType = eventType;
      addEvent(event);
    }
    else
      addEvent(eventType.asEventDescription());
  }

  private void doElementWildcard(int uri, EventType eventType) throws IOException {
    final int localName = readLocalName(stringTable.getLocalNamePartition(uri));
    final String prefix = m_preserveNS ? readPrefixOfQName(uri) : null;
    pushLocusItem(uri, localName);
    startWildcardElement(eventType.getIndex(), uri, localName);
    addEvent(new EXIEventWildcardStartElement(
        stringTable.getURI(uri), stringTable.getLocalNamePartition(uri).localNameEntries[localName].localName,
        uri, localName, prefix, eventType));
  }

  ///////////////////////////////////////////////////////////////////////////
  /// Scanning Functions for private use
  ///////////////////////////////////////////////////////////////////////////

  @Override
  protected boolean readBoolean(InputStream istream) throws IOException {
    return ByteAlignedCommons.readBoolean(istream);
  }
  
  @Override
  protected int readNBitUnsigned(int width, InputStream istream) throws IOException {
    if (width != 0) {
      return ByteAlignedCommons.readNBitUnsigned(width, istream);
    }
    return 0;
  }

  @Override
  protected int readEightBitsUnsigned(InputStream istream) throws IOException {
    return ByteAlignedCommons.readEightBitsUnsigned(istream);
  }

  ///////////////////////////////////////////////////////////////////////////
  /// Convenience Functions
  ///////////////////////////////////////////////////////////////////////////

  private void pushLocusItem(int uri, int localName) {
    m_nameLocusLastDepth += 2;
    m_nameLocusStack[m_nameLocusLastDepth] = localName;
    m_nameLocusStack[m_nameLocusLastDepth + 1] = uri;
    if (m_preserveNS)
      m_prefixUriBindingsLocusStack[++m_prefixUriBindingsLocusLastDepth] = m_prefixUriBindings;
  }

  private void addEvent(EventDescription event) {
    if (m_eventList.length == m_n_events) {
      int len = m_eventList.length;
      len += len >> 1;
      final EventDescription[] eventList = new EventDescription[len];
      System.arraycopy(m_eventList, 0, eventList, 0, m_eventList.length);
      m_eventList = eventList;
    }
    m_eventList[m_n_events++] = event;
  }

}
