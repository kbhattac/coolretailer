#!/usr/bin/python
#
# Copyright 2018 Google LLC
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

import random
import uuid
import logging
import os
import string
from locust import HttpLocust, TaskSet
from locust.events import request_failure
from binascii import hexlify

prefixes = [
    'goo',
    'mic',
    'app',
    'fgssg',
    'int',
    'mo',
    'ewer',
    'am',
    'ma',
    'ip',
    'sdf'
]

USER_AGENTS = [
    "Mozilla/5.0 (Linux; Android 4.1.1; Nexus 7 Build/JRO03D) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.166 Safari/535.19 (LocustIO)",
    "Android 4.0.3;AppleWebKit/534.30;Build/IML74K;GT-I9220 Build/IML74K (LocustIO)",
    "KWC-S4000/ UP.Browser/7.2.6.1.794 (GUI) MMP/2.0 (LocustIO)",
    "Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html) (LocustIO)",
    "Googlebot-Image/1.0 (LocustIO)",
    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.8; rv:24.0) Gecko/20100101 Firefox/24.0 (LocustIO)",
    "Opera/9.80 (Macintosh; Intel Mac OS X 10.6.8; U; fr) Presto/2.9.168 Version/11.52 (LocustIO)",
    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_10_3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/42.0.2311.152 Safari/537.36",
]


class B3Header:
    def generateHeader(self, identifier_length):
        bit_length = identifier_length * 4
        byte_length = int(bit_length / 8)
        identifier = os.urandom(byte_length)
        return hexlify(identifier).decode('ascii')


b3 = B3Header()


def on_failure(request_type, name, response_time, exception, **kwargs):
    logging.error(exception.request)
    logging.error(exception.response)


request_failure += on_failure


def findProduct(l):
    h = {
        "User-Agent": random.choice(USER_AGENTS),
        "x-client-trace-id": str(uuid.uuid4()),
        "x-b3-sampled": "1",
        "x-b3-flags": "1",
        "x-b3-traceid": b3.generateHeader(32),
        "x-b3-spanid": b3.generateHeader(16)
    }
    # logging.info(h)
    nonexisting = "".join(random.choice(string.ascii_lowercase)
                          for x in range(5))
    prefixes.append(nonexisting)
    l.client.get("/api/fetchProducts?name=" +
                 random.choice(prefixes), headers=h)
    prefixes.remove(nonexisting)


class UserBehavior(TaskSet):
    tasks = {findProduct: 1}


class WebsiteUser(HttpLocust):

    task_set = UserBehavior
    min_wait = 1000
    max_wait = 10000
