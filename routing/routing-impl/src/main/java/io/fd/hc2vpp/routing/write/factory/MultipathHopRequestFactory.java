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

package io.fd.hc2vpp.routing.write.factory;

import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.routing.write.factory.base.BasicHopRequestFactory;
import io.fd.hc2vpp.routing.write.trait.RouteRequestProducer;
import io.fd.hc2vpp.vpp.classifier.context.VppClassifierContextManager;
import io.fd.honeycomb.translate.MappingContext;
import io.fd.vpp.jvpp.core.dto.IpAddDelRoute;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev180313.routing.control.plane.protocols.control.plane.protocol._static.routes.ipv4.Route;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev180313.routing.control.plane.protocols.control.plane.protocol._static.routes.ipv4.route.next.hop.NextHop1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev180313.next.hop.content.next.hop.options.next.hop.list.next.hop.list.NextHop;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.vpp.ipv4.unicast.routing.rev180319.VppIpv4NextHopAugmentation;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.vpp.ipv4.unicast.routing.rev180319.VppIpv4RouteAttributesAugmentation;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.vpp.ipv4.unicast.routing.rev180319.routing.control.plane.protocols.control.plane.protocol._static.routes.ipv4.route.VppIpv4Route;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.vpp.ipv6.unicast.routing.rev180319.VppIpv6NextHopAugmentation;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.vpp.ipv6.unicast.routing.rev180319.VppIpv6RouteAttributesAugmentation;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.vpp.ipv6.unicast.routing.rev180319.routing.control.plane.protocols.control.plane.protocol._static.routes.ipv6.route.VppIpv6Route;

/**
 * Factory for creating requests to create route with multiple hops
 */
public class MultipathHopRequestFactory extends BasicHopRequestFactory implements RouteRequestProducer {

    private MultipathHopRequestFactory(final VppClassifierContextManager classifierContextManager,
                                       final NamingContext interfaceContext,
                                       final NamingContext routingProtocolContext) {
        super(classifierContextManager, interfaceContext, routingProtocolContext);
    }

    public static MultipathHopRequestFactory forContexts(
            @Nonnull final VppClassifierContextManager classifierContextManager,
            @Nonnull final NamingContext interfaceContext,
            @Nonnull final NamingContext routingProtocolContext) {
        return new MultipathHopRequestFactory(classifierContextManager, interfaceContext, routingProtocolContext);
    }

    public IpAddDelRoute createIpv4MultipathHopRequest(final boolean add,
                                                       @Nonnull final String parentProtocolName,
                                                       @Nonnull final Route route,
                                                       @Nonnull final NextHop hop,
                                                       @Nonnull final MappingContext mappingContext) {

        final VppIpv4Route routingAttributes = route.getAugmentation(VppIpv4RouteAttributesAugmentation.class) != null ?
            route.getAugmentation(VppIpv4RouteAttributesAugmentation.class).getVppIpv4Route() : null;

        final int nextHopInterfaceIndex =
                getInterfaceNamingContext().getIndex(hop.getOutgoingInterface(), mappingContext);

        if (routingAttributes != null && classifyTablePresent(routingAttributes.getClassifyTable(), getVppClassifierContextManager(),
                mappingContext)) {
            return getMultipathHopRequest(add,
                    route.getDestinationPrefix(),
                    nextHopInterfaceIndex,
                    hop.getAugmentation(NextHop1.class).getNextHopAddress(),
                    hop.getAugmentation(VppIpv4NextHopAugmentation.class).getWeight().byteValue(),
                    getRoutingProtocolContext().getIndex(parentProtocolName, mappingContext),
                    DEFAULT_VNI,
                    classifyTableIndex(routingAttributes.getClassifyTable(), getVppClassifierContextManager(),
                            mappingContext),
                    true);
        } else {
            return getMultipathHopRequest(add,
                    route.getDestinationPrefix(),
                    nextHopInterfaceIndex,
                    hop.getAugmentation(NextHop1.class).getNextHopAddress(),
                    hop.getAugmentation(VppIpv4NextHopAugmentation.class).getWeight().byteValue(),
                    getRoutingProtocolContext().getIndex(parentProtocolName, mappingContext),
                    DEFAULT_VNI,
                    0,
                    false);
        }
    }

