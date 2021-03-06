/*
 * Copyright (c) 2017 Cisco and/or its affiliates.
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

package io.fd.hc2vpp.l3.read.factory;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.l3.read.ipv4.subinterface.SubInterfaceIpv4AddressCustomizer;
import io.fd.hc2vpp.l3.read.ipv4.subinterface.SubInterfaceIpv4NeighbourCustomizer;
import io.fd.honeycomb.translate.impl.read.GenericInitListReader;
import io.fd.honeycomb.translate.impl.read.GenericListReader;
import io.fd.honeycomb.translate.read.ReaderFactory;
import io.fd.honeycomb.translate.read.registry.ModifiableReaderRegistryBuilder;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfacesState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev180319.SubinterfaceStateAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev180319.interfaces.state._interface.SubInterfaces;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev180319.interfaces.state._interface.sub.interfaces.SubInterface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev180319.sub._interface.ip4.attributes.Ipv4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev180319.sub._interface.ip4.attributes.Ipv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev180319.sub._interface.ip4.attributes.ipv4.Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev180319.sub._interface.ip4.attributes.ipv4.Neighbor;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;


public class SubInterfaceStateIpv4ReaderFactory implements ReaderFactory {

    private static final InstanceIdentifier<Interface> IFC_ID =
            InstanceIdentifier.create(InterfacesState.class).child(Interface.class);

    @Inject
    private FutureJVppCore jvpp;

    @Inject
    @Named("interface-context")
    private NamingContext ifcNamingContext;

    @Override
    public void init(@Nonnull final ModifiableReaderRegistryBuilder registry) {

        final InstanceIdentifier<SubInterface> subIfcId =
                IFC_ID.augmentation(SubinterfaceStateAugmentation.class)
                        .child(SubInterfaces.class).child(SubInterface.class);

        //    Ipv4(Structural)
        final InstanceIdentifier<Ipv4> ipv4Id = subIfcId.child(Ipv4.class);
        registry.addStructuralReader(ipv4Id, Ipv4Builder.class);
        //     Address
        registry.add(
                new GenericInitListReader<>(ipv4Id.child(Address.class),
                        new SubInterfaceIpv4AddressCustomizer(jvpp, ifcNamingContext)));

        registry.add(new GenericListReader<>(ipv4Id.child(Neighbor.class),
                new SubInterfaceIpv4NeighbourCustomizer(jvpp, ifcNamingContext)));
    }
}
