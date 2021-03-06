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

package io.fd.hc2vpp.routing.write;

import static io.fd.hc2vpp.routing.helpers.InterfaceTestHelper.INTERFACE_INDEX;
import static io.fd.hc2vpp.routing.helpers.InterfaceTestHelper.INTERFACE_NAME;

import com.google.common.collect.ImmutableList;
import io.fd.hc2vpp.common.test.write.WriterCustomizerTest;
import io.fd.hc2vpp.common.translate.util.MultiNamingContext;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.routing.Ipv6RouteData;
import io.fd.hc2vpp.routing.helpers.ClassifyTableTestHelper;
import io.fd.hc2vpp.routing.helpers.RoutingRequestTestHelper;
import io.fd.hc2vpp.routing.helpers.SchemaContextTestHelper;
import io.fd.hc2vpp.routing.naming.Ipv6RouteNamesFactory;
import io.fd.hc2vpp.vpp.classifier.context.VppClassifierContextManager;
import io.fd.honeycomb.test.tools.HoneycombTestRunner;
import io.fd.honeycomb.test.tools.annotations.InjectTestData;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.core.dto.IpAddDelRoute;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev180313.StaticRoutes1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev180313.routing.control.plane.protocols.control.plane.protocol._static.routes.Ipv6;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev180313.routing.control.plane.protocols.control.plane.protocol._static.routes.ipv6.Route;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev180313.routing.control.plane.protocols.control.plane.protocol._static.routes.ipv6.RouteBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev180313.routing.control.plane.protocols.control.plane.protocol._static.routes.ipv6.RouteKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev180313.routing.control.plane.protocols.control.plane.protocol._static.routes.ipv6.route.NextHopBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev180313.Static;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev180313.next.hop.content.next.hop.options.TableLookupCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev180313.routing.ControlPlaneProtocols;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev180313.routing.control.plane.protocols.ControlPlaneProtocol;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev180313.routing.control.plane.protocols.ControlPlaneProtocolKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev180313.routing.control.plane.protocols.control.plane.protocol.StaticRoutes;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.vpp.routing.types.rev180406.VniReference;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

