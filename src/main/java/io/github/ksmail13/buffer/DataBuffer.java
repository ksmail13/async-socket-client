package io.github.ksmail13.buffer;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public interface DataBuffer {
    DataBuffer append(int val);
    DataBuffer append(byte val);
    DataBuffer append(char val);
    DataBuffer append(short val);
    DataBuffer append(float val);
    DataBuffer append(double val);
    DataBuffer append(String val);
    DataBuffer append(byte[] val);
    DataBuffer append(ByteBuffer buffer);

    ByteBuffer toBuffer();
    ByteBuffer toBuffer(Charset charset);
}
