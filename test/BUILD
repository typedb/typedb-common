#
# Copyright (C) 2021 Vaticle
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

load("@vaticle_dependencies//tool/checkstyle:rules.bzl", "checkstyle_test")

java_library(
    name = "typedb-runner",
    srcs = glob(["*.java", "*/*.java"]),
    deps = [
        "//:common",
        "@maven//:commons_io_commons_io",
        "@maven//:junit_junit",
        "@maven//:org_zeroturnaround_zt_exec",
        "@maven//:org_slf4j_slf4j_api"
    ],
    visibility = ["//visibility:public"],
)

filegroup(
    name = "logback",
    srcs = ["logback.xml"],
    visibility = ["//visibility:public"],
)

checkstyle_test(
    name = "checkstyle",
    include = glob(["*", "core/*", "cluster/*"]),
    license_type = "agpl",
)
