package coralsum.toolkit

import org.slf4j.LoggerFactory

inline fun <reified T> logger() = LoggerFactory.getLogger(T::class.java)