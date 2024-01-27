package my.prototype.subscriber.usecase;

import my.prototype.subscriber.logger.Logger;
import org.springframework.stereotype.Component;

@Component
public class ExampleUseCaseImpl implements ExampleUseCase {

  private final Logger logger;

  public ExampleUseCaseImpl(Logger logger) {
    this.logger = logger;
  }

  @Override
  public void execute() {
    logger.info("ExampleUseCaseImpl executed.");
  }
}
