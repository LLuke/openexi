package org.openexi.tryout;

import java.io.*;

import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

class XMLSniffer {

  private static final Boolean ENDIAN_BIG    = new Boolean(true);
  private static final Boolean ENDIAN_LITTLE = new Boolean(false);

  public static Result probeText(InputStream is) throws IOException {
    return doProbeText(new PushbackInputStream(is, 256));
  }

  private static Result doProbeText(PushbackInputStream stream)
    throws IOException {
    Result res = new Result();
    final byte[] b4 = new byte[4];
    int count;
    for (count = 0; count < 4; count++) {
      b4[count] = (byte) stream.read();
    }
    for (count = 3; count >= 0; count--) {
      stream.unread(b4[count]);
    }

    if (!getIanaEncoding(b4, res)) {
      if (!probeDeclEncoding(stream, res))
        res.setIANAEncoding("UTF-8");
    }
    return res;
  }

  public static Result probeXml(InputStream is, XMLReader xmlReader)
      throws IOException, SAXException {
    Result res = null;
    if (is != null) {
      PushbackInputStream stream = null;
      try {
        stream = new PushbackInputStream(is, 256);
        res = doProbeText(stream);

//        final byte[] b4 = new byte[4];
//        int count;
//        for (count = 0; count < 4; count++) {
//          b4[count] = (byte) stream.read();
//        }
//        for (count = 3; count >= 0; count--) {
//          stream.unread(b4[count]);
//        }
//
//        if (!getIanaEncoding(b4, res)) {
//          if (!probeDeclEncoding(stream, res))
//            res.setIANAEncoding("UTF-8");
//        }

        xmlReader.setContentHandler(new ContentSniffer());
        try {
          xmlReader.parse(new InputSource(stream));
        }
        catch (ContentInfo cInfo) {
          res.setNamespaceName(cInfo.getNamespaceName());
          res.setLocalName(cInfo.getLocalName());
        }
      }
      finally {
        if (stream != null) stream.close();
        is.close();
      }
    }
    return res;
  }

  private static class ContentInfo extends SAXException {

    private static final long serialVersionUID = 7331971173415995144L;
    
    private String m_namespaceName;
    private String m_localName;

    ContentInfo(String namespaceName, String localName) {
      super("");
      m_namespaceName = namespaceName;
      m_localName = localName;
    }
    String getNamespaceName() {
      return m_namespaceName;
    }
    String getLocalName() {
      return m_localName;
    }
  }

  private static class ContentSniffer extends DefaultHandler {


    public void startElement(String namespaceURI,
                             String localName,
                             String qualName,
                             Attributes atts) throws SAXException {
      String name = localName;

      if (name == null || name.length() == 0)
        name = qualName;

      if (namespaceURI != null && namespaceURI.length() == 0)
        namespaceURI = null;

      throw new ContentInfo(namespaceURI, name);
    }
  }

  public static class Result {
    private String  m_namespaceName = null;
    private String  m_localName     = null;
    private String  m_encIana       = null;
    private Boolean m_bigEndian     = null;

    private Result() {
      // so that no one else can instantiate it.
    }
    public String getNamespaceName() {
      return m_namespaceName;
    }
    public String getLocalName() {
      return m_localName;
    }
    public String getIANAEncoding() {
      return m_encIana;
    }
    public Boolean isBigEndian() {
      return m_bigEndian;
    }

    public void setNamespaceName(String namespaceName) {
      m_namespaceName = namespaceName;
    }
    public void setLocalName(String localName) {
      m_localName = localName;
    }
    void setIANAEncoding(String enc) {
      m_encIana = enc;
    }
    void setBigEndian(Boolean isBig) {
      m_bigEndian = isBig;
    }
  }

