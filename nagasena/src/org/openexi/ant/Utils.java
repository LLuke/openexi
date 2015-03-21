package org.openexi.ant;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

abstract class Utils {

  /**
   * Resolve a string representing an uri into an absolute URI given a base URI.
   * Null is returned if the uri is null or the uri seems to be a relative one
   * with baseURI being null.
   * @param uri
   * @param baseURI
   * @return absolute URI
   * @throws URISyntaxException
   */
  public static URI resolveURI(String uri, URI baseURI)
      throws URISyntaxException {
    URI resolved = null;
    if (uri != null) {
      int pos;
      if ((pos = uri.indexOf(':')) <= 1) {
        if (pos == 1) {
          char firstChar = uri.charAt(0);
          if ('A' <= firstChar && firstChar <= 'Z' ||
              'a' <= firstChar && firstChar <= 'z') {
            resolved = new File(uri).toURI();
          }
        }
        else { // relative URI
          if (baseURI != null)
            resolved = baseURI.resolve(uri);
          else
            return null;
        }
      }
      if (resolved == null)
        resolved = new URI(uri); // cross your fingers
    }
    return resolved;
  }
  
}
