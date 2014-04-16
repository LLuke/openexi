package org.openexi.proc.io.compression;

import java.io.IOException;
import java.io.InputStream;

import org.openexi.proc.io.Scanner;
import org.openexi.proc.io.ValueScanner;

import org.openexi.proc.common.CharacterSequence;
import org.openexi.proc.events.EXITextProvider;

final class ScannerValueHolder extends ValuePlaceHolder implements EXITextProvider {
  
  private CharacterSequence m_text;
  
  ScannerValueHolder(String localName, String uri, int tp) {
    super(localName, uri, tp);
    m_text = null;
  }

  public CharacterSequence getCharacters() {
    assert m_text != null;
    return m_text;
  }

  void scanText(Scanner scanner, InputStream istream) throws IOException {
    final ValueScanner valueScanner = scanner.getValueScanner(m_tp); 
    m_text = valueScanner.scan(m_localName, m_uri, m_tp, istream); 
  }
  
}
