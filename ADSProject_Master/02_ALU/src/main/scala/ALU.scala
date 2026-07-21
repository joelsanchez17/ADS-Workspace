// ADS I Class Project
// Assignment 02: Arithmetic Logic Unit and UVM Testbench
//
// Chair of Electronic Design Automation, RPTU University Kaiserslautern-Landau
// File created on 09/21/2025 by Tharindu Samarakoon (gug75kex@rptu.de)
// File updated on 10/29/2025 by Tobias Jauch (tobias.jauch@rptu.de)

package Assignment02

import chisel3._
import chisel3.util._
import chisel3.experimental.ChiselEnum

object ALUOp extends ChiselEnum {
  val ADD, SUB, AND, OR, XOR, SLL, SRL, SRA, SLT, SLTU, PASSB = Value
}

class ALU extends Module {
  val width = 32
  val io = IO(new Bundle {
    val operation = Input(ALUOp())
    val operandA  = Input(UInt(width.W))
    val operandB  = Input(UInt(width.W))
    val aluResult = Output(UInt(width.W))
    val exception = Output(Bool())
  })

  // RV32: shift amount uses only the lower 5 bits of operand B
  val shamt = io.operandB(4, 0)

  // default case: decoder should avoid driving unknown ops
  val res = WireInit(0.U(width.W))
  val illegal = WireInit(true.B)

  switch(io.operation) {
    is(ALUOp.ADD)  { res := io.operandA + io.operandB; illegal := false.B }                         // might produce wrap-around
    is(ALUOp.SUB)  { res := io.operandA - io.operandB; illegal := false.B }
    is(ALUOp.AND)  { res := io.operandA & io.operandB; illegal := false.B }
    is(ALUOp.OR)   { res := io.operandA | io.operandB; illegal := false.B }
    is(ALUOp.XOR)  { res := io.operandA ^ io.operandB; illegal := false.B }
    is(ALUOp.SLL)  { res := io.operandA << shamt; illegal := false.B }
    is(ALUOp.SRL)  { res := io.operandA >> shamt; illegal := false.B }                              // shift right logical
    is(ALUOp.SRA)  { res := (io.operandA.asSInt >> shamt).asUInt; illegal := false.B }              // shift right arithmetic
    is(ALUOp.SLT)  { res := (io.operandA.asSInt < io.operandB.asSInt).asUInt; illegal := false.B }
    is(ALUOp.SLTU) { res := (io.operandA < io.operandB).asUInt; illegal := false.B }
    is(ALUOp.PASSB){ res := io.operandB; illegal := false.B }                                       // e.g., LUI/AUIPC path
  }

  io.aluResult := res
  io.exception := illegal

  // Simulation-time check: operation must be one of the enum values
  assert(io.operation.isOneOf(ALUOp.all), "Illegal ALUOp: %d", io.operation.asUInt)

}