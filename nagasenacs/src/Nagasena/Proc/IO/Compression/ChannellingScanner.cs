using System;
using System.IO;
using System.Diagnostics;
using System.Collections.Generic;

using ICSharpCode.SharpZipLib.Zip.Compression;

using AlignmentType = Nagasena.Proc.Common.AlignmentType;
using Channel = Nagasena.Proc.Common.Channel;
using EventDescription = Nagasena.Proc.Common.EventDescription;
using EventDescription_Fields = Nagasena.Proc.Common.EventDescription_Fields;
using EventCode = Nagasena.Proc.Common.EventCode;
using EventType = Nagasena.Proc.Common.EventType;
using EventTypeList = Nagasena.Proc.Common.EventTypeList;
using XmlUriConst = Nagasena.Proc.Common.XmlUriConst;
using Nagasena.Proc.Events;
using EventCodeTuple = Nagasena.Proc.Grammars.EventCodeTuple;
using EventTypeSchema = Nagasena.Proc.Grammars.EventTypeSchema;
using Characters = Nagasena.Schema.Characters;
using EXISchema = Nagasena.Schema.EXISchema;
using EXISchemaConst = Nagasena.Schema.EXISchemaConst;

namespace Nagasena.Proc.IO.Compression {

  public sealed class ChannellingScanner : Scanner {

    private readonly ChannelKeeper m_channelKeeper;

    private EventDescription[] m_eventList;
    private int m_n_events;

    private readonly bool m_compressed;
    private readonly Inflater m_inflater;
    private int m_bufSize;

    private bool m_foundED;
    private int m_n_blocks;

    private int m_eventIndex;

    private int n_elements;
    private EXIEventElement[] m_elementEvents;
    private EXIEventValueReference[] m_valueEvents;

    public ChannellingScanner(bool compressed) : base(false) {
      if (m_compressed = compressed) {
        m_inflater = new Inflater(true);
      }
      else {
        m_inflater = null;
      }

      m_eventList = new EventDescription[8192];
      m_n_events = 0;

      m_channelKeeper = new ChannelKeeper(new ScannerChannelFactory());

      m_valueEvents = new EXIEventValueReference[8192];

      m_elementEvents = new EXIEventElement[4096];
    }

    protected internal override void init(int inflatorBufSize) {
      m_bufSize = m_compressed ? inflatorBufSize : -1;
    }

    public override void reset() {
      base.reset();
      m_foundED = false;
      m_n_blocks = 0;
      m_n_events = 0;
      m_eventIndex = 0;
      m_channelKeeper.reset();
    }

    public override Stream InputStream {
      set {
        if (m_compressed) {
          m_inflater.Reset();
          base.InputStream = new EXIInflaterInputStream(value, m_inflater, m_bufSize);
        }
        else {
          base.InputStream = value;
        }
      }
    }

    public override void prepare() {
      base.prepare();
      processBlock(); // load buffer with initial block
    }

    public override int BlockSize {
      set {
        m_channelKeeper.BlockSize = value;
        if (m_valueEvents.Length < value) {
          EXIEventValueReference[] valueEvents = new EXIEventValueReference[value];
          Array.Copy(m_valueEvents, 0, valueEvents, 0, m_valueEvents.Length);
          m_valueEvents = valueEvents;
        }
        if (m_elementEvents.Length < value) {
          EXIEventElement[] elementEvents = new EXIEventElement[value];
          Array.Copy(m_elementEvents, 0, elementEvents, 0, m_elementEvents.Length);
          m_elementEvents = elementEvents;
        }
      }
    }

    public int BlockCount {
      get {
        return m_n_blocks;
      }
    }

    public override int BinaryChunkSize {
      set {
        throw new System.NotSupportedException("Setting binary chunk size is not supported.");
      }
    }

    public override EventDescription nextEvent() {
      if (m_eventIndex < m_n_events) {
        return m_eventList[m_eventIndex++];
      }
      else if (!m_foundED) {
        processBlock();
        return nextEvent();
      }
      return null;
    }

