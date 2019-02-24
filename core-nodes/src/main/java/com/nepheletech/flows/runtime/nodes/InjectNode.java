package com.nepheletech.flows.runtime.nodes;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nepheletech.flows.runtime.events.NodesStartedEvent;
import com.nepheletech.flows.runtime.events.NodesStartedEventListener;
import com.nepheletech.flows.runtime.flows.Flow;
import com.nepheletech.json.JsonObject;

import it.sauronsoftware.cron4j.Scheduler;

/**
 * 
 */
public class InjectNode extends AbstractNode implements NodesStartedEventListener {
  private final Logger logger = LoggerFactory.getLogger(InjectNode.class);

  private final String topic;
  private final String payload;
  private final String payloadType;
  private final long repeat;
  private String crontab;
  private final boolean once;
  private final long onceDelay;

  private ScheduledExecutorService fixedRateScheduler = null;
  private Scheduler cron4jScheduler = null;

  private final Runnable scheduledTask = () -> receive(null);

  public InjectNode(Flow flow, JsonObject config) {
    super(flow, config);
    this.topic = config.get("topic").asString("");
    this.payload = config.get("payload").asString("");
    this.payloadType = config.get("payloadType").asString("");
    this.repeat = config.get("repeat").asLong(0L);
    this.crontab = config.get("crontab").asString(null);
    this.once = config.get("once").asBoolean(false);
    this.onceDelay = (long) Math.max(config.get("onceDelay").asDouble(0.1D) * 1000D, 0);

    logger.trace(">>> created");
  }

  @Override
  public void onNodesStarted(NodesStartedEvent event) {
    logger.trace(">>> onNodesStarted");

    if (this.once && this.onceDelay > 0L) {
      final Timer timer = new Timer();
      timer.schedule(new TimerTask() {
        @Override
        public void run() {
          startup();
        }
      }, onceDelay);
    } else {
      startup();
    }
  }

  private void startup() {
    logger.trace(">>> startup");

    if (this.repeat > 0L) {
      fixedRateScheduler = Executors.newScheduledThreadPool(1);
      fixedRateScheduler.scheduleAtFixedRate(scheduledTask, repeat, repeat, TimeUnit.SECONDS);
    } else if (this.crontab != null) {
      cron4jScheduler = new Scheduler();
      cron4jScheduler.schedule(this.crontab, scheduledTask);
      cron4jScheduler.start();
    }

    if (this.once) {
      receive(null);
    }
  }

  @Override
  public void close() {
    logger.trace(">>> close");

    if (cron4jScheduler != null) {
      try {
        cron4jScheduler.stop();
      } catch(Exception e) {
        logger.error("Stopping cron4jScheduler error", e);
      } finally {
        cron4jScheduler = null;
      }
    }
    
    if (fixedRateScheduler != null) {
      try {
        fixedRateScheduler.shutdown();
      } catch(Exception e) {
        logger.error("Stopping fixedRateScheduler error", e);
      } finally {
        fixedRateScheduler = null;
      }
    }

    super.close();
  }

  @Override
  protected void onMessage(JsonObject msg) {
    logger.trace(">>> onMessage: msg={}", msg);

    msg.set("topic", topic);

    if (!"flow".equals(payloadType) && !"global".equals(payloadType)) {
      try {
        if ((payloadType == null && payload.isEmpty())
            || "date".equals(payloadType)) {
          msg.set("payload", System.currentTimeMillis());
        } else if (payloadType == null) {
          msg.set("payload", payload);
        } else if ("none".equals(payloadType)) {
          msg.set("payload", "");
        } else {
          msg.set("payload", evaluateNodeProperty(payload, payloadType, this, msg));
        }
      } catch (Exception err) {
        // error(err, msg);
      }
    } else {
      try {
        msg.set("payload", evaluateNodeProperty(payload, payloadType, this, msg));
      } catch (Exception err) {
        // error(err, msg);
      }
    }

    send(msg);
  }
}
