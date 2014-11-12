package com.sumerogi.proc.io;

import com.sumerogi.proc.common.AlignmentType;

public final class ScannerFactory {

  public static Scanner createScanner(AlignmentType alignmentType) {
    final Scanner scanner;
    switch (alignmentType) {
      case bitPacked:
        scanner = new BitPackedScanner();
        break;
      case byteAligned:
        scanner = new ByteAlignedScanner();
        break;
//      case preCompress:
//        assert false;
//        scanner = null;
//        break;
//      case compress:
//        assert false;
//        scanner = null;
//        break;
      default:
        return null;
    }
    return scanner;
  }
  
}
