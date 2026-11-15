/*
 * This file is part of MKAC - https://github.com/korpys667/MKAC
 * Copyright (C) 2026 korpys667
 *
 * MKAC is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MKAC is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package ru.korpys667.ai

import io.mockk.every
import io.mockk.mockk
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import org.junit.jupiter.api.Test
import ru.korpys667.server.AIServer
import ru.korpys667.server.AIServerProvider

class ReconfigureChannelTest {

  @Test
  fun `every negotiated field the server can correct survives the 422 channel`() {
    val details =
      mapOf<String, Any?>(
        "expected_pre_window" to 32,
        "expected_labels" to listOf("aim", "trigger"),
        "expected_label_titles" to mapOf("aim" to "Aim Assist"),
        "expected_legit_labels" to listOf("clean"),
        "expected_label_mode" to "multilabel",
        "expected_label_thresholds" to mapOf("aim" to mapOf("cheat" to 0.85, "legit" to 0.15)),
        "expected_model_title" to "MKAC 2",
      )

    val failure = assertFailsWith<CompletionException> { requestWith(details).join() }
    val negotiated = failure.cause as AiServiceException

    assertEquals(32, negotiated.newPreWindow)
    assertEquals(listOf("aim", "trigger"), negotiated.newLabels)
    assertEquals(mapOf("aim" to "Aim Assist"), negotiated.newLabelNames)
    assertEquals(listOf("clean"), negotiated.newLegitLabels)
    assertEquals("multilabel", negotiated.newLabelMode)
    assertEquals(mapOf("cheat" to 0.85, "legit" to 0.15), negotiated.newLabelThresholds?.get("aim"))
    assertEquals(
      "MKAC 2",
      negotiated.newModelTitle,
      "a field this channel drops leaves the operator looking at the previous model's name",
    )
  }

  private fun requestWith(details: Map<String, Any?>): CompletableFuture<AiResult> {
    val cause =
      AIServer.RequestException(
        AIServer.ResponseCode.RECONFIGURE_REQUIRED,
        "reconfigure",
        details = details,
      )
    val transport =
      object : AiTransport {
        override fun send(payload: ByteArray): CompletableFuture<String> =
          CompletableFuture.failedFuture(cause)
      }
    val provider = mockk<AIServerProvider>()
    every { provider.get() } returns transport
    return DefaultAiService(provider, JacksonAiResponseParser()).request(byteArrayOf(1))
  }
}
