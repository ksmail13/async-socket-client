package io.github.ksmail13.logging

import org.slf4j.Logger
import org.slf4j.LoggerFactory

interface Logging {
    val log: Logger
        get() = LoggerFactory.getLogger(this::class.java)
}