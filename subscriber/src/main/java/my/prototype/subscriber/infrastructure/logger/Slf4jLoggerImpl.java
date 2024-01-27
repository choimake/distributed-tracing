package my.prototype.subscriber.infrastructure.logger;

import my.prototype.subscriber.logger.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class Slf4jLoggerImpl implements Logger {

  private final org.slf4j.Logger logger = LoggerFactory.getLogger(Slf4jLoggerImpl.class);

  @Override
  public void info(String message) {
    this.logger.info(message);
  }
}
