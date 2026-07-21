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

"RV32I_BasicTester" should "cover basic R-type and I-type instructions" in {
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
      dut.io.result.expect(9.U)     // ADD x3, x1, x2
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(2047.U)  // ADDI x4, x0, 2047
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(16.U)    // ADDI x5, x0, 16
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(2031.U)  // SUB x6, x4, x5
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

"RV32I_BranchJumpTester" should "cover branches and jumps" in {
    test(new PipelinedRV32I("src/test/programs/BinaryFile_branch_jump")).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>

      dut.clock.setTimeout(0)
      dut.clock.step(5)
      dut.io.result.expect(5.U)                          // ADDI x1, x0, 5
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(5.U)                          // ADDI x2, x0, 5
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(7.U)                          // ADDI x3, x0, 7
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(10.U)                         // BEQ x1, x2 taken
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(0.U)                          // flush bubble after taken BEQ
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(0.U)                          // flush bubble after taken BEQ
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(1.U)                          // ADDI x4, x0, 1
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(12.U)                         // BNE x1, x3 taken
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(0.U)                          // flush bubble after taken BNE
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(0.U)                          // flush bubble after taken BNE
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(2.U)                          // ADDI x5, x0, 2
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(12.U)                         // BLT x1, x3 taken
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(0.U)                          // flush bubble after taken BLT
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(0.U)                          // flush bubble after taken BLT
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(3.U)                          // ADDI x6, x0, 3
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(12.U)                         // BGE x3, x1 taken
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(0.U)                          // flush bubble after taken BGE
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(0.U)                          // flush bubble after taken BGE
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(4.U)                          // ADDI x7, x0, 4
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect("hffffffff".U)                // ADDI x8, x0, -1
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(4.U)                          // BLTU x1, x8 taken
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(0.U)                          // flush bubble after taken BLTU
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(0.U)                          // flush bubble after taken BLTU
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(5.U)                          // ADDI x9, x0, 5
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(4.U)                          // BGEU x8, x1 taken
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(0.U)                          // flush bubble after taken BGEU
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(0.U)                          // flush bubble after taken BGEU
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(6.U)                          // ADDI x11, x0, 6
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(12.U)                         // BEQ x1, x3 not taken
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(7.U)                          // ADDI x12, x0, 7
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(10.U)                         // BNE x1, x2 not taken
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(8.U)                          // ADDI x13, x0, 8
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(12.U)                         // BLT x3, x1 not taken
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(9.U)                          // ADDI x14, x0, 9
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(12.U)                         // BGE x1, x3 not taken
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(10.U)                         // ADDI x15, x0, 10
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(4.U)                          // BLTU x8, x1 not taken
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(11.U)                         // ADDI x16, x0, 11
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(4.U)                          // BGEU x1, x8 not taken
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(12.U)                         // ADDI x17, x0, 12
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(164.U)                        // JAL x18, jal_target link
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(0.U)                          // flush bubble after JAL
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(0.U)                          // flush bubble after JAL
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(13.U)                         // ADDI x19, x0, 13
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(192.U)                        // ADDI x20, x0, jalr_target address
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(184.U)                        // JALR x21, 0(x20) link
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(0.U)                          // flush bubble after JALR
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(0.U)                          // flush bubble after JALR
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(14.U)                         // ADDI x22, x0, 14
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(200.U)                        // JAL x0, final_target link value on result bus
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(0.U)                          // flush bubble after JAL x0
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(0.U)                          // flush bubble after JAL x0
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(15.U)                         // ADDI x23, x0, 15
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect("hffffffff".U)                // ADDI x24, x0, -1
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(0.U)                          // ADDI x25, x24, 1
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(0.U)                          // ADDI x26, x0, 0
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
    }
  }
}
