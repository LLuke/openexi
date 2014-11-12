package com.sumerogi.proc.io;

import com.sumerogi.proc.common.AlignmentType;

public final class ScriberFactory {
  
  public static Scriber createScriber(AlignmentType alignmentType) {
    switch (alignmentType) {
      case bitPacked:
        return new BitPackedScriber();
      case byteAligned:
        return new ByteAlignedScriber();        
//      case preCompress:
//        assert false;
//        break;
//      case compress:
//        assert false;
//        break;
    }
    return null;
  }

}
