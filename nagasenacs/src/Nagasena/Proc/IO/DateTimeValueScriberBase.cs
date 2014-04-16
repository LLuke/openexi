using System.Diagnostics;
using System.Numerics;
using System.IO;

using QName = Nagasena.Proc.Common.QName;
using XSDateTime = Nagasena.Schema.XSDateTime;

namespace Nagasena.Proc.IO {

  internal abstract class DateTimeValueScriberBase : ValueScriberBase {

    private static readonly BigInteger BIGINTEGER_TEN = 10;

    protected internal int nextPosition;

    protected internal DateTimeValueScriberBase(QName name) : base(name) {
    }

    public override sealed object toValue(string value, Scribble scribble, Scriber scriber) {
      return scribble.dateTime;
    }

    public override sealed void doScribe(object value, int localName, int uri, int tp, Stream channelStream, Scriber scriber) {
      scribeDateTimeValue((XSDateTime)value, channelStream, scriber);
    }

    public override sealed void scribe(string value, Scribble scribble, int localName, int uri, int tp, Stream channelStream, Scriber scriber) {
      scribeDateTimeValue(scribble.dateTime, channelStream, scriber);
    }

    internal abstract void scribeDateTimeValue(XSDateTime dateTime, Stream channelStream, Scriber scriber);

    ////////////////////////////////////////////////////////////

    public static void canonicalizeValue(Scribble scribble) {
      scribble.dateTime.normalize();
    }

    ////////////////////////////////////////////////////////////

    protected internal void writeYear(int year, Stream ostream, Scriber scriber) {
      if ((year -= 2000) < 0) {
        scriber.writeBoolean(true, ostream);
        year = -1 - year;
      }
      else {
        scriber.writeBoolean(false, ostream);
      }
      scriber.writeUnsignedInteger32(year, ostream);
    }

    protected internal void writeMonthDay(int month, int day, Stream ostream, Scriber scriber) {
      int monthDay = month * 32 + day;
      scriber.writeNBitUnsigned(monthDay, 9, ostream);
    }

    protected internal void writeTime(int hour, int minute, int second, BigInteger reverseFractionalSecond, Stream ostream, Scriber scriber) {
      int time = (hour * 64 + minute) * 64 + second;
      scriber.writeNBitUnsigned(time, 17, ostream);
      if (reverseFractionalSecond != null) {
        scriber.writeBoolean(true, ostream);
        Debug.Assert(reverseFractionalSecond.Sign == 1);
        scriber.writeUnsignedInteger(reverseFractionalSecond, ostream);
      }
      else {
        scriber.writeBoolean(false, ostream);
      }
    }

    protected internal void writeTimeZone(int timeZone, Stream ostream, Scriber scriber) {
      if (timeZone != XSDateTime.FIELD_UNDEFINED) {
        scriber.writeBoolean(true, ostream);
        int hours = timeZone / 60;
        int minutes = timeZone % 60;
        scriber.writeNBitUnsigned((hours + 14) * 64 + minutes, 11, ostream);
      }
      else {
        scriber.writeBoolean(false, ostream);
      }
    }

    ////////////////////////////////////////////////////////////

    protected internal bool parseYearField(string value, Scribble scribble) {
      bool isNegative;
      if (isNegative = value[nextPosition] == '-') {
        ++nextPosition;
      }
      int year, n_digits;
      for (year = n_digits = 0; nextPosition < limitPosition; n_digits++, nextPosition++) {
        char c = value[nextPosition];
        if (c < '0' || '9' < c) {
          break;
        }
        year = 10 * year + (c - '0');
      }
      if (n_digits < 4 || year == 0) {
        return false;
      }
      scribble.intValue1 = isNegative ? 0 - year : year;
      return true;
    }

