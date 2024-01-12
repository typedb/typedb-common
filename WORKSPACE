#
# Copyright (C) 2022 Vaticle
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

workspace(name = "vaticle_typedb_common")

################################
# Load @vaticle_dependencies #
################################

load("//dependencies/vaticle:repositories.bzl", "vaticle_dependencies")
vaticle_dependencies()

# Load //builder/bazel
load("@vaticle_dependencies//builder/bazel:deps.bzl", "bazel_toolchain")
bazel_toolchain()

# Load //builder/java
load("@vaticle_dependencies//builder/java:deps.bzl", java_deps = "deps")
java_deps()

# Load //builder/kotlin
load("@vaticle_dependencies//builder/kotlin:deps.bzl", kotlin_deps = "deps")
kotlin_deps()
load("@io_bazel_rules_kotlin//kotlin:repositories.bzl", "kotlin_repositories")
kotlin_repositories()
load("@io_bazel_rules_kotlin//kotlin:core.bzl", "kt_register_toolchains")
kt_register_toolchains()

# Load //builder/python
load("@vaticle_dependencies//builder/python:deps.bzl", python_deps = "deps")
python_deps()

# Load //tool/common
load("@vaticle_dependencies//tool/common:deps.bzl", "vaticle_dependencies_ci_pip",
vaticle_dependencies_tool_maven_artifacts = "maven_artifacts")
vaticle_dependencies_ci_pip()

# Load //tool/checkstyle
load("@vaticle_dependencies//tool/checkstyle:deps.bzl", checkstyle_deps = "deps")
checkstyle_deps()

# Load //tool/unuseddeps
load("@vaticle_dependencies//tool/unuseddeps:deps.bzl", unuseddeps_deps = "deps")
unuseddeps_deps()

# Load //tool/sonarcloud
load("@vaticle_dependencies//tool/sonarcloud:deps.bzl", "sonarcloud_dependencies")
sonarcloud_dependencies()

######################################
# Load @vaticle_bazel_distribution #
######################################

load("@vaticle_dependencies//distribution:deps.bzl", "vaticle_bazel_distribution")
vaticle_bazel_distribution()

# Load //common
load("@vaticle_bazel_distribution//common:deps.bzl", "rules_pkg")
rules_pkg()
load("@rules_pkg//:deps.bzl", "rules_pkg_dependencies")
rules_pkg_dependencies()

# Load //github
load("@vaticle_bazel_distribution//github:deps.bzl", github_deps = "deps")
github_deps()

# Load @vaticle_bazel_distribution_cloudsmith
load("@vaticle_bazel_distribution//common/cloudsmith:deps.bzl", cloudsmith_deps = "deps")
cloudsmith_deps()
load("@vaticle_bazel_distribution_cloudsmith//:requirements.bzl", install_cloudsmith_deps = "install_deps")
install_cloudsmith_deps()

################################
# Load @vaticle dependencies #
################################

load("//dependencies/maven:artifacts.bzl", vaticle_typedb_common_artifacts = "artifacts")

############################
# Load @maven dependencies #
############################

load("@vaticle_dependencies//library/maven:rules.bzl", "maven")
load("@vaticle_bazel_distribution//maven:deps.bzl", vaticle_bazel_distribution_maven_artifacts = "maven_artifacts")

maven(vaticle_typedb_common_artifacts + vaticle_dependencies_tool_maven_artifacts + vaticle_bazel_distribution_maven_artifacts)

#############################################
# Generate @vaticle_typedb_common_workspace_refs #
#############################################

load("@vaticle_bazel_distribution//common:rules.bzl", "workspace_refs")
workspace_refs(
    name = "vaticle_typedb_common_workspace_refs"
)
