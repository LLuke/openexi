using System;
using System.IO;

using ICSharpCode.SharpZipLib.Zip.Compression;

namespace Nagasena.Proc.IO.Compression {

  internal sealed class EXIDeflaterOutputStream : Stream {

    private readonly Stream m_outputStream;

    private readonly Deflater m_deflater;

    private readonly byte[] m_bytesIn;
    private int m_n_bytesIn;

    private readonly byte[] m_bytesOut;

    public override bool CanRead {
      get {
        return false;
      }
    }
    public override bool CanSeek {
      get {
        return false;
      }
    }
    public override bool CanWrite {
      get {
        return true;
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

    public EXIDeflaterOutputStream(Stream outputStream, Deflater deflater) {
      m_outputStream = outputStream;
      deflater.Reset();
      m_deflater = deflater;

      m_bytesIn = new byte[65536];
      m_n_bytesIn = 0;
      m_bytesOut = new byte[8192];
    }

    public override void Flush() {
      flushInput();
      m_outputStream.Flush();
    }

    public override void Close() {
      m_outputStream.Close();
    }

    public override void WriteByte(byte bt) {
      m_bytesIn[m_n_bytesIn] = (byte)bt;
      if (++m_n_bytesIn == m_bytesIn.Length) {
        flushInput();
      }
    }

    private void flushInput() {
      long initialBytesRead = m_deflater.TotalIn;
      m_deflater.SetInput(m_bytesIn, 0, m_n_bytesIn);
      int n_bytesOut;
      do {
        if ((n_bytesOut = m_deflater.Deflate(m_bytesOut)) != 0) {
          for (int i = 0; i < n_bytesOut; i++) {
            m_outputStream.WriteByte((byte)(0xFF & m_bytesOut[i]));
          }
        }
        else {
          break;
        }
      }
      while (true);
      int bytesRead = (int)(m_deflater.TotalIn - initialBytesRead);
      if (bytesRead == m_n_bytesIn) {
        m_n_bytesIn = 0;
      }
      else if (bytesRead != 0) {
        m_n_bytesIn -= bytesRead;
        Array.Copy(m_bytesIn, bytesRead, m_bytesIn, 0, m_n_bytesIn);
      }
    }

    internal void resetDeflater() {
      if (m_n_bytesIn != 0) {
        flushInput();
      }
      m_deflater.SetInput(m_bytesIn, 0, m_n_bytesIn);
      m_deflater.Finish();
      while (!m_deflater.IsFinished) {
        int len = m_deflater.Deflate(m_bytesOut);
        for (int i = 0; i < len; i++) {
          m_outputStream.WriteByte((byte)(0xFF & m_bytesOut[i]));
        }
      }
      m_deflater.Reset();
    }

    ///////////////////////////////////////////////////////////////////////////
    /// Stream Functions
    ///////////////////////////////////////////////////////////////////////////

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