    private void readValueChannels() {
      m_characterBuffer.nextIndex = 0;
      if (m_binaryDataEnabled) {
        octetBuffer.nextIndex = 0;
      }
      m_channelKeeper.finish();
      ScannerChannel channel;
      IEnumerator<Channel> smallChannels = m_channelKeeper.SmallChannels.GetEnumerator();
      if (smallChannels.MoveNext()) {
        if (m_compressed && m_channelKeeper.TotalValueCount > 100) {
          ((EXIInflaterInputStream)m_inputStream).resetInflator();
        }
        do {
          channel = (ScannerChannel)smallChannels.Current;
          IList<EXIEventValueReference> textProviderList = channel.values;
          int len = textProviderList.Count;
          for (int j = 0; j < len; j++) {
            textProviderList[j].scanText(this, m_binaryDataEnabled, m_inputStream);
          }
        }
        while (smallChannels.MoveNext());
      }
      IEnumerator<Channel> largeChannels = m_channelKeeper.LargeChannels.GetEnumerator();
      while (largeChannels.MoveNext()) {
        if (m_compressed) {
          ((EXIInflaterInputStream)m_inputStream).resetInflator();
        }
        channel = (ScannerChannel)largeChannels.Current;
        IList<EXIEventValueReference> textProviderList = channel.values;
        int len = textProviderList.Count;
        for (int j = 0; j < len; j++) {
          textProviderList[j].scanText(this, m_binaryDataEnabled, m_inputStream);
        }
      }
      if (m_compressed) {
        ((EXIInflaterInputStream)m_inputStream).resetInflator();
      }
    }

