package org.openexi.proc.io;

public final class PrefixUriBindings {

  final private String[] prefixes;
  final private String[] uris;
  final private String defaultUri;

  public PrefixUriBindings() {
    this(new String[0], new String[0], "");
  }

  private PrefixUriBindings(String[] prefixes, String[] uris, String defaultUri) {
    this.prefixes = prefixes;
    this.uris = uris;
    this.defaultUri = defaultUri;
  }

  public final int getSize() {
    return prefixes.length;
  }

  public final String getPrefix(int i) {
    return prefixes[i];
  }

  public final String getUri(int i) {
    return uris[i];
  }

  public final String getPrefix(String uri) {
    final int length = uris.length;
    for (int i = 0; i < length; i++) {
      if (uri.equals(uris[i]))
        return prefixes[i];
    }
    return null;
  }

  public final String getUri(String prefix) {
    final int length = prefixes.length;
    for (int i = 0; i < length; i++) {
      if (prefix.equals(prefixes[i]))
        return uris[i];
    }
    return null;
  }

  public final String getDefaultUri() {
    return defaultUri;
  }

  public PrefixUriBindings unbind(String prefix) {
    final int length = prefixes.length;
    for (int i = 0; i < length; i++) {
      if (prefix.equals(prefixes[i])) {
        String[] newPrefixes = new String[length - 1];
        System.arraycopy(prefixes, 0, newPrefixes, 0, i);
        System.arraycopy(prefixes, i + 1, newPrefixes, i, length - i - 1);
        String[] newUris = new String[length - 1];
        System.arraycopy(uris, 0, newUris, 0, i);
        System.arraycopy(uris, i + 1, newUris, i, length - i - 1);
        return new PrefixUriBindings(newPrefixes, newUris, defaultUri);
      }
    }
    return this;
  }

  public PrefixUriBindings bind(String prefix, String uri) {
    int i;
    for (i = 0; i < prefixes.length; i++) {
      final int res;
      if ((res = prefix.compareTo(prefixes[i])) < 0)
        break;
      if (res == 0) {
        if (uri.equals(uris[i]))
          return this;
        String[] newUris = (String[])uris.clone();
        newUris[i] = uri;
        return new PrefixUriBindings(prefixes, newUris, defaultUri);
      }
    }
    String[] newPrefixes = new String[prefixes.length + 1];
    System.arraycopy(prefixes, 0, newPrefixes, 0, i);
    String[] newUris = new String[uris.length + 1];
    System.arraycopy(uris, 0, newUris, 0, i);
    newPrefixes[i] = prefix;
    newUris[i] = uri;
    System.arraycopy(prefixes, i, newPrefixes, i + 1, prefixes.length - i);
    System.arraycopy(uris, i, newUris, i + 1, uris.length - i);
    return new PrefixUriBindings(newPrefixes, newUris, defaultUri);
  }

  public PrefixUriBindings bindDefault(String uri) {
    if (uri.equals(defaultUri))
      return this;
    return new PrefixUriBindings(prefixes, uris, uri);
  }

  public PrefixUriBindings unbindDefault() {
    if (defaultUri == "")
      return this;
    return new PrefixUriBindings(prefixes, uris, "");
  }

}
