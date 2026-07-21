// ADS I Class Project
// Pipelined RISC-V Core - ID Stage
//
// Chair of Electronic Design Automation, RPTU in Kaiserslautern
// File created on 01/09/2026 by Tobias Jauch (@tojauch)

/*
Instruction Decode (ID) Stage: decoding and operand fetch

Extracted Fields from 32-bit Instruction (see RISC-V specification for reference):
    opcode: instruction format identifier
    funct3: selects variant within instruction format
    funct7: further specifies operation type (R-type only)
    rd: destination register address
    rs1: first source register address
    rs2: second source register address
    imm: 12-bit immediate value (I-type, sign-extended)

Register File Interfaces:
    regFileReq_A, regFileResp_A: read port for rs1 operand
    regFileReq_B, regFileResp_B: read port for rs2 operand

Internal Signals:
    Combinational decoders for instructions

Functionality:
    Decode opcode to determine instruction and identify operation (ADD, SUB, XOR, ...)
    Output: uop (operation code), rd, operandA (from rs1), operandB (rs2 or immediate)

Outputs:
    uop: micro-operation code (identifies instruction type)
    rd: destination register index
    operandA: first operand
    operandB: second operand 
    XcptInvalid: exception flag for invalid instructions
*/

package core_tile

import chisel3._
import chisel3.util._
import uopc._

// -----------------------------------------
// Decode Stage
// -----------------------------------------

class ID extends Module {
  val io = IO(new Bundle {
    val regFileReq_A  = Flipped(new regFileReadReq) 
    val regFileResp_A = Flipped(new regFileReadResp) 
    val regFileReq_B  = Flipped(new regFileReadReq) 
    val regFileResp_B = Flipped(new regFileReadResp) 
    val instr         = Input(UInt(32.W))
    val uop           = Output(uopc())
    val rd            = Output(UInt(5.W))
    val operandA      = Output(UInt(32.W))
    val operandB      = Output(UInt(32.W))
    val XcptInvalid   = Output(Bool())
  })

  val opcode  = io.instr(6, 0)
  io.rd      := io.instr(11, 7)
  val funct3  = io.instr(14, 12)
  val rs1     = io.instr(19, 15)

  // R-Type
  val funct7  = io.instr(31, 25)
  val rs2     = io.instr(24, 20)

  // I-Type
  val imm     = io.instr(31, 20)
  // Sign-extend the 12-bit immediate to 32 bits
  val immSignExt = Cat(Fill(20, imm(11)), imm)
  
  // Initialize signals
  val isInvalid = WireInit(true.B)
  
  isInvalid := true.B
  io.uop := isNOP

  when(opcode === "b0110011".U){
    when(funct3 === "b000".U){
      when(funct7 === "b0000000".U){
        io.uop := isADD
        isInvalid := false.B
      }.elsewhen(funct7 === "b0100000".U){
        io.uop := isSUB
        isInvalid := false.B
      }
    }.elsewhen(funct3 === "b100".U){
      when(funct7 === "b0000000".U){
        io.uop := isXOR
        isInvalid := false.B
      }
    }.elsewhen(funct3 === "b110".U){
      when(funct7 === "b0000000".U){
        io.uop := isOR
        isInvalid := false.B
      }
    }.elsewhen(funct3 === "b111".U){
      when(funct7 === "b0000000".U){
        io.uop := isAND
        isInvalid := false.B
      }
    }.elsewhen(funct3 === "b001".U){
      when(funct7 === "b0000000".U){
        io.uop := isSLL
        isInvalid := false.B
      }
    }.elsewhen(funct3 === "b101".U){
      when(funct7 === "b0000000".U){
        io.uop := isSRL
        isInvalid := false.B
      }.elsewhen(funct7 === "b0100000".U){
        io.uop := isSRA
        isInvalid := false.B
      }
    }.elsewhen(funct3 === "b010".U){
      when(funct7 === "b0000000".U){
        io.uop := isSLT
        isInvalid := false.B
      }
    }.elsewhen(funct3 === "b011".U){
      when(funct7 === "b0000000".U){
        io.uop := isSLTU
        isInvalid := false.B
      }
    }
  }.elsewhen(opcode === "b0010011".U){
    when(funct3 === "b000".U){
      io.uop := isADDI
      isInvalid := false.B
    }.elsewhen(funct3 === "b100".U){
      io.uop := isXORI
      isInvalid := false.B
    }.elsewhen(funct3 === "b110".U){
      io.uop := isORI
      isInvalid := false.B
    }.elsewhen(funct3 === "b111".U){
      io.uop := isANDI
      isInvalid := false.B
    }.elsewhen(funct3 === "b010".U){
      io.uop := isSLTI
      isInvalid := false.B
    }.elsewhen(funct3 === "b011".U){
      io.uop := isSLTIU
      isInvalid := false.B
    }.elsewhen(funct3 === "b001".U){
      when(funct7 === "b0000000".U){
        io.uop := isSLLI
        isInvalid := false.B
      }.elsewhen(funct3 === "b101".U){
        when(funct7 === "b0000000".U){
          io.uop := isSRLI
          isInvalid := false.B
        }.elsewhen(funct7 === "b0100000".U){
          io.uop := isSRAI
          isInvalid := false.B
          }
      }
    }
  }

  io.XcptInvalid := isInvalid

  // Operands
  io.regFileReq_A.addr := rs1
  io.regFileReq_B.addr := rs2

  io.operandA := io.regFileResp_A.data
  // IMM must be sign-extended for I-type instructions!
  io.operandB := Mux(opcode === "b0110011".U, io.regFileResp_B.data, Mux(opcode === "b0010011".U, Cat(Fill(20, imm(11)), imm), 0.U))
  
}