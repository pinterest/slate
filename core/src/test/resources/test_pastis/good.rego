package test_pastis

default allow = false

pii_topics = {
    "test_topic_1": {
        "CN=some-cn-here",
        "CN=some-other-cn-here-just-to-test-trailing-comma",
    },
    "test-topic-2": {},
    "testtopic3": {}
}

allow = true {
    pii_topics[input.topic]
}