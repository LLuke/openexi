using System.IO;

using QName = Nagasena.Proc.Common.QName;
using ExiUriConst = Nagasena.Proc.Util.ExiUriConst;
using EXISchema = Nagasena.Schema.EXISchema;
using EXISchemaConst = Nagasena.Schema.EXISchemaConst;
using XSDateTime = Nagasena.Schema.XSDateTime;

namespace Nagasena.Proc.IO {

  internal sealed class GYearMonthValueScriber : DateTimeValueScriberBase {

    public static readonly GYearMonthValueScriber instance;
    static GYearMonthValueScriber() {
      instance = new GYearMonthValueScriber();
    }

    private GYearMonthValueScriber() : base(new QName("exi:gYearMonth", ExiUriConst.W3C_2009_EXI_URI)) {
    }

    public override short CodecID {
      get {
        return Scriber.CODEC_GYEARMONTH;
      }
    }

    public override int getBuiltinRCS(int simpleType, Scriber scriber) {
      return BuiltinRCS.RCS_ID_DATETIME;
    }

    ////////////////////////////////////////////////////////////

    public override bool process(string value, int tp, EXISchema schema, Scribble scribble, Scriber scriber) {
      if (!trimWhitespaces(value)) {
        return false;
      }
      int year, month;
      int tz = XSDateTime.FIELD_UNDEFINED;
      nextPosition = startPosition; // OK, Let's start parsing
      if (!parseYearField(value, scribble) || nextPosition == limitPosition) {
        return false;
      }
      year = scribble.intValue1;
      if (value[nextPosition++] != '-' || nextPosition == limitPosition) {
        return false;
      }
      if (!parseMonthField(value, scribble)) {
        return false;
      }
      month = scribble.intValue1;
      if (nextPosition != limitPosition) {
        if (!parseTimezoneField(value, scribble) || nextPosition != limitPosition) {
          return false;
        }
        tz = scribble.intValue1;
      }
      scribble.dateTime = new XSDateTime(year, month, XSDateTime.FIELD_UNDEFINED, XSDateTime.FIELD_UNDEFINED, 
        XSDateTime.FIELD_UNDEFINED, XSDateTime.FIELD_UNDEFINED, 
        XSDateTime.BIGINTEGER_MINUSONE, tz, EXISchemaConst.G_YEARMONTH_TYPE);
      return true;
    }

    ////////////////////////////////////////////////////////////

    internal override void scribeDateTimeValue(XSDateTime dateTime, Stream channelStream, Scriber scriber) {
      writeYear(dateTime.year, channelStream, scriber);
      writeMonthDay(dateTime.month, 0, channelStream, scriber);
      writeTimeZone(dateTime.timeZone, channelStream, scriber);
    }

  }

}