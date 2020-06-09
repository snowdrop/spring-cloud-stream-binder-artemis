package me.snowdrop.stream.binder.artemis;

import me.snowdrop.stream.binder.artemis.application.StreamApplication;
import me.snowdrop.stream.binder.artemis.listeners.FailingStreamListener;
import me.snowdrop.stream.binder.artemis.sources.StringStreamSource;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.equalTo;

@RunWith(SpringRunner.class)
@SpringBootTest(
        classes = StreamApplication.class,
        properties = {
                // Keep values the same as in FailingStreamListener
                "spring.cloud.stream.bindings.output.destination=failing-destination",
                "spring.cloud.stream.bindings.input.destination=failing-destination",
                "spring.cloud.stream.bindings.input.group=failing-group",
                "spring.cloud.stream.bindings.input.consumer.back-off-multiplier=1"
        }
)
@Import({ StringStreamSource.class, FailingStreamListener.class })
public class RetryIT {

    @Autowired
    private StringStreamSource source;

    @Autowired
    private FailingStreamListener listener;

    @Test
    public void shouldRetryFailingDeliveries() throws InterruptedException {
        Thread.sleep(5000);
        source.send("test message");

        await().atMost(10, SECONDS)
                .until(listener::getErrorsCounter, is(equalTo(1)));

        assertThat(listener.getReceivedMessages())
                .hasSize(3)
                .containsOnly("test message");
    }

}
