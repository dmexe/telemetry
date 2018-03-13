package me.dmexe.telemetery.netty.channel;

import static me.dmexe.telemetery.netty.channel.NettyConstants.SERVER_CONNECTION_CLOSED;
import static me.dmexe.telemetery.netty.channel.NettyConstants.SERVER_CONNECTION_CLOSED_RESPONSE;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.LastHttpContent;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * There is a state machine for HTTP request/response cycle. Handles transition
 * between IDLE -{@literal >} REQUEST_SEND -{@literal >} RESPONSE_RECEIVED -{@literal >}
 * COMPLETED -{@literal >} IDLE states.
 */
class NettyHttpClientTracingHandler extends ChannelDuplexHandler {
  private static final Logger log = LoggerFactory.getLogger(NettyHttpClientTracingHandler.class);

  enum State {
    IDLE,
    REQUEST_SEND,
    RESPONSE_RECEIVED,
    COMPLETED
  }

  private final NettyHttpTracingContext stats;
  private State state;

  NettyHttpClientTracingHandler(NettyHttpTracingContext stats) {
    Objects.requireNonNull(stats, "recorder cannot be null");
    this.stats = stats;
    reset();
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    if (msg instanceof HttpResponse && !isContinueResponse(msg)) {
      if (nextState(State.RESPONSE_RECEIVED)) {
        HttpResponse response = (HttpResponse) msg;
        stats.handleResponse(response);
      }
    }

    if (msg instanceof LastHttpContent && !isContinueResponse(msg)) {
      if (nextState(State.COMPLETED)) {
        stats.completed();
        nextState(State.IDLE);
      }
    }

    super.channelRead(ctx, msg);
  }

  @Override
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise)
      throws Exception {

    if (msg instanceof HttpRequest) {
      if (nextState(State.REQUEST_SEND)) {
        final HttpRequest request = (HttpRequest) msg;
        stats.handleRequest(request, ctx.channel());
      }
    }

    super.write(ctx, msg, promise);
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    // If request already sent, but channel deactivated without response, consider that a timeout
    // error happens.
    if (state == State.REQUEST_SEND) {
      state = State.RESPONSE_RECEIVED;
      stats.handleResponse(SERVER_CONNECTION_CLOSED_RESPONSE);

      if (nextState(State.COMPLETED)) {
        stats.completed();
        nextState(State.IDLE);
      }
    }

    // If response received, but channel channel deactivated without last http content, force close
    // successfully
    if (state == State.RESPONSE_RECEIVED) {
      if (nextState(State.COMPLETED)) {
        stats.completed();
        nextState(State.IDLE);
      }
    }

    super.channelInactive(ctx);
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    stats.exceptionCaught(cause);
    super.exceptionCaught(ctx, cause);
  }

  private static boolean isContinueResponse(Object msg) {
    if (msg instanceof HttpResponse) {
      final HttpResponse resp = (HttpResponse)msg;
      return resp.status().equals(HttpResponseStatus.CONTINUE);
    }
    return false;
  }

  private boolean nextState(State nextState) {
    boolean transition = false;

    if (nextState == State.REQUEST_SEND && state == State.IDLE) {
      transition = true;
    } else if (nextState == State.RESPONSE_RECEIVED && state == State.REQUEST_SEND) {
      transition = true;
    } else if (nextState == State.COMPLETED && state == State.RESPONSE_RECEIVED) {
      transition = true;
    } else if (nextState == State.IDLE && state == State.COMPLETED) {
      transition = true;
    }

    if (log.isDebugEnabled()) {
      if (transition) {
        log.debug("Transit from {} to {}", state, nextState);
      } else {
        log.debug("Cannot transit from {} to {}, reset to IDLE", state, nextState);
      }
    }

    if (transition && nextState == State.IDLE) {
      reset();
    } else if (transition) {
      this.state = nextState;
    } else {
      reset();
    }

    return transition;
  }

  private void reset() {
    this.state = State.IDLE;
  }
}
