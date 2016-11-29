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

package io.fd.hc2vpp.v3po.interfaces.acl.ingress;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.base.Optional;
import io.fd.hc2vpp.common.test.write.WriterCustomizerTest;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.v3po.interfaces.acl.IetfAclWriter;
import io.fd.hc2vpp.v3po.interfaces.acl.common.AclTableContextManager;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.core.dto.ClassifyTableByInterface;
import io.fd.vpp.jvpp.core.dto.InputAclSetInterface;
import io.fd.vpp.jvpp.core.dto.InputAclSetInterfaceReply;
import java.util.Collections;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.AclBase;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.EthAcl;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.AclKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.AccessListEntriesBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.context.rev161214.mapping.entry.context.attributes.acl.mapping.entry.context.mapping.table.MappingEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classfier.acl.rev161214.ietf.acl.base.attributes.AccessListsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classfier.acl.rev161214.ietf.acl.base.attributes.access.lists.AclBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev161214.SubinterfaceAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev161214.interfaces._interface.SubInterfaces;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev161214.interfaces._interface.sub.interfaces.SubInterface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev161214.interfaces._interface.sub.interfaces.SubInterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev161214.interfaces._interface.sub.interfaces.SubInterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev161214.sub._interface.base.attributes.IetfAcl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev161214.sub._interface.base.attributes.ietf.acl.Ingress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev161214.sub._interface.base.attributes.ietf.acl.IngressBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class SubInterfaceIetfAclCustomizerTest extends WriterCustomizerTest {

    private static final String IFC_TEST_INSTANCE = "ifc-test-instance";
    private static final String IF_NAME = "local0";
    private static final int IF_INDEX = 1;
    private static final String SUBIF_NAME = "local0.123";
    private static final int SUBIF_INDEX = 2;
    private static final long SUB_IF_ID = 123;
    private static final InstanceIdentifier<Ingress> IID =
        InstanceIdentifier.create(Interfaces.class).child(Interface.class, new InterfaceKey(IF_NAME)).augmentation(
            SubinterfaceAugmentation.class).child(SubInterfaces.class)
            .child(SubInterface.class, new SubInterfaceKey(SUB_IF_ID)).child(IetfAcl.class).child(Ingress.class);
    private static final String ACL_NAME = "acl1";
    private static final Class<? extends AclBase> ACL_TYPE = EthAcl.class;

    private SubInterfaceIetfAclCustomizer customizer;
    private Ingress acl;

    @Mock
    private AclTableContextManager aclCtx;

    private static InputAclSetInterface inputAclSetInterfaceWriteRequest() {
        final InputAclSetInterface request = new InputAclSetInterface();
        request.swIfIndex = SUBIF_INDEX;
        request.isAdd = 1;
        request.l2TableIndex = -1;
        request.ip4TableIndex = -1;
        request.ip6TableIndex = -1;
        return request;
    }

    private static InputAclSetInterface inputAclSetInterfaceDeleteRequest() {
        final InputAclSetInterface request = new InputAclSetInterface();
        request.swIfIndex = SUBIF_INDEX;
        request.l2TableIndex = -1;
        request.ip4TableIndex = -1;
        request.ip6TableIndex = -1;
        return request;
    }

    @Override
    protected void setUpTest() {
        customizer =
            new SubInterfaceIetfAclCustomizer(new IngressIetfAclWriter(api, aclCtx), new NamingContext("prefix", IFC_TEST_INSTANCE));
        defineMapping(mappingContext, IF_NAME, IF_INDEX, IFC_TEST_INSTANCE);

        acl = new IngressBuilder().setAccessLists(
            new AccessListsBuilder().setAcl(
                Collections.singletonList(new AclBuilder()
                    .setName(ACL_NAME)
                    .setType(ACL_TYPE)
                    .build())
            ).build()
        ).build();
    }

    @Test
    public void testDelete() throws WriteFailedException {
        defineMapping(mappingContext, IF_NAME, IF_INDEX, IFC_TEST_INSTANCE);
        defineMapping(mappingContext, SUBIF_NAME, SUBIF_INDEX, IFC_TEST_INSTANCE);
        when(api.inputAclSetInterface(any())).thenReturn(future(new InputAclSetInterfaceReply()));
        when(aclCtx.getEntry(SUBIF_INDEX, mappingContext)).thenReturn(Optional.of(
            new MappingEntryBuilder()
                .setIndex(SUBIF_INDEX)
                .setL2TableId(-1)
                .setIp4TableId(-1)
                .setIp6TableId(-1)
                .build()));

        customizer.deleteCurrentAttributes(IID, acl, writeContext);

        final ClassifyTableByInterface expectedRequest = new ClassifyTableByInterface();
        expectedRequest.swIfIndex = SUBIF_INDEX;
        verify(api).inputAclSetInterface(inputAclSetInterfaceDeleteRequest());
    }

    @Test
    public void testWrite() throws WriteFailedException {
        defineMapping(mappingContext, IF_NAME, IF_INDEX, IFC_TEST_INSTANCE);
        defineMapping(mappingContext, SUBIF_NAME, SUBIF_INDEX, IFC_TEST_INSTANCE);

        when(writeContext.readAfter(IID.firstIdentifierOf(SubInterface.class))).thenReturn(Optional.of(
            new SubInterfaceBuilder().build()
        ));

        when(writeContext.readAfter(IetfAclWriter.ACL_ID.child(
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.Acl.class,
            new AclKey(ACL_NAME, ACL_TYPE)))).thenReturn(Optional.of(
            new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.AclBuilder()
                .setAccessListEntries(
                    new AccessListEntriesBuilder().setAce(Collections.emptyList()).build()
                ).build()
        ));

        when(api.inputAclSetInterface(any())).thenReturn(future(new InputAclSetInterfaceReply()));

        customizer.writeCurrentAttributes(IID, acl, writeContext);

        verify(api).inputAclSetInterface(inputAclSetInterfaceWriteRequest());
    }
}