using System;
using System.Diagnostics;
using System.IO;

using SharpZipBaseException = ICSharpCode.SharpZipLib.SharpZipBaseException;
using ICSharpCode.SharpZipLib.Zip.Compression;

namespace Nagasena.Proc.IO.Compression {

  internal class EXIInflaterInputStream : Stream {

    private readonly Stream m_inputStream;

    private readonly Inflater m_inflater;

    private readonly byte[] outputBuffer;
    private int outputOffset;
    private int outputLimit;

    private readonly byte[] inputBuffer;
    private int inputLength;
    private bool hasResidue;

    public override bool CanRead {
      get {
        return true;
      }
    }
    public override bool CanSeek {
      get {
        return false;
      }
    }
    public override bool CanWrite {
      get {
        return false;
      }
    }
    public override long Length {
      get {
        throw new NotSupportedException();
      }
    }
    public override long Position {
      get {
        throw new NotSupportedException();
      }
      set {
        throw new NotSupportedException();
      }
    }

    public EXIInflaterInputStream(Stream inputStream, Inflater inflater, int bufSize) {
      m_inputStream = inputStream;
      m_inflater = inflater;
      inputBuffer = new byte[bufSize];
      inputLength = 0;
      hasResidue = false;
      outputBuffer = new byte[bufSize];
      outputOffset = outputLimit = 0;
    }

    public override int ReadByte() {
      while (outputOffset == outputLimit && !m_inflater.IsFinished) {
        outputOffset = 0;
        try {
          if (hasResidue) {
            hasResidue = false;
            m_inflater.SetInput(inputBuffer, 0, inputLength);
          }
          if ((outputLimit = m_inflater.Inflate(outputBuffer)) == 0) {
            if ((inputLength = m_inputStream.Read(inputBuffer, 0, inputBuffer.Length)) == 0) {
              inputLength = 0;
              return -1;
            }
            m_inflater.SetInput(inputBuffer, 0, inputLength);
          }
        }
        catch (SharpZipBaseException dfe) {
          throw new IOException(dfe.Message);
        }
      }
      return outputOffset != outputLimit ? outputBuffer[outputOffset++] : -1;
    }

    internal virtual void resetInflator() {
      for (int bt = 0; !m_inflater.IsFinished && bt != -1; ) {
        bt = ReadByte();
      }
      int remain = m_inflater.RemainingInput;
      if (hasResidue = (remain != 0)) {
        int inputOffset;
        inputOffset = inputLength - remain;
        inputLength = remain;
        int i, pos;
        for (i = 0, pos = inputOffset; i < inputLength; i++, pos++) {
          inputBuffer[i] = inputBuffer[pos];
        }
      }
      else
        inputLength = 0;
      m_inflater.Reset();

    }

    internal virtual void end() {
    }

    ///////////////////////////////////////////////////////////////////////////
    /// Stream Functions
    ///////////////////////////////////////////////////////////////////////////

    public override void Flush() {
      throw new NotSupportedException();
    }

    public override long Seek(long offset, SeekOrigin origin) {
      throw new NotSupportedException();
    }

    public override void SetLength(long value) {
      throw new NotSupportedException();
    }

    public override int Read(byte[] buffer, int offset, int count) {
      throw new NotSupportedException();
    }

    public override void Write(byte[] buffer, int offset, int count) {
      throw new NotSupportedException();
    }

  }

}