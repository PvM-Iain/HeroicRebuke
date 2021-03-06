package com.bitzcraftonline.rebuke;

import java.util.Random;

public class RandomString
{
  private static final char[] symbols = new char[36];

  private final Random random = new Random();
  private final char[] buf;

  public RandomString(int length)
  {
    if (length < 1) {
      throw new IllegalArgumentException("length < 1: " + length);
    }
    this.buf = new char[length];
  }

  public String nextString() {
    for (int idx = 0; idx < this.buf.length; idx++) {
      this.buf[idx] = symbols[this.random.nextInt(symbols.length)];
    }
    return new String(this.buf);
  }

  static
  {
    for (int idx = 0; idx < 10; idx++) {
      symbols[idx] = (char)(48 + idx);
    }
    for (int idx = 10; idx < 36; idx++)
      symbols[idx] = (char)(97 + idx - 10);
  }
}
