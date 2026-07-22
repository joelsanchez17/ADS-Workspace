// ADS I Class Project
// Pipelined RISC-V Core - EX Barrier
//
// Chair of Electronic Design Automation, RPTU in Kaiserslautern
// File created on 01/09/2026 by Tobias Jauch (@tojauch)

/*
EX-Barrier: pipeline register between Execute and Memory stages

Internal Registers:
    aluResult: ALU computation result
    rd: destination register index
    exception: exception flag

Inputs:
    inAluResult: computation result from EX stage
    inRD: destination register from EX stage
    inXcptInvalid: exception flag from EX stage

Outputs:
    outAluResult: result to MEM stage
    outRD: destination register to MEM stage
    outXcptInvalid: exception flag to MEM stage

Functionality:
    Save all input signals to a register and output them in the following clock cycle
*/

package core_tile

import chisel3._

// -----------------------------------------
// EX-Barrier
// -----------------------------------------

class EXBarrier extends Module {
  val io = IO(new Bundle {
    val inAluResult  = Input(UInt(32.W))
    val outAluResult = Output(UInt(32.W))
    val inInstr        = Input(UInt(32.W))
    val outInstr       = Output(UInt(32.W))
    val inPC           = Input(UInt(32.W))
    val outPC          = Output(UInt(32.W))
    val inRD           = Input(UInt(5.W))
    val outRD          = Output(UInt(5.W))
    val inWrEn         = Input(Bool())
    val outWrEn        = Output(Bool())
    val inXcptInvalid  = Input(Bool())
    val outXcptInvalid = Output(Bool())
  })

  val aluResult   = RegInit(0.U(32.W))
  val instr       = RegInit(0.U(32.W))
  val pc          = RegInit(0.U(32.W))
  val rd          = RegInit(0.U(5.W))
  val wrEN        = RegInit(false.B)
  val XcptInvalid = RegInit(false.B)
  val exception   = RegInit(false.B)

  io.outAluResult := aluResult
  aluResult       := io.inAluResult

  io.outInstr := instr
  instr       := io.inInstr

  io.outPC := pc
  pc       := io.inPC

  io.outRD := rd
  rd := io.inRD

  io.outWrEn := wrEN
  wrEN := io.inWrEn

  io.outXcptInvalid := XcptInvalid
  XcptInvalid := io.inXcptInvalid

}
