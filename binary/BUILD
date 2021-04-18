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
#

load("@graknlabs_bazel_distribution//apt:rules.bzl", "assemble_apt", "deploy_apt")
load("@graknlabs_bazel_distribution//common:rules.bzl", "assemble_targz")
load("@graknlabs_dependencies//distribution:deployment.bzl", "deployment")
load("@graknlabs_dependencies//tool/checkstyle:rules.bzl", "checkstyle_test")

exports_files(["grakn", "grakn.bat", "grakn-docker.sh", "grakn-core.service"])

assemble_targz(
    name = "assemble-bash-targz",
    additional_files = {
        "//binary:grakn": 'grakn',
    },
    permissions = {
        "grakn": "0755",
    },
    visibility = ["//visibility:public"]
)

assemble_targz(
    name = "assemble-apt-targz",
    additional_files = {
        "//binary:grakn-core.service": 'grakn-core.service',
    },
)

assemble_targz(
    name = "assemble-bat-targz",
    additional_files = {
        "//binary:grakn.bat": 'grakn.bat',
    },
    visibility = ["//visibility:public"]
)

assemble_apt(
    name = "assemble-linux-apt",
    package_name = "grakn-bin",
    maintainer = "Grakn Labs <community@grakn.ai>",
    description = "Grakn Core (binaries)",
    archives = [":assemble-bash-targz", ":assemble-apt-targz"],
    installation_dir = "/opt/grakn/core/",
    empty_dirs = [
        "var/log/grakn/",
    ],
    depends = [
        "openjdk-8-jre"
    ],
    permissions = {
        "var/log/grakn/": "0777",
    },
    symlinks = {
        "usr/local/bin/grakn": "/opt/grakn/core/grakn",
        "opt/grakn/core/server/logs": "/var/log/grakn/",
        "usr/lib/systemd/system/grakn-core.service": "/opt/grakn/core/grakn-core.service",
    },
)

deploy_apt(
    name = "deploy-apt",
    target = ":assemble-linux-apt",
    snapshot = deployment['apt.snapshot'],
    release = deployment['apt.release']
)

checkstyle_test(
    name = "checkstyle",
    include = glob(["*"]),
    license_type = "agpl",
)
