package com.sumerogi.proc;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

public class Utilities {

  public static String readTextResource(String resourceLocation, Object obj) throws IOException {
    InputStream inputStream = obj.getClass().getResource(resourceLocation).openStream();
    Reader reader = new InputStreamReader(inputStream, "UTF-8");
    StringBuilder stringBuilder = new StringBuilder();
    char[] charBuf = new char[32];
    int len;
    while ((len = reader.read(charBuf)) >= 0) {
      stringBuilder.append(charBuf, 0, len);
    }
    inputStream.close();
    return stringBuilder.toString();
  }
  
}

