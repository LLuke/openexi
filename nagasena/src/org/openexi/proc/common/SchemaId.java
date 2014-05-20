package org.openexi.proc.common;

/**
 * SchemaId represents the <a href="http://www.w3.org/TR/exi/#key-schemaIdOption">schemaId</a> 
 * property of an EXI stream.
 */
public final class SchemaId {

  private String m_value;

  public SchemaId(String val) {
    m_value = val;
  }
  
  public String getValue() {
    return m_value;
  }

}
