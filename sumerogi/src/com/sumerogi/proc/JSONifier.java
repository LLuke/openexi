package com.sumerogi.proc;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.sumerogi.proc.common.EventDescription;
import com.sumerogi.proc.grammars.GrammarCache;
import com.sumerogi.proc.io.Scanner;

public class JSONifier {
  
  private final ESONDecoder m_decoder;
  
  private OutputStream m_outputStream;
  
  public JSONifier() {
    GrammarCache grammarCache = new GrammarCache();
    m_decoder = new ESONDecoder();
    m_decoder.setGrammarCache(grammarCache);
    m_outputStream = null;
  }

  public final void setOutputStream(OutputStream ostream) {
    m_outputStream = ostream;
  }
  
  public void decode(InputStream inputStream) throws IOException {
    m_decoder.setInputStream(inputStream);
    
    final Scanner scanner = m_decoder.processHeader();
    EventDescription event;
    while ((event = scanner.nextEvent()) != null) {
      switch (event.getEventKind()) {
        case EventDescription.EVENT_START_DOCUMENT:
          break;
        case EventDescription.EVENT_END_DOCUMENT:
          break;
          
          
        default:
          break;
      }
      
    }
  }

}
