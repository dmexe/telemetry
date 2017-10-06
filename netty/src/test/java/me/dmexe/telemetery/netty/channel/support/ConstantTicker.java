package me.dmexe.telemetery.netty.channel.support;

import java.util.concurrent.atomic.AtomicLong;
import me.dmexe.telemetery.netty.channel.Ticker;

public class ConstantTicker implements Ticker {
  private final AtomicLong tick = new AtomicLong(0);

  @Override
  public long nanoTime() {
    return tick.incrementAndGet() * 1000000L;
  }
}