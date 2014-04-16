package org.openexi.scomp;

import org.apache.xerces.xs.XSObject;

abstract class Event {

  public static final byte ATTRIBUTE = 0;
  public static final byte ATTRIBUTE_WILDCARD_NS = 1;
  public static final byte ATTRIBUTE_WILDCARD = 2;
  public static final byte ELEMENT = 3;
  public static final byte ELEMENT_WILDCARD_NS = 4;
  public static final byte ELEMENT_WILDCARD = 5;
  public static final byte END_ELEMENT = 6;
  public static final byte CHARACTERS_TYPED = 7;
  public static final byte CHARACTERS_MIXED = 8;
  
  public abstract byte getEventType();
  
  protected static String roundify(String str) {
    return str != null ? str : "";
  }

  public XSObject getDeclaration() {
    assert false;
    return null;
  }

}
