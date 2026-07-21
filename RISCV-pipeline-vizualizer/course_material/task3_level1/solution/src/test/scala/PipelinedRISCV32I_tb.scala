// ADS I Class Project
// Pipelined RISC-V Core
//
// Chair of Electronic Design Automation, RPTU in Kaiserslautern
// File created on 01/15/2023 by Tobias Jauch (@tojauch)

package PipelinedRV32I_Tester

import chisel3._
import chiseltest._
import PipelinedRV32I._
import org.scalatest.flatspec.AnyFlatSpec

class PipelinedRISCV32ITest extends AnyFlatSpec with ChiselScalatestTester {

"RV32I_BasicTester" should "work" in {
    test(new PipelinedRV32I("src/test/programs/BinaryFile_pipelined")).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>

      dut.clock.setTimeout(0)
      dut.clock.step(5)
      dut.io.result.expect(0.U)     // ADDI x0, x0, 0
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(4.U)     // ADDI x1, x0, 4
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(5.U)     // ADDI x2, x0, 5
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(0.U)     // ADDI x0, x0, 0
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(0.U)     // ADDI x0, x0, 0
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(0.U)     // ADDI x0, x0, 0
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(9.U)     // ADD x3, x1, x2
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(2047.U)  // ADDI x4, x0, 2047
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(16.U)    // ADDI x5, x0, 16
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(0.U)     // ADDI x0, x0, 0
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(0.U)     // ADDI x0, x0, 0
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(0.U)     // ADDI x0, x0, 0
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(2031.U)  // SUB x6, x4, x5
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(0.U)     // ADDI x0, x0, 0
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(0.U)     // ADDI x0, x0, 0
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(0.U)     // ADDI x0, x0, 0
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(2022.U)  // XOR x7, x6, x3
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(2047.U)  // OR x8, x6, x5
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(0.U)     // AND x9, x6, x5
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(0.U)     // ADDI x0, x0, 0
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(64704.U) // SLL x10, x7, x2
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(63.U)    // SRL x11, x7, x2
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(63.U)    // SRA x12, x7, x2
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(0.U)     // SLT x13, x4, x4
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(0.U)     // SLT x13, x4, x5
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(1.U)     // SLT x13, x5, x4
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(0.U)     // SLTU x13, x4, x4
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(0.U)     // SLTU x13, x4, x5
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(1.U)     // SLTU x13, x5, x4
      dut.io.exception.expect(false.B)
      dut.clock.step(1)           
    }
  }

"RV32I_Sign-Ext_Tester" should "work" in {
    test(new PipelinedRV32I("src/test/programs/BinaryFile_pipelined_sign-ext")).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>

      dut.clock.setTimeout(0)
      dut.clock.step(5)
      
      // Test 1: Load -1 into x14 (imm: 0xfff, sign-extended to [rd]: 0xffffffff)
      dut.io.result.expect("hFFFFFFFF".U) // ADDI x14, x0, -1
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      
      // Test 2: Load 16 into x1 (imm: 0x010, positive value stays the same)
      dut.io.result.expect(16.U)          // ADDI x1, x0, 16
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      
      // Test 3: Load -8 into x2 (imm: 0xff8, sign-extended to [rd]: 0xffffff8)
      dut.io.result.expect("hFFFFFFF8".U) // ADDI x2, x0, -8
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      
      // Test 4: Load 0 into x3
      dut.io.result.expect(0.U)           // ADDI x3, x0, 0
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      
      // Test 5: Load 25 into x4
      dut.io.result.expect(25.U)          // ADDI x4, x0, 25
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      
      // Test 6: Load 0 into x5
      dut.io.result.expect(0.U)           // ADDI x5, x0, 0
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      
      // Test 7: ADD x6, x1, x2 = 16 + (-8) = 8
      dut.io.result.expect(8.U)           // ADD x6, x1, x2
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      
      // Test 8: SUB x8, x1, x2 = 16 - (-8) = 24
      dut.io.result.expect(24.U)          // SUB x8, x1, x2
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      
      // Test 9: ADD x9, x4, x2 = 25 + (-8) = 17
      dut.io.result.expect(17.U)          // ADD x9, x4, x2
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      
      // Test 10: ADD x10, x4, x2 (same as above for comparison)
      dut.io.result.expect(17.U)          // ADD x10, x4, x2
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      
      // Test 11: Load 2047 into x11 (max positive 12-bit imm: 0x7ff)
      dut.io.result.expect(2047.U)        // ADDI x11, x0, 2047
      dut.io.exception.expect(false.B)
      dut.clock.step(4) // Extra steps to account for pipeline latency
      
      // Test 12: ADD x12, x11, x2 = 2047 + (-8) = 2039
      dut.io.result.expect(2039.U)        // ADD x12, x11, x2
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
    }
  }


}
