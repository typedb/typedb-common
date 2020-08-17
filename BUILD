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
# GNU Affero General Public License for more det@graknlabs_dependencies//tool/checkstyle:rules.bzlails.
#
# You should have received a copy of the GNU Affero General Public License
# along with this program.  If not, see <https://www.gnu.org/licenses/>.
#

exports_files(["VERSION"], visibility = ["//visibility:public"])
load("@graknlabs_bazel_distribution//github:rules.bzl", "deploy_github")
load("@graknlabs_bazel_distribution//maven:rules.bzl", "assemble_maven", "deploy_maven")
load("@graknlabs_dependencies//library/maven:artifacts.bzl", "maven_overrides", maven_overrides_org = "artifacts")
load("@graknlabs_dependencies//tool/checkstyle:rules.bzl", "checkstyle_test")


java_library(
    name = "common",
    srcs = glob([
        "collection/*.java",
        "concurrent/*.java",
        "exception/*.java",
    ]),
    visibility = ["//visibility:public"],
    tags = [
        "maven_coordinates=io.grakn.common:grakn-common:{pom_version}",
    ],
)

deploy_github(
    name = "deploy-github",
    deployment_properties = "//:deployment.properties",
    title = "Grakn Common",
    title_append_version = True,
    release_description = "//:RELEASE_TEMPLATE.md",
)

assemble_maven(
    name = "assemble-maven",
    target = ":common",
    package = "common",
    workspace_refs = "@graknlabs_common_workspace_refs//:refs.json",
    version_overrides = maven_overrides(maven_overrides_org),
    project_name = "Grakn Common",
    project_description = "Grakn Common classes and tools",
    project_url = "https://github.com/graknlabs/common",
    scm_url = "https://github.com/graknlabs/common",
)

deploy_maven(
    name = "deploy-maven",
    target = ":assemble-maven",
    deployment_properties = "@graknlabs_dependencies//distribution:deployment.properties",
)

checkstyle_test(
    name = "checkstyle",
    targets = [":common"],
)