namespace Nagasena.Proc.Grammars {

  public class ApparatusUtil {

    /// <summary>
    /// Returns the codec ID effective for a type. 
    /// </summary>
    public static short getCodecID(Apparatus apparatus, int typeSerial) {
      return apparatus.m_codecTable[typeSerial];
    }

  }

}