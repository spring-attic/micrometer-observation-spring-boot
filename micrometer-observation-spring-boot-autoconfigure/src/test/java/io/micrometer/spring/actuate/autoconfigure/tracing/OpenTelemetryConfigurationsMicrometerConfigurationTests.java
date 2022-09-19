/*
 * Copyright 2012-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.micrometer.spring.actuate.autoconfigure.tracing;

import io.micrometer.tracing.otel.bridge.OtelCurrentTraceContext;
import io.micrometer.tracing.otel.bridge.OtelHttpClientHandler;
import io.micrometer.tracing.otel.bridge.OtelHttpServerHandler;
import io.micrometer.tracing.otel.bridge.OtelPropagator;
import io.micrometer.tracing.otel.bridge.OtelTracer;
import io.micrometer.tracing.otel.bridge.OtelTracer.EventPublisher;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapPropagator;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link OpenTelemetryConfigurations.MicrometerConfiguration}.
 *
 * @author Moritz Halbritter
 */
class OpenTelemetryConfigurationsMicrometerConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(OpenTelemetryConfigurations.MicrometerConfiguration.class));

	@Test
	void shouldSupplyBeans() {
		this.contextRunner.withUserConfiguration(TracerConfiguration.class, OpenTelemetryConfiguration.class,
				ContextPropagatorsConfiguration.class).run((context) -> {
					assertThat(context).hasSingleBean(OtelTracer.class);
					assertThat(context).hasSingleBean(EventPublisher.class);
					assertThat(context).hasSingleBean(OtelCurrentTraceContext.class);
					assertThat(context).hasSingleBean(OtelHttpClientHandler.class);
					assertThat(context).hasSingleBean(OtelHttpServerHandler.class);
					assertThat(context).hasSingleBean(OtelPropagator.class);
				});
	}

	@Test
	void shouldNotSupplyBeansIfMicrometerTracingBridgeOtelIsMissing() {
		this.contextRunner.withClassLoader(new FilteredClassLoader("io.micrometer.tracing.otel"))
				.withUserConfiguration(TracerConfiguration.class, OpenTelemetryConfiguration.class).run((context) -> {
					assertThat(context).doesNotHaveBean(OtelTracer.class);
					assertThat(context).doesNotHaveBean(EventPublisher.class);
					assertThat(context).doesNotHaveBean(OtelCurrentTraceContext.class);
					assertThat(context).doesNotHaveBean(OtelHttpClientHandler.class);
					assertThat(context).doesNotHaveBean(OtelHttpServerHandler.class);
				});
	}

	@Test
	void shouldNotSupplyBeansIfTracerIsMissing() {
		this.contextRunner.run((context) -> assertThat(context).doesNotHaveBean(OtelTracer.class));
	}

	@Test
	void shouldNotSupplyBeansIfOpenTelemetryIsMissing() {
		this.contextRunner.withUserConfiguration(TracerConfiguration.class).run((context) -> {
			assertThat(context).doesNotHaveBean(OtelHttpClientHandler.class);
			assertThat(context).doesNotHaveBean(OtelHttpServerHandler.class);
		});
	}

	@Test
	void shouldBackOffOnCustomBeans() {
		this.contextRunner.withUserConfiguration(CustomConfiguration.class).run((context) -> {
			assertThat(context).hasBean("customOtelTracer");
			assertThat(context).hasSingleBean(OtelTracer.class);
			assertThat(context).hasBean("customEventPublisher");
			assertThat(context).hasSingleBean(EventPublisher.class);
			assertThat(context).hasBean("customOtelCurrentTraceContext");
			assertThat(context).hasSingleBean(OtelCurrentTraceContext.class);
			assertThat(context).hasBean("customOtelHttpClientHandler");
			assertThat(context).hasSingleBean(OtelHttpClientHandler.class);
			assertThat(context).hasBean("customOtelHttpServerHandler");
			assertThat(context).hasSingleBean(OtelHttpServerHandler.class);
		});
	}

	@Test
	void shouldFailToStartApplicationContextWhenCustomPropagationAndNoCustomPropagationFactoryWasSet() {
		this.contextRunner.withPropertyValues("management.tracing.propagation.type=CUSTOM")
				.run((context) -> assertThat(context).getBeans(TextMapPropagator.class).isEmpty());
	}

	@Test
	void shouldSupplySinglePropagationFactoryForCustomPropagation() {
		this.contextRunner.withUserConfiguration(CustomFactoryConfiguration.class)
				.withPropertyValues("management.tracing.propagation.type=CUSTOM").run((context) -> assertThat(context)
						.hasSingleBean(TextMapPropagator.class).hasBean("customPropagationFactory"));
	}

	@Configuration(proxyBeanMethods = false)
	private static class CustomConfiguration {

		@Bean
		OtelTracer customOtelTracer() {
			return mock(OtelTracer.class);
		}

		@Bean
		EventPublisher customEventPublisher() {
			return mock(EventPublisher.class);
		}

		@Bean
		OtelCurrentTraceContext customOtelCurrentTraceContext() {
			return mock(OtelCurrentTraceContext.class);
		}

		@Bean
		OtelHttpClientHandler customOtelHttpClientHandler() {
			return mock(OtelHttpClientHandler.class);
		}

		@Bean
		OtelHttpServerHandler customOtelHttpServerHandler() {
			return mock(OtelHttpServerHandler.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	private static class TracerConfiguration {

		@Bean
		Tracer tracer() {
			return mock(Tracer.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	private static class OpenTelemetryConfiguration {

		@Bean
		OpenTelemetry openTelemetry() {
			return mock(OpenTelemetry.class, Answers.RETURNS_MOCKS);
		}

	}

	@Configuration(proxyBeanMethods = false)
	private static class ContextPropagatorsConfiguration {

		@Bean
		ContextPropagators contextPropagators() {
			return mock(ContextPropagators.class, Answers.RETURNS_MOCKS);
		}

	}

	@Configuration(proxyBeanMethods = false)
	private static class CustomFactoryConfiguration {

		@Bean
		TextMapPropagator customPropagationFactory() {
			return mock(TextMapPropagator.class);
		}

	}

}
