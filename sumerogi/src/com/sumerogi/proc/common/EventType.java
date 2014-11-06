package com.sumerogi.proc.common;

import com.sumerogi.schema.Characters;

/**
 * EventType denotes terminal symbols of grammar productions  
 * defined in the EXI 1.0 specification. 
 */
public class EventType extends EventCode implements EventDescription {
  
  // DO NOT CHANGE CODE ASSIGNMENT of ITEM_PI ... ITEM_EE 

  /** Event type for Start Document. */
  public static final byte ITEM_START_DOCUMENT                            = 0;

  /** Event type for End of Document. */
  public static final byte ITEM_END_DOCUMENT                              = 1;

  public static final byte ITEM_START_OBJECT_ANONYMOUS                    = 2;
  public static final byte ITEM_START_OBJECT_WILDCARD                     = 3;
  public static final byte ITEM_START_OBJECT_NAMED                        = 4;
  /** Event type for End of Object. */
  public static final byte ITEM_END_OBJECT                                = 5;

  public static final byte ITEM_START_ARRAY_ANONYMOUS                     = 6;
  public static final byte ITEM_START_ARRAY_WILDCARD                      = 7;
  public static final byte ITEM_START_ARRAY_NAMED                         = 8;
  /** Event type for End of Array. */
  public static final byte ITEM_END_ARRAY                                 = 9;

  /** Event type for anonymous String Value */
  public static final byte ITEM_STRING_VALUE_ANONYMOUS                    = 10;
  /** Event type for wildcard String Value */
  public static final byte ITEM_STRING_VALUE_WILDCARD                     = 11;
  /** Event type for named String Value */
  public static final byte ITEM_STRING_VALUE_NAMED                        = 12;
  
  /** Event type for anonymous Number Value */
  public static final byte ITEM_NUMBER_VALUE_ANONYMOUS                    = 13;
  /** Event type for wildcard Number Value */
  public static final byte ITEM_NUMBER_VALUE_WILDCARD                     = 14;
  /** Event type for named Number Value */
  public static final byte ITEM_NUMBER_VALUE_NAMED                        = 15;
  
  /** Event type for anonymous Boolean Value */
  public static final byte ITEM_BOOLEAN_VALUE_ANONYMOUS                   = 16;
  /** Event type for wildcard Boolean Value */
  public static final byte ITEM_BOOLEAN_VALUE_WILDCARD                    = 17;
  /** Event type for named Boolean Value */
  public static final byte ITEM_BOOLEAN_VALUE_NAMED                       = 18;

  /** Event type for anonymous Null Value */
  public static final byte ITEM_NULL_ANONYMOUS                            = 19;
  /** Event type for wildcard Null Value */
  public static final byte ITEM_NULL_WILDCARD                             = 20;
  /** Event type for named Null Value */
  public static final byte ITEM_NULL_NAMED                                = 21;

  /** @y.exclude */
  public final byte depth;
  /** Local name of event type definition. */
  public final String name;
  /** @y.exclude */
  public final IGrammar subsequentGrammar;
  
  private final EventCode[] m_path;
//  private final int m_uriId;
  private final int m_nameId;

  private final byte m_eventKind;

  /**
   * The index of this event type in the EventTypeList to which it belongs.
   */
  private int m_index; 

  private final EventTypeList m_ownerList;

  /**
   * @y.exclude
   */
  public EventType(byte depth, EventTypeList eventTypeList, byte itemType) {
    this(depth, eventTypeList, itemType, EventDescription.NOT_AN_EVENT);
  }

  /**
   * @y.exclude
   */
  public EventType(byte depth, EventTypeList eventTypeList, byte itemType, byte eventKind) {
    this((String)null, -1, depth, eventTypeList, itemType, eventKind);
  }

  /**
   * @y.exclude
   */
  public EventType(String name, int nameId, 
      byte depth, EventTypeList eventTypeList, byte itemType, byte eventKind) {
    this(name, nameId, depth, eventTypeList, itemType, eventKind, (IGrammar)null);
  }
  
  /**
   * @y.exclude
   */
  public EventType(String name, int nameId, byte depth, 
      EventTypeList eventTypeList, byte itemType, byte eventKind,
      IGrammar subsequentGrammar) {
    super(itemType);
    this.depth = depth;
    m_path = new EventCode[depth];
    this.name = name;
    m_nameId = nameId;
    m_index = -1;
    m_ownerList = eventTypeList;
    this.subsequentGrammar = subsequentGrammar;
    m_eventKind = eventKind; 
  }
  
  /** @y.exclude */
  public final int getDepth() {
    return depth;
  }
  
  /** @y.exclude */
  public final EventCode[] getItemPath() {
    return m_path;
  }

  /** @y.exclude */
  public final int getNameId() {
    return m_nameId;
  }
  
  /** @y.exclude */
  public final int getIndex() {
    assert m_index != -1;
    return m_ownerList.isReverse ? m_ownerList.getLength() - (m_index + 1) : m_index;
  }
  
  /** @y.exclude */
  public final EventTypeList getEventTypeList() {
    return m_ownerList;
  }

  /** @y.exclude */
  public final EventDescription asEventDescription() {
    return this;
  }
  
  ///////////////////////////////////////////////////////////////////////////
  // Implementation of EventDescription interface
  ///////////////////////////////////////////////////////////////////////////

  /** @y.exclude */
  public final byte getEventKind() {
    return m_eventKind;
  }
  
//  /** @y.exclude */
//  public final String getURI() {
//    return uri;
//  }

  /** @y.exclude */
  public final String getName() {
    return name;
  }
  
  /** @y.exclude */
  public String getPrefix() {
    return null;
  }
  
  /** @y.exclude */
  public Characters getCharacters() {
    return null;
  }

//  /** @y.exclude */
//  public BinaryDataSource getBinaryDataSource() {
//    return null;
//  }

  /** @y.exclude */
  public final EventType getEventType() {
    return this;
  }
  
  ///////////////////////////////////////////////////////////////////////////
  // Other APIs
  ///////////////////////////////////////////////////////////////////////////
  
  /** @y.exclude */
  public final void computeItemPath() {
    int depth = getDepth();
    int i;
    EventCode item;
    for (i = 0, item = this; i < depth; i++) {
      m_path[(depth - 1) - i] = item;
      item = item.parent;
    }
  }
  
  /** @y.exclude */
  public final void setIndex(int index) {
    m_index = index;
  }
  
}
