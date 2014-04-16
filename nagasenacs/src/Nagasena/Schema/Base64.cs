using System;
using System.IO;
using System.Diagnostics;

namespace Nagasena.Schema {

  public sealed class Base64 {

    private static readonly char[] BASE64_ASCIIS;
    private static readonly byte[] BASE64CHARS = new byte[8192];
    static Base64() {
      string base64Asciis = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=";
      // Base64 asciis
      for (int i = 0, len = base64Asciis.Length; i < len; i++) {
        char c = base64Asciis[i];
        BASE64CHARS[c / 8] |= (byte)(1 << (7 - c % 8));
      }
      BASE64_ASCIIS = new char[base64Asciis.Length];
      for (int i = 0, len = base64Asciis.Length; i < len; i++) {
        BASE64_ASCIIS[i] = base64Asciis[i];
      }
      m_octets = new byte[256];

      for (int i = 'Z'; i >= 'A'; i--) {
        m_octets[i] = (byte)(i - 'A');
      }
      for (int i = 'z'; i >= 'a'; i--) {
        m_octets[i] = (byte)(i - 'a' + 26);
      }
      for (int i = '9'; i >= '0'; i--) {
        m_octets[i] = (byte)(i - '0' + 52);
      }

      m_octets['+'] = 62;
      m_octets['/'] = 63;
    }

    private static readonly byte[] m_octets; // Base64 ASCII -> byte (6 bits)


    private Base64() {
    }

    // REVISIT: abandon this method.
    public static byte[] decode(string norm) {
      byte[] octets = null;
      if (norm != null) {
        MemoryStream baos = new MemoryStream();
        try {
          int len;
          if ((len = norm.Length) > 0) {
            char[] enc = new char[4];
            int pos;
            for (pos = 0; pos < len;) {
              int nc;
              for (nc = 0; nc < 4 && pos < len; pos++) {
                char c = norm[pos];
                if (isBase64Char(c)) {
                  enc[nc++] = c;
                }
                else if (!char.IsWhiteSpace(c)) {
                  return null;
                }
              }
              if (nc == 4) {
                if (enc[0] == '=' || enc[1] == '=') { // invalid
                  return null;
                }
                byte b0, b1, b2, b3;
                b0 = m_octets[enc[0]];
                b1 = m_octets[enc[1]];
                baos.WriteByte((byte)(b0 << 2 | b1 >> 4));
                if (enc[2] == '=') { // it is the end
                  if (enc[3] != '=') {
                    return null;
                  }
                  break;
                }
                b2 = m_octets[enc[2]];
                baos.WriteByte((byte)(((b1 & 0x0F) << 4) | ((b2 >> 2) & 0x0F)));
                if (enc[3] == '=') { // it is the end
                  break;
                }
                b3 = m_octets[enc[3]];
                baos.WriteByte((byte)(b2 << 6 | b3));
              }
              else if (nc > 0) { // not multiple of four
                return null;
              }
            }
            for (; pos < len; pos++) { // Check if there are any extra chars
              if (!char.IsWhiteSpace(norm[pos])) {
                return null;
              }
            }
          }
        }
        finally {
          try {
            baos.Close();
          }
          catch (IOException ioe) {
            Console.WriteLine(ioe.ToString());
            Console.Write(ioe.StackTrace);
          }
        }
        octets = baos.ToArray();
      }
      return octets;
    }

  
    public static int decode(string value, byte[] octets) {
      int len = value.Length;
      int opos = 0;
      char enc0, enc1, enc2, enc3;
      int pos;
      for (pos = 0; pos != len;) {
        do {
          switch (enc0 = value[pos]) {
            case '\t':
            case '\n':
            case '\r':
            case ' ':
              ++pos;
              goto posLoopContinue;
            default:
              goto skipwsBreak1;
          }
        } while (pos != len);
        skipwsBreak1:
        if (pos++ == len || !isBase64Char(enc0)) {
          return -1;
        }
        do {
          switch (enc1 = value[pos]) {
            case '\t':
            case '\n':
            case '\r':
            case ' ':
              ++pos;
              continue;
            default:
              goto skipwsBreak2;
          }
        } while (pos != len);
        skipwsBreak2:
        if (pos++ == len || !isBase64Char(enc1)) {
          return -1;
        }
        if (enc0 == '=' || enc1 == '=') { // invalid
          return -1;
        }
        do {
          switch (enc2 = value[pos]) {
            case '\t':
            case '\n':
            case '\r':
            case ' ':
              ++pos;
              continue;
            default:
              goto skipwsBreak3;
          }
        } while (pos != len);
        skipwsBreak3:
        if (pos++ == len || !isBase64Char(enc2)) {
          return -1;
        }
        do {
          switch (enc3 = value[pos]) {
            case '\t':
            case '\n':
            case '\r':
            case ' ':
              ++pos;
              continue;
            default:
              goto skipwsBreak4;
          }
        } while (pos != len);
        skipwsBreak4:
        if (!isBase64Char(enc3)) {
          return -1;
        }
        ++pos;
        byte b0, b1, b2, b3;
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
        if (enc3 == '=') { // it is the end
          break;
        }
        b3 = m_octets[enc3];
        octets[opos++] = (byte)(b2 << 6 | b3);
        posLoopContinue:;
      }
      while (pos != len) {
        switch (value[pos]) {
          case '\t':
          case '\n':
          case '\r':
          case ' ':
            ++pos;
            continue;
          default:
            goto skipwsBreak5;
        }
      }
      skipwsBreak5:
      return pos != len ? - 1 : opos;
    }