    protected internal bool parseMonthField(string value, Scribble scribble) {
      int month, n_digits;
      for (month = n_digits = 0; nextPosition < limitPosition; n_digits++, nextPosition++) {
        char c = value[nextPosition];
        if (c < '0' || '9' < c) {
          break;
        }
        month = 10 * month + (c - '0');
      }
      if (n_digits != 2 || month == 0 || 12 < month) {
        return false;
      }
      scribble.intValue1 = month;
      return true;
    }

    protected internal bool parseDayField(string value, int year, int month, Scribble scribble) {
      int day, n_digits;
      for (day = n_digits = 0; nextPosition < limitPosition; n_digits++, nextPosition++) {
        char c = value[nextPosition];
        if (c < '0' || '9' < c) {
          break;
        }
        day = 10 * day + (c - '0');
      }
      if (n_digits != 2 || day == 0 || XSDateTime.getLastDayOfMonth(year, month) < day) {
        return false;
      }
      scribble.intValue1 = day;
      return true;
    }

    protected internal bool parseHourField(string value, Scribble scribble) {
      int hour, n_digits;
      for (hour = n_digits = 0; nextPosition < limitPosition; n_digits++, nextPosition++) {
        char c = value[nextPosition];
        if (c < '0' || '9' < c) {
          break;
        }
        hour = 10 * hour + (c - '0');
      }
      if (n_digits != 2 || 24 < hour) {
        return false;
      }
      scribble.intValue1 = hour;
      return true;
    }

    protected internal bool parseMinuteField(string value, Scribble scribble) {
      int minute, n_digits;
      for (minute = n_digits = 0; nextPosition < limitPosition; n_digits++, nextPosition++) {
        char c = value[nextPosition];
        if (c < '0' || '9' < c) {
          break;
        }
        minute = 10 * minute + (c - '0');
      }
      if (n_digits != 2 || 59 < minute) {
        return false;
      }
      scribble.intValue1 = minute;
      return true;
    }

    protected internal bool parseSecondField(string value, Scribble scribble) {
      int second, n_digits;
      for (second = n_digits = 0; nextPosition < limitPosition; n_digits++, nextPosition++) {
        char c = value[nextPosition];
        if (c < '0' || '9' < c) {
          break;
        }
        second = 10 * second + (c - '0');
      }
      if (n_digits != 2) {
        return false;
      }
      scribble.intValue1 = second;
      return true;
    }

    protected internal BigInteger parseFractionalSecondField(string value) {
      BigInteger magnitude = BigInteger.One;
      BigInteger reverseFractionalSecond = BigInteger.Zero;
      int n_digits;
      for (n_digits = 0; nextPosition < limitPosition; n_digits++, nextPosition++, magnitude = BIGINTEGER_TEN * magnitude) {
        char c = value[nextPosition];
        if (c < '0' || '9' < c) {
          break;
        }
        reverseFractionalSecond = reverseFractionalSecond + (magnitude * new BigInteger(c - '0'));
      }
      if (n_digits == 0) {
        return BigInteger.MinusOne;
      }
      return reverseFractionalSecond;
    }

    protected internal bool parseTimezoneField(string value, Scribble scribble) {
      int tz = 0;
      bool isNegative = false;
      char c = value[nextPosition++];
      if (c == 'Z') {
        tz = 0;
      }
      else {
        if (c == '-') {
          isNegative = true;
        }
        else if (c != '+') {
          return false;
        }
        if (nextPosition == limitPosition) {
          return false;
        }
        if (!parseHourField(value, scribble) || nextPosition == limitPosition) {
          return false;
        }
        int hour = scribble.intValue1;
        if (value[nextPosition++] != ':' || nextPosition == limitPosition) {
          return false;
        }
        if (!parseMinuteField(value, scribble)) {
          return false;
        }
        int minute = scribble.intValue1;
        tz = 60 * hour + minute;
      }
      if (isNegative) {
        tz = 0 - tz;
      }
      scribble.intValue1 = tz;
      return true;
    }

  }

}