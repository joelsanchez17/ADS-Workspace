// ADS I Class Project
// Pipelined RISC-V Core - EX Stage
//
// Chair of Electronic Design Automation, RPTU in Kaiserslautern
// File created on 01/09/2026 by Tobias Jauch (@tojauch)

/*
Instruction Execute (EX) Stage: ALU operations and exception detection

Instantiated Modules:
    ALU: Integrate your module from Assignment02 for arithmetic/logical operations

ALU Interface:
    alu.io.operandA: first operand input
    alu.io.operandB: second operand input
    alu.io.operation: operation code controlling ALU function
    alu.io.aluResult: computation result output

Internal Signals:
    Map uopc codes to ALUOp values

Functionality:
    Map instruction uop to ALU operation code
    Pass operands to ALU
    Output results to pipeline

Outputs:
    aluResult: computation result from ALU
    exception: pass exception flag
*/

package core_tile

import chisel3._
import chisel3.util._
import Assignment02.{ALU, ALUOp}
import uopc._

// -----------------------------------------
// Execute Stage
// -----------------------------------------

class EX extends Module {
  val io = IO(new Bundle {
    val uop       = Input(uopc())
    val operandA   = Input(UInt(32.W))
    val operandB   = Input(UInt(32.W))
    val aluResult  = Output(UInt(32.W))
    val inXcptInvalid = Input(Bool())
    val outXcptInvalid = Output(Bool())
  })

  // Instantiate the ALU
  val alu = Module(new ALU)

  // Connect operands to ALU
  alu.io.operandA := io.operandA
  alu.io.operandB := io.operandB

  // Map uopc operations to ALUOp
  alu.io.operation := MuxLookup(io.uop.asUInt, ALUOp.ADD, Array(
    isADD.asUInt   -> ALUOp.ADD,
    isSUB.asUInt   -> ALUOp.SUB,
    isADDI.asUInt  -> ALUOp.ADD,
    isXOR.asUInt   -> ALUOp.XOR,
    isXORI.asUInt  -> ALUOp.XOR,
    isOR.asUInt    -> ALUOp.OR,
    isORI.asUInt   -> ALUOp.OR,
    isAND.asUInt   -> ALUOp.AND,
    isANDI.asUInt  -> ALUOp.AND,
    isSLL.asUInt   -> ALUOp.SLL,
    isSLLI.asUInt  -> ALUOp.SLL,
    isSRL.asUInt   -> ALUOp.SRL,
    isSRLI.asUInt  -> ALUOp.SRL,
    isSRA.asUInt   -> ALUOp.SRA,
    isSRAI.asUInt  -> ALUOp.SRA,
    isSLT.asUInt   -> ALUOp.SLT,
    isSLTI.asUInt  -> ALUOp.SLT,
    isSLTU.asUInt  -> ALUOp.SLTU,
    isSLTIU.asUInt -> ALUOp.SLTU
  ))

  // Connect ALU result and exception to output
  io.aluResult := alu.io.aluResult
  io.outXcptInvalid := io.inXcptInvalid

}
