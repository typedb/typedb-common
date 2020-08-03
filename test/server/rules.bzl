#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
#

def grakn_test(grakn_artifact = None,
               deps = [],
               classpath_resources = [],
               data = [],
               **kwargs):

    new_data = [] + data

    if grakn_artifact != None:
        new_data += [grakn_artifact]

    location = "$(location {})".format(grakn_artifact) if grakn_artifact != None else "none"

    native.java_test(
        deps = depset(deps + [
            "@graknlabs_common//test/server:grakn-setup",
        ]).to_list(),
        classpath_resources = depset(classpath_resources + [
            "@graknlabs_common//test/server:logback",
        ]).to_list(),
        data = depset(new_data).to_list(),
        args = [
            location,
        ],
        **kwargs
    )

