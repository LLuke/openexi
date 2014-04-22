using System.IO;

using QName = Nagasena.Proc.Common.QName;
using EXISchema = Nagasena.Schema.EXISchema;
using EXISchemaConst = Nagasena.Schema.EXISchemaConst;
using XSDateTime = Nagasena.Schema.XSDateTime;

namespace Nagasena.Proc.IO {

  internal sealed class GMonthDayValueScriber : DateTimeValueScriberBase {

    public static readonly GMonthDayValueScriber instance;
    static GMonthDayValueScriber() {
      instance = new GMonthDayValueScriber();
    }

    private GMonthDayValueScriber() : base(new QName("exi:gMonthDay", "http://www.w3.org/2009/exi")) {
    }

    public override short CodecID {
      get {
        return Scriber.CODEC_GMONTHDAY;
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
      int month, day;
      int tz = XSDateTime.FIELD_UNDEFINED;
      nextPosition = startPosition; // OK, Let's start parsing
      if (value[nextPosition++] != '-' || nextPosition == limitPosition || value[nextPosition++] != '-' || nextPosition == limitPosition) {
        return false;
      }
      if (!parseMonthField(value, scribble) || nextPosition == limitPosition) {
        return false;
      }
      month = scribble.intValue1;
      if (value[nextPosition++] != '-' || nextPosition == limitPosition) {
        return false;
      }
      if (!parseDayField(value, XSDateTime.FIELD_UNDEFINED, month, scribble)) {
        return false;
      }
      day = scribble.intValue1;
      if (nextPosition != limitPosition) {
        if (!parseTimezoneField(value, scribble) || nextPosition != limitPosition) {
          return false;
        }
        tz = scribble.intValue1;
      }
      scribble.dateTime = new XSDateTime(XSDateTime.FIELD_UNDEFINED, month, day, XSDateTime.FIELD_UNDEFINED,
        XSDateTime.FIELD_UNDEFINED, XSDateTime.FIELD_UNDEFINED, null, tz, EXISchemaConst.G_MONTHDAY_TYPE);
      return true;
    }

    ////////////////////////////////////////////////////////////

    internal override void scribeDateTimeValue(XSDateTime dateTime, Stream channelStream, Scriber scriber) {
      writeMonthDay(dateTime.month, dateTime.day, channelStream, scriber);
      writeTimeZone(dateTime.timeZone, channelStream, scriber);
    }

  }

}