    private static bool isBase64Char(char c) {
      return (BASE64CHARS[c / 8] & (1 << (7 - c % 8))) != 0;
    }

    public static int calculateTextMaxLength(int octetLength) {
      int maxChars = (octetLength / 3) << 2;
      if (octetLength % 3 != 0) {
        maxChars += 4;
      }
      maxChars += maxChars / 76;
      return maxChars;
    }

    public static int encode(byte[] octets, int offset, int len, char[] encodingResult, int startIndex) {
      int limit = offset + len;
      int opos = startIndex;
      int pos, mod;
      for (pos = offset, mod = 0; pos < limit; mod++) {
        int n, st;
        for (n = 0, st = pos; n < 3 && pos < limit; pos++, n++) {
          ;
        }
        Debug.Assert(n == 1 || n == 2 || n == 3);
        byte b0, b1;
        byte b2 = 64, b3 = 64;
        if ((b0 = (byte)(octets[st] >> 2)) < 0) {
          b0 = (byte)(b0 ^ 0xC0);
        }
        if (n > 1) {
          if ((b1 = (byte)(octets[st + 1] >> 4)) < 0) {
            b1 = (byte)(b1 ^ 0xF0);
          }
          b1 = (byte)((octets[st] & 0x03) << 4 | b1);
          if (n > 2) { // n == 3
            if ((b2 = (byte)(octets[st + 2] >> 6)) < 0) {
              b2 = (byte)(b2 ^ 0xFC);
            }
            b2 = (byte)((octets[st + 1] & 0x0F) << 2 | b2);
            b3 = (byte)(octets[st + 2] & 0x3F);
          }
          else { // n == 2
            b2 = (byte)((octets[st + 1] & 0x0F) << 2);
          }
        }
        else { // n == 1
          b1 = (byte)((octets[st] & 0x03) << 4);
        }
        encodingResult[opos++] = BASE64_ASCIIS[b0];
        encodingResult[opos++] = BASE64_ASCIIS[b1];
        encodingResult[opos++] = BASE64_ASCIIS[b2];
        encodingResult[opos++] = BASE64_ASCIIS[b3];
        if (mod % 19 == 18) {
          encodingResult[opos++] = '\n';
        }
      }
      return opos - startIndex;
    }

  }

}