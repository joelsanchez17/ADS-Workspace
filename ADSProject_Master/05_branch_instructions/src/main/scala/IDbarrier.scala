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
    val inRS1          = Input(UInt(5.W))          
    val inRS2          = Input(UInt(5.W))
    val inRD           = Input(UInt(5.W))
    val inOperandA     = Input(UInt(32.W))
    val inOperandB     = Input(UInt(32.W))
    val inImm          = Input(UInt(32.W))
    val inPC           = Input(UInt(32.W))
    val inWrEn         = Input(Bool())
    val inXcptInvalid  = Input(Bool())
    val flush          = Input(Bool())
    val outUOP         = Output(uopc())
    val outRS1         = Output(UInt(5.W))          
    val outRS2         = Output(UInt(5.W))
    val outRD          = Output(UInt(5.W))
    val outOperandA    = Output(UInt(32.W))
    val outOperandB    = Output(UInt(32.W))
    val outImm         = Output(UInt(32.W))
    val outPC          = Output(UInt(32.W))
    val outWrEn        = Output(Bool())
    val outXcptInvalid = Output(Bool())
  })

  val uop         = RegInit(isNOP)
  val rs1         = RegInit(0.U(5.W))
  val rs2         = RegInit(0.U(5.W))
  val rd          = RegInit(0.U(5.W))
  val operandA    = RegInit(0.U(32.W))
  val operandB    = RegInit(0.U(32.W))
  val imm         = RegInit(0.U(32.W))
  val pc          = RegInit(0.U(32.W))
  val wrEN        = RegInit(false.B)
  val XcptInvalid = RegInit(false.B)

  io.outUOP := uop
  uop := Mux(io.flush, isNOP, io.inUOP)

  io.outRS1 := rs1
  rs1 := Mux(io.flush, 0.U, io.inRS1)

  io.outRS2 := rs2
  rs2 := Mux(io.flush, 0.U, io.inRS2)

  io.outRD := rd
  rd := Mux(io.flush, 0.U, io.inRD)

  io.outOperandA := operandA
  operandA := Mux(io.flush, 0.U, io.inOperandA)
  io.outOperandB := operandB
  operandB := Mux(io.flush, 0.U, io.inOperandB)

  io.outImm := imm
  imm := Mux(io.flush, 0.U, io.inImm)

  io.outPC := pc
  pc := Mux(io.flush, 0.U, io.inPC)

  io.outWrEn := wrEN
  wrEN := Mux(io.flush, false.B, io.inWrEn)

  io.outXcptInvalid := XcptInvalid
  XcptInvalid := Mux(io.flush, false.B, io.inXcptInvalid)

}