    private void readStructureChannel() {
      int n_values = 0;
      n_elements = 0;
      bool reached = false;

      EventCodeTuple eventCodeTuple;
      while (!reached && (eventCodeTuple = NextEventCodes) != null) {
        int localName, uri;
        EventType eventType;
        do {
          EventCode eventCodeItem;
          int width;
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
        
        string name;
        string prefix, publicId, systemId;
        Characters text;
        ScannerChannel channel;
        EXIEventValueReference @event;
        int uriId;
        int localNameId;
        sbyte itemType;
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
            uriId = eventType.URIId;
            localNameId = eventType.NameId;
            prefix = m_preserveNS ? readPrefixOfQName(uriId) : null;
            attribute(eventType);
            if (itemType == EventType.ITEM_AT && uriId == XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI_ID && 
              localNameId == EXISchemaConst.XSI_LOCALNAME_TYPE_ID) {
              addEvent(readXsiTypeValue(prefix, eventType));
            }
            else {
              int tp = itemType == EventType.ITEM_SCHEMA_AT ? ((EventTypeSchema)eventType).nd : EXISchema.NIL_NODE;
              if ((@event = m_valueEvents[n_values]) == null) {
                m_valueEvents[n_values] = @event = new EXIEventValueReference();
              }
              channel = (ScannerChannel)m_channelKeeper.getChannel(localNameId, uriId, stringTable);
              reached = m_channelKeeper.incrementValueCount(channel);
              channel.values.Add(@event);
              @event.nameId = localNameId;
              @event.uriId = uriId;
              @event.tp = tp;
              @event.eventKind = EventDescription_Fields.EVENT_AT;
              @event.eventType = eventType;
              @event.prefix = prefix;
              @event.uri = eventType.uri;
              @event.name = eventType.name;
              @event.text = null;
              @event.binaryData = null;
              addEvent(@event);
              ++n_values;
            }
            break;
          case EventType.ITEM_SCHEMA_AT_INVALID_VALUE:
            uriId = eventType.URIId;
            localNameId = eventType.NameId;
            prefix = m_preserveNS ? readPrefixOfQName(uriId) : null;
            if ((@event = m_valueEvents[n_values]) == null) {
              m_valueEvents[n_values] = @event = new EXIEventValueReference();
            }
            channel = (ScannerChannel)m_channelKeeper.getChannel(localNameId, uriId, stringTable);
            reached = m_channelKeeper.incrementValueCount(channel);
            channel.values.Add(@event);
            attribute(eventType);
            @event.nameId = localNameId;
            @event.uriId = uriId;
            @event.tp = EXISchema.NIL_NODE;
            @event.eventKind = EventDescription_Fields.EVENT_AT;
            @event.eventType = eventType;
            @event.prefix = prefix;
            @event.uri = eventType.uri;
            @event.name = eventType.name;
            @event.text = null;
            @event.binaryData = null;
            addEvent(@event);
            ++n_values;
            break;
          case EventType.ITEM_SCHEMA_CH:
            if ((@event = m_valueEvents[n_values]) == null) {
              m_valueEvents[n_values] = @event = new EXIEventValueReference();
            }
            reached = doCharactersTyped(@event, eventType);
            addEvent(@event);
            ++n_values;
            break;
          case EventType.ITEM_SCHEMA_CH_MIXED:
            undeclaredCharacters(eventType.Index);
            if ((@event = m_valueEvents[n_values]) == null) {
              m_valueEvents[n_values] = @event = new EXIEventValueReference();
            }
            localName = m_nameLocusStack[m_nameLocusLastDepth];
            uri = m_nameLocusStack[m_nameLocusLastDepth + 1];
            channel = (ScannerChannel)m_channelKeeper.getChannel(localName, uri, stringTable);
            reached = m_channelKeeper.incrementValueCount(channel);
            channel.values.Add(@event);
            @event.nameId = localName;
            @event.uriId = uri;
            @event.tp = EXISchema.NIL_NODE;
            @event.eventKind = EventDescription_Fields.EVENT_CH;
            @event.eventType = eventType;
            @event.prefix = null;
            @event.uri = null;
            @event.name = "#text";
            @event.text = null;
            @event.binaryData = null;
            addEvent(@event);
            ++n_values;
            break;
          case EventType.ITEM_CH:
            undeclaredCharacters(eventType.Index);
            if ((@event = m_valueEvents[n_values]) == null) {
              m_valueEvents[n_values] = @event = new EXIEventValueReference();
            }
            localName = m_nameLocusStack[m_nameLocusLastDepth];
            uri = m_nameLocusStack[m_nameLocusLastDepth + 1];
            channel = (ScannerChannel)m_channelKeeper.getChannel(localName, uri, stringTable);
            reached = m_channelKeeper.incrementValueCount(channel);
            channel.values.Add(@event);
            @event.nameId = localName;
            @event.uriId = uri;
            @event.tp = EXISchema.NIL_NODE;
            @event.eventKind = EventDescription_Fields.EVENT_CH;
            @event.eventType = eventType;
            @event.prefix = null;
            @event.uri = null;
            @event.name = "#text";
            @event.text = null;
            @event.binaryData = null;
            addEvent(@event);
            ++n_values;
            break;
          case EventType.ITEM_EE:
            if (eventType.depth != EventCode.EVENT_CODE_DEPTH_ONE) {
              currentState.targetGrammar.end(currentState);
            }
            currentState = m_statesStack[--m_n_stackedStates - 1];
            m_nameLocusLastDepth -= 2;
            if (m_preserveNS) {
              --m_prefixUriBindingsLocusLastDepth;
            }
            addEvent(eventType.asEventDescription());
            break;
          case EventType.ITEM_ED:
            currentState.targetGrammar.endDocument(currentState);
            addEvent(eventType.asEventDescription());
            m_foundED = true;
            break;
          case EventType.ITEM_SCHEMA_WC_NS:
            m_characterBuffer.nextIndex = 0;
            doElementWildcard(eventType.URIId, eventType);
            break;
          case EventType.ITEM_SCHEMA_WC_ANY:
          case EventType.ITEM_SE_WC:
            m_characterBuffer.nextIndex = 0;
            doElementWildcard(readURI(), eventType);
            break;
          case EventType.ITEM_SCHEMA_AT_WC_ANY:
          case EventType.ITEM_SCHEMA_AT_WC_NS:
          case EventType.ITEM_AT_WC_ANY_UNTYPED:
            uriId = itemType == EventType.ITEM_SCHEMA_AT_WC_NS ? eventType.URIId : readURI();
            localNameId = readLocalName(stringTable.getLocalNamePartition(uriId));
            prefix = m_preserveNS ? readPrefixOfQName(uriId) : null;
            if (itemType == EventType.ITEM_AT_WC_ANY_UNTYPED) {
              wildcardAttribute(eventType.Index, uriId, localNameId);
            }
            if (uriId == XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI_ID && localNameId == EXISchemaConst.XSI_LOCALNAME_TYPE_ID) {
              Debug.Assert(itemType == EventType.ITEM_AT_WC_ANY_UNTYPED);
              addEvent(readXsiTypeValue(prefix, eventType));
            }
            else {
              string uriString = stringTable.getURI(uriId);
              string localNameString = stringTable.getLocalNamePartition(uriId).localNameEntries[localNameId].localName;
              int tp = EXISchema.NIL_NODE;
              if (itemType != EventType.ITEM_AT_WC_ANY_UNTYPED) {
                int attr;
                if ((attr = schema.getGlobalAttrOfSchema(uriString, localNameString)) != EXISchema.NIL_NODE) {
                  tp = schema.getTypeOfAttr(attr);
                }
              }
              if ((@event = m_valueEvents[n_values]) == null) {
                m_valueEvents[n_values] = @event = new EXIEventValueReference();
              }
              channel = (ScannerChannel)m_channelKeeper.getChannel(localNameId, uriId, stringTable);
              reached = m_channelKeeper.incrementValueCount(channel);
              channel.values.Add(@event);
              @event.nameId = localNameId;
              @event.uriId = uriId;
              @event.tp = tp;
              @event.eventKind = EventDescription_Fields.EVENT_AT;
              @event.eventType = eventType;
              @event.prefix = prefix;
              @event.uri = uriString;
              @event.name = localNameString;
              @event.text = null;
              @event.binaryData = null;
              addEvent(@event);
              ++n_values;
            }
            break;
          case EventType.ITEM_SCHEMA_NIL:
            uriId = eventType.URIId;
            localNameId = eventType.NameId;
            prefix = m_preserveNS ? readPrefixOfQName(uriId) : null;
            EXIEventSchemaNil eventSchemaNil = readXsiNilValue(prefix, eventType);
            if (eventSchemaNil.Nilled) {
              nillify(eventType.Index);
            }
            addEvent(eventSchemaNil);
            break;
          case EventType.ITEM_SCHEMA_TYPE:
            uriId = eventType.URIId;
            localNameId = eventType.NameId;
            prefix = m_preserveNS ? readPrefixOfQName(uriId) : null;
            addEvent(readXsiTypeValue(prefix, eventType));
            break;
          case EventType.ITEM_NS:
            addEvent(readNS(eventType));
            break;
          case EventType.ITEM_SC:
            throw new System.NotSupportedException("Event type SC is not supported yet.");
          case EventType.ITEM_PI:
            miscContent(eventType.Index);
            name = readText().makeString();
            text = readText();
            text.turnPermanent();
            addEvent(new EXIEventProcessingInstruction(name, text, eventType));
            break;
          case EventType.ITEM_CM:
            miscContent(eventType.Index);
            text = readText();
            text.turnPermanent();
            addEvent(new EXIEventComment(text, eventType));
            break;
          case EventType.ITEM_ER:
            miscContent(eventType.Index);
            name = readText().makeString();
            addEvent(new EXIEventEntityReference(name, eventType));
            break;
          default:
            Debug.Assert(false);
            break;
        }
      }
      if (reached) {
        do {
          EventTypeList eventTypeList = NextEventTypes;
          int n_eventTypes;
          if ((n_eventTypes = eventTypeList.Length) != 1) {
            Debug.Assert(n_eventTypes > 1);
            break;
          }
          EventType eventType;
          switch ((eventType = eventTypeList.item(0)).itemType) {
            case EventType.ITEM_SE:
              int uriId = eventType.URIId;
              int localNameId = eventType.NameId;
              string prefix = m_preserveNS ? readPrefixOfQName(uriId) : null;
              pushLocusItem(uriId, localNameId);
              startElement(eventType);
              if (m_preserveNS) {
                EXIEventElement @event;
                if ((@event = m_elementEvents[n_elements]) == null) {
                  m_elementEvents[n_elements] = @event = new EXIEventElement();
                }
                ++n_elements;
                @event.prefix = prefix;
                @event.eventType = eventType;
                addEvent(@event);
              }
              else {
                addEvent(eventType.asEventDescription());
              }
              break;
            case EventType.ITEM_EE:
              endElement();
              m_nameLocusLastDepth -= 2;
              if (m_preserveNS) {
                --m_prefixUriBindingsLocusLastDepth;
              }
              addEvent(eventType.asEventDescription());
              break;
            case EventType.ITEM_ED:
              endDocument();
              addEvent(eventType.asEventDescription());
              m_foundED = true;
              // falling through...
              goto default;
            default:
              goto readMoreBreak;
          }
        }
        while (true);
        readMoreBreak:;
      }
    }

