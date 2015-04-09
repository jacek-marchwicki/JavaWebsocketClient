# -*- coding: utf-8 -*-
#   Copyright 2015 Jacek Marchwicki <jacek.marchwicki@gmail.com>
#
#   Licensed under the Apache License, Version 2.0 (the "License");
#   you may not use this file except in compliance with the License.
#   You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
#   Unless required by applicable law or agreed to in writing, software
#   distributed under the License is distributed on an "AS IS" BASIS,
#   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#   See the License for the specific language governing permissions and
#   limitations under the License.

import websocket
import logging
import logging.handlers
import sys
import argparse
import os
import pwd


def main():
  parser = argparse.ArgumentParser(description='Example server.')
  parser.add_argument('--host', nargs='?', dest='host',
    default="*", type=str,
    help='host to listen on (default: *)')
  parser.add_argument('--port', nargs='?', dest='port', type=int,
    default=8080,
    help='port to listen on (default: 8080)')
  parser.add_argument('--debug', dest='debug', action='store_const',
    const=True, default=False,
    help='debug')
  parser.add_argument('--stdio', dest='stdio', action='store_const',
    const=True, default=False,
    help='use stdio instead of syslog')
  parser.add_argument('--pidfile', nargs='?', dest='pidfile', type=str,
    required=False, help='create pid file when created')
  parser.add_argument('--syslog', nargs='?', dest='syslog', type=str,
    required=False, help='connect to syslog file')
  parser.add_argument('--username', nargs='?', dest='username',
    default=None, type=str,
    help='username to chroot (default does not chroot)')
  args = parser.parse_args()

  logger = logging.getLogger("websockets-server")
  logger.setLevel(logging.DEBUG if args.debug else logging.INFO)
  formatter = logging.Formatter("%(asctime)s - %(name)s - %(levelname)s - %(message)s")

  if args.stdio:
    handler = logging.StreamHandler(sys.stdout)
    handler.setFormatter(formatter)
    logger.addHandler(handler)

  if args.syslog:
    handler = logging.handlers.SysLogHandler(
        address=args.syslog,
        facility=logging.handlers.SysLogHandler.LOG_DAEMON)
    handler.setFormatter(formatter)
    logger.addHandler(handler)

  if args.pidfile:
    pid = str(os.getpid())
    f = open(args.pidfile, 'w')
    f.write(pid)
    f.close()

  if args.username:
    user = pwd.getpwnam(args.username)
    new_uid = user.pw_uid
    os.setuid(new_uid)

  logger.info("starting")
  websocket.websocket_func(logger, args.host, args.port)

if __name__ == '__main__':
  main()
