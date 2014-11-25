package com.sumerogi.proc.io.compression;

import java.io.IOException;
import java.io.InputStream;

import java.util.List;
import java.util.Iterator;
import java.util.zip.Inflater;

import com.sumerogi.proc.common.AlignmentType;
import com.sumerogi.proc.common.Channel;
import com.sumerogi.proc.common.EventDescription;
import com.sumerogi.proc.common.EventCode;
import com.sumerogi.proc.common.EventType;
import com.sumerogi.proc.common.EventTypeList;
import com.sumerogi.proc.common.StringTable;
import com.sumerogi.proc.events.*;
import com.sumerogi.proc.grammars.EventCodeTuple;
import com.sumerogi.proc.io.ByteAlignedCommons;
import com.sumerogi.proc.io.Scanner;

public final class ChannellingScanner extends Scanner {

  private final ChannelKeeper m_channelKeeper;

  private EventDescription[] m_eventList; 
  private int m_n_events;

  private final boolean m_compressed;
  private final Inflater m_inflater;
  private int m_bufSize;
  
  private boolean m_foundED;
  private int m_n_blocks;

  private int m_eventIndex;
  
  private EXIEventValueReference[] m_valueEvents;

  public ChannellingScanner(boolean compressed) {
    if (m_compressed = compressed)
      m_inflater = new Inflater(true);
    else 
      m_inflater = null;
    
    m_eventList = new EventDescription[8192];
    m_n_events = 0;
    
    m_channelKeeper = new ChannelKeeper(new ScannerChannelFactory());
    
    m_valueEvents = new EXIEventValueReference[8192];
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
  }
  
  public final int getBlockCount() {
    return m_n_blocks;
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
    m_channelKeeper.finish();
    ScannerChannel channel;
    final Iterator<Channel> smallChannels = m_channelKeeper.getSmallChannels().iterator();
    if (smallChannels.hasNext()) {
      if (m_compressed && m_channelKeeper.getTotalValueCount() > 100) {
        ((EXIInflaterInputStream)m_inputStream).resetInflator();
      }
      do {
        channel = (ScannerChannel)smallChannels.next();
        List<EXIEventValueReference> textProviderList = channel.values;
        final int len = textProviderList.size();
        for (int j = 0; j < len; j++) {
          textProviderList.get(j).scanText(this, m_inputStream);
        }
      } while (smallChannels.hasNext());
    }
    final Iterator<Channel> largeChannels = m_channelKeeper.getLargeChannels().iterator();
    while (largeChannels.hasNext()) {
      if (m_compressed)
        ((EXIInflaterInputStream)m_inputStream).resetInflator();
      channel = (ScannerChannel)largeChannels.next(); 
      List<EXIEventValueReference> textProviderList = channel.values;
      final int len = textProviderList.size();
      for (int j = 0; j < len; j++) {
        textProviderList.get(j).scanText(this, m_inputStream);
      }
    }
    if (m_compressed)
      ((EXIInflaterInputStream)m_inputStream).resetInflator();
  }

