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
package ru.korpys667.api.event.internal

import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ru.korpys667.api.event.MKACCancellableEvent
import ru.korpys667.api.event.MKACEvent
import ru.korpys667.api.event.MKACEventListener

class MKACEventBusImplTest {

  private lateinit var bus: MKACEventBusImpl
  private val context = Any()

  @BeforeEach
  fun setUp() {
    bus = MKACEventBusImpl()
  }

  // --- Test event types ---

  private class SimpleEvent : MKACEvent

  private open class ParentEvent : MKACEvent

  private class ChildEvent : ParentEvent()

  private class CancellableTestEvent : MKACCancellableEvent {
    override var cancelled: Boolean = false
  }

  // --- Basic dispatch ---

  @Test
  fun `post delivers event to subscriber`() {
    val received = mutableListOf<MKACEvent>()
    bus.subscribe(context, SimpleEvent::class.java, MKACEventListener { received.add(it) })

    bus.post(SimpleEvent())
    assertEquals(1, received.size)
  }

  @Test
  fun `post with no subscribers does not throw`() {
    bus.post(SimpleEvent())
  }

  @Test
  fun `multiple subscribers all receive event`() {
    var count = 0
    bus.subscribe(context, SimpleEvent::class.java, MKACEventListener { count++ })
    bus.subscribe(context, SimpleEvent::class.java, MKACEventListener { count++ })
    bus.subscribe(context, SimpleEvent::class.java, MKACEventListener { count++ })

    bus.post(SimpleEvent())
    assertEquals(3, count)
  }

  // --- Priority ordering ---

  @Test
  fun `higher priority listeners execute first`() {
    val order = mutableListOf<String>()

    bus.subscribe(
      context,
      SimpleEvent::class.java,
      MKACEventListener { order.add("low") },
      priority = 1,
      ignoreCancelled = false,
    )
    bus.subscribe(
      context,
      SimpleEvent::class.java,
      MKACEventListener { order.add("high") },
      priority = 10,
      ignoreCancelled = false,
    )
    bus.subscribe(
      context,
      SimpleEvent::class.java,
      MKACEventListener { order.add("mid") },
      priority = 5,
      ignoreCancelled = false,
    )

    bus.post(SimpleEvent())
    assertEquals(listOf("high", "mid", "low"), order)
  }

  @Test
  fun `same priority listeners all execute`() {
    var count = 0
    bus.subscribe(
      context,
      SimpleEvent::class.java,
      MKACEventListener { count++ },
      priority = 5,
      ignoreCancelled = false,
    )
    bus.subscribe(
      context,
      SimpleEvent::class.java,
      MKACEventListener { count++ },
      priority = 5,
      ignoreCancelled = false,
    )

    bus.post(SimpleEvent())
    assertEquals(2, count)
  }

  // --- Cancellable events ---

  @Test
  fun `cancelled event skips non-ignoreCancelled listeners`() {
    val received = mutableListOf<String>()

    bus.subscribe(
      context,
      CancellableTestEvent::class.java,
      MKACEventListener {
        it.cancelled = true
        received.add("canceller")
      },
      priority = 10,
      ignoreCancelled = false,
    )

    bus.subscribe(
      context,
      CancellableTestEvent::class.java,
      MKACEventListener { received.add("skipped") },
      priority = 1,
      ignoreCancelled = false,
    )

    bus.post(CancellableTestEvent())
    assertEquals(listOf("canceller"), received)
  }

  @Test
  fun `ignoreCancelled listener still receives cancelled event`() {
    val received = mutableListOf<String>()

    bus.subscribe(
      context,
      CancellableTestEvent::class.java,
      MKACEventListener {
        it.cancelled = true
        received.add("canceller")
      },
      priority = 10,
      ignoreCancelled = false,
    )

    bus.subscribe(
      context,
      CancellableTestEvent::class.java,
      MKACEventListener { received.add("monitor") },
      priority = 1,
      ignoreCancelled = true,
    )

    bus.post(CancellableTestEvent())
    assertEquals(listOf("canceller", "monitor"), received)
  }

  // --- Hierarchy dispatch ---

  @Test
  fun `child event dispatched to parent subscriber`() {
    var parentReceived = false
    bus.subscribe(context, ParentEvent::class.java, MKACEventListener { parentReceived = true })

    bus.post(ChildEvent())
    assertTrue(parentReceived)
  }

  @Test
  fun `child event dispatched to both child and parent subscribers`() {
    val received = mutableListOf<String>()
    bus.subscribe(context, ChildEvent::class.java, MKACEventListener { received.add("child") })
    bus.subscribe(context, ParentEvent::class.java, MKACEventListener { received.add("parent") })

    bus.post(ChildEvent())
    assertTrue(received.contains("child"))
    assertTrue(received.contains("parent"))
  }

  // --- Unregister ---

  @Test
  fun `unregisterListener removes specific listener`() {
    var count = 0
    val listener = MKACEventListener<SimpleEvent> { count++ }
    bus.subscribe(context, SimpleEvent::class.java, listener)

    bus.post(SimpleEvent())
    assertEquals(1, count)

    bus.unregisterListener(context, listener)
    bus.post(SimpleEvent())
    assertEquals(1, count)
  }

  @Test
  fun `unregisterAll removes all listeners for context`() {
    var count = 0
    val ctx1 = Any()
    val ctx2 = Any()
    bus.subscribe(ctx1, SimpleEvent::class.java, MKACEventListener { count++ })
    bus.subscribe(ctx2, SimpleEvent::class.java, MKACEventListener { count++ })

    bus.post(SimpleEvent())
    assertEquals(2, count)

    bus.unregisterAll(ctx1)
    bus.post(SimpleEvent())
    assertEquals(3, count) // only ctx2 listener fires
  }

  @Test
  fun `unregisterAll with unknown context does not throw`() {
    bus.unregisterAll(Any())
  }

  // --- Error isolation ---

  @Test
  fun `listener exception does not prevent other listeners from running`() {
    val received = mutableListOf<String>()

    bus.subscribe(
      context,
      SimpleEvent::class.java,
      MKACEventListener { received.add("before") },
      priority = 10,
      ignoreCancelled = false,
    )

    bus.subscribe(
      context,
      SimpleEvent::class.java,
      MKACEventListener { throw IllegalStateException("boom") },
      priority = 5,
      ignoreCancelled = false,
    )

    bus.subscribe(
      context,
      SimpleEvent::class.java,
      MKACEventListener { received.add("after") },
      priority = 1,
      ignoreCancelled = false,
    )

    bus.post(SimpleEvent())
    assertEquals(listOf("before", "after"), received)
  }
}
