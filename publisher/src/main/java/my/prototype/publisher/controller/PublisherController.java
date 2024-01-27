package my.prototype.publisher.controller;

import my.prototype.publisher.logger.Logger;
import my.prototype.publisher.messaging.MessagingService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PublisherController {

  private final MessagingService messagingService;
  private final Logger logger;

  PublisherController(MessagingService messagingService, Logger logger) {
    this.messagingService = messagingService;
    this.logger = logger;
  }

  @PostMapping("/publish")
  public String execute(@RequestParam("message") String message) {

    try {
      // メッセージングサービスにpublishする
      logger.info("before publish");
      messagingService.publish(message);
      logger.info("after publish");

    } catch (Exception e) {
      return "fail to publish...";
    }

    return "published!";
  }
}