@RunWith(HoneycombTestRunner.class)
public class Ipv6RouteCustomizerTest extends WriterCustomizerTest
        implements RoutingRequestTestHelper, ClassifyTableTestHelper, SchemaContextTestHelper {

    private static final int ROUTE_PROTOCOL_INDEX = 1;
    public static final Ipv6Prefix IPV_6_PREFIX = new Ipv6Prefix("2001:0db8:0a0b:12f0:0000:0000:0000:0001/64");
    @Captor
    private ArgumentCaptor<IpAddDelRoute> requestCaptor;

    @Mock
    private VppClassifierContextManager classifyManager;

    @Mock
    private MultiNamingContext routeHopContext;

    private NamingContext interfaceContext;
    private NamingContext routeContext;
    private NamingContext routingProtocolContext;
    private Ipv6RouteCustomizer customizer;

    private InstanceIdentifier<Route> validId;
    private Ipv6RouteNamesFactory namesFactory;

    @Override
    protected void setUpTest() throws Exception {
        interfaceContext = new NamingContext("interface", "interface-context");
        routeContext = new NamingContext("interface", "interface-context");
        routingProtocolContext = new NamingContext("routing-protocol", "routing-protocol-context");
        customizer =
                new Ipv6RouteCustomizer(api, interfaceContext, routeContext, routingProtocolContext, routeHopContext,
                        classifyManager);

        validId = InstanceIdentifier.create(ControlPlaneProtocols.class)
                .child(ControlPlaneProtocol.class, new ControlPlaneProtocolKey(ROUTE_PROTOCOL_NAME, Static.class))
                .child(StaticRoutes.class)
                .augmentation(StaticRoutes1.class)
                .child(Ipv6.class)
                .child(Route.class);

        namesFactory = new Ipv6RouteNamesFactory(interfaceContext, routingProtocolContext);

        defineMapping(mappingContext, INTERFACE_NAME, INTERFACE_INDEX, "interface-context");
        addMapping(classifyManager, CLASSIFY_TABLE_NAME, CLASSIFY_TABLE_INDEX, mappingContext);
        defineMapping(mappingContext, ROUTE_PROTOCOL_NAME, ROUTE_PROTOCOL_INDEX, "routing-protocol-context");
    }

    @Test
    public void testWriteSingleHop(
            @InjectTestData(resourcePath = "/ipv6/simplehop/simpleHopRouteWithClassifier.json", id = STATIC_ROUTE_PATH) StaticRoutes route)
            throws WriteFailedException {
        whenAddRouteThenSuccess(api);
        customizer.writeCurrentAttributes(validId, getIpv6RouteWithId(route, IPV_6_PREFIX), writeContext);
        verifyInvocation(1, ImmutableList
                .of(desiredFlaglessResult(1, 1, 0, Ipv6RouteData.FIRST_ADDRESS_AS_ARRAY, 64,
                        Ipv6RouteData.SECOND_ADDRESS_AS_ARRAY, INTERFACE_INDEX, 0, ROUTE_PROTOCOL_INDEX,
                    0, CLASSIFY_TABLE_INDEX, 1)), api, requestCaptor);
    }

    //TODO - https://jira.fd.io/browse/HONEYCOMB-396
    @Test
    public void testWriteTableLookup() throws WriteFailedException {
        final Route route = new RouteBuilder()
                .setKey(new RouteKey(IPV_6_PREFIX))
                .setDestinationPrefix(IPV_6_PREFIX)
                .setNextHop(new NextHopBuilder().setNextHopOptions(new TableLookupCaseBuilder()
                        .setSecondaryVrf(new VniReference(4L))
                        .build()).build())
                .build();
        whenAddRouteThenSuccess(api);
        noMappingDefined(mappingContext, namesFactory.uniqueRouteName(ROUTE_PROTOCOL_NAME, route), "route-context");
        customizer.writeCurrentAttributes(validId, route, writeContext);
        verifyInvocation(1, ImmutableList
                        .of(desiredFlaglessResult(1, 1, 0, Ipv6RouteData.FIRST_ADDRESS_AS_ARRAY, 64,
                                new byte[4], ~0, 0, ROUTE_PROTOCOL_INDEX, 4,
                                0, 0)),
                api, requestCaptor);
    }

    @Test
    public void testWriteHopList(
            @InjectTestData(resourcePath = "/ipv6/multihop/multiHopRouteWithClassifier.json", id = STATIC_ROUTE_PATH) StaticRoutes route)
            throws WriteFailedException {
        whenAddRouteThenSuccess(api);
        customizer.writeCurrentAttributes(validId, getIpv6RouteWithId(route, IPV_6_PREFIX), writeContext);
        verifyInvocation(2,
                ImmutableList.of(
                        desiredFlaglessResult(1, 1, 1, Ipv6RouteData.FIRST_ADDRESS_AS_ARRAY, 64,
                                Ipv6RouteData.SECOND_ADDRESS_AS_ARRAY, INTERFACE_INDEX, 2, ROUTE_PROTOCOL_INDEX, 0,
                                CLASSIFY_TABLE_INDEX, 1),
                        desiredFlaglessResult(1, 1, 1, Ipv6RouteData.FIRST_ADDRESS_AS_ARRAY, 64,
                                Ipv6RouteData.SECOND_ADDRESS_AS_ARRAY, INTERFACE_INDEX, 2, ROUTE_PROTOCOL_INDEX, 0,
                                CLASSIFY_TABLE_INDEX, 1)), api,
                requestCaptor);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testUpdate() throws WriteFailedException {
        customizer.updateCurrentAttributes(validId, Ipv6RouteData.IPV6_ROUTE_WITH_CLASSIFIER_BLACKHOLE_HOP,
                Ipv6RouteData.IPV6_ROUTE_WITH_CLASSIFIER_RECEIVE_HOP, writeContext);
    }

    @Test
    public void testDeleteSpecialHop(
            @InjectTestData(resourcePath = "/ipv6/specialhop/specialHopRouteBlackhole.json", id = STATIC_ROUTE_PATH) StaticRoutes route)
            throws WriteFailedException {
        whenAddRouteThenSuccess(api);
        customizer.deleteCurrentAttributes(validId, getIpv6RouteWithId(route, IPV_6_PREFIX),
                writeContext);
        verifyInvocation(1, ImmutableList
                        .of(desiredSpecialResult(0, 1, Ipv6RouteData.FIRST_ADDRESS_AS_ARRAY, 64,
                                                 1, 0, 0, 0, ROUTE_PROTOCOL_INDEX, 0)),
                         api, requestCaptor);
    }

    @Test
    public void testDeleteSingleHop(
            @InjectTestData(resourcePath = "/ipv6/simplehop/simpleHopRouteWithClassifier.json", id = STATIC_ROUTE_PATH) StaticRoutes route)
            throws WriteFailedException {
        whenAddRouteThenSuccess(api);
        customizer.deleteCurrentAttributes(validId, getIpv6RouteWithId(route, IPV_6_PREFIX), writeContext);
        verifyInvocation(1, ImmutableList
                .of(desiredFlaglessResult(0, 1, 0, Ipv6RouteData.FIRST_ADDRESS_AS_ARRAY, 64,
                        Ipv6RouteData.SECOND_ADDRESS_AS_ARRAY, INTERFACE_INDEX, 0, 1,
                    0, CLASSIFY_TABLE_INDEX, 1)), api, requestCaptor);
    }

    @Test
    public void testDeleteHopList(
            @InjectTestData(resourcePath = "/ipv6/multihop/multiHopRouteWithClassifier.json", id = STATIC_ROUTE_PATH) StaticRoutes route)
            throws WriteFailedException {
        whenAddRouteThenSuccess(api);
        customizer.deleteCurrentAttributes(validId, getIpv6RouteWithId(route, IPV_6_PREFIX), writeContext);
        verifyInvocation(2,
                ImmutableList.of(
                        desiredFlaglessResult(0, 1, 1, Ipv6RouteData.FIRST_ADDRESS_AS_ARRAY, 64,
                                Ipv6RouteData.SECOND_ADDRESS_AS_ARRAY, INTERFACE_INDEX, 2, 1, 0,
                                CLASSIFY_TABLE_INDEX, 1),
                        desiredFlaglessResult(0, 1, 1, Ipv6RouteData.FIRST_ADDRESS_AS_ARRAY, 64,
                                Ipv6RouteData.SECOND_ADDRESS_AS_ARRAY, INTERFACE_INDEX, 2, 1, 0,
                                CLASSIFY_TABLE_INDEX, 1)), api,
                requestCaptor);
    }

    @Test
    public void testWriteSpecialHop(
            @InjectTestData(resourcePath = "/ipv6/specialhop/specialHopRouteBlackhole.json", id = STATIC_ROUTE_PATH) StaticRoutes route)
            throws WriteFailedException {
        whenAddRouteThenSuccess(api);
        customizer.writeCurrentAttributes(validId, getIpv6RouteWithId(route, IPV_6_PREFIX),
                writeContext);
        verifyInvocation(1, ImmutableList
                        .of(desiredSpecialResult(1, 1, Ipv6RouteData.FIRST_ADDRESS_AS_ARRAY, 64,
                                                 1, 0, 0, 0, ROUTE_PROTOCOL_INDEX, 0)),
                         api, requestCaptor);
    }
}
