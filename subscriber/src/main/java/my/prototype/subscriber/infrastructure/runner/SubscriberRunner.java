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
import javax.annotation.Nullable;
import javax.annotation.PreDestroy;
import my.prototype.subscriber.usecase.ExampleUseCase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


@Component
public class SubscriberRunner {

  @Value("${gcp.project-id}")
  private String projectId;

  @Value("${gcp.pubsub.subscription-id}")
  private String subscriptionId;

  private final ExampleUseCase useCase;
  private Subscriber subscriber;

  public SubscriberRunner(ExampleUseCase useCase) {
    this.useCase = useCase;
  }

  public void run() {

    // MessageReceiverの設定
    MessageReceiver receiver = createMessageReceiver();

    // subscriberの構築
    var subscriptionName = ProjectSubscriptionName.of(
        projectId, subscriptionId);
    Subscriber.Builder builder = Subscriber.newBuilder(subscriptionName, receiver);

    subscriber = builder.build();

    // メッセージ受信を開始
    subscriber.startAsync().awaitRunning();

  }


  /**
   * Pub/SubメッセージのPull受信処理を停止する
   */
  @PreDestroy
  public void stop() {
    if (subscriber != null) {
      subscriber.stopAsync();
    }
  }

  private MessageReceiver createMessageReceiver() {
    return (PubsubMessage message, AckReplyConsumer consumer) -> {
      // 送られてきたtrace情報をextractして、makeCurrentで、現在のcontextに設定する
      GlobalOpenTelemetry.getPropagators().getTextMapPropagator()
          .extract(Context.current(), message.getAttributesMap(), new TextMapGetter<>() {

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

      // spanを作成して、現在のspanに設定する
      Span span = GlobalOpenTelemetry.getTracerProvider().tracerBuilder("tracerSubscribe").build()
          .spanBuilder("subscribe").startSpan();

      // Reference:
      // https://opentelemetry.io/docs/languages/java/instrumentation/#create-spans
      try (Scope scope = span.makeCurrent()) {
        useCase.execute();
        consumer.ack();
      } catch (Exception e) {
        consumer.nack();
        throw e;
      } finally {
        span.end();
      }
    };
  }

}
