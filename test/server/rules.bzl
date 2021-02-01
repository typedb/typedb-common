#
# Copyright (C) 2021 Grakn Labs
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

def grakn_java_test(name, mac_artifact, linux_artifact, windows_artifact, deps = [], classpath_resources = [], data = [], **kwargs):
    native_grakn_artifacts = {
       "@graknlabs_dependencies//util/platform:is_mac": mac_artifact,
       "@graknlabs_dependencies//util/platform:is_linux": linux_artifact,
       "@graknlabs_dependencies//util/platform:is_windows": windows_artifact,
    }
    native_grakn_artifact_paths = {}
    native_grakn_artifact_labels = {}
    for key in native_grakn_artifacts.keys():
        native_grakn_artifact_paths[key] = [ "$(location {})".format(native_grakn_artifacts[key]) ]
        native_grakn_artifact_labels[key] = [ Label(native_grakn_artifacts[key], relative_to_caller_repository=True) ]
    native.java_test(
        name = name,
        deps = depset(deps + ["@graknlabs_common//test/server:grakn-runner"]).to_list(),
        classpath_resources = depset(classpath_resources + ["@graknlabs_common//test/server:logback"]).to_list(),
        data = data + select(native_grakn_artifact_labels),
        args = select(native_grakn_artifact_paths),
        **kwargs
    )

def native_grakn_artifact(name, mac_artifact, linux_artifact, windows_artifact, output, **kwargs):
    native.genrule(
        name = name,
        outs = [output],
        srcs = select({
            "@graknlabs_dependencies//util/platform:is_mac": [mac_artifact],
            "@graknlabs_dependencies//util/platform:is_linux": [linux_artifact],
            "@graknlabs_dependencies//util/platform:is_windows": [windows_artifact],
        }, no_match_error = "There is no Grakn Core artifact compatible with this operating system. Supported operating systems are Mac and Linux."),
        cmd = "read -a srcs <<< '$(SRCS)' && read -a outs <<< '$(OUTS)' && cp $${srcs[0]} $${outs[0]} && echo $${outs[0]}",
        **kwargs
    )
