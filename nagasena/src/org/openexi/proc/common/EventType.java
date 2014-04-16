package org.openexi.proc.common;

import org.openexi.schema.Characters;

/**
 * EventType denotes terminal symbols of grammar productions  
 * defined in the EXI 1.0 specification. 
 */
public class EventType extends EventCode implements EventDescription {
  
  // DO NOT CHANGE CODE ASSIGNMENT of ITEM_PI ... ITEM_EE 
  /**
   * Event type for a Processing Instruction. Value is 0.
   */
  public static final byte ITEM_PI                                        = 0;
  /**
   * Event type for a Comment.
   * <br/>Value is {@value}.
   */
  public static final byte ITEM_CM                                        = 1;
  /**
   * Event type for an Entity Reference.
   * <br/>Value is {@value}.
   */

  public static final byte ITEM_ER                                        = 2;
  /**
   * Event type for a Character event (character events store values as strings).
   * <br/>Value is {@value}.
   */
  public static final byte ITEM_CH                                        = 3;
  /**
   * Event type for End of Document.
   * <br/>Value is {@value}.
   */
  public static final byte ITEM_ED                                        = 4;
  /**
   * Wildcard event type for an element. OpenEXI will first attempt
   * to find a corresponding element name in the schema, if present. If 
   * no definition is available, it is given this tag.
   * <br/>Value is {@value}.
   */
  public static final byte ITEM_SE_WC                                     = 5;
  /**
   * Self-contained items are not supported in this release of OpenEXI. 
   * Event type for self-contained item.
   * <br/>Value is {@value}.
   */
  public static final byte ITEM_SC                                        = 6;
  /**
   * Event type for a Namespace declaration.<br/>
   * <br/>Value is {@value}.
   */
  public static final byte ITEM_NS                                        = 7;
  /**
   *  Wildcard event type for an Attribute where the attribute's defined 
   *  datatype (if any) is disregarded. 
   *  This is the "catch-all" for Attributes that do not match any of the 
   *  other Event Types in an EXI stream processed using Default options. 
   *  <br />Value is {@value}.
   */
  public static final byte ITEM_AT_WC_ANY_UNTYPED                         = 8;
  /** 
   * Event type for End of Element.
   * <br />Value is {@value}.
   */
  public static final byte ITEM_EE                                        = 9;
  /**
   * Event type for a Document Type Definition.
   * <br />Value is {@value}.
   */
  public static final byte ITEM_DTD                                       = 10;
  /**
   * Event type for Start Element.
   * <br/>Value is {@value}.
   */
  public static final byte ITEM_SE                                        = 11;
  /**
   * Event type for an Attribute learned by built-in element grammars 
   * from prior attribute occurrences.
   * <br/>Value is {@value}.
   */
  public static final byte ITEM_AT                                        = 12;
  /**
   * Event type for Start Document.
   * <br/>Value is {@value}.
   */
  public static final byte ITEM_SD                                        = 13;
  /**
   * Attribute wildcard event type stemming from a schema where the 
   * attribute's defined datatype (if any) is applied. 
   * <br/>Value is {@value}.
   */
  public static final byte ITEM_SCHEMA_WC_ANY                             = 14;
  /**
   * Event type for an element defined in a namespace in an EXI stream
   * processed using a schema. 
   * <br/>Value is {@value}.
   */
  public static final byte ITEM_SCHEMA_WC_NS                              = 15;
  /**
   * Event type for AttributeUse that matches an attribute event with
   * a valid value.
   * <br/>Value is {@value}.
   */
  public static final byte ITEM_SCHEMA_AT                                 = 16;
  /**
   * Attribute wildcard event type stemming from a schema, where the attribute's 
   * defined datatype (if any) is applied.
   * <br/>Value is {@value}.
   */
  public static final byte ITEM_SCHEMA_AT_WC_ANY                          = 17;
  /**
   * Attribute wildcard event type, qualified with a specific namespace, stemming
   * from a schema where the attribute's defined datatype (if any) is applied. 
   * <br/>Value is {@value}.
   */
  public static final byte ITEM_SCHEMA_AT_WC_NS                           = 18;
  /** 
   * Event type for a defined Character event in an EXI stream processed
   * using a schema. 
   * <br/>Value is {@value}.
   */
  public static final byte ITEM_SCHEMA_CH                                 = 19;
  /**
   * Event type for a Character event that occurs in the context of an element 
   * defined so as to permit mixed content (mark up and data) in an EXI 
   * stream processed using a schema. 
   * <br/>Value is {@value}.
   */
  public static final byte ITEM_SCHEMA_CH_MIXED                           = 20;
  /**
   * Special Attribute that indicates the value of the associated element is
   * explicitly <i>nil</i> rather than an empty string.
   * <br/>Value is {@value}.
   */
  public static final byte ITEM_SCHEMA_NIL                                = 21;
  /**
   * Special Attribute that describes a data type for the associated
   * element. For example, the schema might define a String value, but
   * the XML document being processed can declare that the element contains a
   * date-time field.
   * <br/>Value is {@value}.
   */
  public static final byte ITEM_SCHEMA_TYPE                               = 22;
  /**
   * Event type for AttributeUse that matches an attribute event with an 
   * invalid value. 
   * <br/>Value is {@value}.
   */
  public static final byte ITEM_SCHEMA_AT_INVALID_VALUE        = 23;

  /** @y.exclude */
  public final byte depth;
  /** URI of event type definition. */
  public final String uri;
  /** Local name of event type definition. */
  public final String name;
  /** @y.exclude */
  public final IGrammar subsequentGrammar;
  
  private final EventCode[] m_path;
  private final int m_uriId;
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
  public EventType(byte depth, EventTypeList eventTypeList, byte itemType, IGrammar subsequentGrammar) {
    this((String)null, (String)null, -1, -1, depth, eventTypeList, itemType, EventDescription.NOT_AN_EVENT, subsequentGrammar);
  }
  
  /**
   * @y.exclude
   */
  public EventType(String uri, String name, int uriId, int nameId, 
      byte depth, EventTypeList eventTypeList, byte itemType, 
      IGrammar subsequentGrammar) {
    this(uri, name, uriId, nameId, depth, eventTypeList, itemType, EventDescription.NOT_AN_EVENT, subsequentGrammar);
  }
  
  /**
   * @y.exclude
   */
  public EventType(String uri, String name, int uriId, int nameId, 
      byte depth, EventTypeList eventTypeList, byte itemType, byte eventKind,
      IGrammar subsequentGrammar) {
    super(itemType);
    this.depth = depth;
    m_path = new EventCode[depth];
    this.uri = uri;
    this.name = name;
    m_uriId = uriId;
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
  public final int getURIId() {
    return m_uriId;
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
  
  /** @y.exclude */
  public final String getURI() {
    return uri;
  }

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

  /** @y.exclude */
  public BinaryDataSource getBinaryDataSource() {
    return null;
  }

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
