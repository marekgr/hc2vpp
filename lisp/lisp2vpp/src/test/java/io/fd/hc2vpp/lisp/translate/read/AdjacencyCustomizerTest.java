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

package io.fd.hc2vpp.lisp.translate.read;

import static io.fd.hc2vpp.lisp.translate.AdjacencyData.ADDRESS_ONE;
import static io.fd.hc2vpp.lisp.translate.AdjacencyData.ADDRESS_THREE;
import static io.fd.hc2vpp.lisp.translate.AdjacencyData.LOCAL_EID_ONE;
import static io.fd.hc2vpp.lisp.translate.AdjacencyData.LOCAL_EID_PREFIX_ONE;
import static io.fd.hc2vpp.lisp.translate.AdjacencyData.LOCAL_EID_PREFIX_TWO;
import static io.fd.hc2vpp.lisp.translate.AdjacencyData.LOCAL_EID_TWO;
import static io.fd.hc2vpp.lisp.translate.AdjacencyData.REMOTE_EID_ONE;
import static io.fd.hc2vpp.lisp.translate.AdjacencyData.REMOTE_EID_PREFIX_ONE;
import static io.fd.hc2vpp.lisp.translate.AdjacencyData.REMOTE_EID_PREFIX_TWO;
import static io.fd.hc2vpp.lisp.translate.AdjacencyData.REMOTE_EID_TWO;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import io.fd.hc2vpp.common.test.read.ListReaderCustomizerTest;
import io.fd.hc2vpp.common.translate.util.ByteDataTranslator;
import io.fd.hc2vpp.lisp.context.util.AdjacenciesMappingContext;
import io.fd.hc2vpp.lisp.context.util.EidMappingContext;
import io.fd.hc2vpp.lisp.translate.util.EidMetadataProvider;
import io.fd.hc2vpp.lisp.util.AdjacencyMappingContextTestHelper;
import io.fd.hc2vpp.lisp.util.EidMappingContextHelper;
import io.fd.honeycomb.translate.spi.read.ReaderCustomizer;
import io.fd.vpp.jvpp.core.dto.OneAdjacenciesGetReply;
import io.fd.vpp.jvpp.core.types.OneAdjacency;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.lisp.address.address.Ipv4;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.lisp.address.address.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev171013.MappingId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev171013.adjacencies.grouping.Adjacencies;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev171013.adjacencies.grouping.AdjacenciesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev171013.adjacencies.grouping.adjacencies.Adjacency;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev171013.adjacencies.grouping.adjacencies.AdjacencyBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev171013.adjacencies.grouping.adjacencies.AdjacencyKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev171013.dp.subtable.grouping.RemoteMappings;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev171013.dp.subtable.grouping.remote.mappings.RemoteMapping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev171013.dp.subtable.grouping.remote.mappings.RemoteMappingKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev171013.eid.table.grouping.EidTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev171013.eid.table.grouping.eid.table.VniTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev171013.eid.table.grouping.eid.table.VniTableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev171013.eid.table.grouping.eid.table.vni.table.BridgeDomainSubtable;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;


