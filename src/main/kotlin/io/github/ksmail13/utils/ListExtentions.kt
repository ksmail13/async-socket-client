package io.github.ksmail13.utils

import java.util.*
import kotlin.collections.ArrayList

fun <T> List<T>.toReadonlyList(): List<T> = Collections.unmodifiableList(this)