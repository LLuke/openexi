﻿using System.Diagnostics;
using System.Text;

namespace Nagasena.Schema {

  public sealed class HexBin {

    private const string HEXBIN_ASCIIS = "0123456789ABCDEF";

    public static void encode(byte[] octets, int len, StringBuilder encodingResult) {
      if (octets != null && encodingResult != null) {
        for (int i = 0; i < len; i++) {
          int dec0, dec1;
          if ((dec0 = (octets[i] >> 4)) < 0) {
            dec0 &= 0x000F;
          }
          dec1 = octets[i] & 0x000F;
          encodingResult.Append(HEXBIN_ASCIIS[dec0]);
          encodingResult.Append(HEXBIN_ASCIIS[dec1]);
        }
      }
    }

    public static int decode(string norm, byte[] octets) {
      int pos, len, dec, opos;
      for (pos = opos = 0, len = norm.Length, dec = 0; pos < len; dec = 0) {
        int nc;
        for (nc = 0; nc < 2 && pos < len; pos++) {
          char c = norm[pos];
          if (char.IsWhiteSpace(c)) {
            // Permit whitespaces for now.
            continue;
          }
          if ('\u0060' < c) { // 'a' <= c
            if ('\u0066' < c) { // 'f' < c
              return -1;
            }
            else { // between 'a' and 'f'
              dec |= (10 + (c - 'a')) << (4 * (1 - nc++));
            }
          }
          else if (c < '\u003a') { // c <= '9'
            if (c < '\u0030') { // c < '0'
              return -1;
            }
            else { // between '0' and '9'
              dec |= (c - '0') << (4 * (1 - nc++));
            }
          }
          else if ('\u0040' < c && c < '\u0047') { // between 'A' and 'F'
            dec |= (10 + (c - 'A')) << (4 * (1 - nc++));
          }
          else { // off the range.
            return -1;
          }
        }
        if (nc == 1) {
          return -1;
        }
        if (nc != 0) {
          octets[opos++] = (byte)dec;
        }
        else { // i.e. nc == 0
          Debug.Assert(pos == len);
          return opos;
        }
      }
      return opos;
    }

  }

}