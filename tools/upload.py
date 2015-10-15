#!/usr/bin/env python
#
# Copyright 2014 Jacek Marchwicki <jacek.marchwicki@gmail.com>

__author__ = 'Jacek Marchwicki <jacek.marchwicki@gmail.com>'


import argparse
import sys
import urlparse
import urllib2
import json
import subprocess


def main():
    parser = argparse.ArgumentParser(description='Manage task.')
    parser.add_argument('--token', dest='token', required=True, nargs="?",
                        type=str, help='token')
    parser.add_argument('--build-name', dest='build_name', required=True, nargs="?",
                        type=str, help='build name')
    parser.add_argument('--base-url', dest='base_url', nargs="?",
                        type=str, help='base url', default="https://auto-close.appspot.com/")
    parser.add_argument('files', metavar='FILES', type=str, nargs="+",
                        help='files to upload')
    args = parser.parse_args()

    response = execute(urlparse.urljoin(args.base_url, "build"), {
        "token": args.token,
        "build_name": args.build_name
    })

    url = response["upload"]["url"]
    download_url = response["download"]

    command = "tar jcvf - %s | curl --request PUT --upload-file - \"%s\"" % (" ".join(args.files), url)
    subprocess.check_call(command, shell=True)

    print "Serving artifacts at: %s" % download_url

handler = urllib2.HTTPSHandler(debuglevel=0)
opener = urllib2.build_opener(handler)


def execute(base_url, data):
    headers = {
        "Content-Type": "application/json"
    }
    request = urllib2.Request(base_url, json.dumps(data), headers)
    try:
        response = opener.open(request)
        return json.loads(response.read())
    except urllib2.HTTPError as e:
        print >> sys.stderr, "Response from server: %s" % e.read()
        raise e


if __name__ == '__main__':
    main()
