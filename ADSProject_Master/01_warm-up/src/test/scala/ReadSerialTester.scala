// ADS I Class Project
// Chisel Introduction
//
// Chair of Electronic Design Automation, RPTU in Kaiserslautern
// File created on 18/10/2022 by Tobias Jauch (@tojauch)

package readserial

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec


/** 
  *read serial tester
  */
class ReadSerialTester extends AnyFlatSpec with ChiselScalatestTester {

  "ReadSerial" should "work" in {
    test(new ReadSerial).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>

          // idle sequence
          dut.io.rxd.poke(1.U)
          dut.clock.step(1)                // move one clock cycle ahead   
          dut.io.valid.expect(false.B)

          // begin transmission
          dut.io.rxd.poke(0.U)
          dut.clock.step(1)                // move one clock cycle ahead   
          dut.io.valid.expect(false.B)

          // Bit 1
          dut.io.rxd.poke(1.U)
          dut.clock.step(1)                // move one clock cycle ahead   
          dut.io.valid.expect(false.B)
          // Bit 2
          dut.io.rxd.poke(0.U)
          dut.clock.step(1)                // move one clock cycle ahead   
          dut.io.valid.expect(false.B)
          // Bit 3
          dut.io.rxd.poke(1.U)
          dut.clock.step(1)                // move one clock cycle ahead   
          dut.io.valid.expect(false.B)
          // Bit 4
          dut.io.rxd.poke(0.U)
          dut.clock.step(1)                // move one clock cycle ahead   
          dut.io.valid.expect(false.B)
          // Bit 5
          dut.io.rxd.poke(1.U)
          dut.clock.step(1)                // move one clock cycle ahead   
          dut.io.valid.expect(false.B)
          // Bit 6
          dut.io.rxd.poke(0.U)
          dut.clock.step(1)                // move one clock cycle ahead   
          dut.io.valid.expect(false.B)
          // Bit 7
          dut.io.rxd.poke(1.U)
          dut.clock.step(1)                // move one clock cycle ahead   
          dut.io.valid.expect(false.B)
          // Bit 8
          dut.io.rxd.poke(0.U)
          dut.clock.step(1)                // move one clock cycle ahead 

          // output  
          dut.io.valid.expect(true.B) 
          dut.io.data.expect("b10101010".U)

          // short idle
          dut.io.rxd.poke(1.U)
          dut.clock.step(1)                // move one clock cycle ahead   
          dut.io.valid.expect(false.B)

          dut.io.rxd.poke(1.U)
          dut.clock.step(1)                // move one clock cycle ahead   
          dut.io.valid.expect(false.B)

          // new transmission
          dut.io.rxd.poke(0.U)
          dut.clock.step(1)                // move one clock cycle ahead   
          dut.io.valid.expect(false.B)

          // Bit 1
          dut.io.rxd.poke(0.U)
          dut.clock.step(1)                // move one clock cycle ahead   
          dut.io.valid.expect(false.B)
          // Bit 2
          dut.io.rxd.poke(0.U)
          dut.clock.step(1)                // move one clock cycle ahead   
          dut.io.valid.expect(false.B)
          // Bit 3
          dut.io.rxd.poke(1.U)
          dut.clock.step(1)                // move one clock cycle ahead   
          dut.io.valid.expect(false.B)
          // Bit 4
          dut.io.rxd.poke(0.U)
          dut.clock.step(1)                // move one clock cycle ahead   
          dut.io.valid.expect(false.B)
          // Bit 5
          dut.io.rxd.poke(0.U)
          dut.clock.step(1)                // move one clock cycle ahead   
          dut.io.valid.expect(false.B)
          // Bit 6
          dut.io.rxd.poke(1.U)
          dut.clock.step(1)                // move one clock cycle ahead   
          dut.io.valid.expect(false.B)
          // Bit 7
          dut.io.rxd.poke(0.U)
          dut.clock.step(1)                // move one clock cycle ahead   
          dut.io.valid.expect(false.B)
          // Bit 8
          dut.io.rxd.poke(0.U)
          dut.clock.step(1)                // move one clock cycle ahead 

          // output  
          dut.io.valid.expect(true.B) 
          dut.io.data.expect("b00100100".U)

          // immediate new transmission
          dut.io.rxd.poke(0.U)
          dut.clock.step(1)                // move one clock cycle ahead   
          dut.io.valid.expect(false.B)

          // Bit 1
          dut.io.rxd.poke(1.U)
          dut.clock.step(1)                // move one clock cycle ahead   
          dut.io.valid.expect(false.B)
          // Bit 2
          dut.io.rxd.poke(1.U)
          dut.clock.step(1)                // move one clock cycle ahead   
          dut.io.valid.expect(false.B)
          // Bit 3
          dut.io.rxd.poke(1.U)
          dut.clock.step(1)                // move one clock cycle ahead   
          dut.io.valid.expect(false.B)
          // Bit 4
          dut.io.rxd.poke(1.U)
          dut.clock.step(1)                // move one clock cycle ahead   
          dut.io.valid.expect(false.B)
          // Bit 5
          dut.io.rxd.poke(1.U)
          dut.clock.step(1)                // move one clock cycle ahead   
          dut.io.valid.expect(false.B)
          // Bit 6
          dut.io.rxd.poke(1.U)
          dut.clock.step(1)                // move one clock cycle ahead   
          dut.io.valid.expect(false.B)
          // Bit 7
          dut.io.rxd.poke(1.U)
          dut.clock.step(1)                // move one clock cycle ahead   
          dut.io.valid.expect(false.B)
          // Bit 8
          dut.io.rxd.poke(0.U)
          dut.clock.step(1)                // move one clock cycle ahead 

          // output  
          dut.io.valid.expect(true.B) 
          dut.io.data.expect("b11111110".U)

        }
    } 
}

