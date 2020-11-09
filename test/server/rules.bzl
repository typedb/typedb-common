#
# Copyright (C) 2020 Grakn Labs
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU Affero General Public License as
# published by the Free Software Foundation, either version 3 of the
# License, or (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU Affero General Public License for more details.
#
# You should have received a copy of the GNU Affero General Public License
# along with this program.  If not, see <https://www.gnu.org/licenses/>.
#

def grakn_java_test(grakn_artifact = None,
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

def grakn_node_test (name = "", feature_label = "@graknlabs_behaviour//"):
    native.sh_test (
        name = name,
        data = [
            "//:node_modules",
            "//:package.json",
            ":behaviour",
            feature_label,
        ],
        srcs = ["cucumber_test.sh"],
    )
