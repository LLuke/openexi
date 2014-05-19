using System;

using XmlUriConst = Nagasena.Proc.Common.XmlUriConst;

namespace Nagasena.Proc.IO {

  /// <exclude/>
  public sealed class PrefixUriBindings {

    private readonly string[] prefixes;
    private readonly string[] uris;
    private readonly string defaultUri;

    public PrefixUriBindings() : this(new string[0], new string[0], "") {
    }

    private PrefixUriBindings(string[] prefixes, string[] uris, string defaultUri) {
      this.prefixes = prefixes;
      this.uris = uris;
      this.defaultUri = defaultUri;
    }

    public int Size {
      get {
        return prefixes.Length;
      }
    }

    public string getPrefix(int i) {
      return prefixes[i];
    }

    public string getUri(int i) {
      return uris[i];
    }

    public string getPrefix(string uri) {
      int length = uris.Length;
      for (int i = 0; i < length; i++) {
        if (uri.Equals(uris[i])) {
          return prefixes[i];
        }
      }
      if (XmlUriConst.W3C_XML_1998_URI.Equals(uri)) {
        return "xml";
      }
      else {
        return null;
      }
    }

    public string getUri(string prefix) {
      int length = prefixes.Length;
      for (int i = 0; i < length; i++) {
        if (prefix.Equals(prefixes[i])) {
          return uris[i];
        }
      }
      if ("xml".Equals(prefix)) {
        return XmlUriConst.W3C_XML_1998_URI;
      }
      else {
        return null;
      }
    }

    public string DefaultUri {
      get {
        return defaultUri;
      }
    }

    public PrefixUriBindings unbind(string prefix) {
      int length = prefixes.Length;
      for (int i = 0; i < length; i++) {
        if (prefix.Equals(prefixes[i])) {
          string[] newPrefixes = new string[length - 1];
          Array.Copy(prefixes, 0, newPrefixes, 0, i);
          Array.Copy(prefixes, i + 1, newPrefixes, i, length - i - 1);
          string[] newUris = new string[length - 1];
          Array.Copy(uris, 0, newUris, 0, i);
          Array.Copy(uris, i + 1, newUris, i, length - i - 1);
          return new PrefixUriBindings(newPrefixes, newUris, defaultUri);
        }
      }
      return this;
    }

    public PrefixUriBindings bind(string prefix, string uri) {
      int i;
      for (i = 0; i < prefixes.Length; i++) {
        int res;
        if ((res = prefix.CompareTo(prefixes[i])) < 0) {
          break;
        }
        if (res == 0) {
          if (uri.Equals(uris[i])) {
            return this;
          }
          string[] _newUris = (string[])uris.Clone();
          _newUris[i] = uri;
          return new PrefixUriBindings(prefixes, _newUris, defaultUri);
        }
      }
      string[] newPrefixes = new string[prefixes.Length + 1];
      Array.Copy(prefixes, 0, newPrefixes, 0, i);
      string[] newUris = new string[uris.Length + 1];
      Array.Copy(uris, 0, newUris, 0, i);
      newPrefixes[i] = prefix;
      newUris[i] = uri;
      Array.Copy(prefixes, i, newPrefixes, i + 1, prefixes.Length - i);
      Array.Copy(uris, i, newUris, i + 1, uris.Length - i);
      return new PrefixUriBindings(newPrefixes, newUris, defaultUri);
    }

    public PrefixUriBindings bindDefault(string uri) {
      if (uri.Equals(defaultUri)) {
        return this;
      }
      return new PrefixUriBindings(prefixes, uris, uri);
    }

    public PrefixUriBindings unbindDefault() {
      if (defaultUri == "") {
        return this;
      }
      return new PrefixUriBindings(prefixes, uris, "");
    }

  }

}