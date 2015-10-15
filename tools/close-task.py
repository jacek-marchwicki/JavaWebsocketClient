#!/usr/bin/env python
#
# Copyright 2014 Jacek Marchwicki <jacek.marchwicki@gmail.com>

__author__ = 'Jacek Marchwicki <jacek.marchwicki@gmail.com>'


import subprocess
import re
import unittest
import argparse
import sys

import urllib2
import json


def main():
    parser = argparse.ArgumentParser(description='Manage task.')
    parser.add_argument('--token', dest='token', required=True, nargs='?',
                        type=str, help='token')
    parser.add_argument('--merge', dest='merge', action='store_const',
                        const=True, default=False, help='task is merged')
    parser.add_argument('--base-url', dest='base_url', nargs="?",
                        type=str, help='base url', default="https://auto-close.appspot.com/")
    parser.add_argument('message', metavar='MESSAGE', type=str, nargs=1,
                        help='an message added to task')
    args = parser.parse_args()

    message = args.message[0]
    commit_message = get_commit_message_lines()
    closed_urls = get_urls(commit_message, lambda line: get_url_by_keyword("Closes", line))
    referred_urls = get_urls(commit_message, lambda line: get_url_by_keyword("Refers", line))

    if "[wip]" in commit_message[0].lower():
        write_message(args.base_url, referred_urls, args.token, "Referred in [WIP] commit: " + message)
        write_message(args.base_url, closed_urls, args.token, "[WIP] commit pushed to review: " + message)
    elif args.merge:
        write_message(args.base_url, referred_urls, args.token, "Referred commit merged: " + message)
        close_urls(args.base_url, closed_urls, args.token, "Commit merged in review: " + message)
    else:
        write_message(args.base_url, referred_urls, args.token, "Referred in commit: " + message)
        review_urls(args.base_url, closed_urls, args.token, "Commit pushed to review: " + message)


handler = urllib2.HTTPSHandler(debuglevel=0)
opener = urllib2.build_opener(handler)


def write_message(base_url, urls, token, message):
    for url in urls:
        data = {
            "message": message,
            "token": token,
            "url": url
        }
        execute(base_url, data)


def close_urls(base_url, urls, token, message):
    for url in urls:
        data = {
            "message": message,
            "close": "true",
            "token": token,
            "url": url
        }
        execute(base_url, data)


def review_urls(base_url, urls, token, message):
    for url in urls:
        data = {
            "message": message,
            "review": "true",
            "token": token,
            "url": url
        }
        execute(base_url, data)


def execute(base_url, data):
    headers = {
        "Content-Type": "application/json"
    }
    request = urllib2.Request("%sclose" % base_url, json.dumps(data), headers)
    try:
        response = opener.open(request)
        return json.loads(response.read())
    except urllib2.HTTPError as e:
        print >> sys.stderr, "Response from server: %s" % e.read()
        raise e


def get_commit_message_lines():
    commit_message = subprocess.check_output(["git", "log", "-1", "--pretty=%B"]).splitlines()
    commit_message = [i.strip() for i in commit_message]
    return commit_message


def get_urls(commit_message, f):
    urls = []
    for commit_line in commit_message:
        url = f(commit_line)
        if url:
            urls.append(url)
    return urls


def do_work():
    commit_message = get_commit_message_lines()
    closed_urls = get_urls(commit_message, lambda line: get_url_by_keyword("Closes", line))
    refered_urls = get_urls(commit_message, lambda line: get_url_by_keyword("Refer", line))
    print closed_urls
    print refered_urls


def get_url_by_keyword(keyword, line):
    match = re.match("^%s:? ([\w:\/\.-]+)$" % keyword, line)
    if not match:
        return None
    return match.group(1)


class TestGetLinkMethod(unittest.TestCase):
    """
    Run unit tests: python -m unittest close-task
    """

    def test_valid_url(self):
        self.assertEqual(
            get_url_by_keyword("Closes", "Closes https://appunite.dobambam.com/project/100125/tasks/t2038"),
            'https://appunite.dobambam.com/project/100125/tasks/t2038')

    def test_refer_url(self):
        self.assertEqual(get_url_by_keyword("Refer", "Refer https://appunite.dobambam.com/project/100125/tasks/t2038"),
                         'https://appunite.dobambam.com/project/100125/tasks/t2038')

    def test_valid_close_with_semicolon(self):
        self.assertEqual(
            get_url_by_keyword("Closes", "Closes: https://appunite.dobambam.com/project/100125/tasks/t2038"),
            'https://appunite.dobambam.com/project/100125/tasks/t2038')

    def test_invalid_close_after_some_char(self):
        self.assertIsNone(
            get_url_by_keyword("Closes", "xCloses: https://appunite.dobambam.com/project/100125/tasks/t2038"))

    def test_invalid_url(self):
        self.assertIsNone(
            get_url_by_keyword("Closes", "Closes: https://appunite.dobambam.com/project/100125/tasks/t2038[]"))


if __name__ == '__main__':
    main()
