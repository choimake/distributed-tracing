package my.prototype.subscriber.usecase;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import my.prototype.subscriber.logger.Logger;
import org.springframework.stereotype.Component;

@Component
public class ExampleUseCaseImpl implements ExampleUseCase {

  private final Logger logger;

  public ExampleUseCaseImpl(Logger logger) {
    this.logger = logger;
  }

  @Override
  @WithSpan
  public void execute() {
    logger.info("ExampleUseCaseImpl executed.");
    execute2();
  }

  @WithSpan
  private void execute2() {
    logger.info("execute2 executed.");
  }
}
