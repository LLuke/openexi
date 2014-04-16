using System.Diagnostics;

namespace Nagasena.Proc.Common {
  /// <summary>
  /// QName is a pair comprised of a namespace name and a local name  
  /// to be used in a Datatype Representation Map (DTRM) definition
  /// to denote an XSD datatype or an EXI datatype representation. 
  /// </summary>
  public sealed class QName {

    internal string qName;
    /// <summary>
    /// If  namespaceName is <i>null</i>, this indicates a failure of
    /// namespace-prefix binding. No namespace binding for the
    /// unprefixed QName is indicated by an empty namespace
    /// name "".
    /// </summary>
    public string namespaceName;
    /// <summary>
    /// Local name of the datatype.
    /// </summary>
    public string localName;
    internal string prefix;

    public QName() {
    }

    /// <summary>
    /// Creates a QName based on its literal qualified name 
    /// (see http://www.w3.org/TR/xml-names/#ns-qualnames 
    /// for definition) and namespace name. </summary>
    /// <seealso cref= http://www.w3.org/TR/xml-names/#ns-qualnames </seealso>
    /// <param name="qname"> literal qualified name in its entirety </param>
    /// <param name="uri"> namespace name (nullable) of the QName  </param>
    public QName(string qname, string uri) {
      qName = qname;
      int i = qName.IndexOf(':');
      if (i != -1) { // with prefix
        int limit = qName.Length;
        int pos = limit - 1;
        for (; pos > 0; pos--) {
          switch (qname[pos]) {
            case '\t':
            case '\n':
            case '\r':
            case ' ':
              break;
            default:
              goto skipTrailingWhiteSpacesBreak;
          }
        }
        skipTrailingWhiteSpacesBreak:
        localName = qName.Substring(i + 1, pos + 1 - (i + 1));
        for (pos = 0; pos < limit; pos++) {
          switch (qname[pos]) {
            case '\t':
            case '\n':
            case '\r':
            case ' ':
              break;
            default:
              goto skipWhiteSpacesBreak;
          }
        }
        skipWhiteSpacesBreak:
        prefix = qName.Substring(pos, i - pos);
        namespaceName = uri;
      }
      else { // no prefix
        localName = qName;
        namespaceName = uri != null ? uri : "";
        prefix = "";
      }
    }

    internal QName setValue(string uri, string localName, string prefix) {
      return setValue(uri, localName, prefix, null);
    }

    internal QName setValue(string uri, string localName, string prefix, string qname) {
      this.qName = qname;
      this.namespaceName = uri;
      this.localName = localName;
      this.prefix = prefix;
      return this;
    }

    /// <summary>
    /// True if the QName object matches this QName exactly.
    /// </summary>
    public override bool Equals(object obj)
    {
      if (obj != null && obj is QName) {
        QName that = (QName)obj;
        if (namespaceName != null) {
          return namespaceName.Equals(that.namespaceName) && localName.Equals(that.localName);
        }
        /// NOTE: null namespace makes it an unequivocal comparison.
        /// else if (that.getNamespace() == null) {
        ///   return m_localname.equals(that.getLocalPart());
        /// }
      }
      return false;
    }

    /// <summary>
    /// Not for public use.
    /// @y.exclude
    /// </summary>
    public override string ToString() {
      if (namespaceName != null) {
        return "{ " + namespaceName + " }" + localName;
      }
      return "";
    }
    
    internal static bool isSame(QName[] currentDTRM, int n_currentBingings, QName[] newDTRM, int n_newBindings) {
      int len = 2 * n_newBindings;
      Debug.Assert(currentDTRM != null);
      if (n_currentBingings == 0) {
        if (len == 0) {
          return true;
        }
      }
      else {
        int currentLen = 2 * n_currentBingings;
        Debug.Assert(currentLen != 0);
        if (len != 0 && len == currentLen) {
          int i;
          for (i = 0; i < len; i++) {
            QName prevQName = currentDTRM[i];
            QName qName = newDTRM[i];
            if (prevQName == null) {
              if (qName != null) {
                break;
              }
            }
            else { // prevQName != null
              if (qName == null) {
                break;
              }
              else { // neither is null
                if (!qName.Equals(prevQName)) {
                  break;
                }
              }
            }
          }
          if (i == len) {
            return true;
          }
        }
      }
      return false;
    }

    /// <summary>
    /// Not for public use.
    /// @y.exclude
    /// </summary>
    public int uriId;

    /// <summary>
    /// Not for public use.
    /// @y.exclude
    /// </summary>
    public int localNameId;

  }

}