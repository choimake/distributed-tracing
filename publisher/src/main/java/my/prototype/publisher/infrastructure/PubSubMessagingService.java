package my.prototype.publisher.infrastructure;

import com.google.cloud.pubsub.v1.Publisher;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import com.google.pubsub.v1.TopicName;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapSetter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import my.prototype.publisher.messaging.MessagingService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PubSubMessagingService implements MessagingService {

  @Value("${gcp.project-id}")
  private String projectId;

  @Value("${gcp.pubsub.topic-id}")
  private String topicId;

  @Override
  public void publish(String message) throws Exception {
    var topicName = TopicName.of(projectId, topicId);
    Publisher publisher = null;

    try {
      publisher = Publisher.newBuilder(topicName).build();

      var attributes = new HashMap();

      // trace情報を抽出
      // PubSubにtrace情報を渡すことで、分散トレースを実現するため
      //
      // MEMO:
      // 詳細レベルで処理の意味がわからないので、調べた範囲でわかるレベルでのメモ
      //
      // GlobalOpenTelemetry.getPropagators().getTextMapPropagator()
      // 上記は、GlobalOpenTelemetry（OpenTelemetry関連の処理のエントリーポイント）から、
      // Propagator（トレース情報をサービスで伝搬するための方法？）を取得し、その中からTextMapPropagator（Propagatorの一種）を取得している
      //
      // inject(Context.current(), map, (TextMapSetter<Map<String, String>>) (carrier, key, value)
      // -> { ... })
      // トレース情報を指定のmapに注入するための処理
      //
      // if(carrier != null){ carrier.put(key, value); }:
      // carrier（分散トレーシングシステムにおいて、トレース情報（スパンコンテキスト）を伝播（Propagate）するための媒体 by ChatGPT）のnull判定を実施
      // これにより、null参照エラーを防ぐことができる
      GlobalOpenTelemetry.getPropagators()
          .getTextMapPropagator()
          .inject(
              Context.current(),
              attributes,
              (TextMapSetter<Map<String, String>>)
                  (carrier, key, value) -> {
                    if (carrier != null) {
                      carrier.put(key, value);
                    }
                  });

      var byteMessage = ByteString.copyFromUtf8(message);

      // trace情報をPub/Subのメッセージに付与する
      PubsubMessage pubsubMessage =
          PubsubMessage.newBuilder().setData(byteMessage).putAllAttributes(attributes).build();

      // futureを取得していないが、futureを取得して完了を判断するような処理を入れた方がいいかもしれない懸念あり
      publisher.publish(pubsubMessage);

    } catch (Exception e) {
      // 必要であれば、細かく例外処理を書きましょう
      // 不要であれば、catch句は消してしまって、もっと上の処理でキャッチしてもらうようにしても良い
      throw e;
    } finally {

      // 公式ドキュメントを見るに、shutdownは呼ぶ必要があるようなので、呼ぶ
      //
      // Reference:
      // https://cloud.google.com/java/docs/reference/google-cloud-pubsub/latest/com.google.cloud.pubsub.v1.Publisher#com_google_cloud_pubsub_v1_Publisher_newBuilder_com_google_pubsub_v1_TopicName_
      if (publisher != null) {
        publisher.shutdown();

        // shutdown実行後に、全ての実行処理が完了するまで待機する
        // 全てのリソースの解放がされることを確認するため
        //
        // Reference:
        // https://cloud.google.com/java/docs/reference/google-cloud-pubsub/latest/com.google.cloud.pubsub.v1.Publisher#com_google_cloud_pubsub_v1_Publisher_awaitTermination_long_java_util_concurrent_TimeUnit_
        if (!publisher.awaitTermination(1, TimeUnit.MINUTES)) {
          // ここの判定に入った時にはリソースの解放がされていないということになる。
          // プログラム上で対応する方法が見つけられていないので、ログを出力する以外にできることはなさそう。
          // Pub/Subに障害があった時にこのログから検知できる可能性がある気がしている。
        }
      }
    }
  }
}
