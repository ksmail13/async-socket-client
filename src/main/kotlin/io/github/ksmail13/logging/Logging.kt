package io.github.ksmail13.logging

import java.util.logging.ConsoleHandler
import java.util.logging.Level
import java.util.logging.Logger

fun initLog(logger: Logger): Logger {
    logger.addHandler(ConsoleHandler().apply { level = Level.ALL })
    return logger
}