  private void readStructureChannel() throws IOException {
    int n_values = 0;
    boolean reached = false; 
    
    EventCodeTuple eventCodeTuple;
    while (!reached && (eventCodeTuple = getNextEventCodes()) != null) {
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
      
      switch (eventType.itemType) {
        case EventType.ITEM_START_DOCUMENT:
          startDocument();
          addEvent(eventType.asEventDescription());
          break;
        case EventType.ITEM_END_DOCUMENT:
          endDocument();
          addEvent(eventType.asEventDescription());
          m_foundED = true;
          break;
        case EventType.ITEM_START_OBJECT_ANONYMOUS:
          startObjectAnonymous(eventType);
          addEvent(eventType.asEventDescription());
          break;
        case EventType.ITEM_START_OBJECT_WILDCARD:
          doStartObjectWildcard(eventType);
          break;
        case EventType.ITEM_START_OBJECT_NAMED:
          startObjectNamed(eventType);
          addEvent(eventType.asEventDescription());
          break;
        case EventType.ITEM_STRING_VALUE_ANONYMOUS:
          reached = doStringValueAnonymous(eventType, n_values++);
          break;
        case EventType.ITEM_STRING_VALUE_WILDCARD:
          reached = doStringValueWildcard(eventType, n_values++);
          break;
        case EventType.ITEM_STRING_VALUE_NAMED:
          reached = doStringValueNamed(eventType, n_values++);
          break;
        case EventType.ITEM_NUMBER_VALUE_ANONYMOUS:
          reached = doNumberValueAnonymous(eventType, n_values++);
          break;
        case EventType.ITEM_NUMBER_VALUE_WILDCARD:
          reached = doNumberValueWildcard(eventType, n_values++);
          break;
        case EventType.ITEM_NUMBER_VALUE_NAMED:
          reached = doNumberValueNamed(eventType, n_values++);
          break;
        case EventType.ITEM_BOOLEAN_VALUE_ANONYMOUS:  
          reached = doBooleanValueAnonymous(eventType, n_values++);
          break;
        case EventType.ITEM_BOOLEAN_VALUE_WILDCARD:
          reached = doBooleanValueWildcard(eventType, n_values++);
          break;
        case EventType.ITEM_BOOLEAN_VALUE_NAMED:
          reached = doBooleanValueNamed(eventType, n_values++);
          break;
        case EventType.ITEM_NULL_ANONYMOUS:
          reached = doNullValueAnonymous(eventType, n_values++);
          break;
        case EventType.ITEM_NULL_WILDCARD:
          reached = doNullValueWildcard(eventType, n_values++);
          break;
        case EventType.ITEM_NULL_NAMED:
          reached = doNullValueNamed(eventType, n_values++);
          break;
        case EventType.ITEM_END_OBJECT:
          endObject();
          addEvent(eventType.asEventDescription());
          break;
        case EventType.ITEM_START_ARRAY_ANONYMOUS:
          startArrayAnonymous();
          addEvent(eventType.asEventDescription());
          break;
        case EventType.ITEM_START_ARRAY_WILDCARD:        
          doStartArrayWildcard(eventType);
          break;
        case EventType.ITEM_START_ARRAY_NAMED:
          startArrayNamed(eventType);
          addEvent(eventType.asEventDescription());
          break;
        case EventType.ITEM_END_ARRAY:
          endArray();
          addEvent(eventType.asEventDescription());
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
          case EventType.ITEM_END_DOCUMENT:
            endDocument();
            addEvent(eventType.asEventDescription());
            m_foundED = true;
            break;
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
  
  private void doStartObjectWildcard(EventType eventType) throws IOException {
    final int name = readName(stringTable);
    startObjectWildcard(name);
    final String nameString = stringTable.localNameEntries[name].localName;
    // REVISIT: Cache and reuse EXIEventWildcardStartContainer objects.
    addEvent(new EXIEventWildcardStartContainer(nameString, name, eventType, EventDescription.EVENT_START_OBJECT));
  }

  private void doStartArrayWildcard(EventType eventType) throws IOException {
    final int name = readName(stringTable);
    startArrayWildcard(name);
    final String nameString = stringTable.localNameEntries[name].localName;
    // REVISIT: Cache and reuse EXIEventWildcardStartContainer objects.
    addEvent(new EXIEventWildcardStartContainer(nameString, name, eventType, EventDescription.EVENT_START_ARRAY));
  }

  private boolean doValueCommon(int channelName, int name, EventType eventType, int n_values, byte eventKind) {
    EXIEventValueReference event;
    if ((event = m_valueEvents[n_values]) == null) {
      m_valueEvents[n_values] = event = new EXIEventValueReference();
    }
    final ScannerChannel channel = (ScannerChannel)m_channelKeeper.getChannel(channelName, stringTable);
    final boolean reached = m_channelKeeper.incrementValueCount(channel);
    channel.values.add(event);
    event.channelName = channelName;
    event.nameId = name;
    event.eventKind = eventKind; 
    event.eventType = eventType;
    event.name = name != StringTable.NAME_NONE ? stringTable.localNameEntries[name].localName : null;
    event.text = null;
    addEvent(event);
    return reached;
  }
  
  private boolean doStringValueAnonymous(EventType eventType, int n_values) throws IOException {
    anonymousStringValue(eventType);
    return doValueCommon(currentState.name, StringTable.NAME_NONE, eventType, n_values, EventDescription.EVENT_STRING_VALUE);
  }

  private boolean doStringValueWildcard(EventType eventType, int n_values) throws IOException {
    final int name = readName(stringTable);
    wildcardStringValue(eventType.getIndex(), name);
    return doValueCommon(name, name, eventType, n_values, EventDescription.EVENT_STRING_VALUE);
  }

  private boolean doStringValueNamed(EventType eventType, int n_values) throws IOException {
    final int name = eventType.getNameId();
    return doValueCommon(name, name, eventType, n_values, EventDescription.EVENT_STRING_VALUE);
  }
  
  private boolean doNumberValueAnonymous(EventType eventType, int n_values) throws IOException {
    anonymousNumberValue(eventType);
    return doValueCommon(currentState.name, StringTable.NAME_NONE, eventType, n_values, EventDescription.EVENT_NUMBER_VALUE);
  }

  private boolean doNumberValueWildcard(EventType eventType, int n_values) throws IOException {
    final int name = readName(stringTable);
    wildcardNumberValue(eventType.getIndex(), name);
    return doValueCommon(name, name, eventType, n_values, EventDescription.EVENT_NUMBER_VALUE);
  }

  private boolean doNumberValueNamed(EventType eventType, int n_values) throws IOException {
    final int name = eventType.getNameId();
    return doValueCommon(name, name, eventType, n_values, EventDescription.EVENT_NUMBER_VALUE);
  }

  private boolean doBooleanValueAnonymous(EventType eventType, int n_values) throws IOException {
    anonymousBooleanValue(eventType);
    return doValueCommon(currentState.name, StringTable.NAME_NONE, eventType, n_values, EventDescription.EVENT_BOOLEAN_VALUE);
  }

  private boolean doBooleanValueWildcard(EventType eventType, int n_values) throws IOException {
    final int name = readName(stringTable);
    wildcardBooleanValue(eventType.getIndex(), name);
    return doValueCommon(name, name, eventType, n_values, EventDescription.EVENT_BOOLEAN_VALUE);
  }

  private boolean doBooleanValueNamed(EventType eventType, int n_values) throws IOException {
    final int name = eventType.getNameId();
    return doValueCommon(name, name, eventType, n_values, EventDescription.EVENT_BOOLEAN_VALUE);
  }

  private boolean doNullValueAnonymous(EventType eventType, int n_values) throws IOException {
    anonymousNullValue(eventType);
    return doValueCommon(currentState.name, StringTable.NAME_NONE, eventType, n_values, EventDescription.EVENT_NULL);
  }

  private boolean doNullValueWildcard(EventType eventType, int n_values) throws IOException {
    final int name = readName(stringTable);
    wildcardNullValue(eventType.getIndex(), name);
    return doValueCommon(name, name, eventType, n_values, EventDescription.EVENT_NULL);
  }

  private boolean doNullValueNamed(EventType eventType, int n_values) throws IOException {
    final int name = eventType.getNameId();
    return doValueCommon(name, name, eventType, n_values, EventDescription.EVENT_NULL);
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
