package me.dmexe.telemetery.netty.channel;

import static me.dmexe.telemetery.netty.channel.Constants.CLIENT_CONNECTION_CLOSED;

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

public class HttpServerTracingHandler extends ChannelDuplexHandler {
  private static final Logger log = LoggerFactory.getLogger(HttpServerTracingHandler.class);

  enum State {
    IDLE,
    REQUEST_RECEIVED,
    RESPONSE_SEND,
    COMPLETED
  }

  private final HttpTracingContext stats;
  private State state;

  HttpServerTracingHandler(HttpTracingContext stats) {
    Objects.requireNonNull(stats, "stats cannot be null");
    this.stats = stats;
    reset();
  }

  /**
   * Save a request duration timer and request method.
   */
  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    if (msg instanceof HttpRequest) {
      if (nextState(State.REQUEST_RECEIVED)) {
        final HttpRequest request = (HttpRequest) msg;
        stats.handleRequest(request, ctx.channel());
      }
    }

    super.channelRead(ctx, msg);
  }

  /**
   * Save a response status and after {@link LastHttpContent} record request time.
   */
  @Override
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise)
      throws Exception {

    if (msg instanceof HttpResponse && !isContinueResponse(msg)) {
      if (nextState(State.RESPONSE_SEND)) {
        HttpResponse response = (HttpResponse) msg;
        stats.handleResponse(response, ctx.channel());
      }
    }

    if (msg instanceof LastHttpContent && !isContinueResponse(msg)) {
      if (nextState(State.COMPLETED)) {
        promise.addListener(future -> {
          stats.completed();
          nextState(State.IDLE);
        });
      }
    }

    super.write(ctx, msg, promise);
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    // client closed connection abnormally.
    if (state == State.REQUEST_RECEIVED) {
      stats.exceptionCaught(CLIENT_CONNECTION_CLOSED);
    }

    if (state != State.IDLE) {
      nextState(State.IDLE);
    }

    stats.completed();
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

    if (nextState == State.REQUEST_RECEIVED && state == State.IDLE) {
      transition = true;
    } else if (nextState == State.RESPONSE_SEND && state == State.REQUEST_RECEIVED) {
      transition = true;
    } else if (nextState == State.COMPLETED && state == State.RESPONSE_SEND) {
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
