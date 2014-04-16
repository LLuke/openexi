package org.openexi.fujitsu.proc.common;

public interface EXIEvent {

  public static final byte EVENT_SD  = 0;
  public static final byte EVENT_ED  = 1;
  public static final byte EVENT_SE  = 2;
  public static final byte EVENT_AT  = 3;
  public static final byte EVENT_TP  = 4;
  public static final byte EVENT_NL  = 5;
  public static final byte EVENT_CH  = 6;
  public static final byte EVENT_EE  = 7;
  public static final byte EVENT_NS  = 8;
  public static final byte EVENT_PI  = 9;
  public static final byte EVENT_CM  = 10;
  public static final byte EVENT_ER  = 11;
  public static final byte EVENT_DTD = 12;
  
  public byte getEventVariety();
  
  public String getURI();
  
  public String getName();
  
  public String getPrefix();
  
  public CharacterSequence getCharacters();

  /**
   * Return the event type that this event is derived from.
   */
  public EventType getEventType();
  
}
