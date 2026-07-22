// ADS I Class Project
// Pipelined RISC-V Core - ID Barrier
//
// Chair of Electronic Design Automation, RPTU in Kaiserslautern
// File created on 01/09/2026 by Tobias Jauch (@tojauch)

/*
ID-Barrier: pipeline register between Decode and Execute stages

Internal Registers:
    uop: micro-operation code (from uopc enum)
    rd: destination register index, initialized to 0
    operandA: first source operand, initialized to 0
    operandB: second operand/immediate, initialized to 0

Inputs:
    inUOP: micro-operation code from ID stage
    inRD: destination register from ID stage
    inOperandA: first operand from ID stage
    inOperandB: second operand/immediate from ID stage
    inXcptInvalid: exception flag from ID stage

Outputs:
    outUOP: micro-operation code to EX stage
    outRD: destination register to EX stage
    outOperandA: first operand to EX stage
    outOperandB: second operand to EX stage
    outXcptInvalid: exception flag to EX stage
Functionality:
    Save all input signals to a register and output them in the following clock cycle
*/

package core_tile

import chisel3._
import uopc._

// -----------------------------------------
// ID-Barrier
// -----------------------------------------

class IDBarrier extends Module {
  val io = IO(new Bundle {
    val inUOP          = Input(uopc())
    val inInstr        = Input(UInt(32.W))
    val inPC           = Input(UInt(32.W))
    val inRS1          = Input(UInt(5.W))          
    val inRS2          = Input(UInt(5.W))
    val inRD           = Input(UInt(5.W))
    val inOperandA     = Input(UInt(32.W))
    val inOperandB     = Input(UInt(32.W))
    val inWrEn         = Input(Bool())
    val inXcptInvalid  = Input(Bool())
    val outUOP         = Output(uopc())
    val outInstr       = Output(UInt(32.W))
    val outPC          = Output(UInt(32.W))
    val outRS1         = Output(UInt(5.W))          
    val outRS2         = Output(UInt(5.W))
    val outRD          = Output(UInt(5.W))
    val outOperandA    = Output(UInt(32.W))
    val outOperandB    = Output(UInt(32.W))
    val outWrEn        = Output(Bool())
    val outXcptInvalid = Output(Bool())
  })

  val uop         = Reg(uopc())
  val instr       = RegInit(0.U(32.W))
  val pc          = RegInit(0.U(32.W))
  val rs1         = RegInit(0.U(5.W))
  val rs2         = RegInit(0.U(5.W))
  val rd          = RegInit(0.U(5.W))
  val operandA    = RegInit(0.U(32.W))
  val operandB    = RegInit(0.U(32.W))
  val wrEN        = RegInit(false.B)
  val XcptInvalid = RegInit(false.B)

  io.outUOP := uop
  uop := io.inUOP

  io.outInstr := instr
  instr := io.inInstr

  io.outPC := pc
  pc := io.inPC

  io.outRS1 := rs1
  rs1 := io.inRS1

  io.outRS2 := rs2
  rs2 := io.inRS2

  io.outRD := rd
  rd := io.inRD

  io.outOperandA := operandA
  operandA := io.inOperandA
  io.outOperandB := operandB
  operandB := io.inOperandB

  io.outWrEn := wrEN
  wrEN := io.inWrEn

  io.outXcptInvalid := XcptInvalid
  XcptInvalid := io.inXcptInvalid

}
