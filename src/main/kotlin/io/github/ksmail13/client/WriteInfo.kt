package io.github.ksmail13.client

import java.nio.ByteBuffer
import java.util.concurrent.CompletableFuture

typealias WriteInfo = Pair<ByteBuffer, CompletableFuture<Void>>