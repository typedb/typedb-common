#
# Copyright (C) 2021 Grakn Labs
#
# This unpublished material is proprietary to Grakn Labs.
# All rights reserved. The methods and
# techniques described herein are considered trade secrets
# and/or confidential. Reproduction or distribution, in whole
# or in part, is forbidden except by express written permission
# of Grakn Labs.
#

load("@vaticle_bazel_distribution//common:rules.bzl", "assemble_targz")
load("@vaticle_dependencies//tool/checkstyle:rules.bzl", "checkstyle_test")

sed_replace_pattern = "s/com.vaticle.typedb.core.server.TypeDBServer/com.vaticle.typedb.cluster.server.TypeDBServer/g"

genrule(
    name = "bat",
    cmd = "sed -e '" + sed_replace_pattern + "' $(location //binary/core:typedb.bat) > $(OUTS)",
    outs = ["typedb.bat"],
    srcs = [
        "//binary/core:typedb.bat"
    ],
)

assemble_targz(
    name = "assemble-bat-targz",
    additional_files = {
        "bat": 'typedb.bat',
    },
    visibility = ["//visibility:public"]
)

genrule(
    name = "bash",
    cmd = "sed -e '" + sed_replace_pattern + "' $(location //binary/core:typedb) > $(OUTS)",
    outs = ["typedb"],
    srcs = [
        "//binary/core:typedb"
    ],
)

assemble_targz(
    name = "assemble-bash-targz",
    additional_files = {
        "bash": 'typedb',
    },
    permissions = {
        "typedb": "0755",
    },
    visibility = ["//visibility:public"]
)

checkstyle_test(
   name = "checkstyle",
   include = glob(["*"]),
   exclude = [],
   license_type = "commercial",
)

