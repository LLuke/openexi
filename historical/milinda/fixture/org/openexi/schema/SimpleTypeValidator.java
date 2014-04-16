package org.openexi.schema;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * SimpleTypeValidator validates text values against the declared
 * schema datatypes.
 */
public final class SimpleTypeValidator {

  private static final String BASE64_ASCIIS =
    "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=";

  private static final String HEXBIN_ASCIIS = "0123456789ABCDEF";

  private static final byte[] BASE64CHARS = new byte[8192];

  static {
    int i, len;
    // Base64 asciis
    for (i = 0, len = BASE64_ASCIIS.length(); i < len; i++) {
      char c = BASE64_ASCIIS.charAt(i);
      BASE64CHARS[c / 8] |= 1 << (7 - c % 8);
    }
  }

  private static boolean isBase64Char(char c) {
    return (BASE64CHARS[c / 8] & (1 << (7 - c % 8))) != 0;
  }

  /**
   * Encode octets into a base64-encoded ascii equivalent.
   * @param octets binary data
   * @return ascii text
   */
  public static String encodeBinaryByBase64(byte[] octets) {
    StringBuffer encodingResult = new StringBuffer();
    Base64.encode(octets, encodingResult);
    return encodingResult.toString();
  }

  /**
   * Encode octets into a hex-encoded ascii equivalent.
   * @param octets binary data
   * @return ascii text
   */
  public static String encodeBinaryByHexBin(byte[] octets) {
    StringBuffer encodingResult = new StringBuffer();
    HexBin.encode(octets, encodingResult);
    return encodingResult.toString();
  }
  
