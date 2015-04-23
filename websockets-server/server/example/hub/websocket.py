# -*- coding: utf-8 -*-
# Copyright 2013 Jacek Marchwicki <jacek.marchwicki@gmail.com>
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
#   distributed under the License is distributed on an "AS IS" BASIS,
#   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#   See the License for the specific language governing permissions and
#   limitations under the License.

import sys
import logging
import signal
import json
import base64
from datetime import timedelta

from twisted.internet import reactor
from twisted.web.server import Site
from twisted.web.resource import Resource
from autobahn.websocket import WebSocketServerFactory, \
    WebSocketServerProtocol
from autobahn.resource import WebSocketResource, HTTPChannelHixie76Aware


DELTA = timedelta(weeks=1)
SAFE_WAIT = 3
START_COUNT = 10
VALID_AUTH_TOKENS = ["asdf", "asdf1", "asdf2", "asdf3"]


def websocket_func(logger, host, port):
    cons = list()

    # noinspection PyUnusedLocal
    def sigint_handler(signum, frame):
        reactor.stop()

    signal.signal(signal.SIGINT, sigint_handler)

    def send_to_all(msg, except_connection=None):
        if except_connection:
            logger.debug("Sending to all except %d message: %s", id(except_connection), msg)
        else:
            logger.debug("Sending to all: %s", msg)
        json_msg = json.dumps(msg)
        for con in cons:
            if con == except_connection:
                continue
            logger.debug("Sending to %d message: %s", id(con), msg)
            con.sendMessage(json_msg, False)

    class EchoServerProtocol(WebSocketServerProtocol):

        def __init__(self):
            pass

        def send_error(self, error_message):
            logger.error(error_message)
            self.send_to_self({"type": "error", "response": error_message})

        def send_to_self(self, msg):
            logger.debug("Sending to self: %s", msg)
            json_msg = json.dumps(msg)
            self.sendMessage(json_msg, False)

        def send_to_others(self, msg):
            send_to_all(msg, self)

        def onMessage(self, msg, binary):
            if binary:
                return
            try:
                data = json.loads(msg)
                data_type = str(data["type"])
                if "ping" == data_type:
                    self.on_message_ping(data)
                elif "register" == data_type:
                    self.on_message_register(data)
                elif "data" == data_type:
                    self.on_message_data(data)
                else:
                    self.send_error("Received unknown message type: %s" % data_type)
            except Exception as e:
                self.send_error("Error: %s, in message: %s" % (str(e), msg))

        def on_message_data(self, data):
            self.send_to_self({
                "type": "data",
                "id": data["id"],
                "message": base64.b64encode(data["message"])
            })
            self.send_to_others({
                "type": "chat",
                "from": str(id(self)),
                "message": data["message"]
            })

        def on_message_register(self, data):
            auth_token = data["auth_token"]
            if auth_token in VALID_AUTH_TOKENS:
                cons.append(self)
                self.send_to_self({"type": "registered", "response": "you are cool"})
            else:
                self.send_error("Wrong auth token")

        def on_message_ping(self, data):
            self.send_to_self({"type": "pong", "message": data["message"]})

        def onClose(self, was_clean, code, reason):
            logger.debug("Disconnected: %s" % id(self))
            if self in cons:
                cons.remove(self)

        def onOpen(self):
            logger.debug("Connected: %s" % id(self))

    url = "ws://%s:%d" % (host, port)

    factory = WebSocketServerFactory(url)
    factory.protocol = EchoServerProtocol
    factory.setProtocolOptions(allowHixie76=True)

    resource = WebSocketResource(factory)

    root = Resource()
    root.putChild("ws", resource)

    site = Site(root)
    site.protocol = HTTPChannelHixie76Aware

    reactor.listenTCP(port, site)
    logger.info("listening on %s/ws" % url)
    reactor.run()


if __name__ == '__main__':
    handler = logging.StreamHandler(sys.stdout)
    handler.setFormatter(logging.Formatter('%(asctime)s - %(name)s - %(levelname)s - %(message)s'))
    logger = logging.getLogger()
    logger.setLevel(logging.DEBUG)
    logger.addHandler(handler)

    websocket_func(logger, "localhost", 8080)