public class AdjacencyCustomizerTest
        extends ListReaderCustomizerTest<Adjacency, AdjacencyKey, AdjacencyBuilder>
        implements ByteDataTranslator, EidMetadataProvider, EidMappingContextHelper, AdjacencyMappingContextTestHelper {

    public static final String NORMALIZED_PREFIX_ONE = "192.168.2.0/24";
    public static final String NORMALIZED_PREFIX_THREE = "192.168.0.0/16";
    private InstanceIdentifier<Adjacency> identifier;

    public AdjacencyCustomizerTest() {
        super(Adjacency.class, AdjacenciesBuilder.class);
    }

    @Before
    public void init() {
        identifier = InstanceIdentifier.create(EidTable.class)
                .child(VniTable.class, new VniTableKey(2L))
                .child(BridgeDomainSubtable.class)
                .child(RemoteMappings.class)
                .child(RemoteMapping.class, new RemoteMappingKey(new MappingId("remote-mapping")))
                .child(Adjacencies.class)
                .child(Adjacency.class, new AdjacencyKey("adj-one"));
        defineAdjacencyMapping(mappingContext, "local-eid-one", "remote-eid-one", "adj-one",
                "adjacencies-mapping-context");
        defineAdjacencyMapping(mappingContext, "local-eid-two", "remote-eid-two", "adj-two",
                "adjacencies-mapping-context");
    }

    private void defineEidAddressMapping() {
        defineEidMapping(mappingContext, LOCAL_EID_ONE, new MappingId("local-eid-one"), "local-mapping-context");
        defineEidMapping(mappingContext, LOCAL_EID_TWO, new MappingId("local-eid-two"), "local-mapping-context");
        defineEidMapping(mappingContext, REMOTE_EID_ONE, new MappingId("remote-eid-one"), "remote-mapping-context");
        defineEidMapping(mappingContext, REMOTE_EID_TWO, new MappingId("remote-eid-two"), "remote-mapping-context");
    }

    private void defineEidPrefixMapping() {
        defineEidMapping(mappingContext, LOCAL_EID_PREFIX_ONE, new MappingId("local-eid-one"), "local-mapping-context");
        defineEidMapping(mappingContext, LOCAL_EID_PREFIX_TWO, new MappingId("local-eid-two"), "local-mapping-context");
        defineEidMapping(mappingContext, REMOTE_EID_PREFIX_ONE, new MappingId("remote-eid-one"), "remote-mapping-context");
        defineEidMapping(mappingContext, REMOTE_EID_PREFIX_TWO, new MappingId("remote-eid-two"), "remote-mapping-context");
    }

    @Test
    public void getAllIds() throws Exception {
        mockAddressDump();
        defineEidAddressMapping();
        final List<AdjacencyKey> keys = getCustomizer().getAllIds(identifier, ctx);

        assertThat(keys, hasSize(2));
        assertThat(keys, contains(new AdjacencyKey("adj-one"), new AdjacencyKey("adj-two")));
    }

    @Test
    public void readCurrentAttributes() throws Exception {
        mockAddressDump();
        defineEidAddressMapping();
        final AdjacencyBuilder builder = new AdjacencyBuilder();
        getCustomizer().readCurrentAttributes(identifier, builder, ctx);

        assertEquals("adj-one", builder.getId());
        assertEquals(new AdjacencyKey("adj-one"), builder.getKey());
        assertEquals(ADDRESS_ONE.getValue(), Ipv4.class.cast(builder.getLocalEid().getAddress()).getIpv4().getValue());
        assertEquals(ADDRESS_THREE.getValue(),
                Ipv4.class.cast(builder.getRemoteEid().getAddress()).getIpv4().getValue());
    }

    @Test
    public void readCurrentAttributesPrefixBased() throws Exception {
        mockPrefixDump();
        defineEidPrefixMapping();
        final AdjacencyBuilder builder = new AdjacencyBuilder();
        getCustomizer().readCurrentAttributes(identifier, builder, ctx);

        assertEquals("adj-one", builder.getId());
        assertEquals(new AdjacencyKey("adj-one"), builder.getKey());
        assertEquals(NORMALIZED_PREFIX_ONE,
                Ipv4Prefix.class.cast(builder.getLocalEid().getAddress()).getIpv4Prefix().getValue());
        assertEquals(NORMALIZED_PREFIX_THREE,
                Ipv4Prefix.class.cast(builder.getRemoteEid().getAddress()).getIpv4Prefix().getValue());
    }

    @Override
    protected ReaderCustomizer<Adjacency, AdjacencyBuilder> initCustomizer() {
        return new AdjacencyCustomizer(api, new EidMappingContext("local-mapping-context", "local-mapping-"),
                new EidMappingContext("remote-mapping-context", "remote-mapping-"),
                new AdjacenciesMappingContext("adjacencies-mapping-context"));
    }


    private void mockAddressDump() {
        OneAdjacency adjacencyOne = new OneAdjacency();
        adjacencyOne.eidType = 0;
        adjacencyOne.leid = new byte[]{-64, -88, 2, 1};
        adjacencyOne.leidPrefixLen = 32;
        adjacencyOne.reid = new byte[]{-64, -88, 2, 3};
        adjacencyOne.reidPrefixLen = 32;


        OneAdjacency adjacencyTwo = new OneAdjacency();
        adjacencyTwo.eidType = 0;
        adjacencyTwo.leid = new byte[]{-64, -88, 2, 2};
        adjacencyTwo.leidPrefixLen = 32;
        adjacencyTwo.reid = new byte[]{-64, -88, 2, 4};
        adjacencyTwo.reidPrefixLen = 32;

        OneAdjacenciesGetReply reply = new OneAdjacenciesGetReply();
        reply.adjacencies = new OneAdjacency[]{adjacencyOne, adjacencyTwo};

        when(api.oneAdjacenciesGet(any())).thenReturn(future(reply));
    }

    private void mockPrefixDump() {
        OneAdjacency adjacencyOne = new OneAdjacency();
        adjacencyOne.eidType = 0;
        adjacencyOne.leid = new byte[]{-64, -88, 2, 1};
        adjacencyOne.leidPrefixLen = 24;
        adjacencyOne.reid = new byte[]{-64, -88, 2, 3};
        adjacencyOne.reidPrefixLen = 16;


        OneAdjacency adjacencyTwo = new OneAdjacency();
        adjacencyTwo.eidType = 0;
        adjacencyTwo.leid = new byte[]{-64, -88, 2, 2};
        adjacencyTwo.leidPrefixLen = 28;
        adjacencyTwo.reid = new byte[]{-64, -88, 2, 4};
        adjacencyTwo.reidPrefixLen = 8;

        OneAdjacenciesGetReply reply = new OneAdjacenciesGetReply();
        reply.adjacencies = new OneAdjacency[]{adjacencyOne, adjacencyTwo};

        when(api.oneAdjacenciesGet(any())).thenReturn(future(reply));
    }
}