  /**
   * Check encoding name in xml declaration.
   * @return true if positively determined.
   */
  private static boolean probeDeclEncoding(PushbackInputStream is, Result res)
      throws IOException {

    boolean isPositive = false;

    ByteArrayOutputStream baos = new ByteArrayOutputStream();

    byte[] bt = new byte[8];
    bt[0] = (byte)is.read();
    bt[1] = (byte)is.read();
    baos.write(bt, 0, 2);

    int n;
    if (((int)bt[0]) == (int)'<' && ((int)bt[1]) == (int)'?') { // xml decl detected
      do {
        if ((n = is.read(bt, 0, 3)) < 3) {
          baos.write(bt, 0, n);
          break;
        }
        baos.write(bt, 0, 3);

        if ( ( (char) bt[0]) != 'x' || ( (char) bt[1]) != 'm' ||
            ( (char) bt[2]) != 'l')
          break;

        while (Character.isWhitespace( (char) (bt[0] = (byte) is.read()))) // skip whitespaces
          baos.write(bt, 0, 1);

        if ( (n = is.read(bt, 1, 6)) < 6) {
          baos.write(bt, 1, n);
          break;
        }
        baos.write(bt, 0, 7);

        if ( ( (char) bt[0]) != 'v' || ( (char) bt[1]) != 'e' ||
            ( (char) bt[2]) != 'r' || ( (char) bt[3]) != 's' ||
            ( (char) bt[4]) != 'i' || ( (char) bt[5]) != 'o' ||
            ( (char) bt[6]) != 'n')
          break;

        while (!Character.isWhitespace( (char) (bt[0] = (byte) is.read())))
          baos.write(bt, 0, 1);
        baos.write(bt, 0, 1);

        while (Character.isWhitespace( (char) (bt[0] = (byte) is.read())))
          baos.write(bt, 0, 1);

        if ( (n = is.read(bt, 1, 7)) < 7) {
          baos.write(bt, 1, n);
          break;
        }
        baos.write(bt, 0, 8);

        if ( ( (char) bt[0]) != 'e' || ( (char) bt[1]) != 'n' ||
            ( (char) bt[2]) != 'c' || ( (char) bt[3]) != 'o' ||
            ( (char) bt[4]) != 'd' || ( (char) bt[5]) != 'i' ||
            ( (char) bt[6]) != 'n' || ( (char) bt[7]) != 'g')
          break;

        while ( ( (char) (bt[0] = (byte) is.read())) != '"' &&
               ( (char) bt[0]) != '\'')
          baos.write(bt, 0, 1);
        baos.write(bt, 0, 1);

        StringBuffer buf = new StringBuffer();
        while ( ( (char) (bt[0] = (byte) is.read())) != '"' &&
               ( (char) bt[0]) != '\'') {
          baos.write(bt, 0, 1);
          buf.append( (char) bt[0]);
        }
        baos.write(bt, 0, 1);
        res.setIANAEncoding(buf.toString());
        isPositive = true;

      } while (false);
    }
    else {
      // There is no XML declaration.
    }

    bt = baos.toByteArray();
    is.unread(bt);

    return isPositive;
  }

  /**
   * Returns the IANA encoding name that is auto-detected from
   * the bytes specified, with the endian-ness of that encoding where appropriate.
   *
   * @param b4    The first four bytes of the input.
   * @param count The number of bytes actually read.
   * @return a 2-element array:  the first element, an IANA-encoding string,
   *  the second element a Boolean which is true iff the document is big endian, false
   *  if it's little-endian, and null if the distinction isn't relevant.
   */
  private static boolean getIanaEncoding(byte[] b4, Result res) {

      // UTF-16, with BOM
      int b0 = b4[0] & 0xFF;
      int b1 = b4[1] & 0xFF;
      if (b0 == 0xFE && b1 == 0xFF) {
          // UTF-16, big-endian
          res.setIANAEncoding("UTF-16BE");
          res.setBigEndian(ENDIAN_BIG);
          return true;
      }
      if (b0 == 0xFF && b1 == 0xFE) {
          // UTF-16, little-endian
          res.setIANAEncoding("UTF-16LE");
          res.setBigEndian(ENDIAN_LITTLE);
          return true;
      }

      // UTF-8 with a BOM
      int b2 = b4[2] & 0xFF;
      if (b0 == 0xEF && b1 == 0xBB && b2 == 0xBF) {
        res.setIANAEncoding("UTF-8");
        return true;
      }

      // other encodings
      int b3 = b4[3] & 0xFF;
      if (b0 == 0x00 && b1 == 0x00 && b2 == 0x00 && b3 == 0x3C) {
          // UCS-4, big endian (1234)
          res.setIANAEncoding("ISO-10646-UCS-4");
          res.setBigEndian(ENDIAN_BIG);
          return true;
      }
      if (b0 == 0x3C && b1 == 0x00 && b2 == 0x00 && b3 == 0x00) {
          // UCS-4, little endian (4321)
          // UCS-4, big endian (1234)
          res.setIANAEncoding("ISO-10646-UCS-4");
          res.setBigEndian(ENDIAN_LITTLE);
          return true;
      }
      if (b0 == 0x00 && b1 == 0x00 && b2 == 0x3C && b3 == 0x00 ||
          b0 == 0x00 && b1 == 0x3C && b2 == 0x00 && b3 == 0x00) {
          // UCS-4, unusual octet order (2143)
          res.setIANAEncoding("ISO-10646-UCS-4");
          return true;
      }
      if (b0 == 0x00 && b1 == 0x3C && b2 == 0x00 && b3 == 0x3F) {
          // UTF-16, big-endian, no BOM
          // or could turn out to be UCS-2...
          res.setIANAEncoding("UTF-16BE");
          res.setBigEndian(ENDIAN_BIG);
          return true;
      }
      if (b0 == 0x3C && b1 == 0x00 && b2 == 0x3F && b3 == 0x00) {
          // UTF-16, little-endian, no BOM
          // or could turn out to be UCS-2...
          res.setIANAEncoding("UTF-16LE");
          res.setBigEndian(ENDIAN_LITTLE);
          return true;
      }
      if (b0 == 0x4C && b1 == 0x6F && b2 == 0xA7 && b3 == 0x94) {
          // EBCDIC
          res.setIANAEncoding("CP037");
          return true;
      }

      // default encoding
      return false;
  }

}
