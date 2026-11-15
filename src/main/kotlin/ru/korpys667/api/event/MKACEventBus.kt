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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package ru.korpys667.api.event

/** Functional listener for [MKACEventBus] subscriptions. */
fun interface MKACEventListener<T : MKACEvent> {
  fun handle(event: T)
}

/**
 * Lightweight event bus for MKAC events.
 *
 * Events are dispatched on the thread that calls [post].
 */
interface MKACEventBus {
  /** Post an event to all registered listeners. */
  fun post(event: MKACEvent)

  /** Subscribe with default priority (0) and ignoreCancelled=false. */
  fun <T : MKACEvent> subscribe(
    pluginContext: Any,
    eventType: Class<T>,
    listener: MKACEventListener<T>,
  )

  /** Subscribe with explicit priority and ignoreCancelled. */
  fun <T : MKACEvent> subscribe(
    pluginContext: Any,
    eventType: Class<T>,
    listener: MKACEventListener<T>,
    priority: Int,
    ignoreCancelled: Boolean,
  )

  /** Unregister a specific listener for a context. */
  fun unregisterListener(pluginContext: Any, listener: MKACEventListener<*>)

  /** Unregister all listeners for a context. */
  fun unregisterAll(pluginContext: Any)
}
