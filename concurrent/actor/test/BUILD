java_test(
    name = "actor",
    srcs = ["ActorTest.java"],
    deps = [
        "//common/concurrent/actor:lib",
        "//common/concurrent/actor/testkit:lib",
        "@maven//:junit_junit",
        "@maven//:org_slf4j_slf4j_api",
    ],
    runtime_deps = [
        "@maven//:ch_qos_logback_logback_classic",
    ],
    test_class = "grakn.cluster.execution.ActorTest",
)

java_test(
    name = "event-loop",
    srcs = ["EventLoopTest.java"],
    deps = [
        "//common/concurrent/actor:lib",
        "@maven//:junit_junit",
        "@maven//:org_slf4j_slf4j_api",
    ],
    runtime_deps = [
        "@maven//:ch_qos_logback_logback_classic",
    ],
    test_class = "grakn.cluster.execution.EventLoopTest",
)
