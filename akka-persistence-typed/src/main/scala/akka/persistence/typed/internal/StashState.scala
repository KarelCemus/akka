/*
 * Copyright (C) 2016-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package akka.persistence.typed.internal

import akka.actor.typed.scaladsl.StashBuffer
import akka.annotation.InternalApi
import akka.persistence.typed.internal.EventsourcedBehavior.InternalProtocol

/** INTERNAL API: stash buffer state in order to survive restart of internal behavior */
@InternalApi
private[akka] class StashState(settings: EventsourcedSettings) {

  private var _internalStashBuffer: StashBuffer[InternalProtocol] = StashBuffer(settings.stashCapacity)
  private var _externalStashBuffer: StashBuffer[InternalProtocol] = StashBuffer(settings.stashCapacity)
  private var unstashAllExternalInProgress = 0

  def internalStashBuffer: StashBuffer[InternalProtocol] = _internalStashBuffer

  def externalStashBuffer: StashBuffer[InternalProtocol] = _externalStashBuffer

  def clearStashBuffers(): Unit = {
    _internalStashBuffer = StashBuffer(settings.stashCapacity)
    _externalStashBuffer = StashBuffer(settings.stashCapacity)
    unstashAllExternalInProgress = 0
  }

  def isUnstashAllExternalInProgress: Boolean =
    unstashAllExternalInProgress > 0

  def incrementUnstashAllExternalProgress(): Unit =
    unstashAllExternalInProgress += 1

  def decrementUnstashAllExternalProgress(): Unit =
    unstashAllExternalInProgress -= 1

  def startUnstashAllExternal(): Unit =
    unstashAllExternalInProgress = _externalStashBuffer.size

}
