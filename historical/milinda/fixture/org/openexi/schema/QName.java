package org.openexi.schema;

public final class QName {

  public String qName;
  /**
   * namespace name of value null indicates a failure of
   * namespace-prefix binding. No namespace binding for the
   * unprefixed qname is indicated by an empty namespace
   * name "".
   */
  public String namespaceName;
  public String localName;
  public String prefix;

  public QName() {
  }
  
  /**
   * Creates a QName based on its literal qname and namespace name. 
   * @param qname literal qualified name as its entirety 
   * @param uri namespace name (nullable) of the QName 
   */
  public QName(String qname, String uri) {
    qName = qname;
    final int i = qName.indexOf(':');
    if (i != -1) { // with prefix
      localName = qName.substring(i + 1);
      namespaceName = uri;
      prefix = qName.substring(0, i);
    }
    else { // no prefix
      localName = qName;
      namespaceName = uri != null ? uri : "";
      prefix = "";
    }
  }
  
  public QName setValue(String uri, String localName, String prefix) {
    this.qName = null;
    this.namespaceName = uri;
    this.localName = localName;
    this.prefix = prefix;
    return this;
  }
  
  public QName setXsiTypeValue(String qname, PrefixUriBindings namespacePrefixMap) {
    qName = qname;
    final int i = qName.indexOf(':');
    if (i != -1) { // with prefix
      prefix = qName.substring(0, i);
      namespaceName = namespacePrefixMap.getUri(prefix);
      if (namespaceName != null) {
        localName = qName.substring(i + 1);
      }
      else { //  prefix did not resolve into an uri  
        namespaceName = "";
        localName = qname;
        prefix = "";
      }
    }
    else { // no prefix
      localName = qName;
      String uri;
      if ((uri = namespacePrefixMap.getDefaultUri()) == null) {
        // unprefixed qname with null namespace name is in *blank* namespace
        uri = "";
      }
      namespaceName = uri;
      prefix = "";
    }
    return this;
  }
  
  @Override
  public boolean equals(Object obj) {
    if (obj != null && obj instanceof QName) {
      final QName that = (QName)obj;
      if (namespaceName != null) {
        return namespaceName.equals(that.namespaceName) && localName.equals(that.localName);
      }
      /**
       * NOTE: null namespace makes it an unequivocal comparison.
       * else if (that.getNamespace() == null) {
       *   return m_localname.equals(that.getLocalPart());
       * }
       */
    }
    return false;
  }

  @Override
  public String toString() {
    if (namespaceName != null) {
      return "{ " + namespaceName + " }" + localName;
    }
    return "";
  }
  
}
