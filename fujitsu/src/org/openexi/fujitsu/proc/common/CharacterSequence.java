package org.openexi.fujitsu.proc.common;

public interface CharacterSequence {

  public char[] getCharacters();
  
  public int getStartIndex();
  
  public int length();

  public int getUCSCount();
  
  public int indexOf(char c);

  public String substring(int beginIndex, int endIndex);
  
  public String makeString();
  
}
