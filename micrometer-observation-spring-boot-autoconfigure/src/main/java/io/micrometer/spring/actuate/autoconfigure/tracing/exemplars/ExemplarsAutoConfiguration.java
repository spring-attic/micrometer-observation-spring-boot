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

package io.micrometer.spring.actuate.autoconfigure.tracing.exemplars;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.prometheus.client.exemplars.tracer.common.SpanContextSupplier;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.actuate.autoconfigure.metrics.export.prometheus.PrometheusMetricsExportAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.tracing.ConditionalOnEnabledTracing;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Prometheus Exemplars with
 * Micrometer Tracing.
 *
 * @author Jonatan Ivanov
 * @since 3.0.0
 */
@AutoConfiguration(before = PrometheusMetricsExportAutoConfiguration.class)
@ConditionalOnClass({ Tracer.class, SpanContextSupplier.class })
@ConditionalOnEnabledTracing
public class ExemplarsAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	SpanContextSupplier spanContextSupplier(ObjectProvider<Tracer> tracerProvider) {
		return new LazyTracingSpanContextSupplier(tracerProvider);
	}

	/**
	 * Since the MeterRegistry can depend on the {@link Tracer} (Exemplars) and the
	 * {@link Tracer} can depend on the MeterRegistry (recording metrics), this
	 * {@link SpanContextSupplier} breaks the circle by lazily loading the {@link Tracer}.
	 */
	static class LazyTracingSpanContextSupplier implements SpanContextSupplier, SmartInitializingSingleton {

		private final ObjectProvider<Tracer> tracerProvider;

		private Tracer tracer;

		LazyTracingSpanContextSupplier(ObjectProvider<Tracer> tracerProvider) {
			this.tracerProvider = tracerProvider;
		}

		@Override
		public String getTraceId() {
			return this.tracer.currentSpan().context().traceId();
		}

		@Override
		public String getSpanId() {
			return this.tracer.currentSpan().context().spanId();
		}

		@Override
		public boolean isSampled() {
			return this.tracer != null && isSampled(this.tracer);
		}

		private boolean isSampled(Tracer tracer) {
			Span currentSpan = tracer.currentSpan();
			return currentSpan != null && currentSpan.context().sampled();
		}

		@Override
		public void afterSingletonsInstantiated() {
			this.tracer = this.tracerProvider.getIfAvailable();
		}

	}

}
