/*
 * Copyright 2014-2021 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.tests.server.netty

import io.ktor.application.*
import io.ktor.config.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.mockk.*
import io.netty.channel.nio.*
import kotlinx.coroutines.*
import org.junit.*
import java.util.concurrent.*

class NettyConfigurationTest {
    private val environment: ApplicationEngineEnvironment get() {
        val config = MapApplicationConfig()
        val events = ApplicationEvents()

        val env = mockk<ApplicationEngineEnvironment>()
        every { env.developmentMode } returns false
        every { env.config } returns config
        every { env.monitor } returns events
        every { env.stop() } just Runs
        every { env.start() } just Runs
        every { env.connectors } returns listOf(EngineConnectorBuilder())
        every { env.parentCoroutineContext } returns Dispatchers.Default
        return env
    }

    @Test
    fun configuredChildAndParentGroupShutdownGracefully() {
        val parentGroup = spyk(NioEventLoopGroup())
        val childGroup = spyk(NioEventLoopGroup())

        val engine = NettyApplicationEngine(environment) {
            configureBootstrap = {
                group(parentGroup, childGroup)
            }
        }

        engine.stop(10, 10)
        verify { parentGroup.shutdownGracefully(10, 10, TimeUnit.MILLISECONDS) }
        verify { childGroup.shutdownGracefully(10, 10, TimeUnit.MILLISECONDS) }
    }
}
