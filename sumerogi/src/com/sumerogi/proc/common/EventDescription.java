package com.sumerogi.proc.common;

import com.sumerogi.schema.Characters;

/**
 * <p>EventDescription provides accessors to the current ESON event data
 * during the decode process.</p>
 * 
 * <p>Note that the content of EventDescription is transient, which means
 * its content may change when the decoder is asked for access to the
 * next ESON event data.</p>
 */
public interface EventDescription {
  /** @y.exclude */
  public static final byte NOT_AN_EVENT = Byte.MAX_VALUE;
  /**
   * Start Document event.
   */
  public static final byte EVENT_START_DOCUMENT  = 0;
  /**
   * End Document event.
   */
  public static final byte EVENT_END_DOCUMENT  = 1;
  /**
   * Start Object event.
   */
  public static final byte EVENT_START_OBJECT  = 2;
  /**
   * End Object.
   */
  public static final byte EVENT_END_OBJECT  = 3;
  /**
   * Start Array event.
   */
  public static final byte EVENT_START_ARRAY  = 4;
  /**
   * End Array.
   */
  public static final byte EVENT_END_ARRAY  = 5;
  /**
   * String-value event.
   */
  public static final byte EVENT_STRING_VALUE  = 6;
  /**
   * Number-value event.
   */
  public static final byte EVENT_NUMBER_VALUE  = 7;
  /** 
   * Boolean-value event.
   */
  public static final byte EVENT_BOOLEAN_VALUE  = 8;
  /** 
   * Null-value event.
   */
  public static final byte EVENT_NULL  = 9;
  
  /**
   * Gets the event kind of which instance data this EventDescription is describing.  
   * @return a byte representing the event kind.
   */
  public byte getEventKind();
  
  /**
   * Gets the name of the EXI event.
   * @return the name of the event as a String.
   */
  public String getName();
  
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
   * Returns the EventType from which this event is derived.
   */
  public EventType getEventType();
  
}
