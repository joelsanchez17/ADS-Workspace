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
  *4-bit adder tester
  */
class FourBitAdderTester extends AnyFlatSpec with ChiselScalatestTester {

  "4-bit Adder" should "work" in {
    test(new FourBitAdder).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      for (a <- 0 to 15) {
        for (b <- 0 to 15) {

          val result = a + b

          dut.io.a.poke(a.U)
          dut.io.b.poke(b.U)


          if (result < 16){
            dut.io.sum.expect(result.U)
            dut.io.overflow.expect(0.U)
          } else {
            val result_overflow = result - 16
            dut.io.sum.expect(result_overflow.asUInt)
            dut.io.overflow.expect(1.U)
          }
        }
      }
    } 
  }
}