    public IpAddDelRoute createIpv6MultipathHopRequest(final boolean add,
                                                       @Nonnull final String parentProtocolName,
                                                       @Nonnull final org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev180313.routing.control.plane.protocols.control.plane.protocol._static.routes.ipv6.Route route,
                                                       @Nonnull final NextHop hop,
                                                       @Nonnull final MappingContext mappingContext) {
        final VppIpv6Route routingAttributes = route.getAugmentation(VppIpv6RouteAttributesAugmentation.class) != null ?
            route.getAugmentation(VppIpv6RouteAttributesAugmentation.class).getVppIpv6Route() : null;

        final int nextHopInterfaceIndex =
                getInterfaceNamingContext().getIndex(hop.getOutgoingInterface(), mappingContext);

        if (routingAttributes != null && classifyTablePresent(routingAttributes.getClassifyTable(), getVppClassifierContextManager(),
                mappingContext)) {
            return getMultipathHopRequest(add,
                    route.getDestinationPrefix(),
                    nextHopInterfaceIndex,
                    hop.getAugmentation(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev180313.routing.control.plane.protocols.control.plane.protocol._static.routes.ipv6.route.next.hop.NextHop1.class).getNextHopAddress(),
                    hop.getAugmentation(VppIpv6NextHopAugmentation.class).getWeight().byteValue(),
                    getRoutingProtocolContext().getIndex(parentProtocolName, mappingContext),
                    DEFAULT_VNI,
                    classifyTableIndex(routingAttributes.getClassifyTable(), getVppClassifierContextManager(),
                            mappingContext),
                    true);
        } else {
            return getMultipathHopRequest(add,
                    route.getDestinationPrefix(),
                    nextHopInterfaceIndex,
                    hop.getAugmentation(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev180313.routing.control.plane.protocols.control.plane.protocol._static.routes.ipv6.route.next.hop.NextHop1.class).getNextHopAddress(),
                    hop.getAugmentation(VppIpv6NextHopAugmentation.class).getWeight().byteValue(),
                    getRoutingProtocolContext().getIndex(parentProtocolName, mappingContext),
                    DEFAULT_VNI,
                    0,
                    false);
        }
    }


    private IpAddDelRoute getMultipathHopRequest(final boolean isAdd, @Nonnull final Ipv6Prefix destinationAddress,
                                                 final int nextHopInterfaceIndex,
                                                 @Nonnull final Ipv6Address nextHopAddress,
                                                 final byte nextHopWeight,
                                                 final int primaryVrf, final int secondaryVrf,
                                                 final int classifyTableIndex, final boolean classifyIndexSet) {
        return flaglessAddDelRouteRequest(booleanToByte(isAdd), nextHopInterfaceIndex,
                ipv6AddressNoZoneToArray(nextHopAddress), nextHopWeight, (byte)1,
                ipv6AddressPrefixToArray(destinationAddress), extractPrefix(destinationAddress.getValue()), (byte)1,
                primaryVrf, secondaryVrf, classifyTableIndex,
                booleanToByte(classifyIndexSet));
    }

    private IpAddDelRoute getMultipathHopRequest(final boolean isAdd, @Nonnull final Ipv4Prefix destinationAddress,
                                                 final int nextHopInterfaceIndex,
                                                 @Nonnull final Ipv4Address nextHopAddress,
                                                 final byte nextHopWeight,
                                                 final int primaryVrf, final int secondaryVrf,
                                                 final int classifyTableIndex, final boolean classifyIndexSet) {
        return flaglessAddDelRouteRequest(booleanToByte(isAdd), nextHopInterfaceIndex,
                ipv4AddressNoZoneToArray(nextHopAddress.getValue()), nextHopWeight, (byte)0,
                ipv4AddressPrefixToArray(destinationAddress), extractPrefix(destinationAddress.getValue()), (byte)1,
                primaryVrf, secondaryVrf, classifyTableIndex,
                booleanToByte(classifyIndexSet));
    }


}
