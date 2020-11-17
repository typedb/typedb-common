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

def grakn_java_test(name, grakn_artifact_linux, grakn_artifact_mac, deps = [], classpath_resources = [], data = [], **kwargs):

    native.genrule(
        name = "native-grakn-artifact",
        outs = ["grakn-core-server-native.tar.gz"],
        srcs = select({
            "@graknlabs_dependencies//util/platform:is_mac": [grakn_artifact_mac],
            "@graknlabs_dependencies//util/platform:is_linux": [grakn_artifact_linux],
        }, no_match_error = "There is no Grakn Core artifact compatible with this operating system. Supported operating systems are Mac and Linux."),
        cmd = "read -a srcs <<< '$(SRCS)' && read -a outs <<< '$(OUTS)' && cp $${srcs[0]} $${outs[0]}",
    )

    native.java_test(
        name = name,
        deps = depset(deps + ["@graknlabs_common//test/server:grakn-setup"]).to_list(),
        classpath_resources = depset(classpath_resources + ["@graknlabs_common//test/server:logback"]).to_list(),
        data = depset(data + [":native-grakn-artifact"]).to_list(),
        args = ["$(location :native-grakn-artifact)"],
        **kwargs
    )