  public static class HexBin {
    public static byte[] decode(String norm)
        throws SchemaValidatorException {
      byte[] octets = null;
      if (norm != null) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
          int pos, len, dec;
          for (pos = 0, len = norm.length(), dec = 0; pos < len; dec = 0) {
            int nc;
            for (nc = 0; nc < 2 && pos < len; pos++) {
              char c = norm.charAt(pos);
              if (Character.isWhitespace(c)) {
                // Permit whitespaces for now.
                continue;
              }
              else if ('\u0060' < c) { // 'a' <= c
                if ('\u0066' < c) { // 'f' < c
                  throw new SchemaValidatorException(
                      SchemaValidatorException.INVALID_HEX_BINARY,
                      new String[] { norm }, null, EXISchema.NIL_NODE);
                }
                else // between 'a' and 'f'
                  dec |= (10 + (c - 'a')) << (4 * (1 - nc++));
              }
              else if (c < '\u003a') { // c <= '9'
                if (c < '\u0030') { // c < '0'
                  throw new SchemaValidatorException(
                      SchemaValidatorException.INVALID_HEX_BINARY,
                      new String[] { norm }, null, EXISchema.NIL_NODE);
                }
                else // between '0' and '9'
                  dec |= (c - '0') << (4 * (1 - nc++));
              }
              else if ('\u0040' < c && c < '\u0047') { // between 'A' and 'F'
                dec |= (10 + (c - 'A')) << (4 * (1 - nc++));
              }
              else { // off the range.
                throw new SchemaValidatorException(
                    SchemaValidatorException.INVALID_HEX_BINARY,
                    new String[] { norm }, null, EXISchema.NIL_NODE);
              }
            }
            if (nc < 2) {
              throw new SchemaValidatorException(
                  SchemaValidatorException.INVALID_HEX_BINARY,
                  new String[] { norm }, null, EXISchema.NIL_NODE);
            }
            baos.write(dec);
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

    public static void encode(byte[] octets, StringBuffer encodingResult) {
      if (octets != null && encodingResult != null) {
        int i, len;
        for (i = 0, len = octets.length; i < len; i++) {
          int dec0, dec1;
          if ((dec0 = (octets[i] >> 4)) < 0)
            dec0 &= 0x000F;
          dec1 = octets[i] & 0x000F;
          encodingResult.append(HEXBIN_ASCIIS.charAt(dec0));
          encodingResult.append(HEXBIN_ASCIIS.charAt(dec1));
        }
      }
    }
  }

  public static class Base64 {

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

    /**
     * NOTE: This method has to be in sync with the other decode method.
     */
    static byte[] decode(char[] ch, int start, int len, byte[] result, int[] returnedInt)
      throws SchemaValidatorException {
      int n_result = 0;
      if (len > 0) {
        final char[] enc = new char[4];
        int pos;
        for (pos = 0; pos < len;) {
          int nc;
          for (nc = 0; nc < 4 && pos < len; pos++) {
            final char c = ch[start + pos]; 
            if (isBase64Char(c))
              enc[nc++] = c;
            else if (!Character.isWhitespace(c)) {
              throw new SchemaValidatorException(
                  SchemaValidatorException.INVALID_BASE64_BINARY,
                  new String[] { new String(ch, start, len) }, null, EXISchema.NIL_NODE);
            }
          }
          if (nc == 4) {
            if (enc[0] == '=' || enc[1] == '=') { // invalid
              throw new SchemaValidatorException(
                  SchemaValidatorException.INVALID_BASE64_BINARY,
                  new String[] { new String(ch, start, len) }, null, EXISchema.NIL_NODE);
            }
            final byte b0, b1, b2, b3;
            b0 = m_octets[enc[0]];
            b1 = m_octets[enc[1]];
            if (n_result >= result.length) {
              byte[] _result = new byte[2 * result.length];
              System.arraycopy(result, 0, _result, 0, n_result);
              result = _result;
            }
            result[n_result++] = (byte)(b0 << 2 | b1 >> 4);
            if (enc[2] == '=') { // it is the end
              if (enc[3] != '=') {
                throw new SchemaValidatorException(
                    SchemaValidatorException.INVALID_BASE64_BINARY,
                    new String[] { new String(ch, start, len) }, null, EXISchema.NIL_NODE);
              }
              break;
            }
            b2 = m_octets[enc[2]];
            if (n_result >= result.length) {
              byte[] _result = new byte[2 * result.length];
              System.arraycopy(result, 0, _result, 0, n_result);
              result = _result;
            }
            result[n_result++] = (byte)(((b1 & 0x0F) << 4) | ((b2 >> 2) & 0x0F));
            if (enc[3] == '=') // it is the end
              break;
            b3 = m_octets[enc[3]];
            if (n_result >= result.length) {
              byte[] _result = new byte[2 * result.length];
              System.arraycopy(result, 0, _result, 0, n_result);
              result = _result;
            }
            result[n_result++] = (byte)(b2 << 6 | b3);
          }
          else if (nc > 0) { // not multiple of four
            throw new SchemaValidatorException(
                SchemaValidatorException.INVALID_BASE64_BINARY,
                new String[] { new String(ch, start, len) }, null, EXISchema.NIL_NODE);
          }
        }
        for (; pos < len; pos++) { // Check if there are any extra chars
          if (!Character.isWhitespace(ch[start + pos])) { 
            throw new SchemaValidatorException(
                SchemaValidatorException.INVALID_BASE64_BINARY,
                new String[] { new String(ch, start, len) }, null, EXISchema.NIL_NODE);
          }
        }
      }
      returnedInt[0] = n_result;
      return result;
    }
    
    /**
     * NOTE: This method has to be in sync with the other decode method.
     */
    public static byte[] decode(String norm)
        throws SchemaValidatorException {
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
                  throw new SchemaValidatorException(
                      SchemaValidatorException.INVALID_BASE64_BINARY,
                      new String[] { norm }, null, EXISchema.NIL_NODE);
                }
              }
              if (nc == 4) {
                if (enc[0] == '=' || enc[1] == '=') { // invalid
                  throw new SchemaValidatorException(
                      SchemaValidatorException.INVALID_BASE64_BINARY,
                      new String[] { norm }, null, EXISchema.NIL_NODE);
                }
                final byte b0, b1, b2, b3;
                b0 = m_octets[enc[0]];
                b1 = m_octets[enc[1]];
                baos.write(b0 << 2 | b1 >> 4);
                if (enc[2] == '=') { // it is the end
                  if (enc[3] != '=') {
                    throw new SchemaValidatorException(
                        SchemaValidatorException.INVALID_BASE64_BINARY,
                        new String[] { norm }, null, EXISchema.NIL_NODE);
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
                throw new SchemaValidatorException(
                    SchemaValidatorException.INVALID_BASE64_BINARY,
                    new String[] { norm }, null, EXISchema.NIL_NODE);
              }
            }
            for (; pos < len; pos++) { // Check if there are any extra chars
              if (!Character.isWhitespace(norm.charAt(pos))) {
                throw new SchemaValidatorException(
                    SchemaValidatorException.INVALID_BASE64_BINARY,
                    new String[] { norm }, null, EXISchema.NIL_NODE);
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

    public static void encode(byte[] octets, StringBuffer encodingResult) {
      if (octets != null && encodingResult != null) {
        int len;
        if ( (len = octets.length) > 0) {
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
            encodingResult.append(BASE64_ASCIIS.charAt(b0));
            encodingResult.append(BASE64_ASCIIS.charAt(b1));
            encodingResult.append(BASE64_ASCIIS.charAt(b2));
            encodingResult.append(BASE64_ASCIIS.charAt(b3));
            if (mod % 19 == 18)
              encodingResult.append('\n');
          }
        }
      }
    }
  }

}
