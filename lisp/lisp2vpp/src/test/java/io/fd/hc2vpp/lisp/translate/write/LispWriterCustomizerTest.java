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

package io.fd.hc2vpp.lisp.translate.write;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import io.fd.hc2vpp.common.test.write.WriterCustomizerTest;
import io.fd.hc2vpp.lisp.translate.service.LispStateCheckService;
import io.fd.honeycomb.translate.write.WriteContext;
import org.mockito.Mock;

public abstract class LispWriterCustomizerTest extends WriterCustomizerTest{

    @Mock
    protected LispStateCheckService lispStateCheckService;

    protected void mockLispDisabledAfter(){
        doThrow(IllegalArgumentException.class)
                .when(lispStateCheckService).checkLispEnabledAfter(any(WriteContext.class));
    }

    protected void mockLispDisabledBefore(){
        doThrow(IllegalArgumentException.class)
                .when(lispStateCheckService).checkLispEnabledBefore(any(WriteContext.class));
    }


}
