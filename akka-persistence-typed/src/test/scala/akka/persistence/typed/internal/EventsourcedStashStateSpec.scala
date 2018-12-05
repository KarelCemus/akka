/*
 * Copyright (C) 2018 Lightbend Inc. <https://www.lightbend.com>
 */

package akka.persistence.typed.internal

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ Behavior, Signal }
import akka.persistence.typed.internal.EventsourcedBehavior.InternalProtocol
import akka.persistence.typed.internal.EventsourcedBehavior.InternalProtocol.{ IncomingCommand, RecoveryPermitGranted }
import akka.actor.testkit.typed.scaladsl.TestProbe

import scala.concurrent.duration._
import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.scalatest.WordSpecLike

class EventsourcedStashStateSpec extends ScalaTestWithActorTestKit with WordSpecLike {

  "EventsourcedStashReferenceManagement instance" should {

    "clear buffer on PostStop" in {
      val probe = TestProbe[Int]()
      val behavior = TestBehavior(probe)
      val ref = spawn(behavior)
      ref ! RecoveryPermitGranted
      ref ! RecoveryPermitGranted
      probe.expectMessage(1)
      probe.expectMessage(2)
      ref ! IncomingCommand("bye")
      probe.expectTerminated(ref, 100.millis)

      spawn(behavior) ! RecoveryPermitGranted
      probe.expectMessage(1)
    }
  }

  object TestBehavior {

    def apply(probe: TestProbe[Int]): Behavior[InternalProtocol] = {
      val settings = dummySettings()
      Behaviors.setup[InternalProtocol] { ctx ⇒
        val stashState = new StashState(settings)
        Behaviors.receiveMessagePartial[InternalProtocol] {
          case RecoveryPermitGranted ⇒
            stashState.internalStashBuffer.stash(RecoveryPermitGranted)
            probe.ref ! stashState.internalStashBuffer.size
            Behaviors.same[InternalProtocol]
          case _: IncomingCommand[_] ⇒ Behaviors.stopped
        }.receiveSignal {
          case (_, signal: Signal) ⇒
            stashState.clearStashBuffers()
            Behaviors.stopped[InternalProtocol]
        }
      }
    }
  }

  private def dummySettings(capacity: Int = 42) =
    EventsourcedSettings(
      stashCapacity = capacity,
      stashOverflowStrategyConfigurator = "akka.persistence.ThrowExceptionConfigurator",
      logOnStashing = false,
      recoveryEventTimeout = 3.seconds,
      journalPluginId = "",
      snapshotPluginId = "")

}
