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

package io.fd.hc2vpp.v3po.interfacesstate;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.fd.hc2vpp.common.test.read.ReaderCustomizerTest;
import io.fd.hc2vpp.common.test.util.InterfaceDumpHelper;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.v3po.interfacesstate.cache.InterfaceCacheDumpManager;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.ReaderCustomizer;
import io.fd.vpp.jvpp.core.dto.SwInterfaceDetails;
import io.fd.vpp.jvpp.core.dto.SwInterfaceTapDetails;
import io.fd.vpp.jvpp.core.dto.SwInterfaceTapDetailsReplyDump;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfacesState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170607.VppInterfaceStateAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170607.VppInterfaceStateAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170607.interfaces.state._interface.Tap;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170607.interfaces.state._interface.TapBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class TapCustomizerTest extends ReaderCustomizerTest<Tap, TapBuilder> implements InterfaceDumpHelper {

    private static final String IFC_CTX_NAME = "ifc-test-instance";
    private static final String IF_NAME = "tap1";
    private static final String TAP_NAME = "testTapName";
    private static final int IF_INDEX = 1;
    private static final InstanceIdentifier<Tap> IID =
            InstanceIdentifier.create(InterfacesState.class).child(Interface.class, new InterfaceKey(IF_NAME))
                    .augmentation(VppInterfaceStateAugmentation.class).child(Tap.class);
    private NamingContext interfaceContext;

    @Mock
    private InterfaceCacheDumpManager dumpCacheManager;

    public TapCustomizerTest() {
        super(Tap.class, VppInterfaceStateAugmentationBuilder.class);
    }

    @Override
    protected void setUp() throws Exception {
        interfaceContext = new NamingContext("generatedIfaceName", IFC_CTX_NAME);
        defineMapping(mappingContext, IF_NAME, IF_INDEX, IFC_CTX_NAME);
        when(dumpCacheManager.getInterfaceDetail(IID, ctx, IF_NAME)).thenReturn(ifaceDetails());
    }

    private SwInterfaceDetails ifaceDetails() {
        final SwInterfaceDetails details = new SwInterfaceDetails();
        details.swIfIndex = IF_INDEX;
        details.interfaceName = IF_NAME.getBytes();
        details.tag = new byte[64];
        return details;
    }

    @Override
    protected ReaderCustomizer<Tap, TapBuilder> initCustomizer() {
        return new TapCustomizer(api, interfaceContext, dumpCacheManager);
    }

    @Test
    public void testRead() throws ReadFailedException {
        final TapBuilder builder = mock(TapBuilder.class);
        when(api.swInterfaceTapDump(any())).thenReturn(future(tapDump()));
        getCustomizer().readCurrentAttributes(IID, builder, ctx);
        verify(builder).setTapName(TAP_NAME);
    }

    @Test(expected = ReadFailedException.class)
    public void testReadFailed() throws ReadFailedException {
        when(api.swInterfaceTapDump(any())).thenReturn(failedFuture());
        getCustomizer().readCurrentAttributes(IID, mock(TapBuilder.class), ctx);
    }

    private SwInterfaceTapDetailsReplyDump tapDump() {
        final SwInterfaceTapDetailsReplyDump reply = new SwInterfaceTapDetailsReplyDump();
        final SwInterfaceTapDetails details = new SwInterfaceTapDetails();
        details.devName = TAP_NAME.getBytes();
        details.swIfIndex = IF_INDEX;
        reply.swInterfaceTapDetails.add(details);
        return reply;
    }
}