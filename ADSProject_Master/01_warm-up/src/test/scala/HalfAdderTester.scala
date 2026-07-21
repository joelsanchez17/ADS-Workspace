// ADS I Class Project
// Chisel Introduction
//
// Chair of Electronic Design Automation, RPTU in Kaiserslautern
// File created on 18/10/2022 by Tobias Jauch (@tojauch)

package adder

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec


/** 
  *half adder tester
  */
class HalfAdderTester extends AnyFlatSpec with ChiselScalatestTester {

  "HalfAdder" should "work" in {
    test(new HalfAdder).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
          dut.io.a.poke(0.U)
          dut.io.b.poke(0.U)
          dut.io.c.expect(0.U)
          dut.io.s.expect(0.U)

          dut.io.a.poke(1.U)
          dut.io.b.poke(0.U)
          dut.io.c.expect(0.U)
          dut.io.s.expect(1.U)

          dut.io.a.poke(0.U)
          dut.io.b.poke(1.U)
          dut.io.c.expect(0.U)
          dut.io.s.expect(1.U)

          dut.io.a.poke(1.U)
          dut.io.b.poke(1.U)
          dut.io.c.expect(1.U)
          dut.io.s.expect(0.U)
        }
    } 
}

