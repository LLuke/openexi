using System.IO;

using QName = Nagasena.Proc.Common.QName;
using EXISchema = Nagasena.Schema.EXISchema;
using EXISchemaConst = Nagasena.Schema.EXISchemaConst;
using XSDateTime = Nagasena.Schema.XSDateTime;

namespace Nagasena.Proc.IO {

  internal sealed class DateValueScriber : DateTimeValueScriberBase {

    public static readonly DateValueScriber instance;
    static DateValueScriber() {
      instance = new DateValueScriber();
    }

    private DateValueScriber() : base(new QName("exi:date", "http://www.w3.org/2009/exi")) {
    }

    public override short CodecID {
      get {
        return Scriber.CODEC_DATE;
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
      int year, month, day;
      int tz = XSDateTime.FIELD_UNDEFINED;
      nextPosition = startPosition; // OK, Let's start parsing
      if (!parseYearField(value, scribble) || nextPosition == limitPosition) {
        return false;
      }
      year = scribble.intValue1;
      if (value[nextPosition++] != '-' || nextPosition == limitPosition) {
        return false;
      }
      if (!parseMonthField(value, scribble) || nextPosition == limitPosition) {
        return false;
      }
      month = scribble.intValue1;
      if (value[nextPosition++] != '-' || nextPosition == limitPosition) {
        return false;
      }
      if (!parseDayField(value, year, month, scribble)) {
        return false;
      }
      day = scribble.intValue1;
      if (nextPosition != limitPosition) {
        if (!parseTimezoneField(value, scribble) || nextPosition != limitPosition) {
          return false;
        }
        tz = scribble.intValue1;
      }
      scribble.dateTime = new XSDateTime(year, month, day, XSDateTime.FIELD_UNDEFINED,
        XSDateTime.FIELD_UNDEFINED, XSDateTime.FIELD_UNDEFINED, 
        XSDateTime.BIGINTEGER_MINUSONE, tz, EXISchemaConst.DATE_TYPE);
      return true;
    }

    ////////////////////////////////////////////////////////////

    internal override void scribeDateTimeValue(XSDateTime dateTime, Stream channelStream, Scriber scriber) {
      writeYear(dateTime.year, channelStream, scriber);
      writeMonthDay(dateTime.month, dateTime.day, channelStream, scriber);
      writeTimeZone(dateTime.timeZone, channelStream, scriber);
    }

  }

}