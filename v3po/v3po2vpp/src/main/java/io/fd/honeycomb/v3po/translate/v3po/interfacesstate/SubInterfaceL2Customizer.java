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

package io.fd.honeycomb.v3po.translate.v3po.interfacesstate;

import static io.fd.honeycomb.v3po.translate.v3po.util.SubInterfaceUtils.getSubInterfaceName;

import io.fd.honeycomb.v3po.translate.read.ReadContext;
import io.fd.honeycomb.v3po.translate.read.ReadFailedException;
import io.fd.honeycomb.v3po.translate.spi.read.ChildReaderCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.util.FutureJVppCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.util.NamingContext;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.interfaces.state._interface.sub.interfaces.SubInterface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.interfaces.state._interface.sub.interfaces.SubInterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.interfaces.state._interface.sub.interfaces.SubInterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.sub._interface.base.attributes.L2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.sub._interface.base.attributes.L2Builder;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.future.FutureJVpp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Customizer for reading vlan sub interface L2 operational state
 */
public class SubInterfaceL2Customizer extends FutureJVppCustomizer implements ChildReaderCustomizer<L2, L2Builder> {

    private static final Logger LOG = LoggerFactory.getLogger(SubInterfaceL2Customizer.class);
    private final InterconnectionReadUtils icReadUtils;

    public SubInterfaceL2Customizer(@Nonnull final FutureJVpp futureJvpp,
                                    @Nonnull final NamingContext interfaceContext,
                                    @Nonnull final NamingContext bridgeDomainContext) {
        super(futureJvpp);
        this.icReadUtils = new InterconnectionReadUtils(futureJvpp, interfaceContext, bridgeDomainContext);
    }

    @Override
    public void merge(@Nonnull final Builder<? extends DataObject> parentBuilder, @Nonnull final L2 readValue) {
        ((SubInterfaceBuilder) parentBuilder).setL2(readValue);
    }

    @Nonnull
    @Override
    public L2Builder getBuilder(@Nonnull final InstanceIdentifier<L2> id) {
        return new L2Builder();
    }

    @Override
    public void readCurrentAttributes(@Nonnull final InstanceIdentifier<L2> id, @Nonnull final L2Builder builder,
                                      @Nonnull final ReadContext ctx) throws ReadFailedException {
        LOG.debug("Reading attributes for sub-interface L2: {}", id);
        final InterfaceKey parentInterfacekey = id.firstKeyOf(Interface.class);
        final SubInterfaceKey subInterfacekey = id.firstKeyOf(SubInterface.class);
        final String subInterfaceName = getSubInterfaceName(parentInterfacekey.getName(), subInterfacekey.getIdentifier().intValue());

        builder.setInterconnection(icReadUtils.readInterconnection(subInterfaceName, ctx));
    }
}