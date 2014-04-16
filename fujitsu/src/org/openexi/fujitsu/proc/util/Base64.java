package org.openexi.fujitsu.proc.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public final class Base64 {
  
  private static final char[] BASE64_ASCIIS;
  private static final byte[] BASE64CHARS = new byte[8192];
  static {
    String base64Asciis = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=";
    // Base64 asciis
    for (int i = 0, len = base64Asciis.length(); i < len; i++) {
      char c = base64Asciis.charAt(i);
      BASE64CHARS[c / 8] |= 1 << (7 - c % 8);
    }
    BASE64_ASCIIS = new char[base64Asciis.length()];
    for (int i = 0, len = base64Asciis.length(); i < len; i++) {
      BASE64_ASCIIS[i] = base64Asciis.charAt(i);
    }
  }

  static final private byte[] m_octets; // Base64 ASCII -> byte (6 bits)

  static {
    m_octets = new byte[256];

    for (int i = 'Z'; i >= 'A'; i--)
      m_octets[i] = (byte)(i - 'A');
    for (int i = 'z'; i >= 'a'; i--)
      m_octets[i] = (byte)(i - 'a' + 26);
    for (int i = '9'; i >= '0'; i--)
      m_octets[i] = (byte)(i - '0' + 52);

    m_octets['+']  = 62;
    m_octets['/']  = 63;
  }
  
  private Base64() {
  }
  
  // REVISIT: abandon this method.
  public static byte[] decode(String norm) {
    byte[] octets = null;
    if (norm != null) {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      try {
        int len;
        if ((len = norm.length()) > 0) {
          final char[] enc = new char[4];
          int pos;
          for (pos = 0; pos < len;) {
            int nc;
            for (nc = 0; nc < 4 && pos < len; pos++) {
              final char c = norm.charAt(pos);
              if (isBase64Char(c))
                enc[nc++] = c;
              else if (!Character.isWhitespace(c)) {
                return null;
              }
            }
            if (nc == 4) {
              if (enc[0] == '=' || enc[1] == '=') { // invalid
                return null;
              }
              final byte b0, b1, b2, b3;
              b0 = m_octets[enc[0]];
              b1 = m_octets[enc[1]];
              baos.write(b0 << 2 | b1 >> 4);
              if (enc[2] == '=') { // it is the end
                if (enc[3] != '=') {
                  return null;
                }
                break;
              }
              b2 = m_octets[enc[2]];
              baos.write((byte)(((b1 & 0x0F) << 4) | ((b2 >> 2) & 0x0F)));
              if (enc[3] == '=') // it is the end
                break;
              b3 = m_octets[enc[3]];
              baos.write((byte)(b2 << 6 | b3));
            }
            else if (nc > 0) { // not multiple of four
              return null;
            }
          }
          for (; pos < len; pos++) { // Check if there are any extra chars
            if (!Character.isWhitespace(norm.charAt(pos))) {
              return null;
            }
          }
        }
      }
      finally {
        try {
          baos.close();
        }
        catch (IOException ioe) {
          ioe.printStackTrace();
        }
      }
      octets = baos.toByteArray();
    }
    return octets;
  }

  public static int decode(String value, final byte[] octets) {
    final int len = value.length();
    int opos = 0;
    char enc0, enc1, enc2, enc3;
    int pos;
    posLoop:
    for (pos = 0; pos != len;) {
      skipws: 
      do {
        switch (enc0 = value.charAt(pos)) {
          case '\t':
          case '\n':
          case '\r':
          case ' ':
            ++pos;
            continue posLoop;
          default:
            break skipws;
        }
      }  while (pos != len);
      if (pos++ == len || !isBase64Char(enc0))
        return -1;
      skipws:
      do {
        switch (enc1 = value.charAt(pos)) {
          case '\t':
          case '\n':
          case '\r':
          case ' ':
            ++pos;
            continue;
          default:
            break skipws;
        }
      }  while (pos != len);
      if (pos++ == len || !isBase64Char(enc1))
        return -1;
      if (enc0 == '=' || enc1 == '=')  // invalid
        return -1;
      skipws:
      do {
        switch (enc2 = value.charAt(pos)) {
          case '\t':
          case '\n':
          case '\r':
          case ' ':
            ++pos;
            continue;
          default:
            break skipws;
        }
      }  while (pos != len);
      if (pos++ == len || !isBase64Char(enc2))
        return -1;
      skipws:
      do {
        switch (enc3 = value.charAt(pos)) {
          case '\t':
          case '\n':
          case '\r':
          case ' ':
            ++pos;
            continue;
          default:
            break skipws;
        }
      }  while (pos != len);
      if (!isBase64Char(enc3))
        return -1;
      ++pos;
      final byte b0, b1, b2, b3;
      b0 = m_octets[enc0];
      b1 = m_octets[enc1];
      octets[opos++] = (byte)(b0 << 2 | b1 >> 4);
      if (enc2 == '=') { // it is the end
        if (enc3 != '=') {
          return -1;
        }
        break;
      }
      b2 = m_octets[enc2];
      octets[opos++] = (byte)(((b1 & 0x0F) << 4) | ((b2 >> 2) & 0x0F));
      if (enc3 == '=') // it is the end
        break;
      b3 = m_octets[enc3];
      octets[opos++] = (byte)(b2 << 6 | b3);
    }
    skipws:
    while (pos != len) {
      switch (value.charAt(pos)) {
        case '\t':
        case '\n':
        case '\r':
        case ' ':
          ++pos;
          continue;
        default:
          break skipws;
      }
    }
    return pos != len ? -1 : opos; 
  }

  private static boolean isBase64Char(char c) {
    return (BASE64CHARS[c / 8] & (1 << (7 - c % 8))) != 0;
  }

  public static int encode(byte[] octets, final int len, char[] encodingResult, int startIndex) {
    int opos = startIndex;
    int pos, mod;
    for (pos = 0, mod = 0; pos < len; mod++) {
      int n, st;
      for (n = 0, st = pos; n < 3 && pos < len; pos++, n++);
      assert n == 1 || n == 2 || n == 3;
      byte b0, b1;
      byte b2 = 64, b3 = 64;
      if ( (b0 = (byte) (octets[st] >> 2)) < 0)
        b0 = (byte) (b0 ^ 0xC0);
      if (n > 1) {
        if ( (b1 = (byte) (octets[st + 1] >> 4)) < 0)
          b1 = (byte) (b1 ^ 0xF0);
        b1 = (byte) ( (octets[st] & 0x03) << 4 | b1);
        if (n > 2) { // n == 3
          if ( (b2 = (byte) (octets[st + 2] >> 6)) < 0)
            b2 = (byte) (b2 ^ 0xFC);
          b2 = (byte) ( (octets[st + 1] & 0x0F) << 2 | b2);
          b3 = (byte) (octets[st + 2] & 0x3F);
        }
        else { // n == 2
          b2 = (byte) ( (octets[st + 1] & 0x0F) << 2);
        }
      }
      else { // n == 1
        b1 = (byte) ( (octets[st] & 0x03) << 4);
      }
      encodingResult[opos++] = BASE64_ASCIIS[b0]; 
      encodingResult[opos++] = BASE64_ASCIIS[b1]; 
      encodingResult[opos++] = BASE64_ASCIIS[b2]; 
      encodingResult[opos++] = BASE64_ASCIIS[b3]; 
      if (mod % 19 == 18)
        encodingResult[opos++] = '\n';
    }
    return opos - startIndex;
  }

}
