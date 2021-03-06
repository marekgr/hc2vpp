/*
 * Copyright (c) 2016 Cisco and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.fd.hc2vpp.common.integration;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import io.fd.hc2vpp.common.translate.util.VppStatusListener;
import io.fd.honeycomb.translate.read.ReaderFactory;
import io.fd.vpp.jvpp.JVppRegistry;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import net.jmob.guice.conf.core.ConfigurationModule;

public final class VppCommonModule extends AbstractModule {
    @Override
    protected void configure() {

        install(ConfigurationModule.create());
        // Inject non-dependency configuration
        requestInjection(VppConfigAttributes.class);

        bind(VppStatusListener.class).toInstance(new VppStatusListener());
        bind(JVppRegistry.class).toProvider(JVppRegistryProvider.class).in(Singleton.class);
        bind(FutureJVppCore.class).toProvider(JVppCoreProvider.class).in(Singleton.class);
        bind(JVppTimeoutProvider.JVppTimeoutInit.class).toProvider(JVppTimeoutProvider.class).asEagerSingleton();

        // Naming contexts reader exposing context storage over REST/HONEYCOMB_NETCONF
        final Multibinder<ReaderFactory> readerBinder = Multibinder.newSetBinder(binder(), ReaderFactory.class);
        readerBinder.addBinding().toProvider(ContextsReaderFactoryProvider.class).in(Singleton.class);
    }
}
