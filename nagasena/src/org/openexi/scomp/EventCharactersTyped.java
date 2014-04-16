package org.openexi.scomp;

abstract  class EventCharactersTyped extends Event {

  @Override
  public final byte getEventType() {
    return CHARACTERS_TYPED;
  }
  
}
