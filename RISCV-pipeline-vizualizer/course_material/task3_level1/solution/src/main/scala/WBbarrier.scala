// ADS I Class Project
// Pipelined RISC-V Core - WB Barrier
//
// Chair of Electronic Design Automation, RPTU in Kaiserslautern
// File created on 01/09/2026 by Tobias Jauch (@tojauch)

/*
WB-Barrier: final pipeline register after Writeback stage

Internal Registers:
    check_res: final result value for verification, initialized to 0
    isInvalid: invalid instruction flag, initialized to false

Inputs:
    inCheckRes: result from WB stage
    inXcptInvalid: exception flag from MEM barrier

Outputs:
    outCheckRes: final result for external observation
    outXcptInvalid: final exception flag (tied to invalid instruction in ID stage)

Functionality:
    Save all input signals to a register and output them in the following clock cycle
    Enable result observation without pipeline disruption (for result and exception signals)
*/

package core_tile

import chisel3._

// -----------------------------------------
// WB-Barrier
// -----------------------------------------

class WBBarrier extends Module {
  val io = IO(new Bundle {
    val inCheckRes    = Input(UInt(32.W))
    val outCheckRes   = Output(UInt(32.W))
    val inInstr        = Input(UInt(32.W))
    val outInstr       = Output(UInt(32.W))
    val inPC           = Input(UInt(32.W))
    val outPC          = Output(UInt(32.W))
    val inXcptInvalid = Input(Bool())
    val outXcptInvalid  = Output(Bool())
  })

  val check_res   = RegInit(0.U(32.W))
  val instr       = RegInit(0.U(32.W))
  val pc          = RegInit(0.U(32.W))
  val XcptInvalid = RegInit(false.B)

  io.outCheckRes := check_res
  check_res      := io.inCheckRes

  io.outInstr := instr
  instr       := io.inInstr

  io.outPC := pc
  pc       := io.inPC

  io.outXcptInvalid := XcptInvalid
  XcptInvalid := io.inXcptInvalid
}
