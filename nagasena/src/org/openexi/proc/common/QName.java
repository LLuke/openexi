package org.openexi.proc.common;

/**
 * QName is a pair comprised of a namespace name and a local name  
 * to be used in a Datatype Representation Map (DTRM) definition
 * to denote an XSD datatype or an EXI datatype representation. 
 */
public final class QName {

  /**
  * Not for public use.
  * @y.exclude
  */
  public String qName;
  /**
   * If  namespaceName is <i>null</i>, this indicates a failure of
   * namespace-prefix binding. No namespace binding for the
   * unprefixed QName is indicated by an empty namespace
   * name "".
   */
  public String namespaceName;
  /**
   * Local name of the datatype.
   */
  public String localName;
  /**
   * Not for public use.
   * @y.exclude
   */
  public String prefix;
  
  public QName() {
  }
  
  /**
   * Creates a QName based on its literal qualified name 
   * (see http://www.w3.org/TR/xml-names/#ns-qualnames 
   * for definition) and namespace name. 
   * @see http://www.w3.org/TR/xml-names/#ns-qualnames
   * @param qname literal qualified name in its entirety 
   * @param uri namespace name (nullable) of the QName 
   */
  public QName(String qname, String uri) {
    qName = qname;
    final int i = qName.indexOf(':');
    if (i != -1) { // with prefix
      final int limit = qName.length();
      int pos = limit - 1;
      skipTrailingWhiteSpaces:
      for (; pos > 0; pos--) {
        switch (qname.charAt(pos)) {
          case '\t':
          case '\n':
          case '\r':
          case ' ':
            break;
          default:
            break skipTrailingWhiteSpaces;
        }
      }
      localName = qName.substring(i + 1, pos + 1);
      skipWhiteSpaces:
      for (pos = 0; pos < limit; pos++) {
        switch (qname.charAt(pos)) {
          case '\t':
          case '\n':
          case '\r':
          case ' ':
            break;
          default:
            break skipWhiteSpaces;
        }
      }
      prefix = qName.substring(pos, i);
      namespaceName = uri;
    }
    else { // no prefix
      localName = qName;
      namespaceName = uri != null ? uri : "";
      prefix = "";
    }
  }
  /**
   * Not for public use.
   * @y.exclude
   */
  public QName setValue(String uri, String localName, String prefix) {
    return setValue(uri, localName, prefix, null);
  }
  /**
   * Not for public use.
   * @y.exclude
   */
  public QName setValue(String uri, String localName, String prefix, String qname) {
    this.qName = qname;
    this.namespaceName = uri;
    this.localName = localName;
    this.prefix = prefix;
    return this;
  }

  @Override
  /**
   * True if the QName object matches this QName exactly.
   */
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
  /**
   * Not for public use.
   * @y.exclude
   */
  @Override
  public String toString() {
    if (namespaceName != null) {
      return "{ " + namespaceName + " }" + localName;
    }
    return "";
  }
  /**
   * Not for public use.
   * @y.exclude
   */  
  public static final boolean isSame(QName[] currentDTRM, int n_currentBingings, QName[] newDTRM, int n_newBindings) {
    final int len = 2 * n_newBindings;
    assert currentDTRM != null;
    if (n_currentBingings == 0) {
      if (len == 0)
        return true;
    }
    else {
      final int currentLen = 2 * n_currentBingings;
      assert currentLen != 0;
      if (len != 0 && len == currentLen) {
        int i;
        for (i = 0; i < len; i++) {
          final QName prevQName = currentDTRM[i];
          final QName qName = newDTRM[i];
          if (prevQName == null) {
            if (qName != null)
              break;
          }
          else { // prevQName != null
            if (qName == null)
              break;
            else { // neither is null
              if (!qName.equals(prevQName))
                break;
            }
          }
        }
        if (i == len)
          return true;
      }
    }
    return false;
  }
  /**
   * Not for public use.
   * @y.exclude
   */
  public int uriId;
  /**
   * Not for public use.
   * @y.exclude
   */
  public int localNameId;
  
}
