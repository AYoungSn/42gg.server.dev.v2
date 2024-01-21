package com.gg.server.utils.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

/**
 * DataJpa 테스트를 위한 어노테이션
 *
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Tag(TestTypeConstant.UNIT_TEST)
@DataJpaTest
public @interface DataJpaUnitTest {

}