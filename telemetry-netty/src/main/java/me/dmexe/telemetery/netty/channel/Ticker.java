package me.dmexe.telemetery.netty.channel;

@FunctionalInterface
public interface Ticker {
  long nanoTime();
}
