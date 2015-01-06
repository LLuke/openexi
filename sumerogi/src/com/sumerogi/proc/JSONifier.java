package com.sumerogi.proc;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Stack;

import com.sumerogi.proc.common.EventDescription;
import com.sumerogi.proc.io.Scanner;

public class JSONifier {
  
  private final ESONDecoder m_decoder;
  
  private OutputStream m_outputStream;
  
  public JSONifier() {
    m_decoder = new ESONDecoder();
    m_outputStream = null;
  }

  public final void setOutputStream(OutputStream ostream) {
    m_outputStream = ostream;
  }
  
  private void incrementContext(Stack<Integer> context) {
    int n = context.pop().intValue();
    context.push(++n);
  }
  
//  private void decrementContext(Stack<Integer> context) {
//    int n = context.pop().intValue();
//    context.push(--n);
//  }
  
  private void pushToContext(Stack<Integer> context) {
    incrementContext(context);
    context.push(0);
  }

  private int popFromContext(Stack<Integer> context) {
    return context.pop().intValue();
  }
  
  private int peekContext(Stack<Integer> context) {
    return context.peek().intValue();
  }

  private void doDecode(Scanner scanner, OutputStreamWriter writer, Stack<Integer> context) throws IOException {
    EventDescription event;
    while ((event = scanner.nextEvent()) != null) {
      String name;
      byte eventKind;
      switch (eventKind = event.getEventKind()) {
        case EventDescription.EVENT_START_DOCUMENT:
          pushToContext(context);
          break;
        case EventDescription.EVENT_END_DOCUMENT:
          int n;
          n = popFromContext(context);
          assert n == 1;
          break;
        case EventDescription.EVENT_START_OBJECT:
        case EventDescription.EVENT_START_ARRAY:
          if (peekContext(context) > 0)
            writer.write(',');
          if ((name = event.getName()) != null) {
            writer.write("\"" + name + "\"");
            writer.write(':');
          }
          writer.write(eventKind == EventDescription.EVENT_START_OBJECT ? '{' : '[') ;
          pushToContext(context);
          break;
        case EventDescription.EVENT_END_OBJECT:
          writer.write('}');
          popFromContext(context);
          break;
        case EventDescription.EVENT_END_ARRAY:
          writer.write(']');
          popFromContext(context);
          break;
        case EventDescription.EVENT_NUMBER_VALUE:
        case EventDescription.EVENT_STRING_VALUE:
        case EventDescription.EVENT_BOOLEAN_VALUE:
        case EventDescription.EVENT_NULL:
          if (peekContext(context) > 0) {
            writer.write(',');
          }
          if ((name = event.getName()) != null) {
            writer.write("\"" + name + "\"");
            writer.write(':');
          }
          if (eventKind == EventDescription.EVENT_STRING_VALUE)
            writer.write("\"" + event.getCharacters().makeString() + "\"");
          else
            writer.write(event.getCharacters().makeString());
          incrementContext(context);
        default:
          break;
      }
    }
  }
  
  public void decode(InputStream inputStream) throws IOException {
    m_decoder.setInputStream(inputStream);
    
    OutputStreamWriter writer = new OutputStreamWriter(m_outputStream, "UTF-8");
    
    Stack<Integer> context = new Stack<Integer>();
    context.push(0);
    
    doDecode(m_decoder.processHeader(), writer, context);
    
    writer.flush();
    
  }

}
