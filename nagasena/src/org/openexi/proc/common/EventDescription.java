package org.openexi.proc.common;

import org.openexi.schema.Characters;

/**
 * <p>EventDescription provides accessors to the current EXI event data
 * during the decode process.</p>
 * 
 * <p>Note that the content of EventDescription is transient, which means
 * its content may change when the decoder is asked for access to the
 * next EXI event data.</p>
 */
public interface EventDescription {
  /** @y.exclude */
  public static final byte NOT_AN_EVENT = -1;
  /**
   * Start Document event.
   */
  public static final byte EVENT_SD  = 0;
  /**
   * End Document event.
   */
  public static final byte EVENT_ED  = 1;
  /**
   * Start Element event.
   */
  public static final byte EVENT_SE  = 2;
  /**
   * Attribute event.
   */
  public static final byte EVENT_AT  = 3;
  /**
   * Attribute <i>xsi:type</i>.
   */
  public static final byte EVENT_TP  = 4;
  /** 
   *  Attribute <i>xsi:nil</i>.
   */
  public static final byte EVENT_NL  = 5;
  /**
   * Character event (content of an element).
   */
  public static final byte EVENT_CH  = 6;
  /**
   * End Element event.
   */
  public static final byte EVENT_EE  = 7;
  /**
   * Namespace declaration event.
   */
  public static final byte EVENT_NS  = 8;
  /**
   * Processing Instruction event.
   */
  public static final byte EVENT_PI  = 9;
  /**
   * Comment event.
   */
  public static final byte EVENT_CM  = 10;
  /**
   * Entity Reference event.
   */
  public static final byte EVENT_ER  = 11;
  /**
   * Document Type Definition event.
   */
  public static final byte EVENT_DTD = 12;
  /**
   * BLOB event (content of an element).
   */
  public static final byte EVENT_BLOB  = 13;
  
  /**
   * Gets the event kind of which instance data this EventDescription is describing.  
   * @return a byte representing the event kind.
   */
  public byte getEventKind();
  
  /**
   * Gets the URI of the EXI event.
   * @return the URI as a String.
   */
  public String getURI();
  
  /**
   * Gets the name of the EXI event.
   * @return the name of the event as a String.
   */
  public String getName();
  
  /**
   * Gets the namespace prefix of the event.
   * @return the prefix as a String.
   */
  public String getPrefix();

  /** @y.exclude */
  public int getURIId();

  /** @y.exclude */
  public int getNameId();

  /**
   * Gets the value of an EVENT_CH, 
   * Attribute (EVENT_AT, EVENT_NL, EVENT_TP), EVENT_CM,
   * EVENT_DTD or EVENT_PI event.
   * @return a Characters of the corresponding value
   */
  public Characters getCharacters();

  /**
   * Gets the value of an EVENT_BLOB. 
   * @return a BinaryData of the corresponding value
   */
  public BinaryDataSource getBinaryDataSource();

  /**
   * Returns the EventType from which this event is derived.
   */
  public EventType getEventType();
  
}
