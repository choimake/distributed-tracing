package my.prototype.subscriber.infrastructure.runner;

import com.google.cloud.pubsub.v1.AckReplyConsumer;
import com.google.cloud.pubsub.v1.MessageReceiver;
import com.google.cloud.pubsub.v1.Subscriber;
import com.google.pubsub.v1.ProjectSubscriptionName;
import com.google.pubsub.v1.PubsubMessage;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapGetter;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import javax.annotation.Nullable;
import my.prototype.subscriber.usecase.ExampleUseCase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


@Component
public class SubscriberRunner {

  @Value("${gcp.project-id}")
  private String projectId;

  @Value("${gcp.pubsub.subscription-id}")
  private String subscriptionId;

  // TODO: 初めてみる型なので調べる
  public static final BlockingQueue<PubsubMessage> messages = new LinkedBlockingDeque<>();

  private final ExampleUseCase useCase;

  public SubscriberRunner(ExampleUseCase useCase) {
   this.useCase = useCase;
  }

  public void run() throws Exception {

    var subscriptionName = ProjectSubscriptionName.of(
        projectId, subscriptionId);

    MessageReceiver receiver =
        (PubsubMessage message, AckReplyConsumer consumer) -> {
          // offerに失敗した場合の処理を作ってあげる必要がある
          messages.offer(message);
          // メッセージの処理が完了したら、acknowledge (確認応答) を送信
          consumer.ack();
        };

    Subscriber subscriber = null;
    try {
      subscriber = Subscriber.newBuilder(subscriptionName, receiver).build();
      subscriber.startAsync().awaitRunning();

      // 無限ループでサブスクライバーを稼働させる
      while (true) {
        var message = messages.take();

        // 送られてきたtrace情報をextractして、contextに設定する
        // Pub/Subを経由した分散トレースを実現するため
        extractAndSetTraceContext(message);

        // spanを作成して、現在のspanに設定する
        Span span = GlobalOpenTelemetry.getTracerProvider().tracerBuilder("tracerSubscribe").build().spanBuilder("subscribe").startSpan();

        // Reference:
        // https://opentelemetry.io/docs/languages/java/instrumentation/#create-spans
        try(Scope scope = span.makeCurrent()){
          useCase.execute();
        } catch (Exception e) {
          throw e;
        } finally {
          span.end();
        }

        // この無限ループの実装だと1秒に1件ずつメッセージを取得する動きになる
        Thread.sleep(1000);
      }

    } catch (InterruptedException e) {
      System.out.println("Subscriber was interrupted");
      throw e;
    } catch (Exception e) {
      throw e;
    } finally {
      if (subscriber != null) {
        subscriber.stopAsync();
      }
    }
  }

  private void extractAndSetTraceContext(PubsubMessage message) {
    // 送られてきたtrace情報をextractして、makeCurrentで、現在のcontextに設定する
    GlobalOpenTelemetry.getPropagators().getTextMapPropagator().extract(Context.current(), message.getAttributesMap(), new TextMapGetter<>() {
      @Nullable
      @Override
      public String get(@Nullable Map<String, String> carrier, String key) {
        if (carrier == null) {
          return null;
        }
        return carrier.get(key);
      }

      @Override
      public Iterable<String> keys(@Nullable Map<String, String> carrier) {
        if (carrier == null) {
          return Collections.emptyList();
        }
        return carrier.keySet();
      }
    }).makeCurrent();
  }
}
