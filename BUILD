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
load("@graknlabs_dependencies//distribution:deployment.bzl", "deployment")
load("//:deployment.bzl", deployment_github = "deployment")

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
    organisation = deployment_github['github.organisation'],
    repository = deployment_github['github.repository'],
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
    snapshot = deployment['maven.snapshot'],
    release = deployment['maven.release']
)

checkstyle_test(
    name = "checkstyle",
    targets = [":common"],
)

# CI targets that are not declared in any BUILD file, but are called externally
filegroup(
    name = "ci",
    data = [
        "@graknlabs_dependencies//library/maven:update",
        "@graknlabs_dependencies//tool/bazelrun:rbe",
        "@graknlabs_dependencies//tool/checkstyle:test-coverage",
        "@graknlabs_dependencies//tool/release:approval",
        "@graknlabs_dependencies//tool/release:create-notes",
        "@graknlabs_dependencies//tool/sonarcloud:code-analysis",
        "@graknlabs_dependencies//tool/sync:dependencies",
        "@graknlabs_dependencies//tool/unuseddeps:unused-deps",
    ]
)
