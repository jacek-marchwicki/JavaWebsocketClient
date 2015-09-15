#!/usr/bin/env python
#
# Copyright 2014 Jacek Marchwicki <jacek.marchwicki@gmail.com>

__author__ = 'Jacek Marchwicki <jacek.marchwicki@gmail.com>'


import argparse
import subprocess
import urlparse


def main():
    parser = argparse.ArgumentParser(description='Manage task.')
    parser.add_argument('--token', dest='token', required=True, nargs="?",
                        type=str, help='token')
    parser.add_argument('--key-version', dest='key_version', required=False, nargs="?",
                        type=str, help='key_version')
    parser.add_argument('--base-url', dest='base_url', nargs="?",
                        type=str, help='base url', default="https://auto-close.appspot.com/")
    args = parser.parse_args()

    if args.key_version:
        request = "build/keys?token=%s&version=%s" % (args.token, args.key_version)
    else:
        request = "build/keys?token=%s" % (args.token, )

    path = urlparse.urljoin(args.base_url, request)
    command = "curl --location --silent \"%s\" | tar -jxvf -" % (path)
    subprocess.check_call(command, shell=True)


if __name__ == '__main__':
    main()