    private void processBlock() {
      ++m_n_blocks;
      m_n_events = 0;
      readStructureChannel();
      readValueChannels();
      m_channelKeeper.punctuate();
      m_eventIndex = 0;
    }

    public override AlignmentType AlignmentType {
      get {
        return m_compressed ? AlignmentType.compress : AlignmentType.preCompress;
      }
    }

    ///////////////////////////////////////////////////////////////////////////
    /// Event handlers
    ///////////////////////////////////////////////////////////////////////////

    private bool doCharactersTyped(EXIEventValueReference @event, EventType eventType) {
      bool reached;
      currentState.targetGrammar.chars(eventType, currentState);
      int localName = m_nameLocusStack[m_nameLocusLastDepth];
      int uri = m_nameLocusStack[m_nameLocusLastDepth + 1];
      ScannerChannel channel = (ScannerChannel)m_channelKeeper.getChannel(localName, uri, stringTable);
      reached = m_channelKeeper.incrementValueCount(channel);
      channel.values.Add(@event);
      @event.nameId = localName;
      @event.uriId = uri;
      @event.tp = currentState.contentDatatype;
      @event.eventKind = EventDescription_Fields.EVENT_CH;
      @event.eventType = eventType;
      @event.prefix = null;
      @event.uri = null;
      @event.name = "#text";
      @event.text = null;
      @event.binaryData = null;
      return reached;
    }

