#!/usr/bin/env python2
#
# Copyright (c) 2018 Cisco and/or its affiliates.
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at:
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

import argparse
import logging
from ncclient import manager

_SOURCE_TEMPLATE = """<source xmlns:xc="urn:ietf:params:xml:ns:netconf:base:1.0">%s</source>"""


def _copy_config(config_filename, host='localhost', port=2831, username='admin', password='admin'):
    with manager.connect(host=host, port=port, username=username, password=password, hostkey_verify=False) as m:
        logger.info("Connected to HC")
        with open(config_filename, 'r') as f:
            ret = m.copy_config(target='candidate', source=_SOURCE_TEMPLATE % f.read())
            logger.debug("CopyConfig successful:\n%s" % ret)
        ret = m.commit()
        logger.debug("Commit successful:\n%s", ret)


if __name__ == '__main__':
    logger = logging.getLogger("hc2vpp.examples.copy_config")
    logging.basicConfig(level=logging.WARNING)
    argparser = argparse.ArgumentParser(description="Configures VPP using <copy-config> RPC")
    argparser.add_argument('config_filename', help="name of XML file with <config> element")
    args = argparser.parse_args()
    _copy_config(args.config_filename)
