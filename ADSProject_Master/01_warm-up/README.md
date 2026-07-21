# Warm-Up: Digital Design with Chisel

This directory contains the warm-up assignment for ADS I (Applied Digital Systems I), introducing fundamental concepts in hardware design using Chisel and testing with testbenches.

## Prerequisites

- Complete the Chisel Introduction document to set up a working Chisel environment on your system
- Ensure you have Git installed and configured
- Familiarity with Scala or willingness to learn object-oriented programming concepts

## Tasks Overview

### Task 1: Half Adder

Implement a basic **half adder** that adds two 1-bit numbers and produces a sum output and carry-out signal.

**Specifications:**
- Inputs: `a`, `b` (1-bit each)
- Outputs: `s` (sum), `c_o` (carry-out)
- Behavior: Combinational (no delay between input and output)
- Truth table:

| a | b | s | c_o |
|---|---|---|-----|
| 0 | 0 | 0 | 0   |
| 0 | 1 | 1 | 0   |
| 1 | 0 | 1 | 0   |
| 1 | 1 | 0 | 1   |

**Implementation:** Implement in `src/main/scala/Adder.scala`

**Testing:** Use exhaustive testing with all possible input combinations in `src/test/scala/HalfAdderTester.scala`

---

### Task 2: Full Adder

Implement a **full adder** that adds two 1-bit numbers plus a carry-in bit.

**Specifications:**
- Inputs: `a`, `b` (1-bit each), `c_i` (carry-in, 1-bit)
- Outputs: `s` (sum), `c_o` (carry-out)
- Behavior: Combinational (no delay between input and output)
- **Constraint:** Must use exactly **two half adders** (from Task 1) and basic logic operators (AND, OR, etc.)
- Truth table:

| a | b | c_i | s | c_o |
|---|---|-----|---|-----|
| 0 | 0 | 0   | 0 | 0   |
| 0 | 0 | 1   | 1 | 0   |
| 0 | 1 | 0   | 1 | 0   |
| 0 | 1 | 1   | 0 | 1   |
| 1 | 0 | 0   | 1 | 0   |
| 1 | 0 | 1   | 0 | 1   |
| 1 | 1 | 0   | 0 | 1   |
| 1 | 1 | 1   | 1 | 1   |

**Implementation:** Implement in `src/main/scala/Adder.scala`

**Testing:** Use exhaustive testing with all possible input combinations in `src/test/scala/FullAdderTester.scala`

---

### Task 3: 4-Bit Adder

Implement a **4-bit ripple-carry adder** that adds two 4-bit unsigned numbers.

**Specifications:**
- Inputs: `a`, `b` (4-bit each)
- Outputs: `s` (4-bit sum), `c_o` (carry-out, 1-bit)
- Behavior: Combinational (no delay between input and output)
- **Construction:** Use one half adder for the least significant bit (LSB) and n-1 = 3 full adders for remaining bits
- Range: Supports unsigned integers 0-15

**Implementation:** Implement in `src/main/scala/Adder.scala`

**Testing:** Use nested loops for comprehensive testing in `src/test/scala/FourBitAdderTester.scala`
- Test the full range of 4-bit inputs (0-15)
- Verify overflow behavior (sum > 15)
- Test edge cases:
  - When carry ripples through all stages
  - Boundary conditions (maximum values)
  - Overflow detection

---

### Task 4: Serial Receiver

Implement a **serial receiver** that decodes serial byte transmissions from an input line.

**Specifications:**

**Protocol:**
- Start bit: '0' (indicates transmission start)
- Data bits: 8 bits, MSB first
- No parity bit, no stop bit
- Idle state: bus line high ('1')
- New transmission can begin immediately after 8th data bit, or bus returns to idle

**Inputs:**
- `rxd`: Serial input line (1-bit)
- `clk`: Clock signal
- `reset`: Reset signal (aborts ongoing transmission)

**Outputs:**
- `data`: 8-bit parallel data output
- `valid`: 1-bit signal that goes high ('1') for one clock cycle when a complete byte is received

**Design Components:**
- Bit counter: Tracks position in transmission
- Shift register: Accumulates received bits
- Main controller: Manages state machine

**Reset Behavior:**
- When `reset` goes high, abort current transmission
- Reset counter and set `valid` to '0'
- Resume waiting for new start bit ('0')

**Implementation:** Implement in `src/main/scala/ReadSerial.scala`

**Testing:** Design comprehensive test cases in `src/test/scala/ReadSerialTester.scala`
- Validate basic reception of complete bytes
- Test reset signal functionality
- Test error-prone scenarios:
  - Multiple consecutive transmissions
  - Idle periods between transmissions
  - Reset during transmission
- Consider formal verification of the SystemVerilog output (if VDS Lab experience available)

---

## Project Structure

```
src/
├── main/
│   └── scala/
│       ├── Adder.scala          # Half, Full, and 4-bit Adder implementations
│       ├── ReadSerial.scala     # Serial receiver implementation
│       ├── Basic_Adder.scala    # Reference implementation
│       └── MakeVerilog.scala    # Verilog generation utilities
└── test/
    └── scala/
        ├── HalfAdderTester.scala       # Half adder test cases
        ├── FullAdderTester.scala       # Full adder test cases
        ├── FourBitAdderTester.scala    # 4-bit adder test cases
        └── ReadSerialTester.scala      # Serial receiver test cases
```

## Build and Test

Build the project:
```bash
sbt compile
```

Run all tests:
```bash
sbt test
```

Run specific tests:
```bash
sbt "testOnly adder.HalfAdderTester"
sbt "testOnly adder.FullAdderTester"
sbt "testOnly adder.FourBitAdderTester"
sbt "testOnly readserialtest.ReadSerialTester"
```

Generate Verilog output:
```bash
sbt "runMain makeverilog.MakeVerilog"
```

---

## Learning Outcomes

After completing this warm-up assignment, you should understand:
- Basic digital logic design (combinational circuits)
- Component composition and hierarchy
- Hardware testbenches and exhaustive testing
- Serial communication protocols
- Chisel hardware description language syntax and concepts
- Version control with Git
- Test-driven design methodology

## Notes

- All implementations should be combinational (no sequential logic except where required)
- Test cases should be comprehensive to catch edge cases
- Use meaningful variable and module names for code clarity
- Refer to the Chisel documentation when needed
- Start with simpler tasks (Half Adder) to build understanding before tackling complex designs (Serial Receiver)
