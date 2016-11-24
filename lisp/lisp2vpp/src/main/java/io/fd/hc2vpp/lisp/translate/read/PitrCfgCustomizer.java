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


import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.ReaderCustomizer;
import io.fd.hc2vpp.common.translate.util.ByteDataTranslator;
import io.fd.hc2vpp.common.translate.util.FutureJVppCustomizer;
import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import java.util.concurrent.TimeoutException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.lisp.feature.data.grouping.LispFeatureDataBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.pitr.cfg.grouping.PitrCfg;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.pitr.cfg.grouping.PitrCfgBuilder;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import io.fd.vpp.jvpp.VppBaseCallException;
import io.fd.vpp.jvpp.core.dto.ShowLispPitr;
import io.fd.vpp.jvpp.core.dto.ShowLispPitrReply;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Customizer for reading {@link PitrCfg}<br> Currently unsupported in jvpp
 */
public class PitrCfgCustomizer extends FutureJVppCustomizer
        implements ReaderCustomizer<PitrCfg, PitrCfgBuilder>, ByteDataTranslator, JvppReplyConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(PitrCfgCustomizer.class);

    public PitrCfgCustomizer(FutureJVppCore futureJvpp) {
        super(futureJvpp);
    }

    @Override
    public PitrCfgBuilder getBuilder(InstanceIdentifier<PitrCfg> id) {
        return new PitrCfgBuilder();
    }

    @Override
    public void readCurrentAttributes(InstanceIdentifier<PitrCfg> id, PitrCfgBuilder builder, ReadContext ctx)
            throws ReadFailedException {
        LOG.debug("Reading status for Lisp Pitr node {}", id);

        ShowLispPitrReply reply;

        try {
            reply = getPitrStatus();
        } catch (TimeoutException | VppBaseCallException e) {
            throw new ReadFailedException(id, e);
        }

        builder.setLocatorSet(toString(reply.locatorSetName));
        LOG.debug("Reading status for Lisp Pitr node {} successfull", id);
    }

    @Override
    public void merge(Builder<? extends DataObject> parentBuilder, PitrCfg readValue) {
        ((LispFeatureDataBuilder) parentBuilder).setPitrCfg(readValue);
    }

    public ShowLispPitrReply getPitrStatus() throws TimeoutException, VppBaseCallException {
        return getReply(getFutureJVpp().showLispPitr(new ShowLispPitr()).toCompletableFuture());
    }
}