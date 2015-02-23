package com.sumerogi.proc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.net.URISyntaxException;

import com.sumerogi.proc.common.EventDescription;
import com.sumerogi.proc.io.Scanner;
import com.sumerogi.util.Utils;

public class JSONifier {
  
  private final ESONDecoder m_decoder;
  
  private int[] m_context;
  private int m_contextPosition;
  
  private String m_indentation;
  
  public JSONifier() {
    m_decoder = new ESONDecoder();
    m_context = new int[32];
    m_indentation = null;
  }
  
  public void setIndentation(String str) {
    m_indentation = str;
  }

  private void incrementContext() {
    m_context[m_contextPosition]++;
  }
  
  private void pushToContext() {
    incrementContext();
    if (++m_contextPosition == m_context.length) {
      final int[] context = new int[m_contextPosition << 1];
      System.arraycopy(m_context, 0, context, 0, m_contextPosition);
      m_context = context;
    }
    m_context[m_contextPosition] = 0;
  }

  private int popFromContext() {
    return m_context[m_contextPosition--];
  }
  
  private int peekContext() {
    return m_context[m_contextPosition];
  }

  private void doDecode(Scanner scanner, OutputStreamWriter writer) throws IOException {
    EventDescription event;
    while ((event = scanner.nextEvent()) != null) {
      String name;
      byte eventKind;
      switch (eventKind = event.getEventKind()) {
        case EventDescription.EVENT_START_DOCUMENT:
          m_context[m_contextPosition = 0] = 0;
          break;
        case EventDescription.EVENT_END_DOCUMENT:
          int n;
          n = popFromContext();
          assert n == 1 && m_contextPosition == -1;
          if (m_indentation != null)
            writer.write('\n');
          break;
        case EventDescription.EVENT_START_OBJECT:
        case EventDescription.EVENT_START_ARRAY:
          if (peekContext() > 0)
            writer.write(',');
          if (m_indentation != null)
            writeIndentation(writer);
          if ((name = event.getName()) != null) {
            writer.write("\"" + name + "\"");
            writer.write(':');
          }
          writer.write(eventKind == EventDescription.EVENT_START_OBJECT ? '{' : '[') ;
          pushToContext();
          break;
        case EventDescription.EVENT_END_OBJECT:
          popFromContext();
          if (m_indentation != null)
            writeIndentation(writer);
          writer.write('}');
          break;
        case EventDescription.EVENT_END_ARRAY:
          popFromContext();
          if (m_indentation != null)
            writeIndentation(writer);
          writer.write(']');
          break;
        case EventDescription.EVENT_NUMBER_VALUE:
        case EventDescription.EVENT_STRING_VALUE:
        case EventDescription.EVENT_BOOLEAN_VALUE:
        case EventDescription.EVENT_NULL:
          if (peekContext() > 0) {
            writer.write(',');
          }
          if (m_indentation != null)
            writeIndentation(writer);
          if ((name = event.getName()) != null) {
            writer.write("\"" + name + "\"");
            writer.write(':');
          }
          if (eventKind == EventDescription.EVENT_STRING_VALUE)
            writer.write("\"" + event.getCharacters().makeString() + "\"");
          else
            writer.write(event.getCharacters().makeString());
          incrementContext();
        default:
          break;
      }
    }
  }
  
  private void writeIndentation(OutputStreamWriter writer) throws IOException {
    if (m_contextPosition != 0 || m_context[m_contextPosition] != 0) {
      writer.write('\n');
      for (int i = 0; i < m_contextPosition; i++)
        writer.write(m_indentation);
    }
  }
  
  public void decode(InputStream inputStream, OutputStream outputStream) throws IOException {
    m_decoder.setInputStream(inputStream);
    
    OutputStreamWriter writer = new OutputStreamWriter(outputStream, "UTF-8");
    
    doDecode(m_decoder.processHeader(), writer);
    
    writer.flush();
  }
  
  public static void main(String args[]) throws IOException {
    OutputStream outputStream = System.out;
    do {
      if (args.length == 2) {
        outputStream = null;
        break;
      }
      else if (args.length == 1) {
        break;
      }
      else if (args.length > 2) {
        System.err.println("Too many arguments.");
      }
      else if (args.length < 1) {
        System.err.println("Too few arguments.");
      }
      printSynopsis();
      System.exit(1);
      return;
    }
    while (false);

    final URI baseURI = new File(System.getProperty("user.dir")).toURI();

    int pos = 0;

    URI esonUri;
    try {
      esonUri = Utils.resolveURI(args[pos++], baseURI);
    }
    catch (URISyntaxException use) {
      System.err.println("'" + args[pos] + "' is not a valid URI.");
      System.exit(1);
      return;
    }
    assert esonUri != null;
    
    if (outputStream == null) {
      URI outputUri;
      try {
        outputUri = Utils.resolveURI(args[pos++], baseURI);
      }
      catch (URISyntaxException use) {
        System.err.println("'" + args[pos] + "' is not a valid URI.");
        System.exit(1);
        return;
      }
      assert outputUri != null;
      outputStream = new FileOutputStream(outputUri.toURL().getFile());
    }
    assert outputStream != null && pos == args.length;

    // Create an instance of JSONifier.
    JSONifier jsonifier = new JSONifier();
    
    InputStream inputStream;
    try {
      // Open input ESON document. 
      inputStream = esonUri.toURL().openStream();
    }
    catch (IOException e) {
      outputStream.close();
      System.err.println(e.getMessage());
      System.exit(1);
      return;
    }

    try {
      // Invoke JSONifier to decode ESON into JSON.
      jsonifier.decode(inputStream, outputStream);
    }
    finally {
      inputStream.close();
      outputStream.close();
    }
  }

  private static void printSynopsis() {
    System.err.println("USAGE: " + JSONifier.class.getName() +
        " ESON_File [Output_File]");
  }
  
}
