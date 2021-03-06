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

package io.fd.hc2vpp.v3po.factory;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.fd.hc2vpp.v3po.l2.ArpTerminationTableEntryCustomizer;
import io.fd.hc2vpp.v3po.l2.BridgeDomainCustomizer;
import io.fd.hc2vpp.v3po.l2.L2FibEntryCustomizer;
import io.fd.honeycomb.translate.impl.write.GenericListWriter;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.honeycomb.translate.write.WriterFactory;
import io.fd.honeycomb.translate.write.registry.ModifiableWriterRegistryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170607.BridgeDomains;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170607.bridge.domain.attributes.ArpTerminationTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170607.bridge.domain.attributes.arp.termination.table.ArpTerminationTableEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170607.bridge.domains.BridgeDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170607.l2.fib.attributes.L2FibTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170607.l2.fib.attributes.l2.fib.table.L2FibEntry;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;

public final class L2HoneycombWriterFactory implements WriterFactory {

    private final FutureJVppCore jvpp;
    private final NamingContext bdContext;
    private final NamingContext ifcContext;

    @Inject
    public L2HoneycombWriterFactory(final FutureJVppCore vppJvppWriterDependency,
                                    @Named("bridge-domain-context") final NamingContext bridgeDomainContextVppDependency,
                                    @Named("interface-context") final NamingContext interfaceContextVppDependency) {
        this.jvpp = vppJvppWriterDependency;
        this.bdContext = bridgeDomainContextVppDependency;
        this.ifcContext = interfaceContextVppDependency;
    }

    @Override
    public void init(final ModifiableWriterRegistryBuilder registry) {
        // Vpp has no handlers
        //  BridgeDomains has no handlers
        //   BridgeDomain(handled before L2 of ifc and subifc) =
        final InstanceIdentifier<BridgeDomain> bdId =
            InstanceIdentifier.create(BridgeDomains.class).child(BridgeDomain.class);
        registry.addBefore(new GenericListWriter<>(bdId, new BridgeDomainCustomizer(jvpp, bdContext)),
            Sets.newHashSet(
                InterfacesWriterFactory.L2_ID,
                SubinterfaceAugmentationWriterFactory.L2_ID));
        //    L2FibTable has no handlers
        //     L2FibEntry(handled after BridgeDomain and L2 of ifc and subifc) =
        final InstanceIdentifier<L2FibEntry> l2FibEntryId = bdId.child(L2FibTable.class).child(L2FibEntry.class);
        registry.addAfter(
                new GenericListWriter<>(l2FibEntryId, new L2FibEntryCustomizer(jvpp, bdContext, ifcContext)),
                Sets.newHashSet(
                        bdId,
                        InterfacesWriterFactory.L2_ID,
                        SubinterfaceAugmentationWriterFactory.L2_ID));
        //    ArpTerminationTable has no handlers
        //     ArpTerminationTableEntry(handled after BridgeDomain) =
        final InstanceIdentifier<ArpTerminationTableEntry> arpEntryId =
                bdId.child(ArpTerminationTable.class).child(ArpTerminationTableEntry.class);
        registry.addAfter(
                new GenericListWriter<>(arpEntryId, new ArpTerminationTableEntryCustomizer(jvpp, bdContext)),
                bdId);
    }
}
