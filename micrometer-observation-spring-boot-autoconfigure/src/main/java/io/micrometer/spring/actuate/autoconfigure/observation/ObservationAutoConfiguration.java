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

package io.micrometer.spring.actuate.autoconfigure.observation;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.observation.DefaultMeterObservationHandler;
import io.micrometer.core.instrument.observation.MeterObservationHandler;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.handler.TracingAwareMeterObservationHandler;

import org.springframework.boot.actuate.autoconfigure.metrics.CompositeMeterRegistryAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.tracing.MicrometerTracingAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// TODO: Delete this once this PR is merged: https://github.com/spring-projects/spring-boot/pull/32399
/**
 * {@link EnableAutoConfiguration Auto-configuration} for the Micrometer Observation API.
 *
 * @author Moritz Halbritter
 * @author Jonatan Ivanov
 * @since 3.0.0
 */
@AutoConfiguration(after = { CompositeMeterRegistryAutoConfiguration.class, MicrometerTracingAutoConfiguration.class })
@ConditionalOnClass(ObservationRegistry.class)
public class ObservationAutoConfiguration {

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnBean(Tracer.class)
	static class MetricsWithTracingConfiguration {

		@Bean
		@ConditionalOnMissingBean(MeterObservationHandler.class)
		@ConditionalOnBean(MeterRegistry.class)
		TracingAwareMeterObservationHandler<Observation.Context> tracingAwareMeterObservationHandler(
				MeterRegistry meterRegistry, Tracer tracer) {
			return new TracingAwareMeterObservationHandler<>(new DefaultMeterObservationHandler(meterRegistry), tracer);
		}

	}

}
