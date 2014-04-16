package org.openexi.proc.io;

import java.io.IOException;
import java.io.OutputStream;

import org.openexi.proc.common.CharacterSequence;
import org.openexi.proc.common.QName;
import org.openexi.schema.EXISchema;

public final class ValueScriberLexical extends ValueScriber {
  
  private final ValueScriber m_baseValueScriber;
  private final StringValueScriber m_stringValueScriber;
  
  public ValueScriberLexical(ValueScriber valueScriber, StringValueScriber stringValueScriber) {
    super(valueScriber.m_scriber);
    m_baseValueScriber = valueScriber;
    m_stringValueScriber = stringValueScriber;
  }

  @Override
  public boolean process(String value, int tp, EXISchema schema, Scribble scribble) {
    return true;
  }

  @Override
  public void scribe(String value, Scribble scribble, String localName, String uri, int tp) throws IOException {
    scribe(value, scribble, localName, uri, tp, (OutputStream)null);
  }

  @Override
  public void scribe(String value, Scribble scribble, String localName, String uri, int tp, OutputStream channelStream) throws IOException {
    final int length = value.length();
    CharacterBuffer characterBuffer = m_scriber.ensureCharacters(length);
    CharacterSequence characterSequence = characterBuffer.addString(value, length);
    m_stringValueScriber.scribeStringValue(characterSequence, localName, uri, m_baseValueScriber.getBuiltinRCS(tp), channelStream);
  }

  @Override
  public Object toValue(String value, Scribble scribble) {
    return value;
  }

  @Override
  public void doScribe(Object value, String localName, String uri, int tp,  OutputStream channelStream) throws IOException {
    scribe((String)value, (Scribble)null, localName, uri, tp, channelStream);
  }

  @Override
  public final QName getName() {
    // DTRM must not take effect when the value of the Preserve.lexicalValues 
    // fidelity option is true. 
    assert false;
    return (QName)null;
  }

  @Override
  public short getCodecID() {
    assert false; // assuming this function is only used by DTRM 
    return m_baseValueScriber.getCodecID();
  }

  @Override
  public int getBuiltinRCS(int simpleType) {
    return m_baseValueScriber.getBuiltinRCS(simpleType);
  }

}
