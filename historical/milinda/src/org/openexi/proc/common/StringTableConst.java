package org.openexi.proc.common;

public final class StringTableConst {

  /**
   * Initial Entries in "http://www.w3.org/XML/1998/namespace" localName partition.
   */
  public final static String[] XML_LOCALNAMES = { "base", "id", "lang", "space" };
  
  /**
   * Initial Entries in "http://www.w3.org/2001/XMLSchema-instance" localName partition.
   */
  public final static String[] XSI_LOCALNAMES = { "nil", "type" }; 
  
  /**
   * Initial Entries in "http://www.w3.org/2001/XMLSchema" localName partition.
   */
  public final static String[] XSD_LOCALNAMES = { "ENTITIES", "ENTITY", "ID", "IDREF", 
    "IDREFS", "NCName", "NMTOKEN", "NMTOKENS", "NOTATION", "Name", "QName", "anySimpleType", 
    "anyType", "anyURI", "base64Binary", "boolean", "byte", "date", "dateTime", "decimal", 
    "double", "duration", "float", "gDay", "gMonth", "gMonthDay", "gYear", "gYearMonth", 
    "hexBinary", "int", "integer", "language", "long", "negativeInteger", "nonNegativeInteger", 
    "nonPositiveInteger", "normalizedString", "positiveInteger", "short", "string", "time", 
    "token", "unsignedByte", "unsignedInt", "unsignedLong", "unsignedShort" };

  private StringTableConst() {
  }

}
