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

package io.fd.hc2vpp.v3po.interfaces;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.core.dto.SwInterfaceSetL2Bridge;
import io.fd.vpp.jvpp.core.dto.SwInterfaceSetL2BridgeReply;
import io.fd.vpp.jvpp.core.dto.SwInterfaceSetL2Xconnect;
import io.fd.vpp.jvpp.core.dto.SwInterfaceSetL2XconnectReply;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170607.l2.config.attributes.Interconnection;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170607.l2.config.attributes.interconnection.BridgeBased;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170607.l2.config.attributes.interconnection.XconnectBased;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class providing Interconnection CUD support.
 */
final class InterconnectionWriteUtils implements JvppReplyConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(InterconnectionWriteUtils.class);

    private final FutureJVppCore futureJVppCore;
    private final NamingContext interfaceContext;
    private final NamingContext bridgeDomainContext;

    InterconnectionWriteUtils(@Nonnull final FutureJVppCore futureJVppCore,
                              @Nonnull final NamingContext interfaceContext,
                              @Nonnull final NamingContext bridgeDomainContext) {
        this.futureJVppCore = requireNonNull(futureJVppCore, "futureJVppCore should not be null");
        this.interfaceContext = requireNonNull(interfaceContext, "interfaceContext should not be null");
        this.bridgeDomainContext = requireNonNull(bridgeDomainContext, "bridgeDomainContext should not be null");
    }

    void setInterconnection(final InstanceIdentifier<? extends DataObject> id, final int swIfIndex,
                            final String ifcName, final Interconnection ic, final WriteContext writeContext)
            throws WriteFailedException {

        if (ic == null) { // TODO in case of update we should delete interconnection
            LOG.trace("Interconnection is not set. Skipping");
        } else if (ic instanceof XconnectBased) {
            setXconnectBasedL2(id, swIfIndex, ifcName, (XconnectBased) ic, writeContext, (byte) 1 /*enable*/);
        } else if (ic instanceof BridgeBased) {
            setBridgeBasedL2(id, swIfIndex, ifcName, (BridgeBased) ic, writeContext, (byte) 1 /*enable*/);
        } else {
            // Choices&cases are not data objects, so they cannot have a dedicated Reader/Writer
            // This choice is already from augment, so its not possible to augment augmented choice
            LOG.error("Unable to handle Interconnection of type {}", ic.getClass());
            throw new WriteFailedException(id, "Unable to handle Interconnection of type " + ic.getClass());
        }
    }

    void deleteInterconnection(final InstanceIdentifier<? extends DataObject> id, final int swIfIndex,
                               final String ifcName, final Interconnection ic, final WriteContext writeContext)
            throws WriteFailedException {

        if (ic == null) { // TODO in case of update we should delete interconnection
            LOG.trace("Interconnection is not set. Skipping");
        } else if (ic instanceof XconnectBased) {
            setXconnectBasedL2(id, swIfIndex, ifcName, (XconnectBased) ic, writeContext, (byte) 0 /*disable*/);
        } else if (ic instanceof BridgeBased) {
            setBridgeBasedL2(id, swIfIndex, ifcName, (BridgeBased) ic, writeContext, (byte) 0 /*disable*/);
        } else {
            LOG.error("Unable to delete Interconnection of type {}", ic.getClass());
            throw new WriteFailedException(id, "Unable to delete Interconnection of type " + ic.getClass());
        }
    }

    private void setBridgeBasedL2(final InstanceIdentifier<? extends DataObject> id, final int swIfIndex,
                                  final String ifcName, final BridgeBased bb,
                                  final WriteContext writeContext, final byte enabled) throws WriteFailedException {
        LOG.debug("Setting bridge based interconnection(bridge-domain={}) for interface: {}", bb.getBridgeDomain(),
                ifcName);

        String bdName = bb.getBridgeDomain();

        int bdId = bridgeDomainContext.getIndex(bdName, writeContext.getMappingContext());
        checkArgument(bdId > 0, "Unable to set Interconnection for Interface: %s, bridge domain: %s does not exist",
                ifcName, bdName);

        byte bvi = bb.isBridgedVirtualInterface()
                ? (byte) 1
                : (byte) 0;
        byte shg = 0;
        if (bb.getSplitHorizonGroup() != null) {
            shg = bb.getSplitHorizonGroup().byteValue();
        }

        final CompletionStage<SwInterfaceSetL2BridgeReply> swInterfaceSetL2BridgeReplyCompletionStage = futureJVppCore
                .swInterfaceSetL2Bridge(getL2BridgeRequest(swIfIndex, bdId, shg, bvi, enabled));
        getReplyForWrite(swInterfaceSetL2BridgeReplyCompletionStage.toCompletableFuture(), id);

        LOG.debug("Bridge based interconnection updated successfully for: {}, interconnection: {}", ifcName, bb);
    }

    private SwInterfaceSetL2Bridge getL2BridgeRequest(final int swIfIndex, final int bdId, final byte shg,
                                                      final byte bvi, final byte enabled) {
        final SwInterfaceSetL2Bridge swInterfaceSetL2Bridge = new SwInterfaceSetL2Bridge();
        swInterfaceSetL2Bridge.rxSwIfIndex = swIfIndex;
        swInterfaceSetL2Bridge.bdId = bdId;
        swInterfaceSetL2Bridge.shg = shg;
        swInterfaceSetL2Bridge.bvi = bvi;
        swInterfaceSetL2Bridge.enable = enabled;
        return swInterfaceSetL2Bridge;
    }

    private void setXconnectBasedL2(final InstanceIdentifier<? extends DataObject> id, final int swIfIndex,
                                    final String ifcName, final XconnectBased ic,
                                    final WriteContext writeContext, final byte enabled) throws WriteFailedException {
        String outSwIfName = ic.getXconnectOutgoingInterface();
        LOG.debug("Setting xconnect based interconnection(outgoing ifc={}) for interface: {}", outSwIfName, ifcName);

        int outSwIfIndex = interfaceContext.getIndex(outSwIfName, writeContext.getMappingContext());
        checkArgument(outSwIfIndex > 0,
                "Unable to set Interconnection for Interface: %s, outgoing interface: %s does not exist",
                ifcName, outSwIfIndex);

        final CompletionStage<SwInterfaceSetL2XconnectReply> swInterfaceSetL2XconnectReplyCompletionStage =
                futureJVppCore
                        .swInterfaceSetL2Xconnect(getL2XConnectRequest(swIfIndex, outSwIfIndex, enabled));
        getReplyForWrite(swInterfaceSetL2XconnectReplyCompletionStage.toCompletableFuture(), id);
        LOG.debug("Xconnect based interconnection updated successfully for: {}, interconnection: {}", ifcName, ic);
    }

    private SwInterfaceSetL2Xconnect getL2XConnectRequest(final int rxIfc, final int txIfc,
                                                          final byte enabled) {

        final SwInterfaceSetL2Xconnect swInterfaceSetL2Xconnect = new SwInterfaceSetL2Xconnect();
        swInterfaceSetL2Xconnect.enable = enabled;
        swInterfaceSetL2Xconnect.rxSwIfIndex = rxIfc;
        swInterfaceSetL2Xconnect.txSwIfIndex = txIfc;
        return swInterfaceSetL2Xconnect;
    }
}
