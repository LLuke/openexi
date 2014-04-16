package org.openexi.fujitsu.scomp;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;

public final class URIHelper {

  private static final String URIC_ALPHANUM =
      "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
  private static final String URIC_MARK = "-_.!~*'()";

  private static final String URIC_RESERVED   = ";/?:@&=+$,[]#%";
  private static final String URIC_UNRESERVED = URIC_ALPHANUM + URIC_MARK;

  private static final String URIC = URIC_UNRESERVED + URIC_RESERVED;

  private static final byte[] ASCIITABLE;
  private static final byte   ASCII_IS_URIC          = (byte)0x01;
  private static final byte   ASCII_IS_URIC_RESERVED = (byte)0x02;

  static {
    int i, len;
    ASCIITABLE = new byte[256];

    for (i = 0, len = URIC.length(); i < len; i++)
      ASCIITABLE[URIC.charAt(i)] |= ASCII_IS_URIC;

    for (i = 0, len = URIC_RESERVED.length(); i < len; i++)
      ASCIITABLE[URIC.charAt(i)] |= ASCII_IS_URIC_RESERVED;
  }

  private static final String URIC_HEX = "0123456789ABCDEF";

  /////////////////////////////////////////////////////////////////////////
  // Public methods
  /////////////////////////////////////////////////////////////////////////

  public static final String escapeURI(String uri) {
    StringBuffer buf = new StringBuffer();
    int i, len;
    byte[] bts = null;
    try {
      bts = uri.getBytes("UTF8");
    }
    catch (UnsupportedEncodingException e) {
      e.printStackTrace();
      throw new URIHelperConfigurationException(
          "System needs to support UTF8 encoding.", e);
    }
    for (i = 0, len = bts.length; i < len; i++) {
      final byte bt = bts[i];
      char ch = bt >= 0 ? (char)bt : (char)(256+bt);
      if (isURIChar(ch))
        buf.append(ch);
      else {
        buf.append('%');
        byte upper = (byte)(ch >> 4);
        byte lower = (byte)(ch & 0x0F);
        buf.append(URIC_HEX.charAt(upper));
        buf.append(URIC_HEX.charAt(lower));
      }
    }
    return buf.toString();
  }

  public static final String unescapeURI(String uri) {

    int current = 0;
    byte[] bts = new byte[uri.length()];

    int i, len;
    for (i = 0, len = uri.length(); i < len;) {
      char ch = uri.charAt(i++);
      if (ch != '%' || i == len || i + 1 == len) {
        bts[current++] = (byte)ch;
      }
      else {
        byte upper = (byte)URIC_HEX.indexOf(uri.charAt(i++));
        byte lower = (byte)URIC_HEX.indexOf(uri.charAt(i++));
        byte ascii = (byte)((upper << 4) + lower);
        if (URIC_RESERVED.indexOf((char)ascii) < 0)
          bts[current++] = ascii;
        else {
          // RESERVED chars changes URI semantics if unescaped, so leave them
          bts[current++] = (byte)'%';
          bts[current++] = (byte)URIC_HEX.charAt(upper);
          bts[current++] = (byte)URIC_HEX.charAt(lower);
        }
      }
    }

    String unescaped = null;
    try {
      unescaped = new String(bts, 0, current, "UTF8");
    }
    catch (UnsupportedEncodingException e) {
      e.printStackTrace();
      throw new URIHelperConfigurationException(
          "System needs to support UTF8 encoding.", e);
    }
    return unescaped;
  }

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

  /////////////////////////////////////////////////////////////////////////
  // Private methods
  /////////////////////////////////////////////////////////////////////////

  /**
   * Checks if a character is an URI char.
   * See http://www.ietf.org/rfc/rfc2396.txt
   *     http://www.w3.org/TR/REC-xml
   */
  private static boolean isURIChar(char ch) {
    if (ch < 0 || ch > 127)
      return false;

    return (ASCIITABLE[ch] & (int)ASCII_IS_URIC) != 0;
  }

//  /**
//   * Checks if a character is a reserved URI char.
//   */
//  private static boolean isReservedURIChar(char ch) {
//    if (ch < 0 || ch > 127)
//      return false;
//
//    return (ASCIITABLE[ch] & (int)ASCII_IS_URIC_RESERVED) != 0;
//  }

  /////////////////////////////////////////////////////////////////////////
  // Inner classes
  /////////////////////////////////////////////////////////////////////////

  /**
   * URIHelperConfigurationException is a class of runtime exception.
   * It generally means that there are some problems in the system
   * configuration.
   */
  public static final class URIHelperConfigurationException
      extends RuntimeException {

    private static final long serialVersionUID = -1582064518022751291L;
    
    private Exception m_exception;

    private URIHelperConfigurationException(String msg, Exception e) {
      super(msg);
      m_exception = e;
    }

    public Exception getException() {
      return m_exception;
    }
  }

}
