package my.prototype.subscriber;

import my.prototype.subscriber.infrastructure.runner.SubscriberRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class SubscriberApplication {

  SubscriberRunner runner;

  SubscriberApplication(SubscriberRunner runner) {
    this.runner = runner;
  }

  public static void main(String[] args) {

    // この手のアプリケーションのhealth check絡みの処理、どうやって実装するのか疑問
    // ファイル生成してそれの存在を判定するとか？
    //
    // Reference:
    // https://kubernetes.io/ja/docs/tasks/configure-pod-container/configure-liveness-readiness-startup-probes/#define-a-liveness-command

    // CommandLineRunnerはあまりよくないかもしれないという記事を見つけたので、今回使用していない
    //
    // Reference:
    // https://qiita.com/tag1216/items/898348a7fc3465148bc8
    try (ConfigurableApplicationContext ctx = SpringApplication.run(SubscriberApplication.class, args)) {
      SubscriberApplication app = ctx.getBean(SubscriberApplication.class);
      app.run(args);
    } catch (Exception e) {
      e.printStackTrace();
    }
    SpringApplication.run(SubscriberApplication.class, args);
  }

  public void run(String... args) throws Exception {
    runner.run();
  }
}
