package com.example.billing_service;

import com.hospital.billing.BillingServiceApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;

@SpringBootTest(
        classes = BillingServiceApplication.class,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:billing;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
                "spring.jpa.hibernate.ddl-auto=create-drop"
        }
)
@EmbeddedKafka(
        partitions = 1,
        topics = {"payment-topic", "prescription-topic"},
        bootstrapServersProperty = "spring.kafka.bootstrap-servers"
)
class BillingServiceApplicationTests {

    @Test
    void contextLoads() {
    }

}
