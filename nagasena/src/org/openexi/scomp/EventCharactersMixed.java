package org.openexi.scomp;

abstract class EventCharactersMixed extends Event {

  @Override
  public final byte getEventType() {
    return CHARACTERS_MIXED;
  }

}
