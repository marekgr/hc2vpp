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


def _get_config(reply_filename=None, host='localhost', port=2831, username='admin', password='admin'):
    with manager.connect(host=host, port=port, username=username, password=password, hostkey_verify=False) as m:
        logger.info("Connected to HC")
        config = m.get_config(source='running')
        logger.debug("GetConfig successful:\n%s" % config)
        if reply_filename:
            with open(reply_filename, 'w') as f:
                f.write(config.data_xml)
        else:
            print config.data_xml


if __name__ == '__main__':
    logger = logging.getLogger("hc2vpp.examples.get_config")
    logging.basicConfig(level=logging.WARNING)
    argparser = argparse.ArgumentParser(description="Obtains VPP configuration using <get-config> RPC")
    argparser.add_argument('--reply_filename', help="name of XML file to store received configuration")
    args = argparser.parse_args()
    _get_config(args.reply_filename)
