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

load("@graknlabs_dependencies//builder/java:rules.bzl", "native_dep_for_host_platform")

def grakn_java_test(name, server_mac_artifact, server_linux_artifact, server_windows_artifact,
                      console_mac_artifact = None, console_linux_artifact = None, console_windows_artifact = None,
                      native_libraries_deps = [], deps = [], classpath_resources = [], data = [], **kwargs):
    native_server_artifact_paths, native_server_artifact_labels = native_artifact_paths_and_labels(
        server_mac_artifact, server_linux_artifact, server_windows_artifact
    )
    native_console_artifact_paths, native_console_artifact_labels = [], []
    if console_mac_artifact and console_linux_artifact and console_windows_artifact:
        native_console_artifact_paths, native_console_artifact_labels = native_artifact_paths_and_labels(
            console_mac_artifact, console_linux_artifact, console_windows_artifact
        )
    native_deps = []
    for dep in native_libraries_deps:
        native_deps = native_deps + native_dep_for_host_platform(dep)
    native.java_test(
        name = name,
        deps = depset(deps + ["@graknlabs_common//test:grakn-runner"]).to_list() + native_deps,
        classpath_resources = depset(classpath_resources + ["@graknlabs_common//test:logback"]).to_list(),
        data = data + select(native_server_artifact_labels) + (select(native_console_artifact_labels) if native_console_artifact_labels else []),
        args = select(native_server_artifact_paths) + (select(native_console_artifact_paths) if native_console_artifact_paths else []),
        **kwargs
    )

def native_artifact_paths_and_labels(mac_artifact, linux_artifact, windows_artifact):
    native_artifacts = {
       "@graknlabs_dependencies//util/platform:is_mac": mac_artifact,
       "@graknlabs_dependencies//util/platform:is_linux": linux_artifact,
       "@graknlabs_dependencies//util/platform:is_windows": windows_artifact,
    }
    native_artifact_paths = {}
    native_artifact_labels = {}
    for key in native_artifacts.keys():
        native_artifact_labels[key] = [ Label(native_artifacts[key], relative_to_caller_repository=True) ]
        native_artifact_paths[key] = [ "$(location {})".format(native_artifacts[key]) ]
    return native_artifact_paths, native_artifact_labels

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
