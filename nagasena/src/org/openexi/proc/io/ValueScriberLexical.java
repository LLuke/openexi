package org.openexi.proc.io;

import java.io.IOException;
import java.io.OutputStream;

import org.openexi.proc.common.QName;
import org.openexi.proc.grammars.Apparatus;
import org.openexi.schema.Characters;
import org.openexi.schema.EXISchema;

public final class ValueScriberLexical extends ValueScriber {
  
  private final ValueScriber m_baseValueScriber;
  private final StringValueScriber m_stringValueScriber;
  
  public ValueScriberLexical(ValueScriber valueScriber, StringValueScriber stringValueScriber) {
    m_baseValueScriber = valueScriber;
    m_stringValueScriber = stringValueScriber;
  }

  @Override
  public boolean process(String value, int tp, EXISchema schema, Scribble scribble, Scriber scriber) {
    return true;
  }

  @Override
  public void scribe(String value, Scribble scribble, int localName, int uri, int tp, Scriber scriber) throws IOException {
    scribe(value, scribble, localName, uri, tp, (OutputStream)null, scriber);
  }

  @Override
  public void scribe(String value, Scribble scribble, int localName, int uri, int tp, OutputStream channelStream, Scriber scriber) throws IOException {
    final int length = value.length();
    CharacterBuffer characterBuffer = scriber.ensureCharacters(length);
    Characters characterSequence = characterBuffer.addString(value, length);
    m_stringValueScriber.scribeStringValue(characterSequence, localName, uri, m_baseValueScriber.getBuiltinRCS(tp, scriber), channelStream, scriber);
  }

  @Override
  public Object toValue(String value, Scribble scribble, Scriber scriber) {
    return value;
  }

  @Override
  public void doScribe(Object value, int localName, int uri, int tp, OutputStream channelStream, Scriber scriber) throws IOException {
    scribe((String)value, (Scribble)null, localName, uri, tp, channelStream, scriber);
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
    return Apparatus.CODEC_LEXICAL;
  }

  @Override
  public int getBuiltinRCS(int simpleType, Scriber scriber) {
    return m_baseValueScriber.getBuiltinRCS(simpleType, scriber);
  }

}
