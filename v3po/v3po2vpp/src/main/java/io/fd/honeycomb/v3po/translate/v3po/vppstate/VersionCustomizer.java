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

package io.fd.honeycomb.v3po.translate.v3po.vppstate;

import io.fd.honeycomb.v3po.translate.Context;
import io.fd.honeycomb.v3po.translate.read.ReadFailedException;
import io.fd.honeycomb.v3po.translate.spi.read.ChildReaderCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.util.FutureJVppCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.utils.V3poUtils;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VppStateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.state.Version;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.state.VersionBuilder;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.dto.ShowVersion;
import org.openvpp.jvpp.dto.ShowVersionReply;
import org.openvpp.jvpp.future.FutureJVpp;

public final class VersionCustomizer
    extends FutureJVppCustomizer
    implements ChildReaderCustomizer<Version, VersionBuilder> {

    public VersionCustomizer(@Nonnull final FutureJVpp futureJVpp) {
        super(futureJVpp);
    }

    @Override
    public void merge(@Nonnull final Builder<? extends DataObject> parentBuilder, @Nonnull final Version readValue) {
        ((VppStateBuilder) parentBuilder).setVersion(readValue);
    }

    @Nonnull
    @Override
    public VersionBuilder getBuilder(@Nonnull InstanceIdentifier<Version> id) {
        return new VersionBuilder();
    }

    @Override
    public void readCurrentAttributes(@Nonnull InstanceIdentifier<Version> id, @Nonnull final VersionBuilder builder,
                                      @Nonnull final Context context) throws ReadFailedException {

        ShowVersionReply reply;
        try {
            reply = getFutureJVpp().showVersion(new ShowVersion()).toCompletableFuture().get();
        } catch (Exception e) {
            throw new ReadFailedException(id, e);
        }
        builder.setBranch(V3poUtils.toString(reply.version));
        builder.setName(V3poUtils.toString(reply.program));
        builder.setBuildDate(V3poUtils.toString(reply.buildDate));
        builder.setBuildDirectory(V3poUtils.toString(reply.buildDirectory));
    }

}