    private void doElement(EventType eventType) {
      m_characterBuffer.nextIndex = 0;
      int uri = eventType.URIId;
      int localName = eventType.NameId;
      pushLocusItem(uri, localName);
      currentState.targetGrammar.element(eventType, currentState);
      if (m_preserveNS) {
        EXIEventElement @event;
        if ((@event = m_elementEvents[n_elements]) == null) {
          m_elementEvents[n_elements] = @event = new EXIEventElement();
        }
        ++n_elements;
        @event.prefix = readPrefixOfQName(uri);
        @event.eventType = eventType;
        addEvent(@event);
      }
      else {
        addEvent(eventType.asEventDescription());
      }
    }

    private void doElementWildcard(int uri, EventType eventType) {
      int localName = readLocalName(stringTable.getLocalNamePartition(uri));
      string prefix = m_preserveNS ? readPrefixOfQName(uri) : null;
      pushLocusItem(uri, localName);
      startWildcardElement(eventType.Index, uri, localName);
      addEvent(new EXIEventWildcardStartElement(stringTable.getURI(uri), 
        stringTable.getLocalNamePartition(uri).localNameEntries[localName].localName, 
        uri, localName, prefix, eventType));
    }

    ///////////////////////////////////////////////////////////////////////////
    /// Scanning Functions for private use
    ///////////////////////////////////////////////////////////////////////////

    protected internal override bool readBoolean(Stream istream) {
      return ByteAlignedCommons.readBoolean(istream);
    }

    protected internal override int readNBitUnsigned(int width, Stream istream) {
      if (width != 0) {
        return ByteAlignedCommons.readNBitUnsigned(width, istream);
      }
      return 0;
    }

    protected internal override int readEightBitsUnsigned(Stream istream) {
      return ByteAlignedCommons.readEightBitsUnsigned(istream);
    }

    ///////////////////////////////////////////////////////////////////////////
    /// Convenience Functions
    ///////////////////////////////////////////////////////////////////////////

    private void pushLocusItem(int uri, int localName) {
      m_nameLocusLastDepth += 2;
      m_nameLocusStack[m_nameLocusLastDepth] = localName;
      m_nameLocusStack[m_nameLocusLastDepth + 1] = uri;
      if (m_preserveNS) {
        m_prefixUriBindingsLocusStack[++m_prefixUriBindingsLocusLastDepth] = m_prefixUriBindings;
      }
    }

    private void addEvent(EventDescription @event) {
      if (m_eventList.Length == m_n_events) {
        int len = m_eventList.Length;
        len += len >> 1;
        EventDescription[] eventList = new EventDescription[len];
        Array.Copy(m_eventList, 0, eventList, 0, m_eventList.Length);
        m_eventList = eventList;
      }
      m_eventList[m_n_events++] = @event;
    }

  }

}