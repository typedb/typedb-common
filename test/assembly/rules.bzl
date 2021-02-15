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

def server_java_test(name, server_mac_artifact, server_linux_artifact, server_windows_artifact, deps = [], classpath_resources = [], data = [], **kwargs):
    native_server_artifacts = {
       "@graknlabs_dependencies//util/platform:is_mac": server_mac_artifact,
       "@graknlabs_dependencies//util/platform:is_linux": server_linux_artifact,
       "@graknlabs_dependencies//util/platform:is_windows": server_windows_artifact,
    }
    native_artifact_paths, native_artifact_labels = native_artifact_paths_and_labels(native_server_artifacts)
    native.java_test(
        name = name,
        deps = depset(deps + ["@graknlabs_common//test/assembly:server-runner"]).to_list(),
        classpath_resources = depset(classpath_resources + ["@graknlabs_common//test/assembly:logback"]).to_list(),
        data = data + select(native_artifact_labels),
        args = select(native_artifact_paths),
        **kwargs
    )

def console_java_test(name,
                      server_mac_artifact, server_linux_artifact, server_windows_artifact,
                      console_mac_artifact, console_linux_artifact, console_windows_artifact,
                      deps = [], classpath_resources = [], data = [], **kwargs):
    native_server_artifacts = {
       "@graknlabs_dependencies//util/platform:is_mac": server_mac_artifact,
       "@graknlabs_dependencies//util/platform:is_linux": server_linux_artifact,
       "@graknlabs_dependencies//util/platform:is_windows": server_windows_artifact,
    }
    native_console_artifacts = {
       "@graknlabs_dependencies//util/platform:is_mac": console_mac_artifact,
       "@graknlabs_dependencies//util/platform:is_linux": console_linux_artifact,
       "@graknlabs_dependencies//util/platform:is_windows": console_windows_artifact,
    }
    native_artifact_paths, native_artifact_labels = native_artifact_paths_and_labels(native_server_artifacts, native_console_artifacts)
    native.java_test(
        name = name,
        deps = depset(deps + ["@graknlabs_common//test/assembly:console-runner"]).to_list(),
        classpath_resources = depset(classpath_resources + ["@graknlabs_common//test/assembly:logback"]).to_list(),
        data = data + select(native_artifact_labels),
        args = select(native_artifact_paths),
        **kwargs
    )

def native_artifact_paths_and_labels(*artifacts):
    platforms = [ "@graknlabs_dependencies//util/platform:is_mac",
                  "@graknlabs_dependencies//util/platform:is_linux",
                  "@graknlabs_dependencies//util/platform:is_windows"]
    native_artifact_paths = {}
    native_artifact_labels = {}
    for platform in platforms:
        native_artifact_paths[platform] = []
        native_artifact_labels[platform] = []
        for artifact in artifacts:
            native_artifact_paths[platform].append("$(location {})".format(artifact[platform]))
            native_artifact_labels[platform].append(Label(artifact[platform], relative_to_caller_repository=True))
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
